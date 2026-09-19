package dev.hiroaki404.appfunctions.koogdemo.agent

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appfunctions.AppFunctionManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.AgentTheme
import kotlinx.coroutines.launch

private const val TAG = "AppFunctionDiscovery"
private const val AGENT_TAG = "KoogAgent"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appFunctionManager = AppFunctionManager.getInstance(this)
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
                        appFunctionManager = appFunctionManager,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
fun AgentChatScreen(appFunctionManager: AppFunctionManager?, modifier: Modifier = Modifier) {
    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ask the agent (e.g. \"what is 123 plus 456\" or calculate a total)") },
        )
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(
                enabled = !isRunning && appFunctionManager != null,
                onClick = {
                    val manager = appFunctionManager ?: return@Button
                    val currentPrompt = prompt
                    isRunning = true
                    scope.launch {
                        response = try {
                            runAgent(BuildConfig.GEMINI_API_KEY, manager, currentPrompt)
                        } catch (e: Exception) {
                            Log.e(AGENT_TAG, "Agent run failed", e)
                            "Error: ${e.message}"
                        } finally {
                            isRunning = false
                        }
                        Log.d(AGENT_TAG, "response: $response")
                    }
                },
            ) {
                Text("Send")
            }
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.padding(start = 16.dp))
            }
        }
        Text(text = response, modifier = Modifier.padding(top = 16.dp))
    }
}
