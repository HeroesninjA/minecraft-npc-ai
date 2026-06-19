package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class WorldHubGuiSnapshotTest {
    @Test
    fun worldHubGuiExposesCompactMappingSnapshotAndDebugDumpShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/WorldHubGui.kt").readText()

        assertTrue(source.contains("mappingSnapshotLore(worldAdmin, region, place, node, nearbyNodes.size)"))
        assertTrue(source.contains("DebugDumpMappingText.buildMappingText(plugin)"))
        assertTrue(source.contains("ainpc debugdump mapping"))
    }
}
