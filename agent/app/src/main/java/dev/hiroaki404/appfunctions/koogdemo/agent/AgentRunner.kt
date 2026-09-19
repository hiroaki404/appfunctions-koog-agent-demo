package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import androidx.appfunctions.AppFunctionManager

/**
 * Runs a single-turn Koog agent with a hardcoded [AddTool]. A new [AIAgent] is created for every
 * call, per the demo's "one agent per message" approach (see handoff 4.章 agent側の実装方針).
 */
suspend fun runAgent(
    apiKey: String,
    appFunctionManager: AppFunctionManager,
    prompt: String,
): String {
    val toolRegistry = ToolRegistry {
        tool(AddTool(appFunctionManager))
    }
    // koog-agents 1.2.0 has no stable simpleGoogleAIExecutor convenience function (it only ships
    // in prompt-executor-llms-all, which itself has no 1.2.0 release yet — see libs.versions.toml).
    // This mirrors what that helper does: MultiLLMPromptExecutor(LLMProvider.Google to GoogleLLMClient(apiKey)).
    return MultiLLMPromptExecutor(LLMProvider.Google to GoogleLLMClient(apiKey)).use { executor ->
        AIAgent(
            promptExecutor = executor,
            llmModel = GoogleModels.Gemini2_5Flash,
            toolRegistry = toolRegistry,
            systemPrompt = "You are a helpful assistant. Use the add tool for any addition.",
        ).run(prompt)
    }
}
