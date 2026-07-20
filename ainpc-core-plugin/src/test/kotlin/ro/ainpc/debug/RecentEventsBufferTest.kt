package ro.ainpc.debug

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecentEventsBufferTest {
    @Test
    fun recordsOnlyEventMetadata() {
        val buffer = RecentEventsBuffer()

        buffer.addEvent(AINPCTestEvent())

        val events = buffer.snapshot()
        assertEquals(1, events.size)
        assertEquals("AINPCTestEvent", events.single().eventName)
        assertFalse(events.single().isAsync)
        assertEquals(null, events.single().isCancelled)
    }

    @Test
    fun clampsCapacityAndRetainsNewestEvents() {
        val buffer = RecentEventsBuffer()
        buffer.configure(1)
        repeat(RecentEventsBuffer.MIN_CAPACITY + 3) {
            buffer.addEvent(AINPCTestEvent())
        }
        assertEquals(RecentEventsBuffer.MIN_CAPACITY, buffer.snapshot().size)

        buffer.clear()
        buffer.configure(Int.MAX_VALUE)
        repeat(RecentEventsBuffer.MAX_CAPACITY + 3) {
            buffer.addEvent(AINPCTestEvent())
        }
        assertEquals(RecentEventsBuffer.MAX_CAPACITY, buffer.snapshot().size)
    }

    @Test
    fun exposesAsyncAndCancellationStateWithoutPayloads() {
        val buffer = RecentEventsBuffer()
        val event = AINPCCancelledTestEvent()
        event.isCancelled = true

        buffer.addEvent(event)

        val snapshot = buffer.snapshot().single()
        assertTrue(snapshot.isAsync)
        assertEquals(true, snapshot.isCancelled)
        val text = buffer.buildRecentApiEventsText()
        assertTrue(text.contains("Recent AINPC API Events"))
        assertTrue(text.contains("AINPCCancelledTestEvent [ASYNC] [CANCELLED]"))
    }

    private open class AINPCTestEvent(isAsync: Boolean = false) : Event(isAsync) {
        override fun getHandlers(): HandlerList = HANDLERS

        companion object {
            private val HANDLERS = HandlerList()

            @JvmStatic
            fun getHandlerList(): HandlerList = HANDLERS
        }
    }

    private class AINPCCancelledTestEvent : AINPCTestEvent(true), Cancellable {
        private var cancelled = false

        override fun isCancelled(): Boolean = cancelled

        override fun setCancelled(cancelled: Boolean) {
            this.cancelled = cancelled
        }
    }
}
