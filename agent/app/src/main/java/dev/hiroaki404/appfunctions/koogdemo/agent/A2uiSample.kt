package dev.hiroaki404.appfunctions.koogdemo.agent

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.AgentTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

/** The id of the surface drawn by [SAMPLE_ITINERARY_A2UI_JSON]. */
const val SAMPLE_ITINERARY_SURFACE_ID = "sample-itinerary"

/**
 * A fixed sample of what the agent could send to show an itinerary: a JSON array of A2UI v0.9.1
 * messages (`createSurface`, then `updateComponents`). Every value is a literal, so there is no
 * data model. Split it into one message per `processInput` call with [splitA2uiMessages].
 */
const val SAMPLE_ITINERARY_A2UI_JSON = """
[
  {
    "version": "v0.9.1",
    "createSurface": {
      "surfaceId": "$SAMPLE_ITINERARY_SURFACE_ID",
      "catalogId": "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json",
      "theme": { "primaryColor": "#00778D" }
    }
  },
  {
    "version": "v0.9.1",
    "updateComponents": {
      "surfaceId": "$SAMPLE_ITINERARY_SURFACE_ID",
      "components": [
        { "id": "root", "component": "Card", "child": "content" },
        {
          "id": "content",
          "component": "Column",
          "children": ["title", "period", "divider", "day1", "day2"]
        },
        { "id": "title", "component": "Text", "variant": "h3", "text": "京都 2 泊 3 日の旅" },
        { "id": "period", "component": "Row", "align": "center", "children": ["periodIcon", "periodText"] },
        { "id": "periodIcon", "component": "Icon", "name": "calendarToday" },
        { "id": "periodText", "component": "Text", "variant": "caption", "text": "2026-12-05 〜 2026-12-07" },
        { "id": "divider", "component": "Divider" },
        { "id": "day1", "component": "Column", "children": ["day1Head", "day1Spot1", "day1Spot2"] },
        { "id": "day1Head", "component": "Text", "variant": "h5", "text": "1日目" },
        { "id": "day1Spot1", "component": "Row", "align": "center", "children": ["day1Spot1Icon", "day1Spot1Text"] },
        { "id": "day1Spot1Icon", "component": "Icon", "name": "locationOn" },
        { "id": "day1Spot1Text", "component": "Text", "text": "清水寺" },
        { "id": "day1Spot2", "component": "Row", "align": "center", "children": ["day1Spot2Icon", "day1Spot2Text"] },
        { "id": "day1Spot2Icon", "component": "Icon", "name": "locationOn" },
        { "id": "day1Spot2Text", "component": "Text", "text": "祇園・花見小路" },
        { "id": "day2", "component": "Column", "children": ["day2Head", "day2Spot1", "day2Spot2"] },
        { "id": "day2Head", "component": "Text", "variant": "h5", "text": "2日目" },
        { "id": "day2Spot1", "component": "Row", "align": "center", "children": ["day2Spot1Icon", "day2Spot1Text"] },
        { "id": "day2Spot1Icon", "component": "Icon", "name": "locationOn" },
        { "id": "day2Spot1Text", "component": "Text", "text": "伏見稲荷大社" },
        { "id": "day2Spot2", "component": "Row", "align": "center", "children": ["day2Spot2Icon", "day2Spot2Text"] },
        { "id": "day2Spot2Icon", "component": "Icon", "name": "locationOn" },
        { "id": "day2Spot2Text", "component": "Text", "text": "嵐山・竹林の小径" }
      ]
    }
  }
]
"""

/** Splits a JSON array of A2UI messages into one JSON object string per message. */
fun splitA2uiMessages(jsonArray: String): List<String> =
    Json.parseToJsonElement(jsonArray).jsonArray.map { it.toString() }

/**
 * Renders [messagesJson] (a JSON array of A2UI messages) with the official Material 3 renderer and
 * the basic catalog. The messages are queued on first composition and handled by the processor's
 * `collectMessages` loop, which runs while this is in the composition. Only the surface with
 * [surfaceId] is drawn.
 */
@Composable
fun A2uiMessagesSurface(
    messagesJson: String,
    surfaceId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val primaryColor = remember(messagesJson) { a2uiPrimaryColorsOf(messagesJson)[surfaceId] }
    val processor = remember(messagesJson) {
        A2uiMessageProcessor(catalogs = listOf(brandedA2uiCatalog(context))).also { processor ->
            val parser = A2uiMessageParser()
            splitA2uiMessages(messagesJson).forEach { processor.processInput(parser, it) }
        }
    }
    LaunchedEffect(processor) { processor.collectMessages() }
    val surfaces by processor.activeSurfaces.collectAsState()
    surfaces.firstOrNull { it.id == surfaceId }?.let { surface ->
        CompositionLocalProvider(LocalA2uiPrimaryColor provides primaryColor) {
            A2uiSurface(surfaceModel = surface, modifier = modifier)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFBFAF7, widthDp = 390)
@Composable
private fun A2uiSampleItineraryPreview() {
    AgentTheme {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            A2uiMessagesSurface(
                messagesJson = SAMPLE_ITINERARY_A2UI_JSON,
                surfaceId = SAMPLE_ITINERARY_SURFACE_ID,
            )
        }
    }
}
