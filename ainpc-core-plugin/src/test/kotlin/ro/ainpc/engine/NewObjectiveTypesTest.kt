package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NewObjectiveTypesTest {

    @Test
    fun useItemRegistryAliases() {
        assertEquals("use_item", ObjectiveTypeAliasRegistry.normalize("use"))
        assertEquals("use_item", ObjectiveTypeAliasRegistry.normalize("consume"))
        assertEquals("use_item", ObjectiveTypeAliasRegistry.normalize("drink"))
        assertEquals("use_item", ObjectiveTypeAliasRegistry.normalize("eat"))
        assertEquals("use_item", ObjectiveTypeAliasRegistry.normalize("use_item"))
    }

    @Test
    fun equipItemRegistryAliases() {
        assertEquals("equip_item", ObjectiveTypeAliasRegistry.normalize("equip"))
        assertEquals("equip_item", ObjectiveTypeAliasRegistry.normalize("wear"))
        assertEquals("equip_item", ObjectiveTypeAliasRegistry.normalize("don"))
        assertEquals("equip_item", ObjectiveTypeAliasRegistry.normalize("equip_item"))
    }

    @Test
    fun useItemIsSupported() {
        assertTrue(ObjectiveTypeAliasRegistry.isSupported("use_item"))
        assertTrue(ObjectiveTypeAliasRegistry.isSupported("use"))
        assertTrue(ObjectiveTypeAliasRegistry.isSupported("consume"))
    }

    @Test
    fun equipItemIsSupported() {
        assertTrue(ObjectiveTypeAliasRegistry.isSupported("equip_item"))
        assertTrue(ObjectiveTypeAliasRegistry.isSupported("equip"))
        assertTrue(ObjectiveTypeAliasRegistry.isSupported("wear"))
    }

    @Test
    fun registryHas12Types() {
        assertEquals(12, ObjectiveTypeAliasRegistry.supportedTypes().size)
    }

    @Test
    fun questCreateGuiIncludesNewTypes() {
        val source = java.io.File("src/main/kotlin/ro/ainpc/gui/screens/QuestCreateGui.kt").readText()
        assertTrue(source.contains("\"use_item\""))
        assertTrue(source.contains("\"equip_item\""))
    }

    @Test
    fun questDraftValidatorIncludesNewTypes() {
        val source = java.io.File("src/main/kotlin/ro/ainpc/engine/QuestDraftValidator.kt").readText()
        assertTrue(source.contains("\"use_item\""))
        assertTrue(source.contains("\"equip_item\""))
    }

    @Test
    fun questCreateGuiHasTargetOptionsForNewTypes() {
        val source = java.io.File("src/main/kotlin/ro/ainpc/gui/screens/QuestCreateGui.kt").readText()
        assertTrue(source.contains("use_item"))
        assertTrue(source.contains("equip_item"))
    }

    @Test
    fun typoSuggestionForUseItem() {
        val suggestion = ObjectiveTypeAliasRegistry.suggestCorrection("us")
        assertEquals("use_item", suggestion)
    }

    @Test
    fun typoSuggestionForEquipItem() {
        val suggestion = ObjectiveTypeAliasRegistry.suggestCorrection("eqip")
        assertEquals("equip_item", suggestion)
    }
}
