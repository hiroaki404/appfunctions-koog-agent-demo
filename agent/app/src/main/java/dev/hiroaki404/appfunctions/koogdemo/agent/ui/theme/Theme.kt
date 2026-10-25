package dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light only: the demo is projected, so one fixed palette beats following the system theme or wallpaper.
private val LightColorScheme = lightColorScheme(
    primary = Sea,
    onPrimary = Color.White,
    primaryContainer = SeaC,
    onPrimaryContainer = Ink,
    secondary = Sun,
    onSecondary = Color.White,
    secondaryContainer = SunC,
    onSecondaryContainer = Ink,
    background = Bg,
    onBackground = Ink,
    surface = Bg,
    onSurface = Ink,
    surfaceVariant = Surface,
    onSurfaceVariant = Ink2,
    surfaceContainer = Surface,
    surfaceContainerHigh = Surface2,
    outline = Line,
    outlineVariant = Line,
)

@Composable
fun AgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
