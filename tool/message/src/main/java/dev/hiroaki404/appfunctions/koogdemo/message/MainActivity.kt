package dev.hiroaki404.appfunctions.koogdemo.message

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hiroaki404.appfunctions.koogdemo.message.ui.theme.MessageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as MessageApplication).messageRepository
        setContent {
            MessageTheme {
                val viewModel = viewModel { MessageViewModel(repository) }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                // MessageScreen handles the system bar and IME insets itself.
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0),
                ) { innerPadding ->
                    MessageScreen(state = state, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
