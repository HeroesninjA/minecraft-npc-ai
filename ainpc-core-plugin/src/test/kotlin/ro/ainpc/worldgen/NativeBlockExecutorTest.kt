package ro.ainpc.worldgen

import java.lang.reflect.Proxy
import java.util.UUID
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NativeBlockExecutorTest {
    @Test
    fun undoRestoresOriginalStateOnceAfterRepeatedWrites() {
        val fixture = Fixture()
        val result = fixture.executor.execute(listOf(operation(Material.STONE), operation(Material.OAK_PLANKS)))
        assertTrue(result.success)
        assertEquals(Material.OAK_PLANKS, fixture.material)
        assertEquals(1, fixture.snapshots)

        val undo = fixture.executor.undo(result.sessionId!!)

        assertTrue(undo.success)
        assertEquals(1, undo.blocksRestored)
        assertEquals(Material.DIRT, fixture.material)
        assertEquals(1, fixture.restores)
        assertFalse(fixture.executor.undo(result.sessionId).success)
    }

    @Test
    fun previewNeverProvidesDestructiveUndo() {
        val fixture = Fixture()
        val preview = fixture.executor.executeWithPreview(listOf(operation(Material.STONE)))

        assertTrue(preview.isValid())
        assertFalse(fixture.executor.undo(preview.id).success)
        assertEquals(Material.DIRT, fixture.material)
        assertEquals(0, fixture.snapshots)
    }

    @Test
    fun failedRestorationCanBeRetried() {
        val fixture = Fixture()
        val result = fixture.executor.execute(listOf(operation(Material.STONE)))
        fixture.restoreAllowed = false
        assertFalse(fixture.executor.undo(result.sessionId!!).success)
        assertEquals(Material.STONE, fixture.material)
        fixture.restoreAllowed = true
        assertTrue(fixture.executor.undo(result.sessionId).success)
        assertEquals(Material.DIRT, fixture.material)
    }

    @Test
    fun partialFailureIsNotSuccessAndKeepsUndo() {
        val fixture = Fixture()
        val result = fixture.executor.execute(listOf(operation(Material.STONE), operation(Material.AIR).copy(worldName = "missing")))

        assertFalse(result.success)
        assertEquals(1, result.blocksPlaced)
        assertEquals(1, result.blocksFailed)
        assertTrue(fixture.executor.undo(result.sessionId!!).success)
        assertEquals(Material.DIRT, fixture.material)
    }

    @Test
    fun rejectsOffThreadWritesAndUndo() {
        val fixture = Fixture()
        fixture.onPrimaryThread = false
        assertFalse(fixture.executor.execute(listOf(operation(Material.STONE))).success)
        assertEquals(0, fixture.snapshots)
        fixture.onPrimaryThread = true
        val result = fixture.executor.execute(listOf(operation(Material.STONE)))
        fixture.onPrimaryThread = false
        assertFalse(fixture.executor.undo(result.sessionId!!).success)
        fixture.onPrimaryThread = true
        assertTrue(fixture.executor.undo(result.sessionId).success)
    }

    @Test
    fun missingWorldDuringUndoDoesNotDiscardSnapshot() {
        val fixture = Fixture()
        val result = fixture.executor.execute(listOf(operation(Material.STONE)))
        fixture.worldAvailable = false
        assertFalse(fixture.executor.undo(result.sessionId!!).success)
        fixture.worldAvailable = true
        assertTrue(fixture.executor.undo(result.sessionId).success)
    }

    private fun operation(material: Material) = BlockOperation("world", 1, 64, 1, material)

    private class Fixture {
        var material = Material.DIRT
        var snapshots = 0
        var restores = 0
        var restoreAllowed = true
        var onPrimaryThread = true
        var worldAvailable = true
        private val worldId = UUID.randomUUID()
        private val block: Block = proxy(Block::class.java) { name, arguments ->
            when (name) {
                "getState" -> {
                    snapshots++
                    val original = material
                    proxy(BlockState::class.java) { method, parameters ->
                        when (method) {
                            "getWorld" -> world
                            "update" -> {
                                assertEquals(listOf(true, false), parameters.toList())
                                if (restoreAllowed) {
                                    material = original
                                    restores++
                                }
                                restoreAllowed
                            }
                            else -> null
                        }
                    }
                }
                "setType" -> {
                    assertEquals(false, arguments[1])
                    material = arguments[0] as Material
                    null
                }
                else -> null
            }
        }
        val world: World = proxy(World::class.java) { name, _ ->
            when (name) {
                "getName" -> "world"
                "getUID" -> worldId
                "getBlockAt" -> block
                else -> null
            }
        }
        val executor = NativeBlockExecutor({ if (worldAvailable && it == "world") world else null }, { onPrimaryThread })
    }

    companion object {
        private fun <T> proxy(type: Class<T>, invoke: (String, Array<out Any?>) -> Any?): T =
            type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { instance, method, arguments ->
                when (method.name) {
                    "equals" -> instance === arguments?.firstOrNull()
                    "hashCode" -> System.identityHashCode(instance)
                    "toString" -> type.simpleName
                    else -> invoke(method.name, arguments ?: emptyArray())
                }
            })
    }
}
