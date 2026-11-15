package com.gusti.personallifetodo.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Layanan dering alarm — inilah perbaikan atas keluhan Gusti:
 * "alarm saat tugas sudah masuk jamnya harus BERBUNYI, bukan hanya notifikasi".
 *
 * Versi lama hanya memasang suara pada notification channel. Suara channel
 * hanya diputar SEKALI (bunyi "ting" singkat) lalu berhenti, dan di banyak HP
 * layar alarmnya bahkan tidak muncul — yang terlihat hanya notifikasi biasa.
 *
 * Layanan foreground ini yang benar-benar membunyikan alarm seperti jam weker:
 * - Memutar nada alarm bawaan HP secara BERULANG (looping) lewat MediaPlayer
 *   dengan AudioAttributes USAGE_ALARM, sampai pengguna menekan tombol
 *   "Matikan Alarm" / "Mulai Tugas Sekarang" / "Kembali Bekerja" di layar
 *   alarm, atau tombol "Matikan Alarm" pada notifikasinya.
 * - Getar berulang selama alarm berbunyi.
 * - Menampilkan notifikasi full-screen + membuka AlarmActivity di atas
 *   lock screen (layar menyala otomatis).
 * - WakeLock parsial supaya dering tidak terhenti oleh Doze.
 * - Berhenti otomatis setelah 5 menit bila tidak ada yang menekan apa pun,
 *   supaya HP tidak berbunyi tanpa henti selamanya.
 *
 * Dimulai dari AlarmReceiver yang terpicu exact alarm — kategori yang
 * diizinkan Android untuk memulai foreground service dari background.
 */
class AlarmRingService : Service() {

    companion object {
        const val ACTION_RING = "com.gusti.personallifetodo.action.RING"
        const val ACTION_STOP = "com.gusti.personallifetodo.action.STOP_RING"

        private const val AUTO_STOP_MS = 5L * 60L * 1000L

        fun notificationId(taskId: Long, type: AlarmType): Int =
            (taskId * 10 + type.code).toInt()

        /** Mulai membunyikan alarm. false bila foreground service gagal dimulai
         *  (pemanggil lalu jatuh ke notifikasi biasa sebagai cadangan). */
        fun start(context: Context, taskId: Long, type: AlarmType, title: String): Boolean {
            val intent = Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_RING
                putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
                putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, type.name)
                putExtra(AlarmScheduler.EXTRA_TASK_TITLE, title)
            }
            return try {
                ContextCompat.startForegroundService(context, intent)
                true
            } catch (e: Exception) {
                false
            }
        }

        /** Hentikan dering dari mana pun (layar alarm / aksi notifikasi). */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, AlarmRingService::class.java))
            } catch (e: Exception) { /* layanan memang belum jalan */ }
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopSelf() }
    private var currentKey: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        val type = AlarmType.fromName(intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE))
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_TITLE) ?: "Tugas"
        if (taskId <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification(taskId, type, title)
        val notifId = notificationId(taskId, type)
        // startForeground WAJIB dipanggil segera setelah service dimulai.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(notifId, notification)
            }
        } catch (e: Exception) {
            try { startForeground(notifId, notification) } catch (e2: Exception) { /* lanjut: tetap coba berdering */ }
        }

        val key = "$taskId:${type.name}"
        if (currentKey == key && mediaPlayer?.isPlaying == true) {
            // Alarm yang sama sudah berdering (mis. AlarmActivity dibuka ulang) — jangan putar ulang dari awal.
            return START_NOT_STICKY
        }
        currentKey = key
        startRinging()
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, AUTO_STOP_MS)

        // Buka layar alarm langsung (full-screen intent pada notifikasi adalah
        // cadangan resmi bila startActivity dari background ditolak sistem).
        try {
            startActivity(Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
                putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, type.name)
                putExtra(AlarmScheduler.EXTRA_TASK_TITLE, title)
            })
        } catch (e: Exception) { /* full-screen intent jadi cadangan */ }

        return START_NOT_STICKY
    }

    private fun buildNotification(taskId: Long, type: AlarmType, title: String): Notification {
        val fullScreen = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, type.name)
            putExtra(AlarmScheduler.EXTRA_TASK_TITLE, title)
        }
        val fullPi = PendingIntent.getActivity(
            this, notificationId(taskId, type), fullScreen,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, notificationId(taskId, type) + 1000,
            Intent(this, AlarmRingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = when (type) {
            AlarmType.START_TIME -> "Jam mulai tugas sudah tiba: $title. Alarm berbunyi sampai dimatikan."
            AlarmType.LATE_REMINDER -> "Tugas ini sudah lewat jam mulainya dan belum dikerjakan: $title. Alarm akan berbunyi lagi tiap 30 menit sampai tugas dimulai."
            AlarmType.REMINDER_30 -> "Sudah 30 menit mengerjakan: $title. Tetap fokus, atau tandai selesai."
            AlarmType.BREAK_END -> "15 menit istirahat habis. Tekan Kembali Bekerja untuk lanjut: $title"
        }
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ALARM_RING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(type.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullPi, true)
            .setContentIntent(fullPi)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan Alarm", stopPi)
            .build()
    }

    private fun startRinging() {
        stopRingingOnly()

        // WakeLock: cegah CPU tidur selama alarm berbunyi (lepas otomatis oleh timeout).
        try {
            wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PersonalLifeTodo:AlarmRing")
                .apply {
                    setReferenceCounted(false)
                    acquire(AUTO_STOP_MS + 60_000L)
                }
        } catch (e: Exception) { /* tanpa wakelock pun dering tetap dicoba */ }

        // Getar berulang: 0,7 dtk getar / 0,4 dtk jeda, diulang dari awal pola.
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 700, 400, 700, 400, 700)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) { /* getar gagal tidak boleh menggagalkan suara */ }

        // Suara alarm BERULANG (looping) — inti perbaikan ini.
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (alarmUri == null || !tryPlay(alarmUri)) {
            // Cadangan terakhir: nada notifikasi bawaan.
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let { tryPlay(it) }
        }
    }

    private fun tryPlay(uri: Uri): Boolean {
        return try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setVolume(1f, 1f)
                prepare()
                start()
            }
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            try { mediaPlayer?.release() } catch (e2: Exception) { }
            mediaPlayer = null
            false
        }
    }

    private fun stopRingingOnly() {
        try { mediaPlayer?.stop() } catch (e: Exception) { }
        try { mediaPlayer?.release() } catch (e: Exception) { }
        mediaPlayer = null
        try { vibrator?.cancel() } catch (e: Exception) { }
        vibrator = null
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (e: Exception) { }
        wakeLock = null
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        stopRingingOnly()
        currentKey = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }
}
