package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import android.util.Log
import androidx.appfunctions.AppFunctionManager
import kotlinx.coroutines.flow.first

/**
 * Runs a single-turn Koog agent with tools discovered at runtime. A new [AIAgent] is created for every
 * call, per the demo's "one agent per message" approach (see handoff 4.章 agent側の実装方針).
 */
suspend fun runAgent(
    apiKey: String,
    appFunctionManager: AppFunctionManager,
    prompt: String,
): String {
    val tools = appFunctionManager.discoverAppFunctions().first().mapNotNull { metadata ->
        runCatching {
            val descriptor = metadata.toToolDescriptor()
            AppFunctionTool(appFunctionManager, metadata, descriptor)
        }.onFailure { error ->
            Log.w("KoogAgent", "Skipping unsupported AppFunction ${metadata.id}", error)
        }.getOrNull()
    }
    val toolRegistry = ToolRegistry {
        tools.forEach { tool(it) }
    }
    // koog-agents 1.2.0 has no stable simpleGoogleAIExecutor convenience function (it only ships
    // in prompt-executor-llms-all, which itself has no 1.2.0 release yet — see libs.versions.toml).
    // This mirrors what that helper does: MultiLLMPromptExecutor(LLMProvider.Google to GoogleLLMClient(apiKey)).
    return MultiLLMPromptExecutor(LLMProvider.Google to GoogleLLMClient(apiKey)).use { executor ->
        AIAgent(
            promptExecutor = executor,
            llmModel = GoogleModels.Gemini2_5Flash,
            toolRegistry = toolRegistry,
            systemPrompt = "You are a helpful assistant. Use the available tools when they can fulfill the request.",
        ).run(prompt)
    }
}
