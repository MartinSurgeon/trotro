package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Default Campaign Flyer Palette (Dark Green, White, and Red Accents)
val CampaignDarkGreen = Color(0xFF0B4F26)
val CampaignDeepGreen = Color(0xFF073619)
val CampaignLightGreenContainer = Color(0xFFDCEFE2)
val CampaignOnLightGreenContainer = Color(0xFF052912)

val CampaignRed = Color(0xFFD32F2F)
val CampaignDarkRed = Color(0xFFA61B1B)
val CampaignLightRedContainer = Color(0xFFFFDAD6)
val CampaignOnLightRedContainer = Color(0xFF410002)

val CampaignWhite = Color(0xFFFFFFFF)
val CampaignSurfaceBackground = Color(0xFFF6F9F6)
val CampaignCardSurface = Color(0xFFFFFFFF)
val CampaignTextDark = Color(0xFF111C15)
val CampaignTextMuted = Color(0xFF46564B)

data class CampaignColorPreset(
    val name: String,
    val primaryHex: String,
    val accentHex: String,
    val primaryColor: Color,
    val accentColor: Color
)

val CampaignColorPresets = listOf(
    CampaignColorPreset(
        name = "Flyer Green & Red",
        primaryHex = "#0B4F26",
        accentHex = "#D32F2F",
        primaryColor = Color(0xFF0B4F26),
        accentColor = Color(0xFFD32F2F)
    ),
    CampaignColorPreset(
        name = "Civic Navy & Red",
        primaryHex = "#0D3B66",
        accentHex = "#D62828",
        primaryColor = Color(0xFF0D3B66),
        accentColor = Color(0xFFD62828)
    ),
    CampaignColorPreset(
        name = "Church Purple & Crimson",
        primaryHex = "#3B1C71",
        accentHex = "#C62828",
        primaryColor = Color(0xFF3B1C71),
        accentColor = Color(0xFFc62828)
    ),
    CampaignColorPreset(
        name = "Union Forest & Amber",
        primaryHex = "#1B4332",
        accentHex = "#D97706",
        primaryColor = Color(0xFF1B4332),
        accentColor = Color(0xFFD97706)
    ),
    CampaignColorPreset(
        name = "Association Maroon & Gold",
        primaryHex = "#6B1D2F",
        accentHex = "#B45309",
        primaryColor = Color(0xFF6B1D2F),
        accentColor = Color(0xFFB45309)
    )
)

fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        when (cleaned.length) {
            6 -> Color(("FF$cleaned").toLong(16))
            8 -> Color(cleaned.toLong(16))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
