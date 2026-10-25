package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.WorldBlueprint
import ro.ainpc.world.WorldBlueprintSavedSuggestion
import ro.ainpc.world.WorldBlueprintSuggestion
import ro.ainpc.world.WorldBlueprintSuggestionField
import ro.ainpc.world.WorldBlueprintSuggestionProvenance
import ro.ainpc.world.WorldProfessionBlueprint

class WorldBlueprintReviewSelectionTest {
    private val fingerprint: (WorldBlueprint) -> String = { "revision:${it.story}" }
    private val provenance = WorldBlueprintSuggestionProvenance("openai", "test-model", 1L, "instruction")

    @Test
    fun temporaryPreviewTakesPriorityOverSavedSuggestion() {
        val current = WorldBlueprint("world", story = "initial")
        val preview = suggestion(current, WorldBlueprintSuggestionField.STORY,
            current.copy(story = "preview"))
        val saved = suggestion(current, WorldBlueprintSuggestionField.STORY,
            current.copy(story = "saved"))

        val selection = WorldBlueprintReviewSelection.select(current, preview,
            WorldBlueprintSavedSuggestion(saved, 2L), fingerprint)

        assertEquals("preview", selection?.value)
        assertEquals("preview temporar", selection?.source)
        assertEquals("openai/test-model", selection?.provider)
        assertEquals("revision:preview", selection?.fingerprint)
        assertTrue(selection!!.currentRevision)
        assertTrue(selection.saveable)
    }

    @Test
    fun savedSuggestionMarksChangedBaseAsOutdated() {
        val base = WorldBlueprint("world", story = "initial")
        val saved = suggestion(base, WorldBlueprintSuggestionField.STORY,
            base.copy(story = "proposed"))

        val selection = WorldBlueprintReviewSelection.select(base.copy(story = "changed"), null,
            WorldBlueprintSavedSuggestion(saved, 2L), fingerprint)

        assertEquals("sugestie salvata", selection?.source)
        assertFalse(selection!!.currentRevision)
        assertFalse(selection.saveable)
    }

    @Test
    fun professionSuggestionShowsItsOwnValue() {
        val base = WorldBlueprint("world")
        val candidate = base.copy(professions = mapOf("smith" to WorldProfessionBlueprint(3, "Forging")))
        val proposal = suggestion(base, WorldBlueprintSuggestionField.PROFESSION, candidate, "smith")
            .copy(baseFingerprint = "none")

        val selection = WorldBlueprintReviewSelection.select(null, proposal, null, fingerprint)

        assertEquals("smith", selection?.key)
        assertEquals("max_npcs=3; Forging", selection?.value)
        assertTrue(selection!!.currentRevision)
        assertTrue(selection.saveable)
    }

    @Test
    fun outdatedPreviewCannotOfferSave() {
        val base = WorldBlueprint("world", story = "initial")
        val preview = suggestion(base, WorldBlueprintSuggestionField.STORY,
            base.copy(story = "proposed"))

        val selection = WorldBlueprintReviewSelection.select(base.copy(story = "changed"), preview, null, fingerprint)

        assertFalse(selection!!.saveable)
    }

    @Test
    fun savedSuggestionCannotBeSavedAgain() {
        val base = WorldBlueprint("world", story = "initial")
        val saved = suggestion(base, WorldBlueprintSuggestionField.STORY,
            base.copy(story = "proposed"))

        val selection = WorldBlueprintReviewSelection.select(base, null,
            WorldBlueprintSavedSuggestion(saved, 2L), fingerprint)

        assertTrue(selection!!.currentRevision)
        assertFalse(selection.saveable)
    }

    @Test
    fun noSuggestionProducesNoSelection() {
        assertNull(WorldBlueprintReviewSelection.select(WorldBlueprint("world"), null, null, fingerprint))
    }

    private fun suggestion(
        base: WorldBlueprint,
        field: WorldBlueprintSuggestionField,
        candidate: WorldBlueprint,
        key: String? = null,
    ) = WorldBlueprintSuggestion(base.worldName, field, key, fingerprint(base), candidate, provenance)
}
