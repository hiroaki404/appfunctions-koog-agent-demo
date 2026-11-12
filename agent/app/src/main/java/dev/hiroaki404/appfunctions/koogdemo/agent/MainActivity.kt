package dev.hiroaki404.appfunctions.koogdemo.agent

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.AgentTheme
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink2
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Sea
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.SeaC
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Surface2

private const val TAG = "AppFunctionDiscovery"

class MainActivity : ComponentActivity() {
    private val viewModel: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (viewModel.appFunctionManager == null) {
            Log.w(TAG, "AppFunctions is not supported on this device")
        }
        setContent {
            AgentTheme {
                var showTools by rememberSaveable { mutableStateOf(false) }
                fun closeTools() {
                    showTools = false
                    viewModel.acknowledgeNewTools()
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
                    if (showTools) {
                        BackHandler(onBack = ::closeTools)
                        val availableTools by viewModel.availableTools.collectAsStateWithLifecycle()
                        ToolListScreen(
                            state = availableTools,
                            providerAppOf = viewModel::providerAppOf,
                            onBack = ::closeTools,
                            modifier = contentModifier,
                        )
                    } else {
                        AgentChatScreen(
                            viewModel = viewModel,
                            onOpenTools = { showTools = true },
                            // Edge-to-edge ignores adjustResize, so keep the input row above the keyboard explicitly.
                            modifier = contentModifier.imePadding(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentChatScreen(viewModel: AgentViewModel, onOpenTools: () -> Unit, modifier: Modifier = Modifier) {
    val turns by viewModel.turns.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val availableTools by viewModel.availableTools.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf("") }
    var showSampleDialog by remember { mutableStateOf(false) }

    if (showSampleDialog) {
        AlertDialog(
            onDismissRequest = { showSampleDialog = false },
            title = { Text("見本の会話を読み込みますか？") },
            text = { Text("UI 確認用のデバッグ機能です。今の会話は消えます。") },
            confirmButton = {
                TextButton(onClick = {
                    showSampleDialog = false
                    viewModel.loadSampleTurns()
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showSampleDialog = false }) { Text("キャンセル") }
            },
        )
    }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val toolGroups = remember(availableTools) { availableTools.groups(viewModel::providerAppOf) }
    val bandState = remember(toolGroups, turns, availableTools.discovery) {
        appBandState(toolGroups, turns.activeToolCall(), availableTools.discovery)
    }

    // Follow the conversation as it grows: a new turn, each tool call card and the answer all land
    // at the bottom, so scroll there on every change.
    LaunchedEffect(turns) {
        if (turns.isNotEmpty()) listState.smoothScrollToEnd()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .clearFocusOnTapOutsideInput()
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "旅行プランナー",
                // Debug builds only: a long press offers to load a sample conversation for checking the UI.
                modifier = if (BuildConfig.DEBUG) {
                    Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { showSampleDialog = true }) }
                } else {
                    Modifier
                },
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ToolCountPill(state = availableTools, onClick = onOpenTools)
                TextButton(onClick = viewModel::newConversation) {
                    Text("新しい会話", color = Sea, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        AppBand(state = bandState, modifier = Modifier.clearFocusOnTapOutsideInput())
        DiscoveryNotice(
            discovery = availableTools.discovery,
            labelOf = { packageName -> viewModel.providerAppOf(packageName).label },
            modifier = Modifier.clearFocusOnTapOutsideInput(),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .clearFocusOnTapOutsideInput()
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(turns, key = { it.id }) { turn ->
                ChatTurnItem(
                    turn,
                    onApproval = { functionName, approved -> viewModel.respondToApproval(turn.id, functionName, approved) },
                )
            }
            // End marker for the auto-scroll above; takes no space.
            item(key = "end") { Spacer(Modifier.height(0.dp)) }
        }
        ChatInputBar(
            prompt = prompt,
            onPromptChange = { prompt = it },
            isRunning = isRunning,
            sendEnabled = !isRunning && prompt.isNotBlank() && viewModel.appFunctionManager != null,
            onSend = {
                viewModel.send(prompt)
                prompt = ""
                // Hide the keyboard so the tool call cards stay visible while the agent runs.
                focusManager.clearFocus()
                keyboardController?.hide()
            },
        )
    }
}

/** Scroll speed toward the end of the conversation, in pixels per millisecond. */
private const val ScrollPixelsPerMillisecond = 1.2f

/**
 * Scrolls to the end of the list at a calm speed that the audience can follow, instead of
 * `animateScrollToItem`'s quick jump. Expects the list to end with a zero-height marker item.
 */
private suspend fun LazyListState.smoothScrollToEnd() {
    // Let the new content be measured before reading the layout.
    withFrameNanos { }
    val info = layoutInfo
    val last = info.visibleItemsInfo.lastOrNull() ?: return
    val endIndex = info.totalItemsCount - 1
    if (last.index < endIndex - 1) {
        // Far from the end, e.g. a whole sample conversation loaded at once: no need to animate.
        scrollToItem(endIndex)
        return
    }
    // After the last visible item comes at most one spacing and the zero-height end marker.
    val trailing = if (last.index == endIndex) 0 else info.mainAxisItemSpacing
    val distance = last.offset + last.size + trailing + info.afterContentPadding - info.viewportEndOffset
    if (distance <= 0) return
    val millis = (distance / ScrollPixelsPerMillisecond).toInt().coerceIn(350, 1000)
    animateScrollBy(distance.toFloat(), tween(millis, easing = FastOutSlowInEasing))
}

/**
 * Clears focus and hides the keyboard when a pointer goes down on this element.
 * Watches the Initial pass without consuming, so child clicks and scrolling keep working.
 * Apply it to everything except the input bar so that tapping the input field keeps the keyboard open.
 */
@Composable
private fun Modifier.clearFocusOnTapOutsideInput(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return pointerInput(focusManager, keyboardController) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
}

@Composable
private fun ChatInputBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    isRunning: Boolean,
    sendEnabled: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = Ink, fontSize = 15.sp, lineHeight = 22.sp),
            cursorBrush = SolidColor(Sea),
            maxLines = 4,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface2)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (prompt.isEmpty()) {
                        Text("依頼を入力", color = Ink2, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            },
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (sendEnabled || isRunning) Sea else Sea.copy(alpha = 0.38f))
                .clickable(enabled = sendEnabled, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    strokeWidth = 2.5.dp,
                )
            } else {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = "送信", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ChatTurnItem(
    turn: ChatTurn,
    modifier: Modifier = Modifier,
    onApproval: (functionName: String, approved: Boolean) -> Unit = { _, _ -> },
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PromptBubble(turn.prompt)
        turn.toolCalls.calls.forEach { call ->
            ToolCallCard(
                call = call,
                onApprove = { onApproval(call.functionName, true) },
                onDecline = { onApproval(call.functionName, false) },
            )
        }
        if (turn.isThinking) {
            ThinkingIndicator()
        }
        turn.response?.let { response ->
            AnswerText(response)
        }
        turn.error?.let { error ->
            Text(
                "エラー: $error",
                color = MaterialTheme.colorScheme.error,
                fontSize = 15.sp,
                lineHeight = 24.75.sp,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFBFAF7, widthDp = 390)
@Composable
private fun ChatTurnAwaitingApprovalPreview() {
    AgentTheme {
        ChatTurnItem(ApprovalSampleTurn, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun ThinkingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            // Each dot fades in and out on the same cycle, offset so the pulse travels left to right.
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.25f at index * 200
                        1f at index * 200 + 300
                        0.25f at index * 200 + 600
                    },
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(Sea),
            )
        }
        Text("考え中", modifier = Modifier.padding(start = 4.dp), color = Ink2, fontSize = 13.sp)
    }
}

@Composable
private fun PromptBubble(prompt: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            prompt,
            modifier = Modifier
                .widthIn(max = maxWidth * 0.86f)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 4.dp, bottomStart = 18.dp))
                .background(SeaC)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            color = Ink,
            fontSize = 15.sp,
            lineHeight = 23.25.sp,
        )
    }
}
