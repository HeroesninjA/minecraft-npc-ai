package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class WorldBlueprintWorldDirectoryInspectorTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `reports available world directory without creating it`() {
        val candidate = directory.resolve("new_world")

        assertEquals(WorldBlueprintWorldDirectoryStatus.AVAILABLE,
            WorldBlueprintWorldDirectoryInspector.inspect(directory, "new_world"))
        assertFalse(Files.exists(candidate))
    }

    @Test
    fun `distinguishes existing world folder file and unavailable container`() {
        Files.createDirectory(directory.resolve("existing_world"))
        Files.createFile(directory.resolve("occupied_name"))

        assertEquals(WorldBlueprintWorldDirectoryStatus.EXISTING_DIRECTORY,
            WorldBlueprintWorldDirectoryInspector.inspect(directory, "existing_world"))
        assertEquals(WorldBlueprintWorldDirectoryStatus.EXISTING_FILE,
            WorldBlueprintWorldDirectoryInspector.inspect(directory, "occupied_name"))
        assertEquals(WorldBlueprintWorldDirectoryStatus.CONTAINER_UNAVAILABLE,
            WorldBlueprintWorldDirectoryInspector.inspect(directory.resolve("missing"), "new_world"))
    }

    @Test
    fun `rejects non portable names before inspecting paths`() {
        for (name in listOf("../escape", "nested/world", "name\\child", "a b", "CON", "lpt1", "-world", "a".repeat(65))) {
            assertEquals(WorldBlueprintWorldDirectoryStatus.UNSAFE_NAME,
                WorldBlueprintWorldDirectoryInspector.inspect(directory, name), name)
        }
        assertFalse(Files.exists(directory.resolve("escape")))
    }
}
