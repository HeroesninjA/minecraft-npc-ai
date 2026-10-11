package ro.ainpc.routine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState

class RoutineDestinationTest {
    private val resolver = RoutineDecisionResolver()

    @Test
    fun socialScheduleUsesAssignedGardenTavernOrMarketAnchor() {
        val npc = AINPC(null)
        val profile = schedule("SOCIAL")
        for (label in listOf("garden", "tavern", "market", "community_hall")) {
            val anchor = AINPC.OwnedLocation("social", label, "world", 10.0, 65.0, 20.0)
            npc.socialAnchor = anchor
            val assignment = resolver.assign(npc, profile, null, 9000)
            assertEquals(RoutineSlot.SOCIAL, assignment.slot())
            assertEquals(NPCState.SOCIALIZING, assignment.targetState())
            assertSame(anchor, assignment.targetAnchor())
        }
    }

    @Test
    fun missingScheduledDestinationsNeverReportWorkRestOrSocialActivity() {
        for (slot in listOf("HOME", "WORK", "SOCIAL")) {
            val assignment = resolver.assign(AINPC(null), schedule(slot), null, 9000)
            assertEquals(RoutineSlot.IDLE, assignment.slot())
            assertEquals(NPCState.IDLE, assignment.targetState())
            assertNull(assignment.targetAnchor())
        }
    }

    @Test
    fun malformedAnchorCoordinatesOrWorldAreRejected() {
        val npc = AINPC(null)
        for (anchor in listOf(
            AINPC.OwnedLocation("social", "garden", " ", 1.0, 65.0, 1.0),
            AINPC.OwnedLocation("social", "garden", "world", Double.NaN, 65.0, 1.0),
            AINPC.OwnedLocation("social", "garden", "world", 1.0, Double.POSITIVE_INFINITY, 1.0),
            AINPC.OwnedLocation("social", "garden", "world", 1.0, 65.0, Double.NEGATIVE_INFINITY),
        )) {
            npc.socialAnchor = anchor
            assertEquals(RoutineSlot.IDLE, resolver.assign(npc, schedule("SOCIAL"), null, 9000).slot())
        }
    }

    @Test
    fun configuredDestinationRestoresScheduledWorkOnNextResolution() {
        val npc = AINPC(null)
        val profile = schedule("WORK")
        assertEquals(RoutineSlot.IDLE, resolver.assign(npc, profile, null, 9000).slot())

        val anchor = AINPC.OwnedLocation("work", "shop", "world", 10.0, 65.0, 20.0)
        npc.workAnchor = anchor
        val assignment = resolver.assign(npc, profile, null, 9000)
        assertEquals(RoutineSlot.WORK, assignment.slot())
        assertSame(anchor, assignment.targetAnchor())
    }

    private fun schedule(slot: String) = BehaviorProfile(
        profileId = "activity_test",
        schedule = listOf(BehaviorProfile.ScheduleEntry("Activity", 0, 24000, slot, "activity")),
    )
}
