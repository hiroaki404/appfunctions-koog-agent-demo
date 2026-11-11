package dev.hiroaki404.appfunctions.koogdemo.tripapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TripViewModel(
    private val repository: TripRepository,
    /** Today's date, read on every update; replaceable so the upcoming/past split can be pinned. */
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val uiState: StateFlow<TripUiState> = combine(
        repository.savedItineraries,
        repository.savedFlightBookings,
    ) { itineraries, flightBookings ->
        tripUiState(itineraries, flightBookings, today())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripUiState())

    fun seedUpcomingTrip() {
        viewModelScope.launch { repository.seedUpcomingTrip() }
    }

    fun reset() {
        viewModelScope.launch { repository.resetToInitialState() }
    }
}
