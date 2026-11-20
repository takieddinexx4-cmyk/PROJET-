package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class GlassPalette(
    val mode: GlassThemeMode,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val orbTopStart: Color,
    val orbBottomEnd: Color,
    val orbCenter: Color,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassBorderStart: Color,
    val glassBorderMid: Color,
    val glassBorderEnd: Color,
    val frostAlpha: Float
)

val LocalGlassPalette = staticCompositionLocalOf {
    buildGlassPalette(GlassThemeMode.DUAL_PRISM, 0.72f)
}

fun buildGlassPalette(mode: GlassThemeMode, frostIntensity: Float): GlassPalette {
    val clampedFrost = frostIntensity.coerceIn(0.35f, 1.0f)
    return when (mode) {
        GlassThemeMode.DUAL_PRISM -> GlassPalette(
            mode = mode,
            primaryAccent = CrimsonGlow,
            secondaryAccent = SapphireElectric,
            orbTopStart = CrimsonMuted.copy(alpha = 0.28f),
            orbBottomEnd = SapphireElectric.copy(alpha = 0.26f),
            orbCenter = CrimsonDeep.copy(alpha = 0.16f),
            glassFillTop = Color.White.copy(alpha = 0.11f * clampedFrost),
            glassFillBottom = Color(0xFF141826).copy(alpha = 0.62f * clampedFrost + 0.18f),
            glassBorderStart = Color.White.copy(alpha = 0.30f),
            glassBorderMid = SapphireGlow.copy(alpha = 0.22f),
            glassBorderEnd = CrimsonGlow.copy(alpha = 0.24f),
            frostAlpha = clampedFrost
        )

        GlassThemeMode.CRIMSON_OBSIDIAN -> GlassPalette(
            mode = mode,
            primaryAccent = CrimsonGlow,
            secondaryAccent = CrimsonMuted,
            orbTopStart = CrimsonMuted.copy(alpha = 0.32f),
            orbBottomEnd = CrimsonDeep.copy(alpha = 0.28f),
            orbCenter = CrimsonGlow.copy(alpha = 0.12f),
            glassFillTop = Color(0xFFFFD8DF).copy(alpha = 0.10f * clampedFrost),
            glassFillBottom = Color(0xFF180E14).copy(alpha = 0.66f * clampedFrost + 0.16f),
            glassBorderStart = Color.White.copy(alpha = 0.28f),
            glassBorderMid = CrimsonGlow.copy(alpha = 0.28f),
            glassBorderEnd = CrimsonDeep.copy(alpha = 0.24f),
            frostAlpha = clampedFrost
        )

        GlassThemeMode.SAPPHIRE_OBSIDIAN -> GlassPalette(
            mode = mode,
            primaryAccent = SapphireElectric,
            secondaryAccent = SapphireGlow,
            orbTopStart = SapphireElectric.copy(alpha = 0.30f),
            orbBottomEnd = SapphireDeep.copy(alpha = 0.30f),
            orbCenter = SapphireGlow.copy(alpha = 0.12f),
            glassFillTop = Color(0xFFD6E5FF).copy(alpha = 0.10f * clampedFrost),
            glassFillBottom = Color(0xFF0E1424).copy(alpha = 0.66f * clampedFrost + 0.16f),
            glassBorderStart = Color.White.copy(alpha = 0.28f),
            glassBorderMid = SapphireGlow.copy(alpha = 0.28f),
            glassBorderEnd = SapphireDeep.copy(alpha = 0.24f),
            frostAlpha = clampedFrost
        )
    }
}

@Composable
fun DzCastTheme(
    themeMode: GlassThemeMode = GlassThemeMode.DUAL_PRISM,
    frostIntensity: Float = 0.72f,
    content: @Composable () -> Unit
) {
    val glassPalette = buildGlassPalette(themeMode, frostIntensity)

    val colorScheme = darkColorScheme(
        primary = glassPalette.primaryAccent,
        onPrimary = Color.White,
        primaryContainer = when (themeMode) {
            GlassThemeMode.SAPPHIRE_OBSIDIAN -> SapphireSoftTint
            else -> CrimsonSoftTint
        },
        onPrimaryContainer = GlassTextPrimary,
        secondary = glassPalette.secondaryAccent,
        onSecondary = Color.White,
        secondaryContainer = SapphireSoftTint,
        onSecondaryContainer = GlassTextPrimary,
        tertiary = SapphireGlow,
        background = ObsidianDeep,
        onBackground = GlassTextPrimary,
        surface = ObsidianSurface,
        onSurface = GlassTextPrimary,
        surfaceVariant = ObsidianElevated,
        onSurfaceVariant = GlassTextSecondary,
        outline = GlassWhiteMid,
        outlineVariant = GlassWhiteLow,
        error = StatusErrorRose,
        onError = Color.White
    )

    CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DzCastTypography,
            content = content
        )
    }
}
