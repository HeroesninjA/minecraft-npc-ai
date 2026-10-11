package ro.ainpc.story

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.engine.QuestScoringResolver
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldContextSnapshot
import ro.ainpc.world.WorldPlaceInfo
import java.util.logging.Logger

class NpcBindingStorySignalResolverTest {
    @Test
    fun resolvedNpcBindingsExposePlaceIdsWithoutMakingThemQuestDemand() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("NpcBindingStorySignalResolverTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        fun binding(role: String, place: WorldPlaceInfo): WorldContextSnapshot.NpcBindingInfo =
            WorldContextSnapshot.NpcBindingInfo(role, place.displayName(), "world",
                (place.minX() + place.maxX()) / 2.0,
                (place.minY() + place.maxY()) / 2.0,
                (place.minZ() + place.maxZ()) / 2.0)
        val home = requireNotNull(worldAdmin.getPlace("demo_sat:house_1"))
        val work = requireNotNull(worldAdmin.getPlace("demo_sat:fierarie"))
        val social = requireNotNull(worldAdmin.getPlace("demo_sat:piata"))
        val signals = NpcBindingStorySignalResolver().collect(listOf(
            binding("home", home), binding("work", work), binding("social", social),
            WorldContextSnapshot.NpcBindingInfo("work", "invalid", "other_world", work.minX().toDouble(),
                work.minY().toDouble(), work.minZ().toDouble()),
            WorldContextSnapshot.NpcBindingInfo("other", "invalid", "world", work.minX().toDouble(),
                work.minY().toDouble(), work.minZ().toDouble()),
            WorldContextSnapshot.NpcBindingInfo("home", "invalid", "world", Double.NaN, 64.0, 0.0),
            WorldContextSnapshot.NpcBindingInfo("home", "invalid", "world", 1.0e100, 64.0, 0.0),
        ), worldAdmin)

        assertEquals(listOf("npc_home_place_id=${home.id()}", "npc_work_place_id=${work.id()}",
            "npc_social_place_id=${social.id()}"), signals)
        val context = StoryContextSnapshot("Mara", "fierar", "Player", null,
            null, null, emptyList(), emptyList(), signals, emptyList())
        assertEquals(emptyList<String>(), QuestScoringResolver().storyDemandSignals(context))
    }
}
