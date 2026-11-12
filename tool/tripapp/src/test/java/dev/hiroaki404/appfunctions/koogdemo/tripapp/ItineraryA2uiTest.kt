package dev.hiroaki404.appfunctions.koogdemo.tripapp

import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripSeed
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryA2uiTest {
    private val itinerary = TripSeed.upcomingTrip

    private fun messages(id: String = "7", trip: Itinerary = itinerary): JsonArray =
        Json.parseToJsonElement(itineraryA2uiJson(id, trip)).jsonArray

    private fun components(id: String = "7", trip: Itinerary = itinerary): List<JsonObject> =
        messages(id, trip)[1].jsonObject.getValue("updateComponents").jsonObject
            .getValue("components").jsonArray.map { it.jsonObject }

    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.content

    @Test
    fun startsWithCreateSurfaceThenUpdateComponents() {
        val messages = messages()
        assertEquals(2, messages.size)
        val first = messages[0].jsonObject
        assertEquals("v0.9.1", first.text("version"))
        val create = first.getValue("createSurface").jsonObject
        assertEquals("itinerary-7", create.text("surfaceId"))
        assertTrue(create.text("catalogId")!!.endsWith("/catalogs/basic/catalog.json"))
        val update = messages[1].jsonObject.getValue("updateComponents").jsonObject
        assertEquals("itinerary-7", update.text("surfaceId"))
    }

    @Test
    fun createSurfaceCarriesTheBrandColorAsThePrimaryColor() {
        val create = messages()[0].jsonObject.getValue("createSurface").jsonObject
        assertEquals("#00778D", create.getValue("theme").jsonObject.text("primaryColor"))
    }

    @Test
    fun spotDescriptionsAreShownAsCaptions() {
        val captions = components().filter { it.text("variant") == "caption" }.mapNotNull { it.text("text") }
        itinerary.days.flatMap { it.spots }.filter { it.description.isNotBlank() }
            .forEach { spot -> assertTrue(spot.name, spot.description in captions) }
    }

    @Test
    fun hasRootComponentAndTitle() {
        val components = components()
        assertTrue(components.any { it.text("id") == "root" })
        assertTrue(components.any { it.text("text") == itinerary.title })
    }

    @Test
    fun containsEverySpotNameAndDayHeading() {
        val texts = components().mapNotNull { it.text("text") }
        itinerary.days.flatMap { it.spots }.forEach { spot -> assertTrue(spot.name, spot.name in texts) }
        assertTrue(texts.any { it.startsWith("1日目 ") })
        assertTrue(texts.any { it.startsWith("${itinerary.days.size}日目 ") })
    }

    @Test
    fun surfaceIdDiffersForDifferentItineraryIds() {
        assertNotEquals(itinerarySurfaceId("1"), itinerarySurfaceId("2"))
        assertNotEquals(itineraryA2uiJson("1", itinerary), itineraryA2uiJson("2", itinerary))
    }

    @Test
    fun componentIdsAreUnique() {
        val ids = components().map { it.text("id")!! }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun childReferencesPointToExistingComponents() {
        val components = components()
        val ids = components.map { it.text("id")!! }.toSet()
        components.forEach { component ->
            component["child"]?.let { assertTrue(it.jsonPrimitive.content in ids) }
            component["children"]?.jsonArray?.forEach { assertTrue(it.jsonPrimitive.content in ids) }
        }
    }

    @Test
    fun handlesDaysWithoutSpotsAndSpecialCharacters() {
        val trip = Itinerary(
            "\"引用\" と\\改行\n",
            listOf(ItineraryDay("2026-12-20", emptyList()), ItineraryDay("12/21", listOf(Spot("店\"A\"", "")))),
        )
        val components = components("9", trip)
        val texts = components.mapNotNull { it.text("text") }
        assertTrue(trip.title in texts)
        assertTrue("店\"A\"" in texts)
        assertTrue("2日目 12/21" in texts)
        val ids = components.map { it.text("id")!! }
        assertEquals(ids.size, ids.toSet().size)
    }
}
