package dev.hiroaki404.appfunctions.koogdemo.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Column
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Bg
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink
import kotlinx.coroutines.delay
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink2
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Line
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Sea
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.SeaC
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Surface
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Surface2

/**
 * An app the user installed, as one icon on the band; [runningLabel] is set while the agent is using
 * it, i.e. its function is the latest one called in the turn being answered.
 */
data class AppBandApp(
    val packageName: String,
    val label: String,
    val runningLabel: String? = null,
    /** Found while the agent was running ([AppDiscovery]), so its icon pops in. */
    val isNew: Boolean = false,
)

/** What the band shows: installed apps one by one, system apps folded into a single chip. */
data class AppBandState(
    val apps: List<AppBandApp> = emptyList(),
    val systemAppCount: Int = 0,
    /** Set while the agent is using a system app. */
    val systemRunningLabel: String? = null,
    /** True while the turn being answered has called any tool, even one whose app is unknown. */
    val isCalling: Boolean = false,
)

/** Builds the band from the tool list's groups and the turn's latest tool call ([ChatTurn] `activeToolCall`). */
fun appBandState(
    groups: List<AvailableToolsGroup>,
    running: ToolCallUiModel?,
    discovery: AppDiscovery? = null,
): AppBandState {
    val runningPackage = running?.packageName
    val runningLabel = running?.let { it.displayName ?: it.functionName }
    val (system, installed) = groups.partition { it.app.isSystem }
    return AppBandState(
        apps = installed.map { group ->
            AppBandApp(
                packageName = group.packageName,
                label = group.app.label,
                runningLabel = runningLabel.takeIf { group.packageName == runningPackage },
                isNew = discovery != null && group.packageName in discovery.packageNames,
            )
        },
        systemAppCount = system.size,
        systemRunningLabel = runningLabel.takeIf { system.any { it.packageName == runningPackage } },
        isCalling = running != null,
    )
}

private val BandShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(12.dp)

/**
 * The agent and the apps whose functions it can call, in one row under the title. The app the agent
 * is using grows into a pill with the function's name, so the audience sees which app the agent is
 * working with without reading the tool cards.
 */
@Composable
fun AppBand(state: AppBandState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp)
            .clip(BandShape)
            .background(Surface)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(LocalContext.current.packageName, size = 32.dp)
        Connector(active = state.isCalling)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                // Room for a new app's ripple, which the scroll container would otherwise clip.
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.apps.forEach { app ->
                key(app.packageName) {
                    if (app.isNew) NewBandApp(app) else BandApp(app)
                }
            }
        }
        if (state.systemAppCount > 0) {
            SystemChip(count = state.systemAppCount, runningLabel = state.systemRunningLabel)
        }
    }
}

/** The line from the agent to the apps; a dot travels along it while a function runs. */
@Composable
private fun Connector(active: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(28.dp).height(10.dp), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (active) 4.dp else 2.dp)
                .clip(CircleShape)
                .background(if (active) Sea else Line),
        )
        if (active) {
            val transition = rememberInfiniteTransition(label = "connector")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
                label = "dot",
            )
            Box(
                modifier = Modifier
                    .offset(x = 18.dp * progress)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Sea, CircleShape),
            )
        }
    }
}

@Composable
private fun BandApp(app: AppBandApp, modifier: Modifier = Modifier) {
    val running = app.runningLabel != null
    val glow = if (running) pulse() else 0f
    Row(
        modifier = modifier
            .animateContentSize()
            .clip(PillShape)
            .background(if (running) Sea else Color.Transparent)
            .border(if (running) 3.dp else 0.dp, SeaC.copy(alpha = glow), PillShape)
            .padding(
                start = if (running) 4.dp else 0.dp,
                end = if (running) 12.dp else 0.dp,
                top = if (running) 4.dp else 0.dp,
                bottom = if (running) 4.dp else 0.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName, size = if (running) 28.dp else 32.dp)
        app.runningLabel?.let { label ->
            Text(
                label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

/** A just-found app: the icon pops in with a bounce, then a ripple spreads from it three times. */
@Composable
private fun NewBandApp(app: AppBandApp, modifier: Modifier = Modifier) {
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }
    val ripple = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(400)
        repeat(3) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(900, easing = LinearEasing))
        }
        ripple.snapTo(0f)
    }
    AnimatedVisibility(
        visibleState = appear,
        modifier = modifier,
        enter = scaleIn(spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow)) + fadeIn(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (ripple.value > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val scale = 1f + ripple.value * 0.5f
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - ripple.value
                        }
                        .border(2.dp, Sea, CircleShape),
                )
            }
            BandApp(app)
        }
    }
}

/**
 * A notice under the band announcing apps found while the agent was running: 「メッセージアプリを見つけました」
 * over the functions they provide. Shown for five seconds per find.
 */
@Composable
fun DiscoveryNotice(discovery: AppDiscovery?, labelOf: (packageName: String) -> String, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    // Keeps the text while the notice slides away after the discovery is cleared.
    var shown by remember { mutableStateOf<AppDiscovery?>(null) }
    LaunchedEffect(discovery?.id) {
        if (discovery == null) {
            visible = false
            return@LaunchedEffect
        }
        shown = discovery
        visible = true
        delay(5_000)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val current = shown ?: return@AnimatedVisibility
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Ink)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = SeaC)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    discoveredAppsText(current.packageNames.map(labelOf)),
                    color = Bg,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                val functions = discoveredFunctionsText(current.functions)
                if (functions.isNotEmpty()) {
                    Text(
                        functions,
                        color = Line,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** All system apps as one chip, so a device with many of them still fits on one row. */
@Composable
private fun SystemChip(count: Int, runningLabel: String?, modifier: Modifier = Modifier) {
    val running = runningLabel != null
    Text(
        if (running) "システム › $runningLabel" else "システム $count",
        modifier = modifier
            .animateContentSize()
            .widthIn(max = 160.dp)
            .clip(CircleShape)
            .background(if (running) Sea else Surface2)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = if (running) Color.White else Ink2,
        fontSize = 12.sp,
        fontWeight = if (running) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 0 to 1 and back, for the soft glow around the running app. */
@Composable
private fun pulse(): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val value by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    return value
}

@Preview(showBackground = true, backgroundColor = 0xFFFBFAF7, widthDp = 390)
@Composable
private fun AppBandPreview() {
    AppBand(
        AppBandState(
            apps = listOf(
                AppBandApp("trip", "旅行", runningLabel = "旅程の検索"),
                AppBandApp("message", "メッセージ"),
            ),
            systemAppCount = 6,
            isCalling = true,
        ),
    )
}
