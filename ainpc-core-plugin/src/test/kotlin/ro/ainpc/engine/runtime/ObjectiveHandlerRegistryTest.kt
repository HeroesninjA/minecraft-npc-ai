package ro.ainpc.engine.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.runtime.objectivehandlers.BreakBlockObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.CollectItemObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.CraftItemObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.DeliverToNpcObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.EquipItemObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.InspectNodeObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.KillMobObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.PlaceBlockObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.TalkToNpcObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.VisitPlaceObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.VisitRegionObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.UseItemObjectiveHandler
import java.util.UUID

class ObjectiveHandlerRegistryTest {

    @Test
    fun registryAcceptsHandler() {
        val registry = ObjectiveHandlerRegistry()
        val handler = UseItemObjectiveHandler()
        registry.register(handler)
        assertTrue(registry.supports("use_item"))
    }

    @Test
    fun registryFindsHandlerByType() {
        val registry = ObjectiveHandlerRegistry()
        registry.register(UseItemObjectiveHandler())
        val found = registry.find("use_item")
        assertTrue(found != null)
        assertEquals("use_item", found!!.type())
    }

    @Test
    fun registryReturnsNullForUnknown() {
        val registry = ObjectiveHandlerRegistry()
        assertFalse(registry.supports("unknown_type"))
    }

    @Test
    fun useItemObjectiveHandlerHasCorrectType() {
        val handler = UseItemObjectiveHandler()
        assertEquals("use_item", handler.type())
    }

    @Test
    fun useItemHandlerProgressIncrements() {
        val handler = UseItemObjectiveHandler()
        val objective = FeaturePackLoader.QuestEntryDefinition("use_item", "STICK", 3, "Foloseste un bat")
        val context = ObjectiveContext(
            playerId = UUID.randomUUID(),
            objective = objective,
            currentProgress = 1,
            requiredAmount = 3,
            metadata = emptyMap(),
        )
        val result = handler.handleProgress(context)
        assertEquals(1, result.progressed)
        assertFalse(result.completed)
    }

    @Test
    fun useItemHandlerCompletesAtThreshold() {
        val handler = UseItemObjectiveHandler()
        val objective = FeaturePackLoader.QuestEntryDefinition("use_item", "STICK", 3, "Foloseste un bat")
        val context = ObjectiveContext(
            playerId = UUID.randomUUID(),
            objective = objective,
            currentProgress = 2,
            requiredAmount = 3,
            metadata = emptyMap(),
        )
        val result = handler.handleProgress(context)
        assertEquals(1, result.progressed)
        assertTrue(result.completed)
    }

    @Test
    fun useItemHandlerAlreadyCompleted() {
        val handler = UseItemObjectiveHandler()
        val objective = FeaturePackLoader.QuestEntryDefinition("use_item", "STICK", 3, "Foloseste un bat")
        val context = ObjectiveContext(
            playerId = UUID.randomUUID(),
            objective = objective,
            currentProgress = 3,
            requiredAmount = 3,
            metadata = emptyMap(),
        )
        val result = handler.handleProgress(context)
        assertEquals(0, result.progressed)
        assertTrue(result.completed)
    }

    @Test
    fun collectItemHandlerRegistered() {
        val registry = ObjectiveHandlerRegistry()
        registry.register(CollectItemObjectiveHandler())
        assertTrue(registry.supports("collect_item"))
    }

    @Test
    fun collectItemHandlerIncrements() {
        val handler = CollectItemObjectiveHandler()
        val context = ObjectiveContext(
            playerId = UUID.randomUUID(),
            objective = FeaturePackLoader.QuestEntryDefinition("collect_item", "STONE", 10, null),
            currentProgress = 5,
            requiredAmount = 10,
            metadata = emptyMap(),
        )
        val result = handler.handleProgress(context)
        assertEquals(1, result.progressed)
        assertFalse(result.completed)
    }

    @Test
    fun deliverToNpcHandlerRegistered() {
        val registry = ObjectiveHandlerRegistry()
        registry.register(DeliverToNpcObjectiveHandler())
        assertTrue(registry.supports("deliver_to_npc"))
    }

    @Test
    fun killMobHandlerRegistered() {
        val registry = ObjectiveHandlerRegistry()
        registry.register(KillMobObjectiveHandler())
        assertTrue(registry.supports("kill_mob"))
    }

    @Test
    fun visitRegionHandlerRegistered() {
        val registry = ObjectiveHandlerRegistry()
        registry.register(VisitRegionObjectiveHandler())
        assertTrue(registry.supports("visit_region"))
    }

    @Test
    fun visitPlaceHandlerRegistered() {
        val registry = ObjectiveHandlerRegistry()
        registry.register(VisitPlaceObjectiveHandler())
        assertTrue(registry.supports("visit_place"))
    }

    @Test
    fun allObjectiveTypesSupportedByRegistry() {
        val registry = ObjectiveHandlerRegistry()
        registry.register(UseItemObjectiveHandler())
        registry.register(EquipItemObjectiveHandler())
        registry.register(CollectItemObjectiveHandler())
        registry.register(DeliverToNpcObjectiveHandler())
        registry.register(KillMobObjectiveHandler())
        registry.register(TalkToNpcObjectiveHandler())
        registry.register(BreakBlockObjectiveHandler())
        registry.register(PlaceBlockObjectiveHandler())
        registry.register(CraftItemObjectiveHandler())
        registry.register(InspectNodeObjectiveHandler())
        registry.register(VisitRegionObjectiveHandler())
        registry.register(VisitPlaceObjectiveHandler())
        val allTypes = listOf(
            "use_item", "equip_item", "collect_item", "deliver_to_npc",
            "kill_mob", "talk_to_npc", "break_block", "place_block",
            "craft_item", "inspect_node", "visit_region", "visit_place",
        )
        for (type in allTypes) {
            assertTrue(registry.supports(type), "Handler pentru '$type' nu e inregistrat")
        }
    }
}
