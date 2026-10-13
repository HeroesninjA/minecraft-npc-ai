package ro.ainpc.environment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorldSpecialEventRegistryTest {
    @Test
    fun eventsRegisteredBeforeAWorldContextExistsRemainAvailable() {
        val registry = WorldSpecialEventRegistry()

        registry.add("sat", "festival")
        registry.add("sat", "festival")
        registry.add("sat", "furtuna")
        registry.add("alt_sat", "targ")

        assertEquals(listOf("festival", "furtuna"), registry.get("sat"))
        assertEquals(listOf("targ"), registry.get("alt_sat"))
        registry.remove("sat", "festival")
        assertEquals(listOf("furtuna"), registry.get("sat"))
        registry.clear("sat")
        assertEquals(emptyList<String>(), registry.get("sat"))
        assertEquals(listOf("targ"), registry.get("alt_sat"))
    }
}
