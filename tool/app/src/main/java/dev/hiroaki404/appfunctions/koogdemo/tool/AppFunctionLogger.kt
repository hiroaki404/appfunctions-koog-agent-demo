package dev.hiroaki404.appfunctions.koogdemo.tool

import android.util.Log

/** Records AppFunction execution without coupling calculator behavior to Android's logging API. */
interface AppFunctionLogger {
    fun started(functionName: String, arguments: String)
    fun succeeded(functionName: String, arguments: String, result: Any)
    fun failed(functionName: String, arguments: String, error: Exception)
}

object AndroidAppFunctionLogger : AppFunctionLogger {
    private const val TAG = "AppFunctionToolApp"

    override fun started(functionName: String, arguments: String) {
        Log.i(TAG, "Calling AppFunction: $functionName args={$arguments}")
    }

    override fun succeeded(functionName: String, arguments: String, result: Any) {
        Log.i(TAG, "AppFunction succeeded: $functionName args={$arguments} returnValue=$result")
    }

    override fun failed(functionName: String, arguments: String, error: Exception) {
        Log.e(TAG, "AppFunction failed: $functionName args={$arguments}", error)
    }
}
