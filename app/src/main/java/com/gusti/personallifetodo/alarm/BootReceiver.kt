package com.gusti.personallifetodo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gusti.personallifetodo.PersonalLifeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Setelah HP restart / aplikasi diperbarui, semua AlarmManager terhapus.
 *  Receiver ini menjadwalkan ulang alarm untuk semua tugas yang masih relevan. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as? PersonalLifeApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.getSchedulable().forEach { app.alarmScheduler.rescheduleForTask(it) }
                // Notifikasi timer ikut hilang saat HP restart — pulihkan
                // untuk tugas yang sedang dikerjakan / istirahat.
                FocusTimerService.sync(context, app.repository.getActive())
            } finally { pending.finish() }
        }
    }
}
