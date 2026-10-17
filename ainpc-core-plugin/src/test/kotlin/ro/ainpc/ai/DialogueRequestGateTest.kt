package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DialogueRequestGateTest {
    private val gate = DialogueRequestGate()
    private val playerId = UUID.randomUUID()
    private val npcId = UUID.randomUUID()
    private val work = CompletableFuture<String>()

    @Test
    fun `overlapping request is rejected without starting more work`() {
        val accepted = submit { work }
        var started = false

        val rejected = submit {
            started = true
            CompletableFuture.completedFuture("unexpected")
        }

        assertEquals("busy", rejected.join())
        assertFalse(started)
        assertFalse(accepted.isDone)
        assertTrue(gate.isActive(playerId, npcId))
    }

    @Test
    fun `completed work releases the pair for the next request`() {
        val accepted = submit { work }

        work.complete("first")

        assertEquals("first", accepted.join())
        assertFalse(gate.isActive(playerId, npcId))
        assertEquals("second", submit { CompletableFuture.completedFuture("second") }.join())
        assertFalse(gate.isActive(playerId, npcId))
    }

    @Test
    fun `failed work releases the pair and preserves the failure`() {
        val failure = IllegalStateException("provider failed")
        val accepted = submit { work }

        work.completeExceptionally(failure)

        assertSame(failure, assertThrows(CompletionException::class.java) { accepted.join() }.cause)
        assertFalse(gate.isActive(playerId, npcId))
        assertEquals("retry", submit { CompletableFuture.completedFuture("retry") }.join())
    }

    @Test
    fun `synchronous setup failure releases the pair`() {
        val failure = IllegalStateException("setup failed")

        assertSame(failure, assertThrows(IllegalStateException::class.java) { submit { throw failure } })

        assertFalse(gate.isActive(playerId, npcId))
        assertEquals("retry", submit { CompletableFuture.completedFuture("retry") }.join())
    }

    @Test
    fun `cancelling returned future does not cancel work or release admission early`() {
        val accepted = submit { work }

        assertTrue(accepted.cancel(true))
        assertFalse(work.isCancelled)
        assertTrue(gate.isActive(playerId, npcId))
        assertEquals("busy", submit { CompletableFuture.completedFuture("unexpected") }.join())

        work.complete("finished")

        assertFalse(gate.isActive(playerId, npcId))
        assertTrue(accepted.isCancelled)
        assertEquals("next", submit { CompletableFuture.completedFuture("next") }.join())
    }

    @Test
    fun `externally completing returned future does not release unfinished work`() {
        val accepted = submit { work }

        accepted.complete("external")

        assertFalse(work.isDone)
        assertTrue(gate.isActive(playerId, npcId))
        work.completeExceptionally(IllegalStateException("late failure"))

        assertEquals("external", accepted.join())
        assertFalse(gate.isActive(playerId, npcId))
    }

    @Test
    fun `cancelling underlying work releases the pair`() {
        val accepted = submit { work }

        work.cancel(false)

        assertThrows(CancellationException::class.java) { accepted.join() }
        assertFalse(gate.isActive(playerId, npcId))
        assertEquals("next", submit { CompletableFuture.completedFuture("next") }.join())
    }

    @Test
    fun `different players and npcs can work independently`() {
        submit { work }

        for ((otherPlayerId, otherNpcId) in listOf(
            UUID.randomUUID() to npcId,
            playerId to UUID.randomUUID(),
            npcId to playerId,
        )) {
            assertEquals("other", gate.submit(otherPlayerId, otherNpcId, { "busy" }) {
                CompletableFuture.completedFuture("other")
            }.join())
            assertFalse(gate.isActive(otherPlayerId, otherNpcId))
        }
        assertTrue(gate.isActive(playerId, npcId))
    }

    @Test
    fun `concurrent contenders admit exactly one operation for the pair`() {
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val operations = AtomicInteger()
        try {
            val contenders = (1..8).map {
                executor.submit<CompletableFuture<String>> {
                    check(start.await(5, TimeUnit.SECONDS))
                    submit {
                        operations.incrementAndGet()
                        work
                    }
                }
            }
            start.countDown()
            val results = contenders.map { it.get(5, TimeUnit.SECONDS) }

            assertEquals(1, operations.get())
            assertEquals(7, results.count { it.isDone && it.join() == "busy" })
            assertEquals(1, results.count { !it.isDone })
            work.complete("accepted")

            assertEquals(1, results.count { it.join() == "accepted" })
            assertFalse(gate.isActive(playerId, npcId))
        } finally {
            start.countDown()
            work.complete("cleanup")
            executor.shutdownNow()
        }
    }

    @Test
    fun `busy response failure cannot release another request`() {
        submit { work }

        assertThrows(IllegalStateException::class.java) {
            gate.submit(playerId, npcId, { error("busy callback failed") }) { work }
        }

        assertTrue(gate.isActive(playerId, npcId))
        work.complete("finished")
        assertFalse(gate.isActive(playerId, npcId))
    }

    private fun submit(operation: () -> CompletableFuture<String>): CompletableFuture<String> =
        gate.submit(playerId, npcId, { "busy" }, operation)
}
