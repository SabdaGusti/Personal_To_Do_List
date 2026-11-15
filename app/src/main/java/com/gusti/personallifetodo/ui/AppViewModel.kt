package com.gusti.personallifetodo.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gusti.personallifetodo.alarm.AlarmScheduler
import com.gusti.personallifetodo.alarm.FocusTimerService
import com.gusti.personallifetodo.data.RecurrenceType
import com.gusti.personallifetodo.data.TaskEntity
import com.gusti.personallifetodo.data.TaskRepository
import com.gusti.personallifetodo.data.TaskStatus
import com.gusti.personallifetodo.util.RecurrenceGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler,
    /** Konteks aplikasi — dipakai menyalakan/mematikan timer di notifikasi
     *  (FocusTimerService) mengikuti status tugas aktif. */
    private val appContext: Context
) : ViewModel() {

    /** Pulihkan notifikasi timer sesuai tugas aktif (dipanggil saat aplikasi
     *  dibuka — mis. setelah HP restart notifikasi timer ikut hilang). */
    fun syncTimerNotification() {
        viewModelScope.launch { FocusTimerService.sync(appContext, repository.getActive()) }
    }

    val tasks: StateFlow<List<TaskEntity>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Membuat tugas (dengan perulangan -> banyak tugas terpisah) + jadwalkan alarm keterlambatan. */
    fun createTasks(title: String, description: String, start: Long, deadline: Long,
                    recurrence: RecurrenceType, endDate: Long?) {
        viewModelScope.launch {
            val created = RecurrenceGenerator.expand(title, description, start, deadline, recurrence, endDate)
            val ids = repository.insertAll(created)
            created.zip(ids).forEach { (t, id) -> alarmScheduler.rescheduleForTask(t.copy(id = id)) }
        }
    }

    fun updateTask(task: TaskEntity, title: String, description: String, start: Long, deadline: Long) {
        viewModelScope.launch {
            val updated = task.copy(title = title, description = description,
                scheduledStart = start, deadline = deadline)
            repository.update(updated)
            alarmScheduler.rescheduleForTask(updated)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelAllForTask(task)
            if (task.isActive) FocusTimerService.stop(appContext)
            repository.delete(task)
        }
    }

    /** One Task at a Time: gagal (false lewat callback) bila sudah ada tugas aktif lain. */
    fun startTask(task: TaskEntity, onBlocked: () -> Unit = {}) {
        viewModelScope.launch {
            val active = repository.getActive()
            if (active != null && active.id != task.id) { onBlocked(); return@launch }
            val now = System.currentTimeMillis()
            val started = task.copy(
                status = TaskStatus.IN_PROGRESS.name,
                startedAt = now,
                startedLate = task.scheduledStart <= now,
                focusSegmentStartedAt = now,
                reminderCycleStartedAt = now
            )
            repository.update(started)
            alarmScheduler.rescheduleForTask(started)
            // Timer mulai muncul di notifikasi sejak tugas dikerjakan.
            FocusTimerService.showFocus(appContext, started)
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val focus = task.liveFocusMs(now)
            val done = task.copy(
                status = TaskStatus.COMPLETED.name, completedAt = now,
                focusAccumulatedMs = focus, focusSegmentStartedAt = null,
                breakStartedAt = null
            )
            repository.update(done)
            alarmScheduler.cancelAllForTask(done)
            FocusTimerService.stop(appContext)
        }
    }

    /** Batalkan untuk tugas aktif (keputusan Gusti: tombol Batalkan ditambahkan). */
    fun cancelTask(task: TaskEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cancelled = task.copy(
                status = TaskStatus.CANCELLED.name, completedAt = now,
                focusAccumulatedMs = task.liveFocusMs(now), focusSegmentStartedAt = null,
                breakStartedAt = null
            )
            repository.update(cancelled)
            alarmScheduler.cancelAllForTask(cancelled)
            FocusTimerService.stop(appContext)
        }
    }

    /** Istirahat maks 15 menit: segmen fokus ditutup, alarm akhir istirahat dijadwalkan.
     *  Siklus pengingat 30 menit dijeda selama istirahat (blueprint pasal 4). */
    fun startBreak(task: TaskEntity) {
        viewModelScope.launch {
            if (task.statusEnum != TaskStatus.IN_PROGRESS) return@launch
            val now = System.currentTimeMillis()
            val onBreak = task.copy(
                status = TaskStatus.ON_BREAK.name,
                focusAccumulatedMs = task.liveFocusMs(now), focusSegmentStartedAt = null,
                breakStartedAt = now
            )
            repository.update(onBreak)
            alarmScheduler.rescheduleForTask(onBreak)
            // Notifikasi timer beralih ke hitung mundur sisa istirahat.
            FocusTimerService.showBreak(appContext, onBreak)
        }
    }

    /** Kembali Bekerja: dipakai tombol di halaman Fokus DAN tombol di layar alarm istirahat. */
    suspend fun resumeFromBreak(taskId: Long) {
        val task = repository.get(taskId) ?: return
        resumeFromBreakInternal(task)
    }

    fun resumeFromBreak(task: TaskEntity) {
        viewModelScope.launch { resumeFromBreakInternal(task) }
    }

    private suspend fun resumeFromBreakInternal(task: TaskEntity) {
        if (task.statusEnum != TaskStatus.ON_BREAK) return
        val now = System.currentTimeMillis()
        val breakMs = task.breakStartedAt?.let { (now - it).coerceAtLeast(0) } ?: 0
        val resumed = task.copy(
            status = TaskStatus.IN_PROGRESS.name,
            breakAccumulatedMs = task.breakAccumulatedMs + breakMs,
            breakStartedAt = null,
            focusSegmentStartedAt = now,
            // Siklus pengingat 30 menit dimulai lagi dari momen kembali bekerja.
            reminderCycleStartedAt = now
        )
        repository.update(resumed)
        alarmScheduler.rescheduleForTask(resumed)
        // Kembali bekerja: notifikasi timer kembali ke timer fokus.
        FocusTimerService.showFocus(appContext, resumed)
    }
}
