package ro.ainpc.engine;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import ro.ainpc.platform.RuntimeMode;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeaturePackMetadataValidatorTest {

    @Test
    void acceptsDeclaredAddonMetadataForCurrentRuntime() throws Exception {
        FeaturePackMetadataValidator.ValidationResult result = FeaturePackMetadataValidator.validate(load("""
            id: demo_pack
            name: "Demo Pack"
            addon:
              type: "scenario"
              version: "1.0.0"
              primary_scenario: true
              runtime_modes: ["standalone", "hybrid"]
              capabilities: ["scenarios", "progression"]
              dependencies: ["core_demo"]
            """), new File("demo_pack.yml"), RuntimeMode.STANDALONE);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void rejectsUnknownRuntimeMode() throws Exception {
        FeaturePackMetadataValidator.ValidationResult result = FeaturePackMetadataValidator.validate(load("""
            id: demo_pack
            addon:
              type: "feature"
              runtime_modes: ["future_mode"]
            """), new File("demo_pack.yml"), RuntimeMode.STANDALONE);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("runtime necunoscut")));
    }

    @Test
    void rejectsPackThatDoesNotSupportCurrentRuntime() throws Exception {
        FeaturePackMetadataValidator.ValidationResult result = FeaturePackMetadataValidator.validate(load("""
            id: advanced_pack
            addon:
              type: "feature"
              runtime_modes: ["advanced"]
            """), new File("advanced_pack.yml"), RuntimeMode.STANDALONE);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("runtime-ul curent")));
    }

    @Test
    void rejectsInvalidAddonListMetadata() throws Exception {
        FeaturePackMetadataValidator.ValidationResult result = FeaturePackMetadataValidator.validate(load("""
            id: demo_pack
            addon:
              type: "feature"
              capabilities: "scenarios"
            """), new File("demo_pack.yml"), RuntimeMode.STANDALONE);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("capabilities trebuie sa fie lista")));
    }

    private YamlConfiguration load(String content) throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(content);
        return configuration;
    }
}
