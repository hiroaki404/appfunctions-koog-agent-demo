package dev.hiroaki404.appfunctions.koogdemo.tripapp.ui

import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.SavedFlightBooking
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.SavedItinerary
import java.time.LocalDate

/** What [TripScreen] displays; it knows nothing about Room or AppFunctions. */
data class TripUiState(
    /** Trips that have not ended yet, the soonest first. */
    val upcoming: List<SavedItinerary> = emptyList(),
    /** Trips that have ended, the newest first; shown as records. */
    val past: List<SavedItinerary> = emptyList(),
    val flightBookings: List<SavedFlightBooking> = emptyList(),
    /** The date the countdown is measured from. */
    val today: LocalDate = LocalDate.now(),
)

/**
 * Splits [itineraries] by whether their last day is before [today]. A trip whose dates do not
 * parse stays upcoming, so it is still shown with the full card.
 */
fun tripUiState(
    itineraries: List<SavedItinerary>,
    flightBookings: List<SavedFlightBooking>,
    today: LocalDate,
): TripUiState {
    val (past, upcoming) = itineraries.partition { saved ->
        val lastDay = saved.itinerary.days.lastOrNull()?.date?.let(::parseDate)
        lastDay != null && lastDay < today
    }
    // yyyy-MM-dd sorts as text.
    fun firstDate(saved: SavedItinerary) = saved.itinerary.days.firstOrNull()?.date.orEmpty()
    return TripUiState(
        upcoming = upcoming.sortedBy(::firstDate),
        past = past.sortedByDescending(::firstDate),
        flightBookings = flightBookings,
        today = today,
    )
}
