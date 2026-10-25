package ro.ainpc.world

import org.bukkit.World
import org.bukkit.WorldType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class WorldBlueprintWorldCreationJournalTest {
    @TempDir
    lateinit var directory: Path

    private val fingerprint = "a".repeat(64)

    @Test
    fun `persists pending attempt before outcome and keeps history`() {
        val file = directory.resolve("world-blueprint-creations.json")
        val journal = WorldBlueprintWorldCreationJournal(file)
        assertTrue(journal.latest("new_world").isEmpty())
        assertFalse(Files.exists(file))

        val options = WorldBlueprintWorldCreationOptions(123L, WorldType.FLAT,
            World.Environment.NORMAL, false)
        val first = journal.begin("new_world", fingerprint, "admin", options)
        val reloaded = WorldBlueprintWorldCreationJournal(file)
        assertEquals(WorldBlueprintWorldCreationStatus.PENDING, reloaded.latest("new_world").single().status)
        assertEquals(first.id, reloaded.latest("new_world").single().id)
        assertEquals(options, reloaded.latest("new_world").single().options)
        assertTrue(Files.readString(file).contains(fingerprint))
        assertTrue(Files.readString(file).contains("\"version\":3"))

        journal.finish(first.id, WorldBlueprintWorldCreationStatus.FAILED)
        assertThrows(IllegalArgumentException::class.java) {
            journal.finish(first.id, WorldBlueprintWorldCreationStatus.CREATED)
        }
        val second = journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
        journal.finish(second.id, WorldBlueprintWorldCreationStatus.CREATED)

        val history = reloaded.latest("new_world")
        assertEquals(listOf(second.id, first.id), history.map { it.id })
        assertEquals(listOf(WorldBlueprintWorldCreationStatus.CREATED, WorldBlueprintWorldCreationStatus.FAILED),
            history.map { it.status })
        assertTrue(history.all { it.finishedAt != null && it.finishedAt >= it.startedAt })
        assertEquals(listOf(WorldBlueprintWorldCreationOptions(), options), history.map { it.options })
        assertTrue(reloaded.latest("other_world").isEmpty())
    }

    @Test
    fun `migrates version one entries without inventing generation options`() {
        val file = directory.resolve("world-blueprint-creations.json")
        val oldId = UUID.randomUUID()
        Files.writeString(file, """{"version":1,"attempts":[{"id":"$oldId","world_name":"new_world","fingerprint":"$fingerprint","actor":"admin","started_at":1,"status":"CREATED","finished_at":2}]}""")
        val journal = WorldBlueprintWorldCreationJournal(file)
        assertNull(journal.latest("new_world").single().options)

        val newOptions = WorldBlueprintWorldCreationOptions(environment = World.Environment.NETHER)
        val next = journal.begin("new_world", fingerprint, "admin", newOptions)
        val reloaded = WorldBlueprintWorldCreationJournal(file).latest("new_world")
        assertEquals(listOf(next.id, oldId), reloaded.map { it.id })
        assertEquals(newOptions, reloaded.first().options)
        assertNull(reloaded.last().options)
        assertTrue(Files.readString(file).contains("\"version\":3"))
    }

    @Test
    fun `migrates version two entries without adding retry links`() {
        val file = directory.resolve("world-blueprint-creations.json")
        val oldId = UUID.randomUUID()
        Files.writeString(file, """{"version":2,"attempts":[{"id":"$oldId","world_name":"new_world","fingerprint":"$fingerprint","actor":"admin","started_at":1,"status":"PENDING","finished_at":null,"options":{"world_seed":42,"world_type":"flat","world_environment":"normal","world_generate_structures":false}}]}""")
        val journal = WorldBlueprintWorldCreationJournal(file)
        val legacy = journal.latest("new_world").single()
        assertEquals(WorldBlueprintWorldCreationOptions(42L, WorldType.FLAT, World.Environment.NORMAL, false), legacy.options)
        assertNull(legacy.retryOf)

        assertThrows(IllegalArgumentException::class.java) {
            journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
        }
        val retry = journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions(), oldId)
        assertTrue(Files.readString(file).contains("\"version\":3"))
        val migrated = WorldBlueprintWorldCreationJournal(file).latest("new_world")
        assertEquals(oldId, migrated.first().retryOf)
        assertEquals(retry.id, migrated.first().id)
        assertNull(migrated.last().retryOf)
    }

    @Test
    fun `requires explicit matching retry for latest pending attempt`() {
        val file = directory.resolve("world-blueprint-creations.json")
        val journal = WorldBlueprintWorldCreationJournal(file)
        val pending = journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
        val before = Files.readString(file)
        assertThrows(IllegalArgumentException::class.java) {
            journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
        }
        assertThrows(IllegalArgumentException::class.java) {
            journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions(), UUID.randomUUID())
        }
        assertEquals(before, Files.readString(file))

        val retry = journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions(), pending.id)
        assertEquals(pending.id, WorldBlueprintWorldCreationJournal(file).latest("new_world").first().retryOf)
        journal.finish(retry.id, WorldBlueprintWorldCreationStatus.FAILED)
        assertThrows(IllegalArgumentException::class.java) {
            journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions(), retry.id)
        }
        assertNull(journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions()).retryOf)
    }

    @Test
    fun `corrupt retry link is rejected without overwriting the journal`() {
        val file = directory.resolve("world-blueprint-creations.json")
        val journal = WorldBlueprintWorldCreationJournal(file)
        val pending = journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
        journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions(), pending.id)
        val original = Files.readString(file)
        val invalid = original.replace("\"retry_of\":\"${pending.id}\"", "\"retry_of\":\"${UUID.randomUUID()}\"")
        assertTrue(invalid != original)
        Files.writeString(file, invalid)

        assertThrows(IOException::class.java) { journal.latest("new_world") }
        assertThrows(IOException::class.java) {
            journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
        }
        assertEquals(invalid, Files.readString(file))
    }

    @Test
    fun `pages exact world history newest first without writing`() {
        val file = directory.resolve("world-blueprint-creations.json")
        val journal = WorldBlueprintWorldCreationJournal(file)
        val identifiers = (1..12).map { index ->
            if (index == 6) journal.begin("other_world", fingerprint, "admin",
                WorldBlueprintWorldCreationOptions())
            val attempt = journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
            journal.finish(attempt.id, WorldBlueprintWorldCreationStatus.FAILED)
            attempt.id
        }
        val before = Files.readString(file)

        val first = journal.page("new_world", 1)
        val second = journal.page("new_world", 2)
        val third = journal.page("new_world", 3)
        assertEquals(12, first.total)
        assertEquals(3, first.pageCount)
        assertEquals(1, first.page)
        assertEquals(identifiers.reversed().take(5), first.attempts.map { it.id })
        assertEquals(identifiers.reversed().drop(5).take(5), second.attempts.map { it.id })
        assertEquals(identifiers.reversed().drop(10), third.attempts.map { it.id })
        assertTrue(journal.page("new_world", 4).attempts.isEmpty())
        assertEquals(1, journal.page("missing_world", 1).pageCount)
        assertEquals(before, Files.readString(file))
        assertThrows(IllegalArgumentException::class.java) { journal.page("new_world", 0) }
        assertThrows(IllegalArgumentException::class.java) { journal.page("new_world", 1, 21) }
    }

    @Test
    fun `find selects exact historical attempt only in its world without writing`() {
        val file = directory.resolve("world-blueprint-creations.json")
        val journal = WorldBlueprintWorldCreationJournal(file)
        val first = journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions(seed = 42L))
        journal.finish(first.id, WorldBlueprintWorldCreationStatus.FAILED)
        val otherWorld = journal.begin("other_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
        val latest = journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
        val before = Files.readString(file)

        val historical = journal.find("new_world", first.id)
        assertEquals(first.id, historical?.id)
        assertEquals(WorldBlueprintWorldCreationStatus.FAILED, historical?.status)
        assertEquals(WorldBlueprintWorldCreationOptions(seed = 42L), historical?.options)
        assertEquals(latest, journal.find("new_world", latest.id))
        assertNull(journal.find("new_world", otherWorld.id))
        assertNull(journal.find("new_world", UUID.randomUUID()))
        assertEquals(before, Files.readString(file))
    }

    @Test
    fun `corrupt journal cannot be overwritten by a new attempt`() {
        val file = directory.resolve("world-blueprint-creations.json")
        Files.writeString(file, "{\"version\":4,\"attempts\":[]}")
        val before = Files.readString(file)
        val journal = WorldBlueprintWorldCreationJournal(file)

        assertThrows(IOException::class.java) {
            journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
        }
        assertEquals(before, Files.readString(file))
    }

    @Test
    fun `invalid options cannot be read or overwritten`() {
        val file = directory.resolve("world-blueprint-creations.json")
        val journal = WorldBlueprintWorldCreationJournal(file)
        journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions(
            worldType = WorldType.FLAT))
        val invalid = Files.readString(file).replace("\"world_type\":\"flat\"", "\"world_type\":\"custom\"")
        Files.writeString(file, invalid)

        assertThrows(IOException::class.java) { journal.latest("new_world") }
        assertThrows(IOException::class.java) {
            journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions())
        }
        assertEquals(invalid, Files.readString(file))
    }

    @Test
    fun `rejects malformed attempt metadata before writing`() {
        val file = directory.resolve("world-blueprint-creations.json")
        val journal = WorldBlueprintWorldCreationJournal(file)

        assertThrows(IllegalArgumentException::class.java) {
            journal.begin("new_world", "bad", "admin", WorldBlueprintWorldCreationOptions())
        }
        assertThrows(IllegalArgumentException::class.java) {
            journal.begin("new_world", fingerprint, "\n", WorldBlueprintWorldCreationOptions())
        }
        assertThrows(IllegalArgumentException::class.java) {
            journal.begin("new_world", fingerprint, "admin", WorldBlueprintWorldCreationOptions(
                worldType = WorldType.FLAT, environment = World.Environment.NETHER))
        }
        assertFalse(Files.exists(file))
    }
}
