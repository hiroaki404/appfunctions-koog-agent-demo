package dev.hiroaki404.appfunctions.koogdemo.agent

/** A tool lifecycle event that can be rendered without depending on Koog or Compose. */
sealed interface ToolCallUiEvent {
    val toolCallId: String?
    val functionName: String
    val arguments: String

    /** The user-visible name the provider app gives the function, if it defines one. */
    val displayName: String?

    /** The user-visible name of the app providing the function, if known. */
    val appLabel: String?

    /** The provider app's package, so the UI can show its icon; kept as a plain string here. */
    val packageName: String?

    data class Started(
        override val toolCallId: String?,
        override val functionName: String,
        override val arguments: String,
        override val displayName: String? = null,
        override val appLabel: String? = null,
        override val packageName: String? = null,
    ) : ToolCallUiEvent

    data class Completed(
        override val toolCallId: String?,
        override val functionName: String,
        override val arguments: String,
        val result: String,
        override val displayName: String? = null,
        override val appLabel: String? = null,
        override val packageName: String? = null,
    ) : ToolCallUiEvent

    data class Failed(
        override val toolCallId: String?,
        override val functionName: String,
        override val arguments: String,
        val error: String,
        override val displayName: String? = null,
        override val appLabel: String? = null,
        override val packageName: String? = null,
    ) : ToolCallUiEvent
}

enum class ToolCallStatus(val label: String) {
    Running("実行中"),
    Succeeded("成功"),
    Failed("失敗"),
}

data class ToolCallUiModel(
    val toolCallId: String?,
    val functionName: String,
    val arguments: String,
    val status: ToolCallStatus,
    val result: String? = null,
    val error: String? = null,
    val appLabel: String? = null,
    val packageName: String? = null,
    val displayName: String? = null,
    /** True while the call waits for the user to allow or decline it; [status] stays [ToolCallStatus.Running]. */
    val awaitingApproval: Boolean = false,
    /** The id of the A2UI surface the call returned, drawn under the card; null if it returned none. */
    val a2uiSurfaceId: String? = null,
)

/** Immutable, chronological state for tool-call cards. */
data class ToolCallUiState(val calls: List<ToolCallUiModel> = emptyList()) {
    fun apply(event: ToolCallUiEvent): ToolCallUiState = when (event) {
        is ToolCallUiEvent.Started -> copy(
            calls = calls + ToolCallUiModel(
                toolCallId = event.toolCallId,
                functionName = event.functionName,
                arguments = event.arguments,
                status = ToolCallStatus.Running,
                appLabel = event.appLabel,
                packageName = event.packageName,
                displayName = event.displayName,
            ),
        )

        is ToolCallUiEvent.Completed -> updateOrAppend(event) { current ->
            current.copy(status = ToolCallStatus.Succeeded, result = event.result, error = null, awaitingApproval = false)
        }

        is ToolCallUiEvent.Failed -> updateOrAppend(event) { current ->
            current.copy(status = ToolCallStatus.Failed, result = null, error = event.error, awaitingApproval = false)
        }
    }

    /**
     * Marks the latest running call of [functionName] as waiting for the user's approval, or clears the
     * mark again. Calls are matched by function name because Koog doesn't pass the tool call id to the
     * tool; this is the same rule as for events without an id. A no-op if there is no such call.
     */
    fun withAwaitingApproval(functionName: String, awaiting: Boolean): ToolCallUiState {
        val index = calls.indexOfLast { call ->
            call.status == ToolCallStatus.Running && call.functionName == functionName && call.awaitingApproval != awaiting
        }
        if (index < 0) return this
        return copy(calls = calls.mapIndexed { currentIndex, call ->
            if (currentIndex == index) call.copy(awaitingApproval = awaiting) else call
        })
    }

    /**
     * Attaches the A2UI surface [surfaceId] to the latest running call of [functionName], which is the
     * call that returned it: a function's result is handled before its completed event. Matched by
     * function name, as for [withAwaitingApproval]. A no-op if there is no such call.
     */
    fun withA2uiSurface(functionName: String, surfaceId: String): ToolCallUiState {
        val index = calls.indexOfLast { call ->
            call.status == ToolCallStatus.Running && call.functionName == functionName
        }
        if (index < 0) return this
        return copy(calls = calls.mapIndexed { currentIndex, call ->
            if (currentIndex == index) call.copy(a2uiSurfaceId = surfaceId) else call
        })
    }

    private fun updateOrAppend(
        event: ToolCallUiEvent,
        update: (ToolCallUiModel) -> ToolCallUiModel,
    ): ToolCallUiState {
        val index = calls.indexOfLast { call ->
            call.status == ToolCallStatus.Running &&
                if (event.toolCallId != null) {
                    call.toolCallId == event.toolCallId
                } else {
                    // Koog may not provide an ID. In that case, finish the most recently started
                    // invocation of this function so repeated calls remain distinguishable.
                    call.functionName == event.functionName
                }
        }
        if (index < 0) {
            val status = if (event is ToolCallUiEvent.Completed) {
                ToolCallStatus.Succeeded
            } else {
                ToolCallStatus.Failed
            }
            val result = (event as? ToolCallUiEvent.Completed)?.result
            val error = (event as? ToolCallUiEvent.Failed)?.error
            return copy(
                calls = calls + ToolCallUiModel(
                    toolCallId = event.toolCallId,
                    functionName = event.functionName,
                    arguments = event.arguments,
                    status = status,
                    result = result,
                    error = error,
                    appLabel = event.appLabel,
                    packageName = event.packageName,
                    displayName = event.displayName,
                ),
            )
        }
        return copy(calls = calls.mapIndexed { currentIndex, call ->
            if (currentIndex == index) update(call) else call
        })
    }
}
