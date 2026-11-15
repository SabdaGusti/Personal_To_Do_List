package com.gusti.personallifetodo.util

import com.gusti.personallifetodo.data.RecurrenceType
import com.gusti.personallifetodo.data.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

object DateTimeUtil {
    private val dayFmt = SimpleDateFormat("EEE, d MMM", Locale("id", "ID"))
    private val longFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
    private val timeFmt = SimpleDateFormat("HH.mm", Locale("id", "ID"))
    private val fullFmt = SimpleDateFormat("EEE, d MMM · HH.mm", Locale("id", "ID"))
    private val monthFmt = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    fun dayLabel(ms: Long) = dayFmt.format(ms)
    /** Label bulan untuk header kalender menu Tugas: "November 2026". */
    fun monthLabel(ms: Long) = monthFmt.format(ms)
    /** Format tanggal panjang versi web: "Jumat, 6 November 2026". */
    fun longLabel(ms: Long) = longFmt.format(ms)
    fun timeLabel(ms: Long) = timeFmt.format(ms)
    fun fullLabel(ms: Long) = fullFmt.format(ms)
    fun durationLabel(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return when {
            h > 0 -> "%d:%02d:%02d".format(h, m, sec)
            else -> "%02d:%02d".format(m, sec)
        }
    }
    /** Sama persis fmtDur versi web: "45 dtk", "5 mnt", "1 j 5 mnt". */
    fun durationWords(ms: Long): String {
        if (ms < 60_000) return "${ms / 1000} dtk"
        val totalMin = ms / 60000
        val h = totalMin / 60; val m = totalMin % 60
        return if (h > 0) "$h j $m mnt" else "$m mnt"
    }
    fun dayStart(ms: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
    /** Istirahat berjalan dihitung live, sama seperti liveFocusMs (versi web: liveBreakMs). */
    fun liveBreakMs(task: TaskEntity, now: Long): Long =
        task.breakAccumulatedMs + (task.breakStartedAt?.let { (now - it).coerceAtLeast(0) } ?: 0)
    fun sameDay(a: Long, b: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = a }
        val c2 = Calendar.getInstance().apply { timeInMillis = b }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}

/**
 * Perulangan (keputusan versi web, dipertahankan di native):
 * setiap tanggal menjadi SATU tugas terpisah. Tanpa tanggal berakhir:
 * harian 28 jadwal, mingguan 12, bulanan 6. Deadline ikut bergeser per tanggal.
 */
object RecurrenceGenerator {
    const val DEFAULT_DAILY = 28
    const val DEFAULT_WEEKLY = 12
    const val DEFAULT_MONTHLY = 6

    fun count(type: RecurrenceType, start: Long, endDate: Long?): Int {
        if (type == RecurrenceType.NONE) return 1
        if (endDate == null) return when (type) {
            RecurrenceType.DAILY -> DEFAULT_DAILY
            RecurrenceType.WEEKLY -> DEFAULT_WEEKLY
            RecurrenceType.MONTHLY -> DEFAULT_MONTHLY
            RecurrenceType.NONE -> 1
        }
        var n = 0; var t = start
        while (t <= endDate && n < 366) { n++; t = shift(t, type, 1) }
        return n.coerceAtLeast(1)
    }

    fun shift(ms: Long, type: RecurrenceType, steps: Int): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        when (type) {
            RecurrenceType.DAILY -> c.add(Calendar.DAY_OF_YEAR, steps)
            RecurrenceType.WEEKLY -> c.add(Calendar.WEEK_OF_YEAR, steps)
            RecurrenceType.MONTHLY -> c.add(Calendar.MONTH, steps)
            RecurrenceType.NONE -> Unit
        }
        return c.timeInMillis
    }

    /** Membuat seluruh tugas untuk satu form Tambah Tugas. */
    fun expand(
        title: String, description: String,
        start: Long, deadline: Long,
        type: RecurrenceType, endDate: Long?
    ): List<TaskEntity> {
        if (type == RecurrenceType.NONE) {
            return listOf(TaskEntity(title = title, description = description,
                scheduledStart = start, deadline = deadline))
        }
        val groupId = UUID.randomUUID().toString()
        val duration = deadline - start
        val n = count(type, start, endDate)
        return (0 until n).map { i ->
            val s = shift(start, type, i)
            TaskEntity(
                title = title, description = description,
                scheduledStart = s, deadline = s + duration,
                recurrenceType = type.name, recurrenceGroupId = groupId
            )
        }
    }
}
