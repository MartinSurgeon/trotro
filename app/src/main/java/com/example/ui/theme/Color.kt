package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

// Ghana flag colours, used as the default brand and for the kente accent stripe.
val GhanaGreen = Color(0xFF006B3F)
val GhanaGold = Color(0xFFFCD116)
val GhanaRed = Color(0xFFCE1126)
val KenteBlack = Color(0xFF1A1A1A)

data class ColourPreset(
    val name: String,
    val primaryHex: String,
    val accentHex: String
) {
    val primary: Color get() = parseHexColor(primaryHex, GhanaGreen)
    val accent: Color get() = parseHexColor(accentHex, GhanaGold)
}

val ColourPresets = listOf(
    ColourPreset("Ghana green", "#006B3F", "#FCD116"),
    ColourPreset("Royal blue", "#1E3A8A", "#F59E0B"),
    ColourPreset("Church purple", "#5B2A86", "#D4A017"),
    ColourPreset("Maroon", "#7A1F2B", "#E0A526"),
    ColourPreset("Teal", "#00695C", "#FFB300"),
    ColourPreset("Charcoal", "#263238", "#26A69A")
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

/** Black or white, whichever reads better on [background]. */
fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.42f) Color(0xFF111411) else Color.White

/** Lightens dark brand colours so they stay visible on a dark background. */
fun brandForDarkTheme(color: Color): Color =
    if (color.luminance() < 0.25f) lerp(color, Color.White, 0.45f) else color

/** Darkens light brand colours (e.g. gold) so they read as text on a light background. */
fun brandForLightText(color: Color): Color =
    if (color.luminance() > 0.35f) lerp(color, Color.Black, 0.45f) else color

/** Semantic colours for message status, tuned for both themes. */
data class StatusColors(
    val success: Color,
    val progress: Color,
    val warning: Color,
    val error: Color,
    val neutral: Color
)

val LightStatusColors = StatusColors(
    success = Color(0xFF1B7F3B),
    progress = Color(0xFF1565C0),
    warning = Color(0xFFB45309),
    error = Color(0xFFC62828),
    neutral = Color(0xFF5F6368)
)

val DarkStatusColors = StatusColors(
    success = Color(0xFF7BD88F),
    progress = Color(0xFF90CAF9),
    warning = Color(0xFFFFB74D),
    error = Color(0xFFFF8A80),
    neutral = Color(0xFFB0B5BA)
)
