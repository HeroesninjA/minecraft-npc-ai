package ro.ainpc.worldgen

import java.io.File
import java.util.UUID
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfirmedBuildingExecutorTest {
    @Test
    fun sourceChangesConsumeTokenWithoutWritingBlocks() {
        val fixture = Fixture()
        var unchanged = true
        val token = fixture.service.prepare("admin", listOf(BlockOperation("world", 1, 64, 1, Material.STONE))) { unchanged }
        unchanged = false
        val result = fixture.service.confirm("admin", token)
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("mapping changed") })
        unchanged = true
        assertFalse(fixture.service.confirm("admin", token).success)
        assertEquals(0, fixture.writes)
    }

    @Test
    fun unavailableSourceFailsClosed() {
        val fixture = Fixture()
        var unavailable = false
        val token = fixture.service.prepare("admin", listOf(BlockOperation("world", 1, 64, 1, Material.STONE))) {
            if (unavailable) error("Unavailable source") else true
        }
        unavailable = true
        assertFalse(fixture.service.confirm("admin", token).success)
        assertEquals(0, fixture.writes)
    }

    @Test
    fun requiresSameActorAndConsumesSuccessfulToken() {
        val fixture = Fixture()
        val token = fixture.prepare()
        assertFalse(fixture.service.confirm("other", token).success)
        assertEquals(0, fixture.writes)
        assertTrue(fixture.service.confirm("admin", token).success)
        assertFalse(fixture.service.confirm("admin", token).success)
        assertEquals(1, fixture.writes)
    }

    @Test
    fun rechecksAuthorizationAndTerrainAndConsumesRejectedToken() {
        for (denyAuthorization in listOf(true, false)) {
            val fixture = Fixture()
            val token = fixture.prepare()
            if (denyAuthorization) fixture.authorized = false else fixture.errors = listOf("Entity or obstacle appeared")
            assertFalse(fixture.service.confirm("admin", token).success)
            fixture.authorized = true
            fixture.errors = emptyList()
            assertFalse(fixture.service.confirm("admin", token).success)
            assertEquals(0, fixture.writes)
        }
    }

    @Test
    fun expirationAndWrongThreadPreventWrites() {
        val fixture = Fixture()
        val token = fixture.prepare()
        fixture.primary = false
        assertFalse(fixture.service.confirm("admin", token).success)
        fixture.primary = true
        fixture.now = 60000
        assertFalse(fixture.service.confirm("admin", token).success)
        assertEquals(0, fixture.writes)
    }

    @Test
    fun preparationFailsClosedAndDoesNotWrite() {
        val fixture = Fixture()
        fixture.authorized = false
        assertThrows(IllegalStateException::class.java) { fixture.prepare() }
        fixture.authorized = true
        fixture.errors = listOf("Protected terrain")
        assertThrows(IllegalArgumentException::class.java) { fixture.prepare() }
        assertEquals(0, fixture.writes)
    }

    @Test
    fun snapshotsOperationsAndRetainsPartialFailureRecoveryId() {
        val fixture = Fixture()
        val operations = mutableListOf(BlockOperation("world", 1, 64, 1, Material.STONE))
        val token = fixture.service.prepare("admin", operations)
        operations.clear()
        fixture.failPartially = true
        val result = fixture.service.confirm("admin", token)
        assertFalse(result.success)
        assertEquals(fixture.sessionId, result.sessionId)
        assertEquals(1, result.blocksPlaced)
        assertEquals(1, fixture.writes)
    }

    @Test
    fun boundsPendingRequestsAndExpiresOldTokens() {
        val fixture = Fixture()
        repeat(32) { fixture.prepare() }
        assertThrows(IllegalArgumentException::class.java) { fixture.prepare() }
        fixture.now = 60000
        assertTrue(fixture.service.confirm("admin", fixture.prepare()).success)
    }

    private class Fixture {
        var now = 0L
        var primary = true
        var authorized = true
        var errors = emptyList<String>()
        var writes = 0
        var failPartially = false
        val sessionId = UUID.randomUUID()
        val executor = object : BlockExecutor {
            override val executorId = "native"
            override val displayName = "Test"
            override fun isAvailable() = true
            override fun getDescription() = "Test"
            override fun execute(operations: List<BlockOperation>): ExecutionResult {
                writes++
                assertEquals(1, operations.size)
                return if (failPartially) ExecutionResult(false, sessionId, 1, 1, listOf("Partial failure"))
                    else ExecutionResult.success(sessionId, operations.size)
            }
            override fun executeWithPreview(operations: List<BlockOperation>): PreviewSession = error("Not used")
            override fun undo(sessionId: UUID): UndoResult = error("Not used")
            override fun saveSchematic(sessionId: UUID, file: File) = false
            override fun loadSchematic(file: File) = emptyList<BlockOperation>()
        }
        val service = ConfirmedBuildingExecutor(executor, { primary }, { _, _ -> authorized }, { errors }, { now })
        fun prepare() = service.prepare("admin", listOf(BlockOperation("world", 1, 64, 1, Material.STONE)))
    }
}
