package dev.hiroaki404.appfunctions.koogdemo.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Parses tool arguments or a tool result for display. A result may arrive as a JSON string whose
 * content is itself JSON, so that inner object or array is unwrapped. Koog's tool-result text can
 * also wrap the JSON in bare quotes without escaping it. Returns null for plain text.
 */
fun parseToolJson(text: String): JsonElement? {
    val element = runCatching { Json.parseToJsonElement(text) }.getOrNull()
        ?: return unquoted(text)?.let { inner -> runCatching { Json.parseToJsonElement(inner) }.getOrNull() }
    if (element is JsonPrimitive && element.isString) {
        val inner = runCatching { Json.parseToJsonElement(element.content) }.getOrNull()
        if (inner is JsonObject || inner is JsonArray) return inner
    }
    return element
}

private fun unquoted(text: String): String? = text.trim()
    .takeIf { it.length >= 2 && it.startsWith('"') && it.endsWith('"') }
    ?.substring(1, text.trim().length - 1)

/** A scalar as the user would read it: strings without quotes, null as a dash. */
fun JsonPrimitive.displayText(): String = if (this is JsonNull) "—" else content

/**
 * A one-line summary of [element]: the item count and first labels for an array,
 * the first few scalar fields for an object.
 */
fun summarizeJson(element: JsonElement): String = when (element) {
    is JsonPrimitive -> element.displayText()
    is JsonArray -> if (element.isEmpty()) {
        "0件"
    } else {
        val labels = element.take(3).map(::labelOf)
        val more = if (element.size > labels.size) " ほか" else ""
        "${element.size}件: ${labels.joinToString("、")}$more"
    }
    is JsonObject -> element.entries
        .filter { (_, value) -> value is JsonPrimitive && value !is JsonNull }
        .take(3)
        .joinToString(" · ") { (key, value) -> "$key: ${(value as JsonPrimitive).content}" }
        .ifEmpty { "${element.size}項目" }
}

/** The first string field of an object (usually its name or title), or its summary. */
private fun labelOf(element: JsonElement): String = when (element) {
    is JsonObject -> element.values.firstOrNull { it is JsonPrimitive && it.isString }
        ?.let { (it as JsonPrimitive).content }
        ?: summarizeJson(element)
    else -> summarizeJson(element)
}