package ro.ainpc.routine

import java.lang.reflect.Proxy
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox
import org.bukkit.util.VoxelShape
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState

class RoutineTeleportTest {
    @Test
    fun supportedDestinationUsesTranslatedBodyWithoutMutatingOriginal() {
        val fixture = Fixture()
        val original = fixture.body.clone()
        assertTrue(fixture.teleport())
        assertEquals(1, fixture.teleportCalls)
        assertEquals(original, fixture.body)
        assertEquals(10.2, fixture.checkedBody!!.minX, 0.00001)
        assertEquals(65.0, fixture.checkedBody!!.minY, 0.00001)
        assertEquals(20.8, fixture.checkedBody!!.maxZ, 0.00001)
        assertEquals(fixture.target, fixture.teleportedTarget)
    }

    @Test
    fun bodyCollisionRejectsTeleport() {
        val fixture = Fixture()
        fixture.collision = true
        assertFalse(fixture.teleport())
        assertEquals(0, fixture.teleportCalls)
    }

    @Test
    fun missingFloorOrIncompleteFootprintRejectsTeleport() {
        val fixture = Fixture()
        fixture.floorShapes = emptyList()
        assertFalse(fixture.teleport())
        fixture.floorShapes = listOf(BoundingBox(0.4, 0.0, 0.4, 0.6, 1.0, 0.6))
        assertFalse(fixture.teleport())
        assertEquals(0, fixture.teleportCalls)
    }

    @Test
    fun lowerSlabRequiresFeetAtActualSupportHeight() {
        val fixture = Fixture()
        fixture.floorShapes = listOf(BoundingBox(0.0, 0.0, 0.0, 1.0, 0.5, 1.0))
        assertFalse(fixture.teleport())
        fixture.target.y = 64.5
        assertTrue(fixture.teleport())
        assertEquals(1, fixture.teleportCalls)
    }

    @Test
    fun everyColumnUnderNpcMustHaveSupport() {
        val fixture = Fixture()
        fixture.target.x = 10.0
        fixture.missingFloorColumn = 9 to 20
        assertFalse(fixture.teleport())
        assertEquals(0, fixture.teleportCalls)
    }

    @Test
    fun hazardousBodyOrSupportMaterialsAreRejected() {
        for (material in listOf(
            Material.FIRE, Material.SOUL_FIRE, Material.LAVA, Material.WATER, Material.BUBBLE_COLUMN,
            Material.CACTUS, Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE, Material.POWDER_SNOW,
            Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_GATEWAY,
        )) {
            for (height in listOf(64, 65)) {
                val fixture = Fixture()
                fixture.specialMaterial = material
                fixture.specialY = height
                assertFalse(fixture.teleport(), "$material at $height")
                assertEquals(0, fixture.teleportCalls)
            }
        }
    }

    @Test
    fun liquidAndWaterloggedCellsAreRejected() {
        val fixture = Fixture()
        fixture.liquid = true
        assertFalse(fixture.teleport())
        fixture.liquid = false
        fixture.waterlogged = true
        assertFalse(fixture.teleport())
        assertEquals(0, fixture.teleportCalls)
    }

    @Test
    fun unloadedNeighbourChunkRejectsBeforeAnyBlockReadOrCollisionQuery() {
        val fixture = Fixture()
        fixture.target.x = 15.5
        fixture.unloadedChunk = 1 to 1
        assertFalse(fixture.teleport())
        assertTrue(fixture.checkedChunks.contains(1 to 1))
        assertEquals(0, fixture.blockReads)
        assertEquals(null, fixture.checkedBody)
        assertEquals(0, fixture.teleportCalls)
    }

    @Test
    fun negativeCoordinatesUseFloorForChunkAndBlockChecks() {
        val fixture = Fixture()
        fixture.target.x = -0.5
        fixture.target.z = -16.5
        assertTrue(fixture.teleport())
        assertTrue(fixture.checkedChunks.contains(-1 to -2))
        assertTrue(fixture.readCoordinates.contains(Triple(-1, 64, -17)))
    }

    @Test
    fun worldHeightLimitsIncludeGroundAndEntireBody() {
        val fixture = Fixture()
        fixture.target.y = 0.0
        assertFalse(fixture.teleport())
        fixture.target.y = 319.0
        assertFalse(fixture.teleport())
        assertEquals(0, fixture.blockReads)
        assertTrue(fixture.checkedChunks.isEmpty())
    }

    @Test
    fun malformedCoordinatesAndOversizedBodiesFailBeforeWorldReads() {
        val fixture = Fixture()
        for (coordinate in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE)) {
            fixture.target.x = coordinate
            assertFalse(fixture.teleport())
        }
        fixture.target.x = 10.5
        fixture.target.yaw = Float.NaN
        assertFalse(fixture.teleport())
        fixture.target.yaw = 0f
        fixture.body = BoundingBox(-10.0, 65.0, -10.0, 10.0, 85.0, 10.0)
        assertFalse(fixture.teleport())
        fixture.body = BoundingBox(0.0, 65.0, 0.0, 16.0, 81.0, 16.0)
        assertFalse(fixture.teleport())
        assertEquals(0, fixture.blockReads)
        assertTrue(fixture.checkedChunks.isEmpty())
    }

    @Test
    fun missingEntityOrWorldAndInvalidEntityCannotTeleport() {
        val fixture = Fixture()
        assertFalse(RoutineTeleport.tryTeleport(null, fixture.target))
        assertFalse(RoutineTeleport.tryTeleport(fixture.entity, Location(null, 10.5, 65.0, 20.5)))
        fixture.entityValid = false
        assertFalse(fixture.teleport())
        assertEquals(0, fixture.teleportCalls)
    }

    @Test
    fun crossWorldTeleportChecksDestinationWorldInsteadOfSource() {
        val source = Fixture()
        val destination = Fixture()
        assertTrue(RoutineTeleport.tryTeleport(source.entity, destination.target))
        assertTrue(source.checkedChunks.isEmpty())
        assertEquals(0, source.blockReads)
        assertTrue(destination.blockReads > 0)
        assertEquals(1, source.teleportCalls)
    }

    @Test
    fun cancelledTeleportIsNotReportedAsSuccess() {
        val fixture = Fixture()
        fixture.acceptTeleport = false
        assertFalse(fixture.teleport())
        assertEquals(1, fixture.teleportCalls)
    }

    @Test
    fun unsafeFallbackLeavesRoutineIdleWithoutArrival() {
        val fixture = Fixture()
        fixture.collision = true
        val npc = AINPC(null)
        val executor = RoutineActivityExecutor({ fixture.target }, { fixture.current })
        val assignment = RoutineAssignment(RoutineSlot.WORK, "work", "shop", NPCState.WORKING,
            AINPC.OwnedLocation("work", "shop", "world", 10.5, 65.0, 20.5))
        val result = executor.execute(npc, assignment, 2.0, { false }, { fixture.teleport() })
        assertEquals(RoutineActivityExecutor.Outcome.WAITING, result)
        assertEquals(NPCState.IDLE, npc.currentState)
        assertEquals(0, fixture.teleportCalls)
    }

    private class Fixture {
        var collision = false
        var entityValid = true
        var acceptTeleport = true
        var liquid = false
        var waterlogged = false
        var specialMaterial = Material.AIR
        var specialY = 65
        var unloadedChunk: Pair<Int, Int>? = null
        var missingFloorColumn: Pair<Int, Int>? = null
        var floorShapes = listOf(BoundingBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
        var body = BoundingBox(-0.3, 65.0, -0.3, 0.3, 66.8, 0.3)
        var blockReads = 0
        var teleportCalls = 0
        var teleportedTarget: Location? = null
        var checkedBody: BoundingBox? = null
        val checkedChunks = mutableListOf<Pair<Int, Int>>()
        val readCoordinates = mutableListOf<Triple<Int, Int, Int>>()
        val world: World = proxy(World::class.java) { method, arguments ->
            when (method) {
                "getMinHeight" -> 0
                "getMaxHeight" -> 320
                "isChunkLoaded" -> {
                    val chunk = arguments!![0] as Int to arguments[1] as Int
                    checkedChunks.add(chunk)
                    chunk != unloadedChunk
                }
                "getBlockAt" -> {
                    val coordinates = Triple(arguments!![0] as Int, arguments[1] as Int, arguments[2] as Int)
                    readCoordinates.add(coordinates)
                    blockReads++
                    block(coordinates)
                }
                "hasCollisionsIn" -> {
                    checkedBody = (arguments!![0] as BoundingBox).clone()
                    collision
                }
                else -> error("Unexpected world access: $method")
            }
        }
        val current = Location(world, 0.0, 65.0, 0.0)
        val target = Location(world, 10.5, 65.0, 20.5)
        val entity: Entity = proxy(Entity::class.java) { method, arguments ->
            when (method) {
                "isValid" -> entityValid
                "getLocation" -> current.clone()
                "getBoundingBox" -> body
                "teleport" -> {
                    teleportCalls++
                    teleportedTarget = (arguments!![0] as Location).clone()
                    acceptTeleport
                }
                else -> error("Unexpected entity access: $method")
            }
        }

        fun teleport(): Boolean = RoutineTeleport.tryTeleport(entity, target)

        private fun block(coordinates: Triple<Int, Int, Int>): Block = proxy(Block::class.java) { method, _ ->
            when (method) {
                "isLiquid" -> liquid
                "getType" -> if (coordinates.second == specialY) specialMaterial else if (coordinates.second == 64) Material.STONE else Material.AIR
                "getBlockData" -> proxy(Waterlogged::class.java) { dataMethod, _ ->
                    when (dataMethod) {
                        "isWaterlogged" -> waterlogged
                        else -> error("Unexpected block data access: $dataMethod")
                    }
                }
                "getCollisionShape" -> proxy(VoxelShape::class.java) { shapeMethod, _ ->
                    when (shapeMethod) {
                        "getBoundingBoxes" -> if (coordinates.second == 64 &&
                            (coordinates.first to coordinates.third) != missingFloorColumn) floorShapes else emptyList()
                        else -> error("Unexpected shape access: $shapeMethod")
                    }
                }
                else -> error("Unexpected block access: $method")
            }
        }
    }

    private companion object {
        fun <Type> proxy(type: Class<Type>, handler: (String, Array<out Any?>?) -> Any?): Type = type.cast(
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { instance, method, arguments ->
                when (method.name) {
                    "equals" -> instance === arguments?.get(0)
                    "hashCode" -> System.identityHashCode(instance)
                    "toString" -> type.simpleName
                    else -> handler(method.name, arguments)
                }
            },
        )
    }
}
