package dev.hiroaki404.appfunctions.koogdemo.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink2
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Sea
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.SeaC
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Surface
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Surface2

/**
 * The number of tools the next message can use. Turns blue with "+N" when functions were added
 * since the user last opened the tool list, so a newly installed app is visible at a glance.
 */
@Composable
fun ToolCountPill(state: AvailableToolsState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val newCount = state.newFunctionIds.size
    val highlighted = newCount > 0
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(if (highlighted) Sea else Surface2)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Build,
            contentDescription = "使えるツール",
            modifier = Modifier.size(18.dp),
            tint = if (highlighted) Color.White else Sea,
        )
        Text(
            state.functions?.size?.toString() ?: "–",
            color = if (highlighted) Color.White else Ink,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (highlighted) {
            Text(
                "+$newCount",
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                color = Sea,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Every tool the agent can use, grouped by the app providing it. Apps the user installed start expanded. */
@Composable
fun ToolListScreen(
    state: AvailableToolsState,
    providerAppOf: (packageName: String) -> ProviderApp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(state) { state.groups(providerAppOf) }
    // Packages whose expansion the user flipped; the default is expanded for non-system apps.
    var toggled by remember { mutableStateOf(emptySet<String>()) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "戻る", tint = Ink)
            }
            Column {
                Text("使えるツール", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${state.functions?.size ?: 0} 関数", color = Ink2, fontSize = 12.sp)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(groups, key = { it.packageName }) { group ->
                val expanded = !group.app.isSystem xor (group.packageName in toggled)
                ToolGroup(
                    group = group,
                    expanded = expanded,
                    onToggle = {
                        toggled = if (group.packageName in toggled) toggled - group.packageName else toggled + group.packageName
                    },
                )
            }
        }
    }
}

@Composable
private fun ToolGroup(
    group: AvailableToolsGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (group.hasNew) SeaC else Surface)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                group.app.label,
                modifier = Modifier.weight(1f, fill = false),
                color = Ink,
                fontSize = 14.sp,
                fontWeight = if (expanded) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (group.hasNew) {
                Text(
                    "NEW",
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Sea)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                Text(
                    group.functions.size.toString(),
                    color = Ink2,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "閉じる" else "開く",
                modifier = Modifier.size(20.dp),
                tint = Ink2,
            )
        }
        if (expanded) {
            group.functions.forEach { function -> FunctionRow(function) }
        }
    }
}

@Composable
private fun FunctionRow(function: AvailableFunction, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            function.name,
            color = Ink,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        val hasDescription = function.description.isNotBlank()
        Text(
            // KDoc paragraphs arrive with blank lines; fold them so two lines show actual text.
            if (hasDescription) function.description.replace(Regex("\\s+"), " ").trim() else "説明なし",
            color = Ink2,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontStyle = if (hasDescription) FontStyle.Normal else FontStyle.Italic,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
