package ro.ainpc.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReputationApiTest {

    @Test
    fun reputationEntryStoresData() {
        val entry = ReputationEntry(
            playerUuid = "00000000-0000-0000-0000-000000000001",
            playerName = "Hero",
            reputation = 100,
        )
        assertEquals("00000000-0000-0000-0000-000000000001", entry.playerUuid)
        assertEquals("Hero", entry.playerName)
        assertEquals(100, entry.reputation)
    }

    @Test
    fun reputationEntryWithZeroReputation() {
        val entry = ReputationEntry(
            playerUuid = "00000000-0000-0000-0000-000000000002",
            playerName = "TestPlayer",
            reputation = 0,
        )
        assertEquals(0, entry.reputation)
    }

    @Test
    fun reputationApiInterfaceExists() {
        val clazz = ReputationApi::class.java
        assertEquals("ro.ainpc.api.ReputationApi", clazz.name)
    }
}
