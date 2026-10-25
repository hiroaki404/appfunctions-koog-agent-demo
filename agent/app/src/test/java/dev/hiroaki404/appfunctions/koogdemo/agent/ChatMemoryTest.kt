package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies that Koog 1.2.0's ChatMemory carries history across fresh AIAgent instances sharing a provider and sessionId. */
class ChatMemoryTest {
    private class RecordingExecutor : PromptExecutor() {
        val prompts = mutableListOf<Prompt>()

        override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): Message.Assistant {
            prompts += prompt
            return Message.Assistant("reply ${prompts.size}", ResponseMetaInfo.Empty)
        }

        override fun executeStreaming(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): Flow<StreamFrame> =
            throw UnsupportedOperationException()

        override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult = throw UnsupportedOperationException()

        override fun close() = Unit
    }

    private suspend fun runTurn(executor: RecordingExecutor, provider: InMemoryChatHistoryProvider, input: String, sessionId: String) =
        AIAgent(
            promptExecutor = executor,
            llmModel = GoogleModels.Gemini2_5Flash,
            toolRegistry = ToolRegistry { },
            systemPrompt = "system",
        ) {
            install(ChatMemory) { chatHistoryProvider = provider }
        }.run(input, sessionId)

    private fun Prompt.userTexts() = messages.filterIsInstance<Message.User>().map { it.textContent() }

    @Test
    fun sameSessionIdCarriesHistoryAcrossAgents() = runBlocking {
        val executor = RecordingExecutor()
        val provider = InMemoryChatHistoryProvider()

        runTurn(executor, provider, "first", "session-a")
        runTurn(executor, provider, "second", "session-a")

        val secondPrompt = executor.prompts.last()
        assertEquals(listOf("first", "second"), secondPrompt.userTexts())
        assertTrue(secondPrompt.messages.any { it is Message.Assistant && it.textContent() == "reply 1" })
        assertEquals(1, secondPrompt.messages.count { it is Message.System })
    }

    @Test
    fun differentSessionIdStartsFresh() = runBlocking {
        val executor = RecordingExecutor()
        val provider = InMemoryChatHistoryProvider()

        runTurn(executor, provider, "first", "session-a")
        runTurn(executor, provider, "second", "session-b")

        assertEquals(listOf("second"), executor.prompts.last().userTexts())
    }
}
