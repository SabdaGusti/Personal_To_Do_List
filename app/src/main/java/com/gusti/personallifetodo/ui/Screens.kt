package com.gusti.personallifetodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusti.personallifetodo.alarm.AlarmScheduler
import com.gusti.personallifetodo.data.TaskEntity
import com.gusti.personallifetodo.data.TaskStatus
import com.gusti.personallifetodo.ui.theme.LocalWebPalette
import com.gusti.personallifetodo.util.DateTimeUtil
import kotlinx.coroutines.delay
import java.util.Calendar

/* Layar-layar di file ini mengikuti urutan & gaya versi web (App.tsx):
 * Beranda = sapaan + lonceng, hero tugas aktif / tugas berikutnya,
 * Ringkasan Hari Ini (3 kotak di tengah), Jadwal Hari Ini (bar ringkas),
 * lalu Streak dan Tips. Halaman Fokus = kartu berbingkai seperti web. */

private val TIPS = listOf(
    "Satu tugas sampai selesai lebih baik daripada lima tugas setengah jalan.",
    "Kalau ragu mulai dari mana, mulai dari tugas terkecil dulu untuk membangun momentum.",
    "Istirahat singkat yang terjadwal menjaga fokus lebih lama daripada istirahat karena lelah.",
    "Tulis tugas dengan kata kerja yang jelas: 'Kirim laporan', bukan hanya 'Laporan'.",
    "Menunda 5 menit pertama adalah yang paling berat — lewati itu, sisanya mengalir.",
    "Tenggat yang realistis mengalahkan tenggat yang ambisius tapi selalu terlewat."
)

@Composable
private fun useNow(intervalMs: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(intervalMs) } }
    return now
}

/** Blok timer fokus web: label aksen, timer 48.sp, progress bar siklus
 *  30 menit, keterangan pengingat, Istirahat (putih) + Selesai (hijau),
 *  lalu "Batalkan Tugas" merah di tengah. Dipakai hero Beranda & Fokus. */
@Composable
fun FocusTimerBlock(task: TaskEntity, viewModel: AppViewModel) {
    val p = LocalWebPalette.current
    val now = useNow(500)
    val focusMs = task.liveFocusMs(now)
    val cycle = AlarmScheduler.REMINDER_INTERVAL_MS
    Column {
        Text("Waktu fokus", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.accent)
        Text(DateTimeUtil.durationLabel(focusMs), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(p.surface)) {
            Box(Modifier.fillMaxWidth(((focusMs % cycle).toFloat() / cycle)).height(10.dp)
                .clip(RoundedCornerShape(50)).background(p.accent))
        }
        Spacer(Modifier.height(8.dp))
        Text("Pengingat berikutnya dalam " + DateTimeUtil.durationLabel(cycle - (focusMs % cycle)),
            fontSize = 14.sp, color = p.dim)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WebButton("Istirahat", { viewModel.startBreak(task) }, Modifier.weight(1f),
                bg = p.surface, fg = p.text, icon = Icons.Default.Coffee)
            WebButton("Selesai", { viewModel.completeTask(task) }, Modifier.weight(1f),
                bg = p.green, fg = androidx.compose.ui.graphics.Color.White, icon = Icons.Default.Check)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.TextButton(onClick = { viewModel.cancelTask(task) }) {
                Text("Batalkan Tugas", color = p.red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

/** Blok istirahat web: label amber, timer berjalan, progress ke batas
 *  15 menit, sisa waktu, lalu tombol Kembali Bekerja (aksen, lebar penuh). */
@Composable
fun BreakTimerBlock(task: TaskEntity, viewModel: AppViewModel) {
    val p = LocalWebPalette.current
    val now = useNow(500)
    val start = task.breakStartedAt ?: now
    val elapsed = (now - start).coerceAtLeast(0)
    val max = AlarmScheduler.BREAK_MAX_MS
    Column {
        Text("Sedang istirahat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.amber)
        Text(DateTimeUtil.durationLabel(elapsed), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(p.surface)) {
            Box(Modifier.fillMaxWidth((elapsed.toFloat() / max).coerceIn(0f, 1f)).height(10.dp)
                .clip(RoundedCornerShape(50)).background(p.amber))
        }
        Spacer(Modifier.height(8.dp))
        Text("Batas istirahat 15 menit. Sisa " + DateTimeUtil.durationLabel((max - elapsed).coerceAtLeast(0)) + ".",
            fontSize = 14.sp, color = p.dim)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Kembali Bekerja", { viewModel.resumeFromBreak(task) }, Modifier.fillMaxWidth())
    }
}

@Composable
fun DashboardScreen(
    tasks: List<TaskEntity>,
    viewModel: AppViewModel,
    userName: String,
    showBellDot: Boolean,
    onOpenFocus: (TaskEntity) -> Unit,
    onOpenProfile: () -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onBlocked: () -> Unit
) {
    val p = LocalWebPalette.current
    val now = useNow()
    val todayStart = DateTimeUtil.dayStart(now)
    val todayEnd = todayStart + 86_400_000L
    val todayTasks = tasks.filter {
        (it.scheduledStart in todayStart until todayEnd) ||
            (it.statusEnum == TaskStatus.PENDING && it.isOverdue(now))
    }.sortedBy { it.scheduledStart }
    val active = tasks.firstOrNull { it.isActive }
    val nextPending = tasks.filter { it.statusEnum == TaskStatus.PENDING }.minByOrNull { it.scheduledStart }
    val completedToday = tasks.count {
        it.statusEnum == TaskStatus.COMPLETED && it.completedAt != null &&
            it.completedAt in todayStart until todayEnd
    }
    val focusTodayMs = tasks.filter {
        it.completedAt != null && it.completedAt in todayStart until todayEnd
    }.sumOf { it.focusAccumulatedMs } +
        (if (active != null && active.startedAt != null && active.startedAt >= todayStart)
            active.liveFocusMs(now) else 0L)
    val streak = computeStreak(tasks, now)
    val tip = TIPS[((now / 86_400_000L) % TIPS.size).toInt()]

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(Modifier.height(0.dp)) }
        // Sapaan + tanggal panjang + tombol lonceng bundar putih (web)
        item {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greet = when (hour) { in 0..10 -> "Selamat pagi"; in 11..14 -> "Selamat siang"; in 15..18 -> "Selamat sore"; else -> "Selamat malam" }
                    Text(greet + (if (userName.isNotBlank()) ", $userName" else ""),
                        fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
                    Text(DateTimeUtil.longLabel(now), fontSize = 14.sp, color = p.dim)
                }
                Box(
                    Modifier.shadow(1.dp, CircleShape).clip(CircleShape).background(p.surface)
                        .clickable { onOpenProfile() }
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.Notifications, "Profil & izin", tint = p.text, modifier = Modifier.size(22.dp))
                    if (showBellDot) Box(Modifier.align(Alignment.TopEnd).size(10.dp).clip(CircleShape).background(p.red))
                }
            }
        }
        // Hero: tugas aktif (kartu biru berbingkai aksen) atau tugas berikutnya
        item {
            if (active != null) {
                WebCard(radius = 24, containerColor = p.blueBg,
                    border = androidx.compose.foundation.BorderStroke(2.dp, p.accent)) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(active)
                            if (active.isOverdue(now)) { Spacer(Modifier.width(8.dp)); LateBadge() }
                        }
                        // PERBAIKAN BUG (laporan Gusti + screenshot): catatan
                        // "Mulai terlambat dari jadwal" TIDAK BOLEH lagi sebaris
                        // dengan chip+ badge. Dua pill itu saja hampir memenuhi
                        // lebar kartu, jadi sisa lebar untuk teks ini tinggal
                        // beberapa dp: teks terpaksa membungkus 1-2 huruf per
                        // baris membentuk kolom huruf vertikal di tepi kanan,
                        // barisnya menjulang tinggi, chip terdorong ke tengah
                        // kartu dan timer terdorong jauh ke bawah. Sekarang
                        // catatan ini baris sendiri selebar kartu di bawah badge.
                        if (active.startedLate) {
                            Spacer(Modifier.height(6.dp))
                            Text("Mulai terlambat dari jadwal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = p.dim)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(active.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
                        ScheduleLine(active)
                        Spacer(Modifier.height(16.dp))
                        if (active.statusEnum == TaskStatus.ON_BREAK) BreakTimerBlock(active, viewModel)
                        else FocusTimerBlock(active, viewModel)
                    }
                }
            } else {
                WebCard(radius = 24) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Tidak ada tugas aktif — satu tugas dalam satu waktu.",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.dim)
                        if (nextPending != null) {
                            Spacer(Modifier.height(12.dp))
                            StatusChip(nextPending)
                            Spacer(Modifier.height(8.dp))
                            Text(nextPending.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
                            ScheduleLine(nextPending)
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton("Buka Halaman Fokus", { onOpenFocus(nextPending) },
                                Modifier.fillMaxWidth(), icon = Icons.Default.GpsFixed)
                            Spacer(Modifier.height(8.dp))
                            Surface2Button("Mulai Tugas Ini", { viewModel.startTask(nextPending, onBlocked) },
                                Modifier.fillMaxWidth(), icon = Icons.Default.PlayArrow, textSize = 16)
                        } else {
                            Spacer(Modifier.height(12.dp))
                            Text("Belum ada tugas terjadwal", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
                            Text("Tekan tombol + di bawah untuk membuat tugas pertamamu.", fontSize = 14.sp, color = p.dim)
                        }
                    }
                }
            }
        }
        // Ringkasan Hari Ini: 3 kotak putih, angka & label di tengah (web)
        item {
            Column {
                Text("Ringkasan Hari Ini", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryBox("$completedToday", "Tugas selesai", p.green, Modifier.weight(1f))
                    SummaryBox(DateTimeUtil.durationWords(focusTodayMs), "Waktu fokus", p.accent, Modifier.weight(1f))
                    SummaryBox("${todayTasks.size}", "Terjadwal hari ini", p.text, Modifier.weight(1f))
                }
            }
        }
        // Jadwal Hari Ini: bar ringkas web (bukan kartu tugas penuh)
        item {
            Column {
                Text("Jadwal Hari Ini", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
                Spacer(Modifier.height(8.dp))
                // Status di bar jadwal sekarang simbol saja -> beri legenda artinya.
                StatusLegend()
            }
        }
        if (todayTasks.isEmpty()) {
            item {
                WebCard(radius = 16) {
                    Text("Tidak ada tugas untuk hari ini.", Modifier.padding(16.dp), fontSize = 14.sp, color = p.dim)
                }
            }
        } else {
            // Bar-bar jadwal rapat seperti web (space-y-2.5 = 10.dp)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    todayTasks.forEach { task -> ScheduleRow(task, onTap = { onOpenFocus(task) }) }
                }
            }
        }
        // Streak + Tips (web)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WebCard(radius = 16, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clip(CircleShape).background(p.amberBg).padding(10.dp)) {
                            Icon(Icons.Default.Whatshot, null, tint = p.amber, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(if (streak > 0) "$streak hari beruntun" else "Mulai streak barumu",
                                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = p.text)
                            Text("Hari dengan minimal satu tugas selesai.", fontSize = 14.sp, color = p.dim)
                        }
                    }
                }
                WebCard(radius = 16, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Tips singkat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.accent)
                        Text(tip, fontSize = 14.sp, color = p.dim)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SummaryBox(value: String, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val p = LocalWebPalette.current
    WebCard(radius = 16, modifier = modifier) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color, textAlign = TextAlign.Center)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = p.dim, textAlign = TextAlign.Center)
        }
    }
}

private fun computeStreak(tasks: List<TaskEntity>, now: Long): Int {
    val days = tasks.filter { it.statusEnum == TaskStatus.COMPLETED && it.completedAt != null }
        .map { DateTimeUtil.dayStart(it.completedAt!!) }.toSet()
    var streak = 0
    var cursor = DateTimeUtil.dayStart(now)
    if (cursor !in days) cursor -= 86_400_000L
    while (cursor in days) { streak++; cursor -= 86_400_000L }
    return streak
}

/* ---------------- tab Tugas (web) ----------------
 * Judul 24.sp, filter pill bergaya web (bukan FilterChip Material):
 * pill terpilih = aksen + teks ink, lainnya putih + teks dim. */
@Composable
fun PillSelector(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalWebPalette.current
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val isSel = opt == selected
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (isSel) p.accent else p.surface)
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(opt, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = if (isSel) p.accentInk else p.dim)
            }
        }
    }
}

/* ---------------- Tugas: kalender sederhana (permintaan Gusti) ----------------
 * Menu Tugas diper-simple: bukan lagi daftar panjang + filter pill, tapi
 * satu kalender bulanan. Tanggal yang ada tugasnya ditandai titik (merah
 * bila ada tugas terlambat yang belum dikerjakan) + jumlah tugasnya.
 * Tap tanggal -> daftar tugas pada tanggal itu muncul di bawah kalender,
 * memakai kartu yang sama seperti sebelumnya (tombol Fokus / Mulai / Ubah /
 * Hapus / Batalkan tetap ada). Tombol < / > untuk pindah bulan, dan
 * "Hari ini" untuk kembali ke tanggal sekarang. */

private fun firstOfMonth(ms: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    c.set(Calendar.DAY_OF_MONTH, 1)
    return DateTimeUtil.dayStart(c.timeInMillis)
}

private fun shiftMonth(anchor: Long, delta: Int): Long {
    val c = Calendar.getInstance().apply { timeInMillis = anchor }
    c.add(Calendar.MONTH, delta)
    c.set(Calendar.DAY_OF_MONTH, 1)
    return DateTimeUtil.dayStart(c.timeInMillis)
}

@Composable
private fun MonthNavButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val p = LocalWebPalette.current
    Box(
        Modifier.shadow(1.dp, CircleShape).clip(CircleShape).background(p.surface2)
            .clickable { onClick() }.padding(8.dp),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = p.text, modifier = Modifier.size(20.dp)) }
}

@Composable
fun TaskListScreen(
    tasks: List<TaskEntity>,
    viewModel: AppViewModel,
    onOpenFocus: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onBlocked: () -> Unit
) {
    val p = LocalWebPalette.current
    val todayStart = DateTimeUtil.dayStart(System.currentTimeMillis())
    var selectedDay by remember { mutableLongStateOf(todayStart) }
    var monthAnchor by remember { mutableLongStateOf(firstOfMonth(todayStart)) }
    val hasActive = tasks.any { it.isActive }
    val tasksByDay = remember(tasks) { tasks.groupBy { DateTimeUtil.dayStart(it.scheduledStart) } }
    val dayTasks = (tasksByDay[selectedDay] ?: emptyList()).sortedBy { it.scheduledStart }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text("Tugas", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
            Text("Tap tanggal untuk melihat tugasnya.", fontSize = 14.sp, color = p.dim)
        }
        item {
            WebCard(radius = 24, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(DateTimeUtil.monthLabel(monthAnchor), fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold, color = p.text, modifier = Modifier.weight(1f))
                        MonthNavButton(Icons.Default.ChevronLeft, "Bulan sebelumnya") {
                            monthAnchor = shiftMonth(monthAnchor, -1)
                        }
                        Spacer(Modifier.width(8.dp))
                        MonthNavButton(Icons.Default.ChevronRight, "Bulan berikutnya") {
                            monthAnchor = shiftMonth(monthAnchor, 1)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach { d ->
                            Text(d, Modifier.weight(1f), fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = p.dim, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val first = Calendar.getInstance().apply { timeInMillis = monthAnchor }
                    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
                    // Senin = kolom pertama: DAY_OF_WEEK Senin=2 ... Minggu=1.
                    val leading = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    val weeks = (leading + daysInMonth + 6) / 7
                    for (w in 0 until weeks) {
                        Row {
                            for (d in 0 until 7) {
                                val dayNum = w * 7 + d - leading + 1
                                if (dayNum < 1 || dayNum > daysInMonth) {
                                    Box(Modifier.weight(1f).height(58.dp))
                                } else {
                                    val dc = Calendar.getInstance().apply {
                                        timeInMillis = monthAnchor; set(Calendar.DAY_OF_MONTH, dayNum)
                                    }
                                    val dayMs = DateTimeUtil.dayStart(dc.timeInMillis)
                                    val count = tasksByDay[dayMs]?.size ?: 0
                                    val hasLate = tasksByDay[dayMs]?.any {
                                        it.statusEnum == TaskStatus.PENDING && it.isOverdue()
                                    } == true
                                    val isToday = dayMs == todayStart
                                    val isSelected = dayMs == selectedDay
                                    // Sel kalender dirapikan: tinggi TETAP 58.dp
                                    // (kalender jadi lebih panjang ke bawah, sesuai
                                    // izin Gusti). Versi lama memakai aspectRatio(1f)
                                    // + lingkaran sebesar sel, jadi titik & jumlah
                                    // tugas terjepit di tepi bawah lingkaran dan
                                    // tampil berantakan seperti pada screenshot.
                                    // Sekarang angka di dalam lingkaran kecil 38.dp,
                                    // dan penanda tugas punya baris sendiri di
                                    // bawahnya (selalu disediakan, tinggi 12.dp),
                                    // jadi semua angka sejajar rapi antarbaris.
                                    Box(
                                        Modifier.weight(1f).height(58.dp)
                                            .clickable { selectedDay = dayMs },
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                Modifier.size(38.dp).clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isSelected -> p.accent
                                                            isToday -> p.blueBg
                                                            else -> Color.Transparent
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "$dayNum", fontSize = 15.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    color = when {
                                                        isSelected -> p.accentInk
                                                        isToday -> p.accent
                                                        else -> p.text
                                                    }
                                                )
                                            }
                                            // Baris penanda tugas: tingginya SELALU
                                            // 12.dp (dikosongkan bila tanggal itu tanpa
                                            // tugas) supaya posisi angka tidak bergeser
                                            // antar sel. Titik merah = ada tugas
                                            // terlambat; biru = ada tugas.
                                            Box(Modifier.height(12.dp), contentAlignment = Alignment.Center) {
                                                if (count > 0) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            Modifier.size(6.dp).clip(CircleShape)
                                                                .background(if (hasLate) p.red else p.accent)
                                                        )
                                                        if (count > 1) {
                                                            Spacer(Modifier.width(3.dp))
                                                            Text("$count", fontSize = 9.sp, lineHeight = 10.sp,
                                                                fontWeight = FontWeight.Bold, color = p.dim)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Legenda penanda kalender, di dalam kartu di bawah grid tanggal.
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(p.accent))
                        Spacer(Modifier.width(5.dp))
                        Text("Ada tugas", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = p.dim)
                        Spacer(Modifier.width(16.dp))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(p.red))
                        Spacer(Modifier.width(5.dp))
                        Text("Ada yang terlambat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = p.dim)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(DateTimeUtil.longLabel(selectedDay), fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold, color = p.text)
                    Text(
                        if (dayTasks.isEmpty()) "Tidak ada tugas" else "${dayTasks.size} tugas",
                        fontSize = 13.sp, color = p.dim
                    )
                }
                if (selectedDay != todayStart) {
                    Box(
                        Modifier.shadow(1.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                            .background(p.surface)
                            .clickable { selectedDay = todayStart; monthAnchor = firstOfMonth(todayStart) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Hari ini", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = p.accent)
                    }
                }
            }
        }
        if (dayTasks.isEmpty()) {
            item {
                WebCard(radius = 16, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Tidak ada tugas pada tanggal ini. Tap tanggal lain di kalender, atau tambah tugas baru lewat tombol + di bawah.",
                        Modifier.padding(20.dp).fillMaxWidth(),
                        fontSize = 14.sp, color = p.dim, textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(dayTasks) { task ->
                TaskListCard(
                    task = task,
                    onOpenFocus = onOpenFocus,
                    onStart = { viewModel.startTask(it, onBlocked) },
                    onEdit = onEdit,
                    onDelete = { viewModel.deleteTask(it) },
                    onCancel = { viewModel.cancelTask(it) },
                    hasOtherActive = hasActive && !task.isActive
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/* ---------------- Halaman Fokus (web) ----------------
 * Header: tombol pill putih "‹ Kembali" + judul 18.sp. Isi: kartu
 * rounded-3xl berbingkai 2.dp (aksen + latar blueBg bila sedang berjalan,
 * selain itu border + surface), sama seperti FocusPage di web. */
@Composable
fun FocusScreen(
    task: TaskEntity,
    viewModel: AppViewModel,
    hasOtherActive: Boolean,
    onEdit: (TaskEntity) -> Unit,
    onBack: () -> Unit,
    onBlocked: () -> Unit
) {
    val p = LocalWebPalette.current
    val now = useNow()
    val isRunning = task.statusEnum == TaskStatus.IN_PROGRESS
    Column(
        Modifier.fillMaxSize().background(p.bg).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.shadow(1.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                    .background(p.surface)
                    .clickable { onBack() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowBack, null, tint = p.text, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kembali", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text("Halaman Fokus", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
        }
        Spacer(Modifier.height(16.dp))
        WebCard(
            radius = 24,
            containerColor = if (isRunning) p.blueBg else p.surface,
            border = androidx.compose.foundation.BorderStroke(2.dp, if (isRunning) p.accent else p.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(task)
                    if (task.isActive && task.isOverdue(now)) { Spacer(Modifier.width(8.dp)); LateBadge() }
                }
                // Sama seperti hero Beranda: catatan mulai-terlambat di baris
                // sendiri di bawah badge — dulu sebaris dengan chip+badge
                // sehingga terhimpit jadi kolom huruf vertikal di tepi kanan
                // kartu Fokus (bug pada screenshot Gusti).
                if (task.startedLate && task.statusEnum != TaskStatus.PENDING) {
                    Spacer(Modifier.height(6.dp))
                    Text("Mulai terlambat dari jadwal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = p.dim)
                }
                Spacer(Modifier.height(12.dp))
                Text(task.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
                if (task.description.isNotBlank()) Text(task.description, fontSize = 16.sp, color = p.dim)
                ScheduleLine(task)
                Spacer(Modifier.height(20.dp))
                when (task.statusEnum) {
                    TaskStatus.PENDING -> PendingFocus(task, viewModel, hasOtherActive, onEdit, onBlocked)
                    TaskStatus.IN_PROGRESS -> FocusTimerBlock(task, viewModel)
                    TaskStatus.ON_BREAK -> BreakTimerBlock(task, viewModel)
                    TaskStatus.COMPLETED, TaskStatus.CANCELLED -> DoneFocus(task, viewModel, onBack)
                }
            }
        }
        if (task.statusEnum == TaskStatus.PENDING) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Fokus total: " + DateTimeUtil.durationWords(task.liveFocusMs(now)) +
                    " · Istirahat: " + DateTimeUtil.durationWords(DateTimeUtil.liveBreakMs(task, now)),
                Modifier.fillMaxWidth(), fontSize = 14.sp, color = p.dim, textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PendingFocus(
    task: TaskEntity, viewModel: AppViewModel, hasOtherActive: Boolean,
    onEdit: (TaskEntity) -> Unit, onBlocked: () -> Unit
) {
    val p = LocalWebPalette.current
    val overdue = task.isOverdue()
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surface2).padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (overdue) "Waktu mulai sudah lewat — mulai sekarang juga bisa."
                else "Siap fokus? Tekan Mulai saat kamu siap mengerjakan tugas ini.",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.dim, textAlign = TextAlign.Center
            )
            Text(DateTimeUtil.timeLabel(task.scheduledStart), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
            Text(DateTimeUtil.longLabel(task.scheduledStart) + " · Deadline " + DateTimeUtil.timeLabel(task.deadline),
                fontSize = 14.sp, color = p.dim, textAlign = TextAlign.Center)
        }
    }
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Mulai", { viewModel.startTask(task, onBlocked) }, Modifier.fillMaxWidth(),
        icon = Icons.Default.PlayArrow, enabled = !hasOtherActive, textSize = 18)
    if (hasOtherActive) {
        Spacer(Modifier.height(8.dp))
        Text("Selesaikan atau batalkan tugas yang sedang berjalan dulu untuk memulai tugas ini — satu tugas dalam satu waktu.",
            fontSize = 14.sp, color = p.dim, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface2Button("Ubah", { onEdit(task) }, Modifier.weight(1f), icon = Icons.Default.Edit)
        DeleteOutlineButton("Hapus", { viewModel.deleteTask(task) }, Modifier.weight(1f))
    }
}

@Composable
private fun DoneFocus(task: TaskEntity, viewModel: AppViewModel, onBack: () -> Unit) {
    val p = LocalWebPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(p.surface2).padding(14.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(DateTimeUtil.durationWords(task.focusAccumulatedMs), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = p.accent)
                Text("Waktu fokus", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = p.dim)
            }
        }
        Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(p.surface2).padding(14.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(DateTimeUtil.durationWords(task.breakAccumulatedMs), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = p.amber)
                Text("Waktu istirahat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = p.dim)
            }
        }
    }
    task.completedAt?.let {
        Spacer(Modifier.height(12.dp))
        Text("Berakhir " + DateTimeUtil.dayLabel(it) + " " + DateTimeUtil.timeLabel(it),
            Modifier.fillMaxWidth(), fontSize = 14.sp, color = p.dim, textAlign = TextAlign.Center)
    }
    Text("Tugas ini sudah " + (if (task.statusEnum == TaskStatus.COMPLETED) "selesai dan tercatat di Riwayat." else "dibatalkan dan tercatat di Riwayat."),
        Modifier.fillMaxWidth(), fontSize = 14.sp, color = p.dim, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Kembali ke Beranda", onBack, Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Surface2Button("Hapus dari Riwayat", { viewModel.deleteTask(task); onBack() },
        Modifier.fillMaxWidth(), icon = Icons.Default.Delete, fg = p.dim)
}
