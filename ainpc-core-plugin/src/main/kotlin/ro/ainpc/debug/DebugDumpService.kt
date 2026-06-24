package ro.ainpc.debug

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bukkit.configuration.file.FileConfiguration
import ro.ainpc.AINPCPlugin
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DebugDumpService(private val plugin: AINPCPlugin) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    @Throws(IOException::class)
    fun createDump(scope: String?): DebugDumpResult {
        val normalizedScope = normalizeScope(scope)
        val dumpRoot = plugin.dataFolder.toPath()
            .resolve("debug-dumps")
            .resolve("debug-dump-" + DUMP_TIMESTAMP.format(LocalDateTime.now()))
        Files.createDirectories(dumpRoot)

        writeText(
            dumpRoot.resolve("summary.txt"),
            DebugDumpServerSnapshot.buildSummary(normalizedScope, dumpRoot, plugin)
        )
        writeText(dumpRoot.resolve("server.txt"), DebugDumpServerSnapshot.buildServerInfo(plugin))
        writeText(dumpRoot.resolve("config-sanitized.yml"), sanitizeConfig(plugin.config))
        writeText(dumpRoot.resolve("audit.txt"), DebugDumpAudit.buildAuditText(plugin))

        if (normalizedScope == "all" || normalizedScope == "npc") {
            writeJson(dumpRoot.resolve("npcs.json"), DebugDumpNpcJson.buildNpcsJson(plugin))
            writeJson(dumpRoot.resolve("behavior-profiles.json"), DebugDumpBehaviorProfileJson.buildBehaviorProfilesJson(plugin))
        }
        if (normalizedScope == "all" || normalizedScope == "world") {
            writeText(dumpRoot.resolve("mapping.txt"), DebugDumpMappingText.buildMappingText(plugin))
            writeJson(dumpRoot.resolve("world-mapping.json"), DebugDumpWorldJson.buildWorldMappingJson(plugin))
            writeJson(
                dumpRoot.resolve("mapping-snapshot.json"),
                DebugDumpMappingSnapshotJson.buildMappingSnapshotJson(plugin),
            )
            writeJson(
                dumpRoot.resolve("world-admin-snapshot.json"),
                DebugDumpWorldAdminJson.buildWorldAdminSnapshotJson(plugin),
            )
            writeJson(
                dumpRoot.resolve("npc-world-bindings.json"),
                DebugDumpNpcWorldBindingJson.buildNpcWorldBindingsJson(plugin),
            )
            writeJson(dumpRoot.resolve("households.json"), DebugDumpSpawnPersistenceJson.buildHouseholdsJson(plugin))
            writeJson(
                dumpRoot.resolve("spawn-batches.json"),
                DebugDumpSpawnPersistenceJson.buildSpawnBatchesJson(plugin)
            )
        }
        if (normalizedScope == "all" || normalizedScope == "quest") {
            writeText(dumpRoot.resolve("quest.txt"), DebugDumpQuestText.buildQuestText(plugin))
            writeText(
                dumpRoot.resolve("quests.yml"),
                plugin.questConfig.saveToString(),
            )
            writeJson(
                dumpRoot.resolve("quests-snapshot.json"),
                DebugDumpQuestConfigJson.buildQuestConfigSnapshotJson(plugin),
            )
            writeText(dumpRoot.resolve("quest-audit-report.txt"), DebugDumpQuestAudit.buildQuestAuditReportText(plugin))
            writeJson(
                dumpRoot.resolve("loaded-quest-definitions.json"),
                DebugDumpQuestDefinitionJson.buildLoadedQuestDefinitionsJson(plugin, gson),
            )
            writeJson(
                dumpRoot.resolve("quest-mapping-contract.json"),
                DebugDumpQuestMappingContractJson.buildQuestMappingContractJson(plugin, gson),
            )
            writeJson(
                dumpRoot.resolve("player-progressions.json"),
                DebugDumpProgressionJson.buildPlayerProgressionsJson(plugin),
            )
            writeJson(
                dumpRoot.resolve("player-quest-progress.json"),
                DebugDumpProgressionJson.buildPlayerQuestProgressJson(plugin),
            )
            writeJson(
                dumpRoot.resolve("quest-anchor-bindings.json"),
                DebugDumpProgressionJson.buildQuestAnchorBindingsJson(plugin),
            )
            writeJson(
                dumpRoot.resolve("story-progression-gaps.json"),
                DebugDumpStoryProgressionGapJson.buildStoryProgressionGapJson(plugin),
            )
        }
        if (normalizedScope == "all" || normalizedScope == "quest" || normalizedScope == "story") {
            writeJson(dumpRoot.resolve("story-states.json"), DebugDumpStoryStateJson.buildStoryStatesJson(plugin))
            writeJson(
                dumpRoot.resolve("story-events.json"),
                DebugDumpStoryEventJson.buildStoryEventsJson(plugin, gson),
            )
            writeText(dumpRoot.resolve("story.txt"), DebugDumpStoryText.buildStoryText(plugin))
        }
        if (normalizedScope == "all" || normalizedScope == "openai") {
            writeText(dumpRoot.resolve("openai.txt"), buildOpenAiInfo())
        }
        if (normalizedScope == "all" || normalizedScope == "authoring") {
            writeText(dumpRoot.resolve("authoring.txt"), DebugDumpAuthoringText.buildAuthoringText(plugin, null))
        }
        if (normalizedScope == "all" || normalizedScope == "quest") {
            writeJson(
                dumpRoot.resolve("quest-director-decision.json"),
                DebugDumpQuestDirectorJson.buildQuestDirectorSnapshotJson(plugin),
            )
            writeJson(
                dumpRoot.resolve("objective-types-contract.json"),
                DebugDumpObjectiveTypesJson.buildObjectiveTypesJson(),
            )
        }
        if (normalizedScope == "all") {
            writeJson(
                dumpRoot.resolve("narrative-plans.json"),
                DebugDumpNarrativePlanJson.buildNarrativePlanJson(plugin),
            )
        }

        writeText(dumpRoot.resolve("recent-server-log.txt"), readRecentServerLog())
        val bufferText = runCatching {
            plugin.recentEventsBuffer.buildRecentEventsText()
        }.getOrDefault("In-memory buffer neinitializat.\n")
        val dbText = DebugDumpRecentEventsText.buildRecentEventsText(plugin)
        writeText(
            dumpRoot.resolve("recent-public-events.txt"),
            bufferText + "\n" + dbText,
        )
        writeIndexFile(dumpRoot, normalizedScope)
        return DebugDumpResult(dumpRoot, normalizedScope)
    }

    private fun normalizeScope(scope: String?): String = DebugDumpFormatting.normalizeScope(scope)

    private fun sanitizeConfig(config: FileConfiguration): String = DebugDumpFormatting.sanitizeConfig(config)

    private fun buildOpenAiInfo(): String = DebugDumpFormatting.buildOpenAiInfo(
        plugin.config,
        plugin.openAIService.captureDebugSnapshot(),
    )

    private fun readRecentServerLog(): String =
        DebugDumpSecrets.redactText(DebugDumpIO.readRecentServerLog(plugin.dataFolder.toPath(), RECENT_LOG_LINES))

    @Throws(IOException::class)
    private fun writeJson(path: Path, value: Any) {
        DebugDumpIO.writeJson(path, value, gson)
    }

    @Throws(IOException::class)
    private fun writeText(path: Path, content: String) {
        DebugDumpIO.writeText(path, content)
    }

    private fun writeIndexFile(dumpRoot: Path, scope: String) {
        val index = StringBuilder()
        index.append("Debug Dump Index\n")
        index.append("Scope: $scope\n")
        index.append("Directory: ${dumpRoot.fileName}\n")
        index.append("Generated: ${DUMP_TIMESTAMP.format(LocalDateTime.now())}\n")
        index.append("\nFiles:\n")
        try {
            Files.list(dumpRoot).sorted().forEach { path ->
                val fileSize = try { Files.size(path) } catch (_: java.io.IOException) { 0L }
                val sizeLabel = when {
                    fileSize < 1024 -> "${fileSize}B"
                    fileSize < 1048576 -> "${fileSize / 1024}KB"
                    else -> "${fileSize / 1048576}MB"
                }
                index.append("  ${path.fileName} ($sizeLabel)\n")
            }
        } catch (_: IOException) {
            index.append("  <error listing files>\n")
        }
        writeText(dumpRoot.resolve("index.txt"), index.toString())
    }

    data class DebugDumpResult(
        private val directoryValue: Path,
        private val scopeValue: String,
    ) {
        fun directory(): Path = directoryValue

        fun scope(): String = scopeValue
    }

    companion object {
        private val DUMP_TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        private const val RECENT_LOG_LINES = 250
    }
}
