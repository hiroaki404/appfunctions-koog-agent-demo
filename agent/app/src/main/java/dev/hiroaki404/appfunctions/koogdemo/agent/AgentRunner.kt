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
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Koog 1.2.0 has no definition for Gemini 3.5 Flash-Lite, so reuse 3.5 Flash's capabilities with its id. */
private val Gemini3_5FlashLite = GoogleModels.Gemini3_5Flash.copy(id = "gemini-3.5-flash-lite")

/** Where a Koog tool came from, for display: the AppFunction's own name and its provider app's label. */
private data class ToolSource(val functionName: String, val appLabel: String?)

/**
 * Runs one turn of a Koog agent with tools discovered at runtime. A new [AIAgent] is created for every
 * call, per the demo's "one agent per message" approach; conversation
 * continuity comes from [chatHistoryProvider] and [sessionId], which the caller keeps across calls.
 * [appLabelOf] turns a provider app's package name into its user-visible name for tool-call events.
 */
suspend fun runAgent(
    apiKey: String,
    appFunctionManager: AppFunctionManager,
    prompt: String,
    chatHistoryProvider: ChatHistoryProvider,
    sessionId: String,
    appLabelOf: (packageName: String) -> String? = { null },
    onToolCallEvent: suspend (ToolCallUiEvent) -> Unit = {},
): String {
    val discovered = appFunctionManager.discoverAppFunctions().first().mapNotNull { metadata ->
        runCatching {
            val descriptor = metadata.toToolDescriptor()
            metadata to AppFunctionTool(appFunctionManager, metadata, descriptor)
        }.onFailure { error ->
            Log.w("KoogAgent", "Skipping unsupported AppFunction ${metadata.id}", error)
        }.getOrNull()
    }
    val toolSources = discovered.associate { (metadata, tool) ->
        tool.descriptor.name to ToolSource(metadata.id.substringAfter('#'), appLabelOf(metadata.packageName))
    }
    val toolRegistry = ToolRegistry {
        discovered.forEach { (_, appFunctionTool) -> tool(appFunctionTool) }
    }
    fun functionNameOf(toolName: String) = toolSources[toolName]?.functionName ?: toolName
    fun appLabelOfTool(toolName: String) = toolSources[toolName]?.appLabel
    // koog-agents 1.2.0 has no stable simpleGoogleAIExecutor convenience function (it only ships
    // in prompt-executor-llms-all, which itself has no 1.2.0 release yet — see libs.versions.toml).
    // This mirrors what that helper does: MultiLLMPromptExecutor(LLMProvider.Google to GoogleLLMClient(apiKey)).
    return MultiLLMPromptExecutor(LLMProvider.Google to GoogleLLMClient(apiKey)).use { executor ->
        AIAgent(
            promptExecutor = executor,
            llmModel = Gemini3_5FlashLite,
            toolRegistry = toolRegistry,
            systemPrompt = buildSystemPrompt(),
        ) {
            install(ChatMemory) { this.chatHistoryProvider = chatHistoryProvider }
            handleEvents {
                onToolCallStarting { event ->
                    onToolCallEvent(
                        ToolCallUiEvent.Started(
                            toolCallId = event.toolCallId,
                            functionName = functionNameOf(event.toolName),
                            arguments = event.toolArgs.prettyPrinted(),
                            appLabel = appLabelOfTool(event.toolName),
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
                            appLabel = appLabelOfTool(event.toolName),
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
                            appLabel = appLabelOfTool(event.toolName),
                        ),
                    )
                }
            }
        }.run(prompt, sessionId)
    }
}

private fun ai.koog.serialization.JSONObject.prettyPrinted(): String = Json { prettyPrint = true }
    .encodeToString(JsonObject.serializer(), toKotlinxJsonObject())
