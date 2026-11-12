package dev.hiroaki404.appfunctions.koogdemo.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class A2uiExtractionTest {
    @Test
    fun removesA2uiFromWhatTheLlmSeesAndReturnsIt() {
        val result = buildJsonObject {
            put("itineraryId", "abc")
            put("a2ui", "[]")
        }

        val (forLlm, a2ui) = extractA2ui(result)

        assertEquals("[]", a2ui)
        assertEquals(buildJsonObject { put("itineraryId", "abc") }, forLlm)
    }

    @Test
    fun resultWithoutA2uiIsUntouched() {
        val result = buildJsonObject { put("itineraryId", "abc") }

        assertEquals(result to null, extractA2ui(result))
    }

    @Test
    fun a2uiThatIsNotAStringIsKept() {
        val array = buildJsonObject { put("a2ui", JsonArray(listOf(JsonPrimitive("x")))) }
        val number = buildJsonObject { put("a2ui", 1) }
        val nothing = buildJsonObject { put("a2ui", JsonNull) }

        listOf(array, number, nothing).forEach { assertEquals(it to null, extractA2ui(it)) }
    }

    @Test
    fun resultThatIsNotAnObjectIsUntouched() {
        listOf(JsonPrimitive("a2ui"), JsonArray(listOf(JsonPrimitive("a2ui")))).forEach {
            assertEquals(it to null, extractA2ui(it))
        }
    }

    @Test
    fun surfaceIdIsReadFromTheFirstCreateSurface() {
        assertEquals("itinerary-1", a2uiSurfaceIdOf(SAMPLE_ITINERARY_A2UI_JSON.replace(SAMPLE_ITINERARY_SURFACE_ID, "itinerary-1")))
        assertEquals(SAMPLE_ITINERARY_SURFACE_ID, a2uiSurfaceIdOf(SAMPLE_ITINERARY_A2UI_JSON))
    }

    @Test
    fun surfaceIdSkipsMessagesThatAreNotCreateSurface() {
        val json = """[{"version":"v0.9.1","updateComponents":{"surfaceId":"a","components":[]}},""" +
            """{"version":"v0.9.1","createSurface":{"surfaceId":"b","catalogId":"c"}}]"""

        assertEquals("b", a2uiSurfaceIdOf(json))
    }

    @Test
    fun surfaceIdIsNullWithoutCreateSurfaceOrForMalformedJson() {
        assertNull(a2uiSurfaceIdOf("[]"))
        assertNull(a2uiSurfaceIdOf("""[{"version":"v0.9.1","updateComponents":{"surfaceId":"a"}}]"""))
        assertNull(a2uiSurfaceIdOf("not json"))
        assertNull(a2uiSurfaceIdOf("""{"createSurface":{"surfaceId":"a"}}"""))
        assertNull(a2uiSurfaceIdOf("""[{"createSurface":{"surfaceId":1}}]"""))
        assertNull(a2uiSurfaceIdOf(""))
    }

    @Test
    fun deleteSurfaceMessageHasTheShapeTheParserReads() {
        val message = Json.parseToJsonElement(deleteSurfaceMessage("itinerary-1")).jsonObject

        assertEquals(JsonPrimitive("v0.9.1"), message["version"])
        assertEquals(
            JsonObject(mapOf("surfaceId" to JsonPrimitive("itinerary-1"))),
            message["deleteSurface"],
        )
    }
}
