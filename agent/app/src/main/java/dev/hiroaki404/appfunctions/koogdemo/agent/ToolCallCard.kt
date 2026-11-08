package dev.hiroaki404.appfunctions.koogdemo.agent

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Bg
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink2
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Line
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Sea
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Surface
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Surface2
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val CardShape = RoundedCornerShape(14.dp)

/**
 * One tool call: the function, what the model passed and what the app returned. Arguments show as
 * key/value rows with nested values summarized, the result as a one-line summary; tapping the card
 * expands both in full.
 */
@Composable
fun ToolCallCard(call: ToolCallUiModel, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val arguments = remember(call.arguments) { parseToolJson(call.arguments) }
    val result = remember(call.result) { call.result?.let(::parseToolJson) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Bg)
            .border(1.dp, Line, CardShape)
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolStatusIcon(call.status)
            Text(
                call.functionName,
                modifier = Modifier.weight(1f),
                color = Ink,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            call.appLabel?.let { label ->
                Text(
                    label,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Surface2)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    color = Ink2,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "閉じる" else "詳しく見る",
                modifier = Modifier.size(20.dp),
                tint = Ink2,
            )
        }
        Section(label = "引数") {
            when {
                arguments is JsonObject && arguments.isEmpty() -> PlainValue("なし", color = Ink2)
                arguments != null -> JsonFields(arguments, expanded)
                else -> PlainValue(call.arguments, maxLines = if (expanded) Int.MAX_VALUE else 3)
            }
        }
        call.result?.let { raw ->
            Section(label = "結果") {
                val isError = raw.startsWith("Error:")
                when {
                    isError -> PlainValue(raw, color = MaterialTheme.colorScheme.error)
                    result == null -> PlainValue(raw, maxLines = if (expanded) Int.MAX_VALUE else 2)
                    expanded -> JsonFields(result, expanded = true)
                    else -> PlainValue(summarizeJson(result), maxLines = 2)
                }
            }
        }
        call.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = Sea, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

/** Key/value rows for [element]. Collapsed, nested objects and arrays become a one-line summary. */
@Composable
private fun JsonFields(element: JsonElement, expanded: Boolean) {
    when (element) {
        is JsonPrimitive -> PlainValue(element.displayText(), maxLines = if (expanded) Int.MAX_VALUE else 2)
        is JsonObject -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            element.forEach { (key, value) ->
                if (value is JsonPrimitive || !expanded) {
                    FieldRow(key, if (value is JsonPrimitive) value.displayText() else summarizeJson(value), expanded)
                } else {
                    Text(key, color = Ink2, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Nested { JsonFields(value, expanded = true) }
                }
            }
        }
        is JsonArray -> if (!expanded || element.isEmpty()) {
            PlainValue(summarizeJson(element), maxLines = 2)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                element.forEachIndexed { index, item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${index + 1}",
                            modifier = Modifier.widthIn(min = 14.dp),
                            color = Sea,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Box(modifier = Modifier.weight(1f)) { JsonFields(item, expanded = true) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Nested(content: @Composable () -> Unit) {
    Row {
        Box(
            modifier = Modifier
                .padding(start = 2.dp, end = 10.dp, top = 2.dp)
                .size(width = 2.dp, height = 14.dp)
                .background(Line),
        )
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun FieldRow(key: String, value: String, expanded: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            key,
            modifier = Modifier
                .widthIn(max = 128.dp)
                .padding(top = 1.dp),
            color = Ink2,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = Ink,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlainValue(text: String, color: Color = Ink, maxLines: Int = Int.MAX_VALUE) {
    Text(text, color = color, fontSize = 13.5.sp, lineHeight = 20.sp, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun ToolStatusIcon(status: ToolCallStatus, modifier: Modifier = Modifier) {
    when (status) {
        ToolCallStatus.Running -> CircularProgressIndicator(
            modifier = modifier.size(18.dp),
            color = Sea,
            strokeWidth = 2.dp,
        )

        ToolCallStatus.Succeeded -> StatusCircle(Sea, Icons.Rounded.Check, modifier)
        ToolCallStatus.Failed -> StatusCircle(MaterialTheme.colorScheme.error, Icons.Rounded.Close, modifier)
    }
}

@Composable
private fun StatusCircle(color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
    }
}
