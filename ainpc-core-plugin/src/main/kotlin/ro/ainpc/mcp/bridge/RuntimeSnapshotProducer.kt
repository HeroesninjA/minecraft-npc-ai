package ro.ainpc.mcp.bridge

import com.google.gson.GsonBuilder
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import ro.ainpc.AINPCPlugin
import ro.ainpc.bootstrap.RuntimeMetricNames
import ro.ainpc.mcp.McpRuntimeConfig
import java.nio.file.Path
import java.time.Instant

class RuntimeSnapshotProducer(
    private val plugin: AINPCPlugin,
    snapshotPath: Path = Path.of("data", "mcp-runtime-snapshot.json")
) : BukkitRunnable() {
    private val resolvedSnapshotPath: Path = if (snapshotPath.isAbsolute) snapshotPath
        else plugin.dataFolder.toPath().resolve(snapshotPath).normalize()
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val startTime = System.currentTimeMillis()

    override fun run() {
        val timer = plugin.performanceMonitor.timer(RuntimeMetricNames.RUNTIME_SNAPSHOT_EXPORT)
        timer.begin()
        try {
            val snapshot = buildSnapshot()
            writeSnapshot(snapshot)
            timer.end()
        } catch (e: Exception) {
            timer.fail()
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
                samples = npcSamples,
                relationshipCount = plugin.relationshipService.getRelationshipCount(),
                economyNpcCount = plugin.npcEconomyService.getBalanceCount(),
                economyTotalValue = plugin.npcEconomyService.getTotalEconomyValue(),
                socialGatherings = plugin.routineCoordinator.getActiveGatherings().size
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
                samples = questSamples,
                storyEventCount = runCatching {
                    val sql = "SELECT COUNT(*) as cnt FROM story_events"
                    plugin.databaseManager.prepareStatement(sql).use { stmt ->
                        stmt.executeQuery().use { rs -> if (rs.next()) rs.getInt("cnt") else 0 }
                    }
                }.getOrDefault(0),
                recentStoryEvents = runCatching {
                    val sql = "SELECT event_type, title FROM story_events ORDER BY created_at DESC LIMIT 5"
                    plugin.databaseManager.prepareStatement(sql).use { stmt ->
                        stmt.executeQuery().use { rs ->
                            val events = mutableListOf<String>()
                            while (rs.next()) events.add("${rs.getString("event_type")}: ${rs.getString("title") ?: ""}")
                            events
                        }
                    }
                }.getOrDefault(emptyList())
            ),
            buildMode = BuildModeSnapshot(
                activePlayers = buildModePlayers.size,
                byStyle = buildModeByStyle,
                byTarget = buildModeByTarget,
                players = buildModePlayers,
                history = buildModeHistory
            ),
            health = plugin.performanceMonitor.snapshot(),
        )
    }

    private fun writeSnapshot(snapshot: RuntimeSnapshot) {
        val tmpFile = resolvedSnapshotPath.resolveSibling("${resolvedSnapshotPath.fileName}.tmp")
        try {
            resolvedSnapshotPath.parent.toFile().mkdirs()
            tmpFile.toFile().writeText(gson.toJson(snapshot))
            tmpFile.toFile().renameTo(resolvedSnapshotPath.toFile())
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
        plugin.logger.info("RuntimeSnapshotProducer pornit (la fiecare ${ticks}tick, fisier: $resolvedSnapshotPath).")
    }

    fun stop() {
        try { cancel() } catch (_: Exception) {}
    }
}
