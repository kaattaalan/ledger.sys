package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RetroWhite,
    onPrimary = RetroBlack,
    secondary = RetroMutedGray,
    onSecondary = RetroWhite,
    background = RetroBlack,
    onBackground = RetroWhite,
    surface = RetroBlack,
    onSurface = RetroWhite,
    surfaceVariant = RetroMutedDarkGray,
    onSurfaceVariant = RetroWhite,
    outline = RetroWhite,
    error = RetroRed,
    onError = RetroWhite
)

private val LightColorScheme = lightColorScheme(
    primary = RetroBlack,
    onPrimary = RetroWhite,
    secondary = RetroMutedGray,
    onSecondary = RetroBlack,
    background = RetroWhite,
    onBackground = RetroBlack,
    surface = RetroWhite,
    onSurface = RetroBlack,
    surfaceVariant = RetroMutedLightGray,
    onSurfaceVariant = RetroBlack,
    outline = RetroBlack,
    error = RetroRed,
    onError = RetroWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
