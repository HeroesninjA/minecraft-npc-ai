package ro.ainpc.progression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProgressionAnchorBindingTest {

    @Test
    fun anchorBindingStoresAllFields() {
        val now = System.currentTimeMillis()
        val binding = ProgressionAnchorBinding(
            "player-uuid-123", "template:Q01", "collect_iron",
            "Q01", "collect_item", "IRON_INGOT",
            "place", "village:house_1", "Casa 1",
            now, now, "active"
        )
        assertEquals("player-uuid-123", binding.playerUuid())
        assertEquals("template:Q01", binding.templateId())
        assertEquals("collect_iron", binding.objectiveKey())
        assertEquals("Q01", binding.questCode())
        assertEquals("collect_item", binding.objectiveType())
        assertEquals("IRON_INGOT", binding.reference())
        assertEquals("place", binding.anchorType())
        assertEquals("village:house_1", binding.anchorId())
        assertEquals("Casa 1", binding.anchorLabel())
        assertEquals(now, binding.createdAt())
        assertEquals(now, binding.updatedAt())
        assertEquals("active", binding.status())
    }

    @Test
    fun anchorSelectorReturnsTypeId() {
        val binding = ProgressionAnchorBinding(
            "", "t", "k", "", "", "",
            "region", "demo_village", "",
            0, 0, ""
        )
        assertEquals("region:demo_village", binding.anchorSelector())
    }

    @Test
    fun anchorSelectorReturnsEmptyForBlank() {
        val binding = ProgressionAnchorBinding(
            "", "t", "k", "", "", "",
            "", "", "", 0, 0, ""
        )
        assertEquals("", binding.anchorSelector())
    }

    @Test
    fun displayLabelPrefersAnchorLabel() {
        val binding = ProgressionAnchorBinding(
            "", "t", "k", "", "", "",
            "place", "house_1", "Casa Frumoasa",
            0, 0, ""
        )
        assertEquals("Casa Frumoasa", binding.displayLabel())
        assertEquals("anchorLabel", binding.displayLabelSource())
    }

    @Test
    fun displayLabelFallsBackToReference() {
        val binding = ProgressionAnchorBinding(
            "", "t", "k", "", "visit_place", "house_ref",
            "place", "house_1", "",
            0, 0, ""
        )
        assertEquals("house_ref", binding.displayLabel())
        assertEquals("reference", binding.displayLabelSource())
    }

    @Test
    fun displayLabelFallsBackToAnchorId() {
        val binding = ProgressionAnchorBinding(
            "", "t", "k", "", "", "",
            "place", "house_1", "",
            0, 0, ""
        )
        assertEquals("house_1", binding.displayLabel())
        assertEquals("anchorId", binding.displayLabelSource())
    }

    @Test
    fun matchesAnchorMatchesCorrectly() {
        val binding = ProgressionAnchorBinding(
            "", "t", "k", "", "", "",
            "place", "village:house_1", "",
            0, 0, ""
        )
        assertTrue(binding.matchesAnchor("place", "village:house_1"))
        assertFalse(binding.matchesAnchor("region", "village:house_1"))
        assertFalse(binding.matchesAnchor("place", "wrong_id"))
    }
}
