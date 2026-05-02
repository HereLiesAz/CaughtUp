package com.hereliesaz.cleanunderwear.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00E5FF)
val DeepSlate = Color(0xFF0D1117)
val IntelligenceGold = Color(0xFFFFB300)
val UnverifiedAmber = Color(0xFFFF8A40)
val SurveillanceGrey = Color(0xFF1C1F26)
val CircuitBlue = Color(0xFF00838F)
val VerifiedGreen = Color(0xFF00E676)
val WarningRed = Color(0xFFFF5252)

val SurveillanceDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = CircuitBlue,
    tertiary = IntelligenceGold,
    background = DeepSlate,
    surface = SurveillanceGrey,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = WarningRed,
    primaryContainer = DeepSlate,
    onPrimaryContainer = NeonCyan,
    secondaryContainer = CircuitBlue.copy(alpha = 0.3f),
    onSecondaryContainer = Color.White,
    surfaceVariant = SurveillanceGrey,
    onSurfaceVariant = Color.LightGray,
    outline = CircuitBlue
)

val SurveillanceLightColorScheme = lightColorScheme(
    primary = CircuitBlue,
    secondary = NeonCyan,
    tertiary = IntelligenceGold,
    background = Color(0xFFF5F7F9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = DeepSlate,
    onSurface = DeepSlate,
    error = Color(0xFFD32F2F),
    primaryContainer = Color(0xFFF0F2F5),
    onPrimaryContainer = CircuitBlue,
    secondaryContainer = NeonCyan.copy(alpha = 0.1f),
    onSecondaryContainer = CircuitBlue,
    surfaceVariant = Color(0xFFE1E4E8),
    onSurfaceVariant = DeepSlate,
    outline = CircuitBlue
)
