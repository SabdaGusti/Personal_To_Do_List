package com.gusti.personallifetodo

import android.app.Application
import androidx.room.Room
import com.gusti.personallifetodo.alarm.AlarmScheduler
import com.gusti.personallifetodo.alarm.NotificationHelper
import com.gusti.personallifetodo.data.AppDatabase
import com.gusti.personallifetodo.data.TaskRepository

class PersonalLifeApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: TaskRepository
        private set
    lateinit var alarmScheduler: AlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "personal-life-todo.db").build()
        repository = TaskRepository(database.taskDao())
        alarmScheduler = AlarmScheduler(this)
        NotificationHelper.createChannels(this)
    }
}
