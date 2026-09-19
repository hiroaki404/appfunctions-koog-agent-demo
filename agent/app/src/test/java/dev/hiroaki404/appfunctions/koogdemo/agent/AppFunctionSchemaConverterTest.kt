package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.core.tools.ToolParameterType
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFunctionSchemaConverterTest {
    @Test
    fun addSchemaCreatesTwoRequiredIntegerParameters() {
        val metadata = function(
            id = "example.Calculator#add",
            parameters = listOf(parameter("num1", AppFunctionLongTypeMetadata(false)), parameter("num2", AppFunctionLongTypeMetadata(false))),
        )

        val descriptor = metadata.toToolDescriptor("add")

        assertEquals("add", descriptor.name)
        assertEquals(listOf("num1", "num2"), descriptor.requiredParameters.map { it.name })
        assertTrue(descriptor.requiredParameters.all { it.type == ToolParameterType.Integer })
    }

    @Test
    fun calculateTotalSchemaResolvesArrayReferenceObject() {
        val lineItem = AppFunctionObjectTypeMetadata(
            properties = mapOf(
                "name" to AppFunctionStringTypeMetadata(false),
                "unitPrice" to AppFunctionLongTypeMetadata(false),
                "quantity" to AppFunctionLongTypeMetadata(false),
            ),
            required = listOf("name", "unitPrice", "quantity"),
            qualifiedName = "example.LineItem",
            isNullable = false,
        )
        val metadata = function(
            id = "example.Calculator#calculateTotal",
            parameters = listOf(
                parameter("items", AppFunctionArrayTypeMetadata(AppFunctionReferenceTypeMetadata("LineItem", false), false)),
                parameter("discountPercent", AppFunctionLongTypeMetadata(false)),
            ),
            components = AppFunctionComponentsMetadata(mapOf("LineItem" to lineItem)),
        )

        val itemType = (metadata.toToolDescriptor("calculateTotal").requiredParameters.first().type as ToolParameterType.List).itemsType

        val item = itemType as ToolParameterType.Object
        assertEquals(listOf("name", "unitPrice", "quantity"), item.properties.map { it.name })
        assertEquals(listOf("name", "unitPrice", "quantity"), item.requiredProperties)
        assertEquals(ToolParameterType.Integer, item.properties.first { it.name == "unitPrice" }.type)
    }

    @Test
    fun circularReferenceIsRejectedWithoutRecursingForever() {
        val selfReference = AppFunctionReferenceTypeMetadata("Node", false)
        val node = AppFunctionObjectTypeMetadata(
            properties = mapOf("next" to selfReference), required = emptyList(), qualifiedName = "example.Node", isNullable = false,
        )
        val metadata = function("example.Service#node", listOf(parameter("node", selfReference)), AppFunctionComponentsMetadata(mapOf("Node" to node)))

        val failure = runCatching { metadata.toToolDescriptor("node") }.exceptionOrNull()

        assertTrue(failure is UnsupportedAppFunctionTypeException)
    }

    @Test
    fun unsupportedTypesCanBeSkippedByCaller() {
        val allOf = AppFunctionAllOfTypeMetadata(emptyList(), "example.Unsupported", false)
        val metadata = function("example.Service#unsupported", listOf(parameter("value", allOf)))

        assertEquals(null, runCatching { metadata.toToolDescriptor("unsupported") }.getOrNull())
    }

    @Test
    fun longFunctionIdProducesGeminiCompatibleName() {
        val metadata = function("dev.example.a.very.long.package.Name-With-Invalid-Characters#an-even-longer-function-name-than-gemini-allows", emptyList())

        val name = toolNameOf(metadata)

        assertTrue(name.length <= 64)
        assertTrue(name.matches(Regex("[a-zA-Z0-9_]+")))
    }

    private fun parameter(name: String, type: AppFunctionDataTypeMetadata) =
        AppFunctionParameterMetadata(name, true, type)

    private fun function(
        id: String,
        parameters: List<AppFunctionParameterMetadata>,
        components: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(),
    ) = AppFunctionMetadata(
        id = id,
        packageName = "example.tool",
        isEnabled = true,
        schema = null,
        parameters = parameters,
        response = AppFunctionResponseMetadata(AppFunctionLongTypeMetadata(false)),
        components = components,
    )
}
