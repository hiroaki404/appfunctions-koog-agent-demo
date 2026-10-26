package dev.hiroaki404.appfunctions.koogdemo.agent

/** One AppFunction the agent hands to the LLM, reduced to what the tool list shows. */
data class AvailableFunction(
    val id: String,
    val packageName: String,
    val description: String,
) {
    /** The function's own name, without the class that declares it. */
    val name: String
        get() = id.substringAfter('#')
}

/** What the tool list needs to know about the app that provides some functions. */
data class ProviderApp(val label: String, val isSystem: Boolean)

/** The functions one app provides, as one section of the tool list. */
data class AvailableToolsGroup(
    val packageName: String,
    val app: ProviderApp,
    val functions: List<AvailableFunction>,
    val hasNew: Boolean,
)

/**
 * The tools the agent currently offers the LLM, and which of them appeared since the user last
 * looked at the tool list. [functions] stays null until the first search finishes.
 */
data class AvailableToolsState(
    val functions: List<AvailableFunction>? = null,
    val newFunctionIds: Set<String> = emptySet(),
) {
    /**
     * Takes the latest search result. The first result is the baseline, so nothing in it counts as
     * new; functions that disappear (an app was uninstalled) stop counting as new.
     */
    fun update(latest: List<AvailableFunction>): AvailableToolsState {
        val previous = functions ?: return copy(functions = latest)
        val latestIds = latest.mapTo(mutableSetOf()) { it.id }
        val added = latestIds - previous.mapTo(mutableSetOf()) { it.id }
        return copy(functions = latest, newFunctionIds = (newFunctionIds + added) intersect latestIds)
    }

    /** The user has seen the tool list, so nothing is new any more. */
    fun acknowledged(): AvailableToolsState = copy(newFunctionIds = emptySet())

    /** Sections for the tool list: apps the user installed first, then system apps, each by label. */
    fun groups(appOf: (packageName: String) -> ProviderApp): List<AvailableToolsGroup> =
        functions.orEmpty()
            .groupBy { it.packageName }
            .map { (packageName, appFunctions) ->
                AvailableToolsGroup(
                    packageName = packageName,
                    app = appOf(packageName),
                    functions = appFunctions,
                    hasNew = appFunctions.any { it.id in newFunctionIds },
                )
            }
            .sortedWith(compareBy({ it.app.isSystem }, { it.app.label }))
}
