package dev.hiroaki404.appfunctions.koogdemo.agent

/** A tool name split into the part worth showing on a card and a label for the app that provides it. */
data class ToolDisplayName(val function: String, val appLabel: String?)

/**
 * Splits a name produced by [toolNameOf] (e.g. "BaseKototabiAppFunctionService_bookFlight") into the function
 * part and a short label for the owning app. A name without an owner part has no app label.
 */
fun toolDisplayNameOf(toolName: String): ToolDisplayName {
    val separator = toolName.indexOf('_')
    if (separator < 0) return ToolDisplayName(function = toolName, appLabel = null)
    val owner = toolName.substring(0, separator)
    val function = toolName.substring(separator + 1).ifBlank { toolName }
    val appLabel = when {
        owner.contains("Kototabi") -> "kototabi"
        owner.contains("Message") -> "メッセージ"
        else -> owner.removePrefix("Base").removeSuffix("AppFunctionService").ifBlank { null }
    }
    return ToolDisplayName(function = function, appLabel = appLabel)
}

private val LeadingListMarker = Regex("(?m)^([ \\t]*)\\* ")

/** Flattens the light Markdown the model tends to return: drops `**` bold markers and turns "* " bullets into "・". */
fun plainAnswerText(markdown: String): String =
    markdown
        .replace(LeadingListMarker, "$1・")
        .replace("**", "")
