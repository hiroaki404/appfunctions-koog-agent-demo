package dev.hiroaki404.appfunctions.koogdemo.agent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.AgentTheme
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink2
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Sea
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Surface2

private val BodySize = 15.sp
private val BodyLineHeight = 24.sp

/** The model's answer with its headings, lists, bold and code spans rendered. */
@Composable
fun AnswerText(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            val previous = blocks.getOrNull(index - 1)
            val top = when {
                previous == null -> 0.dp
                previous is MarkdownBlock.ListItem && block is MarkdownBlock.ListItem -> 4.dp
                block is MarkdownBlock.Heading -> 14.dp
                else -> 10.dp
            }
            val blockModifier = Modifier.padding(top = top)
            when (block) {
                is MarkdownBlock.Heading -> Text(
                    inlineText(block.text),
                    modifier = blockModifier,
                    color = Ink,
                    fontSize = if (block.level <= 2) 17.sp else 15.5.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold,
                )
                is MarkdownBlock.ListItem -> Row(modifier = blockModifier.padding(start = (block.depth * 16).dp)) {
                    val bullet = block.marker == "•"
                    Text(
                        block.marker,
                        modifier = Modifier.widthIn(min = if (bullet) 16.dp else 22.dp),
                        color = if (bullet) Sea else Ink2,
                        fontSize = BodySize,
                        lineHeight = BodyLineHeight,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(inlineText(block.text), color = Ink, fontSize = BodySize, lineHeight = BodyLineHeight)
                }
                is MarkdownBlock.Paragraph -> Text(
                    inlineText(block.text),
                    modifier = blockModifier,
                    color = Ink,
                    fontSize = BodySize,
                    lineHeight = BodyLineHeight,
                )
            }
        }
    }
}

private fun inlineText(text: String): AnnotatedString = buildAnnotatedString {
    parseInlineMarkdown(text).forEach { span ->
        val style = when {
            span.bold -> SpanStyle(fontWeight = FontWeight.Bold)
            span.italic -> SpanStyle(fontStyle = FontStyle.Italic)
            span.code -> SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.5.sp, background = Surface2)
            else -> null
        }
        if (style == null) append(span.text) else withStyle(style) { append(span.text) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFBFAF7, widthDp = 360)
@Composable
private fun AnswerTextPreview() {
    AgentTheme {
        AnswerText(
            """
            保存されている旅行の予定と、12月の北海道のおすすめスポットです。

            ## 保存されている旅行の予定
            1. **2025年夏の北海道**（2025-08-10 〜 2025-08-11）
                * 時計台、大通公園
            2. **2026年冬の北海道旅行**

            ### 12月の北海道のおすすめスポット
            * **小樽運河**: ガス灯と石造り倉庫が並ぶ運河。
            * **旭山動物園**: *冬は*ペンギンの散歩が見どころ。予約番号は `TRP-0001` です。
            """.trimIndent(),
            modifier = Modifier.padding(16.dp),
        )
    }
}
