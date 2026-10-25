package dev.hiroaki404.appfunctions.koogdemo.kototabi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.KototabiRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KototabiViewModel(private val repository: KototabiRepository) : ViewModel() {
    val uiState: StateFlow<KototabiUiState> = combine(
        repository.savedItineraries,
        repository.savedFlightBookings,
    ) { itineraries, flightBookings ->
        KototabiUiState(
            // Newest trips first, judged by the first day's date (yyyy-MM-dd sorts as text).
            itineraries = itineraries.sortedByDescending { it.itinerary.days.firstOrNull()?.date.orEmpty() },
            flightBookings = flightBookings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KototabiUiState())

    fun seedUpcomingTrip() {
        viewModelScope.launch { repository.seedUpcomingTrip() }
    }

    fun reset() {
        viewModelScope.launch { repository.resetToInitialState() }
    }
}
