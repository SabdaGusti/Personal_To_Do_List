package com.gusti.personallifetodo.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Status tugas sesuai blueprint. ON_BREAK tetap dihitung satu tugas aktif. */
enum class TaskStatus { PENDING, IN_PROGRESS, ON_BREAK, COMPLETED, CANCELLED }

enum class RecurrenceType(val label: String, val badge: String?) {
    NONE("Tidak berulang", null),
    DAILY("Setiap hari", "Berulang Harian"),
    WEEKLY("Setiap minggu", "Berulang Mingguan"),
    MONTHLY("Setiap bulan", "Berulang Bulanan")
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    /** Epoch millis: jadwal mulai. */
    val scheduledStart: Long,
    /** Epoch millis: deadline / waktu selesai yang ditargetkan. */
    val deadline: Long,
    val status: String = TaskStatus.PENDING.name,
    val recurrenceType: String = RecurrenceType.NONE.name,
    /** Semua tugas hasil satu seri perulangan berbagi group id ini. */
    val recurrenceGroupId: String? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    /** True bila tugas dimulai setelah jadwalnya (terlambat), dicatat untuk riwayat. */
    val startedLate: Boolean = false,
    /** Fokus terakumulasi dari segmen-segmen selesai (ms). Segmen berjalan dihitung dari focusSegmentStartedAt. */
    val focusAccumulatedMs: Long = 0,
    val focusSegmentStartedAt: Long? = null,
    val breakAccumulatedMs: Long = 0,
    val breakStartedAt: Long? = null,
    val reminderCycleStartedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val statusEnum: TaskStatus get() = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.PENDING)
    val recurrenceEnum: RecurrenceType get() = runCatching { RecurrenceType.valueOf(recurrenceType) }.getOrDefault(RecurrenceType.NONE)
    val isActive: Boolean get() = statusEnum == TaskStatus.IN_PROGRESS || statusEnum == TaskStatus.ON_BREAK
    /** Terlambat: belum dimulai dan jadwal sudah lewat, ATAU aktif tapi deadline lewat
     *  (keputusan Gusti: tugas aktif tetap biru, hanya ditambah badge merah "Terlambat"). */
    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean = when (statusEnum) {
        TaskStatus.PENDING -> now >= scheduledStart
        TaskStatus.IN_PROGRESS, TaskStatus.ON_BREAK -> now >= deadline
        else -> false
    }
    fun liveFocusMs(now: Long = System.currentTimeMillis()): Long =
        focusAccumulatedMs + (focusSegmentStartedAt?.let { (now - it).coerceAtLeast(0) } ?: 0)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY scheduledStart ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status IN ('IN_PROGRESS','ON_BREAK') LIMIT 1")
    suspend fun getActive(): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status IN ('PENDING','IN_PROGRESS','ON_BREAK')")
    suspend fun getSchedulable(): List<TaskEntity>

    @Insert suspend fun insert(task: TaskEntity): Long
    @Insert suspend fun insertAll(tasks: List<TaskEntity>): List<Long>
    @Update suspend fun update(task: TaskEntity)
    @Delete suspend fun delete(task: TaskEntity)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteById(id: Long)
}

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}

class TaskRepository(private val dao: TaskDao) {
    val tasks: Flow<List<TaskEntity>> = dao.observeAll()
    suspend fun get(id: Long) = dao.getById(id)
    suspend fun getActive() = dao.getActive()
    suspend fun getSchedulable() = dao.getSchedulable()
    suspend fun insert(task: TaskEntity) = dao.insert(task)
    suspend fun insertAll(tasks: List<TaskEntity>) = dao.insertAll(tasks)
    suspend fun update(task: TaskEntity) = dao.update(task)
    suspend fun delete(task: TaskEntity) = dao.delete(task)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
