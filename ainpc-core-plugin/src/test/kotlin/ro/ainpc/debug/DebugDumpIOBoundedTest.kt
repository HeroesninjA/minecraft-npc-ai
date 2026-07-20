package ro.ainpc.debug

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class DebugDumpIOBoundedTest {
    @TempDir
    lateinit var tempDirectory: Path

    @Test
    fun textArtifactsCoverSmallMediumLargeByteFixtures() {
        ARTIFACT_SIZE_FIXTURES.forEach { fixture ->
            val output = tempDirectory.resolve("${fixture.name}.txt")
            val content = "x".repeat(fixture.payloadBytes)

            val result = DebugDumpIO.writeText(output, content, maxBytes = ARTIFACT_FIXTURE_LIMIT_BYTES)
            val written = Files.readString(output, StandardCharsets.UTF_8)

            assertEquals(fixture.payloadBytes, result.originalBytes, fixture.name)
            assertEquals(fixture.truncated, result.truncated, fixture.name)
            assertEquals(result.writtenBytes.toLong(), Files.size(output), fixture.name)
            assertTrue(result.writtenBytes <= ARTIFACT_FIXTURE_LIMIT_BYTES, fixture.name)
            if (fixture.truncated) {
                assertTrue(written.contains("[AINPC export truncated:"), fixture.name)
            } else {
                assertEquals(content, written, fixture.name)
                assertEquals(fixture.payloadBytes, result.writtenBytes, fixture.name)
            }
        }
    }

    @Test
    fun jsonArtifactsCoverSmallMediumLargeByteFixtures() {
        val gson = GsonBuilder().create()

        ARTIFACT_SIZE_FIXTURES.forEach { fixture ->
            val output = tempDirectory.resolve("${fixture.name}.json")
            val snapshot = jsonSnapshotWithSerializedBytes(gson, fixture.payloadBytes)

            val result = DebugDumpIO.writeJson(output, snapshot, gson, maxBytes = ARTIFACT_FIXTURE_LIMIT_BYTES)
            val parsed = JsonParser.parseString(Files.readString(output)).asJsonObject

            assertEquals(fixture.payloadBytes, result.originalBytes, fixture.name)
            assertEquals(fixture.truncated, result.truncated, fixture.name)
            assertEquals(result.writtenBytes.toLong(), Files.size(output), fixture.name)
            assertTrue(result.writtenBytes <= ARTIFACT_FIXTURE_LIMIT_BYTES, fixture.name)
            if (fixture.truncated) {
                assertTrue(parsed.get("truncated").asBoolean, fixture.name)
                assertEquals("artifact_byte_limit", parsed.get("reason").asString, fixture.name)
            } else {
                assertTrue(parsed.has("rows"), fixture.name)
                assertEquals(fixture.payloadBytes, result.writtenBytes, fixture.name)
            }
        }
    }

    @Test
    fun keepsOnlyRequestedLinesForSmallLog() {
        val logPath = tempDirectory.resolve("latest.log")
        Files.writeString(logPath, (1..6).joinToString("\n") { "line-$it" } + "\n")

        val tail = DebugDumpIO.readLogTail(logPath, maxLines = 3, maxBytes = 1024)

        assertFalse(tail.truncatedByBytes)
        assertTrue(tail.truncatedByLines)
        assertEquals(listOf("line-4", "line-5", "line-6"), tail.lines)
        assertTrue(tail.render().contains("max_lines=3"))
    }

    @Test
    fun resolvesPaperLogAndReadsOnlyBoundedTailWindow() {
        val serverRoot = tempDirectory.resolve("server")
        val dataFolder = serverRoot.resolve("plugins").resolve("AINPC")
        val logPath = serverRoot.resolve("logs").resolve("latest.log")
        Files.createDirectories(dataFolder)
        Files.createDirectories(logPath.parent)
        val content = (1..20_000).joinToString("\n") { index ->
            "line-${index.toString().padStart(5, '0')} ${"payload".repeat(8)}"
        } + "\n"
        Files.writeString(logPath, content)

        val tail = DebugDumpIO.readLogTail(logPath, maxLines = 10, maxBytes = 4096)
        val rendered = DebugDumpIO.readRecentServerLog(dataFolder, recentLogLines = 10, maxBytes = 4096)

        assertTrue(tail.sourceBytes > 4096)
        assertTrue(tail.bytesRead <= 4096)
        assertTrue(tail.truncatedByBytes)
        assertEquals(10, tail.lines.size)
        assertEquals("line-20000 ${"payload".repeat(8)}", tail.lines.last())
        assertFalse(rendered.contains("line-00001"))
        assertTrue(rendered.contains("line-20000"))
        assertTrue(rendered.startsWith("[AINPC latest.log tail truncated:"))
    }

    @Test
    fun boundedTailDoesNotEmitBrokenUtf8AtWindowBoundary() {
        val logPath = tempDirectory.resolve("utf8.log")
        val content = (1..100).joinToString("\n") { "linie-$it șță 😀" } + "\n"
        Files.writeString(logPath, content, StandardCharsets.UTF_8)

        val tail = DebugDumpIO.readLogTail(logPath, maxLines = 5, maxBytes = 257)
        val rendered = tail.render()

        assertTrue(tail.truncatedByBytes)
        assertEquals(5, tail.lines.size)
        assertFalse(rendered.contains("�"))
        assertTrue(rendered.contains("linie-100"))
    }

    @Test
    fun textArtifactIsUtf8SafeAndBounded() {
        val output = tempDirectory.resolve("large.txt")

        val result = DebugDumpIO.writeText(output, "șță😀".repeat(1000), maxBytes = 256)
        val written = Files.readString(output, StandardCharsets.UTF_8)

        assertTrue(result.truncated)
        assertTrue(result.originalBytes > 256)
        assertTrue(result.writtenBytes <= 256)
        assertEquals(result.writtenBytes.toLong(), Files.size(output))
        assertTrue(written.contains("[AINPC export truncated:"))
        assertFalse(written.contains("�"))
    }

    @Test
    fun oversizedJsonBecomesValidTruncationEnvelope() {
        val output = tempDirectory.resolve("large.json")
        val gson = GsonBuilder().setPrettyPrinting().create()
        val snapshot = JsonObject().apply { addProperty("rows", "x".repeat(10_000)) }

        val result = DebugDumpIO.writeJson(output, snapshot, gson, maxBytes = 256)
        val parsed = JsonParser.parseString(Files.readString(output)).asJsonObject

        assertTrue(result.truncated)
        assertTrue(result.originalBytes > 256)
        assertTrue(result.writtenBytes <= 256)
        assertTrue(parsed.get("truncated").asBoolean)
        assertEquals("artifact_byte_limit", parsed.get("reason").asString)
        assertEquals(256, parsed.get("max_bytes").asInt)
    }

    private fun jsonSnapshotWithSerializedBytes(gson: Gson, targetBytes: Int): JsonObject {
        val emptySnapshot = JsonObject().apply { addProperty("rows", "") }
        val envelopeBytes = gson.toJson(emptySnapshot).toByteArray(StandardCharsets.UTF_8).size
        require(targetBytes >= envelopeBytes)
        return JsonObject().apply { addProperty("rows", "x".repeat(targetBytes - envelopeBytes)) }
    }

    private data class ArtifactSizeFixture(
        val name: String,
        val payloadBytes: Int,
        val truncated: Boolean,
    )

    private companion object {
        const val ARTIFACT_FIXTURE_LIMIT_BYTES = 256

        val ARTIFACT_SIZE_FIXTURES = listOf(
            ArtifactSizeFixture("small", 128, truncated = false),
            ArtifactSizeFixture("medium", ARTIFACT_FIXTURE_LIMIT_BYTES, truncated = false),
            ArtifactSizeFixture("large", 512, truncated = true),
        )
    }
}
