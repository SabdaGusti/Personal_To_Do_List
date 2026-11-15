package com.gusti.personallifetodo.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusti.personallifetodo.PersonalLifeApp
import com.gusti.personallifetodo.data.TaskStatus
import com.gusti.personallifetodo.ui.AppViewModel
import com.gusti.personallifetodo.ui.theme.PersonalLifeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Layar alarm full-screen. Muncul di atas lock screen (showOnLockScreen +
 * turnScreenOn di Manifest & window flags di bawah).
 *
 * Deringnya diputar oleh AlarmRingService (nada alarm looping + getar),
 * bukan oleh notifikasi. Semua tombol di layar ini menghentikan dering itu.
 *
 * Aksi sesuai blueprint:
 * - START_TIME (jam mulai tugas tiba) -> tombol "Mulai Tugas Sekarang":
 *   menghentikan dering DAN langsung memulai tugas; tombol "Matikan Alarm"
 *   hanya menghentikan dering, tugas tetap belum dimulai.
 * - LATE_REMINDER (tugas sudah lewat jam mulai, belum dikerjakan) -> sama
 *   seperti START_TIME: "Mulai Tugas Sekarang" / "Matikan Alarm". Mematikan
 *   alarm hanya menghentikan dering kali ini; pengingat berikutnya tetap
 *   terjadwal tiap 30 menit sampai tugas benar-benar dimulai.
 * - REMINDER_30 -> tombol "Matikan Alarm": hanya menghentikan dering kejadian
 *   ini. Siklus berikutnya sudah dijadwalkan oleh AlarmReceiver, dan timer
 *   fokus TIDAK dijeda (keputusan Gusti: tetap jalan).
 * - BREAK_END -> tombol "Kembali Bekerja": mengakhiri dering DAN melanjutkan
 *   pengerjaan (keputusan Gusti: ya).
 */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_TITLE) ?: "Tugas"
        // Nama lama "OVERDUE" dari alarm pra-update dipetakan ke START_TIME.
        val type = AlarmType.fromName(intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE))
        val app = application as PersonalLifeApp
        val viewModel = AppViewModel(app.repository, app.alarmScheduler, app)

        setContent {
            PersonalLifeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmScreen(
                        type = type, taskTitle = title,
                        onOff = {
                            stopRinging(taskId, type)
                            finish()
                        },
                        onStartTask = {
                            CoroutineScope(Dispatchers.Main).launch {
                                val task = app.repository.get(taskId)
                                val active = app.repository.getActive()
                                if (task != null && active != null && active.id != task.id) {
                                    // One Task at a Time (lapis pengaman bila layar
                                    // alarm ini tetap terbuka saat tugas lain sudah
                                    // berjalan): dulu tombol ini diam saja terblokir
                                    // tapi tetap membuka halaman tugas yang tidak
                                    // bisa dimulai. Sekarang beri tahu tugas mana
                                    // yang harus diselesaikan dulu, lalu ke Beranda.
                                    stopRinging(taskId, type)
                                    android.widget.Toast.makeText(
                                        this@AlarmActivity,
                                        "Selesaikan dulu tugas yang sedang berjalan: \"${active.title}\"",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                    startActivity(
                                        Intent(this@AlarmActivity, com.gusti.personallifetodo.MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        }
                                    )
                                    finish()
                                    return@launch
                                }
                                if (task != null) viewModel.startTask(task)
                                stopRinging(taskId, type)
                                startActivity(
                                    Intent(this@AlarmActivity, com.gusti.personallifetodo.MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
                                    }
                                )
                                finish()
                            }
                        },
                        onBackToWork = {
                            CoroutineScope(Dispatchers.Main).launch {
                                viewModel.resumeFromBreak(taskId)
                                stopRinging(taskId, type)
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    /** Matikan dering looping (AlarmRingService) + hapus notifikasinya. */
    private fun stopRinging(taskId: Long, type: AlarmType) {
        AlarmRingService.stop(this)
        getSystemService(NotificationManager::class.java)
            ?.cancel(AlarmRingService.notificationId(taskId, type))
    }
}

@Composable
private fun AlarmScreen(
    type: AlarmType, taskTitle: String,
    onOff: () -> Unit, onStartTask: () -> Unit, onBackToWork: () -> Unit
) {
    val color = when (type) {
        AlarmType.START_TIME -> Color(0xFFDC2626)
        AlarmType.LATE_REMINDER -> Color(0xFFDC2626)
        AlarmType.REMINDER_30 -> Color(0xFF2563EB)
        AlarmType.BREAK_END -> Color(0xFFB45309)
    }
    val body = when (type) {
        AlarmType.START_TIME -> "Jam mulai tugas sudah tiba. Alarm terus berbunyi sampai dimatikan atau tugas dimulai."
        AlarmType.LATE_REMINDER -> "Tugas ini sudah lewat jam mulainya dan belum dikerjakan. Alarm akan berbunyi lagi tiap 30 menit sampai tugas dimulai."
        AlarmType.REMINDER_30 -> "Sudah 30 menit berlalu. Timer fokus tetap berjalan."
        AlarmType.BREAK_END -> "Waktu istirahat 15 menit sudah habis."
    }
    val tint = when (type) {
        AlarmType.START_TIME -> Color(0xFFFDECEC)
        AlarmType.LATE_REMINDER -> Color(0xFFFDECEC)
        AlarmType.REMINDER_30 -> Color(0xFFE3ECFD)
        AlarmType.BREAK_END -> Color(0xFFFDF3E0)
    }
    Column(
        modifier = Modifier.fillMaxSize().background(tint).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⏰", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(type.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = color, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(taskTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        if (type == AlarmType.BREAK_END) {
            Button(
                onClick = onBackToWork,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) { Text("Kembali Bekerja", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onOff, modifier = Modifier.fillMaxWidth()) {
                Text("Matikan Alarm")
            }
        } else if (type == AlarmType.START_TIME || type == AlarmType.LATE_REMINDER) {
            Button(
                onClick = onStartTask,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) { Text("Mulai Tugas Sekarang", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onOff, modifier = Modifier.fillMaxWidth()) {
                Text("Matikan Alarm")
            }
        } else {
            Button(
                onClick = onOff,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) { Text("Matikan Alarm", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onOff, modifier = Modifier.fillMaxWidth()) {
                Text("Tutup")
            }
        }
    }
}
