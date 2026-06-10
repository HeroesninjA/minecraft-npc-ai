package ro.ainpc.platform.features

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuntimeFeatureStateTest {
    @Test
    fun featureKeyParsesIdsAndConfigPaths() {
        assertEquals(RuntimeFeatureKey.QUEST, RuntimeFeatureKey.fromId("quest"))
        assertEquals(RuntimeFeatureKey.QUEST, RuntimeFeatureKey.fromId("QUEST"))
        assertEquals(RuntimeFeatureKey.QUEST, RuntimeFeatureKey.fromId("features.quest"))
        assertEquals(RuntimeFeatureKey.QUEST, RuntimeFeatureKey.fromId("features_quest"))
        assertEquals(RuntimeFeatureKey.GENERATION, RuntimeFeatureKey.fromId("features.generation"))
    }

    @Test
    fun statusExposesRuntimeUseSemantics() {
        assertTrue(RuntimeFeatureStatus.ENABLED.allowsRuntimeUse())
        assertTrue(RuntimeFeatureStatus.OPTIONAL.allowsRuntimeUse())
        assertTrue(RuntimeFeatureStatus.FALLBACK.allowsRuntimeUse())
        assertTrue(RuntimeFeatureStatus.EXPERIMENTAL.allowsRuntimeUse())
        assertFalse(RuntimeFeatureStatus.DISABLED.allowsRuntimeUse())
        assertFalse(RuntimeFeatureStatus.BLOCKED.allowsRuntimeUse())
        assertEquals(RuntimeFeatureStatus.BLOCKED, RuntimeFeatureStatus.fromId("blocked"))
    }

    @Test
    fun stateSanitizesReasonsAndKeepsSourcesImmutable() {
        val source = RuntimeFeatureSource.of("config", "features.quest", "true", 10)
        val state = RuntimeFeatureState.of(
            RuntimeFeatureKey.QUEST,
            RuntimeFeatureStatus.ENABLED,
            listOf(" config enabled ", "", "config enabled"),
            listOf(source)
        )

        assertTrue(state.enabled())
        assertFalse(state.blocked())
        assertEquals(listOf("config enabled"), state.reasons())
        assertEquals(listOf(source), state.sources())
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (state.reasons() as MutableList<String>).add("mutated")
        }
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (state.sources() as MutableList<RuntimeFeatureSource>).clear()
        }
    }

    @Test
    fun helpersCreateBooleanAndBlockedStates() {
        val disabled = RuntimeFeatureState.fromBoolean(
            RuntimeFeatureKey.AI,
            false,
            RuntimeFeatureSource.of("config", "features.ai", "false"),
            "features.ai=false"
        )
        val blocked = RuntimeFeatureState.blocked(
            RuntimeFeatureKey.QUEST,
            "addon conflict",
            listOf(RuntimeFeatureSource.of("addon", "conflicts", "quest"))
        )

        assertEquals(RuntimeFeatureStatus.DISABLED, disabled.status())
        assertFalse(disabled.enabled())
        assertEquals(listOf("features.ai=false"), disabled.reasons())
        assertEquals(RuntimeFeatureStatus.BLOCKED, blocked.status())
        assertTrue(blocked.blocked())
        assertFalse(blocked.enabled())
    }
}
