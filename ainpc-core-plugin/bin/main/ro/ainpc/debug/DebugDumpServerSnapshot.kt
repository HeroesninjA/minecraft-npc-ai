package ro.ainpc.debug

import ro.ainpc.AINPCPlugin
import java.nio.file.Path
import java.time.LocalDateTime

object DebugDumpServerSnapshot {
    @JvmStatic
    fun buildSummary(scope: String, dumpRoot: Path, plugin: AINPCPlugin): String {
        val sb = StringBuilder()
        sb.append("AINPC Debug Dump\n")
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n")
        sb.append("Scope: ").append(scope).append("\n")
        sb.append("Path: ").append(dumpRoot.toAbsolutePath()).append("\n")
        sb.append("Plugin version: ").append(plugin.pluginMeta.version).append("\n")
        sb.append("NPC count: ").append(runCatching { plugin.npcManager.getNPCCount() }.getOrDefault(0)).append("\n")

        val worldAdmin = runCatching { plugin.platform.worldAdmin }.getOrNull()
        if (worldAdmin != null) {
            sb.append("World admin enabled: ").append(worldAdmin.isEnabled).append("\n")
            sb.append("Regions: ").append(worldAdmin.regionCount).append("\n")
            sb.append("Places: ").append(worldAdmin.placeCount).append("\n")
            sb.append("Nodes: ").append(worldAdmin.nodeCount).append("\n")
        }

        sb.append("\nFiles:\n")
        sb.append("- summary.txt\n")
        sb.append("- server.txt\n")
        sb.append("- config-sanitized.yml\n")
        sb.append("- audit.txt\n")
        sb.append("- npcs.json, world-mapping.json, npc-world-bindings.json, households.json, spawn-batches.json, quests.yml, quest-audit-report.txt, loaded-quest-definitions.json, player-progressions.json, player-quest-progress.json, quest-anchor-bindings.json, story-states.json, story-events.json, openai.txt depending on scope\n")
        sb.append("- recent-server-log.txt\n")
        return sb.toString()
    }

    @JvmStatic
    fun buildServerInfo(plugin: AINPCPlugin): String {
        val server = plugin.server
        val sb = StringBuilder()
        sb.append("Server: ").append(server.name).append("\n")
        sb.append("Bukkit version: ").append(server.bukkitVersion).append("\n")
        sb.append("Minecraft version: ").append(server.minecraftVersion).append("\n")
        sb.append("Java version: ").append(System.getProperty("java.version")).append("\n")
        sb.append("Java vendor: ").append(System.getProperty("java.vendor")).append("\n")
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ")
            .append(System.getProperty("os.version")).append(" ")
            .append(System.getProperty("os.arch")).append("\n")
        sb.append("Online players: ").append(server.onlinePlayers.size).append("\n")
        sb.append("\nLoaded worlds:\n")
        for (world in server.worlds) {
            sb.append("- ").append(world.name)
                .append(" env=").append(world.environment)
                .append(" loadedChunks=").append(world.loadedChunks.size)
                .append(" entities=").append(world.entities.size)
                .append("\n")
        }
        return sb.toString()
    }
}
