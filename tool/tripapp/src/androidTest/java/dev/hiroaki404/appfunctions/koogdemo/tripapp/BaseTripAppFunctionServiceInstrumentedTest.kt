package dev.hiroaki404.appfunctions.koogdemo.tripapp

import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripDatabase
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripRepository
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripSeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

private val DEC_20: LocalDate = LocalDate.of(2026, 12, 20)
private val APPROVED = UserApproval(approved = true)

@RunWith(AndroidJUnit4::class)
class BaseTripAppFunctionServiceInstrumentedTest {
    private lateinit var database: TripDatabase
    private lateinit var repository: TripRepository
    private lateinit var service: BaseTripAppFunctionService

    @Before
    fun setUp() {
        database = TripDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        repository = TripRepository(database)
        service = object : BaseTripAppFunctionService() {
            override val appFunctionLogger = object : AppFunctionLogger {
                override fun started(functionName: String, arguments: String) = Unit
                override fun succeeded(functionName: String, arguments: String, result: Any) = Unit
                override fun failed(functionName: String, arguments: String, error: Exception) = Unit
            }
            override val repository: TripRepository = this@BaseTripAppFunctionServiceInstrumentedTest.repository

            override fun onExecuteFunction(
                request: androidx.appfunctions.ExecuteAppFunctionRequest,
                metadata: androidx.appfunctions.metadata.AppFunctionMetadata,
                cancellationSignal: android.os.CancellationSignal,
                callback: java.util.function.Consumer<androidx.appfunctions.ExecuteAppFunctionResponse>,
            ) = error("This instrumentation test invokes the service methods directly.")
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveThenSearchFindsItineraryByTitleAndSpotName() = runBlocking {
        service.saveItinerary(TripSeed.pastTrip)
        service.saveItinerary(TripSeed.upcomingTrip)

        assertEquals(listOf(TripSeed.pastTrip), service.searchItineraries("ラベンダー"))
        assertEquals(listOf(TripSeed.upcomingTrip), service.searchItineraries("2026年12月"))
        assertEquals(2, service.searchItineraries(null).size)
        assertEquals(2, service.searchItineraries().size)
        assertEquals(emptyList<Itinerary>(), service.searchItineraries("パリ"))
        assertEquals(2, repository.itineraries.first().size)
    }

    @Test
    fun saveItineraryReturnsDistinctIdsAndRejectsInvalidDate() = runBlocking<Unit> {
        val first = service.saveItinerary(TripSeed.pastTrip)
        val second = service.saveItinerary(TripSeed.upcomingTrip)
        assertEquals(false, first == second)

        val invalid = Itinerary("旅", listOf(ItineraryDay("12/20", emptyList())))
        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            runBlocking { service.saveItinerary(invalid) }
        }
    }

    @Test
    fun bookFlightAssignsSequentialBookingIdsAndStoresThem() = runBlocking {
        val first = service.bookFlight("東京", "札幌", DEC_20, APPROVED)
        val second = service.bookFlight("札幌", "東京", LocalDate.of(2026, 12, 22), APPROVED)

        assertEquals(FlightBooking("TRP-0001", "TR501", "東京", "札幌", "2026-12-20"), first)
        assertEquals("TRP-0002", second.bookingId)
        assertEquals(listOf(first, second), repository.flightBookings.first())
    }

    @Test
    fun appFunctionWritesAreMarkedAsAddedByAi() = runBlocking {
        service.saveItinerary(TripSeed.upcomingTrip)
        service.bookFlight("東京", "札幌", DEC_20, APPROVED)
        repository.saveItinerary(TripSeed.pastTrip)
        repository.seedUpcomingTrip()

        assertEquals(listOf(true, false, false), repository.savedItineraries.first().map { it.addedByAi })
        assertEquals(listOf(true), repository.savedFlightBookings.first().map { it.addedByAi })
    }

    @Test
    fun bookFlightRejectsInvalidInput() {
        // The date is a LocalDate now, so a malformed date can no longer reach the function.
        assertInvalidArgument { service.bookFlight("羽田空港", "札幌", DEC_20, APPROVED) }
        assertInvalidArgument { service.bookFlight("Tokyo", "札幌", DEC_20, APPROVED) }
        assertInvalidArgument { service.bookFlight("東京", "東京", DEC_20, APPROVED) }
    }

    @Test
    fun bookFlightRejectsMissingApproval() = runBlocking {
        assertInvalidArgument { service.bookFlight("東京", "札幌", DEC_20, UserApproval(approved = false)) }

        assertEquals(emptyList<FlightBooking>(), repository.flightBookings.first())
    }

    @Test
    fun getTravelInfoRejectsUnsupportedCombination() {
        assertInvalidArgument { service.getTravelInfo("パリ", 12) }
    }

    @Test
    fun resetRestoresOnlyPastTrips() = runBlocking {
        service.saveItinerary(TripSeed.upcomingTrip)
        service.bookFlight("東京", "札幌", DEC_20, APPROVED)

        repository.resetToInitialState()

        assertEquals(TripSeed.pastTrips, repository.itineraries.first())
        assertEquals(emptyList<FlightBooking>(), repository.flightBookings.first())
    }

    private fun assertInvalidArgument(block: suspend () -> Unit) {
        assertThrows(AppFunctionInvalidArgumentException::class.java) { runBlocking { block() } }
    }
}
