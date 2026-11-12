package dev.hiroaki404.appfunctions.koogdemo.agent

import android.content.Context
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

/**
 * The `primaryColor` of the surface being drawn, from its `createSurface` theme (A2UI basic catalog
 * theme). Null when the surface has none; the renderers then draw the plain Material look.
 */
val LocalA2uiPrimaryColor = staticCompositionLocalOf<Color?> { null }

/**
 * The `theme.primaryColor` of every `createSurface` in [a2uiJson] (a JSON array of A2UI messages),
 * by surface id. The Material renderer keeps the theme but does not draw it, so the agent reads it here.
 */
fun a2uiPrimaryColorsOf(a2uiJson: String): Map<String, Color> = runCatching {
    Json.parseToJsonElement(a2uiJson).jsonArray.mapNotNull { message ->
        val create = (message as? JsonObject)?.get("createSurface") as? JsonObject ?: return@mapNotNull null
        val surfaceId = (create["surfaceId"] as? JsonPrimitive)?.content ?: return@mapNotNull null
        val hex = ((create["theme"] as? JsonObject)?.get("primaryColor") as? JsonPrimitive)?.content
        parseHexColor(hex)?.let { surfaceId to it }
    }.toMap()
}.getOrDefault(emptyMap())

/** `#RRGGBB` or `#AARRGGBB` as a [Color], or null if [hex] is not one. */
fun parseHexColor(hex: String?): Color? {
    val digits = hex?.trim()?.removePrefix("#") ?: return null
    if (digits.length != 6 && digits.length != 8) return null
    val value = digits.toLongOrNull(16) ?: return null
    return Color(if (digits.length == 6) value or 0xFF000000 else value)
}

/**
 * The basic catalog with the Material renderers, where cards, dividers, icons and small headings take
 * on [LocalA2uiPrimaryColor]. Images, videos and audio draw nothing: provider apps' cards show text
 * and icons only.
 */
fun brandedA2uiCatalog(context: Context): A2uiCatalog = materialA2uiBasicCatalogV1(
    image = MaterialA2uiBasicCatalogV1Defaults.image { _, _, _, _, _ -> },
    video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
    audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
    functions = A2uiBasicCatalogV1.Functions(context),
    text = BrandedText,
    icon = BrandedIcon,
    card = BrandedCard,
)

private val White = Color.White

/** Icons in the primary color. */
private object BrandedIcon : A2uiBasicCatalogV1.Icon {
    private val delegate = MaterialA2uiBasicCatalogV1Defaults.icon

    @Composable
    override fun A2uiComponentScope.TypedContent(
        source: A2uiBasicCatalogV1.Icon.Source,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val scope = this
        CompositionLocalProvider(LocalContentColor provides (LocalA2uiPrimaryColor.current ?: LocalContentColor.current)) {
            with(delegate) { scope.TypedContent(source, accessibility, modifier) }
        }
    }
}

/** Small headings (`h4`, `h5`) in the primary color; the title and body text keep the ink color. */
private object BrandedText : A2uiBasicCatalogV1.Text {
    private val delegate = MaterialA2uiBasicCatalogV1Defaults.text

    @Composable
    override fun A2uiComponentScope.TypedContent(
        text: String,
        variant: A2uiBasicCatalogV1.Text.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val scope = this
        val primary = LocalA2uiPrimaryColor.current
        val color = when {
            primary == null -> LocalContentColor.current
            variant == A2uiBasicCatalogV1.Text.Variant.H4 || variant == A2uiBasicCatalogV1.Text.Variant.H5 -> primary
            variant == A2uiBasicCatalogV1.Text.Variant.Caption -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> LocalContentColor.current
        }
        CompositionLocalProvider(LocalContentColor provides color) {
            with(delegate) { scope.TypedContent(text, variant, accessibility, modifier) }
        }
    }
}

/**
 * Cards tinted with the primary color: a pale fill and a light border (the dividers inside use the
 * same border color). The Material card reads both from the color scheme, so only those are changed.
 */
private object BrandedCard : A2uiBasicCatalogV1.Card {
    private val delegate = MaterialA2uiBasicCatalogV1Defaults.card

    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val scope = this
        val primary = LocalA2uiPrimaryColor.current
        if (primary == null) {
            with(delegate) { scope.TypedContent(childId, accessibility, modifier) }
            return
        }
        val colorScheme = MaterialTheme.colorScheme.copy(
            surface = primary.copy(alpha = 0.06f).compositeOver(White),
            outlineVariant = primary.copy(alpha = 0.35f).compositeOver(White),
        )
        MaterialTheme(colorScheme = colorScheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
            with(delegate) { scope.TypedContent(childId, accessibility, modifier) }
        }
    }
}
