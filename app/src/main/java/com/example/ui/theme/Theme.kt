package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.WallpaperTheme

data class TurboAccentPalette(
    val accent: Color = CrimsonTurbo,
    val accentBright: Color = CrimsonBright,
    val accentDeep: Color = CrimsonDeep,
    val secondaryGlow: Color = ElectricCyan
)

val LocalTurboAccent = staticCompositionLocalOf { TurboAccentPalette() }

val TurboShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

fun accentPaletteForTheme(theme: WallpaperTheme): TurboAccentPalette = when (theme) {
    WallpaperTheme.OBSIDIAN_CRIMSON -> TurboAccentPalette(
        accent = CrimsonTurbo,
        accentBright = CrimsonBright,
        accentDeep = CrimsonDeep,
        secondaryGlow = ElectricCyan
    )
    WallpaperTheme.CYBER_AMBER -> TurboAccentPalette(
        accent = CyberAmber,
        accentBright = Color(0xFFFFD54F),
        accentDeep = CyberAmberDeep,
        secondaryGlow = CrimsonTurbo
    )
    WallpaperTheme.ELECTRIC_CYAN -> TurboAccentPalette(
        accent = ElectricCyan,
        accentBright = Color(0xFF6EFFFF),
        accentDeep = ElectricCyanDeep,
        secondaryGlow = Color(0xFF536DFE)
    )
    WallpaperTheme.STEALTH_CARBON -> TurboAccentPalette(
        accent = Color(0xFFD9E3F5),
        accentBright = Color(0xFFFFFFFF),
        accentDeep = Color(0xFF475470),
        secondaryGlow = ElectricCyan
    )
    WallpaperTheme.CUSTOM -> TurboAccentPalette(
        accent = CrimsonTurbo,
        accentBright = CrimsonBright,
        accentDeep = CrimsonDeep,
        secondaryGlow = ElectricCyan
    )
}

@Composable
fun TurboGameXTheme(
    wallpaperTheme: WallpaperTheme = WallpaperTheme.OBSIDIAN_CRIMSON,
    content: @Composable () -> Unit
) {
    val target = accentPaletteForTheme(wallpaperTheme)
    val animSpec = tween<Color>(durationMillis = 360)
    val accent by animateColorAsState(target.accent, animSpec, label = "theme_accent")
    val accentBright by animateColorAsState(target.accentBright, animSpec, label = "theme_accent_bright")
    val accentDeep by animateColorAsState(target.accentDeep, animSpec, label = "theme_accent_deep")
    val secondaryGlow by animateColorAsState(target.secondaryGlow, animSpec, label = "theme_secondary")

    val palette = TurboAccentPalette(
        accent = accent,
        accentBright = accentBright,
        accentDeep = accentDeep,
        secondaryGlow = secondaryGlow
    )

    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = Color.White,
        primaryContainer = palette.accentDeep,
        onPrimaryContainer = TextPrimary,
        secondary = palette.secondaryGlow,
        onSecondary = ObsidianVoid,
        tertiary = EmeraldActive,
        onTertiary = Color.White,
        background = ObsidianVoid,
        onBackground = TextPrimary,
        surface = MidnightSurface,
        onSurface = TextPrimary,
        surfaceVariant = CyberSlate,
        onSurfaceVariant = TextSecondary,
        outline = CyberBorder,
        error = CrimsonBright,
        onError = Color.White
    )

    CompositionLocalProvider(LocalTurboAccent provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TurboTypography,
            shapes = TurboShapes,
            content = content
        )
    }
}
