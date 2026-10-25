package dev.hiroaki404.appfunctions.koogdemo.kototabi.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface KototabiDao {
    @Query("SELECT * FROM ${ItineraryEntity.TABLE_NAME} ORDER BY id")
    fun observeItineraries(): Flow<List<ItineraryEntity>>

    @Query("SELECT * FROM ${ItineraryEntity.TABLE_NAME} ORDER BY id")
    suspend fun getItineraries(): List<ItineraryEntity>

    @Insert
    suspend fun insertItinerary(entity: ItineraryEntity): Long

    @Query("DELETE FROM ${ItineraryEntity.TABLE_NAME}")
    suspend fun deleteAllItineraries()

    @Query("SELECT * FROM ${FlightBookingEntity.TABLE_NAME} ORDER BY sequence")
    fun observeFlightBookings(): Flow<List<FlightBookingEntity>>

    @Query("SELECT COALESCE(MAX(sequence), 0) FROM ${FlightBookingEntity.TABLE_NAME}")
    suspend fun maxFlightSequence(): Int

    @Insert
    suspend fun insertFlightBooking(entity: FlightBookingEntity)

    @Query("DELETE FROM ${FlightBookingEntity.TABLE_NAME}")
    suspend fun deleteAllFlightBookings()
}

@Database(
    entities = [ItineraryEntity::class, FlightBookingEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class KototabiDatabase : RoomDatabase() {
    abstract fun dao(): KototabiDao

    companion object {
        private const val DATABASE_NAME = "kototabi.db"

        fun create(context: Context): KototabiDatabase =
            Room.databaseBuilder(context, KototabiDatabase::class.java, DATABASE_NAME)
                .addCallback(SeedCallback)
                // Demo data only: a schema change simply recreates the database (and re-seeds it).
                .fallbackToDestructiveMigration(true)
                .build()

        /** An empty, non-persistent database (no seed data) for tests. */
        fun createInMemory(context: Context): KototabiDatabase =
            Room.inMemoryDatabaseBuilder(context, KototabiDatabase::class.java).build()
    }

    /** Inserts the past trip when the database file is first created or recreated by a schema change. */
    private object SeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) = seed(db)

        // A destructive migration recreates the tables without calling onCreate.
        override fun onDestructiveMigration(db: SupportSQLiteDatabase) = seed(db)

        private fun seed(db: SupportSQLiteDatabase) {
            val seed = KototabiSeed.pastTrip.toEntity()
            val values = ContentValues().apply {
                put("title", seed.title)
                put("daysJson", seed.daysJson)
                put("addedByAi", 0)
            }
            db.insert(ItineraryEntity.TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, values)
        }
    }
}
