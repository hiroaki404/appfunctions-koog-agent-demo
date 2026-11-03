package dev.hiroaki404.appfunctions.koogdemo.tripapp

import android.app.Application
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripDatabase
import dev.hiroaki404.appfunctions.koogdemo.tripapp.data.TripRepository

/**
 * Owns the single database and repository so the AppFunction service and the activity share the
 * same instance (and therefore the same Room Flow updates) within the process.
 */
class TripApplication : Application() {
    val repository: TripRepository by lazy { TripRepository(TripDatabase.create(this)) }
}
