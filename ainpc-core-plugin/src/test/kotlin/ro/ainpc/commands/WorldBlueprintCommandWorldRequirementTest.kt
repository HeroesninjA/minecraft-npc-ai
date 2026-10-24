package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintCommandWorldRequirementTest {
    @Test
    fun `draft preparation commands do not require a loaded world`() {
        listOf("show", "inspect", "validate", "review", "confirm", "create-world", "creation", "revoke", "wizard", "suggest",
            "story", "property", "profession-limit", "profession", "architecture", "map-rule", "remove",
        ).forEach { action ->
            assertFalse(WorldBlueprintCommand.requiresLoadedWorld(action,
                arrayOf("world", "blueprint", "planned_world", action)), action)
        }
        assertFalse(WorldBlueprintCommand.requiresLoadedWorld("plan",
            arrayOf("world", "blueprint", "planned_world", "plan")))
    }

    @Test
    fun `structure previews and runtime story operations still require a loaded world`() {
        listOf("apply-story", "apply-story-all", "apply-story-batch", "update-story", "update-story-batch",
            "restore-story", "restore-story-batch", "applications",
        ).forEach { action ->
            assertTrue(WorldBlueprintCommand.requiresLoadedWorld(action,
                arrayOf("world", "blueprint", "planned_world", action)), action)
        }
        assertTrue(WorldBlueprintCommand.requiresLoadedWorld("plan",
            arrayOf("world", "blueprint", "planned_world", "plan", "STRUCTURE", "castle", "keep")))
        assertTrue(WorldBlueprintCommand.requiresLoadedWorld("plan", arrayOf("world", "blueprint", "planned_world",
            "plan", "structure", "castle", "keep", "bounds", "0", "64", "0", "4", "68", "4",
            "export", "a".repeat(64))))
        assertTrue(WorldBlueprintCommand.requiresLoadedWorld("plan", arrayOf("world", "blueprint", "planned_world",
            "plan", "structure", "castle", "keep", "bounds", "0", "64", "0", "4", "68", "4",
            "VERIFY", "a".repeat(64))))
    }
}
