package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Integration-style test for quest-mapping data flow.
 * Validates that anchor bindings, quest anchors, and interaction results
 * work together correctly without requiring a live Bukkit server.
 */
class QuestMappingIntegrationTest {

    @Test
    fun questAnchorFromBindingMatchesObjective() {
        val anchor = QuestAnchorResolver.ResolvedQuestAnchor(
            "collect_iron", "visit_place", "blacksmith_tag",
            "place", "village:house_1", "Casa 1"
        )
        assertEquals("collect_iron", anchor.objectiveKey())
        assertEquals("visit_place", anchor.objectiveType())
        assertEquals("place", anchor.anchorType())
        assertEquals("village:house_1", anchor.anchorId())
    }

    @Test
    fun resolvedQuestAnchorsConvertedToVariables() {
        val anchors = QuestAnchorResolver.ResolvedQuestAnchors.valid(listOf(
            QuestAnchorResolver.ResolvedQuestAnchor(
                "obj1", "visit_place", "", "place", "village:market", "Piata"
            ),
            QuestAnchorResolver.ResolvedQuestAnchor(
                "obj2", "inspect_node", "", "node", "village:quest_board", "Avizier"
            )
        ))
        val variables = anchors.toQuestVariables()
        assertTrue(variables.containsKey("quest_anchor_count"))
        assertEquals("2", variables["quest_anchor_count"])
        assertEquals("visit_place", variables["anchor.obj1.objective_type"])
        assertEquals("place", variables["anchor.obj1.type"])
        assertEquals("village:quest_board", variables["anchor.obj2.id"])
    }

    @Test
    fun questInteractionHandledMessageFlow() {
        val result = QuestInteractionResult.handled(
            openConversation = true,
            npcMessages = listOf("Bine ai venit!", "Ce pot face pentru tine?"),
            systemMessages = listOf("&aQuest oferit: Colecteaza fier")
        )
        assertTrue(result.isHandled)
        assertTrue(result.shouldOpenConversation())
        assertEquals(2, result.npcMessages.size)
        assertEquals(1, result.systemMessages.size)
    }

    @Test
    fun questInteractionNotHandledReturnsEmpty() {
        val result = QuestInteractionResult.notHandled()
        assertFalse(result.isHandled)
        assertFalse(result.shouldOpenConversation())
        assertTrue(result.npcMessages.isEmpty())
        assertTrue(result.systemMessages.isEmpty())
    }

    @Test
    fun questGuiSnapshotGroupsEntries() {
        @Suppress("UNUSED_VARIABLE")
        val entry1 = QuestGuiEntry(
            selector = "Q01", templateId = "template:medieval:Q01", questCode = "Q01",
            title = "Iron Gathering", statusDisplay = "ACTIVA", active = true, current = true
        )
        @Suppress("UNUSED_VARIABLE")
        val entry2 = QuestGuiEntry(
            selector = "Q03", templateId = "template:medieval:Q03", questCode = "Q03",
            title = "Road Safety", statusDisplay = "ACTIVA", active = true, current = true
        )
        val snapshot = QuestGuiSnapshot(
            handled = true, playerName = "Steve", filterLabel = "active",
            currentEntries = listOf(entry1, entry2)
        )
        assertTrue(snapshot.handled)
        assertEquals("Steve", snapshot.playerName)
        assertEquals(2, snapshot.currentEntries.size)
        assertEquals(0, snapshot.archivedEntries.size)
        assertTrue(snapshot.allEntries().size == 2)
    }

    @Test
    fun questAvailabilityTracksPrerequisites() {
        val allowed = QuestAvailability.allowed()
        assertTrue(allowed.available())

        val blocked = QuestAvailability.unavailable(listOf("Q01", "Q02"))
        assertFalse(blocked.available())
        assertEquals(2, blocked.issues().size)
    }

    @Test
    fun questLogFlowsThroughFilters() {
        assertEquals(QuestLogFilter.ALL, parseQuestLogFilter("all"))
        assertEquals(QuestLogFilter.ACTIVE, parseQuestLogFilter("active"))
        assertEquals(QuestLogFilter.COMPLETED, parseQuestLogFilter("completed"))
        assertEquals(QuestLogFilter.ARCHIVED, parseQuestLogFilter("archived"))
    }
}
