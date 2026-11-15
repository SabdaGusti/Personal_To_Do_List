package com.gusti.personallifetodo.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object NotificationHelper {
    /** Channel alarm lama (suara channel, hanya cadangan). Sifat channel tidak
     *  bisa diubah setelah dibuat di HP, jadi dering baru memakai channel baru. */
    const val CHANNEL_ALARM = "alarm_channel"
    /** Channel dering alarm: TANPA suara channel (suara diputar looping oleh
     *  AlarmRingService), prioritas tertinggi, tampil di lock screen. */
    const val CHANNEL_ALARM_RING = "alarm_ring_channel_v2"
    const val CHANNEL_INFO = "info_channel"
    /** Channel timer berjalan: notifikasi menetap berisi timer fokus /
     *  istirahat (FocusTimerService). Prioritas rendah, tanpa suara & getar. */
    const val CHANNEL_TIMER = "timer_channel"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarm = NotificationChannel(CHANNEL_ALARM, "Alarm Tugas", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarm keterlambatan, pengingat 30 menit, dan akhir istirahat"
            setSound(alarmSound, attrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setBypassDnd(false)
        }
        nm.createNotificationChannel(alarm)
        val ring = NotificationChannel(CHANNEL_ALARM_RING, "Alarm Berbunyi", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarm yang benar-benar berbunyi (dering berulang) saat jam tugas tiba, pengingat 30 menit, dan akhir istirahat"
            // Sengaja tanpa suara & getar di channel: suara looping + getar
            // dikendalikan AlarmRingService supaya bisa berbunyi terus sampai dimatikan.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ring)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_INFO, "Info Tugas", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifikasi tugas biasa"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, "Timer Tugas Berjalan", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Timer fokus / istirahat yang menetap selama tugas dikerjakan"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
    }
}
