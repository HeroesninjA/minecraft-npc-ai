package ro.ainpc.mcp.bridge

import com.google.gson.GsonBuilder
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import ro.ainpc.AINPCPlugin
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
        val mcpConfig = plugin.config.getBoolean("features.mcp", true)
        val snapshotEnabled = plugin.config.getBoolean("mcp.snapshot.auto", true)
        val intervalTicks = plugin.config.getLong("mcp.snapshot.interval_ticks", 100L)
            .coerceIn(20L, 6000L)
        if (!mcpConfig || !snapshotEnabled) return
        runTaskTimerAsynchronously(plugin, 40L, intervalTicks)
        plugin.logger.info("RuntimeSnapshotProducer pornit (la fiecare ${intervalTicks}tick, fisier: $snapshotPath).")
    }

    fun stop() {
        try { cancel() } catch (_: Exception) {}
    }
}
