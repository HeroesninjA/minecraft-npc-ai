package ro.ainpc.mcp.bridge

import com.google.gson.GsonBuilder
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import ro.ainpc.AINPCPlugin
import ro.ainpc.mcp.McpRuntimeConfig
import java.nio.file.Path
import java.time.Instant

class RuntimeSnapshotProducer(
    private val plugin: AINPCPlugin,
    private val snapshotPath: Path = Path.of("data", "mcp-runtime-snapshot.json")
) : BukkitRunnable() {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val startTime = System.currentTimeMillis()

    override fun run() {
        try {
            val snapshot = buildSnapshot()
            writeSnapshot(snapshot)
        } catch (e: Exception) {
            plugin.logger.warning("RuntimeSnapshotProducer: eroare la producerea snapshot-ului: ${e.message}")
        }
    }

    private fun buildSnapshot(): RuntimeSnapshot {
        val worldAdmin = plugin.platform.worldAdmin
        val allNpcs = plugin.npcManager.getAllNPCs()
        val buildModePlayers = Bukkit.getOnlinePlayers()
            .mapNotNull { player ->
                if (!plugin.guiService.isBuildModeEnabled(player)) return@mapNotNull null
                val state = plugin.guiService.getBuildModeTarget(player)
                val parts = state.split(":", limit = 2)
                val style = parts.getOrNull(0)?.ifBlank { "wand" } ?: "wand"
                val target = parts.getOrNull(1)?.ifBlank { "region" } ?: "region"
                ro.ainpc.mcp.bridge.BuildModePlayerSnapshot(player.name, style, target)
            }
        val buildModeByStyle = buildModePlayers.groupingBy { it.style }.eachCount().toSortedMap()
        val buildModeByTarget = buildModePlayers.groupingBy { it.target }.eachCount().toSortedMap()
        val buildModeHistory = Bukkit.getOnlinePlayers()
            .flatMap { player ->
                plugin.guiService.getBuildModeHistory(player).map { history ->
                    BuildModeHistorySnapshot(
                        playerName = player.name,
                        timestampMillis = history.timestampMillis,
                        action = history.action,
                        style = history.style,
                        target = history.target
                    )
                }
            }
            .sortedByDescending { it.timestampMillis }
            .take(24)

        val npcByRegion = allNpcs
            .mapNotNull { npc ->
                if (npc.worldName != null && (npc.x != 0.0 || npc.y != 0.0 || npc.z != 0.0)) {
                    val region = worldAdmin.findRegion(npc.worldName!!, npc.x.toInt(), npc.y.toInt(), npc.z.toInt())
                    region?.id()
                } else null
            }
            .groupBy { it }
            .mapValues { it.value.size }

        val npcSamples = allNpcs
            .asSequence()
            .filter { it.name.isNotBlank() }
            .take(10)
            .map { npc ->
                val regionId = if (npc.worldName != null) {
                    worldAdmin.findRegion(npc.worldName!!, npc.x.toInt(), npc.y.toInt(), npc.z.toInt())?.id()
                } else null
                NpcSample(
                    npcId = npc.databaseId,
                    name = npc.name,
                    profession = npc.occupation,
                    regionId = regionId,
                    worldName = npc.worldName
                )
            }
            .toList()

        val storedProgressions = try {
            plugin.progressionService.getStoredProgressions()
        } catch (_: Exception) {
            emptyList()
        }
        val questSamples = storedProgressions
            .asSequence()
            .take(10)
            .map { QuestSample(
                templateId = it.templateId(),
                mechanic = it.category(),
                playerUuid = it.playerUuid(),
                status = it.status()
            ) }
            .toList()

        return RuntimeSnapshot(
            timestamp = Instant.now().toString(),
            plugin = PluginSnapshot(
                version = plugin.pluginMeta.version,
                serverType = "Paper",
                onlinePlayers = Bukkit.getOnlinePlayers().size,
                uptimeMinutes = (System.currentTimeMillis() - startTime) / 60000
            ),
            features = FeatureSnapshot(
                ai = plugin.config.getBoolean("features.ai", false),
                mcp = plugin.config.getBoolean("features.mcp", true),
                aiOrchestration = plugin.config.getBoolean("ai.orchestration.enabled", false)
            ),
            npc = NpcSnapshot(
                totalCount = plugin.npcManager.getNPCCount(),
                byRegion = npcByRegion,
                samples = npcSamples
            ),
            world = WorldSnapshot(
                regionCount = worldAdmin.regionCount,
                placeCount = worldAdmin.placeCount,
                nodeCount = worldAdmin.nodeCount,
                regionNames = worldAdmin.regions.map { it.name() },
                placeNames = worldAdmin.places.map { it.displayName() }
            ),
            quests = QuestSnapshot(
                activePlayerQuests = storedProgressions.count { it.playerUuid().isNotEmpty() },
                activeGlobalQuests = storedProgressions.count { it.playerUuid().isEmpty() },
                samples = questSamples
            ),
            buildMode = BuildModeSnapshot(
                activePlayers = buildModePlayers.size,
                byStyle = buildModeByStyle,
                byTarget = buildModeByTarget,
                players = buildModePlayers,
                history = buildModeHistory
            )
        )
    }

    private fun writeSnapshot(snapshot: RuntimeSnapshot) {
        val tmpFile = snapshotPath.resolveSibling("${snapshotPath.fileName}.tmp")
        try {
            tmpFile.toFile().writeText(gson.toJson(snapshot))
            tmpFile.toFile().renameTo(snapshotPath.toFile())
        } catch (e: Exception) {
            try { tmpFile.toFile().delete() } catch (_: Exception) {}
            throw e
        }
    }

    fun start() {
        val config = McpRuntimeConfig.from(plugin.config)
        if (!config.enabled || !config.snapshotAuto) return
        val ticks = config.snapshotIntervalTicks
        runTaskTimerAsynchronously(plugin, 40L, ticks)
        plugin.logger.info("RuntimeSnapshotProducer pornit (la fiecare ${ticks}tick, fisier: $snapshotPath).")
    }

    fun stop() {
        try { cancel() } catch (_: Exception) {}
    }
}
