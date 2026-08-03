package com.example.hanabihinttracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import com.example.hanabihinttracker.domain.Color
import androidx.compose.ui.graphics.Color as UiColor

@Composable
internal fun HanabiTheme(darkBackground: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkBackground) {
        darkColorScheme(
            background = UiColor.Black,
            surface = UiColor(0xFF1B1B1B),
            surfaceVariant = UiColor(0xFF303030)
        )
    } else {
        lightColorScheme(primary = UiColor(0xFF5A3E85), secondary = UiColor(0xFF176B87))
    }
    MaterialTheme(colorScheme = colors, content = content)
}

internal fun Color.uiColor() = UiColor(hex)
internal fun Color.textColor() = if (this == Color.WHITE || this == Color.YELLOW) UiColor.Black else UiColor.White
internal fun Color.brush(): Brush = if (this == Color.RAINBOW) {
    Brush.linearGradient(listOf(UiColor.Red, UiColor.Yellow, UiColor.Green, UiColor.Cyan, UiColor.Blue, UiColor.Magenta))
} else {
    Brush.linearGradient(listOf(uiColor(), uiColor()))
}
