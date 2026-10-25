package dev.hiroaki404.appfunctions.koogdemo.kototabi

import android.app.Application
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.KototabiDatabase
import dev.hiroaki404.appfunctions.koogdemo.kototabi.data.KototabiRepository

/**
 * Owns the single database and repository so the AppFunction service and the activity share the
 * same instance (and therefore the same Room Flow updates) within the process.
 */
class KototabiApplication : Application() {
    val repository: KototabiRepository by lazy { KototabiRepository(KototabiDatabase.create(this)) }
}
