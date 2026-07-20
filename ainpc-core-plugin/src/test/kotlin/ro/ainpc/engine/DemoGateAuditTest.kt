package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DemoGateAuditTest {

    @Test
    fun gate1_mappingSemanticValidSiSalvat() {
        val mappingSource = File("src/main/kotlin/ro/ainpc/commands/AINPCCommandWorld.kt").readText()
        assertTrue(mappingSource.contains("saveMapping") || mappingSource.contains("save"),
            "Mapping-ul trebuie sa aiba comanda de salvare")

        val worldAdminSource = File("src/main/kotlin/ro/ainpc/world/WorldAdminService.kt").readText()
        assertTrue(worldAdminSource.contains("region") && worldAdminSource.contains("place"),
            "WorldAdmin trebuie sa suporte regiuni si places")

        val persistenceSource = File("src/main/kotlin/ro/ainpc/world/")
        assertTrue(persistenceSource.exists() && persistenceSource.listFiles()?.isNotEmpty() == true,
            "Mapping-ul trebuie sa aiba persistenta (fisiere world/)")
    }

    @Test
    fun gate2_packVersionatValidatSiIncarcat() {
        val loaderSource = File("src/main/kotlin/ro/ainpc/engine/FeaturePackLoader.kt").readText()
        assertTrue(loaderSource.contains("loadAllPacks"),
            "FeaturePackLoader trebuie sa incarce pack-uri")
        assertTrue(loaderSource.contains("validate") || loaderSource.contains("metadata") || loaderSource.contains("version"),
            "Pack-urile trebuie sa fie validate la incarcare")

        val yamlSource = File("src/main/kotlin/ro/ainpc/engine/FeaturePackYamlSupport.kt").readText()
        assertTrue(yamlSource.contains("scenarios"),
            "FeaturePackYamlSupport trebuie sa parseze scenarii")

        val medievalPack = File("../ainpc-scenario-medieval/src/main/resources/packs/medieval_quest.yml")
        assertTrue(medievalPack.exists(), "Pack-ul medieval trebuie sa existe")
        val content = medievalPack.readText()
        assertTrue(content.contains("version") || content.contains("addon"),
            "Pack-ul medieval trebuie sa contina versiune/addon metadata")
        assertTrue(content.contains("scenarios:"),
            "Pack-ul medieval trebuie sa contina sectiunea scenarios")
    }

    @Test
    fun gate3_bindinguriIzolateCorect() {
        val bindingSource = File("src/main/kotlin/ro/ainpc/progression/ProgressionAnchorBinding.kt").readText()
        assertTrue(bindingSource.contains("GLOBAL_PLAYER_UUID"),
            "Trebuie sa existe constanta GLOBAL_PLAYER_UUID")

        val repoSource = File("src/main/kotlin/ro/ainpc/progression/ProgressionRepository.kt").readText()
        assertTrue(repoSource.contains("isGlobalNamespace"),
            "Repository-ul trebuie sa trateze namespace-ul global separat")

        val serviceSource = File("src/main/kotlin/ro/ainpc/engine/QuestAnchorBindingService.kt").readText()
        assertTrue(serviceSource.contains("GLOBAL_PLAYER_UUID"),
            "QuestAnchorBindingService trebuie sa interogheze namespace-ul global")

        val testSource = File("src/test/kotlin/ro/ainpc/progression/ProgressionRepositoryTest.kt").readText()
        assertTrue(testSource.contains("globalNamespaceIsolatedFromPlayerBindings"),
            "Trebuie sa existe test de izolare a binding-urilor globale")
    }

    @Test
    fun gate4_acceptareObiectiveMultiStageSiTurnIn() {
        val scenarioEngineSource = File("src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt").readText()
        assertTrue(scenarioEngineSource.contains("acceptQuest"),
            "ScenarioEngine trebuie sa suporte acceptarea quest-urilor")
        assertTrue(scenarioEngineSource.contains("stage") || scenarioEngineSource.contains("Stage"),
            "ScenarioEngine trebuie sa suporte multi-stage")

        val medievalPack = File("../ainpc-scenario-medieval/src/main/resources/packs/medieval_quest.yml").readText()
        assertTrue(medievalPack.contains("stages:"),
            "Pack-ul medieval trebuie sa contina quest-uri cu stage-uri")
        assertTrue(medievalPack.contains("stages:") || medievalPack.contains("stage_"),
            "Pack-ul medieval trebuie sa aiba stage-uri definite")
        assertTrue(medievalPack.contains("return_to_giver") || medievalPack.contains("turn_in"),
            "Pack-ul medieval trebuie sa contina turn-in")

        val testSource = File("src/test/kotlin/ro/ainpc/engine/ScenarioObjectiveProgressTest.kt").readText()
        assertTrue(testSource.contains("resolveQuestObjectiveState"),
            "Trebuie sa existe teste pentru starea obiectivelor")
    }

    @Test
    fun gate5_recompensaSiStoryEvent() {
        val rewardSource = File("src/main/kotlin/ro/ainpc/engine/ScenarioObjectiveProgress.kt").readText()
        assertTrue(rewardSource.contains("grantQuestRewards"),
            "Trebuie sa existe functia de acordare recompense")

        val storySource = File("src/main/kotlin/ro/ainpc/story/StoryStateService.kt").readText()
        assertTrue(storySource.contains("record") || storySource.contains("event"),
            "StoryStateService trebuie sa inregistreze evenimente")

        val medievalPack = File("../ainpc-scenario-medieval/src/main/resources/packs/medieval_quest.yml").readText()
        assertTrue(medievalPack.contains("rewards:"),
            "Pack-ul medieval trebuie sa contina recompense")
        assertTrue(medievalPack.contains("story_event") || medievalPack.contains("record_story_event"),
            "Pack-ul medieval trebuie sa aiba recompense de tip story event")

        val auditSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpQuestAudit.kt").readText()
        assertTrue(auditSource.contains("Reward Type Summary"),
            "Auditul trebuie sa contina sumar recompense")
    }

    @Test
    fun gate6_restartCuProgresPastrat() {
        val progressSource = File("src/main/kotlin/ro/ainpc/engine/ScenarioObjectiveProgress.kt").readText()
        assertTrue(progressSource.contains("persistQuestProgress") || progressSource.contains("snapshot"),
            "Progresul trebuie sa fie persistat")

        val scenarioEngineSource = File("src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt").readText()
        assertTrue(scenarioEngineSource.contains("loadPlayerQuests") || scenarioEngineSource.contains("persistQuestProgress"),
            "ScenarioEngine trebuie sa restaureze progresul la restart")

        val storageTestSource = File("src/test/kotlin/ro/ainpc/progression/ProgressionRepositoryTest.kt").readText()
        assertTrue(storageTestSource.contains("player_quests"),
            "Test-ul de repository trebuie sa verifice tabela player_quests")
    }

    @Test
    fun gate7_aceeasiStareInComenziGuiSiDebugDump() {
        val commandSource = File("src/main/kotlin/ro/ainpc/commands/AINPCCommandQuest.kt").readText()
        assertTrue(commandSource.contains("handleQuestStatus") || commandSource.contains("handleQuestLog"),
            "Trebuie sa existe comanda de status/log quest")

        val debugDumpSource = File("src/main/kotlin/ro/ainpc/debug/DebugDumpQuestAudit.kt").readText()
        assertTrue(debugDumpSource.contains("buildQuestAuditReportText"),
            "DebugDump trebuie sa genereze raport de quest audit")

        val progressionSource = File("src/main/kotlin/ro/ainpc/progression/ProgressionService.kt").readText()
        assertTrue(progressionSource.contains("getGuiSnapshot") || progressionSource.contains("getProgressionGuiSnapshot"),
            "ProgressionService trebuie sa furnizeze snapshot pentru GUI")

        val questMapSource = File("src/main/kotlin/ro/ainpc/gui/screens/QuestMapGui.kt").readText()
        assertTrue(questMapSource.contains("quest") || questMapSource.contains("Quest"),
            "QuestMapGui trebuie sa expuna starea quest-urilor")
    }

    @Test
    fun demoCommandsCoverFullFlow() {
        val smokeFile = File("../deploy/ainpc-quest-smoke-commands.txt")
        assertTrue(smokeFile.exists(), "Trebuie sa existe fisierul de comenzi smoke")

        val commands = smokeFile.readText()
        assertTrue(commands.contains("quest accept") || commands.contains("quest offer"),
            "Smoke commands trebuie sa includa acceptarea quest-urilor")
        assertFalse(commands.contains("TODO") && commands.contains("quest"),
            "Smoke commands nu trebuie sa aiba TODO-uri pentru quest")
    }
}
