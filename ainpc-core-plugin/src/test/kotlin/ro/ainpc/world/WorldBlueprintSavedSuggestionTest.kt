package ro.ainpc.world

import com.google.gson.JsonParser
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

class WorldBlueprintSavedSuggestionTest {
    @TempDir
    lateinit var directory: Path

    private val owner = UUID.fromString("7f220ff0-88f3-452c-a78d-d6b39b9cc842")
    private val anotherOwner = UUID.fromString("97c58ea1-7c51-4f0c-a998-31b17765e738")

    @Test
    fun `first world proposal stays separate until acceptance creates its draft`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val base = WorldBlueprint("world")
        val proposal = suggestion(store, base, "Prima poveste").copy(baseFingerprint = "none")
        val candidateHash = store.fingerprint(proposal.candidate)
        store.saveSuggestion(owner, proposal, candidateHash)

        val restarted = WorldBlueprintStore(file)
        assertNull(restarted.get("world"))
        assertEquals(proposal, restarted.getSavedSuggestion(owner, "world")?.suggestion)
        restarted.acceptSavedSuggestion(owner, "world", candidateHash, "admin")
        assertEquals("Prima poveste", restarted.get("world")?.story)
        assertEquals("none", restarted.get("world")?.suggestionHistory?.single()?.baseFingerprint)
        assertNull(restarted.getSavedSuggestion(owner, "world"))
    }

    @Test
    fun `saved proposal survives restart and acceptance consumes it with the draft update`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val base = completeDraft()
        store.save(base)
        val suggestion = suggestion(store, base, "O poveste noua")
        val candidateHash = store.fingerprint(suggestion.candidate)
        store.saveSuggestion(owner, suggestion, candidateHash)
        val approval = store.confirm(base.worldName, store.fingerprint(base), "admin")

        val restarted = WorldBlueprintStore(file)
        assertEquals(suggestion, restarted.getSavedSuggestion(owner, base.worldName)?.suggestion)
        assertEquals(approval, restarted.get(base.worldName)?.approval)
        assertEquals(base.story, restarted.get(base.worldName)?.story)
        assertNull(restarted.getSavedSuggestion(anotherOwner, base.worldName))
        assertNull(restarted.getSavedSuggestion(owner, "nether"))
        assertFalse(Files.readString(file).contains("INSTRUCTIUNE PRIVATA"))

        restarted.acceptSavedSuggestion(owner, base.worldName, candidateHash, "admin")
        val accepted = WorldBlueprintStore(file)
        assertEquals("O poveste noua", accepted.get(base.worldName)?.story)
        assertNull(accepted.get(base.worldName)?.approval)
        assertNull(accepted.getSavedSuggestion(owner, base.worldName))
        assertEquals(candidateHash, accepted.get(base.worldName)?.suggestionHistory?.single()?.acceptedFingerprint)
        assertThrows(IllegalArgumentException::class.java) {
            accepted.acceptSavedSuggestion(owner, base.worldName, candidateHash, "admin")
        }
    }

    @Test
    fun `wrong fingerprint owner world and stale revision never consume another proposal`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val base = completeDraft()
        store.save(base)
        val first = suggestion(store, base, "Prima")
        val second = suggestion(store, base, "A doua")
        store.saveSuggestion(owner, first, store.fingerprint(first.candidate))
        store.saveSuggestion(anotherOwner, second, store.fingerprint(second.candidate))
        val before = Files.readString(file)

        assertThrows(IllegalArgumentException::class.java) {
            store.acceptSavedSuggestion(owner, base.worldName, store.fingerprint(second.candidate), "admin")
        }
        assertThrows(IllegalArgumentException::class.java) {
            store.acceptSavedSuggestion(owner, "nether", store.fingerprint(first.candidate), "admin")
        }
        assertEquals(before, Files.readString(file))

        store.acceptSavedSuggestion(owner, base.worldName, store.fingerprint(first.candidate), "admin")
        val afterFirst = Files.readString(file)
        assertThrows(IllegalArgumentException::class.java) {
            store.acceptSavedSuggestion(anotherOwner, base.worldName, store.fingerprint(second.candidate), "other-admin")
        }
        assertEquals(afterFirst, Files.readString(file))
        assertEquals(second, WorldBlueprintStore(file).getSavedSuggestion(anotherOwner, base.worldName)?.suggestion)
    }

    @Test
    fun `saving revalidates content and replacing the owner slot preserves other owners`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val base = completeDraft()
        store.save(base)
        val first = suggestion(store, base, "Prima")
        store.saveSuggestion(owner, first, store.fingerprint(first.candidate))
        store.saveSuggestion(anotherOwner, first, store.fingerprint(first.candidate))
        val manipulated = first.copy(candidate = first.candidate.copy(architecture = "Alta arhitectura"))
        val before = Files.readString(file)

        assertThrows(IllegalArgumentException::class.java) {
            store.saveSuggestion(owner, first, "0".repeat(64))
        }
        assertThrows(IllegalArgumentException::class.java) {
            store.saveSuggestion(owner, manipulated, store.fingerprint(manipulated.candidate))
        }
        assertEquals(before, Files.readString(file))

        val nether = completeDraft().copy(worldName = "nether")
        store.save(nether)
        val replacement = suggestion(store, nether, "Lumea noua")
        store.saveSuggestion(owner, replacement, store.fingerprint(replacement.candidate))
        assertNull(store.getSavedSuggestion(owner, base.worldName))
        assertEquals(replacement, store.getSavedSuggestion(owner, "nether")?.suggestion)
        assertEquals(first, store.getSavedSuggestion(anotherOwner, base.worldName)?.suggestion)
        store.discardSavedSuggestion(owner, base.worldName)
        assertEquals(replacement, store.getSavedSuggestion(owner, "nether")?.suggestion)
        store.discardSavedSuggestion(owner, "nether")
        assertNull(WorldBlueprintStore(file).getSavedSuggestion(owner, "nether"))
        assertEquals(nether, store.get("nether"))
    }

    @Test
    fun `structured map rule suggestions are revalidated before saving and accepting`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val base = WorldBlueprint("world")
        store.save(base)
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            "required_place_types", "locuri", store.fingerprint(base))
        val proposal = WorldBlueprintSuggestionParser.parse(request, base,
            "{\"text\":\"house,forge\"}", WorldBlueprintSuggestionProvenance("openai", "test-model", 1L,
                WorldBlueprintSuggestionParser.requestFingerprint(request.instruction)))
        val invalid = proposal.copy(candidate = proposal.candidate.copy(
            mapRules = mapOf("required_place_types" to "house,unknown")))
        val before = Files.readString(file)

        assertThrows(IllegalArgumentException::class.java) {
            store.saveSuggestion(owner, invalid, store.fingerprint(invalid.candidate))
        }
        assertThrows(IllegalArgumentException::class.java) {
            store.acceptSuggestion(invalid, store.fingerprint(invalid.candidate), "admin")
        }
        assertEquals(before, Files.readString(file))

        val fingerprint = store.fingerprint(proposal.candidate)
        store.saveSuggestion(owner, proposal, fingerprint)
        val restarted = WorldBlueprintStore(file)
        restarted.acceptSavedSuggestion(owner, "world", fingerprint, "admin")
        assertEquals("house,forge", restarted.get("world")?.mapRules?.get("required_place_types"))
    }

    @Test
    fun `draft edits and approval revocation preserve saved proposals for review`() {
        val store = WorldBlueprintStore(directory.resolve("world-blueprints.json"))
        val base = completeDraft()
        store.save(base)
        val proposal = suggestion(store, base, "Nou")
        store.saveSuggestion(owner, proposal, store.fingerprint(proposal.candidate))
        store.confirm(base.worldName, store.fingerprint(base), "admin")
        store.revoke(base.worldName)
        store.save(base.copy(architecture = "Lemn"))

        assertEquals(proposal, store.getSavedSuggestion(owner, base.worldName)?.suggestion)
        assertThrows(IllegalArgumentException::class.java) {
            store.saveSuggestion(owner, proposal, store.fingerprint(proposal.candidate))
        }
    }

    @Test
    fun `malformed saved proposals block writes instead of being silently removed`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val base = completeDraft()
        store.save(base)
        val proposal = suggestion(store, base, "Nou")
        store.saveSuggestion(owner, proposal, store.fingerprint(proposal.candidate))
        val valid = Files.readString(file)

        listOf("fingerprint", "owner", "timestamp", "duplicate", "candidate").forEach { problem ->
            val root = JsonParser.parseString(valid).asJsonObject
            val entries = root.getAsJsonArray("saved_suggestions")
            val record = entries[0].asJsonObject
            when (problem) {
                "fingerprint" -> record.addProperty("candidate_fingerprint", "0".repeat(64))
                "owner" -> record.addProperty("owner", "invalid-uuid")
                "timestamp" -> record.addProperty("saved_at", 0)
                "duplicate" -> entries.add(record.deepCopy())
                "candidate" -> record.getAsJsonObject("candidate").addProperty("story", "Alterat")
            }
            val invalid = root.toString()
            Files.writeString(file, invalid)
            assertThrows(IOException::class.java) { store.getSavedSuggestion(owner, base.worldName) }
            assertThrows(IOException::class.java) { store.save(base) }
            assertThrows(IOException::class.java) { store.discardSavedSuggestion(owner, base.worldName) }
            assertEquals(invalid, Files.readString(file))
        }
    }

    @Test
    fun `version three migrates while preserving confirmation and acceptance history`() {
        val file = directory.resolve("world-blueprints.json")
        val store = WorldBlueprintStore(file)
        val base = completeDraft()
        store.save(base)
        val first = suggestion(store, base, "Nou")
        store.acceptSuggestion(first, store.fingerprint(first.candidate), "admin")
        store.confirm(base.worldName, store.fingerprint(first.candidate), "admin")
        val accepted = store.get(base.worldName)!!
        val root = JsonParser.parseString(Files.readString(file)).asJsonObject
        root.addProperty("version", 3)
        root.remove("saved_suggestions")
        Files.writeString(file, root.toString())

        val restarted = WorldBlueprintStore(file)
        assertEquals(accepted, restarted.get(base.worldName))
        assertNull(restarted.getSavedSuggestion(owner, base.worldName))
        val next = suggestion(restarted, accepted, "Alta poveste")
        restarted.saveSuggestion(owner, next, restarted.fingerprint(next.candidate))
        assertEquals(accepted, restarted.get(base.worldName))
        assertEquals(4, JsonParser.parseString(Files.readString(file)).asJsonObject.get("version").asInt)
        assertTrue(restarted.getSavedSuggestion(owner, base.worldName) != null)
    }

    private fun completeDraft() = WorldBlueprint("world", "Poveste", mapOf("clima" to "Rece"), 2,
        mapOf("fierar" to WorldProfessionBlueprint(1, "Atelier")), "Piatra", mapOf("drumuri" to "Leaga satele"))

    private fun suggestion(store: WorldBlueprintStore, base: WorldBlueprint, text: String): WorldBlueprintSuggestion {
        val instruction = "INSTRUCTIUNE PRIVATA"
        val request = WorldBlueprintSuggestionRequest(base.worldName, WorldBlueprintSuggestionField.STORY,
            null, instruction, store.fingerprint(base))
        val provenance = WorldBlueprintSuggestionProvenance("openai", "test-model", 1L,
            WorldBlueprintSuggestionParser.requestFingerprint(instruction))
        return WorldBlueprintSuggestionParser.parse(request, base, "{\"text\":\"$text\"}", provenance)
    }
}
