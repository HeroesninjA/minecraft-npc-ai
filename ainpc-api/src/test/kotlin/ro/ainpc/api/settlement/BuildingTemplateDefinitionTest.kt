package ro.ainpc.api.settlement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuildingTemplateDefinitionTest {

    @Test
    fun buildingTemplateDefaults() {
        val tpl = BuildingTemplateDefinition(templateId = "house_small")
        assertEquals("house_small", tpl.templateId)
        assertEquals("house", tpl.placeType)
        assertEquals(5, tpl.footprintWidth)
        assertEquals(25, tpl.footprintArea())
        assertTrue(tpl.anchors.isEmpty())
    }

    @Test
    fun buildingTemplateWithAnchors() {
        val bed = BuildingAnchorDefinition(
            anchorId = "bed_01", nodeType = "bed",
            offsetX = 2, offsetY = 1, offsetZ = 3
        )
        val entrance = BuildingAnchorDefinition(
            anchorId = "entrance", nodeType = "entrance",
            offsetX = 2, offsetY = 0, offsetZ = 0,
            radius = 1.5, role = "main_door"
        )
        val tpl = BuildingTemplateDefinition(
            templateId = "house_family",
            displayName = "Casa de familie",
            placeType = "house",
            footprintWidth = 7, footprintDepth = 7, footprintHeight = 4,
            anchors = listOf(bed, entrance),
            supportedRotations = listOf(0, 90, 180, 270)
        )
        assertEquals("Casa de familie", tpl.displayName)
        assertEquals(49, tpl.footprintArea())
        assertEquals(2, tpl.anchorCount())
        assertEquals(4, tpl.supportedRotations.size)
    }

    @Test
    fun buildingVariantDefaults() {
        val v = BuildingVariantDefinition(variantId = "stone")
        assertEquals("stone", v.variantId)
        assertTrue(v.biomeFilter.isEmpty())
    }

    @Test
    fun buildingVariantWithOverrides() {
        val v = BuildingVariantDefinition(
            variantId = "plains_wood",
            displayName = "Lemn de campie",
            materialOverrides = mapOf("OAK_LOG" to "BIRCH_LOG"),
            biomeFilter = listOf("plains", "forest"),
            minDifficulty = "medium"
        )
        assertEquals("Lemn de campie", v.displayName)
        assertEquals("medium", v.minDifficulty)
        assertTrue(v.biomeFilter.contains("plains"))
    }

    @Test
    fun buildingAnchorDefaults() {
        val a = BuildingAnchorDefinition(
            anchorId = "workstation", nodeType = "workstation",
            offsetX = 3, offsetY = 1, offsetZ = 2
        )
        assertEquals(2.0, a.radius)
        assertTrue(a.tags.isEmpty())
    }

    @Test
    fun buildingTemplateWithVariants() {
        val tpl = BuildingTemplateDefinition(
            templateId = "forge",
            placeType = "forge",
            variants = listOf(
                BuildingVariantDefinition("standard"),
                BuildingVariantDefinition("rich", minDifficulty = "hard")
            )
        )
        assertEquals(2, tpl.variantCount())
    }
}
