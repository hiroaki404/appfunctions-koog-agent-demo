package dev.hiroaki404.appfunctions.koogdemo.tripapp.data

import androidx.room.withTransaction
import dev.hiroaki404.appfunctions.koogdemo.tripapp.FlightBooking
import dev.hiroaki404.appfunctions.koogdemo.tripapp.Itinerary
import dev.hiroaki404.appfunctions.koogdemo.tripapp.TravelRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single access point for trip data, shared by the AppFunction service and the screen.
 * Inputs are expected to be already validated (see [TravelRules]).
 */
class TripRepository(private val database: TripDatabase) {
    private val dao = database.dao()

    val savedItineraries: Flow<List<SavedItinerary>> =
        dao.observeItineraries().map { entities -> entities.map { it.toSavedItinerary() } }

    val savedFlightBookings: Flow<List<SavedFlightBooking>> =
        dao.observeFlightBookings().map { entities -> entities.map { it.toSavedFlightBooking() } }

    val itineraries: Flow<List<Itinerary>> = savedItineraries.map { saved -> saved.map { it.itinerary } }

    val flightBookings: Flow<List<FlightBooking>> = savedFlightBookings.map { saved -> saved.map { it.booking } }

    /** Saves [itinerary] and returns its id. [addedByAi] marks it as written by the AI agent. */
    suspend fun saveItinerary(itinerary: Itinerary, addedByAi: Boolean = false): String =
        dao.insertItinerary(itinerary.toEntity(addedByAi = addedByAi)).toString()

    /** Returns itineraries matching [keyword] on title or spot names; null or blank returns all. */
    suspend fun searchItineraries(keyword: String?): List<Itinerary> =
        dao.getItineraries()
            .map { it.toItinerary() }
            .filter { TravelRules.matches(it, keyword) }

    /** Saves a booking, assigning the next booking number, and returns it. */
    suspend fun bookFlight(
        flightNumber: String,
        from: String,
        to: String,
        date: String,
        addedByAi: Boolean = false,
    ): FlightBooking =
        database.withTransaction {
            val entity = FlightBookingEntity(
                sequence = dao.maxFlightSequence() + 1,
                flightNumber = flightNumber,
                fromCity = from,
                toCity = to,
                date = date,
                addedByAi = addedByAi,
            )
            dao.insertFlightBooking(entity)
            entity.toFlightBooking()
        }

    /** Debug action: adds the sample December trip to Hokkaido. */
    suspend fun seedUpcomingTrip() {
        dao.insertItinerary(TripSeed.upcomingTrip.toEntity())
    }

    /** Debug action: clears everything and restores the initial state with only the past trip. */
    suspend fun resetToInitialState() {
        database.withTransaction {
            dao.deleteAllItineraries()
            dao.deleteAllFlightBookings()
            dao.insertItinerary(TripSeed.pastTrip.toEntity())
        }
    }
}
