package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Obsidian Base Palette
val ObsidianDeep = Color(0xFF06070B)
val ObsidianSurface = Color(0xFF0E1018)
val ObsidianElevated = Color(0xFF151824)
val ObsidianCardBase = Color(0xFF11141F)

// Muted Crimson Accents (أحمر خافت راقٍ)
val CrimsonMuted = Color(0xFFC82E47)
val CrimsonGlow = Color(0xFFE0425B)
val CrimsonDeep = Color(0xFF7A1729)
val CrimsonSoftTint = Color(0xFF331119)

// Sapphire Midnight Blue Accents (أزرق زجاجي ليلي)
val SapphireElectric = Color(0xFF2F6BFF)
val SapphireGlow = Color(0xFF5D92FF)
val SapphireDeep = Color(0xFF15327A)
val SapphireSoftTint = Color(0xFF111D38)

// Text & Glass Highlights
val GlassTextPrimary = Color(0xFFF4F6FB)
val GlassTextSecondary = Color(0xFFA9B2C8)
val GlassTextMuted = Color(0xFF6E7790)

val GlassWhiteHigh = Color(0x3DFFFFFF)
val GlassWhiteMid = Color(0x1FFFFFFF)
val GlassWhiteLow = Color(0x0DFFFFFF)
val GlassWhiteSubtle = Color(0x08FFFFFF)

// Status Semantic Colors
val StatusLiveEmerald = Color(0xFF22C55E)
val StatusWarningAmber = Color(0xFFF59E0B)
val StatusErrorRose = Color(0xFFEF4444)

enum class GlassThemeMode(val labelAr: String, val subtitleAr: String) {
    DUAL_PRISM("مزيج أحمر وأزرق", "تدرج زجاجي فاخر يجمع الأحمر الخافت والأزرق الليلي"),
    CRIMSON_OBSIDIAN("أسود وأحمر خافت", "توهج ياقوتي هادئ على خلفية سوداء داكنة"),
    SAPPHIRE_OBSIDIAN("أسود وأزرق زجاجي", "إضاءة زرقاء باردة بطابع iOS 27 الزجاجي")
}
