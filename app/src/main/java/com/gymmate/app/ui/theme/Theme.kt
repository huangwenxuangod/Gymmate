package com.gymmate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FD3A8),
    secondary = Color(0xFF86B8A1),
    tertiary = Color(0xFFE3C47A),
    background = Color(0xFF101412),
    surface = Color(0xFF171C19),
    surfaceVariant = Color(0xFF222925),
    onPrimary = Color(0xFF0A130D),
    onBackground = Color(0xFFF3F6F3),
    onSurface = Color(0xFFF3F6F3),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF295C37),
    secondary = Color(0xFF446452),
    tertiary = Color(0xFF8C6A23),
    background = Color(0xFFF7F8F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8ECE7),
    onPrimary = Color.White,
    onBackground = Color(0xFF121512),
    onSurface = Color(0xFF121512),
)

@Composable
fun GymmateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

