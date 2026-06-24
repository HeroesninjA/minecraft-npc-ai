package ro.ainpc.settlement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettlementConfigLoaderTest {

    @Test
    fun parseInline() {
        val def = SettlementConfigLoader.parseInline("test_sat", "world", 0, 64, 0, 50)
        assertEquals("test_sat", def.id)
        assertEquals("world", def.worldName)
        assertEquals(50, def.radius)
        assertEquals("compact", def.profileId)
    }

    @Test
    fun parseInlineWithProfile() {
        val def = SettlementConfigLoader.parseInline("test", "world", 0, 64, 0, 100, "spacious")
        assertEquals("spacious", def.profileId)
    }

    @Test
    fun loadFromEmptyConfigReturnsEmpty() {
        val loader = SettlementConfigLoader(null)
        val config = """
            settlements:
        """.trimIndent()
        val result = loader.parseYamlString(config)
        assertTrue(result.isEmpty())
    }

    @Test
    fun loadFromYamlString() {
        val loader = SettlementConfigLoader(null)
        val yaml = """
            settlements:
              satul_central:
                world: "world"
                center:
                  x: 0
                  y: 64
                  z: 0
                radius: 50
                profile: "compact"
                region_type: "village"
                display_name: "Satul Central"
                tags:
                  - "medieval"
                  - "friendly"
                metadata:
                  population: "200"
        """.trimIndent()
        val results = loader.parseYamlString(yaml)
        assertEquals(1, results.size)
        val def = results[0]
        assertEquals("satul_central", def.id)
        assertEquals("world", def.worldName)
        assertEquals(50, def.radius)
        assertEquals("compact", def.profileId)
        assertEquals("Satul Central", def.displayName)
        assertTrue(def.tags.contains("medieval"))
        assertEquals("200", def.metadata["population"])
    }

    @Test
    fun loadMultipleSettlements() {
        val loader = SettlementConfigLoader(null)
        val yaml = """
            settlements:
              satul_1:
                world: "world"
                center: { x: 0, y: 64, z: 0 }
                radius: 30
              satul_2:
                world: "world_nether"
                center: { x: 100, y: 32, z: -50 }
                radius: 40
                profile: "rural"
        """.trimIndent()
        val results = loader.parseYamlString(yaml)
        assertEquals(2, results.size)
        assertEquals("satul_1", results[0].id)
        assertEquals("satul_2", results[1].id)
        assertEquals("rural", results[1].profileId)
    }

    @Test
    fun missingWorldEmitsError() {
        val loader = SettlementConfigLoader(null)
        val yaml = """
            settlements:
              bad:
                radius: 30
        """.trimIndent()
        val results = loader.parseYamlString(yaml)
        assertTrue(results.isEmpty())
        assertTrue(loader.getErrors().any { it.contains("world") })
    }

    @Test
    fun zeroRadiusEmitsError() {
        val loader = SettlementConfigLoader(null)
        val yaml = """
            settlements:
              bad:
                world: "world"
                radius: 0
        """.trimIndent()
        val results = loader.parseYamlString(yaml)
        assertTrue(results.isEmpty())
        assertTrue(loader.getErrors().any { it.contains("radius") })
    }

    @Test
    fun getDefinitionById() {
        val loader = SettlementConfigLoader(null)
        val yaml = """
            settlements:
              test: { world: "w", center: { x: 0, y: 64, z: 0 }, radius: 10 }
        """.trimIndent()
        loader.parseYamlString(yaml)
        val def = loader.getDefinition("test")
        assertTrue(def != null)
        assertEquals("test", def!!.id)
    }

    @Test
    fun getAllDefinitions() {
        val loader = SettlementConfigLoader(null)
        val yaml = """
            settlements:
              a: { world: "w", center: { x: 0, y: 64, z: 0 }, radius: 10 }
              b: { world: "w", center: { x: 10, y: 64, z: 10 }, radius: 20 }
        """.trimIndent()
        loader.parseYamlString(yaml)
        assertEquals(2, loader.getAllDefinitions().size)
    }
}
