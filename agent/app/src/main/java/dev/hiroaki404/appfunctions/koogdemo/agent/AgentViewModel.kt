package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val AGENT_TAG = "KoogAgent"
private const val DISCOVERY_TAG = "AppFunctionDiscovery"

/** A function call waiting for the user to allow or decline it; [answer] completes with the user's choice. */
private class PendingApproval(val turnId: Int, val functionName: String) {
    val answer = CompletableDeferred<Boolean>()
}

/**
 * Holds the conversation: a chat-history provider shared by every per-message agent, the current
 * session id and the turns shown on screen. Also keeps watching the device's AppFunctions, so every
 * message is answered with the tools available at that moment.
 */
class AgentViewModel(application: Application) : AndroidViewModel(application) {
    val appFunctionManager: AppFunctionManager? = AppFunctionManager.getInstance(application)

    private val chatHistoryProvider = InMemoryChatHistoryProvider()
    private var sessionId = UUID.randomUUID().toString()
    private var nextTurnId = 0

    // Guarded by itself: the agent asks from its own coroutine while the UI answers from the main thread.
    private val pendingApprovals = mutableListOf<PendingApproval>()

    private val _turns = MutableStateFlow<List<ChatTurn>>(emptyList())
    val turns: StateFlow<List<ChatTurn>> = _turns.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _availableTools = MutableStateFlow(AvailableToolsState())
    val availableTools: StateFlow<AvailableToolsState> = _availableTools.asStateFlow()

    /**
     * The functions that can become Koog tools, re-emitted whenever any app's AppFunctions change.
     * Null until the first search finishes.
     */
    private val appFunctions: StateFlow<List<AppFunctionMetadata>?> =
        (appFunctionManager?.discoverAppFunctions() ?: emptyFlow())
            .map { all ->
                val functions = all.filter(::isConvertibleToTool)
                // Display names come from the provider apps' resources, so read them off the main thread.
                functions to functions.map { it.toAvailableFunction(application.resolveFunctionDisplayName(it)) }
            }
            .flowOn(Dispatchers.IO)
            .onEach { (functions, available) ->
                Log.d(DISCOVERY_TAG, "Found ${functions.size} app functions")
                _availableTools.update { state -> state.update(available) }
            }
            .map { (functions, _) -> functions }
            .catch { e -> Log.e(DISCOVERY_TAG, "Watching AppFunctions failed", e) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
                    appFunctions = appFunctions.filterNotNull().first(),
                    prompt = prompt,
                    chatHistoryProvider = chatHistoryProvider,
                    sessionId = currentSessionId,
                    appLabelOf = ::appLabelOf,
                    functionDisplayNameOf = { metadata -> getApplication<Application>().resolveFunctionDisplayName(metadata) },
                    appInstructionsOf = { metadata ->
                        metadata.packageMetadata.resolveAppFunctionAppMetadata(getApplication())?.instructions
                    },
                    requestApproval = { functionName -> awaitApproval(turnId, functionName) },
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

    /**
     * Shows the approval buttons on the card of [functionName] in turn [turnId] and suspends the agent
     * until the user answers. The card is the latest running one of that function, as for tool events.
     * Returns false if the user declines or starts a new conversation.
     */
    private suspend fun awaitApproval(turnId: Int, functionName: String): Boolean {
        val pending = PendingApproval(turnId, functionName)
        synchronized(pendingApprovals) { pendingApprovals += pending }
        _turns.update { turns -> turns.updateTurn(turnId) { it.withAwaitingApproval(functionName, true) } }
        try {
            return pending.answer.await()
        } finally {
            synchronized(pendingApprovals) { pendingApprovals -= pending }
            _turns.update { turns -> turns.updateTurn(turnId) { it.withAwaitingApproval(functionName, false) } }
        }
    }

    /** The user's choice on the approval buttons of the card of [functionName] in turn [turnId]. */
    fun respondToApproval(turnId: Int, functionName: String, approved: Boolean) {
        val pending = synchronized(pendingApprovals) {
            pendingApprovals.lastOrNull { it.turnId == turnId && it.functionName == functionName }
        }
        pending?.answer?.complete(approved)
    }

    /** The provider app's name as shown on the launcher; the agent has no build-time knowledge of these apps. */
    private fun appLabelOf(packageName: String): String? = runCatching {
        val packageManager = getApplication<Application>().packageManager
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

    /** The provider app for the tool list; preinstalled apps count as system apps even after an update. */
    fun providerAppOf(packageName: String): ProviderApp {
        val flags = runCatching {
            getApplication<Application>().packageManager.getApplicationInfo(packageName, 0).flags
        }.getOrDefault(0)
        return ProviderApp(
            label = appLabelOf(packageName) ?: packageName,
            isSystem = flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
        )
    }

    /** The user has seen the tool list, so the count stops showing what was added. */
    fun acknowledgeNewTools() {
        _availableTools.update { it.acknowledged() }
    }

    /** Starts over: a new session id means the agent no longer sees earlier turns. */
    fun newConversation() {
        // A call still waiting for approval can no longer be answered, so decline it.
        synchronized(pendingApprovals) { pendingApprovals.toList() }.forEach { it.answer.complete(false) }
        sessionId = UUID.randomUUID().toString()
        _turns.value = emptyList()
    }

    /** Debug aid: shows [SampleTurns] in place of the conversation, without calling the LLM. */
    fun loadSampleTurns() {
        if (_isRunning.value) return
        sessionId = UUID.randomUUID().toString()
        _turns.value = SampleTurns
    }
}

/** Matches what [runAgent] can register: a function whose types it can't convert is skipped there too. */
private fun isConvertibleToTool(metadata: AppFunctionMetadata): Boolean =
    runCatching { metadata.toToolDescriptor() }
        .onFailure { Log.w(DISCOVERY_TAG, "Skipping unsupported AppFunction ${metadata.id}", it) }
        .isSuccess

private fun AppFunctionMetadata.toAvailableFunction(displayName: String?) =
    AvailableFunction(id, packageName, description, displayName)
