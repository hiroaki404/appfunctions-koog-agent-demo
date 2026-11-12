package dev.hiroaki404.appfunctions.koogdemo.agent

/** One round trip of the conversation: the user's prompt, the tool calls it triggered and the final answer. */
data class ChatTurn(
    val id: Int,
    val prompt: String,
    val toolCalls: ToolCallUiState = ToolCallUiState(),
    val response: String? = null,
    val error: String? = null,
    val isRunning: Boolean = true,
) {
    /** True while the agent is waiting on the LLM, i.e. running with no tool call in flight. */
    val isThinking: Boolean
        get() = isRunning && toolCalls.calls.none { it.status == ToolCallStatus.Running }

    fun applyToolEvent(event: ToolCallUiEvent): ChatTurn = copy(toolCalls = toolCalls.apply(event))

    fun withAwaitingApproval(functionName: String, awaiting: Boolean): ChatTurn =
        copy(toolCalls = toolCalls.withAwaitingApproval(functionName, awaiting))

    fun withA2uiSurface(functionName: String, surfaceId: String): ChatTurn =
        copy(toolCalls = toolCalls.withA2uiSurface(functionName, surfaceId))

    /** The ids of the A2UI surfaces the turn's tool calls returned. */
    val a2uiSurfaceIds: List<String>
        get() = toolCalls.calls.mapNotNull { it.a2uiSurfaceId }

    fun completed(response: String): ChatTurn = copy(response = response, error = null, isRunning = false)

    fun failed(error: String): ChatTurn = copy(response = null, error = error, isRunning = false)
}

/**
 * The latest tool call of the turn still being answered, finished or not. An AppFunction call
 * returns in milliseconds, so this keeps its app in view while the agent goes on to the next step.
 */
fun List<ChatTurn>.activeToolCall(): ToolCallUiModel? =
    lastOrNull()?.takeIf { it.isRunning }?.toolCalls?.calls?.lastOrNull()

/** Applies [transform] to the turn with [id]; a no-op if the turn no longer exists (e.g. after 「新しい会話」). */
fun List<ChatTurn>.updateTurn(id: Int, transform: (ChatTurn) -> ChatTurn): List<ChatTurn> =
    map { turn -> if (turn.id == id) transform(turn) else turn }
