package dev.hiroaki404.appfunctions.koogdemo.agent

/** A tool lifecycle event that can be rendered without depending on Koog or Compose. */
sealed interface ToolCallUiEvent {
    val toolCallId: String?
    val functionName: String
    val arguments: String

    data class Started(
        override val toolCallId: String?,
        override val functionName: String,
        override val arguments: String,
    ) : ToolCallUiEvent

    data class Completed(
        override val toolCallId: String?,
        override val functionName: String,
        override val arguments: String,
        val result: String,
    ) : ToolCallUiEvent

    data class Failed(
        override val toolCallId: String?,
        override val functionName: String,
        override val arguments: String,
        val error: String,
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
            ),
        )

        is ToolCallUiEvent.Completed -> updateOrAppend(event) { current ->
            current.copy(status = ToolCallStatus.Succeeded, result = event.result, error = null)
        }

        is ToolCallUiEvent.Failed -> updateOrAppend(event) { current ->
            current.copy(status = ToolCallStatus.Failed, result = null, error = event.error)
        }
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
                ),
            )
        }
        return copy(calls = calls.mapIndexed { currentIndex, call ->
            if (currentIndex == index) update(call) else call
        })
    }
}
