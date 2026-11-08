package dev.hiroaki404.appfunctions.koogdemo.agent

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolJsonTest {
    @Test
    fun unwrapsJsonEncodedInsideAString() {
        val parsed = parseToolJson("\"[{\\\"title\\\":\\\"北海道\\\"}]\"")

        assertTrue(parsed is JsonArray)
    }

    @Test
    fun parsesJsonWrappedInUnescapedQuotes() {
        val parsed = parseToolJson("\"[{\"title\":\"北海道\"}]\"")

        assertEquals("1件: 北海道", summarizeJson(parsed!!))
    }

    @Test
    fun keepsAPlainStringResultAsAString() {
        assertEquals(JsonPrimitive("保存しました"), parseToolJson("\"保存しました\""))
    }

    @Test
    fun returnsNullForPlainText() {
        assertNull(parseToolJson("Error: not found"))
    }

    @Test
    fun summarizesArraysByCountAndFirstLabels() {
        val parsed = parseToolJson("""[{"id":1,"title":"北海道"},{"title":"沖縄"},{"title":"京都"},{"title":"奈良"}]""")!!

        assertEquals("4件: 北海道、沖縄、京都 ほか", summarizeJson(parsed))
    }

    @Test
    fun summarizesObjectsByTheirScalarFields() {
        val parsed = parseToolJson("""{"bookingId":"TRP-1","days":[1,2],"flight":"TR101","seat":null}""")!!

        assertTrue(parsed is JsonObject)
        assertEquals("bookingId: TRP-1 · flight: TR101", summarizeJson(parsed))
    }}
