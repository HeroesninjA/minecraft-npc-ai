package ro.ainpc.routine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState

class RoutineEngineTest {
    private val engine = RoutineEngine()

    @Test
    fun nightSendsNpcHomeToSleep() {
        val npc = npcWithAnchors()
        val assignment = engine.assign(npc, 19000)
        assertEquals(RoutineSlot.HOME, assignment.slot())
        assertEquals(NPCState.SLEEPING, assignment.targetState())
        assertEquals(npc.homeAnchor, assignment.targetAnchor())
    }

    @Test
    fun workHoursSendNpcToWorkAnchor() {
        val npc = npcWithAnchors()
        npc.occupation = "fierar"

        val assignment = engine.assign(npc, 6000)

        assertEquals(RoutineSlot.WORK, assignment.slot())
        assertEquals(NPCState.CRAFTING, assignment.targetState())
        assertEquals(npc.workAnchor, assignment.targetAnchor())
    }

    @Test
    fun lowSocialNeedUsesSocialAnchorOutsideNight() {
        val npc = npcWithAnchors()
        npc.socialNeedLevel = 20

        val assignment = engine.assign(npc, 9000)

        assertEquals(RoutineSlot.SOCIAL, assignment.slot())
        assertEquals(NPCState.SOCIALIZING, assignment.targetState())
        assertEquals(npc.socialAnchor, assignment.targetAnchor())
    }

    @Test
    fun missingWorkAnchorFallsBackToSocialAnchor() {
        val npc = npcWithAnchors()
        npc.workAnchor = null

        val assignment = engine.assign(npc, 6000)

        assertEquals(RoutineSlot.SOCIAL, assignment.slot())
        assertEquals(npc.socialAnchor, assignment.targetAnchor())
    }

    @Test
    fun previewDayExposesStableGuiScheduleSlots() {
        val npc = npcWithAnchors()
        npc.occupation = "fierar"

        val preview = engine.previewDay(npc)

        assertEquals(4, preview.size)
        assertEquals("Noapte", preview[0].label())
        assertEquals(RoutineSlot.HOME, preview[0].assignment()!!.slot())
        assertEquals("Dimineata", preview[1].label())
        assertEquals(RoutineSlot.WORK, preview[1].assignment()!!.slot())
        assertEquals("Seara", preview[3].label())
        assertEquals(RoutineSlot.SOCIAL, preview[3].assignment()!!.slot())
        assertFalse(preview[1].assignment()!!.activity()!!.isBlank())
    }

    private fun npcWithAnchors(): AINPC {
        val npc = AINPC(null)
        npc.name = "Ion"
        npc.homeAnchor = anchor("home", "casa")
        npc.workAnchor = anchor("work", "atelier")
        npc.socialAnchor = anchor("social", "piata")
        return npc
    }

    private fun anchor(type: String, label: String): AINPC.OwnedLocation {
        return AINPC.OwnedLocation(type, label, "world", 10.0, 64.0, 10.0)
    }
}
