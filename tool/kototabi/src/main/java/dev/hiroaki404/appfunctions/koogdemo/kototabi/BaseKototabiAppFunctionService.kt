package dev.hiroaki404.appfunctions.koogdemo.kototabi

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.KototabiRepository

/**
 * The AppFunctions exposed by the kototabi travel app.
 *
 * The compiler generates a concrete `KototabiAppFunctionService` (declared in AndroidManifest.xml)
 * plus `kototabi_functions.xml` describing the exposed functions.
 */
@AppFunctionServiceEntryPoint(
    serviceName = "KototabiAppFunctionService",
    appFunctionXmlFileName = "kototabi_functions",
)
abstract class BaseKototabiAppFunctionService : AppFunctionService() {
    /** Override in tests or alternate hosts to observe calls without Android logcat. */
    protected open val appFunctionLogger: AppFunctionLogger = AndroidAppFunctionLogger

    /** Shared with the activity through the application; override in tests. */
    protected open val repository: KototabiRepository by lazy {
        (applicationContext as KototabiApplication).repository
    }

    /**
     * Get recommended tourist spots for an area in a given month, from fixed travel guide data.
     *
     * @param area The travel area in Japanese, for example "北海道". English names such as
     *   "Hokkaido" are also accepted.
     * @param month The month of travel as a number from 1 through 12, for example 12 for December.
     * @return The recommended spots for that area and month.
     * @throws AppFunctionInvalidArgumentException If the area and month combination is not
     *   supported. The message lists the supported combinations; choose one of them and retry.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getTravelInfo(area: String, month: Int): List<Spot> = executeLogged(
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
     * Save a trip itinerary in the kototabi app so the user can see it on screen.
     *
     * Whether a trip is in the past or upcoming is judged only by the dates of its days, so use
     * the real dates of the trip.
     *
     * @param itinerary The itinerary to save. It needs a title and at least one day; every day
     *   date must be in `yyyy-MM-dd` format, for example "2026-12-20".
     * @return The ID of the saved itinerary.
     * @throws AppFunctionInvalidArgumentException If the title is blank, there are no days, a date
     *   is not a valid `yyyy-MM-dd` date, or a spot name is blank. Correct the value and retry.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun saveItinerary(itinerary: Itinerary): String = executeLogged(
        "saveItinerary",
        "itinerary=$itinerary",
    ) {
        TravelRules.validate(itinerary)?.let { throw AppFunctionInvalidArgumentException(it) }
        repository.saveItinerary(itinerary, addedByAi = true)
    }

    /**
     * Search the user's saved itineraries, including trips they took in the past.
     *
     * Use this to look up past trips (for example last year's trip to Hokkaido) or the trip that is
     * currently being planned. Whether an itinerary is in the past or upcoming is judged by the
     * dates of its days compared with today's date.
     *
     * @param keyword A short word to look for, matched as a partial, case-insensitive match against
     *   itinerary titles and spot names, for example "北海道" or "小樽運河". Omit it, or pass an empty
     *   string, to list all saved itineraries.
     * @return The matching itineraries, which may be empty.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun searchItineraries(keyword: String? = null): List<Itinerary> = executeLogged(
        "searchItineraries",
        "keyword=$keyword",
    ) {
        repository.searchItineraries(keyword)
    }

    /**
     * Book a flight between two Japanese cities and save the booking in the kototabi app.
     *
     * The flight number and booking number are decided by the app and returned; do not make them up.
     *
     * @param from The departure city as a Japanese city name, for example "東京". Use the city
     *   name, not an airport name or airport code.
     * @param to The arrival city as a Japanese city name, for example "札幌". Use the city name,
     *   not an airport name or airport code.
     * @param date The departure date in `yyyy-MM-dd` format, for example "2026-12-20".
     * @return The confirmed booking, including the booking number and flight number.
     * @throws AppFunctionInvalidArgumentException If the date is not a valid `yyyy-MM-dd` date, a
     *   city is not supported, or no flight is operated between the cities. The message lists the
     *   supported cities or routes; correct the input and retry.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun bookFlight(from: String, to: String, date: String): FlightBooking = executeLogged(
        "bookFlight",
        "from=$from, to=$to, date=$date",
    ) {
        if (!TravelRules.isValidDate(date)) {
            throw AppFunctionInvalidArgumentException(
                "date '$date' must be a valid date in yyyy-MM-dd format, for example 2026-12-20.",
            )
        }
        val fromCity = TravelRules.normalizeCity(from)
            ?: throw unsupportedCity("from", from)
        val toCity = TravelRules.normalizeCity(to)
            ?: throw unsupportedCity("to", to)
        val flightNumber = TravelRules.flightNumberFor(fromCity, toCity)
            ?: throw AppFunctionInvalidArgumentException(
                "No flight from $fromCity to $toCity. Available routes: ${TravelRules.supportedRoutes}.",
            )
        repository.bookFlight(flightNumber, fromCity, toCity, date, addedByAi = true)
    }

    private fun unsupportedCity(parameter: String, value: String) = AppFunctionInvalidArgumentException(
        "$parameter '$value' is not a supported city. Use one of these Japanese city names " +
            "(not airport names): ${TravelRules.supportedCities}.",
    )

    private inline fun <T : Any> executeLogged(
        functionName: String,
        arguments: String,
        block: () -> T,
    ): T {
        appFunctionLogger.started(functionName, arguments)
        return try {
            block().also { result -> appFunctionLogger.succeeded(functionName, arguments, result) }
        } catch (error: Exception) {
            appFunctionLogger.failed(functionName, arguments, error)
            throw error
        }
    }
}
