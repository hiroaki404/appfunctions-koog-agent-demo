package dev.hiroaki404.appfunctions.koogdemo.agent

/** One AppFunction the agent hands to the LLM, reduced to what the tool list shows. */
data class AvailableFunction(
    val id: String,
    val packageName: String,
    val description: String,
    /** The provider app's user-visible name for the function, if it defines one. */
    val displayName: String? = null,
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

/** Apps that started providing functions while the agent was running; [id] differs for every find. */
data class AppDiscovery(val id: Int, val packageNames: Set<String>, val functions: List<AvailableFunction>) {
    /** Drops the apps matching [removed]; null when none is left. */
    fun without(removed: (packageName: String) -> Boolean): AppDiscovery? {
        val kept = packageNames.filterNot(removed).toSet()
        if (kept.isEmpty()) return null
        return copy(packageNames = kept, functions = functions.filter { it.packageName in kept })
    }
}

/**
 * The notice for found apps, e.g. 「メッセージアプリを見つけました」. A label that already ends in
 * 「アプリ」 is used as is.
 */
fun discoveredAppsText(labels: List<String>): String = when (labels.size) {
    1 -> {
        val label = labels.single()
        "${if (label.endsWith("アプリ")) label else "${label}アプリ"}を見つけました"
    }
    else -> "${labels.size}個のアプリを見つけました"
}

/** The found functions by their display names, e.g. 「メッセージの送信」. */
fun discoveredFunctionsText(functions: List<AvailableFunction>): String =
    functions.joinToString("・") { it.displayName ?: it.name }

/**
 * The tools the agent currently offers the LLM, and which of them appeared since the user last
 * looked at the tool list. [functions] stays null until the first search finishes.
 */
data class AvailableToolsState(
    val functions: List<AvailableFunction>? = null,
    val newFunctionIds: Set<String> = emptySet(),
    /** The latest apps found, until the user looks at the tool list or they are uninstalled. */
    val discovery: AppDiscovery? = null,
    /** How many finds there have been, so a new [AppDiscovery] never reuses an id. */
    private val discoveryCount: Int = 0,
) {
    /**
     * Takes the latest search result. The first result is the baseline, so nothing in it counts as
     * new; functions that disappear (an app was uninstalled) stop counting as new. An app that was
     * not there before becomes a new [discovery]; more functions from an app already there do not.
     */
    fun update(latest: List<AvailableFunction>): AvailableToolsState {
        val previous = functions ?: return copy(functions = latest)
        val latestIds = latest.mapTo(mutableSetOf()) { it.id }
        val added = latestIds - previous.mapTo(mutableSetOf()) { it.id }
        val latestPackages = latest.mapTo(mutableSetOf()) { it.packageName }
        val addedPackages = latestPackages - previous.mapTo(mutableSetOf()) { it.packageName }
        return if (addedPackages.isNotEmpty()) {
            copy(
                functions = latest,
                newFunctionIds = (newFunctionIds + added) intersect latestIds,
                discovery = AppDiscovery(
                    id = discoveryCount + 1,
                    packageNames = addedPackages,
                    functions = latest.filter { it.packageName in addedPackages },
                ),
                discoveryCount = discoveryCount + 1,
            )
        } else {
            copy(
                functions = latest,
                newFunctionIds = (newFunctionIds + added) intersect latestIds,
                discovery = discovery?.without { it !in latestPackages },
            )
        }
    }

    /** The user has seen the tool list, so nothing is new any more. */
    fun acknowledged(): AvailableToolsState = copy(newFunctionIds = emptySet(), discovery = null)

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
