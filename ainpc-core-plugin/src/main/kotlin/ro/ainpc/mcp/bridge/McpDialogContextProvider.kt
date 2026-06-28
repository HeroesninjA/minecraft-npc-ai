package ro.ainpc.mcp.bridge

import com.google.gson.Gson
import com.google.gson.JsonObject
import ro.ainpc.mcp.McpRuntimeClient

object McpDialogContextProvider {
    private val gson = Gson()

    fun enrich(
        client: McpRuntimeClient?,
        mcpEnabled: Boolean,
        orchestrationEnabled: Boolean
    ): String {
        if (client == null || !mcpEnabled || !orchestrationEnabled) return ""
        try {
            val result = client.callTool("ainpc.dialog.context", "{}")
            if (!result.available || result.status != "success") return ""

            val payload = gson.fromJson(result.contentJson, JsonObject::class.java)
            val snapshot = payload?.getAsJsonObject("result")?.getAsJsonObject("content")
                ?: return ""

            val contextParts = mutableListOf<String>()
            val server = snapshot.getAsJsonObject("server")
            if (server != null) {
                contextParts.add("jucatori online: ${server.get("onlinePlayers")?.asInt ?: 0}")
                contextParts.add("server activ de ${server.get("uptimeMinutes")?.asLong ?: 0} min")
            }
            val npcData = snapshot.getAsJsonObject("npc")
            if (npcData != null) {
                contextParts.add("NPC-uri in lume: ${npcData.get("totalNpcs")?.asInt ?: 0}")
            }
            val world = snapshot.getAsJsonObject("world")
            if (world != null) {
                contextParts.add("regiuni: ${world.get("totalRegions")?.asInt ?: 0}")
                contextParts.add("locuri: ${world.get("totalPlaces")?.asInt ?: 0}")
            }
            val quests = snapshot.getAsJsonObject("quests")
            if (quests != null) {
                contextParts.add("questuri active: ${quests.get("active")?.asInt ?: 0}")
            }
            val features = snapshot.getAsJsonObject("features")
            if (features != null) {
                contextParts.add("AI activ: ${features.get("aiEnabled")?.asBoolean ?: false}")
            }
            return if (contextParts.isNotEmpty()) {
                "=== CONTEXT DIN MCP RUNTIME ===\n${contextParts.joinToString("\n")}\n\n"
            } else ""
        } catch (_: Exception) {
            return ""
        }
    }
}
