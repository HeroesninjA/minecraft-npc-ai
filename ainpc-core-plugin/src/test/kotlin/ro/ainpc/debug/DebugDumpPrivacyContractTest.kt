package ro.ainpc.debug

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ro.ainpc.context.SensitiveDataRedactionPolicy
import ro.ainpc.context.SensitiveDataRedactor
import java.nio.file.Files
import java.nio.file.Path

class DebugDumpPrivacyContractTest {
    @TempDir
    lateinit var tempDirectory: Path

    @Test
    fun parsesPrivacySafeWithOrWithoutPlayerFilter() {
        assertEquals(DebugDumpExportOptions(), DebugDumpExportOptions.parse(emptyList()))
        assertEquals(
            DebugDumpExportOptions(privacyMode = DebugDumpPrivacyMode.PRIVACY_SAFE),
            DebugDumpExportOptions.parse(listOf("privacy-safe")),
        )
        assertEquals(
            DebugDumpExportOptions("Alice", DebugDumpPrivacyMode.PRIVACY_SAFE),
            DebugDumpExportOptions.parse(listOf("Alice", "privacy-safe")),
        )
        assertEquals(
            DebugDumpExportOptions("Alice", DebugDumpPrivacyMode.PRIVACY_SAFE),
            DebugDumpExportOptions.parse(listOf("privacy-safe", "Alice")),
        )
        assertThrows(IllegalArgumentException::class.java) {
            DebugDumpExportOptions.parse(listOf("Alice", "Bob"))
        }
    }

    @Test
    fun standardModeRedactsSecretsWithoutRemovingDiagnosticIdentifiers() {
        val raw = "player=Alice uuid=550e8400-e29b-41d4-a716-446655440000 api_key=sk-standard-secret-value"
        val redacted = SensitiveDataRedactor.redact(raw, SensitiveDataRedactionPolicy.secretsOnly())

        assertTrue(redacted.contains("Alice"))
        assertTrue(redacted.contains("550e8400-e29b-41d4-a716-446655440000"))
        assertFalse(redacted.contains("sk-standard-secret-value"))
    }

    @Test
    fun privacySafePolicyProtectsEveryWrittenArtifactAndKeepsJsonValid() {
        val policy = SensitiveDataRedactionPolicy.privacySafe(setOf("Alice"))
        val gson = GsonBuilder().setPrettyPrinting().create()
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val email = "alice@example.com"
        val ipAddress = "192.168.10.42"
        val apiKey = "sk-artifact-secret-value"
        val bearer = "abcdefghijklmnopqrstuvwxyz123456"

        DebugDumpIO.writeText(
            tempDirectory.resolve("config-sanitized.yml"),
            "api_key: $apiKey\ndatabase.password: super-secret-password\n",
            policy,
        )
        DebugDumpIO.writeText(
            tempDirectory.resolve("recent-server-log.txt"),
            "Player: Alice\nUUID: $uuid\nEmail: $email\nIP: $ipAddress\nPath: C:\\Users\\Alice\\server\n",
            policy,
        )
        DebugDumpIO.writeText(
            tempDirectory.resolve("openai.txt"),
            "last_prompt_preview: Alice asked a private question\nlast_response_preview: private answer\n",
            policy,
        )
        DebugDumpIO.writeText(
            tempDirectory.resolve("mcp.txt"),
            "Authorization: Bearer $bearer\nplayer_name: Alice\n",
            policy,
        )
        val databaseSnapshot = JsonObject().apply {
            addProperty("player_uuid", uuid)
            addProperty("player_name", "Alice")
            addProperty("email", email)
            addProperty("source_ip", ipAddress)
            addProperty("prompt", "private database prompt")
        }
        val databasePath = tempDirectory.resolve("database.json")
        DebugDumpIO.writeJson(databasePath, databaseSnapshot, gson, policy)

        Files.list(tempDirectory).use { paths ->
            paths.forEach { path ->
                val artifact = Files.readString(path)
                listOf("Alice", uuid, email, ipAddress, apiKey, bearer, "super-secret-password", "private answer")
                    .forEach { sensitiveValue ->
                        assertFalse(artifact.contains(sensitiveValue), "${path.fileName} leaked $sensitiveValue")
                    }
                assertFalse(
                    SensitiveDataRedactor.containsPotentialSensitiveData(artifact, policy),
                    "${path.fileName} is not idempotently redacted",
                )
            }
        }
        assertTrue(JsonParser.parseString(Files.readString(databasePath)).isJsonObject)
        assertTrue(Files.readString(databasePath).contains("<omitted:privacy-safe>"))
    }
}
