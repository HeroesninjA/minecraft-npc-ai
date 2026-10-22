package ro.ainpc.world

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

data class WorldProfessionBlueprint(val maxNpcs: Int, val description: String)

data class WorldBlueprintApproval(val fingerprint: String, val approvedBy: String, val approvedAt: Long)

internal data class WorldBlueprintSuggestionAcceptance(
    val field: WorldBlueprintSuggestionField,
    val key: String?,
    val baseFingerprint: String,
    val acceptedFingerprint: String,
    val provenance: WorldBlueprintSuggestionProvenance,
    val acceptedBy: String,
    val acceptedAt: Long,
)

internal data class WorldBlueprint(
    val worldName: String,
    val story: String = "",
    val properties: Map<String, String> = emptyMap(),
    val professionTypeLimit: Int = 16,
    val professions: Map<String, WorldProfessionBlueprint> = emptyMap(),
    val architecture: String = "",
    val mapRules: Map<String, String> = emptyMap(),
    val approval: WorldBlueprintApproval? = null,
    val suggestionHistory: List<WorldBlueprintSuggestionAcceptance> = emptyList(),
)

internal object WorldBlueprintPolicy {
    private val keyPattern = Regex("[a-z][a-z0-9_-]{0,31}")

    fun validKey(key: String): Boolean = keyPattern.matches(key)

    fun draftIssues(blueprint: WorldBlueprint): List<String> = buildList {
        if (blueprint.worldName.isBlank() || blueprint.worldName.length > 128 ||
            blueprint.worldName.any { it.isISOControl() }
        ) add("numele lumii este invalid")
        if (blueprint.story.length > 4_000 || blueprint.story.any { it.isISOControl() }) add("povestea este invalida")
        if (blueprint.architecture.length > 2_000 || blueprint.architecture.any { it.isISOControl() }) {
            add("arhitectura este invalida")
        }
        if (blueprint.professionTypeLimit !in 1..64) add("limita tipurilor de meserii trebuie sa fie intre 1 si 64")
        if (blueprint.professions.size > blueprint.professionTypeLimit) add("prea multe tipuri de meserii")
        if (blueprint.properties.size > 32 || blueprint.mapRules.size > 32) add("prea multe proprietati sau reguli")
        if (blueprint.properties.any { (key, value) -> !validKey(key) || !validPrompt(value, 1_000) }) {
            add("proprietate invalida")
        }
        if (blueprint.mapRules.any { (key, value) -> !validKey(key) || !validPrompt(value, 1_000) }) {
            add("regula de harta invalida")
        }
        if (blueprint.professions.any { (key, profession) ->
                !validKey(key) || profession.maxNpcs !in 0..512 || !validPrompt(profession.description, 1_000)
            }
        ) add("meserie invalida")
    }

    fun completionIssues(blueprint: WorldBlueprint): List<String> = draftIssues(blueprint) + buildList {
        if (blueprint.story.isBlank()) add("povestea lumii lipseste")
        if (blueprint.properties.isEmpty()) add("proprietatile lumii lipsesc")
        if (blueprint.professions.isEmpty()) add("meseriile lipsesc")
        if (blueprint.architecture.isBlank()) add("stilul arhitectural lipseste")
        if (blueprint.mapRules.isEmpty()) add("regulile hartii lipsesc")
    }

    private fun validPrompt(value: String, limit: Int): Boolean =
        value.isNotBlank() && value.length <= limit && value.none { it.isISOControl() }
}

internal class WorldBlueprintStore(private val file: Path) {
    private data class State(
        val worlds: Map<String, WorldBlueprint> = emptyMap(),
        val suggestions: Map<UUID, WorldBlueprintSavedSuggestion> = emptyMap(),
    )

    @Synchronized
    fun get(worldName: String): WorldBlueprint? = readAll().worlds[worldName]

    @Synchronized
    fun getSavedSuggestion(owner: UUID, worldName: String): WorldBlueprintSavedSuggestion? =
        readAll().suggestions[owner]?.takeIf { it.suggestion.worldName == worldName }

    @Synchronized
    fun saveSuggestion(owner: UUID, suggestion: WorldBlueprintSuggestion, expectedFingerprint: String) {
        val state = readAll()
        validateSuggestion(state.worlds, suggestion, expectedFingerprint, owner.toString())
        require(owner in state.suggestions || state.suggestions.size < 64) { "Limita de 64 de sugestii salvate a fost atinsa." }
        val saved = WorldBlueprintSavedSuggestion(suggestion.copy(
            candidate = suggestion.candidate.copy(approval = null, suggestionHistory = emptyList())), System.currentTimeMillis())
        writeAll(state.copy(suggestions = state.suggestions + (owner to saved)))
    }

    @Synchronized
    fun discardSavedSuggestion(owner: UUID, worldName: String) {
        val state = readAll()
        if (state.suggestions[owner]?.suggestion?.worldName != worldName) return
        writeAll(state.copy(suggestions = state.suggestions - owner))
    }

    @Synchronized
    fun save(blueprint: WorldBlueprint) {
        val issues = WorldBlueprintPolicy.draftIssues(blueprint)
        require(issues.isEmpty()) { issues.joinToString("; ") }
        val state = readAll()
        val all = state.worlds.toMutableMap()
        require(blueprint.worldName in all || all.size < 64) { "limita de 64 de lumi a fost atinsa" }
        val previous = all[blueprint.worldName]
        val fingerprint = fingerprint(blueprint)
        val retainedApproval = previous?.approval?.takeIf { it.fingerprint == fingerprint }
        all[blueprint.worldName] = blueprint.copy(approval = retainedApproval,
            suggestionHistory = previous?.suggestionHistory ?: emptyList())
        writeAll(state.copy(worlds = all))
    }

    @Synchronized
    fun acceptSuggestion(
        suggestion: WorldBlueprintSuggestion, expectedFingerprint: String, actor: String, owner: UUID? = null,
    ): WorldBlueprintSuggestionAcceptance = acceptSuggestion(readAll(), suggestion, expectedFingerprint, actor, owner)

    @Synchronized
    fun acceptSavedSuggestion(
        owner: UUID, worldName: String, expectedFingerprint: String, actor: String,
    ): WorldBlueprintSuggestionAcceptance {
        val state = readAll()
        val saved = state.suggestions[owner]?.takeIf { it.suggestion.worldName == worldName }
            ?: throw IllegalArgumentException("Nu exista sugestie salvata pentru aceasta lume.")
        return acceptSuggestion(state, saved.suggestion, expectedFingerprint, actor, owner)
    }

    private fun acceptSuggestion(
        state: State, suggestion: WorldBlueprintSuggestion, expectedFingerprint: String, actor: String, owner: UUID?,
    ): WorldBlueprintSuggestionAcceptance {
        val acceptance = validateSuggestion(state.worlds, suggestion, expectedFingerprint, actor)
        val history = state.worlds[suggestion.worldName]?.suggestionHistory ?: emptyList()
        val updated = suggestion.candidate.copy(approval = null, suggestionHistory = (history + acceptance).takeLast(16))
        val suggestions = if (owner != null && state.suggestions[owner]?.suggestion?.worldName == suggestion.worldName)
            state.suggestions - owner else state.suggestions
        writeAll(state.copy(worlds = state.worlds + (suggestion.worldName to updated), suggestions = suggestions))
        return acceptance
    }

    private fun validateSuggestion(
        all: Map<String, WorldBlueprint>, suggestion: WorldBlueprintSuggestion, expectedFingerprint: String, actor: String,
    ): WorldBlueprintSuggestionAcceptance {
        require(validActor(actor)) { "Autorul este invalid." }
        val current = all[suggestion.worldName]
        val actualBase = current?.let(::fingerprint) ?: "none"
        require(actualBase == suggestion.baseFingerprint) {
            "Draftul s-a schimbat; cere o sugestie noua pentru revizia curenta."
        }
        require(current != null || all.size < 64) { "limita de 64 de lumi a fost atinsa" }
        val base = current ?: WorldBlueprint(suggestion.worldName)
        val candidate = suggestion.candidate
        require(candidate.worldName == suggestion.worldName) { "Lumea propunerii nu corespunde draftului." }
        val candidateFingerprint = fingerprint(candidate)
        require(expectedFingerprint == candidateFingerprint) { "Amprenta sugestiei nu corespunde preview-ului." }
        require(candidateFingerprint != fingerprint(base)) { "Sugestia nu modifica draftul." }
        val expected = when (suggestion.field) {
            WorldBlueprintSuggestionField.STORY -> base.copy(story = candidate.story)
            WorldBlueprintSuggestionField.ARCHITECTURE -> base.copy(architecture = candidate.architecture)
            WorldBlueprintSuggestionField.PROFESSION_LIMIT -> base.copy(professionTypeLimit = candidate.professionTypeLimit)
            WorldBlueprintSuggestionField.PROPERTY -> {
                val key = requireKey(suggestion.key)
                base.copy(properties = base.properties + (key to requireNotNull(candidate.properties[key])))
            }
            WorldBlueprintSuggestionField.MAP_RULE -> {
                val key = requireKey(suggestion.key)
                base.copy(mapRules = base.mapRules + (key to requireNotNull(candidate.mapRules[key])))
            }
            WorldBlueprintSuggestionField.PROFESSION -> {
                val key = requireKey(suggestion.key)
                base.copy(professions = base.professions + (key to requireNotNull(candidate.professions[key])))
            }
        }
        require(fingerprint(expected) == candidateFingerprint) { "Sugestia modifica si alte campuri ale draftului." }
        val issues = WorldBlueprintPolicy.draftIssues(candidate)
        require(issues.isEmpty()) { issues.joinToString("; ") }
        val provenance = suggestion.provenance
        val acceptance = WorldBlueprintSuggestionAcceptance(suggestion.field, suggestion.key,
            actualBase, candidateFingerprint, provenance, actor, System.currentTimeMillis())
        require(validAcceptance(acceptance)) { "Provenienta sugestiei este invalida." }
        return acceptance
    }

    @Synchronized
    fun confirm(worldName: String, expectedFingerprint: String, actor: String): WorldBlueprintApproval {
        require(validActor(actor)) { "Autorul este invalid." }
        val state = readAll()
        val all = state.worlds.toMutableMap()
        val current = all[worldName] ?: throw IllegalArgumentException("Nu exista draft pentru aceasta lume.")
        val issues = WorldBlueprintPolicy.completionIssues(current)
        require(issues.isEmpty()) { "Draft incomplet: ${issues.joinToString("; ")}" }
        val actual = fingerprint(current)
        require(expectedFingerprint == actual) { "Draftul s-a schimbat; ruleaza review si confirma noua revizie." }
        val approval = WorldBlueprintApproval(actual, actor, System.currentTimeMillis())
        all[worldName] = current.copy(approval = approval)
        writeAll(state.copy(worlds = all))
        return approval
    }

    @Synchronized
    fun revoke(worldName: String) {
        val state = readAll()
        val all = state.worlds.toMutableMap()
        val current = all[worldName] ?: throw IllegalArgumentException("Nu exista draft pentru aceasta lume.")
        if (current.approval == null) return
        all[worldName] = current.copy(approval = null)
        writeAll(state.copy(worlds = all))
    }

    fun fingerprint(blueprint: WorldBlueprint): String = MessageDigest.getInstance("SHA-256")
        .digest(encodeBlueprint(blueprint).toString().toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun writeAll(state: State) {
        val bytes = encode(state).toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= 1_048_576) { "fisierul de blueprint-uri depaseste 1 MiB" }
        Files.createDirectories(file.parent)
        val temporary = Files.createTempFile(file.parent, "world-blueprints-", ".tmp")
        try {
            Files.write(temporary, bytes)
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun readAll(): State {
        if (!Files.exists(file)) return State()
        if (Files.size(file) > 1_048_576) throw IOException("Fisierul de blueprint-uri depaseste 1 MiB")
        val raw = Files.readString(file, StandardCharsets.UTF_8)
        return try {
            decode(raw)
        } catch (error: RuntimeException) {
            throw IOException("Fisierul de blueprint-uri este invalid; nu va fi suprascris", error)
        }
    }

    private fun encode(state: State): String = JsonObject().apply {
        addProperty("version", 4)
        add("worlds", JsonArray().apply {
            state.worlds.toSortedMap().values.forEach { blueprint ->
                add(encodeBlueprint(blueprint).apply {
                    blueprint.approval?.let { approval ->
                        add("approval", JsonObject().apply {
                            addProperty("fingerprint", approval.fingerprint)
                            addProperty("approved_by", approval.approvedBy)
                            addProperty("approved_at", approval.approvedAt)
                        })
                    }
                    add("suggestion_history", JsonArray().apply {
                        blueprint.suggestionHistory.forEach { acceptance ->
                            add(JsonObject().apply {
                                addProperty("field", acceptance.field.id)
                                acceptance.key?.let { addProperty("key", it) }
                                addProperty("base_fingerprint", acceptance.baseFingerprint)
                                addProperty("accepted_fingerprint", acceptance.acceptedFingerprint)
                                addProperty("provider", acceptance.provenance.provider)
                                addProperty("model", acceptance.provenance.model)
                                addProperty("requested_at", acceptance.provenance.requestedAt)
                                addProperty("instruction_fingerprint", acceptance.provenance.instructionFingerprint)
                                addProperty("accepted_by", acceptance.acceptedBy)
                                addProperty("accepted_at", acceptance.acceptedAt)
                            })
                        }
                    })
                })
            }
        })
        add("saved_suggestions", JsonArray().apply {
            state.suggestions.toSortedMap().forEach { (owner, saved) ->
                val suggestion = saved.suggestion
                add(JsonObject().apply {
                    addProperty("owner", owner.toString())
                    addProperty("saved_at", saved.savedAt)
                    addProperty("world", suggestion.worldName)
                    addProperty("field", suggestion.field.id)
                    suggestion.key?.let { addProperty("key", it) }
                    addProperty("base_fingerprint", suggestion.baseFingerprint)
                    addProperty("candidate_fingerprint", fingerprint(suggestion.candidate))
                    addProperty("provider", suggestion.provenance.provider)
                    addProperty("model", suggestion.provenance.model)
                    addProperty("requested_at", suggestion.provenance.requestedAt)
                    addProperty("instruction_fingerprint", suggestion.provenance.instructionFingerprint)
                    add("candidate", encodeBlueprint(suggestion.candidate))
                })
            }
        })
    }.toString()

    private fun encodeBlueprint(blueprint: WorldBlueprint): JsonObject = JsonObject().apply {
        addProperty("world", blueprint.worldName)
        addProperty("story", blueprint.story)
        addProperty("profession_type_limit", blueprint.professionTypeLimit)
        addProperty("architecture", blueprint.architecture)
        add("properties", encodeStrings(blueprint.properties))
        add("map_rules", encodeStrings(blueprint.mapRules))
        add("professions", JsonObject().apply {
            blueprint.professions.toSortedMap().forEach { (key, profession) ->
                add(key, JsonObject().apply {
                    addProperty("max_npcs", profession.maxNpcs)
                    addProperty("description", profession.description)
                })
            }
        })
    }

    private fun decode(raw: String): State {
        val root = JsonParser.parseString(raw).asJsonObject
        val version = intField(root, "version")
        require(version in 1..4)
        val worlds = root.getAsJsonArray("worlds") ?: throw IllegalArgumentException("worlds lipseste")
        require(worlds.size() <= 64)
        val result = LinkedHashMap<String, WorldBlueprint>()
        for (element in worlds) {
            val value = element.asJsonObject
            val draft = decodeBlueprint(value)
            val approval = if (version >= 2 && value.has("approval")) {
                val record = value.getAsJsonObject("approval")
                WorldBlueprintApproval(
                    stringField(record, "fingerprint"),
                    stringField(record, "approved_by"),
                    longField(record, "approved_at"),
                )
            } else null
            val history = if (version >= 3) {
                val entries = value.getAsJsonArray("suggestion_history")
                    ?: throw IllegalArgumentException("suggestion_history lipseste")
                require(entries.size() <= 16)
                entries.map { entry ->
                    val record = entry.asJsonObject
                    val acceptance = WorldBlueprintSuggestionAcceptance(
                        WorldBlueprintSuggestionField.fromId(stringField(record, "field"))
                            ?: throw IllegalArgumentException("field invalid"),
                        if (record.has("key")) stringField(record, "key") else null,
                        stringField(record, "base_fingerprint"),
                        stringField(record, "accepted_fingerprint"),
                        WorldBlueprintSuggestionProvenance(
                            stringField(record, "provider"), stringField(record, "model"),
                            longField(record, "requested_at"), stringField(record, "instruction_fingerprint"),
                        ),
                        stringField(record, "accepted_by"), longField(record, "accepted_at"),
                    )
                    require(validAcceptance(acceptance))
                    acceptance
                }
            } else emptyList()
            val blueprint = draft.copy(approval = approval, suggestionHistory = history)
            require(WorldBlueprintPolicy.draftIssues(blueprint).isEmpty())
            if (approval != null) {
                require(WorldBlueprintPolicy.completionIssues(blueprint).isEmpty())
                require(approval.fingerprint == fingerprint(blueprint))
                require(validActor(approval.approvedBy) && approval.approvedAt > 0)
            }
            require(result.putIfAbsent(blueprint.worldName, blueprint) == null)
        }
        val suggestions = LinkedHashMap<UUID, WorldBlueprintSavedSuggestion>()
        if (version >= 4) {
            val entries = root.getAsJsonArray("saved_suggestions")
                ?: throw IllegalArgumentException("saved_suggestions lipseste")
            require(entries.size() <= 64)
            entries.forEach { entry ->
                val record = entry.asJsonObject
                val ownerText = stringField(record, "owner")
                val owner = UUID.fromString(ownerText)
                require(owner.toString() == ownerText)
                val savedAt = longField(record, "saved_at")
                val candidate = decodeBlueprint(record.getAsJsonObject("candidate"))
                val suggestion = WorldBlueprintSuggestion(
                    stringField(record, "world"),
                    WorldBlueprintSuggestionField.fromId(stringField(record, "field"))
                        ?: throw IllegalArgumentException("field invalid"),
                    if (record.has("key")) stringField(record, "key") else null,
                    stringField(record, "base_fingerprint"), candidate,
                    WorldBlueprintSuggestionProvenance(
                        stringField(record, "provider"), stringField(record, "model"),
                        longField(record, "requested_at"), stringField(record, "instruction_fingerprint"),
                    ),
                )
                require(candidate.worldName == suggestion.worldName)
                val candidateFingerprint = stringField(record, "candidate_fingerprint")
                require(fingerprint(candidate) == candidateFingerprint)
                require(validAcceptance(WorldBlueprintSuggestionAcceptance(suggestion.field, suggestion.key,
                    suggestion.baseFingerprint, candidateFingerprint, suggestion.provenance, ownerText, savedAt)))
                when (suggestion.field) {
                    WorldBlueprintSuggestionField.PROPERTY -> require(suggestion.key in candidate.properties)
                    WorldBlueprintSuggestionField.PROFESSION -> require(suggestion.key in candidate.professions)
                    WorldBlueprintSuggestionField.MAP_RULE -> require(suggestion.key in candidate.mapRules)
                    else -> Unit
                }
                require(suggestions.putIfAbsent(owner, WorldBlueprintSavedSuggestion(suggestion, savedAt)) == null)
            }
        }
        return State(result, suggestions)
    }

    private fun decodeBlueprint(value: JsonObject): WorldBlueprint {
        val professions = LinkedHashMap<String, WorldProfessionBlueprint>()
        for ((key, entry) in value.getAsJsonObject("professions").entrySet()) {
            professions[key] = WorldProfessionBlueprint(
                intField(entry.asJsonObject, "max_npcs"), stringField(entry.asJsonObject, "description"),
            )
        }
        val blueprint = WorldBlueprint(
            stringField(value, "world"), stringField(value, "story"),
            decodeStrings(value.getAsJsonObject("properties")), intField(value, "profession_type_limit"),
            professions, stringField(value, "architecture"), decodeStrings(value.getAsJsonObject("map_rules")),
        )
        require(WorldBlueprintPolicy.draftIssues(blueprint).isEmpty())
        return blueprint
    }

    private fun encodeStrings(values: Map<String, String>): JsonObject = JsonObject().apply {
        values.toSortedMap().forEach { (key, value) -> addProperty(key, value) }
    }

    private fun decodeStrings(json: JsonObject): Map<String, String> =
        json.entrySet().associate { (key, value) ->
            require(value.isJsonPrimitive && value.asJsonPrimitive.isString)
            key to value.asString
        }

    private fun stringField(json: JsonObject, key: String): String {
        val value = json.get(key) ?: throw IllegalArgumentException("$key lipseste")
        require(value.isJsonPrimitive && value.asJsonPrimitive.isString)
        return value.asString
    }

    private fun intField(json: JsonObject, key: String): Int {
        val value = json.get(key) ?: throw IllegalArgumentException("$key lipseste")
        require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber)
        return value.asString.toIntOrNull() ?: throw IllegalArgumentException("$key este invalid")
    }

    private fun longField(json: JsonObject, key: String): Long {
        val value = json.get(key) ?: throw IllegalArgumentException("$key lipseste")
        require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber)
        return value.asString.toLongOrNull() ?: throw IllegalArgumentException("$key este invalid")
    }

    private fun requireKey(key: String?): String {
        require(key != null && WorldBlueprintPolicy.validKey(key)) { "Cheia sugestiei este invalida." }
        return key
    }

    private fun validActor(actor: String): Boolean =
        actor.isNotBlank() && actor.length <= 64 && actor.none { it.isISOControl() }

    private fun validAcceptance(acceptance: WorldBlueprintSuggestionAcceptance): Boolean {
        val keyed = acceptance.field in setOf(WorldBlueprintSuggestionField.PROPERTY,
            WorldBlueprintSuggestionField.PROFESSION, WorldBlueprintSuggestionField.MAP_RULE)
        val provenance = acceptance.provenance
        val hash = Regex("[a-f0-9]{64}")
        return (if (keyed) acceptance.key != null && WorldBlueprintPolicy.validKey(acceptance.key)
            else acceptance.key == null) &&
            (acceptance.baseFingerprint == "none" || hash.matches(acceptance.baseFingerprint)) &&
            hash.matches(acceptance.acceptedFingerprint) &&
            hash.matches(provenance.instructionFingerprint) &&
            provenance.provider in setOf("openai", "ollama") &&
            provenance.model.isNotBlank() && provenance.model.length <= 80 &&
            provenance.model.none { it.isISOControl() } && provenance.requestedAt > 0 &&
            validActor(acceptance.acceptedBy) && acceptance.acceptedAt >= provenance.requestedAt
    }
}
