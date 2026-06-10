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
        }
        if (normalizedScope == "all" || normalizedScope == "world") {
            writeJson(dumpRoot.resolve("world-mapping.json"), DebugDumpWorldJson.buildWorldMappingJson(plugin))
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
            writeText(
                dumpRoot.resolve("quests.yml"),
                plugin.questConfig.saveToString(),
            )
            writeText(dumpRoot.resolve("quest-audit-report.txt"), DebugDumpQuestAudit.buildQuestAuditReportText(plugin))
            writeJson(
                dumpRoot.resolve("loaded-quest-definitions.json"),
                DebugDumpQuestDefinitionJson.buildLoadedQuestDefinitionsJson(plugin, gson),
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
        }
        if (normalizedScope == "all" || normalizedScope == "quest" || normalizedScope == "story") {
            writeJson(dumpRoot.resolve("story-states.json"), DebugDumpStoryStateJson.buildStoryStatesJson(plugin))
            writeJson(
                dumpRoot.resolve("story-events.json"),
                DebugDumpStoryEventJson.buildStoryEventsJson(plugin, gson),
            )
        }
        if (normalizedScope == "all" || normalizedScope == "openai") {
            writeText(dumpRoot.resolve("openai.txt"), buildOpenAiInfo())
        }

        writeText(dumpRoot.resolve("recent-server-log.txt"), readRecentServerLog())
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
