package com.gusti.personallifetodo.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusti.personallifetodo.data.RecurrenceType
import com.gusti.personallifetodo.data.TaskEntity
import com.gusti.personallifetodo.data.TaskStatus
import com.gusti.personallifetodo.ui.theme.LocalWebPalette
import com.gusti.personallifetodo.ui.theme.ThemeChoice
import com.gusti.personallifetodo.util.DateTimeUtil
import com.gusti.personallifetodo.util.RecurrenceGenerator
import java.util.Calendar

/* Form, Statistik, dan Profil di file ini juga mengikuti versi web:
 * form = label 14.sp SemiBold di atas kolom rounded-xl (12.dp) berbingkai
 * border; Statistik = pemilih periode 3 kolom + grid metrik 2 kolom +
 * progress bar; Profil = kartu putih rounded-3xl (24.dp). */

/** Kolom teks gaya web: label di atas, kotak rounded-xl, fokus = aksen. */
@Composable
private fun WebField(
    label: String, value: String, onChange: (String) -> Unit,
    placeholder: String, modifier: Modifier = Modifier, minLines: Int = 1
) {
    val p = LocalWebPalette.current
    Column(modifier) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.text)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            placeholder = { Text(placeholder, color = p.dim) },
            modifier = Modifier.fillMaxWidth(), minLines = minLines,
            textStyle = TextStyle(fontSize = 16.sp, color = p.text),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
                focusedContainerColor = p.surface, unfocusedContainerColor = p.surface,
                focusedTextColor = p.text, unfocusedTextColor = p.text,
                cursorColor = p.accent
            )
        )
    }
}

/** Kotak tanggal/waktu gaya web (pengganti input date/time HTML). */
@Composable
private fun WebDateTimeBox(label: String, valueText: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalWebPalette.current
    Column(modifier) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.text)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surface)
                .border(1.dp, p.border, RoundedCornerShape(12.dp))
                .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp)
        ) { Text(valueText, fontSize = 16.sp, color = p.text) }
    }
}

@Composable
fun AddEditTaskScreen(
    existing: TaskEntity?,
    viewModel: AppViewModel,
    onDone: () -> Unit
) {
    val p = LocalWebPalette.current
    val context = LocalContext.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var start by remember { mutableStateOf(existing?.scheduledStart ?: defaultStart()) }
    var deadline by remember { mutableStateOf(existing?.deadline ?: (defaultStart() + 3600_000)) }
    var recurrence by remember { mutableStateOf(existing?.recurrenceEnum ?: RecurrenceType.NONE) }
    var useEndDate by remember { mutableStateOf(false) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun pickDate(current: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(context, { _, y, mo, d ->
            val c = Calendar.getInstance().apply { timeInMillis = current; set(y, mo, d) }
            onPicked(c.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
    fun pickTime(current: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        TimePickerDialog(context, { _, h, mi ->
            val c = Calendar.getInstance().apply {
                timeInMillis = current; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, mi)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            onPicked(c.timeInMillis)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }
    fun pickEndDate(current: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(context, { _, y, mo, d ->
            val c = Calendar.getInstance().apply { set(y, mo, d, 23, 59, 0); set(Calendar.MILLISECOND, 0) }
            onPicked(c.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    val previewCount = if (existing == null)
        RecurrenceGenerator.count(recurrence, start, if (useEndDate) endDate else null) else 1

    LazyColumn(
        Modifier.fillMaxSize().background(p.bg).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(0.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (existing == null) "Tambah Tugas" else "Ubah Tugas",
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
                Spacer(Modifier.weight(1f))
                Box(Modifier.clip(CircleShape).background(p.surface2).clickable { onDone() }.padding(8.dp)) {
                    Icon(Icons.Default.Close, "Tutup formulir", tint = p.text, modifier = Modifier.size(20.dp))
                }
            }
        }
        item { WebField("Judul tugas", title, { title = it }, "Contoh: Kerjakan laporan bab 3") }
        item { WebField("Deskripsi (opsional)", description, { description = it }, "Detail singkat tugas ini", minLines = 3) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WebDateTimeBox("Tanggal mulai", DateTimeUtil.dayLabel(start), { pickDate(start) { start = it } }, Modifier.weight(1f))
                WebDateTimeBox("Waktu mulai", DateTimeUtil.timeLabel(start), { pickTime(start) { start = it } }, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WebDateTimeBox("Tanggal deadline", DateTimeUtil.dayLabel(deadline), { pickDate(deadline) { deadline = it } }, Modifier.weight(1f))
                WebDateTimeBox("Waktu deadline", DateTimeUtil.timeLabel(deadline), { pickTime(deadline) { deadline = it } }, Modifier.weight(1f))
            }
        }
        if (existing == null) {
            // Kartu Perulangan web: rounded-2xl berbingkai border, isi padding 16.dp
            item {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surface)
                        .border(1.dp, p.border, RoundedCornerShape(16.dp)).padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Perulangan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.text)
                        PillSelector(
                            RecurrenceType.values().map { it.label },
                            recurrence.label,
                            { lbl -> recurrence = RecurrenceType.values().first { it.label == lbl } }
                        )
                        if (recurrence != RecurrenceType.NONE) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = useEndDate, onCheckedChange = { useEndDate = it },
                                    colors = CheckboxDefaults.colors(checkedColor = p.accent))
                                Text("Berakhir pada tanggal tertentu", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = p.text)
                            }
                            if (useEndDate) {
                                WebDateTimeBox("Berakhir pada",
                                    endDate?.let { DateTimeUtil.dayLabel(it) } ?: "Belum dipilih",
                                    { pickEndDate(endDate ?: start) { endDate = it } })
                            }
                            val lastStart = RecurrenceGenerator.shift(start, recurrence, previewCount - 1)
                            Text(
                                "Akan dibuat $previewCount jadwal " + recurrence.label.removePrefix("Setiap ").lowercase() +
                                    ", dari " + DateTimeUtil.dayLabel(start) + " sampai " + DateTimeUtil.dayLabel(lastStart) +
                                    ". Tiap jadwal berdiri sendiri, jadi satu tanggal bisa diubah atau dihapus tanpa mengubah yang lain.",
                                fontSize = 14.sp, color = p.dim
                            )
                        }
                    }
                }
            }
        }
        if (existing != null && existing.recurrenceEnum != RecurrenceType.NONE) {
            item {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.blueBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row {
                        Icon(Icons.Default.Repeat, null, tint = p.text, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Jadwal ini bagian dari perulangan " +
                            existing.recurrenceEnum.label.removePrefix("Setiap ").lowercase() +
                            ". Perubahan di sini hanya berlaku untuk jadwal ini.",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.text)
                    }
                }
            }
        }
        if (error != null) {
            item {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.redBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(error!!, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.red)
                }
            }
        }
        item {
            Column {
                PrimaryButton(
                    if (existing == null) "Buat Tugas" else "Simpan Perubahan",
                    onClick = {
                        when {
                            title.isBlank() -> error = "Judul tugas wajib diisi."
                            deadline <= start -> error = "Deadline harus setelah waktu mulai."
                            else -> {
                                error = null
                                if (existing == null) {
                                    viewModel.createTasks(title.trim(), description.trim(), start, deadline,
                                        recurrence, if (useEndDate) endDate else null)
                                } else {
                                    viewModel.updateTask(existing, title.trim(), description.trim(), start, deadline)
                                }
                                onDone()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Pengingat mengikuti aturan aplikasi: alarm saat waktu mulai lewat, pengingat tiap 30 menit saat mengerjakan, istirahat maks 15 menit.",
                    Modifier.fillMaxWidth(), fontSize = 14.sp, color = p.dim, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun defaultStart(): Long {
    val c = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    return c.timeInMillis
}

/* ---------------- Statistik (web) ---------------- */
@Composable
fun StatsScreen(tasks: List<TaskEntity>, onOpenFocus: (TaskEntity) -> Unit) {
    val p = LocalWebPalette.current
    var range by remember { mutableStateOf("Harian") }
    val now = System.currentTimeMillis()
    val todayStart = DateTimeUtil.dayStart(now)
    val periodStart = when (range) {
        "Harian" -> todayStart
        "Mingguan" -> {
            val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            todayStart - ((dow + 5) % 7) * 86_400_000L
        }
        else -> {
            val c = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.DAY_OF_MONTH, 1) }
            DateTimeUtil.dayStart(c.timeInMillis)
        }
    }
    val periodTasks = tasks.filter { (it.completedAt ?: it.startedAt ?: it.scheduledStart) >= periodStart }
    val completed = periodTasks.count { it.statusEnum == TaskStatus.COMPLETED }
    val startedLate = periodTasks.count { it.startedLate }
    val overduePending = tasks.count { it.statusEnum == TaskStatus.PENDING && it.isOverdue(now) && it.scheduledStart >= periodStart }
    val focusMs = periodTasks.sumOf { it.liveFocusMs(now) }
    val breakMs = periodTasks.sumOf { DateTimeUtil.liveBreakMs(it, now) }
    val cancelled = periodTasks.count { it.statusEnum == TaskStatus.CANCELLED }
    val denom = completed + cancelled + overduePending
    val pct = if (denom > 0) (completed * 100) / denom else null
    val history = tasks.filter { it.statusEnum == TaskStatus.COMPLETED || it.statusEnum == TaskStatus.CANCELLED }
        .sortedByDescending { it.completedAt ?: 0L }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(Modifier.height(0.dp))
            Text("Statistik", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
        }
        // Pemilih periode web: grid 3 kolom sama lebar, rounded-xl
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Harian", "Mingguan", "Bulanan").forEach { r ->
                    val sel = range == r
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (sel) p.accent else p.surface)
                            .clickable { range = r }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(r, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) p.accentInk else p.dim) }
                }
            }
        }
        // Grid metrik 2 kolom web
        item {
            val metrics = listOf(
                Triple("Tugas selesai", "$completed", p.green),
                Triple("Tugas terlambat", "${startedLate + overduePending}", p.red),
                Triple("Durasi fokus", DateTimeUtil.durationWords(focusMs), p.accent),
                Triple("Durasi istirahat", DateTimeUtil.durationWords(breakMs), p.amber),
                Triple("Frekuensi penundaan", "$startedLate kali", p.text),
                Triple("Persentase penyelesaian", pct?.let { "$it%" } ?: "—", p.text)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                metrics.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { (label, value, color) ->
                            WebCard(radius = 16, modifier = Modifier.weight(1f)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
                                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.dim)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (pct != null) {
            item {
                Column {
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).background(p.surface2)) {
                        Box(Modifier.fillMaxWidth(pct / 100f).height(12.dp).clip(RoundedCornerShape(50)).background(p.green))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("$completed selesai dari $denom tugas yang jatuh tempo pada periode ini.", fontSize = 14.sp, color = p.dim)
                }
            }
        }
        item { Text("Riwayat", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = p.text) }
        if (history.isEmpty()) {
            item {
                WebCard(radius = 16, modifier = Modifier.fillMaxWidth()) {
                    Text("Belum ada riwayat. Tugas yang selesai atau dibatalkan akan muncul di sini.",
                        Modifier.padding(16.dp), fontSize = 14.sp, color = p.dim)
                }
            }
        } else {
            items(history) { task -> TaskListCard(task, onOpenFocus = onOpenFocus, showActions = false) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/* ---------------- Profil & Pengaturan (web) ----------------
 * Kartu putih rounded-3xl: Nama sapaan, Tema tampilan (Sistem/Terang/Gelap),
 * Notifikasi & Alarm (khusus native: izin alarm eksak + baterai),
 * dan Aturan aplikasi (6 poin yang sama dengan web). */
@Composable
fun ProfileScreen(
    userName: String,
    onSaveName: (String) -> Unit,
    themeChoice: ThemeChoice,
    onThemeChange: (ThemeChoice) -> Unit,
    exactAlarmGranted: Boolean,
    onRequestExactAlarm: () -> Unit,
    onRequestBatteryExemption: () -> Unit
) {
    val p = LocalWebPalette.current
    var name by remember(userName) { mutableStateOf(userName) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(Modifier.height(0.dp))
            Text("Profil & Pengaturan", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = p.text)
        }
        item {
            WebCard(radius = 24, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Nama sapaan", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = p.text)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            WebFieldPlain(name, { name = it }, "Nama kamu")
                        }
                        PrimaryButton("Simpan", { onSaveName(name.trim()) }, textSize = 14)
                    }
                }
            }
        }
        item {
            WebCard(radius = 24, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Tema tampilan", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = p.text)
                    Text("Pilih terang, gelap, atau ikuti pengaturan perangkat.", fontSize = 14.sp, color = p.dim)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(ThemeChoice.SYSTEM, ThemeChoice.LIGHT, ThemeChoice.DARK).forEach { choice ->
                            val sel = themeChoice == choice
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) p.accent else p.surface2)
                                    .clickable { onThemeChange(choice) }.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(choice.label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = if (sel) p.accentInk else p.text)
                            }
                        }
                    }
                }
            }
        }
        item {
            WebCard(radius = 24, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notifikasi & Alarm", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = p.text)
                    Text(
                        if (exactAlarmGranted) "Alarm eksak: aktif. Alarm bisa berbunyi tepat waktu walau aplikasi ditutup."
                        else "Alarm eksak: belum aktif. Tanpa izin ini Android boleh menggeser waktu alarm.",
                        fontSize = 14.sp, color = p.dim
                    )
                    if (!exactAlarmGranted) PrimaryButton("Aktifkan Alarm Eksak", onRequestExactAlarm, Modifier.fillMaxWidth(), textSize = 14)
                    Surface2Button("Izinkan Abaikan Optimasi Baterai", onRequestBatteryExemption, Modifier.fillMaxWidth())
                    Text("Catatan jujur: versi Android native ini menjadwalkan alarm lewat AlarmManager, jadi alarm tetap berbunyi saat aplikasi ditutup — beda dengan versi web yang hanya berbunyi selama aplikasi terbuka. Hemat baterai ekstrem, mode Jangan Ganggu, atau force-stop manual tetap bisa menahan alarm.",
                        fontSize = 14.sp, color = p.dim)
                }
            }
        }
        item {
            WebCard(radius = 24, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Aturan aplikasi", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = p.text)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Satu tugas dalam satu waktu — tidak ada dua tugas aktif.",
                        "Waktu mulai lewat dan belum dimulai: status Terlambat dan alarm berbunyi sampai dimatikan.",
                        "Pengingat berbunyi tiap 30 menit selama tugas dikerjakan; mematikannya tidak membatalkan siklus berikutnya dan timer fokus tetap berjalan.",
                        "Istirahat maksimal 15 menit; alarm akhir istirahat berbunyi sampai kamu menekan Kembali Bekerja.",
                        "Tugas aktif yang melewati deadline tetap biru dengan badge merah Terlambat, dan pengingat tidak berhenti.",
                        "Jadwal bisa dibuat berulang (harian, mingguan, atau bulanan) dari formulir Tambah Tugas. Tiap tanggal menjadi jadwal tersendiri yang bisa diubah atau dihapus satu per satu."
                    ).forEach { rule ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text("•  ", fontSize = 14.sp, color = p.dim)
                            Text(rule, fontSize = 14.sp, color = p.dim)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** Kolom teks tanpa label (dipakai baris Nama sapaan, gaya input web). */
@Composable
private fun WebFieldPlain(value: String, onChange: (String) -> Unit, placeholder: String) {
    val p = LocalWebPalette.current
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true,
        placeholder = { Text(placeholder, color = p.dim) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 16.sp, color = p.text),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = p.accent, unfocusedBorderColor = p.border,
            focusedContainerColor = p.surface, unfocusedContainerColor = p.surface,
            focusedTextColor = p.text, unfocusedTextColor = p.text, cursorColor = p.accent
        )
    )
}
