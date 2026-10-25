package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import android.app.Application
import android.util.Log
import androidx.appfunctions.AppFunctionManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val AGENT_TAG = "KoogAgent"

/**
 * Holds the conversation: a chat-history provider shared by every per-message agent, the current
 * session id and the turns shown on screen.
 */
class AgentViewModel(application: Application) : AndroidViewModel(application) {
    val appFunctionManager: AppFunctionManager? = AppFunctionManager.getInstance(application)

    private val chatHistoryProvider = InMemoryChatHistoryProvider()
    private var sessionId = UUID.randomUUID().toString()
    private var nextTurnId = 0

    private val _turns = MutableStateFlow<List<ChatTurn>>(emptyList())
    val turns: StateFlow<List<ChatTurn>> = _turns.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun send(prompt: String) {
        val manager = appFunctionManager ?: return
        if (prompt.isBlank() || _isRunning.value) return
        val turnId = nextTurnId++
        val currentSessionId = sessionId
        _isRunning.value = true
        _turns.update { it + ChatTurn(id = turnId, prompt = prompt) }
        viewModelScope.launch {
            try {
                val response = runAgent(
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    appFunctionManager = manager,
                    prompt = prompt,
                    chatHistoryProvider = chatHistoryProvider,
                    sessionId = currentSessionId,
                    onToolCallEvent = { event ->
                        _turns.update { turns -> turns.updateTurn(turnId) { it.applyToolEvent(event) } }
                    },
                )
                Log.d(AGENT_TAG, "response: $response")
                _turns.update { turns -> turns.updateTurn(turnId) { it.completed(response) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AGENT_TAG, "Agent run failed", e)
                _turns.update { turns -> turns.updateTurn(turnId) { it.failed(e.message ?: e.toString()) } }
            } finally {
                _isRunning.value = false
            }
        }
    }

    /** Starts over: a new session id means the agent no longer sees earlier turns. */
    fun newConversation() {
        sessionId = UUID.randomUUID().toString()
        _turns.value = emptyList()
    }
}
