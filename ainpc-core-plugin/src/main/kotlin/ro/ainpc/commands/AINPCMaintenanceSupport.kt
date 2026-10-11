package ro.ainpc.commands

import com.google.gson.JsonParser
import ro.ainpc.AINPCPlugin

internal fun isActivityBuildRuntimeWritable(plugin: AINPCPlugin): Boolean {
    val state = runCatching { plugin.mcpRuntimeClient.callTool("ainpc.feature.state") }.getOrNull() ?: return false
    return state.available && activityBuildWriteAllowed(state.contentJson)
}

internal fun activityBuildWriteAllowed(content: String?): Boolean = runCatching {
    val value = JsonParser.parseString(content ?: return false).asJsonObject
        .getAsJsonObject("tools")?.get("readOnly") ?: return false
    value.isJsonPrimitive && value.asJsonPrimitive.isBoolean && !value.asBoolean
}.getOrDefault(false)

internal fun isRuntimeReadOnly(plugin: AINPCPlugin): Boolean {
    val featureState = runCatching { plugin.mcpRuntimeClient.callTool("ainpc.feature.state") }.getOrNull()
        ?: return false
    if (!featureState.available || featureState.contentJson.isNullOrBlank()) return false
    val root = runCatching { JsonParser.parseString(featureState.contentJson).asJsonObject }.getOrNull() ?: return false
    return root.getAsJsonObject("tools")
        ?.get("readOnly")
        ?.asBoolean == true
}
