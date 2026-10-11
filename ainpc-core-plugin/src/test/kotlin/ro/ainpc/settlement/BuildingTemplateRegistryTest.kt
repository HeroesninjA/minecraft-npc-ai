package ro.ainpc.settlement

import java.nio.file.Path
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ro.ainpc.api.settlement.BuildingTemplateDefinition

class BuildingTemplateRegistryTest {
    @TempDir
    lateinit var directory: Path

    @BeforeEach
    fun setUp() = BuildingTemplateRegistry.clear()

    @AfterEach
    fun tearDown() = BuildingTemplateRegistry.clear()

    @Test
    fun customCatalogSurvivesListingAndRestartWithoutRewritingTheFile() {
        val file = directory.resolve("building_templates.yml").toFile()
        val content = templateYaml("house_small", 9)
        file.writeText(content)
        BuildingTemplateRegistry.reload(file)

        BuildingTemplateRegistry.loadDefaults()
        assertEquals(9, BuildingTemplateRegistry.get("house_small")?.footprintWidth)
        assertNull(BuildingTemplateRegistry.get("forge"))

        BuildingTemplateRegistry.clear()
        BuildingTemplateRegistry.reload(file)
        assertEquals(content, file.readText())
        assertEquals(9, BuildingTemplateRegistry.get("house_small")?.footprintWidth)
    }

    @Test
    fun failedReloadRetainsTheWholePreviousCatalog() {
        val file = directory.resolve("building_templates.yml").toFile()
        file.writeText(templateYaml("custom", 9))
        BuildingTemplateRegistry.reload(file)
        val previous = BuildingTemplateRegistry.getAll()

        for (invalidContent in listOf(templateYaml("replacement", -1), "profiles: [")) {
            file.writeText(invalidContent)
            val exception = assertThrows(IllegalArgumentException::class.java) { BuildingTemplateRegistry.reload(file) }
            assertTrue(exception.message.orEmpty().contains("building_templates.yml"))
            assertEquals(previous, BuildingTemplateRegistry.getAll())
        }
        assertTrue(file.delete())
        assertThrows(IllegalArgumentException::class.java) { BuildingTemplateRegistry.reload(file) }
        assertEquals(previous, BuildingTemplateRegistry.getAll())
    }

    @Test
    fun reloadRemovesDeletedFileEntriesAndPreservesProgrammaticTemplates() {
        val file = directory.resolve("building_templates.yml").toFile()
        file.writeText(templateYaml("old", 9))
        BuildingTemplateRegistry.reload(file)
        BuildingTemplateRegistry.register(BuildingTemplateDefinition("addon_house"))
        BuildingTemplateRegistry.register(BuildingTemplateDefinition("new", footprintWidth = 12))

        file.writeText(templateYaml("new", 6))
        BuildingTemplateRegistry.reload(file)

        assertNull(BuildingTemplateRegistry.get("old"))
        assertNotNull(BuildingTemplateRegistry.get("addon_house"))
        assertEquals(12, BuildingTemplateRegistry.get("new")?.footprintWidth)
        assertEquals(2, BuildingTemplateRegistry.count())
    }

    @Test
    fun explicitEmptyCatalogIsNotRepopulatedByDefaults() {
        val file = directory.resolve("building_templates.yml").toFile()
        file.writeText("profiles: {}")
        BuildingTemplateRegistry.reload(file)

        BuildingTemplateRegistry.loadDefaults()

        assertEquals(0, BuildingTemplateRegistry.count())
    }

    private fun templateYaml(id: String, width: Int): String = """
        profiles:
          $id:
            place_type: house
            footprint: {width: $width, depth: 6, height: 4}
    """.trimIndent()
}
