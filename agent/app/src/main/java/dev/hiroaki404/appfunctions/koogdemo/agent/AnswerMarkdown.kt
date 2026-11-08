package dev.hiroaki404.appfunctions.koogdemo.agent

/** One line-level element of the light Markdown the model returns. */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock

    /** [marker] is "•" for bullets or the original number with its dot ("1.") for ordered lists. */
    data class ListItem(val depth: Int, val marker: String, val text: String) : MarkdownBlock

    /** Consecutive plain lines, joined with "\n". */
    data class Paragraph(val text: String) : MarkdownBlock
}

/** A run of inline text with the styles the answer view supports. */
data class InlineSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
)

private val HeadingLine = Regex("^\\s*(#{1,6})\\s+(.*)$")
private val BulletLine = Regex("^([ \\t]*)[*+-]\\s+(.*)$")
private val OrderedLine = Regex("^([ \\t]*)(\\d+)[.)]\\s+(.*)$")
private val RuleLine = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")

/** Splits [markdown] into headings, list items and paragraphs. Horizontal rules are dropped. */
fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    fun flushParagraph() {
        if (paragraph.isNotEmpty()) blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n"))
        paragraph.clear()
    }
    markdown.lines().forEach { line ->
        val heading = HeadingLine.matchEntire(line)
        val bullet = BulletLine.matchEntire(line)
        val ordered = OrderedLine.matchEntire(line)
        when {
            line.isBlank() || RuleLine.matches(line) -> flushParagraph()
            heading != null -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(heading.groupValues[1].length, heading.groupValues[2].trim())
            }
            bullet != null -> {
                flushParagraph()
                blocks += MarkdownBlock.ListItem(depthOf(bullet.groupValues[1]), "•", bullet.groupValues[2].trim())
            }
            ordered != null -> {
                flushParagraph()
                blocks += MarkdownBlock.ListItem(depthOf(ordered.groupValues[1]), "${ordered.groupValues[2]}.", ordered.groupValues[3].trim())
            }
            else -> paragraph += line.trim()
        }
    }
    flushParagraph()
    return blocks
}

private fun depthOf(indent: String): Int = indent.fold(0) { width, char -> width + if (char == '\t') 2 else 1 } / 2

// Italic needs a non-space right after the opening "*" so arithmetic such as "2 * 3" stays as is.
private val InlineMarkup = Regex("\\*\\*(.+?)\\*\\*|`([^`]+)`|(?<![*\\w])\\*(?![\\s*])(.+?)(?<![\\s*])\\*(?![*\\w])")

/** Splits one block's text into styled runs for `**bold**`, `*italic*` and `` `code` ``. */
fun parseInlineMarkdown(text: String): List<InlineSpan> {
    val spans = mutableListOf<InlineSpan>()
    var cursor = 0
    InlineMarkup.findAll(text).forEach { match ->
        if (match.range.first > cursor) spans += InlineSpan(text.substring(cursor, match.range.first))
        val (bold, code, italic) = match.destructured
        spans += when {
            bold.isNotEmpty() -> InlineSpan(bold, bold = true)
            code.isNotEmpty() -> InlineSpan(code, code = true)
            else -> InlineSpan(italic, italic = true)
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) spans += InlineSpan(text.substring(cursor))
    return spans
}
