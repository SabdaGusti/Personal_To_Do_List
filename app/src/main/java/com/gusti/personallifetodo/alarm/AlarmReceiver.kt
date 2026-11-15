package com.gusti.personallifetodo.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.gusti.personallifetodo.PersonalLifeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Menerima alarm dari AlarmManager pada jam yang dijadwalkan, lalu menyuruh
 * AlarmRingService BERDERING (nada alarm looping + getar + layar full-screen),
 * bukan lagi hanya menampilkan notifikasi sekali bunyi — itu keluhan Gusti:
 * saat jam tugas tiba yang terlihat hanya notifikasi, alarmnya tidak berbunyi.
 *
 * Notifikasi lama hanya dipakai sebagai cadangan bila foreground service
 * ditolak sistem di HP tertentu.
 *
 * Aturan lanjutan sesuai blueprint:
 * - REMINDER_30: jadwalkan siklus 30 menit berikutnya selama tugas masih aktif.
 *   Timer fokus TIDAK dijeda oleh alarm ini (keputusan Gusti: tetap jalan).
 * - START_TIME: setelah berbunyi, jadwalkan pengingat berikutnya — tugas
 *   yang belum dimulai akan diingatkan lagi 30 menit kemudian lewat
 *   LATE_REMINDER (perbaikan keluhan Gusti: "30 menit terlambat mengerjakan
 *   tugas tidak muncul alarm lagi, harusnya ada").
 * - LATE_REMINDER: selama tugas masih PENDING, jadwalkan kelipatan
 *   30 menit berikutnya dari jam mulai. Berhenti sendiri begitu tugas
 *   dimulai / selesai / dibatalkan / dihapus.
 * - BREAK_END: tidak diulang otomatis; dering berhenti lewat tombol di
 *   AlarmActivity (Matikan Alarm / Kembali Bekerja).
 * - START_TIME / LATE_REMINDER saat tugas LAIN sedang dikerjakan: dering
 *   layar-penuh DITUNDA (bug laporan Gusti: alarm tugas berikutnya menutupi
 *   pekerjaan yang sedang berjalan). Cukup notifikasi info biasa, dan rantai
 *   pengingat 30 menit diteruskan — dering penuh kembali pada pengingat
 *   berikutnya bila sudah tidak ada tugas aktif. Alarm yang status tugasnya
 *   sudah tidak cocok (sudah dimulai/selesai/dihapus) tidak berdering sama
 *   sekali.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        val typeName = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE) ?: return
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_TITLE) ?: "Tugas"
        val type = AlarmType.fromName(typeName)
        if (taskId <= 0) return

        val app = context.applicationContext as? PersonalLifeApp
        if (app == null) {
            // Tanpa akses database, status tidak bisa diperiksa — bunyikan seperti biasa.
            if (!AlarmRingService.start(context, taskId, type, title)) {
                showAlarmNotification(context, taskId, type, title)
            }
            return
        }

        // Status tugas diperiksa DULU di database, baru diputuskan berdering
        // atau tidak (dulu dering langsung dibunyikan tanpa pemeriksaan).
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.repository.get(taskId)
                // Alarm basi: tugas sudah dihapus, atau statusnya sudah berubah
                // sejak alarm dijadwalkan (sudah dimulai manual / selesai /
                // dibatalkan). Jangan berdering untuk keadaan yang sudah lewat.
                if (task == null) return@launch
                val statusMatches = when (type) {
                    AlarmType.START_TIME, AlarmType.LATE_REMINDER ->
                        task.statusEnum == com.gusti.personallifetodo.data.TaskStatus.PENDING
                    AlarmType.REMINDER_30 ->
                        task.statusEnum == com.gusti.personallifetodo.data.TaskStatus.IN_PROGRESS
                    AlarmType.BREAK_END ->
                        task.statusEnum == com.gusti.personallifetodo.data.TaskStatus.ON_BREAK
                }
                if (!statusMatches) {
                    app.alarmScheduler.rescheduleForTask(task)
                    return@launch
                }

                // PERBAIKAN BUG (laporan Gusti): alarm jam mulai tugas
                // BERIKUTNYA muncul layar-penuh saat ia SEDANG mengerjakan
                // tugas lain — pekerjaan yang berjalan tertutup alarm yang
                // tombol "Mulai Tugas Sekarang"-nya pun terblokir aturan
                // One Task at a Time. Sekarang: selama ada tugas lain yang
                // aktif, alarm tugas berikutnya DITUNDA — cukup notifikasi
                // biasa (tanpa dering looping, tanpa layar penuh), dan rantai
                // pengingat tiap 30 menit tetap dijadwalkan. Begitu tugas
                // yang berjalan selesai, pengingat berikutnya berdering
                // penuh seperti biasa.
                val active = app.repository.getActive()
                if ((type == AlarmType.START_TIME || type == AlarmType.LATE_REMINDER) &&
                    active != null && active.id != taskId
                ) {
                    showDeferredNotification(context, task, active.title)
                    app.alarmScheduler.rescheduleForTask(task)
                    return@launch
                }

                // Jalur normal: bunyikan alarm sungguhan lewat layanan dering.
                val ringing = AlarmRingService.start(context, taskId, type, title)
                if (!ringing) {
                    // Cadangan terakhir: notifikasi prioritas tinggi + full-screen intent.
                    showAlarmNotification(context, taskId, type, title)
                }

                // Jadwalkan pengingat berikutnya sesuai status TERBARU tugas:
                // - START_TIME / LATE_REMINDER + masih PENDING -> pengingat
                //   terlambat 30 menit berikutnya.
                // - REMINDER_30 + masih IN_PROGRESS -> siklus 30 menit berikutnya.
                val shouldReschedule = when (type) {
                    AlarmType.START_TIME, AlarmType.LATE_REMINDER ->
                        task.statusEnum == com.gusti.personallifetodo.data.TaskStatus.PENDING
                    AlarmType.REMINDER_30 ->
                        task.statusEnum == com.gusti.personallifetodo.data.TaskStatus.IN_PROGRESS
                    AlarmType.BREAK_END -> false
                }
                if (shouldReschedule) app.alarmScheduler.rescheduleForTask(task)
            } finally { pending.finish() }
        }
    }

    /** Notifikasi TUNDA: jam mulai tugas berikutnya tiba saat tugas lain
     *  sedang dikerjakan. Sengaja di channel INFO (suara notifikasi biasa
     *  sekali, tanpa layar penuh) supaya fokus yang berjalan tidak terganggu.
     *  Tap notifikasi membuka aplikasi. */
    private fun showDeferredNotification(
        context: Context,
        task: com.gusti.personallifetodo.data.TaskEntity,
        activeTitle: String
    ) {
        val openApp = Intent(context, com.gusti.personallifetodo.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, AlarmRingService.notificationId(task.id, AlarmType.START_TIME), openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = "Jam mulai \"${task.title}\" sudah tiba, tapi kamu masih mengerjakan " +
            "\"$activeTitle\". Selesaikan dulu tugas itu — tugas ini akan diingatkan lagi tiap 30 menit."
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_INFO)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Tugas Berikutnya Sudah Waktunya")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            ?.notify(AlarmRingService.notificationId(task.id, AlarmType.START_TIME), notification)
    }

    /** Cadangan saja — jalur utama adalah AlarmRingService yang berdering looping. */
    private fun showAlarmNotification(context: Context, taskId: Long, type: AlarmType, title: String) {
        val fullScreen = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, type.name)
            putExtra(AlarmScheduler.EXTRA_TASK_TITLE, title)
        }
        val fullPi = PendingIntent.getActivity(
            context, AlarmRingService.notificationId(taskId, type), fullScreen,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = when (type) {
            AlarmType.START_TIME -> "Jam mulai tugas sudah tiba: $title"
            AlarmType.LATE_REMINDER -> "Tugas ini sudah lewat jam mulainya dan belum dikerjakan: $title. Mulai sekarang, yuk."
            AlarmType.REMINDER_30 -> "Sudah 30 menit mengerjakan: $title. Tetap fokus, atau tandai selesai."
            AlarmType.BREAK_END -> "15 menit istirahat habis. Tekan Kembali Bekerja untuk lanjut: $title"
        }
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(type.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullPi, true)
            .setContentIntent(fullPi)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.notify(AlarmRingService.notificationId(taskId, type), notification)
    }
}
