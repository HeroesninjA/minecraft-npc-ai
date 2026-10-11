package ro.ainpc.story

import com.google.gson.JsonParser
import com.google.gson.JsonParseException

internal class NpcStorySignalResolver {
    private val rolePattern = Regex("[a-z][a-z0-9_]{0,63}")

    fun collect(profileDataJson: String?): List<String> {
        if (profileDataJson.isNullOrBlank()) return emptyList()
        val narrative = try {
            val profile = JsonParser.parseString(profileDataJson)
            if (!profile.isJsonObject) return emptyList()
            profile.asJsonObject.get("narrative")?.takeIf { it.isJsonObject }?.asJsonObject
                ?: return emptyList()
        } catch (_: JsonParseException) {
            return emptyList()
        }

        return listOf("quest_role", "social_role").mapNotNull { key ->
            val value = narrative.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString?.trim() ?: return@mapNotNull null
            value.takeIf(rolePattern::matches)?.let { "npc_$key=$it" }
        }
    }
}
