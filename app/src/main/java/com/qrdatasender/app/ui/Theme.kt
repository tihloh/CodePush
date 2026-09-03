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

@Composable
fun QrTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = Typography(), content = content)
}

