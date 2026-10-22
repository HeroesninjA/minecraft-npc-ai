package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintWizardFlowTest {
    @Test
    fun `walks through the prompts and keeps a draft for review`() {
        var blueprint = WorldBlueprint("world")
        var step = WorldBlueprintWizardFlow.firstMissing(blueprint)
        assertEquals(WorldBlueprintWizardStep.STORY, step)

        fun answer(text: String) {
            val update = WorldBlueprintWizardFlow.advance(blueprint, step, text)
            blueprint = update.blueprint
            step = update.nextStep
        }

        answer("Doua sate rivale")
        answer("Clima: Ierni lungi")
        answer("gata")
        answer("3")
        answer("Fierar 2 Repara unelte")
        answer("fermier 0 Dezactivat temporar")
        answer("gata")
        answer("Case de piatra si lemn")
        answer("drumuri: Leaga satele")
        answer("gata")

        assertEquals(WorldBlueprintWizardStep.REVIEW, step)
        assertEquals("Ierni lungi", blueprint.properties["clima"])
        assertEquals(2, blueprint.professions["fierar"]?.maxNpcs)
        assertEquals(0, blueprint.professions["fermier"]?.maxNpcs)
        assertTrue(WorldBlueprintPolicy.completionIssues(blueprint).isEmpty())
    }

    @Test
    fun `rejects skipping required entries and invalid values`() {
        val blueprint = WorldBlueprint("world", story = "Poveste")

        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.PROPERTY, "gata")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.PROPERTY, "cheie fara separator")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.PROFESSION_LIMIT, "65")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.PROFESSION, "fierar 513 atelier")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.MAP_RULE, "gata")
        }
        assertTrue(blueprint.properties.isEmpty())
    }

    @Test
    fun `resumes at the first incomplete category without replacing existing data`() {
        val blueprint = WorldBlueprint("world", story = "Poveste", properties = mapOf("clima" to "Rece"))

        assertEquals(WorldBlueprintWizardStep.PROFESSION_LIMIT, WorldBlueprintWizardFlow.firstMissing(blueprint))
        val update = WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.PROFESSION_LIMIT, "4")
        assertEquals("Rece", update.blueprint.properties["clima"])
        assertEquals(4, update.blueprint.professionTypeLimit)
        assertEquals(WorldBlueprintWizardStep.PROFESSION, update.nextStep)
    }

    @Test
    fun `edits one selected category without resetting other fields`() {
        val blueprint = WorldBlueprint(
            "world", "Poveste veche", mapOf("clima" to "Rece"), 4,
            mapOf("fierar" to WorldProfessionBlueprint(2, "Atelier")),
            "Piatra", mapOf("drumuri" to "Leaga satele"),
        )

        assertEquals(WorldBlueprintWizardStep.STORY, WorldBlueprintWizardFlow.editStep("STORY"))
        assertEquals(null, WorldBlueprintWizardFlow.editStep("unknown"))
        val story = WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.STORY, "Poveste noua")
        assertEquals(WorldBlueprintWizardStep.PROPERTY, story.nextStep)
        assertEquals("Poveste noua", story.blueprint.story)
        assertEquals(blueprint.properties, story.blueprint.properties)

        val profession = WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.PROFESSION,
            "fierar 3 Atelier nou")
        assertEquals(WorldBlueprintWizardStep.PROFESSION, profession.nextStep)
        assertEquals(3, profession.blueprint.professions["fierar"]?.maxNpcs)
        assertEquals(WorldBlueprintWizardStep.ARCHITECTURE,
            WorldBlueprintWizardFlow.advance(profession.blueprint, WorldBlueprintWizardStep.PROFESSION, "gata").nextStep)
    }

    @Test
    fun `removes collection entries but cannot finish an empty required category`() {
        val blueprint = WorldBlueprint("world", properties = mapOf("clima" to "Rece"),
            professions = mapOf("fierar" to WorldProfessionBlueprint(1, "Atelier")),
            mapRules = mapOf("drumuri" to "Leaga satele"))
        val properties = WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.PROPERTY, "sterge CLIMA")
        assertTrue(properties.blueprint.properties.isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintWizardFlow.advance(properties.blueprint, WorldBlueprintWizardStep.PROPERTY, "gata")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.PROFESSION, "sterge brutar")
        }
        assertTrue(WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.PROFESSION,
            "sterge fierar").blueprint.professions.isEmpty())
        assertTrue(WorldBlueprintWizardFlow.advance(blueprint, WorldBlueprintWizardStep.MAP_RULE,
            "sterge drumuri").blueprint.mapRules.isEmpty())
        assertEquals("clima", WorldBlueprintWizardFlow.currentSummary(blueprint, WorldBlueprintWizardStep.PROPERTY))
    }
}
