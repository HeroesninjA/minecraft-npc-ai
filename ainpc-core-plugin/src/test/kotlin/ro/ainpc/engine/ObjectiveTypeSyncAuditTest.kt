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
}
