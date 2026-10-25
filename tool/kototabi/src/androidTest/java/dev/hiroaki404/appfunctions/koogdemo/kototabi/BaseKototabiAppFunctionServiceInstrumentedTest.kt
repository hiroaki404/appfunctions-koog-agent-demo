package dev.hiroaki404.appfunctions.koogdemo.kototabi

import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.KototabiDatabase
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.KototabiRepository
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.KototabiSeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaseKototabiAppFunctionServiceInstrumentedTest {
    private lateinit var database: KototabiDatabase
    private lateinit var repository: KototabiRepository
    private lateinit var service: BaseKototabiAppFunctionService

    @Before
    fun setUp() {
        database = KototabiDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        repository = KototabiRepository(database)
        service = object : BaseKototabiAppFunctionService() {
            override val appFunctionLogger = object : AppFunctionLogger {
                override fun started(functionName: String, arguments: String) = Unit
                override fun succeeded(functionName: String, arguments: String, result: Any) = Unit
                override fun failed(functionName: String, arguments: String, error: Exception) = Unit
            }
            override val repository: KototabiRepository = this@BaseKototabiAppFunctionServiceInstrumentedTest.repository

            override fun onExecuteFunction(
                request: androidx.appfunctions.ExecuteAppFunctionRequest,
                cancellationSignal: android.os.CancellationSignal,
                callback: java.util.function.Consumer<androidx.appfunctions.ExecuteAppFunctionResponse>,
            ) = error("This instrumentation test invokes the kototabi methods directly.")
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveThenSearchFindsItineraryByTitleAndSpotName() = runBlocking {
        service.saveItinerary(KototabiSeed.pastTrip)
        service.saveItinerary(KototabiSeed.upcomingTrip)

        assertEquals(listOf(KototabiSeed.pastTrip), service.searchItineraries("ラベンダー"))
        assertEquals(listOf(KototabiSeed.upcomingTrip), service.searchItineraries("2026年12月"))
        assertEquals(2, service.searchItineraries(null).size)
        assertEquals(2, service.searchItineraries().size)
        assertEquals(emptyList<Itinerary>(), service.searchItineraries("パリ"))
        assertEquals(2, repository.itineraries.first().size)
    }

    @Test
    fun saveItineraryReturnsDistinctIdsAndRejectsInvalidDate() = runBlocking {
        val first = service.saveItinerary(KototabiSeed.pastTrip)
        val second = service.saveItinerary(KototabiSeed.upcomingTrip)
        assertEquals(false, first == second)

        val invalid = Itinerary("旅", listOf(ItineraryDay("12/20", emptyList())))
        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            runBlocking { service.saveItinerary(invalid) }
        }
    }

    @Test
    fun bookFlightAssignsSequentialBookingIdsAndStoresThem() = runBlocking {
        val first = service.bookFlight("東京", "札幌", "2026-12-20")
        val second = service.bookFlight("札幌", "東京", "2026-12-22")

        assertEquals(FlightBooking("KTB-0001", "KT501", "東京", "札幌", "2026-12-20"), first)
        assertEquals("KTB-0002", second.bookingId)
        assertEquals(listOf(first, second), repository.flightBookings.first())
    }

    @Test
    fun appFunctionWritesAreMarkedAsAddedByAi() = runBlocking {
        service.saveItinerary(KototabiSeed.upcomingTrip)
        service.bookFlight("東京", "札幌", "2026-12-20")
        repository.saveItinerary(KototabiSeed.pastTrip)
        repository.seedUpcomingTrip()

        assertEquals(listOf(true, false, false), repository.savedItineraries.first().map { it.addedByAi })
        assertEquals(listOf(true), repository.savedFlightBookings.first().map { it.addedByAi })
    }

    @Test
    fun bookFlightRejectsInvalidInput() {
        assertInvalidArgument { service.bookFlight("東京", "札幌", "2026/12/20") }
        assertInvalidArgument { service.bookFlight("羽田空港", "札幌", "2026-12-20") }
        assertInvalidArgument { service.bookFlight("Tokyo", "札幌", "2026-12-20") }
        assertInvalidArgument { service.bookFlight("東京", "東京", "2026-12-20") }
    }

    @Test
    fun getTravelInfoRejectsUnsupportedCombination() {
        assertInvalidArgument { service.getTravelInfo("パリ", 12) }
    }

    @Test
    fun resetRestoresOnlyPastTrip() = runBlocking {
        service.saveItinerary(KototabiSeed.upcomingTrip)
        service.bookFlight("東京", "札幌", "2026-12-20")

        repository.resetToInitialState()

        assertEquals(listOf(KototabiSeed.pastTrip), repository.itineraries.first())
        assertEquals(emptyList<FlightBooking>(), repository.flightBookings.first())
    }

    private fun assertInvalidArgument(block: suspend () -> Unit) {
        assertThrows(AppFunctionInvalidArgumentException::class.java) { runBlocking { block() } }
    }
}
