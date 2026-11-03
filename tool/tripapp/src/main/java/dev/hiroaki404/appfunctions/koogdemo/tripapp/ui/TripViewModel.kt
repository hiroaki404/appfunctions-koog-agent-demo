package dev.hiroaki404.appfunctions.koogdemo.tripapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripViewModel(private val repository: TripRepository) : ViewModel() {
    val uiState: StateFlow<TripUiState> = combine(
        repository.savedItineraries,
        repository.savedFlightBookings,
    ) { itineraries, flightBookings ->
        TripUiState(
            // Newest trips first, judged by the first day's date (yyyy-MM-dd sorts as text).
            itineraries = itineraries.sortedByDescending { it.itinerary.days.firstOrNull()?.date.orEmpty() },
            flightBookings = flightBookings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripUiState())

    fun seedUpcomingTrip() {
        viewModelScope.launch { repository.seedUpcomingTrip() }
    }

    fun reset() {
        viewModelScope.launch { repository.resetToInitialState() }
    }
}
