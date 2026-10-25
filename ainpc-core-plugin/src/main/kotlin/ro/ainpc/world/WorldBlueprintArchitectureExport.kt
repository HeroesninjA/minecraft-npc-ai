package ro.ainpc.world

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

internal data class WorldBlueprintArchitectureExportResult(val path: Path, val created: Boolean)

internal class WorldBlueprintArchitectureExport private constructor(val fingerprint: String, val json: String) {
    fun write(directory: Path, expectedFingerprint: String): WorldBlueprintArchitectureExportResult {
        val bytes = expectedBytes(expectedFingerprint)
        return synchronized(writeLock) {
            val destinationDirectory = directory.toAbsolutePath().normalize()
            Files.createDirectories(destinationDirectory)
            if (!Files.isDirectory(destinationDirectory, NOFOLLOW_LINKS)) {
                throw IOException("Directorul de export nu este un director obisnuit.")
            }
            val destination = destinationDirectory.resolve("$fingerprint.json")
            if (Files.exists(destination, NOFOLLOW_LINKS)) {
                verifyExisting(destination, bytes)
                return@synchronized WorldBlueprintArchitectureExportResult(destination, false)
            }
            val count = Files.list(destinationDirectory).use { paths ->
                paths.filter { path -> path.fileName.toString().endsWith(".json", ignoreCase = true) &&
                    Files.isRegularFile(path, NOFOLLOW_LINKS) }.limit(MAX_FILES + 1L).count()
            }
            if (count >= MAX_FILES) throw IOException("Directorul contine deja $MAX_FILES exporturi JSON.")
            val temporary = Files.createTempFile(destinationDirectory, ".architecture-export-", ".tmp")
            try {
                Files.write(temporary, bytes)
                try {
                    Files.move(temporary, destination)
                    WorldBlueprintArchitectureExportResult(destination, true)
                } catch (_: FileAlreadyExistsException) {
                    verifyExisting(destination, bytes)
                    WorldBlueprintArchitectureExportResult(destination, false)
                }
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
    }

    fun verify(directory: Path, expectedFingerprint: String): Path {
        val bytes = expectedBytes(expectedFingerprint)
        return synchronized(writeLock) {
            val sourceDirectory = directory.toAbsolutePath().normalize()
            if (!Files.isDirectory(sourceDirectory, NOFOLLOW_LINKS)) {
                throw IOException("Directorul de export nu este un director obisnuit existent.")
            }
            val source = sourceDirectory.resolve("$fingerprint.json")
            verifyExisting(source, bytes)
            source
        }
    }

    private fun expectedBytes(expectedFingerprint: String): ByteArray {
        require(hashPattern.matches(expectedFingerprint) && fingerprint.equals(expectedFingerprint, ignoreCase = true)) {
            "Amprenta planului nu corespunde exportului curent."
        }
        val bytes = json.toByteArray(UTF_8)
        require(bytes.size <= MAX_BYTES) { "Exportul depaseste limita de $MAX_BYTES octeti." }
        return bytes
    }

    companion object {
        const val MAX_BYTES = 2 * 1024 * 1024
        const val MAX_FILES = 16
        private const val MAX_OPERATIONS = 4096
        private val hashPattern = Regex("[0-9a-fA-F]{64}")
        private val writeLock = Any()
        private val compactGson = Gson()
        private val prettyGson = GsonBuilder().setPrettyPrinting().create()

        fun create(
            worldName: String,
            worldId: UUID,
            blueprintFingerprint: String,
            structureTypeId: String,
            regionId: String,
            bounds: WorldBlueprintRegionBounds,
            plan: WorldBlueprintArchitecturePlan,
        ): WorldBlueprintArchitectureExport {
            require(hashPattern.matches(blueprintFingerprint)) { "Amprenta draftului trebuie sa fie SHA-256." }
            require(plan.blocks.size in 1..MAX_OPERATIONS) { "Planul necesita intre 1 si $MAX_OPERATIONS operatii." }
            val positions = HashSet<Triple<Int, Int, Int>>()
            val operations = JsonArray()
            plan.blocks.forEach { block ->
                require(positions.add(Triple(block.x, block.y, block.z))) { "Planul contine coordonate repetate." }
                require(block.x in bounds.minX..bounds.maxX && block.y in bounds.minY..bounds.maxY &&
                    block.z in bounds.minZ..bounds.maxZ) { "O operatie depaseste limitele regiunii." }
                operations.add(JsonObject().apply {
                    addProperty("x", block.x)
                    addProperty("y", block.y)
                    addProperty("z", block.z)
                    addProperty("block_id", block.blockId)
                    addProperty("purpose", block.purpose.name.lowercase(Locale.ROOT))
                })
            }
            val core = JsonObject().apply {
                addProperty("recipe_version", 1)
                addProperty("world_name", worldName)
                addProperty("world_uuid", worldId.toString())
                addProperty("blueprint_fingerprint", blueprintFingerprint.lowercase(Locale.ROOT))
                addProperty("structure_type_id", structureTypeId)
                addProperty("region_id", regionId)
                add("bounds", JsonObject().apply {
                    addProperty("min_x", bounds.minX)
                    addProperty("min_y", bounds.minY)
                    addProperty("min_z", bounds.minZ)
                    addProperty("max_x", bounds.maxX)
                    addProperty("max_y", bounds.maxY)
                    addProperty("max_z", bounds.maxZ)
                })
                addProperty("template_id", plan.templateId)
                add("material_roles", JsonObject().apply {
                    addProperty("floor", plan.materials.floor)
                    addProperty("wall", plan.materials.wall)
                    addProperty("roof", plan.materials.roof)
                })
                addProperty("entrance", plan.entrance.id)
                addProperty("clear_interior", plan.clearInterior)
                add("operations", operations)
            }
            val fingerprint = MessageDigest.getInstance("SHA-256")
                .digest(compactGson.toJson(core).toByteArray(UTF_8)).joinToString("") { "%02x".format(it) }
            val json = prettyGson.toJson(JsonObject().apply {
                addProperty("schema_version", 1)
                addProperty("plan_fingerprint", fingerprint)
                add("plan", core)
            })
            require(json.toByteArray(UTF_8).size <= MAX_BYTES) { "Exportul depaseste limita de $MAX_BYTES octeti." }
            return WorldBlueprintArchitectureExport(fingerprint, json)
        }

        private fun verifyExisting(path: Path, expected: ByteArray) {
            if (!Files.isRegularFile(path, NOFOLLOW_LINKS) || Files.size(path) > MAX_BYTES) {
                throw IOException("Fisierul de export existent nu este un fisier obisnuit in limita admisa.")
            }
            val existing = Files.newInputStream(path, NOFOLLOW_LINKS).use { it.readNBytes(MAX_BYTES + 1) }
            if (existing.size > MAX_BYTES || !existing.contentEquals(expected)) {
                throw IOException("Fisierul de export existent are un continut diferit; nu a fost suprascris.")
            }
        }
    }
}
