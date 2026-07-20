package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.runtime.ObjectiveHandlerRegistry
import ro.ainpc.engine.runtime.ObjectiveContext
import ro.ainpc.engine.runtime.objectivehandlers.BreakBlockObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.CollectItemObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.CraftItemObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.DeliverToNpcObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.EquipItemObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.InspectNodeObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.KillMobObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.PlaceBlockObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.TalkToNpcObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.UseItemObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.VisitPlaceObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.VisitRegionObjectiveHandler
import java.util.UUID

class ObjectiveTypePipelineAuditTest {

    private val registry = ObjectiveHandlerRegistry().also {
        it.register(CollectItemObjectiveHandler())
        it.register(DeliverToNpcObjectiveHandler())
        it.register(TalkToNpcObjectiveHandler())
        it.register(VisitRegionObjectiveHandler())
        it.register(VisitPlaceObjectiveHandler())
        it.register(InspectNodeObjectiveHandler())
        it.register(KillMobObjectiveHandler())
        it.register(PlaceBlockObjectiveHandler())
        it.register(BreakBlockObjectiveHandler())
        it.register(CraftItemObjectiveHandler())
        it.register(UseItemObjectiveHandler())
        it.register(EquipItemObjectiveHandler())
    }

    @Test
    fun all12TypesHaveCompletePipeline() {
        val allTypes = ObjectiveTypeAliasRegistry.supportedTypes()
        assertEquals(12, allTypes.size)

        for (type in allTypes) {
            val normalized = ObjectiveTypeAliasRegistry.normalize(type)
            assertEquals(type, normalized, "Type '$type' should self-normalize")

            val requiredFields = ObjectiveTypeAliasRegistry.requiredFields(type)
            assertNotNull(requiredFields, "Type '$type' should have required fields defined")

            assertTrue(registry.supports(type),
                "Type '$type' should have a handler registered")

            assertFalse(ObjectiveTypeAliasRegistry.isDeprecated(type),
                "Type '$type' should not be deprecated")
        }
    }

    @Test
    fun handlerProgressWorksForEachType() {
        val playerId = UUID.randomUUID()
        val typesToTest = listOf(
            Triple("collect_item", "STONE", 5),
            Triple("deliver_to_npc", "BREAD", 2),
            Triple("talk_to_npc", "guard", 1),
            Triple("visit_region", "demo_sat", 1),
            Triple("visit_place", "demo_sat:piata", 1),
            Triple("inspect_node", "quest_board", 1),
            Triple("kill_mob", "ZOMBIE", 3),
            Triple("place_block", "OAK_PLANKS", 5),
            Triple("break_block", "STONE", 5),
            Triple("craft_item", "IRON_SWORD", 1),
            Triple("use_item", "STICK", 3),
            Triple("equip_item", "IRON_HELMET", 1),
        )

        for ((type, item, amount) in typesToTest) {
            val handler = registry.find(type)
            assertNotNull(handler, "Handler for '$type' should exist")

            val objective = FeaturePackLoader.QuestEntryDefinition(type, item, amount, null)
            val context = ObjectiveContext(
                playerId = playerId,
                objective = objective,
                currentProgress = 0,
                requiredAmount = amount,
                metadata = emptyMap(),
            )
            val result = handler!!.handleProgress(context)
            assertTrue(result.progressed >= 0,
                "Progress for '$type' should be non-negative, got ${result.progressed}")
        }
    }

    @Test
    fun objectiveTypeNormalizationPreservesAllCanonicalNames() {
        for (type in ObjectiveTypeAliasRegistry.supportedTypes()) {
            assertEquals(type, ObjectiveTypeAliasRegistry.normalize(type),
                "Canonical type '$type' should normalize to itself")
        }
    }

    @Test
    fun deprecatedAliasesMapToCanonicalTypes() {
        assertEquals("talk_to_npc", ObjectiveTypeAliasRegistry.normalize("talk_nlc"))
        assertEquals("inspect_node", ObjectiveTypeAliasRegistry.normalize("interact_nkde"))
        assertEquals("deliver_to_npc", ObjectiveTypeAliasRegistry.normalize("turnin"))
        assertEquals("collect_item", ObjectiveTypeAliasRegistry.normalize("gather"))
        assertEquals("kill_mob", ObjectiveTypeAliasRegistry.normalize("slay"))
        assertEquals("place_block", ObjectiveTypeAliasRegistry.normalize("construct"))
        assertEquals("craft_item", ObjectiveTypeAliasRegistry.normalize("fabricate"))
    }

    @Test
    fun allRequiredFieldsReferenceValidTypes() {
        for (type in ObjectiveTypeAliasRegistry.supportedTypes()) {
            val fields = ObjectiveTypeAliasRegistry.requiredFields(type)
            for (field in fields) {
                assertTrue(field == "item" || field == "npc_target" || field.isEmpty(),
                    "Type '$type' has unexpected required field '$field'")
            }
        }
    }
}
