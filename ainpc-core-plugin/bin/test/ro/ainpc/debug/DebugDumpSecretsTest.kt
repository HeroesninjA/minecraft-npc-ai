package ro.ainpc.debug

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebugDumpSecretsTest {
    @Test
    fun redactsCommonSecretShapesFromPlainText() {
        val raw = """
            api_key: sk-test-secret-value
            openai_api_key: sk-openai-secret-value
            password = very-secret-password
            Authorization: Bearer abcdefghijklmnopqrstuvwxyz
            env OPENAI_API_KEY=sk-env-secret-value
        """.trimIndent()

        val redacted = DebugDumpSecrets.redactText(raw)

        assertFalse(redacted.contains("sk-test-secret-value"))
        assertFalse(redacted.contains("sk-openai-secret-value"))
        assertFalse(redacted.contains("very-secret-password"))
        assertFalse(redacted.contains("abcdefghijklmnopqrstuvwxyz"))
        assertFalse(redacted.contains("sk-env-secret-value"))
        assertTrue(redacted.contains("<redacted>"))
        assertFalse(DebugDumpSecrets.containsPotentialSecret(redacted))
    }

    @Test
    fun sanitizeConfigRedactsNestedDebugDumpSecrets() {
        val config = YamlConfiguration()
        config.set("openai.api_key", "sk-config-secret-value")
        config.set("openai.base_url", "https://api.openai.com/v1")
        config.set("debug.token", "debug-token-value")
        config.set("database.password", "db-password-value")

        val sanitized = DebugDumpFormatting.sanitizeConfig(config)

        assertFalse(sanitized.contains("sk-config-secret-value"))
        assertFalse(sanitized.contains("debug-token-value"))
        assertFalse(sanitized.contains("db-password-value"))
        assertTrue(sanitized.contains("base_url"))
        assertFalse(DebugDumpSecrets.containsPotentialSecret(sanitized))
    }

    @Test
    fun scannerFlagsUnredactedSecretsButAllowsOrdinaryDebugText() {
        assertTrue(DebugDumpSecrets.containsPotentialSecret("OPENAI_API_KEY=sk-live-secret-value"))
        assertTrue(DebugDumpSecrets.containsPotentialSecret("Authorization: Bearer abcdefghijklmnopqrstuvwxyz"))
        assertFalse(DebugDumpSecrets.containsPotentialSecret("debugdump completed without credentials"))
        assertFalse(DebugDumpSecrets.containsPotentialSecret("api_key: \"<redacted>\""))
    }
}
