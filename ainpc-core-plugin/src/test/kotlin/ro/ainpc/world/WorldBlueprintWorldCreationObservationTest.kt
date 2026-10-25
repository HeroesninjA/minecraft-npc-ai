package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorldBlueprintWorldCreationObservationTest {
    @Test
    fun `loaded world takes precedence over directory evidence`() {
        assertEquals(WorldBlueprintWorldCreationObservation.LOADED,
            WorldBlueprintWorldCreationObservation.from(true, WorldBlueprintWorldDirectoryStatus.AVAILABLE))
    }

    @Test
    fun `distinguishes directory other entry absence and uncertainty`() {
        val expected = mapOf(
            WorldBlueprintWorldDirectoryStatus.EXISTING_DIRECTORY to WorldBlueprintWorldCreationObservation.DIRECTORY_PRESENT,
            WorldBlueprintWorldDirectoryStatus.EXISTING_FILE to WorldBlueprintWorldCreationObservation.OTHER_ENTRY,
            WorldBlueprintWorldDirectoryStatus.SYMBOLIC_LINK to WorldBlueprintWorldCreationObservation.OTHER_ENTRY,
            WorldBlueprintWorldDirectoryStatus.AVAILABLE to WorldBlueprintWorldCreationObservation.ENTRY_ABSENT,
            WorldBlueprintWorldDirectoryStatus.UNKNOWN to WorldBlueprintWorldCreationObservation.UNKNOWN,
            WorldBlueprintWorldDirectoryStatus.UNSAFE_NAME to WorldBlueprintWorldCreationObservation.UNKNOWN,
            WorldBlueprintWorldDirectoryStatus.CONTAINER_UNAVAILABLE to WorldBlueprintWorldCreationObservation.UNKNOWN,
        )
        expected.forEach { (directory, observation) ->
            assertEquals(observation, WorldBlueprintWorldCreationObservation.from(false, directory), directory.name)
        }
    }
}
