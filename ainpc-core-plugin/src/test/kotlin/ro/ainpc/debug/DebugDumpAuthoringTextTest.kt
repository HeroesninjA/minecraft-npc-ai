package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.QuestAuthoringService
import ro.ainpc.progression.ProgressionGuiSnapshot
import ro.ainpc.story.StoryContextSnapshot

class DebugDumpAuthoringTextTest {
    @Test
    fun rendersAuthoringSnapshotText() {
        val snapshot = QuestAuthoringService().analyze(
            StoryContextSnapshot.empty(),
            emptyList(),
            "quest_alpha",
            "mechanic_beta",
            null,
            null,
            false,
            listOf("mapping blocked")
        )

        val text = DebugDumpAuthoringText.buildText(
            snapshot,
            StoryContextSnapshot.empty(),
            ProgressionGuiSnapshot.empty()
        )

        assertTrue(text.contains("AINPC Quest Authoring Dump"))
        assertTrue(text.contains("Requested selector: quest_alpha"))
        assertTrue(text.contains("Requested mechanic: mechanic_beta"))
        assertTrue(text.contains("Decision runtime executable: false"))
        assertTrue(text.contains("Candidate templates: []"))
        assertTrue(text.contains("Blocked reasons: [mapping blocked]"))
        assertTrue(text.contains("Warnings:"))
        assertTrue(text.contains("mapping blocked"))
        assertTrue(text.contains("Seed story mode: no_story"))
    }
}
