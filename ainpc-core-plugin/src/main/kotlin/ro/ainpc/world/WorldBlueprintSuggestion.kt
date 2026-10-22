package ro.ainpc.world

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

internal enum class WorldBlueprintSuggestionField(val id: String) {
    STORY("story"), PROPERTY("property"), PROFESSION_LIMIT("profession-limit"),
    PROFESSION("profession"), ARCHITECTURE("architecture"), MAP_RULE("map-rule");

    companion object {
        fun fromId(value: String): WorldBlueprintSuggestionField? =
            entries.firstOrNull { it.id == value.lowercase(Locale.ROOT) }
    }
}

internal data class WorldBlueprintSuggestionRequest(
    val worldName: String,
    val field: WorldBlueprintSuggestionField,
    val key: String?,
    val instruction: String,
    val baseFingerprint: String,
)

internal data class WorldBlueprintSuggestionProvenance(
    val provider: String,
    val model: String,
    val requestedAt: Long,
    val instructionFingerprint: String,
)

internal data class WorldBlueprintSuggestion(
    val worldName: String,
    val field: WorldBlueprintSuggestionField,
    val key: String?,
    val baseFingerprint: String,
    val candidate: WorldBlueprint,
    val provenance: WorldBlueprintSuggestionProvenance,
)

internal data class WorldBlueprintSavedSuggestion(val suggestion: WorldBlueprintSuggestion, val savedAt: Long)

internal object WorldBlueprintSuggestionParser {
    fun requestFingerprint(instruction: String): String = MessageDigest.getInstance("SHA-256")
        .digest(instruction.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    fun prompt(request: WorldBlueprintSuggestionRequest): String {
        val schema = when (request.field) {
            WorldBlueprintSuggestionField.PROFESSION_LIMIT -> "{\"limit\":8}"
            WorldBlueprintSuggestionField.PROFESSION -> "{\"max_npcs\":2,\"description\":\"descriere\"}"
            else -> "{\"text\":\"descriere\"}"
        }
        return "Propune doar un camp de draft pentru o lume Minecraft. " +
            "Raspunde exclusiv cu un obiect JSON, fara Markdown, explicatii sau alte chei. " +
            "Schema exacta: $schema. Nu propune comenzi, actiuni sau blocuri de executat. " +
            "Camp: ${request.field.id}; cheie: ${request.key ?: "nu exista"}. " +
            "Cererea administratorului: ${request.instruction}"
    }

    fun parse(
        request: WorldBlueprintSuggestionRequest,
        base: WorldBlueprint,
        response: String,
        provenance: WorldBlueprintSuggestionProvenance,
    ): WorldBlueprintSuggestion {
        require(base.worldName == request.worldName) { "Lumea propunerii nu corespunde draftului." }
        require(provenance.instructionFingerprint == requestFingerprint(request.instruction)) {
            "Provenienta cererii este invalida."
        }
        val raw = response.trim()
        require(raw.length in 2..8_192 && raw.startsWith('{') && raw.endsWith('}')) {
            "AI nu a returnat un obiect JSON valid."
        }
        val value = try {
            JsonParser.parseString(raw).asJsonObject
        } catch (_: RuntimeException) {
            throw IllegalArgumentException("AI nu a returnat un obiect JSON valid.")
        }
        val expectedKeys = when (request.field) {
            WorldBlueprintSuggestionField.PROFESSION_LIMIT -> setOf("limit")
            WorldBlueprintSuggestionField.PROFESSION -> setOf("max_npcs", "description")
            else -> setOf("text")
        }
        require(value.keySet() == expectedKeys) { "Schema propunerii AI este diferita de cea asteptata." }
        val key = request.key
        val candidate = when (request.field) {
            WorldBlueprintSuggestionField.STORY -> base.copy(story = stringField(value, "text"))
            WorldBlueprintSuggestionField.ARCHITECTURE -> base.copy(architecture = stringField(value, "text"))
            WorldBlueprintSuggestionField.PROPERTY -> {
                require(key != null && WorldBlueprintPolicy.validKey(key)) { "Cheia proprietatii este invalida." }
                base.copy(properties = base.properties + (key to stringField(value, "text")))
            }
            WorldBlueprintSuggestionField.MAP_RULE -> {
                require(key != null && WorldBlueprintPolicy.validKey(key)) { "Cheia regulii este invalida." }
                base.copy(mapRules = base.mapRules + (key to stringField(value, "text")))
            }
            WorldBlueprintSuggestionField.PROFESSION_LIMIT -> base.copy(professionTypeLimit = intField(value, "limit"))
            WorldBlueprintSuggestionField.PROFESSION -> {
                require(key != null && WorldBlueprintPolicy.validKey(key)) { "ID-ul meseriei este invalid." }
                base.copy(professions = base.professions + (key to WorldProfessionBlueprint(
                    intField(value, "max_npcs"), stringField(value, "description"),
                )))
            }
        }
        val issues = WorldBlueprintPolicy.draftIssues(candidate)
        require(issues.isEmpty()) { "Propunere incompatibila cu limitele draftului: ${issues.joinToString("; ")}" }
        return WorldBlueprintSuggestion(request.worldName, request.field, request.key, request.baseFingerprint,
            candidate.copy(approval = null, suggestionHistory = emptyList()), provenance)
    }

    private fun stringField(value: JsonObject, key: String): String {
        val field = value.get(key)
        require(field != null && field.isJsonPrimitive && field.asJsonPrimitive.isString) {
            "$key trebuie sa fie text."
        }
        return field.asString
    }

    private fun intField(value: JsonObject, key: String): Int {
        val field = value.get(key)
        require(field != null && field.isJsonPrimitive && field.asJsonPrimitive.isNumber) {
            "$key trebuie sa fie numar intreg."
        }
        return field.asString.toIntOrNull() ?: throw IllegalArgumentException("$key trebuie sa fie numar intreg.")
    }
}
