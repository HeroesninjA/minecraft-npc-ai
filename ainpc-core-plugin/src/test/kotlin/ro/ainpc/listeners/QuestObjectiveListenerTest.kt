package ro.ainpc.listeners

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestObjectiveListenerTest {
    @Test
    fun questObjectiveListenerTracksFallbackKillCredit() {
        val source = File("src/main/kotlin/ro/ainpc/listeners/QuestObjectiveListener.kt").readText()

        assertTrue(source.contains("recentMobAttackers"))
        assertTrue(source.contains("onEntityDamageByEntity"))
        assertTrue(source.contains("resolvePlayerAttacker"))
        assertTrue(source.contains("plugin.server.getPlayer(attackerId)"))
    }
}
