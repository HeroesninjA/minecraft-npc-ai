package ro.ainpc.listeners

import org.bukkit.event.Event
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.Modifier

class RecentPublicEventListenerTest {
    @Test
    fun catalogCoversEveryConcretePublicApiEvent() {
        val eventSources = File("../ainpc-api/src/main/kotlin/ro/ainpc/api/events")
        assertTrue(eventSources.isDirectory)
        val declarationPattern = Regex("""(?m)^(?:data\s+)?class\s+(\w+Event)\s*\(""")
        val declaredEventNames = eventSources.walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .flatMap { file ->
                declarationPattern.findAll(file.readText()).map { match -> match.groupValues[1] }
            }
            .toSet()
        val registeredEventNames = RecentPublicEventListener.PUBLIC_EVENT_TYPES
            .map { eventType -> eventType.simpleName }
            .toSet()

        assertEquals(33, declaredEventNames.size)
        assertEquals(declaredEventNames, registeredEventNames)
        assertTrue(
            RecentPublicEventListener.PUBLIC_EVENT_TYPES.all { eventType ->
                Event::class.java.isAssignableFrom(eventType) && !Modifier.isAbstract(eventType.modifiers)
            }
        )
    }

    @Test
    fun listenerRegistryRegistersTheDiagnosticCollector() {
        val source = File("src/main/kotlin/ro/ainpc/listeners/ListenerRegistry.kt").readText()

        assertTrue(source.contains("RecentPublicEventListener(plugin.recentEventsBuffer).register(plugin)"))
    }

    @Test
    fun serviceRegistryReappliesBufferCapacityOnReload() {
        val source = File("src/main/kotlin/ro/ainpc/bootstrap/ServiceRegistry.kt").readText()

        assertTrue(source.contains("if (::recentEventsBuffer.isInitialized)"))
        assertTrue(source.contains("RecentEventsBuffer.DEFAULT_CAPACITY"))
    }
}
