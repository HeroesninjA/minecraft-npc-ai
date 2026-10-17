package ro.ainpc.story

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ro.ainpc.engine.QuestScoringResolver
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlace
import ro.ainpc.world.WorldPlaceInfo
import java.util.logging.Logger

class StoryStructureSignalResolverTest {
    @Test
    fun currentPlaceHookSurvivesGenericNearbyNodeLimit() {
        fun node(id: String, placeId: String, hook: String): WorldNodeInfo = WorldNodeInfo(
            id, "demo_sat", placeId, "npc_spawn", "world", 0.0, 64.0, 0.0, 1.0,
            mapOf("quest_hook" to hook),
        )
        val nearby = (1..12).map { node("other_$it", "demo_sat:house_2", "other_$it") }
        val local = node("local_hook", "demo_sat:house_1", "household_welcome")
        val resolver = StoryStructureSignalResolver()

        val selected = resolver.selectStoryRelevantNodes(nearby, "demo_sat:house_1", listOf(local, nearby.first()))

        assertEquals(listOf(local.id()) + nearby.take(4).map { it.id() }, selected.map { it.id() })
        assertEquals(listOf("node_quest_hook" to "household_welcome"),
            resolver.collectNearbyNodeQuestHookSignals(selected, "demo_sat:house_1"))
        assertEquals(nearby.take(5).map { it.id() },
            resolver.selectStoryRelevantNodes(nearby, "", listOf(local)).map { it.id() })
    }

    @Test
    fun configuredPlaceHooksReachContextAndEventKeyWithoutCreatingQuests() {
        val place = WorldPlace("workshop", "region", "Atelier", "world", PlaceType.CUSTOM,
            0, 0, 0, 4, 4, 4)
        place.putMetadata("story_category", "profession")
        place.putMetadata("quest_hooks", "repair_tool, metal_supply, repair_tool")
        val placeInfo = WorldPlaceInfo("workshop", "region", "Atelier", "world", PlaceType.CUSTOM,
            0, 0, 0, 4, 4, 4, emptyList(), null, true, place.getMetadata())
        val signals = StoryStructureSignalResolver().collectPlaceStructureSignals(placeInfo)
        val hooks = signals.filter { it.first == "quest_hook" }.map { it.second }
        val visit = requireNotNull(StructureStoryEventPlanner().planPlaceVisit("region", place, null))

        assertEquals(listOf("repair_tool", "metal_supply"), hooks)
        assertEquals("repair_tool,metal_supply", visit.payload["quest_hooks"])
        assertEquals("repair_tool", visit.eventKey)
        val context = StoryContextSnapshot(null, null, null, null, null, null,
            emptyList(), emptyList(), signals.map { (key, value) -> "$key=$value" }, emptyList())
        val demand = QuestScoringResolver().storyDemandSignals(context)
        assertEquals("structure_category=profession", demand.single { it.startsWith("structure_category=") })
        assertEquals(listOf("quest_hook=repair_tool", "quest_hook=metal_supply"),
            demand.filter { it.startsWith("quest_hook=") })
    }

    @Test
    fun customWorkplaceStoryCategoryCanBeConfiguredWithoutChangingKnownTypes() {
        val customPlace = WorldPlace("custom_workshop", "region", "Atelier", "world", PlaceType.CUSTOM,
            0, 0, 0, 4, 4, 4)
        customPlace.setTags(listOf("work", "leisure"))
        customPlace.putMetadata("story_category", " PROFESSION ")
        val customInfo = WorldPlaceInfo("custom_workshop", "region", "Atelier", "world", PlaceType.CUSTOM,
            0, 0, 0, 4, 4, 4, customPlace.getTags(), null, true, customPlace.getMetadata())
        val resolver = StoryStructureSignalResolver()

        assertEquals("profession", resolver.collectPlaceStructureSignals(customInfo)
            .single { it.first == "structure_category" }.second)
        assertEquals("structure_visit_profession",
            StructureStoryEventPlanner().planPlaceVisit("region", customPlace, null)?.eventType)
        assertEquals("profession", StructureStoryCategory.forPlaceType("forge", emptyList(),
            mapOf("story_category" to "leisure")))
    }

    @Test
    fun mappedPlaceHooksMatchVisitEventHooks() {
        val resolver = StoryStructureSignalResolver()
        val planner = StructureStoryEventPlanner()
        for ((type, tags, expectedHooks) in listOf(
            Triple(PlaceType.CASTLE_ROOM, emptyList(), listOf("civic_request", "public_event", "lost_record")),
            Triple(PlaceType.CAVE_ROOM, emptyList(), listOf("exploration", "danger", "recover_artifact")),
            Triple(PlaceType.CAMP, emptyList(), listOf("exploration", "route_report", "danger")),
            Triple(PlaceType.CUSTOM, listOf(" SOCIAL ", "trade", "leisure"), listOf("market_supply", "community_event")),
            Triple(PlaceType.CUSTOM, listOf(" MERCHANT ", "leisure"), listOf("market_supply")),
            Triple(PlaceType.CUSTOM, listOf(" MERCHANT ", "trade", "market", "leisure"), listOf("market_supply")),
        )) {
            val place = WorldPlace("place", "region", "Loc", "world", type, 0, 0, 0, 4, 4, 4)
            place.setTags(tags)
            val placeInfo = WorldPlaceInfo("place", "region", "Loc", "world", type,
                0, 0, 0, 4, 4, 4, tags, null, true, emptyMap())
            val contextHooks = resolver.collectPlaceStructureSignals(placeInfo)
                .filter { it.first == "quest_hook" }.map { it.second }

            assertEquals(expectedHooks, contextHooks, type.id)
            val visitHooks = planner.planPlaceVisit("region", place, null)!!.payload["quest_hooks"]
            assertEquals(contextHooks, visitHooks?.split(","), type.id)
        }
    }

    @Test
    fun mappedPlaceCategoryMatchesVisitEventCategory() {
        val resolver = StoryStructureSignalResolver()
        val planner = StructureStoryEventPlanner()
        for ((type, category) in listOf(
            PlaceType.FORGE to "profession",
            PlaceType.CASTLE_ROOM to "public",
            PlaceType.CAVE_ROOM to "exterior",
            PlaceType.CAMP to "exterior",
        )) {
            val place = WorldPlace("place", "region", "Loc", "world", type, 0, 0, 0, 4, 4, 4)
            val placeInfo = WorldPlaceInfo("place", "region", "Loc", "world", type,
                0, 0, 0, 4, 4, 4, emptyList(), null, true, emptyMap())

            assertEquals(category, resolver.collectPlaceStructureSignals(placeInfo)
                .single { it.first == "structure_category" }.second, type.id)
            val visit = planner.planPlaceVisit("region", place, null)
            assertEquals("structure_visit_$category", visit?.eventType, type.id)
            assertEquals(category, visit?.payload?.get("structure_category"), type.id)
        }
    }

    @Test
    fun distinctCurrentPlaceHooksAreSelectedBeforeDuplicateHooksFillTheLimit() {
        fun node(index: Int, hook: String): WorldNodeInfo = WorldNodeInfo(
            "demo_sat:house_1:node_$index", "demo_sat", "demo_sat:house_1", "npc_spawn", "world",
            0.0, 64.0, 0.0, 1.0, mapOf("quest_hook" to hook),
        )
        val repeatedNodes = (1..5).map { node(it, "household_welcome") }
        val distinctNode = node(6, "family_help")
        val resolver = StoryStructureSignalResolver()

        val selected = resolver.selectStoryRelevantNodes(repeatedNodes + distinctNode, "demo_sat:house_1")

        assertEquals(listOf(repeatedNodes.first().id(), distinctNode.id()) +
            repeatedNodes.drop(1).take(3).map { it.id() }, selected.map { it.id() })
        assertEquals(listOf("node_quest_hook" to "household_welcome", "node_quest_hook" to "family_help"),
            resolver.collectNearbyNodeQuestHookSignals(selected, "demo_sat:house_1"))
    }

    @Test
    fun currentPlaceHookIsSelectedAfterFiveOtherCurrentPlaceNodes() {
        fun node(index: Int, hook: String? = null): WorldNodeInfo = WorldNodeInfo(
            "demo_sat:house_1:node_$index", "demo_sat", "demo_sat:house_1", "quest_board", "world",
            0.0, 64.0, 0.0, 1.0, hook?.let { mapOf("quest_hook" to it) } ?: emptyMap(),
        )
        val ordinaryNodes = (1..5).map { node(it) }
        val hookNode = node(6, "household_welcome")
        val resolver = StoryStructureSignalResolver()

        val selected = resolver.selectStoryRelevantNodes(ordinaryNodes + hookNode, "demo_sat:house_1")

        assertEquals(listOf(hookNode.id()) + ordinaryNodes.take(4).map { it.id() }, selected.map { it.id() })
        assertEquals(listOf("node_quest_hook" to "household_welcome"),
            resolver.collectNearbyNodeQuestHookSignals(selected, "demo_sat:house_1"))
    }

    @Test
    fun currentPlaceHookIsSelectedEvenWhenOtherNodesFillTheLimit() {
        fun node(placeId: String, index: Int): WorldNodeInfo = WorldNodeInfo(
            "$placeId:node_$index", "demo_sat", placeId, "npc_spawn", "world",
            0.0, 64.0, 0.0, 1.0, mapOf("quest_hook" to "hook_$index"),
        )
        val otherNodes = (1..5).map { node("demo_sat:house_2", it) }
        val currentNode = node("demo_sat:house_1", 6)
        val resolver = StoryStructureSignalResolver()

        val selected = resolver.selectStoryRelevantNodes(otherNodes + currentNode, "demo_sat:house_1")

        assertEquals(5, selected.size)
        assertEquals(currentNode.id(), selected.first().id())
        assertEquals(otherNodes.take(4).map { it.id() }, selected.drop(1).map { it.id() })
        assertEquals(listOf("node_quest_hook" to "hook_6"),
            resolver.collectNearbyNodeQuestHookSignals(selected, "demo_sat:house_1"))
        assertEquals(otherNodes.map { it.id() },
            resolver.selectStoryRelevantNodes(otherNodes + currentNode, "").map { it.id() })
    }

    @Test
    fun nearbyQuestHooksAreScopedToTheCurrentPlaceAndBecomeDemandSignals() {
        fun node(id: String, placeId: String, hook: String): WorldNodeInfo = WorldNodeInfo(
            id, "demo_sat", placeId, "npc_spawn", "world", 0.0, 64.0, 0.0, 1.0,
            mapOf("quest_hook" to hook),
        )
        val nodes = listOf(
            node("demo_sat:house_1:spawn", "demo_sat:house_1", " household_welcome "),
            node("demo_sat:house_1:second", "demo_sat:house_1", "household_welcome"),
            node("demo_sat:house_2:spawn", "demo_sat:house_2", "other_story"),
            node("demo_sat:house_1:blank", "demo_sat:house_1", "  "),
        )

        val hooks = StoryStructureSignalResolver().collectNearbyNodeQuestHookSignals(nodes, "demo_sat:house_1")
        val context = StoryContextSnapshot(
            null, null, null, null, null, null, emptyList(), emptyList(),
            hooks.map { (key, value) -> "$key=$value" }, emptyList(),
        )

        assertEquals(listOf("node_quest_hook" to "household_welcome"), hooks)
        assertEquals(listOf("node_quest_hook=household_welcome"), QuestScoringResolver().storyDemandSignals(context))
        assertEquals(emptyList<Pair<String, String>>(), StoryStructureSignalResolver().collectNearbyNodeQuestHookSignals(nodes, ""))
    }

    @Test
    fun emptyQuestHookDoesNotMakeSpawnNodeStoryRelevant() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("EmptyNodeStoryHookTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        val house = worldAdmin.getPlaceModels("demo_sat").single { it.id == "demo_sat:house_1" }
        val spawn = worldAdmin.getNodeModelsForPlace(house.id).single { it.type.id == "npc_spawn" }
        for (hook in listOf("", "   ")) {
            spawn.putMetadata("quest_hook", hook)
            assertFalse(StoryStructureSignalResolver().isStoryRelevantNode(requireNotNull(worldAdmin.getNode(spawn.id))))
            assertNull(StructureStoryEventPlanner().planNodeInteraction("demo_sat", house, spawn))
        }
    }

    @Test
    fun leisureTagClassifiesOnlyExplicitlyTaggedCustomPlaces() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("LeisureStorySignalsTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        val resolver = StoryStructureSignalResolver()
        val planner = StructureStoryEventPlanner()
        val custom = worldAdmin.getPlaceModels("demo_sat").single { it.id == "demo_sat:altar" }
        val tavern = worldAdmin.getPlaceModels("demo_sat").single { it.id == "demo_sat:taverna" }

        assertEquals("unknown", resolver.collectPlaceStructureSignals(worldAdmin.getPlace(custom.id))
            .single { it.first == "structure_category" }.second)
        assertNull(planner.planPlaceVisit("demo_sat", custom, null))

        custom.setTags(custom.getTags() + " LEISURE ")
        tavern.setTags(tavern.getTags() + "leisure")
        for ((place, category) in listOf(custom to "leisure", tavern to "profession")) {
            val signals = resolver.collectPlaceStructureSignals(worldAdmin.getPlace(place.id))
            assertEquals(category, signals.single { it.first == "structure_category" }.second)
            assertEquals("structure_visit_$category", planner.planPlaceVisit("demo_sat", place, null)!!.eventType)
        }
    }

    @Test
    fun generatedForgeAndMarketHaveConsistentStoryContextAndVisitEvents() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("StructureStorySignalsTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        val resolver = StoryStructureSignalResolver()
        val eventPlanner = StructureStoryEventPlanner()
        val expectedHooks = mapOf(
            "demo_sat:fierarie" to setOf("forge_order", "repair_tool", "metal_supply"),
            "demo_sat:piata" to setOf("market_supply", "community_event", "trade"),
        )

        for ((placeId, hooks) in expectedHooks) {
            val place = requireNotNull(worldAdmin.getPlace(placeId))
            val signals = resolver.collectPlaceStructureSignals(place)
            val model = worldAdmin.getPlaceModels("demo_sat").single { it.id == placeId }
            val visit = eventPlanner.planPlaceVisit("demo_sat", model, null)

            assertEquals("profession", signals.single { it.first == "structure_category" }.second, placeId)
            assertEquals(hooks, signals.filter { it.first == "quest_hook" }.map { it.second }.toSet(), placeId)
            assertNotNull(visit, placeId)
            assertEquals("structure_visit_profession", visit!!.eventType, placeId)
            assertEquals("profession", visit.payload["structure_category"], placeId)
        }
    }
}
