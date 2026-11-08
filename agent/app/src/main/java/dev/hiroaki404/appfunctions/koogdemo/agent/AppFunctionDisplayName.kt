package dev.hiroaki404.appfunctions.koogdemo.agent

import android.annotation.SuppressLint
import android.content.Context
import androidx.appfunctions.metadata.AppFunctionMetadata

/**
 * The string resource a provider app may define to give one of its functions a user-visible name,
 * e.g. `app_function_display_name_getTravelInfo`. AppFunctions 1.0.0-beta01 only has a display text
 * for the whole app (`appfn:displayDescription`), so this follows the same idea per function: a
 * localized resource owned by the provider app, separate from the KDoc the model reads.
 */
fun functionDisplayNameResourceName(functionName: String): String? =
    functionName.takeIf { it.isNotEmpty() && it.all { c -> c.isLetterOrDigit() || c == '_' } }
        ?.let { "app_function_display_name_$it" }

/** The AppFunction's method name, without the declaring class that [AppFunctionMetadata.id] starts with. */
val AppFunctionMetadata.functionName: String get() = id.substringAfter('#')

/**
 * Reads the provider app's display name for [metadata] in the current locale, or null when the app
 * does not define one. Reads another app's resources, so call it off the main thread.
 */
@SuppressLint("DiscouragedApi") // The resource name is a convention between apps; there is no R class to use.
fun Context.resolveFunctionDisplayName(metadata: AppFunctionMetadata): String? = runCatching {
    val name = functionDisplayNameResourceName(metadata.functionName) ?: return null
    val appInfo = packageManager.getApplicationInfo(metadata.packageName, 0)
    val resources = packageManager.getResourcesForApplication(appInfo, resources.configuration)
    val id = resources.getIdentifier(name, "string", metadata.packageName)
    if (id == 0) null else resources.getString(id).takeIf { it.isNotBlank() }
}.getOrNull()
