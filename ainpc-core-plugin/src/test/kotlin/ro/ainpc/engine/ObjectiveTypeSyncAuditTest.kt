package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ObjectiveTypeSyncAuditTest {

    @Test
    fun questCreateGuiMatchesRegistry() {
        val guiSource = File("src/main/kotlin/ro/ainpc/gui/screens/QuestCreateGui.kt").readText()
        val registry = ObjectiveTypeAliasRegistry.supportedTypes()

        for (type in registry) {
            assertTrue(guiSource.contains("\"$type\""),
                "QuestCreateGui.kt nu contine tipul '$type' (prezent in registru)")
        }
    }

    @Test
    fun questDraftValidatorMatchesRegistry() {
        val validatorSource = File("src/main/kotlin/ro/ainpc/engine/QuestDraftValidator.kt").readText()
        val registry = ObjectiveTypeAliasRegistry.supportedTypes()

        for (type in registry) {
            assertTrue(validatorSource.contains("\"$type\""),
                "QuestDraftValidator.kt nu contine tipul '$type' (prezent in registru)")
        }
    }

    @Test
    fun objectiveTypeAliasRegistrySupportsAllQuestTypes() {
        val types = setOf(
            "collect_item", "deliver_to_npc", "talk_to_npc",
            "visit_region", "visit_place", "inspect_node",
            "kill_mob", "place_block", "break_block", "craft_item",
            "use_item",
            "equip_item"
        )
        val registry = ObjectiveTypeAliasRegistry.supportedTypes()
        assertTrue(registry.containsAll(types),
            "Registrul lipseste tipuri: ${types - registry}")
    }

    @Test
    fun noUnknownTypesInRegistry() {
        val expected = setOf(
            "collect_item", "deliver_to_npc", "talk_to_npc",
            "visit_region", "visit_place", "inspect_node",
            "kill_mob", "place_block", "break_block", "craft_item",
            "use_item",
            "equip_item"
        )
        val registry = ObjectiveTypeAliasRegistry.supportedTypes()
        val extra = registry - expected
        assertTrue(extra.isEmpty(),
            "Registrul contine tipuri neasteptate: $extra")
    }

    @Test
    fun quickQuestGuiSubsetIsValid() {
        val guiSource = File("src/main/kotlin/ro/ainpc/gui/screens/QuickQuestGui.kt").readText()
        val registry = ObjectiveTypeAliasRegistry.supportedTypes()

        val quickQuestList = listOf("talk_to_npc", "deliver_to_npc", "collect_item",
            "visit_place", "visit_region", "inspect_node", "kill_mob",
            "craft_item", "place_block", "break_block")
        for (type in quickQuestList) {
            assertTrue(type in registry,
                "QuickQuestGui.kt contine tipul '$type' care nu exista in registru")
            assertTrue(guiSource.contains("\"$type\""),
                "QuickQuestGui.kt nu contine tipul '$type' declarat in lista sa")
        }
        val notInQuickQuest = registry - quickQuestList.toSet()
        assertTrue(notInQuickQuest.isNotEmpty(),
            "QuickQuest trebuie sa fie un subset (nu toate tipurile). Missing: $notInQuickQuest")
    }

    @Test
    fun questSeedFactorySubsetIsValid() {
        val factorySource = File("src/main/kotlin/ro/ainpc/engine/QuestSeedFactory.kt").readText()
        val registry = ObjectiveTypeAliasRegistry.supportedTypes()

        val seedList = registry.toList()
        for (type in seedList) {
            assertTrue(factorySource.contains("\"$type\""),
                "QuestSeedFactory.kt nu contine tipul '$type' (prezent in registru)")
        }
    }
}
