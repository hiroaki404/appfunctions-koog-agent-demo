package dev.hiroaki404.appfunctions.koogdemo.agent

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.AgentTheme
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Bg
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Ink2
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Line
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Sea
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.SeaC
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Surface
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.Surface2
import kotlinx.coroutines.launch

private const val TAG = "AppFunctionDiscovery"

class MainActivity : ComponentActivity() {
    private val viewModel: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appFunctionManager = viewModel.appFunctionManager
        if (appFunctionManager == null) {
            Log.w(TAG, "AppFunctions is not supported on this device")
        } else {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    appFunctionManager.discoverAppFunctions().collect { functions ->
                        Log.d(TAG, "Found ${functions.size} app functions")
                        functions.forEach { function ->
                            val params = function.parameters.joinToString { "${it.name}: ${it.dataType}" }
                            Log.d(TAG, "${function.id}($params) - ${function.description}")
                        }
                    }
                }
            }
        }
        setContent {
            AgentTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AgentChatScreen(
                        viewModel = viewModel,
                        // Edge-to-edge ignores adjustResize, so keep the input row above the keyboard explicitly.
                        modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding(),
                    )
                }
            }
        }
    }
}

@Composable
fun AgentChatScreen(viewModel: AgentViewModel, modifier: Modifier = Modifier) {
    val turns by viewModel.turns.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.lastIndex)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("旅行プランナー", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = viewModel::newConversation) {
                Text("新しい会話", color = Sea, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(turns, key = { it.id }) { turn ->
                ChatTurnItem(turn)
            }
        }
        ChatInputBar(
            prompt = prompt,
            onPromptChange = { prompt = it },
            isRunning = isRunning,
            sendEnabled = !isRunning && prompt.isNotBlank() && viewModel.appFunctionManager != null,
            onSend = {
                viewModel.send(prompt)
                prompt = ""
            },
        )
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
private fun ChatTurnItem(turn: ChatTurn, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PromptBubble(turn.prompt)
        turn.toolCalls.calls.forEach { call ->
            ToolCallCard(call = call)
        }
        turn.response?.let { response ->
            Text(
                plainAnswerText(response),
                color = Ink,
                fontSize = 15.sp,
                lineHeight = 24.75.sp,
            )
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

@Composable
private fun ToolCallCard(call: ToolCallUiModel, modifier: Modifier = Modifier) {
    val displayName = toolDisplayNameOf(call.functionName)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Bg)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolStatusIcon(call.status)
            Text(
                displayName.function,
                modifier = Modifier.weight(1f, fill = false),
                color = Ink,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            displayName.appLabel?.let { label ->
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
        }
        Text(
            call.arguments,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Surface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            color = Ink,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 19.5.sp,
        )
        call.result?.let { result ->
            Text(
                "→ $result",
                color = Ink2,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        call.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
    }
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
private fun StatusCircle(
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
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
