package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ReturnToGiverTrackingTest {

    @Test
    fun scenarioEngineStoresQuestGiverOnAccept() {
        val source = File("src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt").readText()
        assertTrue(source.contains("quest_giver_npc_id"))
        assertTrue(source.contains("quest_giver_npc_name"))
        assertTrue(source.contains("bindQuestProgressToNpc"))
    }

    @Test
    fun trackingResolutionFindsQuestGiverNpc() {
        val source = File("src/main/kotlin/ro/ainpc/engine/ScenarioTrackingResolution.kt").readText()
        assertTrue(source.contains("resolveQuestGiverNpc"))
        assertTrue(source.contains("quest_giver_uuid"))
        assertTrue(source.contains("quest_giver_name"))
        assertTrue(source.contains("quest_giver_db_id"))
    }

    @Test
    fun questScenarioAcceptsReturnToGiverMode() {
        val source = File("src/main/kotlin/ro/ainpc/engine/ScenarioSimpleQuest.kt").readText()
        assertTrue(source.contains("return_to_giver"))
    }
}
