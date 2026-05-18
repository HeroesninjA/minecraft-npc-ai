package ro.ainpc.engine

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.platform.RuntimeMode
import java.io.File

class FeaturePackMetadataValidatorTest {
    @Test
    @Throws(Exception::class)
    fun acceptsDeclaredAddonMetadataForCurrentRuntime() {
        val result = FeaturePackMetadataValidator.validate(
            load(
                """
                id: demo_pack
                name: "Demo Pack"
                addon:
                  type: "scenario"
                  version: "1.0.0"
                  primary_scenario: true
                  runtime_modes: ["standalone", "hybrid"]
                  capabilities: ["scenarios", "progression"]
                  dependencies: ["core_demo"]
                """
            ),
            File("demo_pack.yml"),
            RuntimeMode.STANDALONE
        )

        assertTrue(result.valid())
        assertTrue(result.errors().isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun rejectsUnknownRuntimeMode() {
        val result = FeaturePackMetadataValidator.validate(
            load(
                """
                id: demo_pack
                addon:
                  type: "feature"
                  runtime_modes: ["future_mode"]
                """
            ),
            File("demo_pack.yml"),
            RuntimeMode.STANDALONE
        )

        assertFalse(result.valid())
        assertTrue(result.errors().any { error -> error.contains("runtime necunoscut") })
    }

    @Test
    @Throws(Exception::class)
    fun rejectsPackThatDoesNotSupportCurrentRuntime() {
        val result = FeaturePackMetadataValidator.validate(
            load(
                """
                id: advanced_pack
                addon:
                  type: "feature"
                  runtime_modes: ["advanced"]
                """
            ),
            File("advanced_pack.yml"),
            RuntimeMode.STANDALONE
        )

        assertFalse(result.valid())
        assertTrue(result.errors().any { error -> error.contains("runtime-ul curent") })
    }

    @Test
    @Throws(Exception::class)
    fun rejectsInvalidAddonListMetadata() {
        val result = FeaturePackMetadataValidator.validate(
            load(
                """
                id: demo_pack
                addon:
                  type: "feature"
                  capabilities: "scenarios"
                """
            ),
            File("demo_pack.yml"),
            RuntimeMode.STANDALONE
        )

        assertFalse(result.valid())
        assertTrue(result.errors().any { error -> error.contains("capabilities trebuie sa fie lista") })
    }

    @Throws(InvalidConfigurationException::class)
    private fun load(content: String): YamlConfiguration {
        val configuration = YamlConfiguration()
        configuration.loadFromString(content)
        return configuration
    }
}
