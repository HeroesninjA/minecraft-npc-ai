package ro.ainpc.npc

import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.environment.EnvironmentContext
import ro.ainpc.topology.TopologyCategory
import java.lang.reflect.Proxy

class NpcPerceptionSnapshotTest {
    @Test
    fun `capture detaches cached perception and environment events without reading entity or world APIs`() {
        val npc = AINPC(null).apply { worldName = "world" }
        val context = npc.context
        val events = mutableListOf("festival")
        context.isAtHome = true
        context.healthPercent = 25.0
        context.nearbyPlayers.add(Proxy.newProxyInstance(
            Player::class.java.classLoader, arrayOf(Player::class.java),
        ) { _, method, _ -> error("Unexpected player access: ${method.name}") } as Player)
        context.nearbyNPCs.add(AINPC(null))
        setField(context, "timeOfDay", "NIGHT")
        setField(context, "topologyCategory", TopologyCategory.PLAINS)
        setField(context, "environmentContext", EnvironmentContext.empty().copy(
            worldName = "world", weather = EnvironmentContext.Weather.SNOW, specialEvents = events,
        ))

        val snapshot = NpcAgentSnapshot.capture(npc, context, 1_000L)
        npc.worldName = "another-world"
        context.isAtHome = false
        context.healthPercent = 100.0
        context.nearbyPlayers.clear()
        context.nearbyNPCs.clear()
        events.clear()
        setField(context, "timeOfDay", "MORNING")

        assertEquals(2, snapshot.schemaVersion)
        assertEquals("world", snapshot.perception.worldName)
        assertEquals("NIGHT", snapshot.perception.timeOfDay)
        assertEquals(TopologyCategory.PLAINS, snapshot.perception.topologyCategory)
        assertEquals(25.0, snapshot.perception.healthPercent)
        assertTrue(snapshot.perception.isAtHome)
        assertEquals(1, snapshot.perception.nearbyPlayerCount)
        assertEquals(1, snapshot.perception.nearbyNpcCount)
        assertEquals(listOf("festival"), snapshot.perception.environmentContext.specialEvents)
        assertThrows(UnsupportedOperationException::class.java) {
            (snapshot.perception.environmentContext.specialEvents as MutableList).clear()
        }
    }

    @Test
    fun `description uses the captured perception after live location weather and health change`() {
        val npc = AINPC(null)
        val context = npc.context
        context.isAtHome = true
        context.healthPercent = 25.0
        setField(context, "timeOfDay", "NIGHT")
        setField(context, "biome", "PLAINS")
        setField(context, "topologyCategory", TopologyCategory.PLAINS)
        setField(context, "environmentContext", EnvironmentContext.empty().copy(
            worldName = "world", weather = EnvironmentContext.Weather.SNOW, specialEvents = listOf("festival"),
        ))
        val snapshot = NpcAgentSnapshot.capture(npc)
        context.isAtHome = false
        context.isAtWork = true
        context.healthPercent = 100.0
        setField(context, "timeOfDay", "MORNING")
        setField(context, "topologyCategory", TopologyCategory.FOREST)
        setField(context, "environmentContext", EnvironmentContext.empty())

        val description = context.generateContextDescription(null, null, snapshot.cognition, snapshot.perception)

        assertTrue(description.contains("Este noapte."))
        assertTrue(description.contains("Vremea: ninge."))
        assertTrue(description.contains("Evenimente active: festival"))
        assertTrue(description.contains("sanatate scazuta"))
        assertTrue(description.contains("Sunt acasa."))
        assertFalse(description.contains("Sunt la munca."))
        assertFalse(description.contains("padure"))
    }

    private fun setField(context: NPCContext, name: String, value: Any) {
        NPCContext::class.java.getDeclaredField(name).apply {
            isAccessible = true
            set(context, value)
        }
    }
}
