package dev.hiroaki404.appfunctions.koogdemo.agent

import android.content.Context
import android.util.Log
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.processor.processInput
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val A2UI_TAG = "A2uiHost"

/** The A2UI version the agent speaks; the messages it sends itself (deleting a surface) carry it. */
private const val A2UI_VERSION = "v0.9.1"

/** A2UI `deleteSurface` message for [surfaceId], as the parser expects it. */
internal fun deleteSurfaceMessage(surfaceId: String): String = buildJsonObject {
    put("version", A2UI_VERSION)
    put("deleteSurface", buildJsonObject { put("surfaceId", surfaceId) })
}.toString()

/**
 * Holds the one [A2uiMessageProcessor] that draws every A2UI surface a function returned, with the
 * basic catalog. The agent knows nothing about what the surfaces show; it only forwards the messages.
 *
 * The processor queues messages and handles them in [A2uiMessageProcessor.collectMessages], which
 * runs in [scope] (the ViewModel's main-thread scope), so surface state changes on the main thread
 * wherever [process] is called from. [context] is kept for the catalog's functions, so pass the
 * application context.
 */
class A2uiHost(context: Context, scope: CoroutineScope) {
    private val parser = A2uiMessageParser()
    private val processor = A2uiMessageProcessor(
        catalogs = listOf(
            materialA2uiBasicCatalogV1(
                // Provider apps' cards show text and icons only, so images, videos and audio draw nothing.
                image = MaterialA2uiBasicCatalogV1Defaults.image { _, _, _, _, _ -> },
                video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
                audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
                functions = A2uiBasicCatalogV1.Functions(context),
            ),
        ),
    )

    /** The surfaces drawn now, in creation order. */
    val activeSurfaces: StateFlow<List<A2uiSurfaceModel>> = processor.activeSurfaces

    init {
        // User actions on a surface go out as processor.outboundEvents; sending them back to a provider
        // app is not supported, so they are not collected. The loop only drains the inbound queue.
        scope.launch {
            try {
                processor.collectMessages()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(A2UI_TAG, "Processing A2UI messages stopped", e)
            }
        }
    }

    /**
     * Queues the messages of [a2uiJson], a JSON array of A2UI messages, one `processInput` call each. A
     * message that fails to parse is logged and skipped; the rest still go through.
     */
    fun process(a2uiJson: String) {
        val messages = try {
            splitA2uiMessages(a2uiJson)
        } catch (e: Exception) {
            Log.w(A2UI_TAG, "Ignoring A2UI that is not a JSON array", e)
            return
        }
        messages.forEach { message ->
            try {
                processor.processInput(parser, message)
            } catch (e: Exception) {
                Log.w(A2UI_TAG, "Ignoring an A2UI message that failed: $message", e)
            }
        }
    }

    /** Removes the surfaces with [surfaceIds]; ids that are not drawn are ignored by the processor. */
    fun deleteSurfaces(surfaceIds: Collection<String>) {
        surfaceIds.forEach { process("[${deleteSurfaceMessage(it)}]") }
    }
}
