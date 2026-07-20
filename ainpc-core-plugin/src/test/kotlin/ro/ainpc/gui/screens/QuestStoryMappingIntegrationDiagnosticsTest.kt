package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestStoryMappingIntegrationDiagnosticsTest {

    @Test
    fun questMapHasBindAllAndUnbindAll() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestMapGui.kt").readText()
        assertTrue(source.contains("Bind all"))
        assertTrue(source.contains("Unbind all"))
    }

    @Test
    fun questMapHasStoryContextInDetail() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestMapGui.kt").readText()
        assertTrue(source.contains("Story context"))
        assertTrue(source.contains("storyStateService.listRecentEvents"))
    }

    @Test
    fun questMapHasObjectiveFilter() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestMapGui.kt").readText()
        assertTrue(source.contains("quest_map_obj_filter"))
        assertTrue(source.contains("Filtru:"))
    }

    @Test
    fun worldHubHasStoryEvents() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldHubGui.kt").readText()
        assertTrue(source.contains("Evenimente Story Recente"))
        assertTrue(source.contains("storyStateService.listRecentEvents"))
        assertTrue(source.contains("formatStoryEventTime"))
    }

    @Test
    fun worldPlaceHasStoryEventsAndCreateEvent() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldPlaceGui.kt").readText()
        assertTrue(source.contains("Evenimente Story Recente"))
        assertTrue(source.contains("Creeaza Story Event"))
        assertTrue(source.contains("storyStateService.listRecentEvents"))
    }

    @Test
    fun worldRegionHasStoryEventsAndCreateEvent() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldRegionGui.kt").readText()
        assertTrue(source.contains("Evenimente Story Recente"))
        assertTrue(source.contains("Creeaza Story Event"))
        assertTrue(source.contains("storyStateService.listRecentEvents"))
    }

    @Test
    fun storyAuthoringShowsAnchorCountInReview() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/StoryAuthoringGui.kt").readText()
        assertTrue(source.contains("Ancore quest la aceasta locatie"))
        assertTrue(source.contains("anchorCount"))
    }

    @Test
    fun storyAuthoringShowsAnchorCountInScopeSelection() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/StoryAuthoringGui.kt").readText()
        assertTrue(source.contains("Ancore quest totale"))
        assertTrue(source.contains("Regiuni cu ancore"))
    }

    @Test
    fun storyGuiShowsQuestAnchors() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/StoryGui.kt").readText()
        assertTrue(source.contains("Ancore regiune"))
        assertTrue(source.contains("Ancore place"))
        assertTrue(source.contains("Quest Mapping"))
    }

    @Test
    fun quickQuestShowsMappingContext() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuickQuestGui.kt").readText()
        assertTrue(source.contains("Ancore quest aici"))
    }

    @Test
    fun questDetailShowsStoryEventsInMappingCard() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestDetailGui.kt").readText()
        assertTrue(source.contains("Evenimente story:"))
        assertTrue(source.contains("storyStateService.listRecentEvents"))
    }

    @Test
    fun adminHubShowsExtendedStats() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminHubGui.kt").readText()
        assertTrue(source.contains("Quest:") || source.contains("quest"))
        assertTrue(source.contains("Story:"))
    }

    @Test
    fun progressionServiceValidatesOrphanAnchors() {
        val source = File("src/main/kotlin/ro/ainpc/progression/ProgressionService.kt").readText()
        assertTrue(source.contains("Ancora orfana detectata"))
        assertTrue(source.contains("nu exista in world mapping"))
    }

    @Test
    fun auditCommandValidatesQuestAnchors() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()
        assertTrue(source.contains("validateQuestAnchorStructure"))
        assertTrue(source.contains("validateQuestAnchorTarget"))
        assertTrue(source.contains("queryQuestAnchorAuditPage"))
    }

    @Test
    fun debugDumpAuditValidatesAnchorMapping() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpQuestAudit.kt").readText()
        assertTrue(source.contains("Ancora orfana in mapping"))
        assertTrue(source.contains("Nu pot valida ancorele in world mapping"))
    }
}
