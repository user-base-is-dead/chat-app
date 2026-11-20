package com.meghnathnews.mironchat.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeChoice(val label: String) {
    System("System"),
    Dark("Dark"),
    Light("Light"),
}

data class AppColor(val id: String, val label: String, val value: Color)

val appColors = listOf(
    AppColor("default", "Default", Color(0xFF3A8FE6)),
)

private class Palette(
    val base: Color,
    val sheet: Color,
    val sheetTint: Float,
    val raised: Color,
    val canvasTint: Float,
    val surfaceTint: Float,
    val strongTint: Float,
    val dockTint: Float,
    val bubbleTint: Float,
    val ink: Color,
    val inkMuted: Color,
    val mutedIcon: Color,
    val mutedBadge: Color,
    val border: Color,
    val borderStrong: Color,
    val bubbleMuted: Color,
    val danger: Color,
    val accentLift: Float,
)

private val lightPalette = Palette(
    base = Color(0xFFECECF1),
    sheet = Color(0xFFFFFFFF),
    sheetTint = 0f,
    raised = Color(0xFFF2F2F7),
    canvasTint = 0f,
    surfaceTint = 0f,
    strongTint = 0f,
    dockTint = 0f,
    bubbleTint = 0f,
    ink = Color(0xFF111114),
    inkMuted = Color(0xFF6C6C72),
    mutedIcon = Color(0xFFAEAEB2),
    mutedBadge = Color(0xFFC7C7CC),
    border = Color(0x333C3C43),
    borderStrong = Color(0x573C3C43),
    bubbleMuted = Color(0xFF8E8E93),
    danger = Color(0xFFE5484D),
    accentLift = 0f,
)

private val darkPalette = Palette(
    base = Color(0xFF000000),
    sheet = Color(0xFF1C1C1E),
    sheetTint = 0f,
    raised = Color(0xFF2C2C2E),
    canvasTint = 0f,
    surfaceTint = 0f,
    strongTint = 0f,
    dockTint = 0f,
    bubbleTint = 0f,
    ink = Color(0xFFF5F5F7),
    inkMuted = Color(0xFF98989F),
    mutedIcon = Color(0xFF636366),
    mutedBadge = Color(0xFF3A3A3C),
    border = Color(0x24FFFFFF),
    borderStrong = Color(0x42FFFFFF),
    bubbleMuted = Color(0xFF98989F),
    danger = Color(0xFFF2555A),
    accentLift = 0.22f,
)

object Appearance {
    private const val PREFS = "appearance"
    private const val THEME = "theme"
    private const val COLOR = "color"

    private var systemDark by mutableStateOf(false)

    var theme by mutableStateOf(ThemeChoice.Light)
        private set
    var color by mutableStateOf(appColors.first())
        private set

    val dark: Boolean
        get() = theme == ThemeChoice.Dark || (theme == ThemeChoice.System && systemDark)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        systemDark = nightMask == Configuration.UI_MODE_NIGHT_YES
        theme = ThemeChoice.entries.firstOrNull { it.name == prefs.getString(THEME, null) } ?: ThemeChoice.Light
        color = appColors.firstOrNull { it.id == prefs.getString(COLOR, null) } ?: appColors.first()
    }

    fun chooseTheme(context: Context, choice: ThemeChoice) {
        theme = choice
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(THEME, choice.name).apply()
    }

    fun chooseColor(context: Context, choice: AppColor) {
        color = choice
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(COLOR, choice.id).apply()
    }

    fun followSystem(isDark: Boolean) {
        systemDark = isDark
    }
}

private val palette: Palette get() = if (Appearance.dark) darkPalette else lightPalette

val Chroma: Color get() = Appearance.color.value
val Canvas: Color get() = lerp(palette.base, Chroma, palette.canvasTint)
val Panel: Color get() = lerp(palette.sheet, Chroma, palette.sheetTint)
val Card: Color get() = Panel
val Surface: Color get() = lerp(palette.raised, Chroma, palette.surfaceTint)
val SurfaceStrong: Color get() = if (Appearance.dark) Color(0xFF3A3A3C) else Color(0xFFDFDFE6)
val Ink: Color get() = palette.ink
val InkMuted: Color get() = palette.inkMuted
val MutedIcon: Color get() = palette.mutedIcon
val MutedBadge: Color get() = palette.mutedBadge
val BorderStrong: Color get() = palette.borderStrong
val BubbleBorder: Color get() = palette.border
val BubbleOther: Color get() = if (Appearance.dark) Color(0xFF2A2A2F) else Color.White
val BubbleMuted: Color get() = palette.bubbleMuted
val AvatarBlank: Color get() = if (Appearance.dark) Color(0xFF4A4A51) else Color(0xFFC2C7CF)
val AvatarBlankIcon: Color get() = if (Appearance.dark) Color(0xFFA3A3AB) else Color.White
private val soft: Boolean get() = Appearance.color.id == "default"
val BubbleMine: Color get() = when {
    !soft -> Chroma
    Appearance.dark -> lerp(Color(0xFF1C1C1E), Chroma, 0.56f)
    else -> lerp(Color.White, Chroma, 0.24f)
}
val BubbleMineText: Color get() = if (soft && !Appearance.dark) palette.ink else Color.White
val BubbleMineMuted: Color get() = if (soft && !Appearance.dark) lerp(Chroma, Color.Black, 0.26f) else Color.White.copy(alpha = 0.76f)
val BubbleMineFailed: Color get() = if (soft && !Appearance.dark) palette.danger else Color(0xFFFFD3D6)
val Danger: Color get() = palette.danger
val Dock: Color get() = if (Appearance.dark) Color(0xFF1C1C1E) else Color(0xFFF9F9F9)
val DockAccent: Color get() = lerp(Chroma, Color.White, palette.accentLift)
val DockPill: Color get() = Chroma.copy(alpha = if (Appearance.dark) 0.24f else 0.14f)
val Fab: Color get() = Chroma
val OnFab: Color get() = Color.White
val Stage: Color get() = if (Appearance.dark) Color(0xFF0B141A) else Color(0xFFEFEAE2)
val DoodleInk: Color get() = if (Appearance.dark) Color(0xFF121C23) else Color(0xFFE4DED3)
val Cover: Color get() = Chroma

@Composable
fun MironTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    val systemDark = isSystemInDarkTheme()
    val dark = Appearance.dark

    SideEffect {
        Appearance.followSystem(systemDark)
        WindowCompat.getInsetsController((view.context as Activity).window, view)
            .isAppearanceLightStatusBars = !dark
    }

    val scheme = if (dark) {
        darkColorScheme(
            primary = Chroma,
            onPrimary = Color.White,
            background = Canvas,
            onBackground = Ink,
            surface = Panel,
            onSurface = Ink,
            onSurfaceVariant = InkMuted,
            surfaceContainerHigh = Surface,
            error = Danger,
        )
    } else {
        lightColorScheme(
            primary = Chroma,
            onPrimary = Color.White,
            background = Canvas,
            onBackground = Ink,
            surface = Panel,
            onSurface = Ink,
            onSurfaceVariant = InkMuted,
            surfaceContainerHigh = Panel,
            error = Danger,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private val roleLight = mapOf(
    "red" to Color(0xFFD93636),
    "orange" to Color(0xFFD9480F),
    "amber" to Color(0xFFB7791F),
    "yellow" to Color(0xFFA68B00),
    "lime" to Color(0xFF5C940D),
    "green" to Color(0xFF2B8A3E),
    "teal" to Color(0xFF099268),
    "cyan" to Color(0xFF0B7285),
    "blue" to Color(0xFF1C7ED6),
    "indigo" to Color(0xFF4263EB),
    "purple" to Color(0xFF8E3BC9),
    "pink" to Color(0xFFC2255C),
)

private val roleDark = mapOf(
    "red" to Color(0xFFFF6B6B),
    "orange" to Color(0xFFFF8A4C),
    "amber" to Color(0xFFF5B841),
    "yellow" to Color(0xFFF0D43A),
    "lime" to Color(0xFFA3E048),
    "green" to Color(0xFF51CF66),
    "teal" to Color(0xFF38D9A9),
    "cyan" to Color(0xFF3BC9DB),
    "blue" to Color(0xFF4DABF7),
    "indigo" to Color(0xFF8C9EFF),
    "purple" to Color(0xFFCC8CF0),
    "pink" to Color(0xFFF783AC),
)

fun roleColor(key: String?): Color? = key?.let { (if (Appearance.dark) roleDark else roleLight)[it] }
