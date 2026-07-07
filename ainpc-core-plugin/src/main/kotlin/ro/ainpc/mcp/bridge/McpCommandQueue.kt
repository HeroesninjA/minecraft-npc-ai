package ro.ainpc.mcp.bridge

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.NPCState
import java.io.File
import java.util.UUID
import java.util.logging.Level

class McpCommandQueue(private val plugin: AINPCPlugin) {
    private val commandDir: File
        get() {
            val path = plugin.config.getString("mcp.command.path", "data/mcp-commands")
            val dir = File(path)
            if (!dir.isAbsolute) {
                return File(plugin.server.worldContainer.parentFile, path)
            }
            return dir
        }

    private val gson = Gson()
    private val processedFiles = mutableSetOf<String>()

    data class McpCommand(
        val commandId: String = UUID.randomUUID().toString(),
        val type: String,
        val params: Map<String, Any> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    )

    data class McpCommandResult(
        val commandId: String,
        val success: Boolean,
        val message: String = "",
        val data: Map<String, Any> = emptyMap(),
        val processedAt: Long = System.currentTimeMillis()
    )

    fun start() {
        commandDir.mkdirs()
        plugin.debug("[McpCmd] Director comenzi MCP: ${commandDir.absolutePath}")
    }

    fun tick() {
        if (!commandDir.exists()) return
        val files = commandDir.listFiles { f -> f.name.endsWith(".cmd.json") } ?: return
        for (file in files.sortedBy { it.lastModified() }) {
            if (file.name in processedFiles) continue
            processCommandFile(file)
            processedFiles.add(file.name)
        }
        cleanupProcessed()
    }

    private fun processCommandFile(file: File) {
        try {
            val content = file.readText()
            val command = gson.fromJson(content, McpCommand::class.java) ?: return
            val result = executeCommand(command)
            val resultFile = File(commandDir, "${file.nameWithoutExtension}.result.json")
            resultFile.writeText(gson.toJson(result))
            file.delete()
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "[McpCmd] Eroare procesare comanda ${file.name}", e)
            try {
                val resultFile = File(commandDir, "${file.nameWithoutExtension}.result.json")
                resultFile.writeText(gson.toJson(McpCommandResult(
                    commandId = file.name.removeSuffix(".cmd.json"),
                    success = false,
                    message = e.message ?: "Eroare necunoscuta"
                )))
            } catch (_: Exception) {}
        }
    }

    private fun executeCommand(command: McpCommand): McpCommandResult {
        return when (command.type) {
            "npc.say" -> executeNpcSay(command)
            "npc.setState" -> executeNpcSetState(command)
            "npc.teleport" -> executeNpcTeleport(command)
            "broadcast" -> executeBroadcast(command)
            "executeCommand" -> executeConsoleCommand(command)
            "quest.progress" -> executeQuestProgress(command)
            "quest.complete" -> executeQuestComplete(command)
            "quest.list" -> executeQuestList(command)
            else -> McpCommandResult(
                commandId = command.commandId,
                success = false,
                message = "Tip comanda necunoscut: ${command.type}"
            )
        }
    }

    private fun resolveNpc(params: Map<String, Any>): ro.ainpc.npc.AINPC? {
        val npcId = params["npcId"]?.toString()
        val npcName = params["npcName"]?.toString()
        val npcUuid = params["uuid"]?.toString()
        return when {
            npcUuid != null -> plugin.npcManager.getNPCByUUID(UUID.fromString(npcUuid))
            npcId != null -> plugin.npcManager.getNPCById(npcId.toIntOrNull() ?: return null)
            npcName != null -> plugin.npcManager.getNPCByName(npcName)
            else -> null
        }
    }

    private fun executeNpcSay(command: McpCommand): McpCommandResult {
        val npc = resolveNpc(command.params) ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "NPC negasit"
        )
        val message = command.params["message"]?.toString() ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "Parametru 'message' lipsa"
        )
        val range = (command.params["range"] as? Number)?.toDouble() ?: 32.0
        val location = npc.location ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "NPC fara locatie"
        )
        val players = location.world?.players?.filter { p ->
            p.location.distanceSquared(location) <= range * range
        } ?: emptyList()
        for (player in players) {
            player.sendMessage("§7[§6${npc.name}§7] §f$message")
        }
        return McpCommandResult(
            commandId = command.commandId, success = true,
            message = "Mesaj trimis la ${players.size} jucatori",
            data = mapOf("playerCount" to players.size)
        )
    }

    private fun executeNpcSetState(command: McpCommand): McpCommandResult {
        val npc = resolveNpc(command.params) ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "NPC negasit"
        )
        val stateStr = command.params["state"]?.toString() ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "Parametru 'state' lipsa"
        )
        val state = try {
            NPCState.valueOf(stateStr.uppercase())
        } catch (e: IllegalArgumentException) {
            return McpCommandResult(
                commandId = command.commandId, success = false,
                message = "Stare invalida: $stateStr. Valori: ${NPCState.entries.joinToString(", ") { it.name }}"
            )
        }
        npc.changeState(state)
        return McpCommandResult(
            commandId = command.commandId, success = true,
            message = "Starea NPC-ului ${npc.name} a fost setata la ${state.displayName}"
        )
    }

    private fun executeNpcTeleport(command: McpCommand): McpCommandResult {
        val npc = resolveNpc(command.params) ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "NPC negasit"
        )
        val x = (command.params["x"] as? Number)?.toDouble()
        val y = (command.params["y"] as? Number)?.toDouble()
        val z = (command.params["z"] as? Number)?.toDouble()
        val worldName = command.params["world"]?.toString()
        if (x == null || y == null || z == null) {
            return McpCommandResult(
                commandId = command.commandId, success = false,
                message = "Parametri 'x', 'y', 'z' lipsa sau invalizi"
            )
        }
        val world = if (worldName != null) plugin.server.getWorld(worldName) else npc.location?.world
        if (world == null) {
            return McpCommandResult(
                commandId = command.commandId, success = false, message = "Lumea negasita"
            )
        }
        npc.teleport(org.bukkit.Location(world, x, y, z))
        return McpCommandResult(
            commandId = command.commandId, success = true,
            message = "NPC-ul ${npc.name} a fost teleportat"
        )
    }

    private fun executeBroadcast(command: McpCommand): McpCommandResult {
        val message = command.params["message"]?.toString() ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "Parametru 'message' lipsa"
        )
        plugin.server.broadcastMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message))
        return McpCommandResult(
            commandId = command.commandId, success = true,
            message = "Mesaj broadcast trimis"
        )
    }

    private fun executeConsoleCommand(command: McpCommand): McpCommandResult {
        val cmd = command.params["command"]?.toString() ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "Parametru 'command' lipsa"
        )
        val success = plugin.server.dispatchCommand(plugin.server.consoleSender, cmd)
        return McpCommandResult(
            commandId = command.commandId, success = success,
            message = if (success) "Comanda executata: $cmd" else "Esec la executia comenzii: $cmd"
        )
    }

    private fun executeQuestProgress(command: McpCommand): McpCommandResult {
        val playerName = command.params["player"]?.toString() ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "Parametru 'player' lipsa"
        )
        val player = plugin.server.getPlayerExact(playerName) ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "Jucator negasit: $playerName"
        )
        val snapshot = plugin.progressionService.getProgressionGuiSnapshot(player, "all", false)
        val active = snapshot.allEntries().filter { it.active() }
        val data = active.map { entry -> mapOf(
            "title" to entry.title(),
            "mechanic" to (entry.mechanicDisplay() ?: ""),
            "status" to (entry.statusDisplay() ?: ""),
            "tracked" to entry.tracked().toString()
        )}
        return McpCommandResult(
            commandId = command.commandId, success = true,
            message = "Progres chestionat pentru $playerName: ${active.size} active",
            data = mapOf("quests" to data, "count" to active.size)
        )
    }

    private fun executeQuestComplete(command: McpCommand): McpCommandResult {
        val playerName = command.params["player"]?.toString() ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "Parametru 'player' lipsa"
        )
        val objectiveKey = command.params["objective"]?.toString() ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "Parametru 'objective' lipsa"
        )
        val player = plugin.server.getPlayerExact(playerName) ?: return McpCommandResult(
            commandId = command.commandId, success = false, message = "Jucator negasit: $playerName"
        )
        plugin.server.dispatchCommand(plugin.server.consoleSender,
            "ainpc quest progress $playerName $objectiveKey")
        return McpCommandResult(
            commandId = command.commandId, success = true,
            message = "Obiectivul '$objectiveKey' marcat ca progres pentru $playerName"
        )
    }

    private fun executeQuestList(command: McpCommand): McpCommandResult {
        val playerName = command.params["player"]?.toString()
        val filter = command.params["filter"]?.toString() ?: "all"

        val data = if (playerName != null) {
            val player = plugin.server.getPlayerExact(playerName)
            if (player == null) return McpCommandResult(
                commandId = command.commandId, success = false, message = "Jucator negasit: $playerName"
            )
            val snapshot = plugin.progressionService.getProgressionGuiSnapshot(player, filter, false)
            snapshot.allEntries().map { entry -> mapOf(
                "title" to entry.title(),
                "mechanic" to (entry.mechanicDisplay() ?: ""),
                "status" to (entry.statusDisplay() ?: ""),
                "actor" to (entry.actorName() ?: ""),
                "tracked" to entry.tracked().toString(),
                "current" to entry.current().toString(),
                "active" to entry.active().toString()
            )}
        } else {
            plugin.server.getOnlinePlayers().flatMap { p ->
                val snapshot = plugin.progressionService.getProgressionGuiSnapshot(p, filter, false)
                snapshot.allEntries().map { entry -> mapOf(
                    "player" to p.name,
                    "title" to entry.title(),
                    "mechanic" to (entry.mechanicDisplay() ?: ""),
                    "status" to (entry.statusDisplay() ?: ""),
                    "actor" to (entry.actorName() ?: ""),
                    "tracked" to entry.tracked().toString()
                )}
            }
        }
        return McpCommandResult(
            commandId = command.commandId, success = true,
            message = "Chestionare finalizata: ${data.size} entry-uri",
            data = mapOf("entries" to data, "count" to data.size)
        )
    }

    private fun cleanupProcessed() {
        if (processedFiles.size > 100) {
            val toRemove = processedFiles.take(50)
            processedFiles.removeAll(toRemove)
        }
    }
}
