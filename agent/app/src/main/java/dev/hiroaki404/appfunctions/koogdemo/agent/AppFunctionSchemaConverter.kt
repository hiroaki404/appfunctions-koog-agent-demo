package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import androidx.appfunctions.metadata.AppFunctionAllOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBytesTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionOneOfTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionParcelableTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata

class UnsupportedAppFunctionTypeException(message: String) : IllegalArgumentException(message)

/** Produces a Gemini-compatible name without leaking a package-qualified function identifier. */
fun toolNameOf(metadata: AppFunctionMetadata): String {
    val (owner, function) = metadata.id.split('#', limit = 2).let { it.firstOrNull().orEmpty() to it.getOrNull(1).orEmpty() }
    val simpleOwner = owner.substringAfterLast('.').ifBlank { "appFunction" }
    return "${simpleOwner}_${function.ifBlank { "call" }}"
        .replace(Regex("[^a-zA-Z0-9_]"), "_")
        .take(64)
        .ifBlank { "appFunction" }
}

fun AppFunctionMetadata.toToolDescriptor(toolName: String = toolNameOf(this)): ToolDescriptor {
    fun descriptor(parameter: AppFunctionParameterMetadata) =
        ToolParameterDescriptor(parameter.name, parameter.description, parameter.dataType.toToolParameterType(components))

    return ToolDescriptor(
        name = toolName,
        description = description,
        requiredParameters = parameters.filter { it.isRequired && !it.dataType.isNullable }.map(::descriptor),
        optionalParameters = parameters.filterNot { it.isRequired && !it.dataType.isNullable }.map(::descriptor),
    )
}

fun AppFunctionDataTypeMetadata.toToolParameterType(
    components: AppFunctionComponentsMetadata,
    visitingReferences: Set<String> = emptySet(),
): ToolParameterType = when (this) {
    is AppFunctionStringTypeMetadata -> enumValues?.let { ToolParameterType.Enum(it.toTypedArray()) } ?: ToolParameterType.String
    is AppFunctionIntTypeMetadata -> enumValues?.let { ToolParameterType.Enum(it.map(Int::toString).toTypedArray()) } ?: ToolParameterType.Integer
    is AppFunctionLongTypeMetadata -> ToolParameterType.Integer
    is AppFunctionFloatTypeMetadata, is AppFunctionDoubleTypeMetadata -> ToolParameterType.Float
    is AppFunctionBooleanTypeMetadata -> ToolParameterType.Boolean
    is AppFunctionUnitTypeMetadata -> ToolParameterType.Null
    is AppFunctionArrayTypeMetadata -> ToolParameterType.List(itemType.toToolParameterType(components, visitingReferences))
    is AppFunctionObjectTypeMetadata -> ToolParameterType.Object(
        properties = properties.map { (name, type) ->
            ToolParameterDescriptor(name, type.description, type.toToolParameterType(components, visitingReferences))
        },
        requiredProperties = required,
        additionalProperties = false,
    )
    is AppFunctionReferenceTypeMetadata -> {
        if (referenceDataType in visitingReferences) {
            throw UnsupportedAppFunctionTypeException("Circular AppFunction type reference: $referenceDataType")
        }
        val resolved = components.dataTypes[referenceDataType]
            ?: throw UnsupportedAppFunctionTypeException("Unknown AppFunction type reference: $referenceDataType")
        resolved.toToolParameterType(components, visitingReferences + referenceDataType)
    }
    is AppFunctionAllOfTypeMetadata,
    is AppFunctionOneOfTypeMetadata,
    is AppFunctionBytesTypeMetadata,
    is AppFunctionParcelableTypeMetadata -> throw UnsupportedAppFunctionTypeException("Unsupported AppFunction type: ${javaClass.simpleName}")
    else -> throw UnsupportedAppFunctionTypeException("Unsupported AppFunction type: ${javaClass.simpleName}")
}
