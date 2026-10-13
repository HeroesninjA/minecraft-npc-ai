package ro.ainpc.engine

import java.util.UUID

internal object QuestOfferNpcMatcher {
    fun matches(
        progress: PlayerQuestProgress,
        npcUuid: UUID,
        npcDatabaseId: Int,
        npcName: String,
    ): Boolean {
        if (!progress.isOffered()) return false
        val variables = progress.questVariables()

        firstValue(variables, "quest_giver_uuid")?.let { storedUuid ->
            return storedUuid.equals(npcUuid.toString(), ignoreCase = true)
        }

        firstValue(variables, "quest_giver_db_id", "quest_giver_npc_id")?.let { storedId ->
            return npcDatabaseId > 0 && storedId.toIntOrNull() == npcDatabaseId
        }

        firstValue(
            variables,
            "quest_giver_name",
            "quest_giver_display_name",
            "quest_giver_npc_name",
        )?.let { storedName ->
            return npcName.isNotBlank() && storedName.equals(npcName, ignoreCase = true)
        }

        return false
    }

    private fun firstValue(variables: Map<String, String>, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> variables[key]?.trim()?.takeIf(String::isNotBlank) }
}
