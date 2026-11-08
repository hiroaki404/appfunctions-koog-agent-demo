package dev.hiroaki404.appfunctions.koogdemo.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerMarkdownTest {
    @Test
    fun splitsHeadingsListsAndParagraphs() {
        val markdown = """
            ## 旅程
            12月の北海道です。
            2泊3日の予定です。

            * 小樽
              * 運河
            1. 札幌
            ---
            以上です。
        """.trimIndent()

        assertEquals(
            listOf(
                MarkdownBlock.Heading(2, "旅程"),
                MarkdownBlock.Paragraph("12月の北海道です。\n2泊3日の予定です。"),
                MarkdownBlock.ListItem(0, "•", "小樽"),
                MarkdownBlock.ListItem(1, "•", "運河"),
                MarkdownBlock.ListItem(0, "1.", "札幌"),
                MarkdownBlock.Paragraph("以上です。"),
            ),
            parseMarkdownBlocks(markdown),
        )
    }

    @Test
    fun parsesBoldItalicAndCode() {
        assertEquals(
            listOf(
                InlineSpan("便名 "),
                InlineSpan("TR101", bold = true),
                InlineSpan(" / "),
                InlineSpan("TRP-1", code = true),
                InlineSpan(" / "),
                InlineSpan("予約済み", italic = true),
            ),
            parseInlineMarkdown("便名 **TR101** / `TRP-1` / *予約済み*"),
        )
    }

    @Test
    fun keepsArithmeticAsterisks() {
        assertEquals(listOf(InlineSpan("2 * 3 = 6")), parseInlineMarkdown("2 * 3 = 6"))
    }
}
