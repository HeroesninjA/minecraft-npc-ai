package ro.ainpc.debug

object DebugDumpManifest {
    const val SCHEMA_VERSION = 1

    @JvmStatic
    fun build(
        scope: String,
        generatedAt: String,
        privacyMode: DebugDumpPrivacyMode,
        playerFilterPresent: Boolean,
        artifacts: List<DebugDumpWriteResult>,
        retentionPolicy: DebugDumpRetentionPolicy,
        cleanupBeforeExport: DebugDumpCleanupResult,
    ): Map<String, Any> = buildInternal(
        scope,
        generatedAt,
        privacyMode,
        playerFilterPresent,
        artifacts,
        retentionPolicy,
        cleanupBeforeExport,
        null,
    )

    internal fun buildCaptured(
        scope: String,
        capturedAt: String,
        completedAt: String,
        privacyMode: DebugDumpPrivacyMode,
        playerFilterPresent: Boolean,
        artifacts: List<DebugDumpWriteResult>,
        retentionPolicy: DebugDumpRetentionPolicy,
        cleanupBeforeExport: DebugDumpCleanupResult,
        databaseTransactionUsed: Boolean,
    ): Map<String, Any> = buildInternal(
        scope,
        capturedAt,
        privacyMode,
        playerFilterPresent,
        artifacts,
        retentionPolicy,
        cleanupBeforeExport,
        linkedMapOf(
            "model" to "main-thread-runtime-capture+async-database-and-io",
            "runtime_captured_at" to capturedAt,
            "artifact_set_completed_at" to completedAt,
            "runtime_capture_thread" to "server-main",
            "database_and_file_io_thread" to "paper-async-worker",
            "database_transaction_used" to databaseTransactionUsed,
            "artifact_set_frozen_before_write" to true,
        ),
    )

    private fun buildInternal(
        scope: String,
        generatedAt: String,
        privacyMode: DebugDumpPrivacyMode,
        playerFilterPresent: Boolean,
        artifacts: List<DebugDumpWriteResult>,
        retentionPolicy: DebugDumpRetentionPolicy,
        cleanupBeforeExport: DebugDumpCleanupResult,
        snapshot: Map<String, Any>?,
    ): Map<String, Any> {
        val confidentiality = linkedMapOf<String, Any>(
            "classification" to if (privacyMode == DebugDumpPrivacyMode.PRIVACY_SAFE) {
                "privacy-safe-review-required"
            } else {
                "restricted-operational"
            },
            "privacy_mode" to privacyMode.cliValue,
            "manual_review_required" to true,
            "player_filter_present" to playerFilterPresent,
            "player_filter_value_included" to false,
            "operational_identifiers_may_remain" to (privacyMode == DebugDumpPrivacyMode.STANDARD),
        )
        val retention = linkedMapOf<String, Any>(
            "max_age_days" to retentionPolicy.maxAge.toDays(),
            "max_exports" to retentionPolicy.maxExports,
            "max_total_bytes" to retentionPolicy.maxTotalBytes,
            "cleanup_before_deleted" to cleanupBeforeExport.deletedCount(),
            "cleanup_before_freed_bytes" to cleanupBeforeExport.freedBytes(),
            "cleanup_before_failures" to cleanupBeforeExport.failures.map { failure ->
                linkedMapOf(
                    "directory" to failure.directoryName,
                    "operation" to failure.operation,
                    "error_type" to failure.errorType,
                )
            },
            "cleanup_after_export_enforced" to true,
        )
        val artifactRows = artifacts.map { artifact ->
            linkedMapOf<String, Any>(
                "file" to artifact.path.fileName.toString(),
                "format" to artifact.format.name.lowercase(),
                "original_bytes" to artifact.originalBytes,
                "written_bytes" to artifact.writtenBytes,
                "truncated" to artifact.truncated,
            )
        }
        val manifest = linkedMapOf<String, Any>(
            "schema_version" to SCHEMA_VERSION,
            "document_type" to "ainpc-debug-dump-manifest",
            "generated_at" to generatedAt,
            "scope" to scope,
            "confidentiality" to confidentiality,
            "retention" to retention,
            "artifact_manifest_scope" to "payload-before-manifest-and-index",
            "artifact_count" to artifactRows.size,
            "artifacts" to artifactRows,
        )
        snapshot?.let { manifest["snapshot"] = it }
        return manifest
    }
}
