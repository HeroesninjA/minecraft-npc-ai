package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ro.ainpc.world.WorldBlueprint
import ro.ainpc.world.WorldBlueprintApproval
import ro.ainpc.world.WorldBlueprintStore
import ro.ainpc.world.WorldProfessionBlueprint
import java.nio.file.Path

class WorldBlueprintReviewDraftTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun missingAndIncompleteDraftsCannotBeConfirmed() {
        val store = WorldBlueprintStore(directory.resolve("world-blueprints.json"))

        assertNull(WorldBlueprintReviewSelection.draft(null, store::fingerprint))
        val incomplete = WorldBlueprintReviewSelection.draft(completeDraft().copy(architecture = ""), store::fingerprint)!!
        assertFalse(incomplete.canConfirm)
        assertTrue(incomplete.completionIssues.isNotEmpty())
    }

    @Test
    fun completeDraftUsesItsOwnRevisionForConfirmation() {
        val store = WorldBlueprintStore(directory.resolve("world-blueprints.json"))
        val draft = completeDraft()

        val review = WorldBlueprintReviewSelection.draft(draft, store::fingerprint)!!

        assertTrue(review.canConfirm)
        assertEquals(store.fingerprint(draft), review.fingerprint)
        assertTrue(review.completionIssues.isEmpty())
    }

    @Test
    fun onlyApprovalForTheDisplayedRevisionSuppressesConfirmation() {
        val store = WorldBlueprintStore(directory.resolve("world-blueprints.json"))
        val draft = completeDraft()
        val approved = draft.copy(approval = WorldBlueprintApproval(store.fingerprint(draft), "admin", 1L))

        val current = WorldBlueprintReviewSelection.draft(approved, store::fingerprint)!!
        val changed = WorldBlueprintReviewSelection.draft(approved.copy(story = "Poveste noua"), store::fingerprint)!!

        assertTrue(current.approved)
        assertFalse(current.canConfirm)
        assertFalse(changed.approved)
        assertTrue(changed.canConfirm)
    }

    @Test
    fun confirmationOfAnOldGuiSnapshotCannotApproveAnEditedDraft() {
        val store = WorldBlueprintStore(directory.resolve("world-blueprints.json"))
        val draft = completeDraft()
        store.save(draft)
        val review = WorldBlueprintReviewSelection.draft(store.get(draft.worldName), store::fingerprint)!!
        val changed = draft.copy(architecture = "Case din lemn")
        store.save(changed)

        assertThrows(IllegalArgumentException::class.java) {
            store.confirm(draft.worldName, review.fingerprint, "admin")
        }

        assertEquals(changed, store.get(draft.worldName))
        assertNull(store.get(draft.worldName)?.approval)
    }

    private fun completeDraft() = WorldBlueprint(
        worldName = "world",
        story = "Regat de munte",
        properties = mapOf("clima" to "Rece"),
        professions = mapOf("fierar" to WorldProfessionBlueprint(2, "Atelier")),
        architecture = "Case de piatra",
        mapRules = mapOf("drumuri" to "Leaga satele"),
    )
}
