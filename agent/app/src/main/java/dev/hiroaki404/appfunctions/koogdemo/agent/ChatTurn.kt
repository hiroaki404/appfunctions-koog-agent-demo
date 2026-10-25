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
    fun applyToolEvent(event: ToolCallUiEvent): ChatTurn = copy(toolCalls = toolCalls.apply(event))

    fun completed(response: String): ChatTurn = copy(response = response, error = null, isRunning = false)

    fun failed(error: String): ChatTurn = copy(response = null, error = error, isRunning = false)
}

/** Applies [transform] to the turn with [id]; a no-op if the turn no longer exists (e.g. after 「新しい会話」). */
fun List<ChatTurn>.updateTurn(id: Int, transform: (ChatTurn) -> ChatTurn): List<ChatTurn> =
    map { turn -> if (turn.id == id) transform(turn) else turn }
