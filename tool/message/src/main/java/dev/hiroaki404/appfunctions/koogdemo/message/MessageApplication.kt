package dev.hiroaki404.appfunctions.koogdemo.message

import android.app.Application

/** Owns the process-wide message history so the AppFunction service and activity share it. */
class MessageApplication : Application() {
    val messageRepository = MessageRepository()
}
