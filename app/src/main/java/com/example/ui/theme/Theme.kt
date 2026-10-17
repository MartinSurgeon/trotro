package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CampaignSmsTheme(
    primaryColorHex: String = "#0B4F26",
    accentColorHex: String = "#D32F2F",
    content: @Composable () -> Unit
) {
    val primaryColor = parseHexColor(primaryColorHex, CampaignDarkGreen)
    val accentColor = parseHexColor(accentColorHex, CampaignRed)

    val colorScheme = lightColorScheme(
        primary = primaryColor,
        onPrimary = CampaignWhite,
        primaryContainer = primaryColor.copy(alpha = 0.12f),
        onPrimaryContainer = primaryColor,
        secondary = accentColor,
        onSecondary = CampaignWhite,
        secondaryContainer = accentColor.copy(alpha = 0.12f),
        onSecondaryContainer = accentColor,
        tertiary = primaryColor,
        onTertiary = CampaignWhite,
        background = CampaignSurfaceBackground,
        onBackground = CampaignTextDark,
        surface = CampaignCardSurface,
        onSurface = CampaignTextDark,
        surfaceVariant = Color(0xFFEAF1EC),
        onSurfaceVariant = CampaignTextMuted,
        error = Color(0xFFB3261E),
        onError = CampaignWhite
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
