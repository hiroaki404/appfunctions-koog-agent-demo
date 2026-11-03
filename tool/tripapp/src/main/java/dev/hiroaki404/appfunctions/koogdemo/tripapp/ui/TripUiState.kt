package dev.hiroaki404.appfunctions.koogdemo.tripapp.ui

import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.SavedFlightBooking
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.SavedItinerary

/** What [TripScreen] displays; it knows nothing about Room or AppFunctions. */
data class TripUiState(
    val itineraries: List<SavedItinerary> = emptyList(),
    val flightBookings: List<SavedFlightBooking> = emptyList(),
)
