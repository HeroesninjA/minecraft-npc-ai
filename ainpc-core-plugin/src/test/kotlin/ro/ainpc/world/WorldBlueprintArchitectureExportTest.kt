package ro.ainpc.world

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WorldBlueprintArchitectureExportTest {
    @TempDir
    lateinit var directory: Path

    private val worldId = UUID.fromString("1bfb0dad-350a-4eac-a3b1-52b0fe112cf5")
    private val draftFingerprint = "ab".repeat(32)
    private val bounds = WorldBlueprintRegionBounds(0, 64, 0, 2, 66, 2)
    private val plan = WorldBlueprintArchitecturePlan("cuboid_shell", listOf(
        WorldBlueprintPlannedBlock(0, 64, 0, "stone"),
        WorldBlueprintPlannedBlock(1, 65, 0, "air", WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE),
        WorldBlueprintPlannedBlock(1, 65, 1, "air", WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE),
    ), WorldBlueprintArchitectureMaterialRoles("stone", "oak_planks", "bricks"),
        WorldBlueprintArchitectureEntrance.NORTH, true)

    @Test
    fun `export is deterministic and reconstructs the complete ordered recipe`() {
        val export = create()
        assertEquals(export.json, create().json)
        assertEquals(export.fingerprint, create(blueprintFingerprint = draftFingerprint.uppercase()).fingerprint)
        val document = JsonParser.parseString(export.json).asJsonObject
        assertEquals(setOf("schema_version", "plan_fingerprint", "plan"), document.keySet())
        assertEquals(1, document.get("schema_version").asInt)
        assertEquals(export.fingerprint, document.get("plan_fingerprint").asString)
        val core = document.getAsJsonObject("plan")
        assertEquals(1, core.get("recipe_version").asInt)
        assertEquals("world", core.get("world_name").asString)
        assertEquals(worldId.toString(), core.get("world_uuid").asString)
        assertEquals(draftFingerprint, core.get("blueprint_fingerprint").asString)
        assertEquals("castle", core.get("structure_type_id").asString)
        assertEquals("citadel", core.get("region_id").asString)
        val exportedBounds = core.getAsJsonObject("bounds")
        assertEquals(bounds, WorldBlueprintRegionBounds(exportedBounds.get("min_x").asInt,
            exportedBounds.get("min_y").asInt, exportedBounds.get("min_z").asInt,
            exportedBounds.get("max_x").asInt, exportedBounds.get("max_y").asInt, exportedBounds.get("max_z").asInt))
        assertEquals(plan.templateId, core.get("template_id").asString)
        val roles = core.getAsJsonObject("material_roles")
        assertEquals(plan.materials, WorldBlueprintArchitectureMaterialRoles(roles.get("floor").asString,
            roles.get("wall").asString, roles.get("roof").asString))
        assertEquals("north", core.get("entrance").asString)
        assertTrue(core.get("clear_interior").asBoolean)
        val blocks = core.getAsJsonArray("operations").map { element ->
            val operation = element.asJsonObject
            WorldBlueprintPlannedBlock(operation.get("x").asInt, operation.get("y").asInt,
                operation.get("z").asInt, operation.get("block_id").asString,
                WorldBlueprintArchitectureBlockPurpose.valueOf(operation.get("purpose").asString.uppercase()))
        }
        assertEquals(plan.blocks, blocks)
        val recalculated = MessageDigest.getInstance("SHA-256").digest(Gson().toJson(core).toByteArray(UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertEquals(recalculated, export.fingerprint)
    }

    @Test
    fun `fingerprint binds world draft selection bounds recipe and operation order`() {
        val variants = listOf(
            create(worldName = "other_world"),
            create(worldId = UUID.fromString("690628d6-936f-427b-a69a-734bd3adcf35")),
            create(blueprintFingerprint = "c".repeat(64)),
            create(structureTypeId = "house"),
            create(regionId = "harbor"),
            create(bounds = bounds.copy(maxX = 3)),
            create(plan = plan.copy(templateId = "other_template")),
            create(plan = plan.copy(materials = plan.materials.copy(floor = "andesite"))),
            create(plan = plan.copy(materials = plan.materials.copy(wall = "spruce_planks"))),
            create(plan = plan.copy(materials = plan.materials.copy(roof = "deepslate"))),
            create(plan = plan.copy(entrance = WorldBlueprintArchitectureEntrance.SOUTH)),
            create(plan = plan.copy(clearInterior = false)),
            create(plan = plan.copy(blocks = plan.blocks.reversed())),
            create(plan = plan.copy(blocks = listOf(plan.blocks[0].copy(x = 2)) + plan.blocks.drop(1))),
            create(plan = plan.copy(blocks = listOf(plan.blocks[0].copy(blockId = "andesite")) + plan.blocks.drop(1))),
            create(plan = plan.copy(blocks = listOf(plan.blocks[0].copy(
                purpose = WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE)) + plan.blocks.drop(1))),
        )
        variants.forEach { assertNotEquals(create().fingerprint, it.fingerprint) }
        assertEquals(variants.size, variants.map { it.fingerprint }.toSet().size)
    }

    @Test
    fun `invalid draft hashes and invalid operations are rejected`() {
        listOf("", "a".repeat(63), "g".repeat(64), " a".repeat(32)).forEach { hash ->
            assertThrows(IllegalArgumentException::class.java) { create(blueprintFingerprint = hash) }
        }
        listOf(emptyList(), listOf(plan.blocks[0], plan.blocks[0].copy(blockId = "bricks")),
            listOf(plan.blocks[0].copy(x = -1)), listOf(plan.blocks[0].copy(y = 67)),
            listOf(plan.blocks[0].copy(z = 3)), List(4097) { plan.blocks[0] }).forEach { blocks ->
            assertThrows(IllegalArgumentException::class.java) { create(plan = plan.copy(blocks = blocks)) }
        }
    }

    @Test
    fun `maximum operation budget remains fully exportable`() {
        val blocks = List(4096) { coordinate -> WorldBlueprintPlannedBlock(coordinate, 64, 0, "stone") }
        val export = create(bounds = WorldBlueprintRegionBounds(0, 64, 0, 4095, 64, 0),
            plan = plan.copy(blocks = blocks))
        assertEquals(4096, JsonParser.parseString(export.json).asJsonObject.getAsJsonObject("plan")
            .getAsJsonArray("operations").size())
        assertTrue(export.json.toByteArray(UTF_8).size < WorldBlueprintArchitectureExport.MAX_BYTES)
    }

    @Test
    fun `UTF8 size budget rejects oversized metadata before writing`() {
        assertThrows(IllegalArgumentException::class.java) {
            create(worldName = "ă".repeat(WorldBlueprintArchitectureExport.MAX_BYTES / 2 + 1))
        }
        assertEquals(0, Files.list(directory).use { it.count() })
    }

    @Test
    fun `wrong or malformed expected hash has no filesystem side effects`() {
        val export = create()
        val targetDirectory = directory.resolve("absent").resolve("exports")
        listOf("", "g".repeat(64), "f".repeat(64), "../export").forEach { expected ->
            assertThrows(IllegalArgumentException::class.java) { export.write(targetDirectory, expected) }
        }
        assertFalse(Files.exists(directory.resolve("absent")))
    }

    @Test
    fun `writing uses hash filename and identical repeat preserves bytes and modification time`() {
        val export = create()
        val first = export.write(directory.resolve("exports"), export.fingerprint.uppercase())
        assertTrue(first.created)
        assertEquals(directory.resolve("exports").resolve("${export.fingerprint}.json"), first.path)
        assertEquals(export.json, Files.readString(first.path))
        Files.setLastModifiedTime(first.path, FileTime.fromMillis(1234567000))
        val modificationTime = Files.getLastModifiedTime(first.path)
        val repeated = create().write(directory.resolve("exports"), export.fingerprint)
        assertFalse(repeated.created)
        assertEquals(first.path, repeated.path)
        assertEquals(modificationTime, Files.getLastModifiedTime(first.path))
        assertEquals(export.json, Files.readString(first.path))
        assertEquals(1, Files.list(first.path.parent).use { it.count() })
    }

    @Test
    fun `verification of recreated recipe preserves export siblings and modification times`() {
        val export = create()
        val file = export.write(directory, export.fingerprint).path
        val sibling = directory.resolve("keep.txt")
        Files.writeString(sibling, "unchanged sibling")
        Files.setLastModifiedTime(file, FileTime.fromMillis(1234567000))
        Files.setLastModifiedTime(sibling, FileTime.fromMillis(1234568000))
        val fileTime = Files.getLastModifiedTime(file)
        val siblingTime = Files.getLastModifiedTime(sibling)
        val directoryTime = Files.getLastModifiedTime(directory)
        val files = Files.list(directory).use { it.toList().toSet() }

        val verified = create().verify(directory.resolve("absent").resolve(".."), export.fingerprint.uppercase())

        assertEquals(file, verified)
        assertEquals(export.json, Files.readString(file))
        assertEquals("unchanged sibling", Files.readString(sibling))
        assertEquals(fileTime, Files.getLastModifiedTime(file))
        assertEquals(siblingTime, Files.getLastModifiedTime(sibling))
        assertEquals(directoryTime, Files.getLastModifiedTime(directory))
        assertEquals(files, Files.list(directory).use { it.toList().toSet() })
        assertFalse(Files.exists(directory.resolve("absent")))
    }

    @Test
    fun `verification of missing directories never creates them`() {
        val export = create()
        val targetDirectory = directory.resolve("absent").resolve("exports")

        assertThrows(IOException::class.java) { export.verify(targetDirectory, export.fingerprint) }

        assertFalse(Files.exists(directory.resolve("absent")))
        assertEquals(0, Files.list(directory).use { it.count() })
    }

    @Test
    fun `verification of missing file preserves the existing directory`() {
        val export = create()
        val sibling = directory.resolve("keep.txt")
        Files.writeString(sibling, "unchanged sibling")
        val directoryTime = Files.getLastModifiedTime(directory)

        assertThrows(IOException::class.java) { export.verify(directory, export.fingerprint) }

        assertEquals("unchanged sibling", Files.readString(sibling))
        assertEquals(directoryTime, Files.getLastModifiedTime(directory))
        assertEquals(listOf(sibling), Files.list(directory).use { it.toList() })
    }

    @Test
    fun `verification rejects wrong malformed and stale hashes before filesystem access`() {
        val export = create()
        val targetDirectory = directory.resolve("absent").resolve("exports")
        listOf("", "g".repeat(64), "f".repeat(64), "../export", " ${export.fingerprint}").forEach { expected ->
            assertThrows(IllegalArgumentException::class.java) { export.verify(targetDirectory, expected) }
        }
        val changedDraft = create(blueprintFingerprint = "c".repeat(64))
        assertThrows(IllegalArgumentException::class.java) {
            changedDraft.verify(targetDirectory, export.fingerprint)
        }
        assertFalse(Files.exists(directory.resolve("absent")))
        assertEquals(0, Files.list(directory).use { it.count() })
    }

    @Test
    fun `verification rejects altered bytes including JSON formatting without changing them`() {
        val export = create()
        val file = directory.resolve("${export.fingerprint}.json")
        val changedValues = listOf("tampered", export.json + "\n",
            Gson().toJson(JsonParser.parseString(export.json)), export.json.replace("oak_planks", "spruce_planks"))
        changedValues.forEach { changed ->
            Files.writeString(file, changed)
            Files.setLastModifiedTime(file, FileTime.fromMillis(1234567000))
            val modificationTime = Files.getLastModifiedTime(file)

            assertThrows(IOException::class.java) { export.verify(directory, export.fingerprint) }

            assertEquals(changed, Files.readString(file))
            assertEquals(modificationTime, Files.getLastModifiedTime(file))
        }
        assertEquals(listOf(file), Files.list(directory).use { it.toList() })
    }

    @Test
    fun `verification rejects oversized file without changing it`() {
        val export = create()
        val file = directory.resolve("${export.fingerprint}.json")
        Files.write(file, ByteArray(WorldBlueprintArchitectureExport.MAX_BYTES + 1) { 42 })
        Files.setLastModifiedTime(file, FileTime.fromMillis(1234567000))
        val modificationTime = Files.getLastModifiedTime(file)

        assertThrows(IOException::class.java) { export.verify(directory, export.fingerprint) }

        assertEquals(WorldBlueprintArchitectureExport.MAX_BYTES + 1L, Files.size(file))
        assertEquals(modificationTime, Files.getLastModifiedTime(file))
        assertEquals(listOf(file), Files.list(directory).use { it.toList() })
    }

    @Test
    fun `verification rejects nonregular destination without changing it`() {
        val export = create()
        val destination = Files.createDirectory(directory.resolve("${export.fingerprint}.json"))
        val modificationTime = Files.getLastModifiedTime(destination)

        assertThrows(IOException::class.java) { export.verify(directory, export.fingerprint) }

        assertTrue(Files.isDirectory(destination))
        assertEquals(modificationTime, Files.getLastModifiedTime(destination))
        assertEquals(0, Files.list(destination).use { it.count() })
        assertEquals(listOf(destination), Files.list(directory).use { it.toList() })
    }

    @Test
    fun `verification rejects a regular file as export directory without changing it`() {
        val export = create()
        val targetDirectory = directory.resolve("not-a-directory")
        Files.writeString(targetDirectory, "unchanged")
        val modificationTime = Files.getLastModifiedTime(targetDirectory)

        assertThrows(IOException::class.java) { export.verify(targetDirectory, export.fingerprint) }

        assertEquals("unchanged", Files.readString(targetDirectory))
        assertEquals(modificationTime, Files.getLastModifiedTime(targetDirectory))
        assertEquals(listOf(targetDirectory), Files.list(directory).use { it.toList() })
    }

    @Test
    fun `verification rejects symbolic link directory and destination`() {
        val export = create()
        val actualDirectory = Files.createDirectory(directory.resolve("actual"))
        val file = export.write(actualDirectory, export.fingerprint).path
        val directoryLink = directory.resolve("linked-directory")
        val fileLink = directory.resolve("${export.fingerprint}.json")
        val linksCreated = try {
            Files.createSymbolicLink(directoryLink, actualDirectory)
            Files.createSymbolicLink(fileLink, file)
            true
        } catch (_: IOException) {
            false
        } catch (_: UnsupportedOperationException) {
            false
        } catch (_: SecurityException) {
            false
        }
        assumeTrue(linksCreated, "Symbolic links are unavailable in this environment")

        assertThrows(IOException::class.java) { export.verify(directoryLink, export.fingerprint) }
        assertThrows(IOException::class.java) { export.verify(directory, export.fingerprint) }

        assertEquals(export.json, Files.readString(file))
        assertTrue(Files.isSymbolicLink(directoryLink))
        assertTrue(Files.isSymbolicLink(fileLink))
        assertEquals(1, Files.list(actualDirectory).use { it.count() })
    }

    @Test
    fun `verification works at export quota without creating temporary files`() {
        val export = create()
        val file = export.write(directory, export.fingerprint).path
        repeat(WorldBlueprintArchitectureExport.MAX_FILES - 1) { index ->
            Files.writeString(directory.resolve("existing-$index.JSON"), "{}")
        }
        val files = Files.list(directory).use { it.toList().toSet() }

        assertEquals(file, create().verify(directory, export.fingerprint))

        assertEquals(WorldBlueprintArchitectureExport.MAX_FILES, files.size)
        assertEquals(files, Files.list(directory).use { it.toList().toSet() })
        assertTrue(files.all { it.fileName.toString().endsWith(".json", ignoreCase = true) })
    }

    @Test
    fun `tampered existing export is never overwritten`() {
        val export = create()
        val file = directory.resolve("${export.fingerprint}.json")
        Files.writeString(file, "tampered")
        assertThrows(IOException::class.java) { export.write(directory, export.fingerprint) }
        assertEquals("tampered", Files.readString(file))
        assertEquals(1, Files.list(directory).use { it.count() })
    }

    @Test
    fun `oversized existing export is rejected without replacement`() {
        val export = create()
        val file = directory.resolve("${export.fingerprint}.json")
        Files.write(file, ByteArray(WorldBlueprintArchitectureExport.MAX_BYTES + 1) { 42 })
        assertThrows(IOException::class.java) { export.write(directory, export.fingerprint) }
        assertEquals(WorldBlueprintArchitectureExport.MAX_BYTES + 1L, Files.size(file))
        assertEquals(1, Files.list(directory).use { it.count() })
    }

    @Test
    fun `nonregular existing destination is rejected`() {
        val export = create()
        val destination = Files.createDirectory(directory.resolve("${export.fingerprint}.json"))
        assertThrows(IOException::class.java) { export.write(directory, export.fingerprint) }
        assertTrue(Files.isDirectory(destination))
        assertEquals(0, Files.list(destination).use { it.count() })
        assertEquals(1, Files.list(directory).use { it.count() })
    }

    @Test
    fun `full quota permits identical repeat and blocks a new export`() {
        val export = create()
        export.write(directory, export.fingerprint)
        repeat(WorldBlueprintArchitectureExport.MAX_FILES - 1) { index ->
            Files.writeString(directory.resolve("existing-$index.JSON"), "{}")
        }
        assertFalse(export.write(directory, export.fingerprint).created)
        val other = create(regionId = "other")
        assertThrows(IOException::class.java) { other.write(directory, other.fingerprint) }
        assertFalse(Files.exists(directory.resolve("${other.fingerprint}.json")))
        assertEquals(WorldBlueprintArchitectureExport.MAX_FILES.toLong(), Files.list(directory).use { it.count() })
    }

    @Test
    fun `quota only counts regular JSON files directly in export directory`() {
        repeat(WorldBlueprintArchitectureExport.MAX_FILES) { index ->
            Files.writeString(directory.resolve("note-$index.txt"), "note")
        }
        val nested = Files.createDirectory(directory.resolve("nested.json"))
        repeat(WorldBlueprintArchitectureExport.MAX_FILES) { index ->
            Files.writeString(nested.resolve("export-$index.json"), "{}")
        }
        val export = create()
        assertTrue(export.write(directory, export.fingerprint).created)
        assertFalse(Files.list(directory).use { paths -> paths.anyMatch { it.fileName.toString().endsWith(".tmp") } })
    }

    @Test
    fun `concurrent exports respect quota and leave no temporary files`() {
        val executor = Executors.newFixedThreadPool(4)
        try {
            val attempts = (0 until WorldBlueprintArchitectureExport.MAX_FILES + 4).map { index ->
                executor.submit<Boolean> {
                    val export = create(regionId = "region-$index")
                    try {
                        export.write(directory, export.fingerprint).created
                    } catch (_: IOException) {
                        false
                    }
                }
            }
            assertEquals(WorldBlueprintArchitectureExport.MAX_FILES, attempts.count { it.get(30, TimeUnit.SECONDS) })
            val files = Files.list(directory).use { it.toList() }
            assertEquals(WorldBlueprintArchitectureExport.MAX_FILES, files.size)
            assertTrue(files.all { it.fileName.toString().endsWith(".json") })
        } finally {
            executor.shutdownNow()
        }
    }

    private fun create(
        worldName: String = "world",
        worldId: UUID = this.worldId,
        blueprintFingerprint: String = draftFingerprint,
        structureTypeId: String = "castle",
        regionId: String = "citadel",
        bounds: WorldBlueprintRegionBounds = this.bounds,
        plan: WorldBlueprintArchitecturePlan = this.plan,
    ) = WorldBlueprintArchitectureExport.create(worldName, worldId, blueprintFingerprint, structureTypeId,
        regionId, bounds, plan)
}
