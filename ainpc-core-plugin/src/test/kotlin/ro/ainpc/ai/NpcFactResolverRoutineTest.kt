package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NpcFactResolverRoutineTest {
    private fun facts(activity: String, routine: String) = NpcFactResolver.NpcFacts(
        "Radu", "fierar", "calm", "vorbeste", activity, "sat", routine,
    )

    @Test
    fun `activity during conversation mentions actual routine without claiming it is complete`() {
        assertEquals(
            "Vorbesc cu tine acum; in program am: merge la lucru.",
            NpcFactResolver.resolve("Ce faci?", facts("vorbesc", "merge la lucru")).orElseThrow(),
        )
        assertEquals(
            "Lucrez la atelier.",
            NpcFactResolver.resolve("Ce faci?", facts("lucrez la atelier", "merge la lucru")).orElseThrow(),
        )
    }

    @Test
    fun `schedule questions use the current plan and handle missing plans`() {
        assertEquals(
            "Acum am in program: sa lucrez.",
            NpcFactResolver.resolve("Care e rutina ta?", facts("vorbesc", "work")).orElseThrow(),
        )
        assertEquals(
            "Nu am un program stabilit acum.",
            NpcFactResolver.resolve("Ce urmeaza sa faci?", facts("vorbesc", "")).orElseThrow(),
        )
    }

    @Test
    fun `internal navigation failures are not spoken as a schedule`() {
        val brokenPlan = facts("vorbesc", "ancora WORK lipseste sau este invalida")

        assertEquals("Vorbesc.", NpcFactResolver.resolve("Ce faci?", brokenPlan).orElseThrow())
        assertEquals(
            "Nu am un program stabilit acum.",
            NpcFactResolver.resolve("Ce ai in program?", brokenPlan).orElseThrow(),
        )
    }
}
