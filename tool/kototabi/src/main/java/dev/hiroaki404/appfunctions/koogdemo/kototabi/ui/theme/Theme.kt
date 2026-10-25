package dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light only, no dynamic color: the demo is projected and must look the same on every device.
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
    surfaceVariant = SurfaceBg,
    onSurfaceVariant = Ink2,
    surfaceContainer = SurfaceBg,
    surfaceContainerHigh = Surface2,
    outline = Line,
    outlineVariant = Line,
)

@Composable
fun KototabiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
