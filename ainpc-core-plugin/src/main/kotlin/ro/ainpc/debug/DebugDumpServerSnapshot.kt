package ro.ainpc.debug

import ro.ainpc.AINPCPlugin
import java.nio.file.Path
import java.time.LocalDateTime

internal data class DebugDumpWorldSummary(
    val name: String,
    val environment: String,
    val loadedChunkCount: Int,
    val entityCount: Int,
)

internal data class DebugDumpWorldAdminSummary(
    val enabled: Boolean,
    val regionCount: Int,
    val placeCount: Int,
    val nodeCount: Int,
)

internal data class DebugDumpServerSnapshotData(
    val pluginVersion: String,
    val npcCount: Int,
    val worldAdmin: DebugDumpWorldAdminSummary?,
    val serverName: String,
    val bukkitVersion: String,
    val minecraftVersion: String,
    val javaVersion: String,
    val javaVendor: String,
    val osName: String,
    val osVersion: String,
    val osArch: String,
    val onlinePlayerCount: Int,
    val worlds: List<DebugDumpWorldSummary>,
)

object DebugDumpServerSnapshot {
    @JvmStatic
    fun buildSummary(scope: String, dumpRoot: Path, plugin: AINPCPlugin): String =
        buildSummary(scope, dumpRoot, LocalDateTime.now(), capture(plugin))

    internal fun buildSummary(
        scope: String,
        dumpRoot: Path,
        capturedAt: LocalDateTime,
        snapshot: DebugDumpServerSnapshotData,
    ): String {
        val sb = StringBuilder()
        sb.append("AINPC Debug Dump\n")
        sb.append("Generated: ").append(capturedAt).append("\n")
        sb.append("Scope: ").append(scope).append("\n")
        sb.append("Path: ").append(dumpRoot.toAbsolutePath()).append("\n")
        sb.append("Plugin version: ").append(snapshot.pluginVersion).append("\n")
        sb.append("NPC count: ").append(snapshot.npcCount).append("\n")

        snapshot.worldAdmin?.let { worldAdmin ->
            sb.append("World admin enabled: ").append(worldAdmin.enabled).append("\n")
            sb.append("Regions: ").append(worldAdmin.regionCount).append("\n")
            sb.append("Places: ").append(worldAdmin.placeCount).append("\n")
            sb.append("Nodes: ").append(worldAdmin.nodeCount).append("\n")
        }

        sb.append("\nFiles:\n")
        sb.append("- summary.txt\n")
        sb.append("- server.txt\n")
        sb.append("- config-sanitized.yml\n")
        sb.append("- audit.txt\n")
        sb.append("- quest.txt, mapping.txt, npcs.json, world-mapping.json, mapping-snapshot.json, world-admin-snapshot.json, npc-world-bindings.json, households.json, spawn-batches.json, quests.yml, quests-snapshot.json, quest-audit-report.txt, loaded-quest-definitions.json, quest-mapping-contract.json, player-progressions.json, player-quest-progress.json, quest-anchor-bindings.json, story-states.json, story-events.json, openai.txt depending on scope\n")
        sb.append("- recent-server-log.txt\n")
        sb.append("- recent-api-events.txt\n")
        sb.append("- recent-story-events.txt\n")
        sb.append("- narrative-plans.json (when scope=all)\n")
        return sb.toString()
    }

    @JvmStatic
    fun buildServerInfo(plugin: AINPCPlugin): String = buildServerInfo(capture(plugin))

    internal fun buildServerInfo(snapshot: DebugDumpServerSnapshotData): String {
        val sb = StringBuilder()
        sb.append("Server: ").append(snapshot.serverName).append("\n")
        sb.append("Bukkit version: ").append(snapshot.bukkitVersion).append("\n")
        sb.append("Minecraft version: ").append(snapshot.minecraftVersion).append("\n")
        sb.append("Java version: ").append(snapshot.javaVersion).append("\n")
        sb.append("Java vendor: ").append(snapshot.javaVendor).append("\n")
        sb.append("OS: ").append(snapshot.osName).append(" ")
            .append(snapshot.osVersion).append(" ")
            .append(snapshot.osArch).append("\n")
        sb.append("Online players: ").append(snapshot.onlinePlayerCount).append("\n")
        sb.append("\nLoaded worlds:\n")
        snapshot.worlds.forEach { world ->
            sb.append("- ").append(world.name)
                .append(" env=").append(world.environment)
                .append(" loadedChunks=").append(world.loadedChunkCount)
                .append(" entities=").append(world.entityCount)
                .append("\n")
        }
        return sb.toString()
    }

    internal fun capture(plugin: AINPCPlugin): DebugDumpServerSnapshotData {
        val server = plugin.server
        val worldAdmin = runCatching { plugin.platform.worldAdmin }.getOrNull()?.let { snapshot ->
            DebugDumpWorldAdminSummary(
                snapshot.isEnabled,
                snapshot.regionCount,
                snapshot.placeCount,
                snapshot.nodeCount,
            )
        }
        return DebugDumpServerSnapshotData(
            plugin.pluginMeta.version,
            runCatching { plugin.npcManager.getNPCCount() }.getOrDefault(0),
            worldAdmin,
            server.name,
            server.bukkitVersion,
            server.minecraftVersion,
            System.getProperty("java.version"),
            System.getProperty("java.vendor"),
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"),
            server.onlinePlayers.size,
            server.worlds.map { world ->
                DebugDumpWorldSummary(
                    world.name,
                    world.environment.name,
                    world.loadedChunks.size,
                    world.entities.size,
                )
            },
        )
    }
}
