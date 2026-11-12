package dev.hiroaki404.appfunctions.koogdemo.tripapp

import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.dayLabel
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ui.tripPeriodText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val A2UI_VERSION = "v0.9.1"
private const val BASIC_CATALOG_ID = "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"

/** The app's brand color (the sea blue of its theme), sent as the surface theme's `primaryColor`. */
internal const val ITINERARY_PRIMARY_COLOR = "#00778D"

/** The A2UI surface id for the itinerary saved with [itineraryId]. Itinerary ids are unique. */
fun itinerarySurfaceId(itineraryId: String): String = "itinerary-$itineraryId"

/**
 * Builds the itinerary card as a JSON array of A2UI v0.9.1 messages: `createSurface`, then
 * `updateComponents`. Every value is a literal, so there is no data model.
 */
fun itineraryA2uiJson(itineraryId: String, itinerary: Itinerary): String {
    val surfaceId = itinerarySurfaceId(itineraryId)
    return buildJsonArray {
        add(
            buildJsonObject {
                put("version", A2UI_VERSION)
                put(
                    "createSurface",
                    buildJsonObject {
                        put("surfaceId", surfaceId)
                        put("catalogId", BASIC_CATALOG_ID)
                        put("theme", buildJsonObject { put("primaryColor", ITINERARY_PRIMARY_COLOR) })
                    },
                )
            },
        )
        add(
            buildJsonObject {
                put("version", A2UI_VERSION)
                put(
                    "updateComponents",
                    buildJsonObject {
                        put("surfaceId", surfaceId)
                        put("components", itineraryComponents(itinerary))
                    },
                )
            },
        )
    }.toString()
}

private fun itineraryComponents(itinerary: Itinerary): JsonArray = buildJsonArray {
    fun node(id: String, component: String, block: JsonObjectBuilder.() -> Unit = {}) {
        add(
            buildJsonObject {
                put("id", id)
                put("component", component)
                block()
            },
        )
    }

    fun ids(vararg ids: String) = JsonArray(ids.map(::JsonPrimitive))

    val dayIds = itinerary.days.indices.map { "day$it" }
    node("root", "Card") { put("child", "content") }
    node("content", "Column") {
        put("children", JsonArray((listOf("title", "period", "divider") + dayIds).map(::JsonPrimitive)))
    }
    node("title", "Text") {
        put("variant", "h3")
        put("text", itinerary.title)
    }
    node("period", "Row") {
        put("align", "center")
        put("children", ids("periodIcon", "periodText"))
    }
    node("periodIcon", "Icon") { put("name", "calendarToday") }
    node("periodText", "Text") {
        put("variant", "caption")
        put("text", tripPeriodText(itinerary))
    }
    node("divider", "Divider")

    itinerary.days.forEachIndexed { dayIndex, day ->
        val dayId = "day$dayIndex"
        val spotIds = day.spots.indices.map { "$dayId-spot$it" }
        node(dayId, "Column") {
            put("children", JsonArray((listOf("$dayId-head") + spotIds).map(::JsonPrimitive)))
        }
        node("$dayId-head", "Text") {
            put("variant", "h5")
            put("text", "${dayIndex + 1}日目 ${dayLabel(day.date)}")
        }
        day.spots.forEachIndexed { spotIndex, spot ->
            val spotId = "$dayId-spot$spotIndex"
            node(spotId, "Row") {
                put("align", "center")
                put("children", ids("$spotId-icon", "$spotId-body"))
            }
            node("$spotId-icon", "Icon") { put("name", "locationOn") }
            // The name, with the description under it in small text when there is one.
            val hasDescription = spot.description.isNotBlank()
            node("$spotId-body", "Column") {
                put(
                    "children",
                    if (hasDescription) ids("$spotId-text", "$spotId-description") else ids("$spotId-text"),
                )
            }
            node("$spotId-text", "Text") { put("text", spot.name) }
            if (hasDescription) {
                node("$spotId-description", "Text") {
                    put("variant", "caption")
                    put("text", spot.description)
                }
            }
        }
    }
}
