package com.qrdatasender.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA7F3D0),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF4D6358),
    tertiary = Color(0xFF3E6373),
    background = Color(0xFFF5F9F7),
    surface = Color(0xFFF5F9F7),
    surfaceContainer = Color(0xFFE8EFEB),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF87D7B5),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF005139),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFB4CCBF),
    tertiary = Color(0xFFA5CDDF),
    background = Color(0xFF0F1512),
    surface = Color(0xFF0F1512),
    surfaceContainer = Color(0xFF1B211E),
    error = Color(0xFFFFB4AB)
)

@Composable
fun QrTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography(), content = content)
}
