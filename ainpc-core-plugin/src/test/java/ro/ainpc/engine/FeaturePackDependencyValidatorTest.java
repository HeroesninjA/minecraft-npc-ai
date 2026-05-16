package ro.ainpc.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeaturePackDependencyValidatorTest {

    @Test
    void returnsNoMissingDependenciesWhenAllDeclaredPacksExist() {
        List<String> missing = FeaturePackDependencyValidator.missingDependencies(
            List.of("medieval", "social", "ainpc-scenario-medieval"),
            List.of("Medieval", "social")
        );

        assertTrue(missing.isEmpty());
    }

    @Test
    void reportsMissingDependenciesInDeclaredOrder() {
        List<String> missing = FeaturePackDependencyValidator.missingDependencies(
            List.of("medieval"),
            List.of("social", "economy", "social")
        );

        assertEquals(List.of("social", "economy"), missing);
    }

    @Test
    void ignoresBlankDependencyValues() {
        List<String> missing = FeaturePackDependencyValidator.missingDependencies(
            List.of("medieval"),
            List.of("", " ", "medieval")
        );

        assertTrue(missing.isEmpty());
    }

    @Test
    void removesTransitiveUnavailablePacksFromCandidateSet() {
        Set<String> available = FeaturePackDependencyValidator.resolveAvailablePackIds(Map.of(
            "quest-addon", List.of("base-addon"),
            "base-addon", List.of("missing-shared"),
            "medieval", List.of()
        ));

        assertEquals(Set.of("medieval"), available);
    }
}
