package ro.ainpc.engine

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import ro.ainpc.npc.NpcPersistenceMode
import java.util.UUID

internal data class ScenarioSessionSnapshot(
    val version: Int,
    val scenarioId: UUID,
    val templateId: String,
    val phase: String,
    val startedAt: Long,
    val anchor: Anchor,
    val npcRoles: Map<UUID, String>,
    val playerRoles: Map<UUID, String>,
    val actors: Map<String, Actor>,
    val templateFingerprint: String? = null,
    val retiredActorIds: Set<String> = emptySet(),
) {
    data class Anchor(val world: String, val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float)
    data class Actor(val uuid: UUID, val databaseId: Int?, val expiresAt: Long? = null)
}

internal object ScenarioSessionSnapshotCodec {
    const val VERSION = 1

    fun capture(scenario: ActiveScenario, actorDatabaseId: (UUID) -> Int?): ScenarioSessionSnapshot? {
        val location = scenario.anchorLocation ?: return null
        val world = location.world?.name ?: return null
        val snapshot = ScenarioSessionSnapshot(
            VERSION,
            scenario.id,
            scenario.templateId,
            scenario.currentPhase,
            scenario.startTime,
            ScenarioSessionSnapshot.Anchor(world, location.x, location.y, location.z, location.yaw, location.pitch),
            scenario.npcRoles.toMap(),
            scenario.playerRoles.toMap(),
            scenario.spawnedActors.mapValues { (actorId, uuid) ->
                ScenarioSessionSnapshot.Actor(uuid, actorDatabaseId(uuid)?.takeIf { it > 0 },
                    scenario.actorExpirationTimes[actorId])
            },
            scenario.recoveryFingerprint,
            scenario.retiredActorIds.toSet(),
        )
        return snapshot.takeIf { candidate ->
            isStructurallyValid(candidate) && candidate.actors.none { (actorId, actor) ->
                scenario.actors[actorId]?.persistenceMode != NpcPersistenceMode.RUNTIME_ONLY && actor.databaseId == null
            }
        }
    }

    fun encode(snapshot: ScenarioSessionSnapshot): String {
        require(isStructurallyValid(snapshot))
        val root = JsonObject()
        root.addProperty("version", snapshot.version)
        root.addProperty("scenario_id", snapshot.scenarioId.toString())
        root.addProperty("template_id", snapshot.templateId)
        root.addProperty("phase", snapshot.phase)
        root.addProperty("started_at", snapshot.startedAt)
        snapshot.templateFingerprint?.let { root.addProperty("template_fingerprint", it) }
        root.add("anchor", JsonObject().apply {
            addProperty("world", snapshot.anchor.world)
            addProperty("x", snapshot.anchor.x)
            addProperty("y", snapshot.anchor.y)
            addProperty("z", snapshot.anchor.z)
            addProperty("yaw", snapshot.anchor.yaw)
            addProperty("pitch", snapshot.anchor.pitch)
        })
        root.add("npc_roles", encodeRoles(snapshot.npcRoles))
        root.add("player_roles", encodeRoles(snapshot.playerRoles))
        root.add("actors", JsonObject().apply {
            snapshot.actors.toSortedMap().forEach { (actorId, actor) ->
                add(actorId, JsonObject().apply {
                    addProperty("uuid", actor.uuid.toString())
                    actor.databaseId?.let { addProperty("database_id", it) }
                    actor.expiresAt?.let { addProperty("expires_at", it) }
                })
            }
        })
        if (snapshot.retiredActorIds.isNotEmpty()) {
            root.add("retired_actor_ids", JsonArray().apply {
                snapshot.retiredActorIds.sorted().forEach(::add)
            })
        }
        return root.toString()
    }

    fun decode(raw: String): ScenarioSessionSnapshot? = try {
        if (raw.length > 131_072) return null
        val root = JsonParser.parseString(raw).asJsonObject
        val anchor = root.get("anchor")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val npcRoles = decodeRoles(root.get("npc_roles")?.takeIf { it.isJsonObject }?.asJsonObject) ?: return null
        val playerRoles = decodeRoles(root.get("player_roles")?.takeIf { it.isJsonObject }?.asJsonObject) ?: return null
        val actorJson = root.get("actors")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val actors = LinkedHashMap<String, ScenarioSessionSnapshot.Actor>()
        for ((actorId, value) in actorJson.entrySet()) {
            if (!value.isJsonObject) return null
            val actor = value.asJsonObject
            val uuid = uuidField(actor, "uuid") ?: return null
            val databaseId = if (actor.has("database_id")) intField(actor, "database_id") ?: return null else null
            val expiresAt = if (actor.has("expires_at")) longField(actor, "expires_at") ?: return null else null
            actors[actorId] = ScenarioSessionSnapshot.Actor(uuid, databaseId, expiresAt)
        }
        val retiredActorIds = LinkedHashSet<String>()
        if (root.has("retired_actor_ids")) {
            val retiredJson = root.get("retired_actor_ids")?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
            for (value in retiredJson) {
                val actorId = value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: return null
                if (!retiredActorIds.add(actorId)) return null
            }
        }
        ScenarioSessionSnapshot(
            intField(root, "version") ?: return null,
            uuidField(root, "scenario_id") ?: return null,
            stringField(root, "template_id") ?: return null,
            stringField(root, "phase") ?: return null,
            longField(root, "started_at") ?: return null,
            ScenarioSessionSnapshot.Anchor(
                stringField(anchor, "world") ?: return null,
                doubleField(anchor, "x") ?: return null,
                doubleField(anchor, "y") ?: return null,
                doubleField(anchor, "z") ?: return null,
                doubleField(anchor, "yaw")?.toFloat() ?: return null,
                doubleField(anchor, "pitch")?.toFloat() ?: return null,
            ),
            npcRoles,
            playerRoles,
            actors,
            if (root.has("template_fingerprint"))
                stringField(root, "template_fingerprint") ?: return null
            else null,
            retiredActorIds,
        ).takeIf(::isStructurallyValid)
    } catch (_: RuntimeException) {
        null
    }

    fun templateIssues(snapshot: ScenarioSessionSnapshot, template: ScenarioTemplate): List<String> = buildList {
        if (!snapshot.templateId.equals(template.templateId, ignoreCase = true)) add("template_id diferit")
        if (snapshot.phase !in template.phases) add("faza absenta din template")
        if (snapshot.actors.keys.any { it !in template.actors }) add("actor absent din template")
        if (snapshot.retiredActorIds.any { it !in template.actors }) add("actor retras absent din template")
        if (snapshot.actors.values.any { it.databaseId == null }) add("actor fara rand DB")
        if (snapshot.templateFingerprint != null &&
            snapshot.templateFingerprint != ScenarioTemplateRecoveryFingerprint.compute(template)
        ) add("amprenta template diferita")
    }

    internal fun isStructurallyValid(snapshot: ScenarioSessionSnapshot): Boolean =
        snapshot.version == VERSION && snapshot.templateId.isNotBlank() && snapshot.templateId.length <= 128 &&
            snapshot.phase.isNotBlank() && snapshot.phase.length <= 128 && snapshot.startedAt > 0 &&
            snapshot.anchor.world.isNotBlank() && snapshot.anchor.world.length <= 128 &&
            listOf(snapshot.anchor.x, snapshot.anchor.y, snapshot.anchor.z).all(Double::isFinite) &&
            snapshot.anchor.yaw.isFinite() && snapshot.anchor.pitch.isFinite() &&
            snapshot.npcRoles.size <= 256 && snapshot.playerRoles.size <= 256 &&
            snapshot.actors.size + snapshot.retiredActorIds.size <= 256 &&
            snapshot.npcRoles.values.all { it.isNotBlank() && it.length <= 128 } &&
            snapshot.playerRoles.values.all { it.isNotBlank() && it.length <= 128 } &&
            snapshot.actors.keys.all { it.isNotBlank() && it.length <= 128 } &&
            snapshot.retiredActorIds.all { it.isNotBlank() && it.length <= 128 && it !in snapshot.actors } &&
            snapshot.actors.values.all { it.databaseId == null || it.databaseId > 0 } &&
            snapshot.actors.values.all { it.expiresAt == null || it.expiresAt >= snapshot.startedAt } &&
            snapshot.actors.values.map { it.uuid }.toSet().size == snapshot.actors.size &&
            (snapshot.templateFingerprint == null || snapshot.templateFingerprint.matches(Regex("[0-9a-f]{64}")))

    private fun encodeRoles(roles: Map<UUID, String>): JsonObject = JsonObject().apply {
        roles.toSortedMap(compareBy(UUID::toString)).forEach { (uuid, role) -> addProperty(uuid.toString(), role) }
    }

    private fun decodeRoles(json: JsonObject?): Map<UUID, String>? {
        if (json == null) return null
        val roles = LinkedHashMap<UUID, String>()
        for ((key, value) in json.entrySet()) {
            val uuid = parseUuid(key) ?: return null
            val role = value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: return null
            roles[uuid] = role
        }
        return roles
    }

    private fun stringField(json: JsonObject, key: String): String? =
        json.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun intField(json: JsonObject, key: String): Int? =
        json.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asString?.toIntOrNull()

    private fun longField(json: JsonObject, key: String): Long? =
        json.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asString?.toLongOrNull()

    private fun doubleField(json: JsonObject, key: String): Double? =
        json.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asString?.toDoubleOrNull()
            ?.takeIf(Double::isFinite)

    private fun uuidField(json: JsonObject, key: String): UUID? = stringField(json, key)?.let(::parseUuid)

    private fun parseUuid(value: String): UUID? = runCatching { UUID.fromString(value) }
        .getOrNull()?.takeIf { it.toString().equals(value, ignoreCase = true) }
}
