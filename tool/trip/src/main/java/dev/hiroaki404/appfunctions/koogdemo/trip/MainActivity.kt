package dev.hiroaki404.appfunctions.koogdemo.trip

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
import dev.hiroaki404.appfunctions.koogdemo.trip.ui.TripScreen
import dev.hiroaki404.appfunctions.koogdemo.trip.ui.TripViewModel
import dev.hiroaki404.appfunctions.koogdemo.trip.ui.theme.TripTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TripViewModel by viewModels {
        viewModelFactory {
            initializer { TripViewModel((applicationContext as TripApplication).repository) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TripScreen(
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
