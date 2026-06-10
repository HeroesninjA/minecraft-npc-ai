package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeaturePackDefaultsTest {
    @Test
    fun neutralFallbackDoesNotLoadMedievalPackOrProfessions() {
        val packs = linkedMapOf<String, FeaturePackLoader.FeaturePack>()
        val traits = linkedMapOf<String, FeaturePackLoader.TraitDefinition>()
        val professions = linkedMapOf<String, FeaturePackLoader.ProfessionDefinition>()

        FeaturePackDefaults.loadNeutralFallbackPack(
            packs,
            traits,
            professions,
            { pack, topology -> pack.addTopology(topology) },
            { pack, _ -> pack.addonDescriptor = null },
        )

        assertNotNull(packs["core_minimal"])
        assertNull(packs["medieval"])
        assertTrue(professions.containsKey("worker"))
        assertTrue(professions.containsKey("caretaker"))
        assertTrue(professions.containsKey("guide"))
        assertFalse(professions.containsKey("blacksmith"))
        assertFalse(professions.containsKey("farmer"))
        assertFalse(professions.containsKey("guard"))
    }
}
