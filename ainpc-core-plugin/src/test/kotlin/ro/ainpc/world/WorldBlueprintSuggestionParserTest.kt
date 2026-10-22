package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintSuggestionParserTest {
    private fun provenance(request: WorldBlueprintSuggestionRequest) = WorldBlueprintSuggestionProvenance(
        "openai", "test-model", 100L, WorldBlueprintSuggestionParser.requestFingerprint(request.instruction))

    @Test
    fun `parses a single advisory field without changing the base draft`() {
        val base = WorldBlueprint("world", story = "Poveste veche", properties = mapOf("clima" to "Rece"),
            approval = WorldBlueprintApproval("old", "admin", 1L))
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.STORY,
            null, "o poveste noua", "base-hash")

        val suggestion = WorldBlueprintSuggestionParser.parse(request, base,
            "{\"text\":\"Poveste noua\"}", provenance(request))

        assertEquals("Poveste noua", suggestion.candidate.story)
        assertEquals("Poveste veche", base.story)
        assertEquals(base.properties, suggestion.candidate.properties)
        assertNull(suggestion.candidate.approval)
        assertEquals("base-hash", suggestion.baseFingerprint)
        assertTrue(WorldBlueprintSuggestionParser.requestFingerprint("o poveste noua")
            .matches(Regex("[a-f0-9]{64}")))
    }

    @Test
    fun `validates profession and property proposals against draft policy`() {
        val base = WorldBlueprint("world", professionTypeLimit = 1)
        val profession = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.PROFESSION,
            "fierar", "meserie", "none")
        val property = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.PROPERTY,
            "clima", "iarna", "none")

        assertEquals(3, WorldBlueprintSuggestionParser.parse(profession, base,
            "{\"max_npcs\":3,\"description\":\"Fierarul satului\"}", provenance(profession))
            .candidate.professions["fierar"]?.maxNpcs)
        assertEquals("Rece", WorldBlueprintSuggestionParser.parse(property, base,
            "{\"text\":\"Rece\"}", provenance(property)).candidate.properties["clima"])
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(profession, base,
                "{\"max_npcs\":513,\"description\":\"Fierar\"}", provenance(profession))
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(profession, base,
                "{\"max_npcs\":\"3\",\"description\":\"Fierar\"}", provenance(profession))
        }
    }

    @Test
    fun `rejects markdown unknown keys and a different world`() {
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            "drumuri", "rute", "none")
        val base = WorldBlueprint("world")

        listOf(
            "```json\n{\"text\":\"Drumuri\"}\n```",
            "{\"text\":\"Drumuri\",\"command\":\"/op player\"}",
            "{\"text\":null}",
            "not json",
        ).forEach { response ->
            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintSuggestionParser.parse(request, base, response, provenance(request))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(request, WorldBlueprint("nether"),
                "{\"text\":\"Drumuri\"}", provenance(request))
        }
        assertTrue(base.mapRules.isEmpty())
    }

    @Test
    fun `rejects an incompatible profession type limit`() {
        val base = WorldBlueprint("world", professionTypeLimit = 2,
            professions = mapOf("fierar" to WorldProfessionBlueprint(1, "Atelier"),
                "fermier" to WorldProfessionBlueprint(1, "Ferma")))
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.PROFESSION_LIMIT,
            null, "o limita mica", "base-hash")

        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(request, base, "{\"limit\":1}", provenance(request))
        }
        assertEquals(2, base.professionTypeLimit)
    }
}
