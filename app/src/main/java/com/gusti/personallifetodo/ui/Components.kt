package com.gusti.personallifetodo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusti.personallifetodo.data.TaskEntity
import com.gusti.personallifetodo.data.TaskStatus
import com.gusti.personallifetodo.ui.theme.LocalWebPalette
import com.gusti.personallifetodo.ui.theme.StatusColors
import com.gusti.personallifetodo.ui.theme.WebPalette
import com.gusti.personallifetodo.util.DateTimeUtil

/* Komponen di file ini meniru satu-satu potongan UI versi web (App.tsx):
 * StatusChip, LateBadge, ScheduleLine, bar Jadwal Hari Ini, dan kartu daftar
 * Tugas/Riwayat — termasuk radius (rounded-2xl = 16.dp, rounded-3xl = 24.dp,
 * rounded-xl = 12.dp), padding, dan bobot/teks yang sama. */

/** Warna solid status: sama dengan statusSolidVar() di web. Tugas aktif yang
 *  lewat deadline TETAP biru; keterlambatan ditunjukkan badge terpisah. */
fun statusColor(task: TaskEntity, palette: WebPalette): Color = when (task.statusEnum) {
    TaskStatus.COMPLETED -> StatusColors.CompletedGreen
    TaskStatus.CANCELLED -> StatusColors.CancelledGrey
    TaskStatus.IN_PROGRESS, TaskStatus.ON_BREAK -> StatusColors.InProgressBlue
    TaskStatus.PENDING ->
        if (task.isOverdue()) palette.statusOverdueDark else StatusColors.PendingRed
}

fun statusLabel(task: TaskEntity): String = when (task.statusEnum) {
    TaskStatus.PENDING -> if (task.isOverdue()) "Terlambat" else "Belum Dikerjakan"
    TaskStatus.IN_PROGRESS, TaskStatus.ON_BREAK -> "Sedang Dikerjakan"
    TaskStatus.COMPLETED -> "Selesai"
    TaskStatus.CANCELLED -> "Dibatalkan"
}

private fun statusIcon(task: TaskEntity): ImageVector = when (task.statusEnum) {
    TaskStatus.COMPLETED -> Icons.Default.Check
    TaskStatus.CANCELLED -> Icons.Default.Block
    TaskStatus.IN_PROGRESS, TaskStatus.ON_BREAK -> Icons.Default.PlayArrow
    TaskStatus.PENDING -> if (task.isOverdue()) Icons.Default.Notifications else Icons.Default.AccessTime
}

/** Chip status web: pill, padding 14x6.dp, ikon 16.dp, teks 14.sp ExtraBold
 *  putih, bayangan kecil (shadow-md). */
@Composable
fun StatusChip(task: TaskEntity, modifier: Modifier = Modifier) {
    val palette = LocalWebPalette.current
    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(statusColor(task, palette))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(statusIcon(task), null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(statusLabel(task), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1)
        }
    }
}

/** Alias nama lama agar kode lain tetap jalan. */
@Composable
fun StatusBadge(task: TaskEntity, modifier: Modifier = Modifier) = StatusChip(task, modifier)

@Composable
fun LateBadge(modifier: Modifier = Modifier) {
    val palette = LocalWebPalette.current
    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(palette.statusOverdueDark)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text("Terlambat", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1)
    }
}

/** Baris jadwal web: ikon kalender 14.dp + "Jum, 6 Nov · 10.00",
 *  "· Deadline 11.00", "· Berulang Harian" (teks 14.sp warna dim). */
@Composable
fun ScheduleLine(task: TaskEntity, modifier: Modifier = Modifier) {
    val palette = LocalWebPalette.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CalendarMonth, null, tint = palette.dim, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            DateTimeUtil.dayLabel(task.scheduledStart) + " · " + DateTimeUtil.timeLabel(task.scheduledStart) +
                " · Deadline " + DateTimeUtil.timeLabel(task.deadline) +
                (task.recurrenceEnum.badge?.let { " · Berulang " + it.removePrefix("Berulang ") } ?: ""),
            fontSize = 14.sp, color = palette.dim, maxLines = 2
        )
        task.recurrenceEnum.badge?.let {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Repeat, null, tint = palette.dim, modifier = Modifier.size(14.dp))
        }
    }
}

/* ---------------- tombol gaya web ----------------
 * Web: rounded-2xl (16.dp), padding vertikal 14.dp, teks 16.sp ExtraBold.
 * Varian: aksen (primary), surface2, putih/surface, hijau Selesai,
 * dan Hapus berbingkai merah 2.dp di atas redBg. */

@Composable
fun WebButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color,
    fg: Color,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    textSize: Int = 16,
    border: BorderStroke? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = textSize.sp)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                  icon: ImageVector? = null, enabled: Boolean = true, textSize: Int = 16) {
    val p = LocalWebPalette.current
    WebButton(text, onClick, modifier, bg = p.accent, fg = p.accentInk, icon = icon, enabled = enabled, textSize = textSize)
}

@Composable
fun Surface2Button(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                   icon: ImageVector? = null, enabled: Boolean = true, fg: Color? = null, textSize: Int = 14) {
    val p = LocalWebPalette.current
    WebButton(text, onClick, modifier, bg = p.surface2, fg = fg ?: p.text, icon = icon, enabled = enabled, textSize = textSize)
}

@Composable
fun DeleteOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalWebPalette.current
    WebButton(text, onClick, modifier, bg = p.redBg, fg = p.red, icon = Icons.Default.Delete,
        textSize = 14, border = BorderStroke(2.dp, StatusColors.PendingRed))
}

/** Kartu putih web: rounded sesuai parameter, bayangan halus, tanpa border. */
@Composable
fun WebCard(
    modifier: Modifier = Modifier,
    radius: Int = 24,
    containerColor: Color? = null,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val p = LocalWebPalette.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(radius.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor ?: p.surface),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) { content() }
}

/** Simbol status SAJA (permintaan Gusti: Jadwal Hari Ini dirapikan, tanda
 *  selesai / belum dikerjakan diganti simbol saja): lingkaran 32.dp berwarna
 *  solid sesuai status + ikon putih 18.dp, TANPA teks. Pill teks lama
 *  ("Belum Dikerjakan" dll) memakan hampir separuh lebar bar, sehingga judul
 *  terpotong ("Bac...", "Try ...") dan teks "Tap untuk buka Fokus"
 *  membungkus satu kata per baris seperti pada screenshot. Warna + ikonnya
 *  sama persis dengan chip status, dan artinya dijelaskan legenda di bawah
 *  judul "Jadwal Hari Ini" (lihat StatusLegend). */
@Composable
fun StatusSymbol(task: TaskEntity, modifier: Modifier = Modifier, sizeDp: Int = 32) {
    val palette = LocalWebPalette.current
    Box(
        modifier = modifier.size(sizeDp.dp).clip(CircleShape)
            .background(statusColor(task, palette)),
        contentAlignment = Alignment.Center
    ) {
        Icon(statusIcon(task), statusLabel(task), tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/** Legenda arti simbol status untuk Jadwal Hari Ini di Beranda:
 *  lingkaran kecil 18.dp + label singkat 11.sp, dalam satu baris. */
@Composable
fun StatusLegend(modifier: Modifier = Modifier) {
    val palette = LocalWebPalette.current
    val items = listOf(
        Triple(StatusColors.CompletedGreen, Icons.Default.Check, "Selesai"),
        Triple(StatusColors.InProgressBlue, Icons.Default.PlayArrow, "Berjalan"),
        Triple(StatusColors.PendingRed, Icons.Default.AccessTime, "Belum"),
        Triple(palette.statusOverdueDark, Icons.Default.Notifications, "Terlambat")
    )
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
        items.forEach { (color, icon, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(11.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = palette.dim)
            }
        }
    }
}

/* ---------------- bar Jadwal Hari Ini (Beranda) ----------------
 * Dirapikan (permintaan Gusti): bar tinted 14% warna status, stripe kiri
 * 8.dp, jam lebar 46.dp (14.sp ExtraBold), judul tebal 1 baris + sub 12.sp
 * dim (masing-masing maksimal 1 baris), lalu SIMBOL status lingkaran +
 * chevron kanan. Teks "Tap untuk buka Fokus" dihapus dari bar — bar-nya
 * sendiri tetap bisa di-tap untuk masuk Halaman Fokus, ditandai chevron —
 * karena teks itu yang membungkus berantakan di versi lama. */
@Composable
fun ScheduleRow(task: TaskEntity, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalWebPalette.current
    val color = statusColor(task, p)
    Card(
        onClick = onTap,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = lerp(p.surface, color, 0.14f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(8.dp).fillMaxHeight().background(color))
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 12.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(DateTimeUtil.timeLabel(task.scheduledStart),
                    fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                    color = p.text, modifier = Modifier.width(46.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        DateTimeUtil.dayLabel(task.scheduledStart) +
                            " · Deadline " + DateTimeUtil.timeLabel(task.deadline) +
                            (task.recurrenceEnum.badge?.let { " · $it" } ?: ""),
                        fontSize = 12.sp, color = p.dim, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                StatusSymbol(task)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, null, tint = p.dim, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/* ---------------- kartu daftar Tugas & Riwayat ----------------
 * Persis web (tab Tugas): tinted 10% warna status, stripe kiri 12.dp,
 * isi padding 16.dp, chip di atas, judul 16.sp ExtraBold, lalu tombol aksi
 * sesuai status (bukan hanya bisa di-tap seperti versi Android lama). */
@Composable
fun TaskListCard(
    task: TaskEntity,
    onOpenFocus: (TaskEntity) -> Unit,
    onStart: (TaskEntity) -> Unit = {},
    onEdit: (TaskEntity) -> Unit = {},
    onDelete: (TaskEntity) -> Unit = {},
    onCancel: (TaskEntity) -> Unit = {},
    hasOtherActive: Boolean = false,
    showActions: Boolean = true,
    modifier: Modifier = Modifier
) {
    val p = LocalWebPalette.current
    val color = statusColor(task, p)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = lerp(p.surface, color, 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(12.dp).fillMaxHeight().background(color))
            Column(Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(task)
                    if (task.isActive && task.isOverdue()) {
                        Spacer(Modifier.width(8.dp)); LateBadge()
                    }
                }
                // Catatan mulai-terlambat di baris sendiri (dulu sebaris dengan
                // chip+badge -> terhimpit & membungkus per huruf, bug yang sama
                // seperti di Beranda/Halaman Fokus).
                if (task.startedLate && task.statusEnum != TaskStatus.PENDING) {
                    Spacer(Modifier.height(6.dp))
                    Text("Mulai terlambat dari jadwal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = p.red)
                }
                Spacer(Modifier.height(8.dp))
                Text(task.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = p.text, maxLines = 2)
                if (task.description.isNotBlank()) {
                    Text(task.description, fontSize = 14.sp, color = p.dim)
                }
                ScheduleLine(task)
                if (task.statusEnum == TaskStatus.COMPLETED || task.statusEnum == TaskStatus.CANCELLED) {
                    Text(
                        "Fokus " + DateTimeUtil.durationWords(task.focusAccumulatedMs) +
                            " · Istirahat " + DateTimeUtil.durationWords(task.breakAccumulatedMs) +
                            (task.completedAt?.let { " · Berakhir " + DateTimeUtil.timeLabel(it) } ?: ""),
                        fontSize = 14.sp, color = p.dim
                    )
                }
                if (showActions) {
                    Spacer(Modifier.height(12.dp))
                    ActionButtons(task, onOpenFocus, onStart, onEdit, onDelete, onCancel, hasOtherActive)
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    task: TaskEntity,
    onOpenFocus: (TaskEntity) -> Unit,
    onStart: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onCancel: (TaskEntity) -> Unit,
    hasOtherActive: Boolean
) {
    val p = LocalWebPalette.current
    when (task.statusEnum) {
        TaskStatus.PENDING -> Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Fokus", { onOpenFocus(task) }, Modifier.weight(1f), icon = Icons.Default.GpsFixed, textSize = 14)
                Surface2Button("Mulai", { onStart(task) }, Modifier.weight(1f), icon = Icons.Default.PlayArrow, enabled = !hasOtherActive)
            }
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Surface2Button("Ubah", { onEdit(task) }, Modifier.weight(1f), icon = Icons.Default.Edit)
                DeleteOutlineButton("Hapus", { onDelete(task) }, Modifier.weight(1f))
            }
            if (hasOtherActive) Text("Selesaikan tugas aktif dulu untuk memulai yang lain.", fontSize = 12.sp, color = p.dim)
        }
        TaskStatus.IN_PROGRESS, TaskStatus.ON_BREAK -> Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            PrimaryButton("Lihat Timer Fokus", { onOpenFocus(task) }, Modifier.weight(1f), icon = Icons.Default.GpsFixed, textSize = 14)
            WebButton("Batalkan", { onCancel(task) }, Modifier.weight(0.6f), bg = p.redBg, fg = p.red, textSize = 14)
        }
        TaskStatus.COMPLETED, TaskStatus.CANCELLED -> Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            Surface2Button("Fokus", { onOpenFocus(task) }, Modifier.weight(1f), icon = Icons.Default.GpsFixed)
            Surface2Button("Hapus dari Riwayat", { onDelete(task) }, Modifier.weight(1.4f), icon = Icons.Default.Delete, fg = p.dim)
        }
    }
}

/** Nama lama yang dipakai layar lain: sekarang mengarah ke kartu daftar web. */
@Composable
fun TaskCard(task: TaskEntity, onTap: () -> Unit, modifier: Modifier = Modifier) {
    TaskListCard(task = task, onOpenFocus = { onTap() }, showActions = false, modifier = modifier)
}
