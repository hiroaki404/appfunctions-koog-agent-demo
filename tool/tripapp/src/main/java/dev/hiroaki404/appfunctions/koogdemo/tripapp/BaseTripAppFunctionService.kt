package dev.hiroaki404.appfunctions.koogdemo.tripapp

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * The AppFunctions exposed by the travel app.
 *
 * The compiler generates a concrete `TripAppFunctionService` (declared in AndroidManifest.xml)
 * plus `trip_functions.xml` describing the exposed functions.
 */
@AppFunctionServiceEntryPoint(
    serviceName = "TripAppFunctionService",
    appFunctionXmlFileName = "trip_functions",
)
abstract class BaseTripAppFunctionService : AppFunctionService() {
    /** Override in tests or alternate hosts to observe calls without Android logcat. */
    protected open val appFunctionLogger: AppFunctionLogger = AndroidAppFunctionLogger

    /** Shared with the activity through the application; override in tests. */
    protected open val repository: TripRepository by lazy {
        (applicationContext as TripApplication).repository
    }

    /**
     * Gets recommended tourist spots for an area and month from fixed travel guide data.
     *
     * @param area The travel area. Only "北海道" is supported; "Hokkaido" and cities in it, such as
     *   "札幌", correspond to this value.
     * @param month The month of travel as a number from 1 through 12. Data exists only for 12
     *   (December).
     * @return The recommended spots for that area and month.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getTravelInfo(
        @AppFunctionStringValueConstraint(enumValues = ["北海道"]) area: String,
        month: Int,
    ): List<Spot> = executeLogged(
        "getTravelInfo",
        "area=$area, month=$month",
    ) {
        TravelRules.spotsFor(area, month)
            ?: throw AppFunctionInvalidArgumentException(
                "No travel info for area '$area' in month $month. " +
                    "Supported area / month combinations: ${TravelRules.supportedTravelInfo}.",
            )
    }

    /**
     * Saves a new trip itinerary in the travel app and shows it on screen. Existing itineraries
     * are not changed.
     *
     * @param itinerary The itinerary to save, with a non-blank title and at least one day. The day
     *   dates are the real dates of the trip, in `yyyy-MM-dd` format.
     * @return The ID of the saved itinerary, and the itinerary as A2UI messages in `a2ui`. `a2ui`
     *   is only for display by agents that can render it; do not read or quote it.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun saveItinerary(itinerary: Itinerary): SavedItineraryResult = executeLogged(
        "saveItinerary",
        "itinerary=$itinerary",
    ) {
        TravelRules.validate(itinerary)?.let { throw AppFunctionInvalidArgumentException(it) }
        val itineraryId = repository.saveItinerary(itinerary, addedByAi = true)
        SavedItineraryResult(itineraryId, itineraryA2uiJson(itineraryId, itinerary))
    }

    /**
     * Searches the user's saved itineraries, including past trips and trips being planned.
     *
     * @param keyword A short word matched as a partial, case-insensitive match against itinerary
     *   titles and spot names, for example "北海道" or "小樽運河". If null or empty, returns all saved
     *   itineraries.
     * @return The matching itineraries, which may be empty. Each itinerary's day dates show when
     *   the trip takes place; the result has no separate past or upcoming status.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun searchItineraries(keyword: String? = null): List<Itinerary> = executeLogged(
        "searchItineraries",
        "keyword=$keyword",
    ) {
        repository.searchItineraries(keyword)
    }

    /**
     * Books a flight between 東京 and 札幌 and saves the booking in the travel app. The app assigns
     * the flight number and booking number.
     *
     * @param fromCity The departure city. English names, airport names, and airport codes, such as
     *   "Tokyo", "羽田空港", and "CTS", correspond to these Japanese city names.
     * @param toCity The arrival city, in the same form as `fromCity` and different from it.
     * @param date The departure date.
     * @param userApproval The user's approval of this booking. The agent fills this in after asking
     *   the user to confirm; the LLM does not specify it. The booking is rejected unless the user
     *   approved.
     * @return The confirmed booking, including the assigned booking number and flight number.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun bookFlight(
        @AppFunctionStringValueConstraint(enumValues = ["東京", "札幌"]) fromCity: String,
        @AppFunctionStringValueConstraint(enumValues = ["東京", "札幌"]) toCity: String,
        date: LocalDate,
        userApproval: UserApproval,
    ): FlightBooking = executeLogged(
        "bookFlight",
        "fromCity=$fromCity, toCity=$toCity, date=$date, userApproval=$userApproval",
    ) {
        if (!userApproval.approved) {
            throw AppFunctionInvalidArgumentException("The user has not approved this booking.")
        }
        val from = fromCity.trim().takeIf(TravelRules::isSupportedCity)
            ?: throw unsupportedCity("fromCity", fromCity)
        val to = toCity.trim().takeIf(TravelRules::isSupportedCity)
            ?: throw unsupportedCity("toCity", toCity)
        val flightNumber = TravelRules.flightNumberFor(from, to)
            ?: throw AppFunctionInvalidArgumentException(
                "No flight from $from to $to. Available routes: ${TravelRules.supportedRoutes}.",
            )
        // Bookings are stored and returned with the yyyy-MM-dd text the rest of the app uses.
        repository.bookFlight(flightNumber, from, to, date.toString(), addedByAi = true)
    }

    private fun unsupportedCity(parameter: String, value: String) = AppFunctionInvalidArgumentException(
        "$parameter '$value' is not a supported city. Use exactly one of these Japanese city names " +
            "(not English or airport names): ${TravelRules.supportedCities}.",
    )

    private suspend fun <T : Any> executeLogged(
        functionName: String,
        arguments: String,
        block: suspend () -> T,
    ): T = withContext(Dispatchers.IO) {
        appFunctionLogger.started(functionName, arguments)
        try {
            block().also { result -> appFunctionLogger.succeeded(functionName, arguments, result) }
        } catch (error: Exception) {
            appFunctionLogger.failed(functionName, arguments, error)
            throw error
        }
    }
}
