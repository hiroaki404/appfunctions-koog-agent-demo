package dev.hiroaki404.appfunctions.koogdemo.kototabi.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.hiroaki404.appfunctions.koogdemo.kototabi.FlightBooking
import dev.hiroaki404.appfunctions.koogdemo.kototabi.Itinerary
import dev.hiroaki404.appfunctions.koogdemo.kototabi.ItineraryDay
import dev.hiroaki404.appfunctions.koogdemo.kototabi.Spot
import dev.hiroaki404.appfunctions.koogdemo.kototabi.TravelRules
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A stored itinerary. To keep the demo simple, the days and their spots are stored together in the
 * single [daysJson] column. A real product should normalize days and spots into separate tables and
 * read them with `@Relation`.
 */
@Entity(tableName = ItineraryEntity.TABLE_NAME)
data class ItineraryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val daysJson: String,
    /** True when the AI agent created this itinerary through an AppFunction. */
    val addedByAi: Boolean = false,
) {
    companion object {
        const val TABLE_NAME = "itineraries"
    }
}

/** A booked flight. [sequence] is the number behind the booking id, such as 1 for `KTB-0001`. */
@Entity(tableName = FlightBookingEntity.TABLE_NAME)
data class FlightBookingEntity(
    @PrimaryKey val sequence: Int,
    val flightNumber: String,
    val fromCity: String,
    val toCity: String,
    val date: String,
    /** True when the AI agent made this booking through an AppFunction. */
    val addedByAi: Boolean = false,
) {
    companion object {
        const val TABLE_NAME = "flight_bookings"
    }
}

// The AppFunction data classes are not @Serializable on purpose; these private DTOs only describe
// how days are laid out in ItineraryEntity.daysJson.
@Serializable
private data class StoredSpot(val name: String, val description: String)

@Serializable
private data class StoredDay(val date: String, val spots: List<StoredSpot>)

private val json = Json

fun Itinerary.toEntity(id: Long = 0, addedByAi: Boolean = false): ItineraryEntity {
    val stored = days.map { day ->
        StoredDay(day.date, day.spots.map { StoredSpot(it.name, it.description) })
    }
    return ItineraryEntity(id = id, title = title, daysJson = json.encodeToString(stored), addedByAi = addedByAi)
}

fun ItineraryEntity.toItinerary(): Itinerary {
    val stored = json.decodeFromString<List<StoredDay>>(daysJson)
    return Itinerary(
        title = title,
        days = stored.map { day -> ItineraryDay(day.date, day.spots.map { Spot(it.name, it.description) }) },
    )
}

fun FlightBookingEntity.toFlightBooking(): FlightBooking = FlightBooking(
    bookingId = TravelRules.bookingId(sequence),
    flightNumber = flightNumber,
    from = fromCity,
    to = toCity,
    date = date,
)

/** An itinerary together with whether the AI agent added it; what the screen displays. */
data class SavedItinerary(val itinerary: Itinerary, val addedByAi: Boolean)

/** A flight booking together with whether the AI agent made it; what the screen displays. */
data class SavedFlightBooking(val booking: FlightBooking, val addedByAi: Boolean)

fun ItineraryEntity.toSavedItinerary(): SavedItinerary = SavedItinerary(toItinerary(), addedByAi)

fun FlightBookingEntity.toSavedFlightBooking(): SavedFlightBooking = SavedFlightBooking(toFlightBooking(), addedByAi)
