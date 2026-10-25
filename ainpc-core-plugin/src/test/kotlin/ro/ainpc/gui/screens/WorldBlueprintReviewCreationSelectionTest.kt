package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.WorldBlueprintWorldCreationAttempt
import ro.ainpc.world.WorldBlueprintWorldCreationStatus
import ro.ainpc.world.WorldBlueprint
import java.io.IOException
import java.util.UUID

class WorldBlueprintReviewCreationSelectionTest {
    @Test
    fun `history summarizes latest creation state without treating missing data as success`() {
        assertTrue(WorldBlueprintReviewCreationSelection.historyLine(Result.success(null)).contains("Nicio incercare"))
        assertTrue(WorldBlueprintReviewCreationSelection.historyLine(
            Result.success(attempt(WorldBlueprintWorldCreationStatus.PENDING))).contains("PENDING"))
        assertTrue(WorldBlueprintReviewCreationSelection.historyLine(
            Result.success(attempt(WorldBlueprintWorldCreationStatus.CREATED))).contains("CREATED"))
        assertTrue(WorldBlueprintReviewCreationSelection.historyLine(
            Result.success(attempt(WorldBlueprintWorldCreationStatus.FAILED))).contains("FAILED"))
        assertTrue(WorldBlueprintReviewCreationSelection.historyLine(
            Result.failure<WorldBlueprintWorldCreationAttempt?>(IOException("invalid"))).contains("nu poate fi citit"))
    }

    @Test
    fun `generation preview distinguishes configured options from server defaults`() {
        val configured = WorldBlueprint("new_world", mapRules = mapOf(
            "world_seed" to "-42", "world_type" to "flat", "world_environment" to "normal",
            "world_generate_structures" to "false",
        ))

        val lines = WorldBlueprintReviewCreationSelection.generationLines(configured)
        val defaults = WorldBlueprintReviewCreationSelection.generationLines(WorldBlueprint("new_world"))

        assertTrue(lines.any { "Seed: -42" in it })
        assertTrue(lines.any { "Tip: flat; dimensiune: normal" in it })
        assertTrue(lines.any { "Structuri vanilla: false" in it })
        assertTrue(defaults.all { "implicit server" in it })
    }

    @Test
    fun `pending latest attempt prevents another ordinary creation`() {
        val pending = attempt(WorldBlueprintWorldCreationStatus.PENDING)

        assertTrue(WorldBlueprintReviewCreationSelection.issue(Result.success(pending))?.contains("PENDING") == true)
    }

    @Test
    fun `unreadable journal fails closed`() {
        assertTrue(WorldBlueprintReviewCreationSelection.issue(Result.failure(IOException("invalid")))
            ?.contains("nu poate fi citit") == true)
    }

    @Test
    fun `empty or finalized journal allows command preflight to decide`() {
        assertNull(WorldBlueprintReviewCreationSelection.issue(Result.success(null)))
        assertNull(WorldBlueprintReviewCreationSelection.issue(
            Result.success(attempt(WorldBlueprintWorldCreationStatus.FAILED))))
        assertNull(WorldBlueprintReviewCreationSelection.issue(
            Result.success(attempt(WorldBlueprintWorldCreationStatus.CREATED))))
    }

    private fun attempt(status: WorldBlueprintWorldCreationStatus) = WorldBlueprintWorldCreationAttempt(
        UUID.randomUUID(), "new_world", "a".repeat(64), "admin", 1L, status,
        if (status == WorldBlueprintWorldCreationStatus.PENDING) null else 2L,
    )
}
