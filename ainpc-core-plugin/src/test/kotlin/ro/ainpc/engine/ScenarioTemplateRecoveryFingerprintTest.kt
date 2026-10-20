package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NpcScenarioActorDefinition

class ScenarioTemplateRecoveryFingerprintTest {
    @Test
    fun `fingerprint is stable and changes with phases or actor definitions`() {
        val original = template()
        val same = template()

        assertEquals(ScenarioTemplateRecoveryFingerprint.compute(original),
            ScenarioTemplateRecoveryFingerprint.compute(same))
        same.addPhase("LATER", "")
        assertNotEquals(ScenarioTemplateRecoveryFingerprint.compute(original),
            ScenarioTemplateRecoveryFingerprint.compute(same))
        same.phases.removeAt(same.phases.lastIndex)
        same.actors["guide"] = NpcScenarioActorDefinition("guide", "Another Guide")
        assertNotEquals(ScenarioTemplateRecoveryFingerprint.compute(original),
            ScenarioTemplateRecoveryFingerprint.compute(same))
    }

    private fun template(): ScenarioTemplate = ScenarioTemplate(ScenarioType.THEFT).apply {
        templateId = "demo:theft"
        addPhase("START", "")
        addActor("guide", NpcScenarioActorDefinition("guide", "Guide"))
    }
}
