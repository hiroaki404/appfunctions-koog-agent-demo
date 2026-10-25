package dev.hiroaki404.appfunctions.koogdemo.agent

private val LeadingListMarker = Regex("(?m)^([ \\t]*)\\* ")

/** Flattens the light Markdown the model tends to return: drops `**` bold markers and turns "* " bullets into "・". */
fun plainAnswerText(markdown: String): String =
    markdown
        .replace(LeadingListMarker, "$1・")
        .replace("**", "")
