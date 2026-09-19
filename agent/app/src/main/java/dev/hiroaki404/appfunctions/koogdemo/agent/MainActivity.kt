package dev.hiroaki404.appfunctions.koogdemo.agent

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.appfunctions.AppFunctionManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.hiroaki404.appfunctions.koogdemo.agent.ui.theme.AgentTheme
import kotlinx.coroutines.launch

private const val TAG = "AppFunctionDiscovery"

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
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AgentTheme {
        Greeting("Android")
    }
}