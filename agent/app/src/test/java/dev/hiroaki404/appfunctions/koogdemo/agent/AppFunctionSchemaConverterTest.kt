package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.core.tools.ToolParameterType
import androidx.appfunctions.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun saveItinerarySchemaResolvesNestedReferences() {
        val spot = AppFunctionObjectTypeMetadata(
            properties = mapOf("name" to AppFunctionStringTypeMetadata(false), "description" to AppFunctionStringTypeMetadata(false)),
            required = listOf("name", "description"),
            qualifiedName = "example.Spot",
            isNullable = false,
        )
        val itineraryDay = AppFunctionObjectTypeMetadata(
            properties = mapOf(
                "date" to AppFunctionStringTypeMetadata(false),
                "spots" to AppFunctionArrayTypeMetadata(AppFunctionReferenceTypeMetadata("Spot", false), false),
            ),
            required = listOf("date", "spots"),
            qualifiedName = "example.ItineraryDay",
            isNullable = false,
        )
        val itinerary = AppFunctionObjectTypeMetadata(
            properties = mapOf(
                "title" to AppFunctionStringTypeMetadata(false),
                "days" to AppFunctionArrayTypeMetadata(AppFunctionReferenceTypeMetadata("ItineraryDay", false), false),
            ),
            required = listOf("title", "days"),
            qualifiedName = "example.Itinerary",
            isNullable = false,
        )
        val metadata = function(
            id = "example.Travel#saveItinerary",
            parameters = listOf(parameter("itinerary", AppFunctionReferenceTypeMetadata("Itinerary", false))),
            components = AppFunctionComponentsMetadata(mapOf("Spot" to spot, "ItineraryDay" to itineraryDay, "Itinerary" to itinerary)),
        )

        val root = metadata.toToolDescriptor("saveItinerary").requiredParameters.single().type as ToolParameterType.Object
        val day = (root.properties.first { it.name == "days" }.type as ToolParameterType.List).itemsType as ToolParameterType.Object
        val spotType = (day.properties.first { it.name == "spots" }.type as ToolParameterType.List).itemsType as ToolParameterType.Object

        assertEquals(listOf("title", "days"), root.properties.map { it.name })
        assertEquals(listOf("date", "spots"), day.properties.map { it.name })
        assertEquals(listOf("name", "description"), spotType.properties.map { it.name })
        assertEquals(listOf("name", "description"), spotType.requiredProperties)
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

    @Test
    fun userApprovalIsRecognizedOnlyWhenBothNameAndTypeMatch() {
        val approval = userApprovalType("example.tool.UserApproval")
        val other = AppFunctionObjectTypeMetadata(
            properties = mapOf("approved" to AppFunctionBooleanTypeMetadata(false)),
            required = listOf("approved"),
            qualifiedName = "example.tool.Confirmation",
            isNullable = false,
        )
        val components = AppFunctionComponentsMetadata(mapOf("example.tool.UserApproval" to approval, "example.tool.Confirmation" to other))

        assertTrue(parameter("userApproval", AppFunctionReferenceTypeMetadata("example.tool.UserApproval", false)).isUserApproval(components))
        assertTrue(parameter("userApproval", approval).isUserApproval(components))
        // The name matches but the type does not.
        assertFalse(parameter("userApproval", AppFunctionReferenceTypeMetadata("example.tool.Confirmation", false)).isUserApproval(components))
        assertFalse(parameter("userApproval", AppFunctionBooleanTypeMetadata(false)).isUserApproval(components))
        // The type matches but the name does not.
        assertFalse(parameter("approval", AppFunctionReferenceTypeMetadata("example.tool.UserApproval", false)).isUserApproval(components))
        // An unresolvable reference is not an approval either.
        assertFalse(parameter("userApproval", AppFunctionReferenceTypeMetadata("example.tool.Missing", false)).isUserApproval(components))
    }

    @Test
    fun userApprovalTypeWithoutBooleanApprovedPropertyIsNotAnApproval() {
        val wrong = AppFunctionObjectTypeMetadata(
            properties = mapOf("approved" to AppFunctionStringTypeMetadata(false)),
            required = listOf("approved"),
            qualifiedName = "example.tool.UserApproval",
            isNullable = false,
        )
        val components = AppFunctionComponentsMetadata(mapOf("example.tool.UserApproval" to wrong))

        assertFalse(parameter("userApproval", AppFunctionReferenceTypeMetadata("example.tool.UserApproval", false)).isUserApproval(components))
    }

    @Test
    fun userApprovalParameterIsLeftOutOfTheToolDescriptor() {
        val metadata = function(
            id = "example.Travel#bookFlight",
            parameters = listOf(
                parameter("fromCity", AppFunctionStringTypeMetadata(false)),
                parameter("userApproval", AppFunctionReferenceTypeMetadata("example.tool.UserApproval", false)),
            ),
            components = AppFunctionComponentsMetadata(mapOf("example.tool.UserApproval" to userApprovalType("example.tool.UserApproval"))),
        )

        val descriptor = metadata.toToolDescriptor("bookFlight")

        assertEquals(listOf("fromCity"), descriptor.requiredParameters.map { it.name })
        assertTrue(descriptor.optionalParameters.isEmpty())
        assertEquals("userApproval", metadata.userApprovalParameter()?.name)
    }

    @Test
    fun parameterWithApprovalNameButOtherTypeStaysInTheToolDescriptor() {
        val metadata = function(
            id = "example.Service#act",
            parameters = listOf(parameter("userApproval", AppFunctionBooleanTypeMetadata(false))),
        )

        assertEquals(listOf("userApproval"), metadata.toToolDescriptor("act").requiredParameters.map { it.name })
        assertNull(metadata.userApprovalParameter())
    }

    private fun userApprovalType(qualifiedName: String) = AppFunctionObjectTypeMetadata(
        properties = mapOf("approved" to AppFunctionBooleanTypeMetadata(false)),
        required = listOf("approved"),
        qualifiedName = qualifiedName,
        isNullable = false,
    )

    private fun parameter(name: String, type: AppFunctionDataTypeMetadata) =
        AppFunctionParameterMetadata(name, true, type)

    private fun function(
        id: String,
        parameters: List<AppFunctionParameterMetadata>,
        components: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(),
    ) = AppFunctionMetadata(
        name = AppFunctionName("example.tool", id),
        schema = null,
        parameters = parameters,
        response = AppFunctionResponseMetadata(AppFunctionLongTypeMetadata(false)),
        packageMetadata = AppFunctionPackageMetadata("example.tool", components),
    )
}
