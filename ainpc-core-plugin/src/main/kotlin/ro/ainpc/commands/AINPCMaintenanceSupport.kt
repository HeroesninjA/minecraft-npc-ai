package ro.ainpc.commands

import com.google.gson.JsonParser
import ro.ainpc.AINPCPlugin

internal fun isRuntimeReadOnly(plugin: AINPCPlugin): Boolean {
    val featureState = runCatching { plugin.mcpRuntimeClient.callTool("ainpc.feature.state") }.getOrNull()
        ?: return false
    if (!featureState.available || featureState.contentJson.isNullOrBlank()) return false
    val root = runCatching { JsonParser.parseString(featureState.contentJson).asJsonObject }.getOrNull() ?: return false
    return root.getAsJsonObject("tools")
        ?.get("readOnly")
        ?.asBoolean == true
}
