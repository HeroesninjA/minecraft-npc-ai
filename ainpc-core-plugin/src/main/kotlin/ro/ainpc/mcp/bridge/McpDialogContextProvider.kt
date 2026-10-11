package ro.ainpc.mcp.bridge

import com.google.gson.Gson
import com.google.gson.JsonObject
import ro.ainpc.mcp.McpRuntimeClient

/**
 * Mcpdialogcontextprovider.
 */
object McpDialogContextProvider {
    private val gson = Gson()

    fun enrich(
        client: McpRuntimeClient?,
        mcpEnabled: Boolean,
        orchestrationEnabled: Boolean,
        regionId: String
    ): String {
        if (client == null || !mcpEnabled || !orchestrationEnabled || regionId.isBlank()) return ""
        try {
            val result = client.callTool("ainpc.dialog.context", gson.toJson(mapOf("regionId" to regionId)))
            if (!result.available || result.status != "success") return ""

            val payload = gson.fromJson(result.contentJson, JsonObject::class.java)
            if (payload?.get("available")?.asBoolean != true) return ""
            if (payload.get("snapshotState")?.asString !in setOf("fresh", "cached")) return ""
            val local = payload.getAsJsonObject("local") ?: return ""
            if (local.get("regionId")?.asString != regionId) return ""
            val npcCount = local.get("npcCount")?.asInt ?: return ""
            val examples = local.getAsJsonArray("npcExamples")
                ?.mapNotNull { entry ->
                    val sample = entry.asJsonObject
                    val name = compact(sample.get("name")?.asString.orEmpty(), 80)
                    val profession = compact(sample.get("profession")?.asString.orEmpty(), 60)
                    if (name.isBlank()) null else if (profession.isBlank()) name else "$name ($profession)"
                }
                .orEmpty()
            return buildString {
                append("=== CONTEXT LOCAL DIN MCP ===\n")
                append("In regiunea curenta sunt $npcCount NPC-uri inregistrate.\n")
                if (examples.isNotEmpty()) {
                    append("Exemple de NPC-uri din regiune (lista incompleta): ")
                    append(examples.joinToString(", "))
                    append(".\n")
                }
                append("Foloseste aceste date doar cand intrebarea este despre regiune sau locuitorii ei.\n\n")
            }
        } catch (_: Exception) {
            return ""
        }
    }

    private fun compact(value: String, limit: Int): String = value.replace(Regex("\\s+"), " ").trim().take(limit)
}
