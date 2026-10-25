package dev.hiroaki404.appfunctions.koogdemo.message.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light only: the design has no dark variant and does not use dynamic color.
private val LightColorScheme = lightColorScheme(
    primary = Sea,
    onPrimary = Color.White,
    primaryContainer = SeaContainer,
    onPrimaryContainer = Sea,
    secondary = Sun,
    onSecondary = Color.White,
    secondaryContainer = SunContainer,
    onSecondaryContainer = Sun,
    background = Bg,
    onBackground = Ink,
    surface = Bg,
    onSurface = Ink,
    surfaceVariant = Surface1,
    onSurfaceVariant = Ink2,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    outline = Line,
    outlineVariant = Line,
)

@Composable
fun MessageTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
