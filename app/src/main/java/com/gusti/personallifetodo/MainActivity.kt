package com.gusti.personallifetodo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gusti.personallifetodo.alarm.AlarmScheduler
import com.gusti.personallifetodo.ui.AddEditTaskScreen
import com.gusti.personallifetodo.ui.AppViewModel
import com.gusti.personallifetodo.ui.DashboardScreen
import com.gusti.personallifetodo.ui.FocusScreen
import com.gusti.personallifetodo.ui.ProfileScreen
import com.gusti.personallifetodo.ui.StatsScreen
import com.gusti.personallifetodo.ui.TaskListScreen
import com.gusti.personallifetodo.ui.theme.LocalWebPalette
import com.gusti.personallifetodo.ui.theme.PersonalLifeTheme
import com.gusti.personallifetodo.ui.theme.ThemeChoice

sealed interface Screen {
    data object Dashboard : Screen
    data object TaskList : Screen
    data object Add : Screen
    data class Edit(val taskId: Long) : Screen
    data class Focus(val taskId: Long) : Screen
    data object Stats : Screen
    data object Profile : Screen
}

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AppViewModel
    private lateinit var scheduler: AlarmScheduler

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ditolak pun aplikasi tetap jalan; alarm tampil lewat full-screen intent */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PersonalLifeApp
        viewModel = AppViewModel(app.repository, app.alarmScheduler, app)
        scheduler = app.alarmScheduler
        // Pulihkan timer di notifikasi bila ada tugas yang sedang berjalan
        // (mis. notifikasinya hilang karena HP restart).
        viewModel.syncTimerNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val focusFromIntent = intent?.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L) ?: -1L

        setContent {
            val prefs = remember { getSharedPreferences("settings", MODE_PRIVATE) }
            var themeChoice by remember {
                mutableStateOf(
                    runCatching {
                        ThemeChoice.valueOf(prefs.getString("theme", ThemeChoice.SYSTEM.name)!!)
                    }.getOrDefault(ThemeChoice.SYSTEM)
                )
            }
            var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
            PersonalLifeTheme(themeChoice) {
                AppShell(
                    viewModel = viewModel,
                    initialFocusId = focusFromIntent,
                    themeChoice = themeChoice,
                    onThemeChange = { choice ->
                        themeChoice = choice
                        prefs.edit().putString("theme", choice.name).apply()
                    },
                    userName = userName,
                    onSaveName = { name ->
                        userName = name
                        prefs.edit().putString("user_name", name).apply()
                    },
                    exactAlarmGranted = scheduler.canScheduleExact(),
                    onRequestExactAlarm = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:$packageName")))
                        }
                    },
                    onRequestBatteryExemption = {
                        val pm = getSystemService(PowerManager::class.java)
                        if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:$packageName")))
                        } else {
                            Toast.makeText(this, "Aplikasi sudah diabaikan dari optimasi baterai", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBlocked = {
                        Toast.makeText(this,
                            "Selesaikan dulu tugas yang sedang berjalan (One Task at a Time)",
                            Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

/* Navigasi bawah PERSIS web: bar putih (surface) dengan garis atas 1.dp,
 * item ikon 24.dp + label 12.sp Bold (aktif = aksen, lain = dim), dan di
 * tengah tombol + bundar 56.dp berwarna aksen yang MENONJOL ke atas
 * (di web: -mt-6, bayangan besar) dengan label "Tambah" di bawahnya.
 * Versi lama memakai NavigationBar Material 3 (ada pill indikator aktif)
 * sehingga tampilannya menyimpang dari web. */
@Composable
private fun WebNavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalWebPalette.current
    val tint = if (selected) p.accent else p.dim
    Column(
        modifier.clickable { onClick() }.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

@Composable
private fun WebBottomBar(screen: Screen, onNavigate: (Screen) -> Unit) {
    val p = LocalWebPalette.current
    Column(Modifier.fillMaxWidth().background(p.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.border))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.Top) {
            WebNavItem("Beranda", Icons.Default.Home, screen is Screen.Dashboard,
                { onNavigate(Screen.Dashboard) }, Modifier.weight(1f))
            WebNavItem("Tugas", Icons.Default.List, screen is Screen.TaskList,
                { onNavigate(Screen.TaskList) }, Modifier.weight(1f))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.offset(y = (-20).dp).shadow(10.dp, CircleShape).size(56.dp)
                        .clip(CircleShape).background(p.accent)
                        .clickable { onNavigate(Screen.Add) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Add, "Tambah tugas baru", tint = p.accentInk, modifier = Modifier.size(28.dp)) }
                Text("Tambah", Modifier.offset(y = (-16).dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.dim)
            }
            WebNavItem("Statistik", Icons.Default.BarChart, screen is Screen.Stats,
                { onNavigate(Screen.Stats) }, Modifier.weight(1f))
            WebNavItem("Profil", Icons.Default.Person, screen is Screen.Profile,
                { onNavigate(Screen.Profile) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppShell(
    viewModel: AppViewModel,
    initialFocusId: Long,
    themeChoice: ThemeChoice,
    onThemeChange: (ThemeChoice) -> Unit,
    userName: String,
    onSaveName: (String) -> Unit,
    exactAlarmGranted: Boolean,
    onRequestExactAlarm: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onBlocked: () -> Unit
) {
    val p = LocalWebPalette.current
    val tasks by viewModel.tasks.collectAsState()
    var screen by remember {
        mutableStateOf<Screen>(if (initialFocusId > 0) Screen.Focus(initialFocusId) else Screen.Dashboard)
    }
    val showBottomBar = screen is Screen.Dashboard || screen is Screen.TaskList ||
        screen is Screen.Stats || screen is Screen.Profile

    Scaffold(
        containerColor = p.bg,
        bottomBar = { if (showBottomBar) WebBottomBar(screen) { screen = it } }
    ) { padding ->
        Box(Modifier.padding(padding).background(p.bg)) {
            when (val s = screen) {
                is Screen.Dashboard -> DashboardScreen(
                    tasks = tasks, viewModel = viewModel, userName = userName,
                    showBellDot = !exactAlarmGranted,
                    onOpenFocus = { screen = Screen.Focus(it.id) },
                    onOpenProfile = { screen = Screen.Profile },
                    onEdit = { screen = Screen.Edit(it.id) },
                    onBlocked = onBlocked
                )
                is Screen.TaskList -> TaskListScreen(
                    tasks = tasks, viewModel = viewModel,
                    onOpenFocus = { screen = Screen.Focus(it.id) },
                    onEdit = { screen = Screen.Edit(it.id) },
                    onBlocked = onBlocked
                )
                is Screen.Add -> AddEditTaskScreen(null, viewModel,
                    onDone = { screen = Screen.TaskList })
                is Screen.Edit -> {
                    val task = tasks.firstOrNull { it.id == s.taskId }
                    if (task == null) screen = Screen.TaskList
                    else AddEditTaskScreen(task, viewModel, onDone = { screen = Screen.Focus(task.id) })
                }
                is Screen.Focus -> {
                    val task = tasks.firstOrNull { it.id == s.taskId }
                    if (task == null) screen = Screen.Dashboard
                    else FocusScreen(
                        task = task, viewModel = viewModel,
                        hasOtherActive = tasks.any { it.isActive && it.id != task.id },
                        onEdit = { screen = Screen.Edit(it.id) },
                        onBack = { screen = Screen.Dashboard },
                        onBlocked = onBlocked
                    )
                }
                is Screen.Stats -> StatsScreen(tasks,
                    onOpenFocus = { screen = Screen.Focus(it.id) })
                is Screen.Profile -> ProfileScreen(
                    userName = userName, onSaveName = onSaveName,
                    themeChoice = themeChoice, onThemeChange = onThemeChange,
                    exactAlarmGranted = exactAlarmGranted,
                    onRequestExactAlarm = onRequestExactAlarm,
                    onRequestBatteryExemption = onRequestBatteryExemption
                )
            }
        }
    }
}
