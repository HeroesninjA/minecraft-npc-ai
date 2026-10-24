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
    fun `strict response format matches each locally validated suggestion shape`() {
        WorldBlueprintSuggestionField.entries.forEach { field ->
            val format = WorldBlueprintSuggestionParser.responseFormat(field)
            val schema = format.getAsJsonObject("schema")
            val expected = when (field) {
                WorldBlueprintSuggestionField.PROFESSION_LIMIT -> mapOf("limit" to "integer")
                WorldBlueprintSuggestionField.PROFESSION -> mapOf("max_npcs" to "integer", "description" to "string")
                else -> mapOf("text" to "string")
            }
            assertEquals("json_schema", format.get("type").asString)
            assertTrue(format.get("strict").asBoolean)
            assertEquals("object", schema.get("type").asString)
            assertEquals(false, schema.get("additionalProperties").asBoolean)
            assertEquals(expected.keys, schema.getAsJsonArray("required").map { it.asString }.toSet())
            assertEquals(expected, schema.getAsJsonObject("properties").entrySet()
                .associate { it.key to it.value.asJsonObject.get("type").asString })
        }
    }

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

    @Test
    fun `validates only the suggested structured rule and keeps custom rules declarative`() {
        val base = WorldBlueprint("world", mapRules = mapOf("required_region_types" to "unknown"))
        val structured = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            "required_place_types", "locuri", "base-hash")
        val custom = structured.copy(key = "drumuri")

        assertTrue(WorldBlueprintSuggestionParser.prompt(structured, base).contains("house,forge"))
        assertEquals("house,forge", WorldBlueprintSuggestionParser.parse(structured, base,
            "{\"text\":\"house,forge\"}", provenance(structured)).candidate.mapRules["required_place_types"])
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(structured, base,
                "{\"text\":\"house,unknown\"}", provenance(structured))
        }
        assertEquals("Evita lacurile", WorldBlueprintSuggestionParser.parse(custom, base,
            "{\"text\":\"Evita lacurile\"}", provenance(custom)).candidate.mapRules["drumuri"])
        assertEquals(mapOf("required_region_types" to "unknown"), base.mapRules)
    }

    @Test
    fun `prompt includes only a bounded current target value`() {
        val base = WorldBlueprint("world", story = "PRIVATE_STORY", properties = mapOf(
            "clima" to "Rece", "secret" to "PRIVATE_PROPERTY"))
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.PROPERTY,
            "clima", "Schimba clima", "base-hash")

        val prompt = WorldBlueprintSuggestionParser.prompt(request, base)
        assertTrue(prompt.contains("\"value\":\"Rece\""))
        assertTrue(prompt.contains("\"truncated\":false"))
        assertTrue(prompt.contains("Schimba clima"))
        assertTrue(!prompt.contains("PRIVATE_STORY"))
        assertTrue(!prompt.contains("PRIVATE_PROPERTY"))

        val storyRequest = request.copy(field = WorldBlueprintSuggestionField.STORY, key = null)
        val longStory = "A".repeat(512) + "PRIVATE_TAIL"
        val storyPrompt = WorldBlueprintSuggestionParser.prompt(storyRequest, base.copy(story = longStory))
        assertTrue(storyPrompt.contains("\"value\":\"${"A".repeat(512)}\""))
        assertTrue(storyPrompt.contains("\"truncated\":true"))
        assertTrue(!storyPrompt.contains("PRIVATE_TAIL"))
        val unicodePrompt = WorldBlueprintSuggestionParser.prompt(storyRequest,
            base.copy(story = "A".repeat(511) + "🏰" + "PRIVATE_TAIL"))
        assertTrue(unicodePrompt.contains("🏰"))
        assertTrue(!unicodePrompt.contains("PRIVATE_TAIL"))
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.prompt(request, base.copy(worldName = "nether"))
        }
    }

    @Test
    fun `rejects invalid numeric structured rules before preview`() {
        listOf("max_structure_non_air_positions", "max_structure_fluid_positions").forEach { key ->
            val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
                key, "teren", "none")

            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintSuggestionParser.parse(request, WorldBlueprint("world"),
                    "{\"text\":\"-1\"}", provenance(request))
            }
        }
    }

    @Test
    fun `rejects invalid type style mapping before AI preview`() {
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            WorldBlueprintStructureStyles.KEY, "stiluri", "none")

        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(request, WorldBlueprint("world"),
                "{\"text\":\"castle=Turnuri;castle=Ziduri\"}", provenance(request))
        }
    }

    @Test
    fun `rejects unsupported base sample limits before AI preview`() {
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            WorldBlueprintBaseSupportRule.KEY, "baza", "none")

        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(request, WorldBlueprint("world"),
                "{\"text\":\"6\"}", provenance(request))
        }
    }

    @Test
    fun `rejects unsupported full base limits before AI preview`() {
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            WorldBlueprintBaseUnsupportedPositionsRule.KEY, "baza", "none")

        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(request, WorldBlueprint("world"),
                "{\"text\":\"18446744073709551617\"}", provenance(request))
        }
    }

    @Test
    fun `rejects incomplete architecture material roles before AI preview`() {
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            WorldBlueprintArchitectureMaterials.KEY, "materiale", "none")

        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(request, WorldBlueprint("world"),
                "{\"text\":\"floor=stone\"}", provenance(request))
        }
    }

    @Test
    fun `rejects unsupported architecture entrance direction before AI preview`() {
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            WorldBlueprintArchitectureEntrance.KEY, "intrare", "none")

        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(request, WorldBlueprint("world"),
                "{\"text\":\"up\"}", provenance(request))
        }
    }

    @Test
    fun `rejects ambiguous interior clearance flag before AI preview`() {
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            WorldBlueprintArchitectureInteriorClearance.KEY, "interior", "none")

        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(request, WorldBlueprint("world"),
                "{\"text\":\"yes\"}", provenance(request))
        }
    }
}
