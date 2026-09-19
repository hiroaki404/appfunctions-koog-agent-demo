package dev.hiroaki404.appfunctions.koogdemo.agent

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.serialization.JSONObject
import ai.koog.serialization.kotlinx.toKotlinxJsonObject
import ai.koog.serialization.typeToken
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionBooleanTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDoubleTypeMetadata
import androidx.appfunctions.metadata.AppFunctionFloatTypeMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class AppFunctionTool(
    private val manager: AppFunctionManager,
    private val appFunctionMetadata: AppFunctionMetadata,
    descriptor: ToolDescriptor,
) : Tool<JSONObject, String>(typeToken<JSONObject>(), typeToken<String>(), descriptor) {
    override suspend fun execute(args: JSONObject): String = try {
        val parameters = AppFunctionData.Builder(appFunctionMetadata.parameters, appFunctionMetadata.components).also { builder ->
            args.toKotlinxJsonObject().forEach { (name, value) ->
                val type = appFunctionMetadata.parameters.firstOrNull { it.name == name }?.dataType ?: return@forEach
                if (value !is JsonNull) builder.setJsonValue(name, value, type)
            }
        }.build()
        when (val response = manager.executeAppFunction(
            ExecuteAppFunctionRequest(appFunctionMetadata.packageName, appFunctionMetadata.id, parameters),
        )) {
            is ExecuteAppFunctionResponse.Success -> Json.encodeToString(JsonElement.serializer(), valueToJson(
                response.returnValue,
                ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
                resolve(appFunctionMetadata.response.valueType),
            ))
            is ExecuteAppFunctionResponse.Error -> "Error: ${response.error.errorMessage}"
        }
    } catch (e: Exception) {
        "Error: ${e.message ?: e.javaClass.simpleName}"
    }

    private fun AppFunctionData.Builder.setJsonValue(name: String, value: JsonElement, type: AppFunctionDataTypeMetadata) {
        when (val resolved = resolve(type)) {
            is AppFunctionStringTypeMetadata -> setString(name, value.jsonPrimitive.content)
            is AppFunctionIntTypeMetadata -> setInt(name, value.jsonPrimitive.int)
            is AppFunctionLongTypeMetadata -> setLong(name, value.jsonPrimitive.long)
            is AppFunctionFloatTypeMetadata -> setFloat(name, value.jsonPrimitive.float)
            is AppFunctionDoubleTypeMetadata -> setDouble(name, value.jsonPrimitive.double)
            is AppFunctionBooleanTypeMetadata -> setBoolean(name, value.jsonPrimitive.boolean)
            is AppFunctionObjectTypeMetadata -> setAppFunctionData(name, objectData(value.jsonObject, resolved))
            is AppFunctionArrayTypeMetadata -> setArray(name, value.jsonArray, resolved.itemType)
            else -> throw UnsupportedAppFunctionTypeException("Unsupported AppFunction value type: ${resolved.javaClass.simpleName}")
        }
    }

    private fun AppFunctionData.Builder.setArray(name: String, values: JsonArray, itemType: AppFunctionDataTypeMetadata) {
        when (resolve(itemType)) {
            is AppFunctionStringTypeMetadata -> setStringList(name, values.map { it.jsonPrimitive.content })
            is AppFunctionIntTypeMetadata -> setIntArray(name, values.map { it.jsonPrimitive.int }.toIntArray())
            is AppFunctionLongTypeMetadata -> setLongArray(name, values.map { it.jsonPrimitive.long }.toLongArray())
            is AppFunctionFloatTypeMetadata -> setFloatArray(name, values.map { it.jsonPrimitive.float }.toFloatArray())
            is AppFunctionDoubleTypeMetadata -> setDoubleArray(name, values.map { it.jsonPrimitive.double }.toDoubleArray())
            is AppFunctionBooleanTypeMetadata -> setBooleanArray(name, values.map { it.jsonPrimitive.boolean }.toBooleanArray())
            is AppFunctionObjectTypeMetadata -> setAppFunctionDataList(name, values.map { objectData(it.jsonObject, resolve(itemType) as AppFunctionObjectTypeMetadata) })
            else -> throw UnsupportedAppFunctionTypeException("Unsupported AppFunction array item type")
        }
    }

    private fun objectData(json: JsonObject, type: AppFunctionObjectTypeMetadata): AppFunctionData =
        AppFunctionData.Builder(type, appFunctionMetadata.components).also { builder ->
            json.forEach { (name, value) -> type.properties[name]?.let { propertyType ->
                if (value !is JsonNull) builder.setJsonValue(name, value, propertyType)
            } }
        }.build()

    private fun resolve(type: AppFunctionDataTypeMetadata): AppFunctionDataTypeMetadata =
        if (type is AppFunctionReferenceTypeMetadata) appFunctionMetadata.components.dataTypes[type.referenceDataType]
            ?: throw UnsupportedAppFunctionTypeException("Unknown AppFunction type reference: ${type.referenceDataType}") else type

    private fun valueToJson(data: AppFunctionData, key: String, type: AppFunctionDataTypeMetadata): JsonElement = when (type) {
        is AppFunctionStringTypeMetadata -> JsonPrimitive(data.getString(key))
        is AppFunctionIntTypeMetadata -> JsonPrimitive(data.getInt(key))
        is AppFunctionLongTypeMetadata -> JsonPrimitive(data.getLong(key))
        is AppFunctionFloatTypeMetadata -> JsonPrimitive(data.getFloat(key))
        is AppFunctionDoubleTypeMetadata -> JsonPrimitive(data.getDouble(key))
        is AppFunctionBooleanTypeMetadata -> JsonPrimitive(data.getBoolean(key))
        is AppFunctionObjectTypeMetadata -> JsonObject(type.properties.mapValues { (name, propertyType) ->
            valueToJson(data.getAppFunctionData(key)!!, name, resolve(propertyType))
        })
        is AppFunctionArrayTypeMetadata -> arrayToJson(data, key, type.itemType)
        else -> throw UnsupportedAppFunctionTypeException("Unsupported AppFunction return type")
    }

    private fun arrayToJson(data: AppFunctionData, key: String, itemType: AppFunctionDataTypeMetadata): JsonElement = JsonArray(when (val type = resolve(itemType)) {
        is AppFunctionStringTypeMetadata -> data.getStringList(key).orEmpty().map(::JsonPrimitive)
        is AppFunctionIntTypeMetadata -> (data.getIntArray(key) ?: IntArray(0)).map(::JsonPrimitive)
        is AppFunctionLongTypeMetadata -> (data.getLongArray(key) ?: LongArray(0)).map(::JsonPrimitive)
        is AppFunctionFloatTypeMetadata -> (data.getFloatArray(key) ?: FloatArray(0)).map(::JsonPrimitive)
        is AppFunctionDoubleTypeMetadata -> (data.getDoubleArray(key) ?: DoubleArray(0)).map(::JsonPrimitive)
        is AppFunctionBooleanTypeMetadata -> (data.getBooleanArray(key) ?: BooleanArray(0)).map(::JsonPrimitive)
        is AppFunctionObjectTypeMetadata -> data.getAppFunctionDataList(key).orEmpty().map { objectData ->
            JsonObject(type.properties.mapValues { (name, propertyType) -> valueToJson(objectData, name, resolve(propertyType)) })
        }
        else -> throw UnsupportedAppFunctionTypeException("Unsupported AppFunction return array item type")
    })
}
