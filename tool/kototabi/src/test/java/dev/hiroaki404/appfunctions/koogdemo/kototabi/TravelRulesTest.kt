package dev.hiroaki404.appfunctions.koogdemo.kototabi

import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.KototabiSeed
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.toEntity
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.toItinerary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelRulesTest {
    @Test
    fun hokkaidoDecemberReturnsFixedSpots() {
        val names = TravelRules.spotsFor("北海道", 12)!!.map { it.name }
        assertTrue(names.containsAll(listOf("さっぽろホワイトイルミネーション", "小樽運河", "旭山動物園")))
    }

    /** Other names for the area are left to the calling LLM, which the getTravelInfo description asks to map them. */
    @Test
    fun areaMustMatchTheJapaneseNameExactly() {
        assertNotNull(TravelRules.spotsFor(" 北海道 ", 12))
        assertNull(TravelRules.spotsFor("Hokkaido", 12))
        assertNull(TravelRules.spotsFor("札幌", 12))
    }

    @Test
    fun unsupportedAreaOrMonthReturnsNull() {
        assertNull(TravelRules.spotsFor("パリ", 12))
        assertNull(TravelRules.spotsFor("北海道", 3))
        assertTrue(TravelRules.supportedTravelInfo.contains("北海道 / 12月"))
    }

    @Test
    fun dateValidationRequiresIsoFormat() {
        assertTrue(TravelRules.isValidDate("2026-12-20"))
        assertFalse(TravelRules.isValidDate("2026/12/20"))
        assertFalse(TravelRules.isValidDate("2026-1-5"))
        assertFalse(TravelRules.isValidDate("2026-02-30"))
        assertFalse(TravelRules.isValidDate("12月20日"))
        assertFalse(TravelRules.isValidDate(""))
    }

    /** English and airport names are left to the calling LLM, which the bookFlight description asks to map them. */
    @Test
    fun onlyJapaneseCityNamesAreSupported() {
        assertTrue(TravelRules.isSupportedCity("東京"))
        assertTrue(TravelRules.isSupportedCity("札幌"))
        assertFalse(TravelRules.isSupportedCity("Tokyo"))
        assertFalse(TravelRules.isSupportedCity("羽田空港"))
        assertFalse(TravelRules.isSupportedCity("CTS"))
        assertEquals("東京、札幌", TravelRules.supportedCities)
    }

    @Test
    fun flightNumbersComeFromTheCityPairTable() {
        assertEquals("KT501", TravelRules.flightNumberFor("東京", "札幌"))
        assertEquals("KT502", TravelRules.flightNumberFor("札幌", "東京"))
        assertNull(TravelRules.flightNumberFor("東京", "東京"))
    }

    @Test
    fun bookingIdIsZeroPaddedSequence() {
        assertEquals("KTB-0001", TravelRules.bookingId(1))
        assertEquals("KTB-0042", TravelRules.bookingId(42))
    }

    @Test
    fun keywordMatchesTitleOrSpotNamePartially() {
        val trip = KototabiSeed.pastTrip
        assertTrue(TravelRules.matches(trip, "北海道"))
        assertTrue(TravelRules.matches(trip, "ラベンダー"))
        assertTrue(TravelRules.matches(trip, "時計台"))
        assertFalse(TravelRules.matches(trip, "小樽"))
    }

    @Test
    fun nullOrBlankKeywordMatchesEverything() {
        val trip = KototabiSeed.pastTrip
        assertTrue(TravelRules.matches(trip, null))
        assertTrue(TravelRules.matches(trip, ""))
        assertTrue(TravelRules.matches(trip, "   "))
    }

    @Test
    fun validateRejectsBadItineraries() {
        val day = ItineraryDay("2026-12-20", listOf(Spot("小樽運河", "運河")))
        assertNull(TravelRules.validate(Itinerary("12月の北海道", listOf(day))))
        assertNotNull(TravelRules.validate(Itinerary(" ", listOf(day))))
        assertNotNull(TravelRules.validate(Itinerary("旅", emptyList())))
        assertNotNull(TravelRules.validate(Itinerary("旅", listOf(day.copy(date = "12/20")))))
        assertNotNull(TravelRules.validate(Itinerary("旅", listOf(day.copy(spots = listOf(Spot("", "")))))))
    }

    @Test
    fun itineraryRoundTripsThroughEntityJson() {
        val restored = KototabiSeed.upcomingTrip.toEntity().toItinerary()
        assertEquals(KototabiSeed.upcomingTrip, restored)
    }
}
