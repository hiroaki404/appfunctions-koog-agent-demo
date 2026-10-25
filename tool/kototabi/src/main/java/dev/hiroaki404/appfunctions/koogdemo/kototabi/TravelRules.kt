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

    private val areaAliases = mapOf(
        "北海道" to "北海道",
        "hokkaido" to "北海道",
        "札幌" to "北海道",
        "sapporo" to "北海道",
        "沖縄" to "沖縄",
        "okinawa" to "沖縄",
    )

    private val travelInfo: Map<Pair<String, Int>, List<Spot>> = mapOf(
        ("北海道" to 12) to listOf(
            Spot("さっぽろホワイトイルミネーション", "札幌の大通公園などを光で彩る冬の風物詩。12月の札幌観光の定番。"),
            Spot("小樽運河", "ガス灯と石造り倉庫が並ぶ運河。冬は雪景色の中の夜景が美しい。"),
            Spot("旭山動物園", "旭川にある人気の動物園。冬はペンギンの散歩やホッキョクグマの観察が見どころ。"),
            Spot("定山渓温泉", "札幌近郊の温泉地。雪見の露天風呂でゆっくり過ごせる。"),
            Spot("札幌ラーメン横丁", "すすきのにあるラーメン店街。味噌ラーメンで体を温められる。"),
        ),
        ("沖縄" to 7) to listOf(
            Spot("美ら海水族館", "巨大水槽でジンベエザメが泳ぐ沖縄を代表する水族館。"),
            Spot("古宇利島", "古宇利大橋で渡れる離島。夏はエメラルドグリーンの海が楽しめる。"),
            Spot("首里城公園", "琉球王国の歴史を伝える城跡。"),
        ),
    )

    /** The `area / month` combinations [spotsFor] can answer, for error messages. */
    val supportedTravelInfo: String
        get() = travelInfo.keys.joinToString(", ") { (area, month) -> "$area / ${month}月" }

    /** Returns the fixed spots for [area] and [month], or null for an unsupported combination. */
    fun spotsFor(area: String, month: Int): List<Spot>? {
        val key = areaAliases[area.trim().lowercase(Locale.ROOT)] ?: return null
        return travelInfo[key to month]
    }

    private val cityAliases = mapOf(
        "東京" to "東京",
        "tokyo" to "東京",
        "札幌" to "札幌",
        "sapporo" to "札幌",
        "大阪" to "大阪",
        "osaka" to "大阪",
        "那覇" to "那覇",
        "naha" to "那覇",
    )

    private val flightNumbers = mapOf(
        ("東京" to "札幌") to "KT501",
        ("札幌" to "東京") to "KT502",
        ("東京" to "大阪") to "KT101",
        ("大阪" to "東京") to "KT102",
        ("東京" to "那覇") to "KT301",
        ("那覇" to "東京") to "KT302",
    )

    /** The Japanese city names [normalizeCity] accepts, for error messages. */
    val supportedCities: String
        get() = flightNumbers.keys.flatMap { it.toList() }.distinct().joinToString("、")

    /** Returns the canonical Japanese city name for [value], or null if the city is unsupported. */
    fun normalizeCity(value: String): String? = cityAliases[value.trim().removeSuffix("市").lowercase(Locale.ROOT)]

    /** Returns the flight number for a normalized city pair, or null if no flight is operated. */
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
