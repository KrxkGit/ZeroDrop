package com.zerodrop.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    background = OLED_BLACK,
    surface = OLED_BLACK,
    primary = SCORE_WHITE,
    secondary = SCORE_DIM,
    onBackground = SCORE_WHITE,
    onSurface = SCORE_WHITE
)

@Composable
fun ZeroDropTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
