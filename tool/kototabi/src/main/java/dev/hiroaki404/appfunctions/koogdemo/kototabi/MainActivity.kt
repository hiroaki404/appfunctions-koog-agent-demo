package dev.hiroaki404.appfunctions.koogdemo.kototabi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.KototabiScreen
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.KototabiViewModel
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ui.theme.KototabiTheme

class MainActivity : ComponentActivity() {
    private val viewModel: KototabiViewModel by viewModels {
        viewModelFactory {
            initializer { KototabiViewModel((applicationContext as KototabiApplication).repository) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KototabiTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KototabiScreen(
                        state = state,
                        onSeedUpcomingTrip = viewModel::seedUpcomingTrip,
                        onReset = viewModel::reset,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
