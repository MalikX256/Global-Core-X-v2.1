package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SleekBlue,
    onPrimary = Color.White,
    primaryContainer = SleekZinc800,
    onPrimaryContainer = SleekZinc100,
    secondary = SleekGreen,
    onSecondary = Color.Black,
    secondaryContainer = SleekGreenMuted,
    onSecondaryContainer = SleekGreen,
    tertiary = SleekPurple,
    onTertiary = Color.White,
    background = SleekBlack,
    onBackground = SleekZinc100,
    surface = SleekZinc900,
    onSurface = SleekZinc100,
    surfaceVariant = SleekZinc900,
    onSurfaceVariant = SleekZinc400,
    outline = SleekZinc800,
    error = SleekSosRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = Color(0xFF6366F1),
    onTertiary = Color.White,
    background = TechWhite,
    onBackground = TextPrimaryLight,
    surface = TechSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = TechCardLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = TechCardBorderLight,
    error = SosRed,
    onError = Color.White
)

@Composable
fun GlobalCoreXTheme(
    darkTheme: Boolean = true, // default to premium dark
    dynamicColor: Boolean = false, // Keep branded high-tech styling
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
