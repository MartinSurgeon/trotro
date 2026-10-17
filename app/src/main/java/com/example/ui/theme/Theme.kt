package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

object DawuroTheme {
    val statusColors: StatusColors
        @Composable @ReadOnlyComposable
        get() = LocalStatusColors.current
}

@Composable
fun DawuroTheme(
    primaryColorHex: String = "#006B3F",
    accentColorHex: String = "#FCD116",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val brand = parseHexColor(primaryColorHex, GhanaGreen)
    val accent = parseHexColor(accentColorHex, GhanaGold)

    val colorScheme = remember(brand, accent, darkTheme) {
        if (darkTheme) darkScheme(brand, accent) else lightScheme(brand, accent)
    }

    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun lightScheme(brand: Color, accent: Color) = lightColorScheme(
    primary = brand,
    onPrimary = contentColorFor(brand),
    primaryContainer = lerp(brand, Color.White, 0.85f),
    onPrimaryContainer = lerp(brand, Color.Black, 0.55f),
    secondary = accent,
    onSecondary = contentColorFor(accent),
    secondaryContainer = lerp(accent, Color.White, 0.75f),
    onSecondaryContainer = lerp(accent, Color.Black, 0.65f),
    tertiary = brandForLightText(accent),
    onTertiary = contentColorFor(brandForLightText(accent)),
    background = Color(0xFFF7F8F6),
    onBackground = Color(0xFF151815),
    surface = Color(0xFFF7F8F6),
    onSurface = Color(0xFF151815),
    surfaceVariant = Color(0xFFE6E9E5),
    onSurfaceVariant = Color(0xFF4A504B),
    surfaceTint = brand,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDDE0DC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F2EF),
    surfaceContainerHigh = Color(0xFFEAEDE9),
    surfaceContainerHighest = Color(0xFFE4E7E3),
    outline = Color(0xFF79807A),
    outlineVariant = Color(0xFFD3D8D2),
    inverseSurface = Color(0xFF2B2F2B),
    inverseOnSurface = Color(0xFFEFF1EE),
    inversePrimary = brandForDarkTheme(brand),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF5F1111)
)

private fun darkScheme(brand: Color, accent: Color): androidx.compose.material3.ColorScheme {
    val darkBrand = brandForDarkTheme(brand)
    return darkColorScheme(
        primary = darkBrand,
        onPrimary = contentColorFor(darkBrand),
        primaryContainer = lerp(brand, Color.Black, 0.45f),
        onPrimaryContainer = lerp(darkBrand, Color.White, 0.75f),
        secondary = accent,
        onSecondary = contentColorFor(accent),
        secondaryContainer = lerp(accent, Color.Black, 0.65f),
        onSecondaryContainer = lerp(accent, Color.White, 0.7f),
        tertiary = accent,
        onTertiary = contentColorFor(accent),
        background = Color(0xFF111311),
        onBackground = Color(0xFFE3E6E2),
        surface = Color(0xFF111311),
        onSurface = Color(0xFFE3E6E2),
        surfaceVariant = Color(0xFF3F4540),
        onSurfaceVariant = Color(0xFFBFC6BF),
        surfaceTint = darkBrand,
        surfaceBright = Color(0xFF373A37),
        surfaceDim = Color(0xFF111311),
        surfaceContainerLowest = Color(0xFF0C0E0C),
        surfaceContainerLow = Color(0xFF1A1C1A),
        surfaceContainer = Color(0xFF1E201E),
        surfaceContainerHigh = Color(0xFF282B28),
        surfaceContainerHighest = Color(0xFF333633),
        outline = Color(0xFF899089),
        outlineVariant = Color(0xFF3F4540),
        inverseSurface = Color(0xFFE3E6E2),
        inverseOnSurface = Color(0xFF2B2F2B),
        inversePrimary = brand,
        error = Color(0xFFFF8A80),
        onError = Color(0xFF3B0000),
        errorContainer = Color(0xFF5F1111),
        onErrorContainer = Color(0xFFFDE7E7)
    )
}
