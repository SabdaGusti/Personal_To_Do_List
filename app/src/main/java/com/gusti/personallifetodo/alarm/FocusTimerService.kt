package com.gusti.personallifetodo.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gusti.personallifetodo.MainActivity
import com.gusti.personallifetodo.data.TaskEntity
import com.gusti.personallifetodo.data.TaskStatus
import com.gusti.personallifetodo.util.DateTimeUtil

/**
 * Timer di NOTIFIKASI — permintaan Gusti: "aku ingin timer juga muncul di
 * bagian notifikasi".
 *
 * Selama ada tugas yang sedang dikerjakan, layanan foreground ringan ini
 * menampilkan SATU notifikasi menetap (ongoing) berisi timer yang berjalan:
 * - Mode FOKUS: chronometer menghitung NAIK dari total waktu fokus tugas
 *   (termasuk segmen-segmen sebelumnya), teksnya menyebut deadline.
 * - Mode ISTIRAHAT: chronometer menghitung MUNDUR sisa waktu istirahat
 *   sampai batas 15 menit.
 *
 * Timer-nya dirender oleh SystemUI Android dari stempel `when` notifikasi
 * (setUsesChronometer), jadi tetap berjalan akurat walau aplikasi ditutup —
 * layanan tidak perlu bangun tiap detik. Tap notifikasi membuka Halaman
 * Fokus tugas itu (MainActivity membaca EXTRA_TASK_ID seperti dari alarm).
 * Notifikasi hilang begitu tugas selesai / dibatalkan / dihapus, dan
 * dipulihkan oleh BootReceiver + saat aplikasi dibuka (sync) bila HP sempat
 * restart. Channel-nya IMPORTANCE_LOW: menetap tapi tanpa suara/getar.
 *
 * foregroundServiceType memakai mediaPlayback seperti AlarmRingService —
 * tipe yang izinnya (FOREGROUND_SERVICE_MEDIA_PLAYBACK) sudah dinyatakan
 * proyek ini — supaya tidak menambah izin baru.
 */
class FocusTimerService : Service() {

    companion object {
        const val TIMER_NOTIFICATION_ID = 9001

        private const val ACTION_SHOW = "com.gusti.personallifetodo.action.TIMER_SHOW"
        private const val ACTION_STOP_TIMER = "com.gusti.personallifetodo.action.TIMER_STOP"
        private const val EXTRA_MODE = "extra_timer_mode"
        private const val EXTRA_DEADLINE = "extra_timer_deadline"
        private const val EXTRA_WHEN = "extra_timer_when"
        private const val MODE_FOCUS = "FOCUS"
        private const val MODE_BREAK = "BREAK"

        /** Tampilkan / perbarui timer fokus untuk tugas yang sedang berjalan. */
        fun showFocus(context: Context, task: TaskEntity) {
            val now = System.currentTimeMillis()
            // Basis chronometer = sekarang - total fokus berjalan, supaya
            // angka yang tampil adalah total waktu fokus, bukan segmen ini saja.
            show(context, task.id, task.title, MODE_FOCUS, task.deadline, now - task.liveFocusMs(now))
        }

        /** Tampilkan / perbarui timer istirahat: hitung mundur ke akhir 15 menit. */
        fun showBreak(context: Context, task: TaskEntity) {
            val start = task.breakStartedAt ?: System.currentTimeMillis()
            show(context, task.id, task.title, MODE_BREAK, task.deadline, start + AlarmScheduler.BREAK_MAX_MS)
        }

        /** Samakan notifikasi timer dengan tugas aktif saat ini (atau tidak ada). */
        fun sync(context: Context, active: TaskEntity?) {
            if (active == null) {
                stop(context)
                return
            }
            when (active.statusEnum) {
                TaskStatus.IN_PROGRESS -> showFocus(context, active)
                TaskStatus.ON_BREAK -> showBreak(context, active)
                else -> stop(context)
            }
        }

        /** Hilangkan timer dari notifikasi (tugas selesai / batal / dihapus). */
        fun stop(context: Context) {
            try { context.stopService(Intent(context, FocusTimerService::class.java)) } catch (e: Exception) { }
            try {
                context.getSystemService(NotificationManager::class.java)?.cancel(TIMER_NOTIFICATION_ID)
            } catch (e: Exception) { }
        }

        private fun show(context: Context, taskId: Long, title: String, mode: String, deadline: Long, whenMs: Long) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_SHOW
                putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
                putExtra(AlarmScheduler.EXTRA_TASK_TITLE, title)
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_DEADLINE, deadline)
                putExtra(EXTRA_WHEN, whenMs)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // Cadangan: pasang notifikasinya langsung tanpa layanan.
                // Chronometer-nya tetap berjalan karena dirender SystemUI.
                try {
                    context.getSystemService(NotificationManager::class.java)
                        ?.notify(TIMER_NOTIFICATION_ID, buildNotification(context, taskId, title, mode, deadline, whenMs))
                } catch (e2: Exception) { }
            }
        }

        fun buildNotification(
            context: Context, taskId: Long, title: String,
            mode: String, deadline: Long, whenMs: Long
        ): Notification {
            val openApp = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            }
            val pi = PendingIntent.getActivity(
                context, TIMER_NOTIFICATION_ID, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val isBreak = mode == MODE_BREAK
            val text = if (isBreak) {
                "Sedang istirahat · kembali bekerja sebelum " + DateTimeUtil.timeLabel(whenMs)
            } else {
                "Sedang dikerjakan · Deadline " + DateTimeUtil.timeLabel(deadline)
            }
            return NotificationCompat.Builder(context, NotificationHelper.CHANNEL_TIMER)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setUsesChronometer(true)
                .setWhen(whenMs)
                .setChronometerCountDown(isBreak)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pi)
                .build()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP_TIMER) {
            stopSelf()
            return START_NOT_STICKY
        }
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_TITLE) ?: "Tugas"
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_FOCUS
        val deadline = intent.getLongExtra(EXTRA_DEADLINE, 0L)
        val whenMs = intent.getLongExtra(EXTRA_WHEN, System.currentTimeMillis())
        val notification = buildNotification(this, taskId, title, mode, deadline, whenMs)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    TIMER_NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(TIMER_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            try { startForeground(TIMER_NOTIFICATION_ID, notification) } catch (e2: Exception) {
                try {
                    getSystemService(NotificationManager::class.java)
                        ?.notify(TIMER_NOTIFICATION_ID, notification)
                } catch (e3: Exception) { }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }
}
