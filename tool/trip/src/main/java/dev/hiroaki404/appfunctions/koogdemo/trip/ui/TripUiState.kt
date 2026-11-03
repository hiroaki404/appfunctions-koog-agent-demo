package dev.hiroaki404.appfunctions.koogdemo.trip.ui

import dev.hiroaki404.appfunctions.koogdemo.trip.data.SavedFlightBooking
import dev.hiroaki404.appfunctions.koogdemo.trip.data.SavedItinerary

/** What [TripScreen] displays; it knows nothing about Room or AppFunctions. */
data class TripUiState(
    val itineraries: List<SavedItinerary> = emptyList(),
    val flightBookings: List<SavedFlightBooking> = emptyList(),
)
