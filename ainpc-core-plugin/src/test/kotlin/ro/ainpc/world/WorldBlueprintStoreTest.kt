package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class WorldBlueprintStoreTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `round trips independent world drafts without applying them`() {
        val store = WorldBlueprintStore(directory.resolve("world-blueprints.json"))
        val first = WorldBlueprint(
            worldName = "world",
            story = "Un regat vechi cu doua cetati rivale.",
            properties = mapOf("clima" to "Rece si ploios"),
            professionTypeLimit = 3,
            professions = mapOf("fierar" to WorldProfessionBlueprint(2, "Repara unelte si arme")),
            architecture = "Case de piatra si lemn, acoperisuri abrupte",
            mapRules = mapOf("drumuri" to "Leaga satele, evita muntii abrupti"),
        )

        store.save(first)
        store.save(WorldBlueprint("nether", story = "Taram vulcanic"))
        store.save(first.copy(properties = first.properties + ("pericol" to "Bandiți la nord")))

        val reloaded = WorldBlueprintStore(directory.resolve("world-blueprints.json"))
        assertEquals("Taram vulcanic", reloaded.get("nether")?.story)
        assertEquals(2, reloaded.get("world")?.properties?.size)
        assertEquals(2, reloaded.get("world")?.professions?.get("fierar")?.maxNpcs)
        assertTrue(WorldBlueprintPolicy.completionIssues(reloaded.get("world")!!).isEmpty())
        assertFalse(WorldBlueprintPolicy.completionIssues(reloaded.get("nether")!!).isEmpty())
    }

    @Test
    fun `rejects invalid limits without overwriting the last valid draft`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val blueprint = WorldBlueprint("world", professionTypeLimit = 1,
            professions = mapOf("fierar" to WorldProfessionBlueprint(2, "Atelier")))
        store.save(blueprint)
        val before = Files.readString(file)

        assertThrows(IllegalArgumentException::class.java) {
            store.save(blueprint.copy(professions = blueprint.professions +
                ("brutar" to WorldProfessionBlueprint(1, "Cuptor"))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            store.save(blueprint.copy(professions = mapOf("fierar" to WorldProfessionBlueprint(513, "Atelier"))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            store.save(blueprint.copy(mapRules = mapOf("drumuri" to "")))
        }
        assertEquals(before, Files.readString(file))
    }

    @Test
    fun `corrupt or unsupported file is never silently replaced`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        Files.writeString(file, "{\"version\":5,\"worlds\":[]}")

        assertThrows(IOException::class.java) { store.save(WorldBlueprint("world", story = "Nou")) }
        assertEquals("{\"version\":5,\"worlds\":[]}", Files.readString(file))

        Files.writeString(file, "{\"version\":1.5,\"worlds\":[]}")
        assertThrows(IOException::class.java) { store.get("world") }
    }

    @Test
    fun `confirmation binds the exact complete draft and edits revoke it`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val blueprint = WorldBlueprint(
            "world", "Poveste", mapOf("clima" to "Rece"), 2,
            mapOf("fierar" to WorldProfessionBlueprint(2, "Atelier")),
            "Case de piatra", mapOf("drumuri" to "Leaga satele"),
        )
        store.save(blueprint)
        val fingerprint = store.fingerprint(blueprint)
        assertTrue(fingerprint.matches(Regex("[a-f0-9]{64}")))
        assertThrows(IllegalArgumentException::class.java) { store.confirm("world", "stale", "admin") }

        val approval = store.confirm("world", fingerprint, "admin")
        assertEquals(fingerprint, approval.fingerprint)
        assertEquals("admin", WorldBlueprintStore(file).get("world")?.approval?.approvedBy)
        store.save(blueprint)
        assertEquals(approval, store.get("world")?.approval)

        store.save(blueprint.copy(story = "Poveste schimbata", approval = approval))
        assertEquals(null, store.get("world")?.approval)
        assertThrows(IllegalArgumentException::class.java) { store.confirm("world", fingerprint, "admin") }
        assertEquals("Poveste schimbata", store.get("world")?.story)
    }

    @Test
    fun `incomplete drafts cannot be confirmed`() {
        val store = WorldBlueprintStore(directory.resolve("world-blueprints.json"))
        val blueprint = WorldBlueprint("world", story = "Inceput")
        store.save(blueprint)

        assertThrows(IllegalArgumentException::class.java) {
            store.confirm("world", store.fingerprint(blueprint), "admin")
        }
        assertEquals(null, store.get("world")?.approval)
    }

    @Test
    fun `reads version one and migrates it on the next write`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        store.save(WorldBlueprint("world", story = "Poveste"))
        Files.writeString(file, Files.readString(file).replace("\"version\":4", "\"version\":1"))

        assertEquals("Poveste", store.get("world")?.story)
        store.save(store.get("world")!!.copy(architecture = "Piatra"))
        assertTrue(Files.readString(file).contains("\"version\":4"))
    }

    @Test
    fun `reads version two and migrates it on the next write`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        store.save(WorldBlueprint("world", story = "Poveste"))
        Files.writeString(file, Files.readString(file)
            .replace("\"version\":4", "\"version\":2")
            .replace(",\"suggestion_history\":[]", ""))

        assertEquals("Poveste", store.get("world")?.story)
        store.save(store.get("world")!!.copy(architecture = "Piatra"))
        assertTrue(Files.readString(file).contains("\"version\":4"))
    }

    @Test
    fun `accepts only the reviewed field and preserves provenance across edits`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val initial = WorldBlueprint("world", "Poveste", mapOf("clima" to "Rece"), 2,
            mapOf("fierar" to WorldProfessionBlueprint(1, "Atelier")), "Piatra",
            mapOf("drumuri" to "Leaga satele"))
        store.save(initial)
        store.confirm("world", store.fingerprint(initial), "admin")
        val suggestion = suggestion(store, initial, "Poveste noua")
        val candidateHash = store.fingerprint(suggestion.candidate)

        assertThrows(IllegalArgumentException::class.java) {
            store.acceptSuggestion(suggestion, "0".repeat(64), "admin")
        }
        assertEquals("Poveste", store.get("world")?.story)
        val acceptance = store.acceptSuggestion(suggestion, candidateHash, "admin")
        val accepted = WorldBlueprintStore(file).get("world")!!
        assertEquals("Poveste noua", accepted.story)
        assertEquals(null, accepted.approval)
        assertEquals(candidateHash, acceptance.acceptedFingerprint)
        assertEquals("openai", accepted.suggestionHistory.single().provenance.provider)
        assertEquals("admin", accepted.suggestionHistory.single().acceptedBy)

        store.save(accepted.copy(architecture = "Lemn", suggestionHistory = emptyList()))
        assertEquals(acceptance, store.get("world")?.suggestionHistory?.single())
        assertThrows(IllegalArgumentException::class.java) {
            store.acceptSuggestion(suggestion, candidateHash, "admin")
        }
    }

    @Test
    fun `rejects stale and multi field proposals without changing disk`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val initial = WorldBlueprint("world", story = "Vechi")
        store.save(initial)
        val suggestion = suggestion(store, initial, "Nou")
        val manipulated = suggestion.copy(candidate = suggestion.candidate.copy(architecture = "Piatra"))
        val before = Files.readString(file)

        assertThrows(IllegalArgumentException::class.java) {
            store.acceptSuggestion(manipulated, store.fingerprint(manipulated.candidate), "admin")
        }
        assertEquals(before, Files.readString(file))

        store.save(initial.copy(story = "Altul"))
        val updated = Files.readString(file)
        assertThrows(IllegalArgumentException::class.java) {
            store.acceptSuggestion(suggestion, store.fingerprint(suggestion.candidate), "admin")
        }
        assertEquals(updated, Files.readString(file))
    }

    @Test
    fun `can accept the first field of a new world but rejects a no op`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val base = WorldBlueprint("world")
        val instruction = "O poveste"
        val provenance = WorldBlueprintSuggestionProvenance("ollama", "test-model",
            System.currentTimeMillis(), WorldBlueprintSuggestionParser.requestFingerprint(instruction))
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.STORY,
            null, instruction, "none")
        val suggestion = WorldBlueprintSuggestionParser.parse(request, base, "{\"text\":\"Inceput\"}", provenance)

        store.acceptSuggestion(suggestion, store.fingerprint(suggestion.candidate), "admin")
        assertEquals("Inceput", store.get("world")?.story)
        assertEquals("none", store.get("world")?.suggestionHistory?.single()?.baseFingerprint)

        val current = store.get("world")!!
        val noOp = WorldBlueprintSuggestionParser.parse(request.copy(baseFingerprint = store.fingerprint(current)),
            current, "{\"text\":\"Inceput\"}", provenance)
        val before = Files.readString(file)
        assertThrows(IllegalArgumentException::class.java) {
            store.acceptSuggestion(noOp, store.fingerprint(noOp.candidate), "admin")
        }
        assertEquals(before, Files.readString(file))
    }

    @Test
    fun `rejects malformed provenance history without overwriting it`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val initial = WorldBlueprint("world", story = "Vechi")
        store.save(initial)
        val suggestion = suggestion(store, initial, "Nou")
        store.acceptSuggestion(suggestion, store.fingerprint(suggestion.candidate), "admin")
        val tampered = Files.readString(file).replace("\"provider\":\"openai\"", "\"provider\":\"unknown\"")
        Files.writeString(file, tampered)

        assertThrows(IOException::class.java) { store.get("world") }
        assertThrows(IOException::class.java) { store.save(initial) }
        assertEquals(tampered, Files.readString(file))
    }

    private fun suggestion(store: WorldBlueprintStore, base: WorldBlueprint, text: String): WorldBlueprintSuggestion {
        val instruction = "Propune o poveste"
        val request = WorldBlueprintSuggestionRequest(base.worldName, WorldBlueprintSuggestionField.STORY,
            null, instruction, store.fingerprint(base))
        val provenance = WorldBlueprintSuggestionProvenance("openai", "test-model",
            System.currentTimeMillis(), WorldBlueprintSuggestionParser.requestFingerprint(instruction))
        return WorldBlueprintSuggestionParser.parse(request, base, "{\"text\":\"$text\"}", provenance)
    }

    @Test
    fun `rejects tampered approval and allows explicit revocation`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val blueprint = WorldBlueprint("world", "Poveste", mapOf("clima" to "Rece"), 2,
            mapOf("fierar" to WorldProfessionBlueprint(1, "Atelier")), "Piatra",
            mapOf("drumuri" to "Leaga satele"))
        store.save(blueprint)
        val fingerprint = store.fingerprint(blueprint)
        store.confirm("world", fingerprint, "admin")
        val approved = Files.readString(file)
        val tampered = approved.replace(fingerprint, "0".repeat(64))
        Files.writeString(file, tampered)

        assertThrows(IOException::class.java) { store.get("world") }
        assertThrows(IOException::class.java) { store.save(blueprint) }
        assertEquals(tampered, Files.readString(file))

        Files.writeString(file, approved)
        store.revoke("world")
        assertEquals(null, store.get("world")?.approval)
        assertEquals("Poveste", store.get("world")?.story)
    }
}
