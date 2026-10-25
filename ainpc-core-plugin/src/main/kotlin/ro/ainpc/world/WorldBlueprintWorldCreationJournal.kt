package ro.ainpc.world

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.World
import org.bukkit.WorldType
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.UUID

internal enum class WorldBlueprintWorldCreationStatus {
    PENDING,
    CREATED,
    FAILED,
}

internal data class WorldBlueprintWorldCreationOptions(
    val seed: Long? = null,
    val worldType: WorldType? = null,
    val environment: World.Environment? = null,
    val generateStructures: Boolean? = null,
)

internal data class WorldBlueprintWorldCreationAttempt(
    val id: UUID,
    val worldName: String,
    val fingerprint: String,
    val actor: String,
    val startedAt: Long,
    val status: WorldBlueprintWorldCreationStatus,
    val finishedAt: Long? = null,
    val options: WorldBlueprintWorldCreationOptions? = null,
    val retryOf: UUID? = null,
)

internal data class WorldBlueprintWorldCreationPage(
    val page: Int,
    val pageCount: Int,
    val total: Int,
    val attempts: List<WorldBlueprintWorldCreationAttempt>,
)

internal class WorldBlueprintWorldCreationJournal(private val file: Path) {
    @Synchronized
    fun begin(worldName: String, fingerprint: String, actor: String,
              options: WorldBlueprintWorldCreationOptions, retryOf: UUID? = null): WorldBlueprintWorldCreationAttempt {
        require(validWorldName(worldName) && validActor(actor) && validFingerprint(fingerprint) && validOptions(options)) {
            "Datele incercarii de creare sunt invalide."
        }
        val attempts = readAll()
        val latest = attempts.lastOrNull { it.worldName == worldName }
        if (latest?.status == WorldBlueprintWorldCreationStatus.PENDING) {
            require(retryOf == latest.id) {
                "Incercarea ${latest.id} este PENDING; verifica creation verify si repeta explicit cu retry ${latest.id}."
            }
        } else {
            require(retryOf == null) { "Nu exista o incercare PENDING recenta pentru acest retry." }
        }
        require(attempts.size < 1_024) { "Jurnalul de creare a lumilor a atins limita de 1024 incercari." }
        val attempt = WorldBlueprintWorldCreationAttempt(UUID.randomUUID(), worldName, fingerprint,
            actor, System.currentTimeMillis(), WorldBlueprintWorldCreationStatus.PENDING,
            options = options, retryOf = retryOf)
        writeAll(attempts + attempt)
        return attempt
    }

    @Synchronized
    fun finish(id: UUID, status: WorldBlueprintWorldCreationStatus) {
        require(status != WorldBlueprintWorldCreationStatus.PENDING) { "Rezultatul final al crearii este invalid." }
        val attempts = readAll().toMutableList()
        val index = attempts.indexOfFirst { it.id == id }
        require(index >= 0 && attempts[index].status == WorldBlueprintWorldCreationStatus.PENDING) {
            "Incercarea de creare nu exista sau este deja finalizata."
        }
        attempts[index] = attempts[index].copy(status = status,
            finishedAt = maxOf(System.currentTimeMillis(), attempts[index].startedAt))
        writeAll(attempts)
    }

    @Synchronized
    fun latest(worldName: String, limit: Int = 5): List<WorldBlueprintWorldCreationAttempt> {
        require(limit in 1..20) { "Limita jurnalului este invalida." }
        return readAll().asReversed().filter { it.worldName == worldName }.take(limit)
    }

    @Synchronized
    fun find(worldName: String, id: UUID): WorldBlueprintWorldCreationAttempt? =
        readAll().firstOrNull { it.worldName == worldName && it.id == id }

    @Synchronized
    fun page(worldName: String, page: Int, pageSize: Int = 5): WorldBlueprintWorldCreationPage {
        require(page in 1..1_024 && pageSize in 1..20) { "Pagina jurnalului este invalida." }
        val matching = readAll().asReversed().filter { it.worldName == worldName }
        val pageCount = maxOf(1, (matching.size + pageSize - 1) / pageSize)
        val attempts = if (page > pageCount) emptyList() else matching.drop((page - 1) * pageSize).take(pageSize)
        return WorldBlueprintWorldCreationPage(page, pageCount, matching.size, attempts)
    }

    private fun readAll(): List<WorldBlueprintWorldCreationAttempt> {
        if (!Files.exists(file)) return emptyList()
        if (Files.size(file) > 1_048_576) throw IOException("Jurnalul de creare depaseste 1 MiB.")
        return try {
            val root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).asJsonObject
            require(root.keySet() == setOf("version", "attempts"))
            val version = root.get("version").asInt
            require(version in 1..3)
            val entries = root.getAsJsonArray("attempts")
            require(entries.size() <= 1_024)
            val attempts = entries.map { element ->
                val entry = element.asJsonObject
                val expectedKeys = setOf("id", "world_name", "fingerprint", "actor", "started_at", "status", "finished_at")
                require(entry.keySet() == when (version) {
                    1 -> expectedKeys
                    2 -> expectedKeys + "options"
                    else -> expectedKeys + setOf("options", "retry_of")
                })
                WorldBlueprintWorldCreationAttempt(
                    UUID.fromString(entry.get("id").asString),
                    entry.get("world_name").asString,
                    entry.get("fingerprint").asString,
                    entry.get("actor").asString,
                    entry.get("started_at").asLong,
                    WorldBlueprintWorldCreationStatus.valueOf(entry.get("status").asString),
                    entry.get("finished_at").takeUnless { it.isJsonNull }?.asLong,
                    if (version == 1) null else readOptions(entry.get("options")),
                    if (version < 3) null else entry.get("retry_of").takeUnless { it.isJsonNull }
                        ?.asString?.let(UUID::fromString),
                ).also { attempt -> require(validAttempt(attempt)) }
            }
            require(attempts.map { it.id }.toSet().size == attempts.size)
            attempts.forEachIndexed { index, attempt ->
                val retryOf = attempt.retryOf ?: return@forEachIndexed
                val previous = attempts.subList(0, index).lastOrNull { it.worldName == attempt.worldName }
                require(previous?.id == retryOf && previous.status == WorldBlueprintWorldCreationStatus.PENDING)
            }
            attempts
        } catch (error: RuntimeException) {
            throw IOException("Jurnalul de creare este invalid; nu va fi suprascris.", error)
        }
    }

    private fun readOptions(value: JsonElement): WorldBlueprintWorldCreationOptions? {
        if (value.isJsonNull) return null
        val options = value.asJsonObject
        require(options.keySet() == setOf("world_seed", "world_type", "world_environment", "world_generate_structures"))
        val seed = options.get("world_seed").takeUnless { it.isJsonNull }?.also {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber)
        }?.asString?.let { requireNotNull(it.toLongOrNull()) }
        val worldType = options.get("world_type").takeUnless { it.isJsonNull }?.also {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString)
        }?.asString?.let { WorldType.valueOf(it.uppercase(Locale.ROOT)) }
        val environment = options.get("world_environment").takeUnless { it.isJsonNull }?.also {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString)
        }?.asString?.let { World.Environment.valueOf(it.uppercase(Locale.ROOT)) }
        val structures = options.get("world_generate_structures").takeUnless { it.isJsonNull }?.also {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isBoolean)
        }?.asBoolean
        return WorldBlueprintWorldCreationOptions(seed, worldType, environment, structures).also {
            require(validOptions(it))
        }
    }

    private fun writeAll(attempts: List<WorldBlueprintWorldCreationAttempt>) {
        val root = JsonObject().apply {
            addProperty("version", 3)
            add("attempts", JsonArray().apply {
                attempts.forEach { attempt ->
                    add(JsonObject().apply {
                        addProperty("id", attempt.id.toString())
                        addProperty("world_name", attempt.worldName)
                        addProperty("fingerprint", attempt.fingerprint)
                        addProperty("actor", attempt.actor)
                        addProperty("started_at", attempt.startedAt)
                        addProperty("status", attempt.status.name)
                        if (attempt.finishedAt == null) add("finished_at", null)
                        else addProperty("finished_at", attempt.finishedAt)
                        add("options", attempt.options?.let { options -> JsonObject().apply {
                            addProperty("world_seed", options.seed)
                            addProperty("world_type", options.worldType?.name?.lowercase(Locale.ROOT))
                            addProperty("world_environment", options.environment?.name?.lowercase(Locale.ROOT))
                            addProperty("world_generate_structures", options.generateStructures)
                        } })
                        addProperty("retry_of", attempt.retryOf?.toString())
                    })
                }
            })
        }
        val bytes = root.toString().toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= 1_048_576) { "Jurnalul de creare depaseste 1 MiB." }
        Files.createDirectories(file.parent)
        val temporary = Files.createTempFile(file.parent, "world-blueprint-creations-", ".tmp")
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

    private fun validAttempt(attempt: WorldBlueprintWorldCreationAttempt): Boolean =
        validWorldName(attempt.worldName) && validActor(attempt.actor) && validFingerprint(attempt.fingerprint) &&
            attempt.retryOf != attempt.id &&
            (attempt.options == null || validOptions(attempt.options)) &&
            attempt.startedAt > 0 && when (attempt.status) {
                WorldBlueprintWorldCreationStatus.PENDING -> attempt.finishedAt == null
                else -> attempt.finishedAt != null && attempt.finishedAt >= attempt.startedAt
            }

    private fun validOptions(options: WorldBlueprintWorldCreationOptions): Boolean =
        (options.worldType == null || options.worldType in setOf(WorldType.NORMAL, WorldType.FLAT,
            WorldType.LARGE_BIOMES, WorldType.AMPLIFIED)) &&
            (options.environment == null || options.environment in setOf(World.Environment.NORMAL,
                World.Environment.NETHER, World.Environment.THE_END)) &&
            (options.environment == null || options.environment == World.Environment.NORMAL || options.worldType == null)

    private fun validWorldName(value: String): Boolean =
        value.isNotBlank() && value.length <= 128 && value.none { it.isISOControl() }

    private fun validActor(value: String): Boolean =
        value.isNotBlank() && value.length <= 128 && value.none { it.isISOControl() }

    private fun validFingerprint(value: String): Boolean = value.matches(Regex("[a-f0-9]{64}"))
}
