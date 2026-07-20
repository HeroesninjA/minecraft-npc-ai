package ro.ainpc.debug

import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.Comparator

data class DebugDumpRetentionPolicy(
    val maxAge: Duration,
    val maxExports: Int,
    val maxTotalBytes: Long,
) {
    init {
        require(!maxAge.isNegative && !maxAge.isZero) { "maxAge trebuie sa fie pozitiv." }
        require(maxExports > 0) { "maxExports trebuie sa fie pozitiv." }
        require(maxTotalBytes > 0L) { "maxTotalBytes trebuie sa fie pozitiv." }
    }
}

enum class DebugDumpCleanupReason {
    AGE,
    COUNT,
    QUOTA
}

data class DebugDumpDeletedExport(
    val directoryName: String,
    val bytes: Long,
    val reason: DebugDumpCleanupReason,
)

data class DebugDumpCleanupFailure(
    val directoryName: String,
    val operation: String,
    val errorType: String,
)

data class DebugDumpCleanupResult(
    val deletedExports: List<DebugDumpDeletedExport>,
    val failures: List<DebugDumpCleanupFailure>,
    val remainingExports: Int,
    val remainingBytes: Long,
    val ageLimitSatisfied: Boolean,
    val countLimitSatisfied: Boolean,
    val quotaSatisfied: Boolean,
) {
    fun deletedCount(): Int = deletedExports.size

    fun freedBytes(): Long = deletedExports.fold(0L) { total, entry -> saturatingAdd(total, entry.bytes) }

    fun limitsSatisfied(): Boolean =
        failures.isEmpty() && ageLimitSatisfied && countLimitSatisfied && quotaSatisfied
}

object DebugDumpRetention {
    private val managedDirectoryPattern = Regex("^debug-dump-\\d{8}-\\d{6}(?:-\\d+)?$")
    private val timestampPattern = Regex("^\\d{8}-\\d{6}$")

    @JvmStatic
    @Throws(IOException::class)
    fun createUniqueExportDirectory(root: Path, timestampLabel: String): Path {
        require(timestampPattern.matches(timestampLabel)) { "Timestamp debug dump invalid: $timestampLabel" }
        Files.createDirectories(root)
        for (suffix in 0..9999) {
            val suffixText = if (suffix == 0) "" else "-%02d".format(suffix)
            val candidate = root.resolve("debug-dump-$timestampLabel$suffixText")
            try {
                return Files.createDirectory(candidate)
            } catch (_: FileAlreadyExistsException) {
            }
        }
        throw IOException("Nu am putut aloca un director unic pentru debug dump.")
    }

    @JvmStatic
    fun cleanup(
        root: Path,
        policy: DebugDumpRetentionPolicy,
        now: Instant = Instant.now(),
        protectedDirectory: Path? = null,
    ): DebugDumpCleanupResult {
        val normalizedRoot = root.toAbsolutePath().normalize()
        if (!Files.exists(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
            return emptyResult()
        }
        if (Files.isSymbolicLink(normalizedRoot) || !Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
            return emptyResult(
                DebugDumpCleanupFailure(normalizedRoot.fileName?.toString() ?: "<root>", "scan_root", "UnsafeRoot")
            )
        }

        val failures = mutableListOf<DebugDumpCleanupFailure>()
        val deleted = mutableListOf<DebugDumpDeletedExport>()
        val protectedPath = protectedDirectory?.toAbsolutePath()?.normalize()
            ?.takeIf { candidate -> candidate.parent == normalizedRoot }
        val cutoff = now.minus(policy.maxAge)
        val entries = scanManagedExports(normalizedRoot, failures).toMutableList()

        deleteMatching(
            normalizedRoot,
            entries,
            protectedPath,
            DebugDumpCleanupReason.AGE,
            deleted,
            failures,
        ) { entry -> entry.modifiedAt.isBefore(cutoff) }

        while (entries.size > policy.maxExports) {
            val candidate = oldestDeletable(entries, protectedPath) ?: break
            if (deleteExport(normalizedRoot, candidate, DebugDumpCleanupReason.COUNT, deleted, failures)) {
                entries.remove(candidate)
            } else {
                candidate.deletionAttempted = true
            }
        }

        while (totalBytes(entries) > policy.maxTotalBytes) {
            val candidate = oldestDeletable(entries, protectedPath) ?: break
            if (deleteExport(normalizedRoot, candidate, DebugDumpCleanupReason.QUOTA, deleted, failures)) {
                entries.remove(candidate)
            } else {
                candidate.deletionAttempted = true
            }
        }

        val remaining = scanManagedExports(normalizedRoot, failures)
        val remainingBytes = totalBytes(remaining)
        return DebugDumpCleanupResult(
            deleted.toList(),
            failures.distinct().toList(),
            remaining.size,
            remainingBytes,
            remaining.none { entry -> entry.modifiedAt.isBefore(cutoff) && entry.path != protectedPath },
            remaining.size <= policy.maxExports,
            remainingBytes <= policy.maxTotalBytes,
        )
    }

    private fun deleteMatching(
        root: Path,
        entries: MutableList<ManagedExport>,
        protectedPath: Path?,
        reason: DebugDumpCleanupReason,
        deleted: MutableList<DebugDumpDeletedExport>,
        failures: MutableList<DebugDumpCleanupFailure>,
        predicate: (ManagedExport) -> Boolean,
    ) {
        entries.sortedWith(managedExportComparator).forEach { entry ->
            if (entry.path != protectedPath && predicate(entry)) {
                if (deleteExport(root, entry, reason, deleted, failures)) {
                    entries.remove(entry)
                } else {
                    entry.deletionAttempted = true
                }
            }
        }
    }

    private fun oldestDeletable(entries: List<ManagedExport>, protectedPath: Path?): ManagedExport? =
        entries.asSequence()
            .filter { entry -> entry.path != protectedPath && !entry.deletionAttempted }
            .minWithOrNull(managedExportComparator)

    private fun deleteExport(
        root: Path,
        entry: ManagedExport,
        reason: DebugDumpCleanupReason,
        deleted: MutableList<DebugDumpDeletedExport>,
        failures: MutableList<DebugDumpCleanupFailure>,
    ): Boolean {
        if (entry.path.parent != root || Files.isSymbolicLink(entry.path) || !managedDirectoryPattern.matches(entry.name)) {
            failures += DebugDumpCleanupFailure(entry.name, "delete", "UnsafeManagedPath")
            return false
        }
        return try {
            Files.walk(entry.path).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { path -> Files.deleteIfExists(path) }
            }
            deleted += DebugDumpDeletedExport(entry.name, entry.bytes, reason)
            true
        } catch (exception: Exception) {
            failures += DebugDumpCleanupFailure(entry.name, "delete", exception.javaClass.simpleName)
            false
        }
    }

    private fun scanManagedExports(
        root: Path,
        failures: MutableList<DebugDumpCleanupFailure>,
    ): List<ManagedExport> {
        val exports = mutableListOf<ManagedExport>()
        try {
            Files.list(root).use { paths ->
                paths.forEach { path ->
                    val normalized = path.toAbsolutePath().normalize()
                    val name = normalized.fileName?.toString().orEmpty()
                    if (normalized.parent == root &&
                        managedDirectoryPattern.matches(name) &&
                        !Files.isSymbolicLink(normalized) &&
                        Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)
                    ) {
                        val modifiedAt = try {
                            Files.getLastModifiedTime(normalized, LinkOption.NOFOLLOW_LINKS).toInstant()
                        } catch (exception: Exception) {
                            failures += DebugDumpCleanupFailure(name, "read_modified_time", exception.javaClass.simpleName)
                            Instant.EPOCH
                        }
                        exports += ManagedExport(normalized, name, modifiedAt, directorySize(normalized, failures))
                    }
                }
            }
        } catch (exception: Exception) {
            failures += DebugDumpCleanupFailure(root.fileName?.toString() ?: "<root>", "scan", exception.javaClass.simpleName)
        }
        return exports.sortedWith(managedExportComparator)
    }

    private fun directorySize(path: Path, failures: MutableList<DebugDumpCleanupFailure>): Long {
        var total = 0L
        return try {
            Files.walk(path).use { paths ->
                paths.filter { child -> Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS) }
                    .forEach { child -> total = saturatingAdd(total, Files.size(child)) }
            }
            total
        } catch (exception: Exception) {
            failures += DebugDumpCleanupFailure(path.fileName.toString(), "measure", exception.javaClass.simpleName)
            total
        }
    }

    private fun totalBytes(entries: Collection<ManagedExport>): Long =
        entries.fold(0L) { total, entry -> saturatingAdd(total, entry.bytes) }

    private fun emptyResult(failure: DebugDumpCleanupFailure? = null): DebugDumpCleanupResult =
        DebugDumpCleanupResult(
            emptyList(),
            failure?.let(::listOf).orEmpty(),
            0,
            0L,
            failure == null,
            failure == null,
            failure == null,
        )

    private data class ManagedExport(
        val path: Path,
        val name: String,
        val modifiedAt: Instant,
        val bytes: Long,
        var deletionAttempted: Boolean = false,
    )

    private val managedExportComparator = compareBy<ManagedExport>({ entry -> entry.modifiedAt }, { entry -> entry.name })
}

private fun saturatingAdd(left: Long, right: Long): Long =
    if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right
