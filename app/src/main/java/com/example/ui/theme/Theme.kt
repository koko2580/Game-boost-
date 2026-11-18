package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.WallpaperTheme

data class TurboAccentPalette(
    val accent: Color = CrimsonTurbo,
    val accentBright: Color = CrimsonBright,
    val accentDeep: Color = CrimsonDeep,
    val secondaryGlow: Color = CyberAmber
)

val LocalTurboAccent = staticCompositionLocalOf { TurboAccentPalette() }

fun accentPaletteForTheme(theme: WallpaperTheme): TurboAccentPalette = when (theme) {
    WallpaperTheme.OBSIDIAN_CRIMSON -> TurboAccentPalette(
        accent = CrimsonTurbo,
        accentBright = CrimsonBright,
        accentDeep = CrimsonDeep,
        secondaryGlow = CyberAmber
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
        secondaryGlow = Color(0xFF448AFF)
    )
    WallpaperTheme.STEALTH_CARBON -> TurboAccentPalette(
        accent = Color(0xFFD9E1F2),
        accentBright = Color(0xFFFFFFFF),
        accentDeep = Color(0xFF4B5570),
        secondaryGlow = CrimsonTurbo
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
    val palette = accentPaletteForTheme(wallpaperTheme)
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
            content = content
        )
    }
}
