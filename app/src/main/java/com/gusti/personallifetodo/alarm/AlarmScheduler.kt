package com.gusti.personallifetodo.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gusti.personallifetodo.data.TaskEntity
import com.gusti.personallifetodo.data.TaskStatus

/** Jenis alarm sesuai blueprint pasal 3. */
enum class AlarmType(val code: Int, val title: String) {
    // code tetap 1 seperti OVERDUE lama, supaya requestCode PendingIntent lama
    // tetap bisa dibatalkan oleh cancelAllForTask setelah update.
    START_TIME(1, "Waktunya Mulai Tugas!"),
    REMINDER_30(2, "Pengingat: 30 menit berlalu"),
    BREAK_END(3, "Istirahat selesai"),
    // Pengingat berulang tiap 30 menit untuk tugas yang BELUM dimulai dan
    // jam mulainya sudah lewat (terlambat). Kode baru 4: requestCode-nya unik
    // dan tidak menabrak alarm lama, dan cancelAllForTask otomatis ikut
    // membatalkannya karena membatalkan semua nilai enum.
    LATE_REMINDER(4, "Tugas Belum Dikerjakan!");

    companion object {
        /** Nama lama "OVERDUE" dari alarm yang terjadwal sebelum update tetap dikenali. */
        fun fromName(name: String?): AlarmType = when (name) {
            "OVERDUE" -> START_TIME
            else -> runCatching { valueOf(name ?: START_TIME.name) }.getOrDefault(START_TIME)
        }
    }
}

/**
 * Penjadwal alarm native dengan AlarmManager.setExactAndAllowWhileIdle.
 * Inilah bedanya dengan versi web: alarm tetap terpicu walau aplikasi
 * ditutup total dan layar terkunci (selama izin exact alarm diberikan).
 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val REMINDER_INTERVAL_MS = 30L * 60L * 1000L
        const val BREAK_MAX_MS = 15L * 60L * 1000L
    }

    private fun pendingIntent(taskId: Long, type: AlarmType, title: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_ALARM_TYPE, type.name)
            putExtra(EXTRA_TASK_TITLE, title)
        }
        // requestCode unik per tugas + jenis alarm (maks 4 alarm bersamaan per tugas)
        val requestCode = (taskId * 10 + type.code).toInt()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true

    private fun schedule(taskId: Long, type: AlarmType, title: String, triggerAt: Long) {
        val am = alarmManager ?: return
        val pi = pendingIntent(taskId, type, title)
        if (triggerAt <= System.currentTimeMillis()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // Fallback: alarm tidak eksak (tetap berbunyi, bisa bergeser oleh Doze)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(taskId: Long, type: AlarmType, title: String = "") {
        alarmManager?.cancel(pendingIntent(taskId, type, title))
    }

    fun cancelAllForTask(task: TaskEntity) {
        AlarmType.values().forEach { cancel(task.id, it, task.title) }
    }

    /** Jadwalkan ulang semua alarm yang relevan untuk satu tugas. Dipanggil
     *  setiap kali tugas dibuat / diubah / dimulai / istirahat / boot ulang. */
    fun rescheduleForTask(task: TaskEntity, now: Long = System.currentTimeMillis()) {
        cancelAllForTask(task)
        when (task.statusEnum) {
            TaskStatus.PENDING -> {
                // Alarm BERBUNYI tepat pada jam mulai tugas (bukan sekadar notifikasi):
                // AlarmReceiver meneruskannya ke AlarmRingService yang berdering looping.
                if (task.scheduledStart > now) {
                    schedule(task.id, AlarmType.START_TIME, task.title, task.scheduledStart)
                } else {
                    // PERBAIKAN keluhan Gusti: tugas sudah lewat jam mulainya tapi
                    // belum dikerjakan — versi lama menjadwalkan START_TIME di waktu
                    // yang sudah lewat, dan schedule() membuang waktu lampau, jadi
                    // TIDAK ADA alarm apa pun lagi (sesudah restart / dibuat sudah
                    // terlambat / alarm jam mulai sudah berbunyi sekali lalu berhenti).
                    // Sekarang: pengingat TERLAMBAT berdering lagi tiap 30 menit,
                    // dihitung dari jam mulai (mulai+30, +60, +90, ...), pada
                    // kelipatan berikutnya yang masih di masa depan. Rantai ini
                    // diperbarui AlarmReceiver setiap kali pengingat berbunyi,
                    // dan berhenti sendiri begitu tugas dimulai/selesai/dibatalkan
                    // (status berubah -> cabang lain / cancelAllForTask).
                    var next = task.scheduledStart + REMINDER_INTERVAL_MS
                    while (next <= now) next += REMINDER_INTERVAL_MS
                    schedule(task.id, AlarmType.LATE_REMINDER, task.title, next)
                }
            }
            TaskStatus.IN_PROGRESS -> {
                val cycleStart = task.reminderCycleStartedAt ?: task.startedAt ?: now
                var next = cycleStart + REMINDER_INTERVAL_MS
                while (next <= now) next += REMINDER_INTERVAL_MS
                schedule(task.id, AlarmType.REMINDER_30, task.title, next)
            }
            TaskStatus.ON_BREAK -> {
                val breakStart = task.breakStartedAt ?: now
                schedule(task.id, AlarmType.BREAK_END, task.title, breakStart + BREAK_MAX_MS)
            }
            TaskStatus.COMPLETED, TaskStatus.CANCELLED -> Unit // semua alarm dihentikan
        }
    }
}
