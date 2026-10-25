package dev.hiroaki404.appfunctions.koogdemo.kototabi

import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Pure travel logic (fixed data tables, input normalization, validation, matching) kept free of
 * Android APIs so it can be unit tested on the JVM.
 */
object TravelRules {
    /** Whether [value] is a real calendar date in the `yyyy-MM-dd` format used with the LLM. */
    fun isValidDate(value: String): Boolean = try {
        LocalDate.parse(value)
        true
    } catch (_: DateTimeParseException) {
        false
    }

    // Area and city names must match exactly (only surrounding whitespace is ignored). Other names
    // for the same place, such as "Hokkaido" or an airport name, are left to the calling LLM: the
    // AppFunction descriptions list the accepted names and ask it to map to them.

    private val travelInfo: Map<Pair<String, Int>, List<Spot>> = mapOf(
        ("北海道" to 12) to listOf(
            Spot("さっぽろホワイトイルミネーション", "札幌の大通公園などを光で彩る冬の風物詩。12月の札幌観光の定番。"),
            Spot("小樽運河", "ガス灯と石造り倉庫が並ぶ運河。冬は雪景色の中の夜景が美しい。"),
            Spot("旭山動物園", "旭川にある人気の動物園。冬はペンギンの散歩やホッキョクグマの観察が見どころ。"),
            Spot("定山渓温泉", "札幌近郊の温泉地。雪見の露天風呂でゆっくり過ごせる。"),
            Spot("札幌ラーメン横丁", "すすきのにあるラーメン店街。味噌ラーメンで体を温められる。"),
        ),
    )

    /** The `area / month` combinations [spotsFor] can answer, for error messages. */
    val supportedTravelInfo: String
        get() = travelInfo.keys.joinToString(", ") { (area, month) -> "$area / ${month}月" }

    /** Returns the fixed spots for [area] and [month], or null for an unsupported combination. */
    fun spotsFor(area: String, month: Int): List<Spot>? = travelInfo[area.trim() to month]

    private val flightNumbers = mapOf(
        ("東京" to "札幌") to "KT501",
        ("札幌" to "東京") to "KT502",
    )

    private val cities = flightNumbers.keys.flatMap { it.toList() }.distinct()

    /** The Japanese city names [isSupportedCity] accepts, for error messages. */
    val supportedCities: String
        get() = cities.joinToString("、")

    /** Whether [city] is one of the Japanese city names flights are operated between. */
    fun isSupportedCity(city: String): Boolean = city in cities

    /** Returns the flight number for a city pair, or null if no flight is operated. */
    fun flightNumberFor(from: String, to: String): String? = flightNumbers[from to to]

    /** Every operated route as `東京→札幌 (KT501)` entries, for error messages. */
    val supportedRoutes: String
        get() = flightNumbers.entries.joinToString(", ") { (route, number) ->
            "${route.first}→${route.second} ($number)"
        }

    /** Formats the sequence number assigned by the app as a booking id, for example `KTB-0001`. */
    fun bookingId(sequence: Int): String = "KTB-%04d".format(Locale.ROOT, sequence)

    /**
     * Whether [itinerary] matches [keyword] by case-insensitive partial match on the title or any
     * spot name. A null or blank keyword matches everything.
     */
    fun matches(itinerary: Itinerary, keyword: String?): Boolean {
        val needle = keyword?.trim().orEmpty()
        if (needle.isEmpty()) return true
        return itinerary.title.contains(needle, ignoreCase = true) ||
            itinerary.days.any { day -> day.spots.any { it.name.contains(needle, ignoreCase = true) } }
    }

    /** Returns a description of the first problem in [itinerary], or null if it can be saved. */
    fun validate(itinerary: Itinerary): String? {
        if (itinerary.title.isBlank()) return "title must not be blank."
        if (itinerary.days.isEmpty()) return "days must contain at least one day."
        itinerary.days.forEach { day ->
            if (!isValidDate(day.date)) {
                return "day date '${day.date}' must be a valid date in yyyy-MM-dd format, for example 2026-12-20."
            }
            if (day.spots.any { it.name.isBlank() }) {
                return "every spot on ${day.date} must have a non-blank name."
            }
        }
        return null
    }
}
