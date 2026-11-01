package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElectricThemeColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = Color(0xFF04131D),
    primaryContainer = Color(0xFF083344),
    onPrimaryContainer = CyanGlow,
    secondary = AmberElectric,
    onSecondary = Color(0xFF1C1300),
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = AmberGlow,
    background = ElectricBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = ElectricSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = ElectricSurfaceCard,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = StatusOutage,
    onError = Color.White
)

private val HighContrastLightScheme = lightColorScheme(
    primary = LightPrimaryBrand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = AmberElectric,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightCard,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardElevated,
    onSurfaceVariant = LightTextSecondary,
    error = StatusOutage,
    onError = Color.White
)

@Composable
fun GranmaElectricaTheme(
    darkTheme: Boolean = true, // Default to electric dark theme for high-voltage visual identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ElectricThemeColorScheme else HighContrastLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
