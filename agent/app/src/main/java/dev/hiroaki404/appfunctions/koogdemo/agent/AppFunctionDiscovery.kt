package dev.hiroaki404.appfunctions.koogdemo.agent

import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ObserveAppFunctionsEvent
import androidx.appfunctions.metadata.AppFunctionMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Emits every AppFunction visible to this app, re-querying whenever any app's metadata changes.
 * No package is fixed: the agent does not know the tool app at build time.
 */
fun AppFunctionManager.discoverAppFunctions(): Flow<List<AppFunctionMetadata>> =
    observeAppFunctions()
        .filterIsInstance<ObserveAppFunctionsEvent.MetadataChanged>()
        .map { }
        .onStart { emit(Unit) }
        .map { searchAppFunctions(AppFunctionSearchSpec()) }
