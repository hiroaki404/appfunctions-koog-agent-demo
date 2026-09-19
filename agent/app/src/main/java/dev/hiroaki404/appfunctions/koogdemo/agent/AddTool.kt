package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.metadata.AppFunctionMetadata
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

/**
 * Hand-written Koog tool wired to a single, hardcoded AppFunction ("Before" state of the demo).
 * Unlike the generic tool added in a later phase, this tool knows the target package and function
 * id at compile time and only supports `add`'s specific parameter shape.
 */
class AddTool(
    private val appFunctionManager: AppFunctionManager,
) : SimpleTool<AddTool.Args>(
    argsType = typeToken<Args>(),
    name = "add",
    description = "Adds two integers together by calling the tool app's AppFunction.",
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The first integer")
        val num1: Long,
        @property:LLMDescription("The second integer")
        val num2: Long,
    )

    override suspend fun execute(args: Args): String {
        val metadata = findAddFunctionMetadata()
            ?: return "Error: add function not found. Is the tool app installed?"

        val parameters = AppFunctionData.Builder(metadata.parameters, metadata.components)
            .setLong("num1", args.num1)
            .setLong("num2", args.num2)
            .build()

        val request = ExecuteAppFunctionRequest(
            targetPackageName = TOOL_PACKAGE,
            functionIdentifier = metadata.id,
            functionParameters = parameters,
        )

        return when (val response = appFunctionManager.executeAppFunction(request)) {
            is ExecuteAppFunctionResponse.Success ->
                "Result: ${response.returnValue.getLong(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)}"
            is ExecuteAppFunctionResponse.Error -> "Error: ${response.error.errorMessage}"
        }
    }

    private suspend fun findAddFunctionMetadata(): AppFunctionMetadata? =
        appFunctionManager.discoverAppFunctions().first()
            .firstOrNull { it.packageName == TOOL_PACKAGE && it.id.endsWith("#add") }

    private companion object {
        const val TOOL_PACKAGE = "dev.hiroaki404.appfunctions.koogdemo.tool"
    }
}
