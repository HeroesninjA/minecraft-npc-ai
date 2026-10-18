package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.ScenarioType

class DebugDumpStoryFeatureCaptureTest {
    @Test
    fun captureKeepsScenarioAndActionValuesAfterSourceChanges() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "village", "blacksmith", "Fierarul", "", ScenarioType.QUEST,
        )
        scenario.questCode = "sword"
        scenario.progressionMechanicId = "crafting"
        scenario.questScenarioKind = "investigation"
        scenario.rewards.add(FeaturePackLoader.QuestEntryDefinition(
            "record_story_event", "IRON_SWORD", 1, "Sabie vanduta",
            mapOf("event_key" to "sword_sale", "scope" to "place"),
            emptyMap(),
            mapOf("merchant" to "unknown"),
        ))

        val capture = DebugDumpStoryFeatureCapture.from(listOf(scenario))
        scenario.questCode = "changed"
        scenario.progressionMechanicId = "changed"
        scenario.rewards.clear()

        val snapshot = capture.find("village:blacksmith", null)
        assertEquals("Fierarul", snapshot?.name)
        assertEquals("crafting", snapshot?.progressionMechanicId)
        assertEquals("village", capture.find("village:crafting:blacksmith", null)?.packId)
        assertEquals("blacksmith", capture.find("other:blacksmith", null)?.id)
        assertNull(capture.find("unknown", "changed"))
        val action = snapshot?.findRecordStoryEventAction("sword_sale")
        assertEquals("Sabie vanduta", action?.description)
        assertEquals("place", action?.metadataValue("scope"))
        assertEquals("unknown", action?.payload?.get("merchant"))
    }

    @Test
    fun captureSkipsScenariosWithoutProgression() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "village", "festival", "Festival", "", ScenarioType.CELEBRATION,
        )

        assertNull(DebugDumpStoryFeatureCapture.from(listOf(scenario)).find("village:festival", null))
    }
}
