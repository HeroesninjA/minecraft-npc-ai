package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.actions.GiveItemAction
import ro.ainpc.engine.runtime.actions.SetStoryStateAction

/**
 * Smoke test for quest-mapping integration.
 * Validates that key components compile and basic contracts hold.
 */
class QuestMappingSmokeTest {

    @Test
    fun actionHandlersHaveValidTypes() {
        assertEquals("give_item", GiveItemAction().type())
        assertEquals("set_story_state", SetStoryStateAction().type())
    }

    @Test
    fun runtimeDefinitionStoresParameters() {
        val def = ScenarioRuntimeDefinition(
            "test_action", "give_item",
            mapOf("item" to "IRON_INGOT", "amount" to "5")
        )
        assertEquals("test_action", def.id())
        assertEquals("give_item", def.type())
        assertEquals("IRON_INGOT", def.parameter("item"))
        assertEquals("5", def.parameter("amount"))
        assertEquals("", def.parameter("nonexistent"))
        assertEquals(mapOf("item" to "IRON_INGOT", "amount" to "5"), def.parameters())
    }

    @Test
    fun runtimeDefinitionHandlesNulls() {
        val def = ScenarioRuntimeDefinition(null, null, null)
        assertEquals("", def.id())
        assertEquals("", def.type())
        assertTrue(def.parameters().isEmpty())
    }

    @Test
    fun questAnchorResolverResolvedQuestAnchorStoresData() {
        val anchor = QuestAnchorResolver.ResolvedQuestAnchor(
            "objective_1", "visit_place", "", "place", "village:house_1", "Casa 1"
        )
        assertEquals("objective_1", anchor.objectiveKey())
        assertEquals("visit_place", anchor.objectiveType())
        assertEquals("place", anchor.anchorType())
        assertEquals("village:house_1", anchor.anchorId())
        assertEquals("Casa 1", anchor.label())
    }

    @Test
    fun questAnchorResolverResolvedQuestAnchorsValidWhenNoIssues() {
        val result = QuestAnchorResolver.ResolvedQuestAnchors.valid(
            listOf(QuestAnchorResolver.ResolvedQuestAnchor("k", "t", "", "r", "id", "l"))
        )
        assertTrue(result.valid())
        assertEquals(1, result.anchors().size)
        assertTrue(result.issues().isEmpty())
    }

    @Test
    fun questAnchorResolverResolvedQuestAnchorsStoresIssues() {
        val issues = listOf(
            QuestAnchorResolver.ResolutionIssue("obj1", "region", "", "Regiune negasita")
        )
        val result = QuestAnchorResolver.ResolvedQuestAnchors(emptyList(), issues)
        assertFalse(result.valid())
        assertEquals(1, result.issues().size)
        assertEquals("Regiune negasita", result.issues()[0].message())
    }

    @Test
    fun questInteractionResultHandledAndNotHandled() {
        val handled = QuestInteractionResult.handled(true, listOf("mesaj"), listOf("sistem"))
        assertTrue(handled.isHandled)
        assertEquals(1, handled.systemMessages.size)

        val notHandled = QuestInteractionResult.notHandled()
        assertFalse(notHandled.isHandled)
    }

    @Test
    fun questGuiSnapshotEmpty() {
        val empty = QuestGuiSnapshot.empty()
        assertFalse(empty.handled)
        assertTrue(empty.currentEntries.isEmpty())
        assertTrue(empty.archivedEntries.isEmpty())
    }

    @Test
    fun questLogFilterAllFilter() {
        val filter = QuestLogFilter.ALL
        assertTrue(filter.showsCurrent())
        assertTrue(filter.showsArchived())
    }

    @Test
    fun questLogFilterActiveOnlyShowsCurrent() {
        val filter = QuestLogFilter.ACTIVE
        assertTrue(filter.showsCurrent())
        assertFalse(filter.showsArchived())
    }

    @Test
    fun questLogFilterCompletedOnlyShowsArchived() {
        val filter = QuestLogFilter.COMPLETED
        assertFalse(filter.showsCurrent())
        assertTrue(filter.showsArchived())
    }
}
