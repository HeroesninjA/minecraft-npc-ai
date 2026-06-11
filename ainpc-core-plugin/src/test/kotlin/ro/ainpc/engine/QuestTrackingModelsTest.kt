package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QuestTrackingModelsTest {
    @Test
    fun horizontalDirectionKeepsCompassBuckets() {
        assertEquals("aceeasi coloana", formatHorizontalDirection(0.2, 0.2))
        assertEquals("est", formatHorizontalDirection(10.0, 0.0))
        assertEquals("sud-est", formatHorizontalDirection(1.0, 1.0))
        assertEquals("sud", formatHorizontalDirection(0.0, 10.0))
        assertEquals("vest", formatHorizontalDirection(-10.0, 0.0))
        assertEquals("nord", formatHorizontalDirection(0.0, -10.0))
    }

    @Test
    fun verticalHintUsesRoundedFourBlockThreshold() {
        assertEquals("", formatVerticalHint(3.49))
        assertEquals("", formatVerticalHint(-3.49))
        assertEquals(" &7si cu &f4 blocuri mai sus", formatVerticalHint(4.0))
        assertEquals(" &7si cu &f4 blocuri mai jos", formatVerticalHint(-4.0))
    }

    @Test
    fun trackingCoordinatesUseRoundedBlockPositions() {
        val target = QuestTrackingTarget(
            "place",
            "market",
            "Piata",
            "world",
            12.4,
            65.5,
            -3.6,
            true,
        )

        assertEquals("world 12 66 -4", formatQuestTrackingCoordinates(target))
    }

    @Test
    fun anchorTypesNormalizeKnownValuesAndKeepCustomFallbacks() {
        assertEquals("region", normalizeTrackingAnchorType(" REGION "))
        assertEquals("regiune", formatQuestAnchorType(" REGION "))
        assertEquals("loc", formatQuestAnchorType("place"))
        assertEquals("punct", formatQuestAnchorType("node"))
        assertEquals("npc", formatQuestAnchorType("npc"))
        assertEquals("tinta", formatQuestAnchorType(null))
        assertEquals("custom-anchor", formatQuestAnchorType("custom-anchor"))
    }

    @Test
    fun centerAndQuestPhaseKeepLegacyFormatting() {
        assertEquals(5.0, center(2.0, 8.0))
        assertEquals("", formatQuestPhase(null))
        assertEquals("", formatQuestPhase(" "))
        assertEquals("Quest Ready", formatQuestPhase("QUEST-ready"))
        assertEquals("Accepted Waiting", formatQuestPhase("accepted__WAITING"))
    }
}
