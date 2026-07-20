package ro.ainpc.debug

import com.google.gson.Gson
import ro.ainpc.context.SensitiveDataRedactionPolicy
import ro.ainpc.context.SensitiveDataRedactor
import java.io.IOException
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.ArrayDeque

enum class DebugDumpArtifactFormat {
    TEXT,
    JSON,
}

data class DebugDumpWriteResult(
    val path: Path,
    val format: DebugDumpArtifactFormat,
    val originalBytes: Int,
    val writtenBytes: Int,
    val truncated: Boolean,
)

data class DebugDumpLogTail(
    val lines: List<String>,
    val sourceBytes: Long,
    val bytesRead: Int,
    val maxLines: Int,
    val maxBytes: Int,
    val truncatedByBytes: Boolean,
    val truncatedByLines: Boolean,
    val partialFirstLine: Boolean,
) {
    val truncated: Boolean
        get() = truncatedByBytes || truncatedByLines || partialFirstLine

    fun render(): String = buildString {
        if (truncated) {
            append("[AINPC latest.log tail truncated: source_bytes=")
                .append(sourceBytes)
                .append(" read_bytes=")
                .append(bytesRead)
                .append(" max_bytes=")
                .append(maxBytes)
                .append(" max_lines=")
                .append(maxLines)
                .append("]")
                .append(System.lineSeparator())
        }
        lines.forEach { line -> append(line).append(System.lineSeparator()) }
    }
}

object DebugDumpIO {
    @JvmStatic
    @Throws(IOException::class)
    fun writeJson(
        path: Path,
        value: Any,
        gson: Gson,
        redactionPolicy: SensitiveDataRedactionPolicy = SensitiveDataRedactionPolicy.secretsOnly(),
        maxBytes: Int = Int.MAX_VALUE,
    ): DebugDumpWriteResult {
        requireArtifactLimit(maxBytes)
        val content = SensitiveDataRedactor.redact(gson.toJson(value), redactionPolicy)
        val originalPayload = content.toByteArray(StandardCharsets.UTF_8)
        val payload = if (originalPayload.size <= maxBytes) {
            originalPayload
        } else {
            limitedJsonEnvelope(gson, originalPayload.size, maxBytes)
        }
        Files.write(path, payload)
        return DebugDumpWriteResult(
            path = path,
            format = DebugDumpArtifactFormat.JSON,
            originalBytes = originalPayload.size,
            writtenBytes = payload.size,
            truncated = originalPayload.size > maxBytes,
        )
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeText(
        path: Path,
        content: String,
        redactionPolicy: SensitiveDataRedactionPolicy = SensitiveDataRedactionPolicy.secretsOnly(),
        maxBytes: Int = Int.MAX_VALUE,
    ): DebugDumpWriteResult {
        requireArtifactLimit(maxBytes)
        val redactedContent = SensitiveDataRedactor.redact(content, redactionPolicy)
        val originalPayload = redactedContent.toByteArray(StandardCharsets.UTF_8)
        val payload = if (originalPayload.size <= maxBytes) {
            originalPayload
        } else {
            limitedTextPayload(originalPayload, maxBytes)
        }
        Files.write(path, payload)
        return DebugDumpWriteResult(
            path = path,
            format = DebugDumpArtifactFormat.TEXT,
            originalBytes = originalPayload.size,
            writtenBytes = payload.size,
            truncated = originalPayload.size > maxBytes,
        )
    }

    @JvmStatic
    fun readRecentServerLog(
        dataFolderPath: Path,
        recentLogLines: Int,
        maxBytes: Int = DEFAULT_LOG_TAIL_BYTES,
    ): String {
        var serverRoot: Path? = dataFolderPath.parent
        if (serverRoot != null) {
            serverRoot = serverRoot.parent
        }
        if (serverRoot == null) {
            return "Server root indisponibil.\n"
        }

        val latestLog = serverRoot.resolve("logs").resolve("latest.log")
        if (!Files.exists(latestLog)) {
            return "latest.log indisponibil la ${latestLog.toAbsolutePath()}\n"
        }

        return try {
            readLogTail(latestLog, recentLogLines, maxBytes).render()
        } catch (exception: IOException) {
            "Nu pot citi latest.log: ${exception.message}\n"
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readLogTail(logPath: Path, maxLines: Int, maxBytes: Int): DebugDumpLogTail {
        val boundedLines = maxLines.coerceIn(1, MAX_LOG_TAIL_LINES)
        val boundedBytes = maxBytes.coerceIn(1, MAX_LOG_TAIL_BYTES)
        Files.newByteChannel(logPath, StandardOpenOption.READ).use { channel ->
            val sourceBytes = channel.size()
            val requestedBytes = minOf(sourceBytes, boundedBytes.toLong()).toInt()
            if (requestedBytes == 0) {
                return DebugDumpLogTail(
                    lines = emptyList(),
                    sourceBytes = sourceBytes,
                    bytesRead = 0,
                    maxLines = boundedLines,
                    maxBytes = boundedBytes,
                    truncatedByBytes = false,
                    truncatedByLines = false,
                    partialFirstLine = false,
                )
            }

            channel.position(sourceBytes - requestedBytes)
            val byteBuffer = ByteBuffer.allocate(requestedBytes)
            while (byteBuffer.hasRemaining()) {
                val read = channel.read(byteBuffer)
                if (read < 0) {
                    break
                }
            }
            val bytesRead = byteBuffer.position()
            val rawBytes = byteBuffer.array().copyOf(bytesRead)
            val truncatedByBytes = sourceBytes > bytesRead
            val alignedOffset = if (truncatedByBytes) utf8AlignedStart(rawBytes) else 0
            var decoded = String(rawBytes, alignedOffset, rawBytes.size - alignedOffset, StandardCharsets.UTF_8)
            var partialFirstLine = truncatedByBytes && decoded.isNotEmpty()
            if (partialFirstLine) {
                val boundary = firstLineBoundary(decoded)
                if (boundary >= 0) {
                    decoded = decoded.substring(boundary)
                }
            }

            val lines = ArrayDeque<String>(boundedLines)
            var observedLines = 0
            StringReader(decoded).buffered().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    observedLines++
                    if (lines.size >= boundedLines) {
                        lines.removeFirst()
                    }
                    lines.addLast(line)
                    line = reader.readLine()
                }
            }
            if (decoded.isEmpty()) {
                partialFirstLine = false
            }
            return DebugDumpLogTail(
                lines = lines.toList(),
                sourceBytes = sourceBytes,
                bytesRead = bytesRead,
                maxLines = boundedLines,
                maxBytes = boundedBytes,
                truncatedByBytes = truncatedByBytes,
                truncatedByLines = observedLines > boundedLines,
                partialFirstLine = partialFirstLine,
            )
        }
    }

    private fun limitedJsonEnvelope(gson: Gson, originalBytes: Int, maxBytes: Int): ByteArray {
        val fullEnvelope = gson.toJson(
            linkedMapOf(
                "truncated" to true,
                "reason" to "artifact_byte_limit",
                "original_bytes" to originalBytes,
                "max_bytes" to maxBytes,
            )
        ).toByteArray(StandardCharsets.UTF_8)
        if (fullEnvelope.size <= maxBytes) {
            return fullEnvelope
        }
        val compactEnvelope = "{\"truncated\":true}".toByteArray(StandardCharsets.UTF_8)
        return if (compactEnvelope.size <= maxBytes) compactEnvelope else "{}".toByteArray(StandardCharsets.UTF_8)
    }

    private fun limitedTextPayload(originalPayload: ByteArray, maxBytes: Int): ByteArray {
        val marker = (
            System.lineSeparator() +
                "[AINPC export truncated: original_bytes=${originalPayload.size} max_bytes=$maxBytes]" +
                System.lineSeparator()
            ).toByteArray(StandardCharsets.UTF_8)
        val boundedMarker = utf8Prefix(marker, maxBytes)
        val contentBudget = maxBytes - boundedMarker.size
        val prefix = utf8Prefix(originalPayload, contentBudget)
        return prefix + boundedMarker
    }

    private fun utf8Prefix(bytes: ByteArray, maxBytes: Int): ByteArray {
        if (bytes.size <= maxBytes) {
            return bytes
        }
        var end = maxBytes
        while (end > 0 && end < bytes.size && bytes[end].toInt() and UTF8_CONTINUATION_MASK == UTF8_CONTINUATION_PREFIX) {
            end--
        }
        return bytes.copyOf(end)
    }

    private fun utf8AlignedStart(bytes: ByteArray): Int {
        var offset = 0
        while (offset < bytes.size && bytes[offset].toInt() and UTF8_CONTINUATION_MASK == UTF8_CONTINUATION_PREFIX) {
            offset++
        }
        return offset
    }

    private fun firstLineBoundary(text: String): Int {
        val index = text.indexOfFirst { character -> character == '\n' || character == '\r' }
        if (index < 0) {
            return -1
        }
        return if (text[index] == '\r' && text.getOrNull(index + 1) == '\n') index + 2 else index + 1
    }

    private fun requireArtifactLimit(maxBytes: Int) {
        require(maxBytes >= MIN_ARTIFACT_BYTES) { "maxBytes trebuie sa fie cel putin $MIN_ARTIFACT_BYTES." }
    }

    private const val MIN_ARTIFACT_BYTES = 64
    private const val MAX_LOG_TAIL_LINES = 1000
    private const val MAX_LOG_TAIL_BYTES = 8 * 1024 * 1024
    private const val UTF8_CONTINUATION_MASK = 0xC0
    private const val UTF8_CONTINUATION_PREFIX = 0x80
    const val DEFAULT_LOG_TAIL_BYTES = 512 * 1024
}
