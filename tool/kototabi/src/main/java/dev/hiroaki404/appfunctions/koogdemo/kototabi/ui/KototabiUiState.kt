package dev.hiroaki404.appfunctions.koogdemo.kototabi.ui

import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.SavedFlightBooking
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.SavedItinerary

/** What [KototabiScreen] displays; it knows nothing about Room or AppFunctions. */
data class KototabiUiState(
    val itineraries: List<SavedItinerary> = emptyList(),
    val flightBookings: List<SavedFlightBooking> = emptyList(),
)
