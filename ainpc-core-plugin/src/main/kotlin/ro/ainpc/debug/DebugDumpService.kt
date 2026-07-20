package ro.ainpc.debug

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.bukkit.configuration.file.FileConfiguration
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.RuntimeMetricNames
import ro.ainpc.context.SensitiveDataRedactionPolicy
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DebugDumpService(private val plugin: AINPCPlugin) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    @Throws(IOException::class)
    fun createDump(
        scope: String?,
        playerName: String? = null,
        privacyMode: DebugDumpPrivacyMode = DebugDumpPrivacyMode.STANDARD,
    ): DebugDumpResult = writeCapturedDump(captureRuntimeSnapshot(scope, playerName, privacyMode))

    internal fun captureRuntimeSnapshot(
        scope: String?,
        playerName: String? = null,
        privacyMode: DebugDumpPrivacyMode = DebugDumpPrivacyMode.STANDARD,
    ): DebugDumpCapturedSnapshot {
        val normalizedScope = normalizeScope(scope)
        val capturedAt = LocalDateTime.now()
        val dataFolder = plugin.dataFolder.toPath()
        val artifacts = DebugDumpArtifactSet()
        val serverSnapshot = DebugDumpServerSnapshot.capture(plugin)

        artifacts.addText("config-sanitized.yml", sanitizeConfig(plugin.config))
        artifacts.addText("audit.txt", DebugDumpAudit.buildAuditText(plugin))
        artifacts.addJson("health.json", plugin.performanceMonitor.snapshot())
        artifacts.addJson("traces.json", plugin.performanceMonitor.traceSnapshot())

        if (normalizedScope == "all" || normalizedScope == "npc") {
            artifacts.addJson("npcs.json", DebugDumpNpcJson.buildNpcsJson(plugin))
        }

        var worldMapping: JsonObject? = null
        fun capturedWorldMapping(): JsonObject {
            if (worldMapping == null) {
                worldMapping = DebugDumpWorldJson.buildWorldMappingJson(plugin)
            }
            return requireNotNull(worldMapping)
        }

        var mappingSnapshot: JsonObject? = null
        if (normalizedScope == "all" || normalizedScope == "world") {
            val capturedMapping = capturedWorldMapping()
            val worldAdminSnapshot = DebugDumpWorldAdminJson.buildWorldAdminSnapshotJson(plugin)
            val overlaySources = worldAdminSnapshot.getAsJsonArray("overlay_sources")
                ?.mapNotNull { element -> runCatching { element.asString }.getOrNull() }
                .orEmpty()
            mappingSnapshot = DebugDumpMappingSnapshotJson.buildMappingSnapshotJson(
                capturedMapping,
                runCatching { plugin.platform.worldAdminService }.getOrNull(),
                overlaySources,
            )
            artifacts.addJson("world-mapping.json", capturedMapping)
            artifacts.addJson("mapping-snapshot.json", mappingSnapshot)
            artifacts.addJson("world-admin-snapshot.json", worldAdminSnapshot)
        }

        if (normalizedScope == "all" || normalizedScope == "quest") {
            val questConfig = runCatching { plugin.questConfig }.getOrNull()
            val questFileName = runCatching { plugin.questConfigFile.name }.getOrDefault("")
            val questYaml = questConfig?.saveToString().orEmpty()
            val normalizedDocument = if (questConfig == null) {
                JsonObject()
            } else {
                ScriptDocumentNormalizer.normalizeDocument(questYaml, questFileName.ifBlank { null })
            }
            artifacts.addText("quests.yml", questYaml)
            artifacts.addJson(
                "quests-snapshot.json",
                DebugDumpQuestConfigJson.buildQuestConfigSnapshotJson(
                    normalizedDocument.deepCopy(),
                    questFileName,
                    questConfig != null,
                ),
            )
            artifacts.addJson(
                "loaded-quest-definitions.json",
                DebugDumpQuestDefinitionJson.buildLoadedQuestDefinitionsJson(plugin, gson),
            )
            artifacts.addJson(
                "quest-mapping-contract.json",
                DebugDumpQuestMappingContractJson.buildQuestMappingContractJson(
                    normalizedDocument.deepCopy(),
                    questFileName,
                    capturedWorldMapping(),
                ),
            )
            artifacts.addJson(
                "quest-director-decision.json",
                DebugDumpQuestDirectorJson.buildQuestDirectorSnapshotJson(plugin),
            )
            artifacts.addJson("objective-types-contract.json", DebugDumpObjectiveTypesJson.buildObjectiveTypesJson())
            artifacts.addJson("quest-warnings-contract.json", DebugDumpWarningsJson.buildWarningsJson(plugin))
        }

        if (normalizedScope == "all" || normalizedScope == "openai") {
            artifacts.addText("openai.txt", buildOpenAiInfo())
        }
        if (normalizedScope == "all" || normalizedScope == "authoring") {
            artifacts.addText("authoring.txt", DebugDumpAuthoringText.buildAuthoringText(plugin, null))
        }
        if (normalizedScope == "all") {
            artifacts.addJson("narrative-plans.json", DebugDumpNarrativePlanJson.buildNarrativePlanJson(plugin))
        }

        val recentApiEvents = runCatching { plugin.recentEventsBuffer.buildRecentApiEventsText() }
            .getOrDefault("Bufferul in-memory pentru evenimente API nu este initializat.\n")
        artifacts.addText("recent-api-events.txt", recentApiEvents)

        val capturesStory = normalizedScope == "all" || normalizedScope == "quest" || normalizedScope == "story"
        return DebugDumpCapturedSnapshot(
            normalizedScope,
            playerName,
            privacyMode,
            capturedAt,
            dataFolder,
            dataFolder.resolve("debug-dumps"),
            buildRetentionPolicy(),
            buildRedactionPolicy(privacyMode, playerName),
            serverSnapshot,
            artifacts.values(),
            mappingSnapshot,
            if (capturesStory) DebugDumpStoryText.captureScenarioSnapshots(plugin) else emptyList(),
        )
    }

    @Throws(IOException::class)
    internal fun writeCapturedDump(capture: DebugDumpCapturedSnapshot): DebugDumpResult {
        val timer = plugin.performanceMonitor.timer(RuntimeMetricNames.DEBUG_DUMP_EXPORT)
        timer.begin()
        var writer: ArtifactWriter? = null
        try {
            val completed = completeSnapshot(capture)
            val cleanupBefore = DebugDumpRetention.cleanup(capture.dumpsRoot, capture.retentionPolicy)
            val dumpRoot = DebugDumpRetention.createUniqueExportDirectory(
                capture.dumpsRoot,
                DUMP_TIMESTAMP.format(capture.capturedAt),
            )
            writer = ArtifactWriter(gson, capture.redactionPolicy)
            writer.writeText(
                dumpRoot.resolve("summary.txt"),
                DebugDumpServerSnapshot.buildSummary(capture.scope, dumpRoot, capture.capturedAt, capture.server),
            )
            writer.writeText(dumpRoot.resolve("server.txt"), DebugDumpServerSnapshot.buildServerInfo(capture.server))
            completed.artifacts.forEach { artifact -> writer.write(dumpRoot, artifact) }
            writeManifestFile(writer, dumpRoot, capture, completed, cleanupBefore)
            writeIndexFile(writer, dumpRoot, capture, completed, cleanupBefore)

            val cleanupAfter = DebugDumpRetention.cleanup(
                capture.dumpsRoot,
                capture.retentionPolicy,
                protectedDirectory = dumpRoot,
            )
            val result = DebugDumpResult(dumpRoot, capture.scope, capture.privacyMode, cleanupBefore, cleanupAfter)
            timer.end(writer.results.size)
            return result
        } catch (error: Throwable) {
            timer.fail(writer?.results?.size ?: 0)
            throw error
        }
    }

    private fun completeSnapshot(capture: DebugDumpCapturedSnapshot): DebugDumpCompletedSnapshot {
        val artifacts = DebugDumpArtifactSet(capture.runtimeArtifacts)
        if (capture.scope == "all" || capture.scope == "npc") {
            artifacts.addJson(
                "behavior-profiles.json",
                DebugDumpBehaviorProfileJson.buildBehaviorProfilesJson(capture.dataFolder),
            )
        }

        var databaseTransactionUsed = false
        val databaseManager = runCatching { plugin.databaseManager }.getOrNull()
        if (databaseManager == null) {
            addDatabaseArtifacts(capture, artifacts)
        } else {
            databaseManager.executeTransaction {
                addDatabaseArtifacts(capture, artifacts)
            }
            databaseTransactionUsed = true
        }

        artifacts.addText("recent-server-log.txt", readRecentServerLog(capture.dataFolder))
        return DebugDumpCompletedSnapshot(
            artifacts.values(),
            LocalDateTime.now(),
            databaseTransactionUsed,
        )
    }

    private fun addDatabaseArtifacts(capture: DebugDumpCapturedSnapshot, artifacts: DebugDumpArtifactSet) {
        val needsStorySnapshot = capture.scope in setOf("all", "world", "quest", "story")
        val story = if (needsStorySnapshot) {
            StoryDatabaseSnapshot(
                DebugDumpStoryStateJson.buildStoryStatesJson(plugin),
                DebugDumpStoryEventJson.buildStoryEventsJson(plugin, gson),
                DebugDumpStoryProgressionGapJson.buildStoryProgressionGapJson(plugin),
            )
        } else {
            null
        }

        if (capture.scope == "all" || capture.scope == "world") {
            val npcBindings = DebugDumpNpcWorldBindingJson.buildNpcWorldBindingsJson(plugin)
            val mappingSnapshot = requireNotNull(capture.mappingSnapshot)
            val storySnapshot = requireNotNull(story)
            mappingSnapshot.add(
                "story_summary",
                DebugDumpMappingSnapshotJson.buildStorySummaryJson(
                    storySnapshot.states,
                    storySnapshot.events,
                    storySnapshot.progressionGaps,
                ),
            )
            artifacts.addText(
                "mapping.txt",
                DebugDumpMappingText.buildCapturedMappingText(mappingSnapshot, npcBindings),
            )
            artifacts.addJson("npc-world-bindings.json", npcBindings)
            artifacts.addJson("households.json", DebugDumpSpawnPersistenceJson.buildHouseholdsJson(plugin))
            artifacts.addJson("spawn-batches.json", DebugDumpSpawnPersistenceJson.buildSpawnBatchesJson(plugin))
        }

        if (capture.scope == "all" || capture.scope == "quest") {
            val playerProgressions = DebugDumpProgressionJson.buildPlayerProgressionsJson(plugin, capture.playerFilter)
            val playerQuestProgress = DebugDumpProgressionJson.buildPlayerQuestProgressJson(plugin, capture.playerFilter)
            val questAnchors = DebugDumpProgressionJson.buildQuestAnchorBindingsJson(plugin)
            val questAudit = DebugDumpQuestAudit.buildQuestAuditReportText(plugin)
            artifacts.addText(
                "quest.txt",
                DebugDumpQuestText.buildQuestText(playerProgressions, playerQuestProgress, questAnchors, questAudit),
            )
            artifacts.addText("quest-audit-report.txt", questAudit)
            artifacts.addJson("player-progressions.json", playerProgressions)
            artifacts.addJson("player-quest-progress.json", playerQuestProgress)
            artifacts.addJson(
                "player-progression.json",
                DebugDumpPlayerProgressionJson.buildPlayerProgressionJson(plugin, capture.playerFilter),
            )
            artifacts.addJson("quest-anchor-bindings.json", questAnchors)
            artifacts.addJson("story-progression-gaps.json", requireNotNull(story).progressionGaps)
        }

        if (capture.scope == "all" || capture.scope == "quest" || capture.scope == "story") {
            val storySnapshot = requireNotNull(story)
            artifacts.addJson("story-states.json", storySnapshot.states)
            artifacts.addJson("story-events.json", storySnapshot.events)
            artifacts.addText(
                "story.txt",
                DebugDumpStoryText.buildCapturedStoryText(
                    capture.storyScenarios,
                    storySnapshot.states,
                    storySnapshot.events,
                    storySnapshot.progressionGaps,
                ),
            )
        }

        artifacts.addText("recent-story-events.txt", DebugDumpStoryEventsText.buildStoryEventsText(plugin))
    }

    private fun normalizeScope(scope: String?): String = DebugDumpFormatting.normalizeScope(scope)

    private fun sanitizeConfig(config: FileConfiguration): String = DebugDumpFormatting.sanitizeConfig(config)

    private fun buildOpenAiInfo(): String = DebugDumpFormatting.buildOpenAiInfo(
        plugin.config,
        plugin.openAIService.captureDebugSnapshot(),
    )

    private fun readRecentServerLog(dataFolder: Path): String =
        DebugDumpSecrets.redactText(
            DebugDumpIO.readRecentServerLog(dataFolder, RECENT_LOG_LINES, RECENT_LOG_BYTES)
        )

    private fun buildRedactionPolicy(
        privacyMode: DebugDumpPrivacyMode,
        playerName: String?,
    ): SensitiveDataRedactionPolicy {
        if (privacyMode == DebugDumpPrivacyMode.STANDARD) {
            return SensitiveDataRedactionPolicy.secretsOnly()
        }
        val identifiers = buildSet {
            playerName?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
            runCatching { plugin.server.onlinePlayers.map { it.name } }
                .getOrDefault(emptyList())
                .forEach(::add)
        }
        return SensitiveDataRedactionPolicy.privacySafe(identifiers)
    }

    private fun buildRetentionPolicy(): DebugDumpRetentionPolicy {
        val retentionDays = plugin.config.getInt(
            "debug.dumps.retention_days",
            DEFAULT_RETENTION_DAYS,
        ).coerceIn(MIN_RETENTION_DAYS, MAX_RETENTION_DAYS)
        val maxExports = plugin.config.getInt(
            "debug.dumps.max_exports",
            DEFAULT_MAX_EXPORTS,
        ).coerceIn(MIN_MAX_EXPORTS, MAX_MAX_EXPORTS)
        val maxTotalMib = plugin.config.getLong(
            "debug.dumps.max_total_mib",
            DEFAULT_MAX_TOTAL_MIB,
        ).coerceIn(MIN_MAX_TOTAL_MIB, MAX_MAX_TOTAL_MIB)
        return DebugDumpRetentionPolicy(
            Duration.ofDays(retentionDays.toLong()),
            maxExports,
            maxTotalMib * MIB_BYTES,
        )
    }

    private fun writeManifestFile(
        writer: ArtifactWriter,
        dumpRoot: Path,
        capture: DebugDumpCapturedSnapshot,
        completed: DebugDumpCompletedSnapshot,
        cleanupBefore: DebugDumpCleanupResult,
    ) {
        val payloadArtifacts = writer.results.toList()
        writer.writeJson(
            dumpRoot.resolve("manifest.json"),
            DebugDumpManifest.buildCaptured(
                capture.scope,
                capture.capturedAt.toString(),
                completed.completedAt.toString(),
                capture.privacyMode,
                capture.playerFilter != null,
                payloadArtifacts,
                capture.retentionPolicy,
                cleanupBefore,
                completed.databaseTransactionUsed,
            ),
        )
    }

    private fun writeIndexFile(
        writer: ArtifactWriter,
        dumpRoot: Path,
        capture: DebugDumpCapturedSnapshot,
        completed: DebugDumpCompletedSnapshot,
        cleanupBefore: DebugDumpCleanupResult,
    ) {
        val index = StringBuilder()
        index.append("Debug Dump Index\n")
        index.append("Scope: ${capture.scope}\n")
        if (capture.playerFilter != null) index.append("Player filter: ${capture.playerFilter}\n")
        index.append("Privacy mode: ${capture.privacyMode.cliValue}\n")
        index.append("Confidentiality manifest: manifest.json (manual review required)\n")
        index.append("Directory: ${dumpRoot.fileName}\n")
        index.append("Generated: ${DUMP_TIMESTAMP.format(capture.capturedAt)}\n")
        index.append("Snapshot model: main-thread runtime capture + async database/log/file I/O\n")
        index.append("Runtime captured: ${capture.capturedAt}\n")
        index.append("Artifact set completed: ${completed.completedAt}\n")
        index.append("Database transaction used: ${completed.databaseTransactionUsed}\n")
        index.append("Artifact set frozen before write: true\n")
        index.append("Max artifact bytes: $MAX_ARTIFACT_BYTES\n")
        index.append("Recent log limits: $RECENT_LOG_LINES lines / $RECENT_LOG_BYTES bytes\n")
        index.append(
            "Retention limits: ${capture.retentionPolicy.maxAge.toDays()} days / " +
                "${capture.retentionPolicy.maxExports} exports / ${capture.retentionPolicy.maxTotalBytes} bytes\n"
        )
        index.append(
            "Cleanup before export: deleted=${cleanupBefore.deletedCount()} " +
                "freed_bytes=${cleanupBefore.freedBytes()} failures=${cleanupBefore.failures.size}\n"
        )
        index.append("Cleanup after export: enforced; current export is protected\n")
        val truncatedResults = writer.results.filter(DebugDumpWriteResult::truncated)
        index.append("Truncated artifacts: ${truncatedResults.size}\n")
        truncatedResults.forEach { result ->
            index.append("  ${result.path.fileName}: ${result.originalBytes} -> ${result.writtenBytes} bytes\n")
        }
        index.append("\nFiles:\n")
        try {
            Files.list(dumpRoot).use { paths ->
                paths.sorted().forEach { path ->
                    val fileSize = try { Files.size(path) } catch (_: IOException) { 0L }
                    val sizeLabel = when {
                        fileSize < 1024 -> "${fileSize}B"
                        fileSize < 1048576 -> "${fileSize / 1024}KB"
                        else -> "${fileSize / 1048576}MB"
                    }
                    index.append("  ${path.fileName} ($sizeLabel)\n")
                }
            }
        } catch (_: IOException) {
            index.append("  <error listing files>\n")
        }
        writer.writeText(dumpRoot.resolve("index.txt"), index.toString())
    }

    data class DebugDumpResult(
        private val directoryValue: Path,
        private val scopeValue: String,
        private val privacyModeValue: DebugDumpPrivacyMode,
        private val cleanupBeforeValue: DebugDumpCleanupResult,
        private val cleanupAfterValue: DebugDumpCleanupResult,
    ) {
        fun directory(): Path = directoryValue

        fun scope(): String = scopeValue

        fun privacyMode(): DebugDumpPrivacyMode = privacyModeValue

        fun cleanupDeletedCount(): Int = cleanupBeforeValue.deletedCount() + cleanupAfterValue.deletedCount()

        fun cleanupFreedBytes(): Long = cleanupBeforeValue.freedBytes() + cleanupAfterValue.freedBytes()

        fun cleanupFailureCount(): Int = cleanupBeforeValue.failures.size + cleanupAfterValue.failures.size

        fun retentionLimitsSatisfied(): Boolean =
            cleanupBeforeValue.limitsSatisfied() && cleanupAfterValue.limitsSatisfied()

        fun retainedExportCount(): Int = cleanupAfterValue.remainingExports

        fun retainedExportBytes(): Long = cleanupAfterValue.remainingBytes
    }

    private data class StoryDatabaseSnapshot(
        val states: JsonObject,
        val events: JsonObject,
        val progressionGaps: JsonObject,
    )

    private class ArtifactWriter(
        private val gson: Gson,
        private val redactionPolicy: SensitiveDataRedactionPolicy,
    ) {
        val results = mutableListOf<DebugDumpWriteResult>()

        fun write(root: Path, artifact: DebugDumpArtifactSnapshot) {
            val path = root.resolve(artifact.fileName)
            when (artifact.format) {
                DebugDumpArtifactFormat.TEXT -> writeText(path, artifact.value as String)
                DebugDumpArtifactFormat.JSON -> writeJson(path, artifact.value)
            }
        }

        fun writeJson(path: Path, value: Any) {
            results += DebugDumpIO.writeJson(path, value, gson, redactionPolicy, MAX_ARTIFACT_BYTES)
        }

        fun writeText(path: Path, content: String) {
            results += DebugDumpIO.writeText(path, content, redactionPolicy, MAX_ARTIFACT_BYTES)
        }
    }

    companion object {
        private val DUMP_TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        private const val RECENT_LOG_LINES = 250
        private const val RECENT_LOG_BYTES = 512 * 1024
        private const val MAX_ARTIFACT_BYTES = 4 * 1024 * 1024
        private const val MIB_BYTES = 1024L * 1024L
        private const val DEFAULT_RETENTION_DAYS = 14
        private const val MIN_RETENTION_DAYS = 1
        private const val MAX_RETENTION_DAYS = 3650
        private const val DEFAULT_MAX_EXPORTS = 20
        private const val MIN_MAX_EXPORTS = 1
        private const val MAX_MAX_EXPORTS = 1000
        private const val DEFAULT_MAX_TOTAL_MIB = 512L
        private const val MIN_MAX_TOTAL_MIB = 16L
        private const val MAX_MAX_TOTAL_MIB = 10240L
    }
}
