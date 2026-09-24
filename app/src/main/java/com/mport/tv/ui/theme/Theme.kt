package com.mport.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = CyanAccent,
    onPrimary = NavyDeep,
    primaryContainer = BlueChip,
    onPrimaryContainer = TextPrimary,
    secondary = CyanBright,
    onSecondary = NavyDeep,
    tertiary = BlueChip,
    background = NavyDeep,
    onBackground = TextPrimary,
    surface = Navy,
    onSurface = TextPrimary,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondary,
    outline = DividerDark,
    error = PowerRed,
    onError = Color.White
)

private val LightColors = lightColorScheme(
    primary = BlueChip,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = NavyDeep,
    secondary = CyanAccent,
    background = LightBg,
    onBackground = LightText,
    surface = LightCard,
    onSurface = LightText,
    surfaceVariant = Color(0xFFE8EEF5),
    onSurfaceVariant = Color(0xFF5A6570),
    outline = Color(0xFFCFD8DC),
    error = PowerRed
)

@Composable
fun MPorTTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MPorTTypography,
        content = content
    )
}

/** Prefer app setting, fallback system, default dark (TV style). */
@Composable
fun rememberAppDarkTheme(settingsDark: Boolean?): Boolean {
    val system = isSystemInDarkTheme()
    return settingsDark ?: true // default dark like ion-tv
}
