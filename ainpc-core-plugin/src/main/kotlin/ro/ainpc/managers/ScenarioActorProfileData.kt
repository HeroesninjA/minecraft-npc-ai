package ro.ainpc.managers

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcEntityKind
import ro.ainpc.npc.NpcInteractionProfile
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.npc.NpcSimulationMode
import java.util.UUID

internal object ScenarioActorProfileData {
    enum class RestartDisposition {
        LOAD,
        QUARANTINE,
    }

    data class Metadata(
        val lifecycleType: NpcLifecycleType,
        val persistenceMode: NpcPersistenceMode,
        val simulationMode: NpcSimulationMode,
        val interactionProfile: NpcInteractionProfile,
        val entityKind: NpcEntityKind,
        val entityArchetype: String,
        val ownerScenarioId: String?,
        val scenarioSessionId: UUID?,
        val ownerQuestId: String?,
        val spawnSource: String?,
        val despawnRule: String?,
        val durationSeconds: Long?,
        val temporaryTags: Set<String>,
    ) {
        fun applyTo(npc: AINPC) {
            npc.lifecycleType = lifecycleType
            npc.persistenceMode = persistenceMode
            npc.simulationMode = simulationMode
            npc.interactionProfile = interactionProfile
            npc.entityKind = entityKind
            npc.entityArchetype = entityArchetype
            npc.ownerScenarioId = ownerScenarioId
            npc.scenarioSessionId = scenarioSessionId
            npc.ownerQuestId = ownerQuestId
            npc.spawnSource = spawnSource
            npc.despawnRule = despawnRule
            npc.durationSeconds = durationSeconds
            npc.temporaryTags.clear()
            npc.temporaryTags.addAll(temporaryTags)
        }
    }

    fun write(profile: JsonObject, npc: AINPC) {
        if (!isScenarioActor(npc.profileSource)) return

        val actor = JsonObject()
        actor.addProperty("version", 1)
        actor.addProperty("lifecycle_type", npc.lifecycleType.name)
        actor.addProperty("persistence_mode", npc.persistenceMode.name)
        actor.addProperty("simulation_mode", npc.simulationMode.name)
        actor.addProperty("interaction_profile", npc.interactionProfile.name)
        actor.addProperty("entity_kind", npc.entityKind.name)
        actor.addProperty("entity_archetype", npc.entityArchetype)
        actor.addProperty("owner_scenario_id", npc.ownerScenarioId)
        npc.scenarioSessionId?.let { actor.addProperty("scenario_session_id", it.toString()) }
        actor.addProperty("owner_quest_id", npc.ownerQuestId)
        actor.addProperty("spawn_source", npc.spawnSource)
        actor.addProperty("despawn_rule", npc.despawnRule)
        actor.addProperty("duration_seconds", npc.durationSeconds)
        val tags = JsonArray()
        npc.temporaryTags.sorted().forEach(tags::add)
        actor.add("temporary_tags", tags)
        profile.add("scenario_actor", actor)
    }

    fun read(profileData: String?, gson: Gson): Metadata? = try {
        if (profileData.isNullOrBlank()) null
        else gson.fromJson(profileData, JsonObject::class.java)?.let(::read)
    } catch (_: RuntimeException) {
        null
    }

    fun read(profile: JsonObject): Metadata? {
        val actor = profile.get("scenario_actor")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val version = actor.get("version")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            ?.asString ?: return null
        if (version != "1") return null
        val lifecycleType = enumField(actor, "lifecycle_type", NpcLifecycleType.entries) ?: return null
        val persistenceMode = enumField(actor, "persistence_mode", NpcPersistenceMode.entries) ?: return null
        val simulationMode = enumField(actor, "simulation_mode", NpcSimulationMode.entries) ?: return null
        val interactionProfile = enumField(actor, "interaction_profile", NpcInteractionProfile.entries) ?: return null
        val entityKind = enumField(actor, "entity_kind", NpcEntityKind.entries) ?: return null
        val duration = actor.get("duration_seconds")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            ?.asLong
        val tags = actor.get("temporary_tags")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.mapNotNull { it.takeIf { tag -> tag.isJsonPrimitive && tag.asJsonPrimitive.isString }?.asString }
            ?.toSet() ?: emptySet()
        val sessionId = if (actor.has("scenario_session_id")) {
            val raw = stringField(actor, "scenario_session_id") ?: return null
            runCatching { UUID.fromString(raw) }.getOrNull()
                ?.takeIf { it.toString().equals(raw, ignoreCase = true) } ?: return null
        } else null

        return Metadata(
            lifecycleType,
            persistenceMode,
            simulationMode,
            interactionProfile,
            entityKind,
            stringField(actor, "entity_archetype") ?: "villager",
            stringField(actor, "owner_scenario_id"),
            sessionId,
            stringField(actor, "owner_quest_id"),
            stringField(actor, "spawn_source"),
            stringField(actor, "despawn_rule"),
            duration,
            tags,
        )
    }

    fun restartDisposition(profileSource: String?, profileData: String?, gson: Gson): RestartDisposition {
        if (!isScenarioActor(profileSource)) return RestartDisposition.LOAD
        val metadata = read(profileData, gson)
        return if (metadata?.lifecycleType == NpcLifecycleType.PERMANENT &&
            metadata.persistenceMode != NpcPersistenceMode.RUNTIME_ONLY
        ) RestartDisposition.LOAD else RestartDisposition.QUARANTINE
    }

    private fun isScenarioActor(profileSource: String?): Boolean =
        profileSource.equals("scenario_actor", ignoreCase = true)

    private fun <T : Enum<T>> enumField(actor: JsonObject, key: String, values: List<T>): T? {
        val raw = stringField(actor, key) ?: return null
        return values.firstOrNull { it.name == raw }
    }

    private fun stringField(actor: JsonObject, key: String): String? =
        actor.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
}
