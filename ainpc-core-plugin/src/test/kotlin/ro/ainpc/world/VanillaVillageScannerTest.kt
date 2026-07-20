package ro.ainpc.world

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.scan.VanillaVillageScanCursor
import ro.ainpc.world.scan.VanillaVillageScanSession
import ro.ainpc.world.scan.VanillaVillageScanService
import ro.ainpc.world.scan.VanillaVillageFeatureType
import ro.ainpc.world.scan.normalizeVillageScanBlockBudget
import java.io.File

class VanillaVillageScannerTest {
    @Test
    fun cursorResumesWithoutExceedingTheBlockBudget() {
        val cursor = VanillaVillageScanCursor(
            minX = 1,
            maxX = 2,
            minY = 10,
            maxY = 11,
            minZ = 20,
            maxZ = 21,
        )
        val visited = mutableListOf<Triple<Int, Int, Int>>()

        assertEquals(3, cursor.consume(3) { x, y, z -> visited.add(Triple(x, y, z)) })
        assertEquals(3L, cursor.scannedBlocks)
        assertFalse(cursor.isComplete)
        assertEquals(
            listOf(Triple(1, 10, 20), Triple(1, 11, 20), Triple(1, 10, 21)),
            visited,
        )

        assertEquals(2, cursor.consume(2) { x, y, z -> visited.add(Triple(x, y, z)) })
        assertEquals(3, cursor.consume(99) { x, y, z -> visited.add(Triple(x, y, z)) })
        assertEquals(8L, cursor.totalBlocks)
        assertEquals(8L, cursor.scannedBlocks)
        assertTrue(cursor.isComplete)
        assertEquals(0, cursor.consume(1) { _, _, _ -> error("Cursorul finalizat nu trebuie sa mai consume.") })
        assertEquals(8, visited.distinct().size)
    }

    @Test
    fun cursorAndConfiguredBudgetRejectUnboundedWork() {
        val cursor = VanillaVillageScanCursor(0, 0, 0, 0, 0, 0)

        assertThrows(IllegalArgumentException::class.java) {
            cursor.consume(0) { _, _, _ -> }
        }
        assertEquals(
            VanillaVillageScanService.MIN_BLOCKS_PER_TICK,
            normalizeVillageScanBlockBudget(Int.MIN_VALUE),
        )
        assertEquals(
            VanillaVillageScanService.DEFAULT_BLOCKS_PER_TICK,
            normalizeVillageScanBlockBudget(VanillaVillageScanService.DEFAULT_BLOCKS_PER_TICK),
        )
        assertEquals(
            VanillaVillageScanService.MAX_BLOCKS_PER_TICK,
            normalizeVillageScanBlockBudget(Int.MAX_VALUE),
        )
    }

    @Test
    fun sessionPublishesSignalsOnlyAfterEveryBudgetedStepCompletes() {
        val session = VanillaVillageScanSession(
            worldName = "world",
            centerX = 0,
            centerY = 1,
            centerZ = 0,
            horizontalRadius = 1,
            verticalRadius = 1,
            minY = 0,
            maxY = 1,
            horizontalRadiusClamped = false,
            verticalRadiusClamped = false,
            materialAt = { x, y, z ->
                if (x == -1 && y == 0 && z == -1) Material.BELL else Material.AIR
            },
            classifier = { material ->
                if (material == Material.BELL) VanillaVillageFeatureType.BELL else null
            },
        )

        assertThrows(IllegalStateException::class.java) { session.result() }
        assertEquals(7, session.scanNext(7))
        assertEquals(7L, session.scannedBlocks)
        assertFalse(session.isComplete)
        assertEquals(11, session.scanNext(100))

        val result = session.result()
        assertTrue(session.isComplete)
        assertEquals(18L, session.totalBlocks)
        assertEquals(1, result.count(VanillaVillageFeatureType.BELL))
        assertTrue(result.warnings().isEmpty())
    }

    @Test
    fun productionCommandsUseTheSharedSynchronousScanQueue() {
        val serviceSource = File("src/main/kotlin/ro/ainpc/world/scan/VanillaVillageScanService.kt").readText()
        val worldCommandSource = File("src/main/kotlin/ro/ainpc/commands/AINPCCommandWorld.kt").readText()
        val commandSource = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()
        val generatorSource = File("src/main/kotlin/ro/ainpc/spawn/AutoSettlementGenerator.kt").readText()

        assertTrue(serviceSource.contains("runTaskTimer(plugin"))
        assertTrue(serviceSource.contains("Bukkit.isPrimaryThread()"))
        assertFalse(serviceSource.contains("runTaskTimerAsynchronously"))
        assertTrue(worldCommandSource.contains("vanillaVillageScanService.submit("))
        assertTrue(commandSource.contains("vanillaVillageScanService.submit("))
        assertTrue(worldCommandSource.contains("ensureDeferredVillageMappingAllowed(sender"))
        assertTrue(commandSource.contains("ensureDeferredVillageMappingAllowed(sender"))
        assertTrue(generatorSource.contains("fun generateFromScan("))
        assertFalse(generatorSource.contains("VanillaVillageScanner"))
    }
}
