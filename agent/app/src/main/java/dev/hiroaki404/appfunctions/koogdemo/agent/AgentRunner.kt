package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.serialization.kotlinx.toKotlinxJsonObject
import android.util.Log
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.metadata.AppFunctionMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Koog 1.2.0 has no definition for Gemini 3.5 Flash-Lite, so reuse 3.5 Flash's capabilities with its id. */
private val Gemini3_5FlashLite = GoogleModels.Gemini3_5Flash.copy(id = "gemini-3.5-flash-lite")

/**
 * Where a Koog tool came from, for display: the AppFunction's own name, the display name its provider
 * app gives it (if any), and the provider app's label and package.
 */
private data class ToolSource(
    val functionName: String,
    val displayName: String?,
    val appLabel: String?,
    val packageName: String,
)

/**
 * Runs one turn of a Koog agent with tools discovered at runtime. A new [AIAgent] is created for every
 * call, per the demo's "one agent per message" approach; conversation
 * continuity comes from [chatHistoryProvider] and [sessionId], which the caller keeps across calls.
 * [appFunctions] is the latest list from [discoverAppFunctions], so a function installed since the
 * previous call is already a tool here.
 * [appLabelOf] turns a provider app's package name into its user-visible name for tool-call events.
 * [functionDisplayNameOf] reads the user-visible name the provider app gives a function; it runs off
 * the main thread, and the card falls back to the function name when it returns null.
 * [appInstructionsOf] reads the app description of a function's provider app; it runs off the main
 * thread, once per app, and the results are added to the system prompt.
 * [requestApproval] asks the user to allow a function that takes a user approval (see
 * [isUserApproval]); it is given the function's name and suspends the tool call, and so the agent,
 * until the user answers. The default declines.
 * [onA2ui] is given the A2UI messages (a JSON array) a function returned in its `a2ui` field, with the
 * function's name, before that call's completed event; they are not passed on to the LLM.
 */
suspend fun runAgent(
    apiKey: String,
    appFunctionManager: AppFunctionManager,
    appFunctions: List<AppFunctionMetadata>,
    prompt: String,
    chatHistoryProvider: ChatHistoryProvider,
    sessionId: String,
    appLabelOf: (packageName: String) -> String? = { null },
    functionDisplayNameOf: (AppFunctionMetadata) -> String? = { null },
    appInstructionsOf: (AppFunctionMetadata) -> String? = { null },
    requestApproval: suspend (functionName: String) -> Boolean = { false },
    onA2ui: (functionName: String, a2uiJson: String) -> Unit = { _, _ -> },
    onToolCallEvent: suspend (ToolCallUiEvent) -> Unit = {},
): String {
    val discovered = appFunctions.mapNotNull { metadata ->
        runCatching {
            val descriptor = metadata.toToolDescriptor()
            metadata to AppFunctionTool(appFunctionManager, metadata, descriptor, requestApproval, onA2ui)
        }.onFailure { error ->
            Log.w("KoogAgent", "Skipping unsupported AppFunction ${metadata.id}", error)
        }.getOrNull()
    }
    val toolSources = withContext(Dispatchers.IO) {
        discovered.associate { (metadata, tool) ->
            tool.descriptor.name to ToolSource(
                functionName = metadata.functionName,
                displayName = functionDisplayNameOf(metadata),
                appLabel = appLabelOf(metadata.packageName),
                packageName = metadata.packageName,
            )
        }
    }
    val toolRegistry = ToolRegistry {
        discovered.forEach { (_, appFunctionTool) -> tool(appFunctionTool) }
    }
    fun functionNameOf(toolName: String) = toolSources[toolName]?.functionName ?: toolName
    fun displayNameOf(toolName: String) = toolSources[toolName]?.displayName
    fun appLabelOfTool(toolName: String) = toolSources[toolName]?.appLabel
    fun packageNameOfTool(toolName: String) = toolSources[toolName]?.packageName
    val appInstructions = withContext(Dispatchers.IO) {
        discovered.map { (metadata, _) -> metadata }.distinctBy { it.packageName }.mapNotNull { metadata ->
            appInstructionsOf(metadata)?.takeIf { it.isNotBlank() }?.let { instructions ->
                AppInstructions(appLabelOf(metadata.packageName) ?: metadata.packageName, instructions)
            }
        }
    }
    Log.d("KoogAgent", "App instructions: $appInstructions")
    // koog-agents 1.2.0 has no stable simpleGoogleAIExecutor convenience function (it only ships
    // in prompt-executor-llms-all, which itself has no 1.2.0 release yet — see libs.versions.toml).
    // This mirrors what that helper does: MultiLLMPromptExecutor(LLMProvider.Google to GoogleLLMClient(apiKey)).
    return MultiLLMPromptExecutor(LLMProvider.Google to GoogleLLMClient(apiKey)).use { executor ->
        AIAgent(
            promptExecutor = executor,
            llmModel = Gemini3_5FlashLite,
            toolRegistry = toolRegistry,
            systemPrompt = buildSystemPrompt(appInstructions = appInstructions),
        ) {
            install(ChatMemory) { this.chatHistoryProvider = chatHistoryProvider }
            handleEvents {
                onToolCallStarting { event ->
                    onToolCallEvent(
                        ToolCallUiEvent.Started(
                            toolCallId = event.toolCallId,
                            functionName = functionNameOf(event.toolName),
                            arguments = event.toolArgs.prettyPrinted(),
                            displayName = displayNameOf(event.toolName),
                            appLabel = appLabelOfTool(event.toolName),
                            packageName = packageNameOfTool(event.toolName),
                        ),
                    )
                }
                onToolCallCompleted { event ->
                    onToolCallEvent(
                        ToolCallUiEvent.Completed(
                            toolCallId = event.toolCallId,
                            functionName = functionNameOf(event.toolName),
                            arguments = event.toolArgs.prettyPrinted(),
                            result = event.toolResult?.toString() ?: "null",
                            displayName = displayNameOf(event.toolName),
                            appLabel = appLabelOfTool(event.toolName),
                            packageName = packageNameOfTool(event.toolName),
                        ),
                    )
                }
                onToolCallFailed { event ->
                    onToolCallEvent(
                        ToolCallUiEvent.Failed(
                            toolCallId = event.toolCallId,
                            functionName = functionNameOf(event.toolName),
                            arguments = event.toolArgs.prettyPrinted(),
                            error = event.error?.message ?: event.message,
                            displayName = displayNameOf(event.toolName),
                            appLabel = appLabelOfTool(event.toolName),
                            packageName = packageNameOfTool(event.toolName),
                        ),
                    )
                }
            }
        }.run(prompt, sessionId)
    }
}

private fun ai.koog.serialization.JSONObject.prettyPrinted(): String = Json { prettyPrint = true }
    .encodeToString(JsonObject.serializer(), toKotlinxJsonObject())
