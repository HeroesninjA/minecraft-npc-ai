package ro.ainpc.routine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCState
import java.util.UUID

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
        npc.occupation = "worker"

        val assignment = engine.assign(npc, 6000)

        assertEquals(RoutineSlot.WORK, assignment.slot())
        assertEquals(NPCState.WORKING, assignment.targetState())
        assertEquals(npc.workAnchor, assignment.targetAnchor())
    }

    @Test
    fun lowSocialNeedUsesSocialAnchorOutsideNight() {
        val npc = npcWithAnchors()
        npc.socialNeedLevel = 20

        val assignment = engine.assign(npc, 9000)

        assertEquals(RoutineSlot.WORK, assignment.slot())
        assertEquals(NPCState.WORKING, assignment.targetState())
        assertEquals(npc.workAnchor, assignment.targetAnchor())
    }

    @Test
    fun missingWorkAnchorFallsBackToSocialAnchor() {
        val npc = npcWithAnchors()
        npc.workAnchor = null

        val assignment = engine.assign(npc, 6000)

        assertEquals(RoutineSlot.WORK, assignment.slot())
        assertNull(assignment.targetAnchor())
    }

    @Test
    fun previewDayExposesStableGuiScheduleSlots() {
        val npc = npcWithAnchors()
        npc.occupation = "worker"

        val preview = engine.previewDay(npc)

        assertEquals(4, preview.size)
        assertEquals("Noapte", preview[0].label())
        assertEquals(RoutineSlot.HOME, preview[0].assignment()!!.slot())
        assertEquals("Dimineata", preview[1].label())
        assertEquals(RoutineSlot.WORK, preview[1].assignment()!!.slot())
        assertEquals("Seara", preview[3].label())
        assertEquals(RoutineSlot.HOME, preview[3].assignment()!!.slot())
        assertEquals("doarme", preview[0].assignment()!!.activity())
        assertFalse(preview[1].assignment()!!.activity()!!.isBlank())
    }

    @Test
    fun missingAllAnchorsFallsBackToIdleWithClearReason() {
        val npc = AINPC(null)
        npc.name = "Ion"
        npc.uuid = UUID(0L, 1L)

        val assignment = engine.assign(npc, 6000)

        assertEquals(RoutineSlot.WORK, assignment.slot())
        assertEquals(NPCState.WORKING, assignment.targetState())
        assertNull(assignment.targetAnchor())
        assertEquals("munca", assignment.activity())
        assertEquals("Munca", assignment.goal())
    }

    @Test
    fun negativeWorldTimeNormalizesIntoNightSchedule() {
        val npc = npcWithAnchors()

        val assignment = engine.assign(npc, -1000)

        assertEquals(RoutineSlot.HOME, assignment.slot())
        assertEquals(NPCState.SLEEPING, assignment.targetState())
        assertEquals(npc.homeAnchor, assignment.targetAnchor())
    }

    @Test
    fun afternoonWithoutWorkAnchorFallsBackToSocialAnchor() {
        val npc = npcWithAnchors()
        npc.workAnchor = null

        val assignment = engine.assign(npc, 13000)

        assertEquals(RoutineSlot.HOME, assignment.slot())
        assertEquals("odihneste", assignment.activity())
        assertEquals(npc.homeAnchor, assignment.targetAnchor())
    }

    @Test
    fun eveningWithoutSocialAnchorReturnsHome() {
        val npc = npcWithAnchors()
        npc.socialAnchor = null

        val assignment = engine.assign(npc, 17000)

        assertEquals(RoutineSlot.HOME, assignment.slot())
        assertEquals("doarme", assignment.activity())
        assertEquals(npc.homeAnchor, assignment.targetAnchor())
    }

    @Test
    fun unsafeNeedsOverrideWorkAndSocialSlots() {
        val npc = npcWithAnchors()
        npc.socialNeedLevel = 10
        npc.safetyLevel = 20

        val assignment = engine.assign(npc, 9000)

        assertEquals(RoutineSlot.WORK, assignment.slot())
        assertEquals("munca", assignment.activity())
        assertEquals(npc.workAnchor, assignment.targetAnchor())
    }

    @Test
    fun stableRoutineOffsetStaggersEveningDeparture() {
        val earlyNpc = findNpcWithOffset { it < 100L }
        val lateNpc = findNpcWithOffset { it >= 1500L }
        assertTrue(stableRoutineOffset(earlyNpc) < 100L)
        assertTrue(stableRoutineOffset(lateNpc) >= 1500L)

        val earlyAssignment = engine.assign(earlyNpc, 11400)
        val lateAssignment = engine.assign(lateNpc, 11400)

        assertEquals(RoutineSlot.WORK, earlyAssignment.slot())
        assertEquals(RoutineSlot.HOME, lateAssignment.slot())
    }

    @Test
    fun occupationBiasSeparatesFarmerAndMerchantRoutines() {
        val farmer = npcWithAnchors().apply {
            name = "Ion"
            occupation = "farmer"
            uuid = UUID(0L, 42L)
        }
        val merchant = npcWithAnchors().apply {
            name = "Ion"
            occupation = "merchant"
            uuid = UUID(0L, 42L)
        }

        val time = findDivergentTime(farmer, merchant)

        val farmerAssignment = engine.assign(farmer, time)
        val merchantAssignment = engine.assign(merchant, time)

        assertTrue(farmerAssignment.slot() != merchantAssignment.slot())
        assertFalse(farmerAssignment.goal().isNullOrBlank())
        assertFalse(merchantAssignment.goal().isNullOrBlank())
    }

    private fun npcWithAnchors(): AINPC {
        val npc = AINPC(null)
        npc.name = "Ion"
        npc.uuid = UUID(0L, 1L)
        npc.homeAnchor = anchor("home", "casa")
        npc.workAnchor = anchor("work", "atelier")
        npc.socialAnchor = anchor("social", "piata")
        return npc
    }

    private fun anchor(type: String, label: String): AINPC.OwnedLocation {
        return AINPC.OwnedLocation(type, label, "world", 10.0, 64.0, 10.0)
    }

    private fun findNpcWithOffset(predicate: (Long) -> Boolean): AINPC {
        for (seed in 0L..2000L) {
            val npc = npcWithAnchors().apply {
                name = "Ion-$seed"
                occupation = "worker"
                uuid = UUID(0L, seed)
            }
            if (predicate(stableRoutineOffset(npc))) {
                return npc
            }
        }
        error("No NPC matched the requested routine offset predicate")
    }

    private fun stableRoutineOffset(npc: AINPC): Long {
        val key = buildString {
            append(npc.uuid.toString())
            append('|')
            append(npc.name)
        }
        return Math.floorMod(key.hashCode().toLong(), 1800L)
    }

    private fun findDivergentTime(first: AINPC, second: AINPC): Long {
        for (time in 0L until 24000L step 100L) {
            if (engine.assign(first, time).slot() != engine.assign(second, time).slot()) {
                return time
            }
        }
        error("No divergent routine time found for the compared occupations")
    }

    private fun expectedGoalFor(profileId: String, slot: RoutineSlot): String {
        return when (profileId) {
            "farmer" -> when (slot) {
                RoutineSlot.HOME -> "sa se intoarca la casa si gospodarie"
                RoutineSlot.WORK -> "sa lucreze pe camp"
                RoutineSlot.SOCIAL -> "sa vorbeasca cu ceilalti fermieri"
                RoutineSlot.IDLE -> "sa astepte sa se deschida campul"
            }
            "merchant" -> when (slot) {
                RoutineSlot.HOME -> "sa pregateasca marfa acasa"
                RoutineSlot.WORK -> "sa vanda si sa cumpere in piata"
                RoutineSlot.SOCIAL -> "sa negocieze si sa discute"
                RoutineSlot.IDLE -> "sa astepte clienti"
            }
            else -> error("Unexpected profile id: $profileId")
        }
    }
}
