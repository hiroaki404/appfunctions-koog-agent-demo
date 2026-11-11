package dev.hiroaki404.appfunctions.koogdemo.tripapp.ui

import dev.hiroaki404.appfunctions.koogdemo.tripapp.Itinerary
import dev.hiroaki404.appfunctions.koogdemo.tripapp.ItineraryDay
import dev.hiroaki404.appfunctions.koogdemo.tripapp.Spot
import dev.hiroaki404.appfunctions.koogdemo.tripapp.TravelRules
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.SavedItinerary
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TripScreenModelTest {
    private fun trip(vararg dates: String, title: String = "旅") =
        Itinerary(title, dates.map { ItineraryDay(it, listOf(Spot("小樽運河", ""))) })

    @Test
    fun spotKindsFromNamesThenDescriptions() {
        fun kind(name: String, description: String = "") = spotKindOf(Spot(name, description))

        assertEquals(SpotKind.HotSpring, kind("定山渓温泉"))
        assertEquals(SpotKind.Animals, kind("旭山動物園"))
        // 「水族館」 is checked before the 「海」 in the name.
        assertEquals(SpotKind.Animals, kind("美ら海水族館"))
        assertEquals(SpotKind.Water, kind("小樽運河"))
        assertEquals(SpotKind.Lights, kind("さっぽろホワイトイルミネーション"))
        assertEquals(SpotKind.Food, kind("札幌ラーメン横丁"))
        assertEquals(SpotKind.Landmark, kind("伏見稲荷大社"))
        assertEquals(SpotKind.Landmark, kind("時計台"))
        // Nothing in the name, so the description decides.
        assertEquals(SpotKind.Lights, kind("函館山", "100万ドルの夜景を眺める。"))
        assertEquals(SpotKind.Other, kind("ラベンダー畑", "富良野の花畑。"))
    }

    @Test
    fun periodTextCountsNightsAndDays() {
        assertEquals("12/5（土）〜 12/7（月）・2泊3日", tripPeriodText(trip("2026-12-05", "2026-12-06", "2026-12-07")))
        assertEquals("12/5（土）・日帰り", tripPeriodText(trip("2026-12-05")))
        assertEquals("12/05 〜 12/06", tripPeriodText(trip("12/05", "12/06")))
    }

    @Test
    fun dayLabelAddsTheWeekday() {
        assertEquals("12/20 日", dayLabel("2026-12-20"))
        assertEquals("いつか", dayLabel("いつか"))
    }

    @Test
    fun daysUntilCountsToTheFirstDayAndStopsAtZero() {
        val trip = trip("2026-12-05", "2026-12-07")

        assertEquals(24, daysUntil(trip, LocalDate.of(2026, 11, 11)))
        assertEquals(0, daysUntil(trip, LocalDate.of(2026, 12, 6)))
        assertNull(daysUntil(trip("未定"), LocalDate.of(2026, 11, 11)))
    }

    @Test
    fun recordDatesShortenTheSharedParts() {
        assertEquals("2025/8/10–11", recordDateText(trip("2025-08-10", "2025-08-11")))
        assertEquals("2025/7/31–8/1", recordDateText(trip("2025-07-31", "2025-08-01")))
        assertEquals("2024/12/31–2025/1/1", recordDateText(trip("2024-12-31", "2025-01-01")))
        assertEquals("2025/8/10", recordDateText(trip("2025-08-10")))
    }

    @Test
    fun spotsTextJoinsEveryDay() {
        assertEquals("時計台・大通公園・ラベンダー畑", spotsText(TripSeed.pastTrip))
    }

    @Test
    fun airportCodesForOperatedCities() {
        assertEquals("HND", TravelRules.airportCodeOf("東京"))
        assertEquals("CTS", TravelRules.airportCodeOf("札幌"))
        assertNull(TravelRules.airportCodeOf("パリ"))
    }

    @Test
    fun tripsSplitIntoUpcomingAndPastByTheirLastDay() {
        val ongoing = SavedItinerary(trip("2026-11-10", "2026-11-12", title = "旅行中"), addedByAi = false)
        val later = SavedItinerary(trip("2026-12-20", title = "あと"), addedByAi = true)
        val sooner = SavedItinerary(trip("2026-12-05", title = "さき"), addedByAi = true)
        val older = SavedItinerary(trip("2024-10-12", title = "古い"), addedByAi = false)
        val newer = SavedItinerary(trip("2025-08-10", title = "新しい"), addedByAi = false)
        val undated = SavedItinerary(trip("未定", title = "未定"), addedByAi = true)

        val state = tripUiState(
            itineraries = listOf(older, later, ongoing, newer, undated, sooner),
            flightBookings = emptyList(),
            today = LocalDate.of(2026, 11, 11),
        )

        assertEquals(listOf("旅行中", "さき", "あと", "未定"), state.upcoming.map { it.itinerary.title })
        assertEquals(listOf("新しい", "古い"), state.past.map { it.itinerary.title })
    }
}
