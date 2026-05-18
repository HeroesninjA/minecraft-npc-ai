package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeaturePackDependencyValidatorTest {
    @Test
    fun returnsNoMissingDependenciesWhenAllDeclaredPacksExist() {
        val missing = FeaturePackDependencyValidator.missingDependencies(
            listOf("medieval", "social", "ainpc-scenario-medieval"),
            listOf("Medieval", "social")
        )

        assertTrue(missing.isEmpty())
    }

    @Test
    fun reportsMissingDependenciesInDeclaredOrder() {
        val missing = FeaturePackDependencyValidator.missingDependencies(
            listOf("medieval"),
            listOf("social", "economy", "social")
        )

        assertEquals(listOf("social", "economy"), missing)
    }

    @Test
    fun ignoresBlankDependencyValues() {
        val missing = FeaturePackDependencyValidator.missingDependencies(
            listOf("medieval"),
            listOf("", " ", "medieval")
        )

        assertTrue(missing.isEmpty())
    }

    @Test
    fun removesTransitiveUnavailablePacksFromCandidateSet() {
        val available = FeaturePackDependencyValidator.resolveAvailablePackIds(
            mapOf(
                "quest-addon" to listOf("base-addon"),
                "base-addon" to listOf("missing-shared"),
                "medieval" to listOf()
            )
        )

        assertEquals(setOf("medieval"), available)
    }
}
