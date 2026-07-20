package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.Instant

class DebugDumpRetentionTest {
    @TempDir
    lateinit var tempDirectory: Path

    @Test
    fun deletesOnlyExpiredManagedExports() {
        val now = Instant.parse("2026-07-17T12:00:00Z")
        val expired = createExport("debug-dump-20260701-120000", 32, now.minus(Duration.ofDays(16)))
        val recent = createExport("debug-dump-20260716-120000", 48, now.minus(Duration.ofDays(1)))
        val questSaves = Files.createDirectories(tempDirectory.resolve("quest-saves"))
        val malformed = Files.createDirectories(tempDirectory.resolve("debug-dump-manual"))

        val result = DebugDumpRetention.cleanup(
            tempDirectory,
            DebugDumpRetentionPolicy(Duration.ofDays(7), 10, 1024L),
            now,
        )

        assertFalse(Files.exists(expired))
        assertTrue(Files.exists(recent))
        assertTrue(Files.exists(questSaves))
        assertTrue(Files.exists(malformed))
        assertEquals(listOf(DebugDumpCleanupReason.AGE), result.deletedExports.map { entry -> entry.reason })
        assertTrue(result.limitsSatisfied())
    }

    @Test
    fun enforcesCountThenQuotaOldestFirstAndProtectsCurrentExport() {
        val now = Instant.parse("2026-07-17T12:00:00Z")
        val oldest = createExport("debug-dump-20260714-120000", 100, now.minus(Duration.ofDays(3)))
        val middle = createExport("debug-dump-20260715-120000", 100, now.minus(Duration.ofDays(2)))
        val current = createExport("debug-dump-20260717-120000", 300, now)

        val result = DebugDumpRetention.cleanup(
            tempDirectory,
            DebugDumpRetentionPolicy(Duration.ofDays(30), 2, 350L),
            now,
            current,
        )

        assertFalse(Files.exists(oldest))
        assertFalse(Files.exists(middle))
        assertTrue(Files.exists(current))
        assertEquals(
            listOf(DebugDumpCleanupReason.COUNT, DebugDumpCleanupReason.QUOTA),
            result.deletedExports.map { entry -> entry.reason },
        )
        assertEquals(1, result.remainingExports)
        assertEquals(300L, result.remainingBytes)
        assertEquals(200L, result.freedBytes())
        assertTrue(result.limitsSatisfied())
    }

    @Test
    fun reportsUnsatisfiedQuotaInsteadOfDeletingProtectedCurrentExport() {
        val now = Instant.parse("2026-07-17T12:00:00Z")
        val current = createExport("debug-dump-20260717-120000", 200, now)

        val result = DebugDumpRetention.cleanup(
            tempDirectory,
            DebugDumpRetentionPolicy(Duration.ofDays(30), 1, 100L),
            now,
            current,
        )

        assertTrue(Files.exists(current))
        assertFalse(result.quotaSatisfied)
        assertFalse(result.limitsSatisfied())
        assertEquals(0, result.deletedCount())
    }

    @Test
    fun createsUniqueManagedDirectoryForSameSecond() {
        val first = DebugDumpRetention.createUniqueExportDirectory(tempDirectory, "20260717-120000")
        val second = DebugDumpRetention.createUniqueExportDirectory(tempDirectory, "20260717-120000")

        assertEquals("debug-dump-20260717-120000", first.fileName.toString())
        assertEquals("debug-dump-20260717-120000-01", second.fileName.toString())
    }

    @Test
    fun refusesCleanupWhenRootIsNotDirectory() {
        val rootFile = tempDirectory.resolve("debug-dumps")
        Files.writeString(rootFile, "not a directory")

        val result = DebugDumpRetention.cleanup(
            rootFile,
            DebugDumpRetentionPolicy(Duration.ofDays(7), 10, 1024L),
        )

        assertTrue(Files.exists(rootFile))
        assertEquals("UnsafeRoot", result.failures.single().errorType)
        assertFalse(result.limitsSatisfied())
    }

    private fun createExport(name: String, bytes: Int, modifiedAt: Instant): Path {
        val directory = Files.createDirectories(tempDirectory.resolve(name))
        Files.write(directory.resolve("payload.bin"), ByteArray(bytes) { 1 })
        Files.setLastModifiedTime(directory, FileTime.from(modifiedAt))
        return directory
    }
}
