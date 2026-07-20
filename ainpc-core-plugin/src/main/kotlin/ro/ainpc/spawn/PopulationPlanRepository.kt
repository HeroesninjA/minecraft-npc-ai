package ro.ainpc.spawn

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.UUID

internal class PopulationPlanRepository(
    private val directory: Path,
    private val clock: () -> Long = System::currentTimeMillis,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
) {
    private val lock = Any()

    fun save(plan: PopulationPlan): StoredPlan = synchronized(lock) {
        validatePlan(plan)
        Files.createDirectories(directory)
        val target = resolvePlanPath(plan.planId)
        val existing = if (Files.isRegularFile(target)) readStoredPlan(target) else null
        require(existing == null || existing.plan.regionId == plan.regionId) {
            "Plan ID '${plan.planId}' este deja asociat regiunii '${existing?.plan?.regionId}'."
        }
        require(existing == null || existing.plan.seed == plan.seed) {
            "Coliziune plan ID '${plan.planId}': seed-ul existent este diferit."
        }
        val now = clock()
        val stored = StoredPlan(
            plan = plan,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        writeJson(target, toJson(stored))
        stored
    }

    fun find(planId: String): StoredPlan? = synchronized(lock) {
        val target = resolvePlanPath(planId)
        if (Files.isRegularFile(target)) readStoredPlan(target) else null
    }

    fun list(regionId: String? = null): List<StoredPlan> = synchronized(lock) {
        if (!Files.isDirectory(directory)) return@synchronized emptyList()
        val regionFilter = regionId?.trim()?.takeIf { it.isNotEmpty() }
        val storedPlans = Files.list(directory).use { paths ->
            paths
                .filter { path -> Files.isRegularFile(path) && path.fileName.toString().endsWith(PLAN_FILE_SUFFIX) }
                .map(::readStoredPlan)
                .toList()
        }
        storedPlans
            .filter { stored -> regionFilter == null || stored.plan.regionId == regionFilter }
            .sortedWith(compareByDescending<StoredPlan> { it.updatedAt }.thenBy { it.plan.planId })
    }

    fun select(planId: String): StoredPlan? = synchronized(lock) {
        val target = resolvePlanPath(planId)
        if (!Files.isRegularFile(target)) return@synchronized null
        val stored = readStoredPlan(target)
        val selections = readSelections()
        val entry = JsonObject().apply {
            addProperty("plan_id", stored.plan.planId)
            addProperty("selected_at", clock())
        }
        selections.getAsJsonObject("regions").add(stored.plan.regionId, entry)
        writeJson(directory.resolve(SELECTION_FILE_NAME), selections)
        stored
    }

    fun selectedPlanId(regionId: String): String? = synchronized(lock) {
        val normalizedRegionId = regionId.trim()
        require(normalizedRegionId.isNotEmpty()) { "Region ID nu poate fi gol." }
        val selectionPath = directory.resolve(SELECTION_FILE_NAME)
        if (!Files.isRegularFile(selectionPath)) return@synchronized null
        val entry = readSelections().getAsJsonObject("regions").getAsJsonObject(normalizedRegionId)
            ?: return@synchronized null
        val planId = entry.get("plan_id")?.takeIf { it.isJsonPrimitive }?.asString
            ?: return@synchronized null
        val planPath = runCatching { resolvePlanPath(planId) }.getOrNull() ?: return@synchronized null
        if (!Files.isRegularFile(planPath)) return@synchronized null
        val stored = readStoredPlan(planPath)
        if (stored.plan.regionId == normalizedRegionId) stored.plan.planId else null
    }

    private fun resolvePlanPath(planId: String): Path {
        require(PLAN_ID_PATTERN.matches(planId)) {
            "Plan ID invalid: foloseste 1-200 caractere alfanumerice, '.', '_' sau '-'."
        }
        return directory.resolve("$planId$PLAN_FILE_SUFFIX")
    }

    private fun validatePlan(plan: PopulationPlan) {
        resolvePlanPath(plan.planId)
        require(plan.regionId.isNotBlank()) { "Region ID nu poate fi gol." }
        require(plan.seed.isNotBlank()) { "Seed-ul planului nu poate fi gol." }
        require(plan.targetPopulation >= 0) { "Populatia tinta nu poate fi negativa." }
        require(plan.isValid()) { "Planul trebuie sa contina cel putin un household." }
    }

    private fun toJson(stored: StoredPlan): JsonObject = JsonObject().apply {
        addProperty("schema_version", SCHEMA_VERSION)
        addProperty("plan_id", stored.plan.planId)
        addProperty("region_id", stored.plan.regionId)
        addProperty("created_at", stored.createdAt)
        addProperty("updated_at", stored.updatedAt)
        add("plan", gson.toJsonTree(stored.plan))
    }

    private fun readStoredPlan(path: Path): StoredPlan {
        try {
            val root = readJsonObject(path)
            requireSchema(root, path)
            val envelopePlanId = requiredString(root, "plan_id", path)
            val envelopeRegionId = requiredString(root, "region_id", path)
            val createdAt = requiredLong(root, "created_at", path)
            val updatedAt = requiredLong(root, "updated_at", path)
            val planElement = root.get("plan") ?: throw IOException("Campul 'plan' lipseste din $path.")
            val plan = gson.fromJson(planElement, PopulationPlan::class.java)
                ?: throw IOException("Planul din $path este null.")
            validatePlan(plan)
            if (envelopePlanId != plan.planId || envelopeRegionId != plan.regionId) {
                throw IOException("Metadata planului din $path nu corespunde payload-ului.")
            }
            if (createdAt < 0 || updatedAt < createdAt) {
                throw IOException("Timestamp-uri invalide in $path.")
            }
            return StoredPlan(plan, createdAt, updatedAt)
        } catch (exception: IOException) {
            throw exception
        } catch (exception: Exception) {
            throw IOException("PopulationPlan invalid in $path: ${exception.message}", exception)
        }
    }

    private fun readSelections(): JsonObject {
        val path = directory.resolve(SELECTION_FILE_NAME)
        if (!Files.isRegularFile(path)) {
            return JsonObject().apply {
                addProperty("schema_version", SCHEMA_VERSION)
                add("regions", JsonObject())
            }
        }
        try {
            val root = readJsonObject(path)
            requireSchema(root, path)
            if (root.get("regions")?.isJsonObject != true) {
                throw IOException("Campul 'regions' lipseste sau este invalid in $path.")
            }
            return root
        } catch (exception: IOException) {
            throw exception
        } catch (exception: Exception) {
            throw IOException("Index de selectie invalid in $path: ${exception.message}", exception)
        }
    }

    private fun readJsonObject(path: Path): JsonObject {
        Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
            val element = JsonParser.parseReader(reader)
            if (!element.isJsonObject) throw IOException("Documentul $path nu este un obiect JSON.")
            return element.asJsonObject
        }
    }

    private fun requireSchema(root: JsonObject, path: Path) {
        val version = root.get("schema_version")?.takeIf { it.isJsonPrimitive }?.asInt
            ?: throw IOException("Campul 'schema_version' lipseste din $path.")
        if (version != SCHEMA_VERSION) {
            throw IOException("Schema PopulationPlan $version nu este suportata in $path; asteptat $SCHEMA_VERSION.")
        }
    }

    private fun requiredString(root: JsonObject, key: String, path: Path): String {
        return root.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
            ?: throw IOException("Campul '$key' lipseste sau este gol in $path.")
    }

    private fun requiredLong(root: JsonObject, key: String, path: Path): Long {
        return root.get(key)?.takeIf { it.isJsonPrimitive }?.asLong
            ?: throw IOException("Campul '$key' lipseste sau este invalid in $path.")
    }

    private fun writeJson(path: Path, json: JsonObject) {
        Files.createDirectories(path.parent)
        val temporary = path.resolveSibling("${path.fileName}.tmp-${UUID.randomUUID()}")
        try {
            Files.newBufferedWriter(
                temporary,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            ).use { writer -> gson.toJson(json, writer) }
            try {
                Files.move(
                    temporary,
                    path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    data class StoredPlan(
        val plan: PopulationPlan,
        val createdAt: Long,
        val updatedAt: Long
    )

    companion object {
        const val DIRECTORY_NAME = "population-plans"
        private const val SCHEMA_VERSION = 1
        private const val PLAN_FILE_SUFFIX = ".plan.json"
        private const val SELECTION_FILE_NAME = "selections.json"
        private val PLAN_ID_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,199}")
    }
}
