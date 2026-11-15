package com.gusti.personallifetodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Palet PERSIS versi web (client/src/theme.css di Muse):
 * terang  --bg #EEF2F9, --surface #FFF, --surface2 #E3EAF6, --text #14213D,
 *         --dim #5B6B8C, --border #D3DCEE, --accent #2563EB,
 *         --red-bg #FDECEC, --green-bg #E7F7EE, --blue-bg #E3ECFD,
 *         --amber #B45309, --amber-bg #FDF3E0.
 * gelap   --bg #0A1122, --surface #131F3A, --surface2 #1B2C50,
 *         --text #EAF0FD, --dim #93A5C9, --border #27395F, --accent #5B8CFF,
 *         --accent-ink #081226, --red #F87171, --red-bg #3D1A1E,
 *         --green #4ADE80, --green-bg #123324, --blue-bg #16294F,
 *         --amber #FBBF24, --amber-bg #3A2C10.
 * Warna status solid sama di dua tema (web): hijau #15803D, abu #475569,
 * biru #2563EB, merah #DC2626; merah tua terlambat #991B1B (terang) /
 * #B91C1C (gelap). Dynamic color Android DIMATIKAN: di versi lama dynamic
 * color menimpa palet ini sehingga tampilan Android menyimpang dari web.
 */
data class WebPalette(
    val isDark: Boolean,
    val bg: Color, val surface: Color, val surface2: Color,
    val text: Color, val dim: Color, val border: Color,
    val accent: Color, val accentInk: Color,
    val red: Color, val redBg: Color,
    val green: Color, val greenBg: Color,
    val blueBg: Color, val amber: Color, val amberBg: Color,
    val statusOverdueDark: Color
)

val LightPalette = WebPalette(
    isDark = false,
    bg = Color(0xFFEEF2F9), surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE3EAF6), text = Color(0xFF14213D),
    dim = Color(0xFF5B6B8C), border = Color(0xFFD3DCEE),
    accent = Color(0xFF2563EB), accentInk = Color(0xFFFFFFFF),
    red = Color(0xFFDC2626), redBg = Color(0xFFFDECEC),
    green = Color(0xFF16A34A), greenBg = Color(0xFFE7F7EE),
    blueBg = Color(0xFFE3ECFD), amber = Color(0xFFB45309),
    amberBg = Color(0xFFFDF3E0), statusOverdueDark = Color(0xFF991B1B)
)

val DarkPalette = WebPalette(
    isDark = true,
    bg = Color(0xFF0A1122), surface = Color(0xFF131F3A),
    surface2 = Color(0xFF1B2C50), text = Color(0xFFEAF0FD),
    dim = Color(0xFF93A5C9), border = Color(0xFF27395F),
    accent = Color(0xFF5B8CFF), accentInk = Color(0xFF081226),
    red = Color(0xFFF87171), redBg = Color(0xFF3D1A1E),
    green = Color(0xFF4ADE80), greenBg = Color(0xFF123324),
    blueBg = Color(0xFF16294F), amber = Color(0xFFFBBF24),
    amberBg = Color(0xFF3A2C10), statusOverdueDark = Color(0xFFB91C1C)
)

val LocalWebPalette = staticCompositionLocalOf { LightPalette }

/** Warna status solid versi web (statusSolidVar di App.tsx web). */
object StatusColors {
    val PendingRed = Color(0xFFDC2626)
    val CompletedGreen = Color(0xFF15803D)
    val InProgressBlue = Color(0xFF2563EB)
    val CancelledGrey = Color(0xFF475569)
}

/* Web memakai Plus Jakarta Sans. Font itu tidak dibundel (file font tidak
 * tersedia offline di proyek ini), jadi dipakai SansSerif bawaan Android
 * dengan bobot yang sama (ExtraBold untuk judul/tombol, seperti web). */
private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp)
)

enum class ThemeChoice(val label: String) { SYSTEM("Sistem"), LIGHT("Terang"), DARK("Gelap") }

@Composable
fun PersonalLifeTheme(choice: ThemeChoice = ThemeChoice.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (choice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    val palette = if (dark) DarkPalette else LightPalette
    val colors = if (dark) darkColorScheme(
        primary = palette.accent, onPrimary = palette.accentInk,
        background = palette.bg, onBackground = palette.text,
        surface = palette.surface, onSurface = palette.text,
        surfaceVariant = palette.surface2, onSurfaceVariant = palette.dim,
        outline = palette.border, error = palette.red
    ) else lightColorScheme(
        primary = palette.accent, onPrimary = palette.accentInk,
        background = palette.bg, onBackground = palette.text,
        surface = palette.surface, onSurface = palette.text,
        surfaceVariant = palette.surface2, onSurfaceVariant = palette.dim,
        outline = palette.border, error = palette.red
    )
    CompositionLocalProvider(LocalWebPalette provides palette) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
    }
}
