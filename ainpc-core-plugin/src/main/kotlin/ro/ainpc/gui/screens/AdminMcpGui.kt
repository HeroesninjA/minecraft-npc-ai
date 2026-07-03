package ro.ainpc.gui.screens

import com.google.gson.JsonParser
import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class AdminMcpGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.MCP

    override fun title(player: Player): String = "&0MCP Admin"

    override fun size(player: Player): Int = 36

    override fun render(context: GuiRenderContext) {
        val plugin = context.plugin()
        val health = plugin.mcpRuntimeClient.health()

        val statusMaterial = when {
            !health.enabled -> Material.GRAY_DYE
            health.available && health.status.equals("healthy", ignoreCase = true) -> Material.LIME_DYE
            health.available -> Material.ORANGE_DYE
            else -> Material.RED_DYE
        }
        val statusColor = when {
            !health.enabled -> "&7"
            health.available && health.status.equals("healthy", ignoreCase = true) -> "&a"
            health.available -> "&6"
            else -> "&c"
        }

        context.item(4, GuiItemFactory.item(statusMaterial,
            "${statusColor}MCP: ${health.status}",
            listOf(
                "&7Enabled: &f${health.enabled}",
                "&7Available: &f${health.available}",
                "&7Endpoint: &f${health.endpoint}",
                "&7Status: &f${health.status}",
                "&7Detail: &f${health.detail}",
                "&7Response: &f${health.durationMillis}ms"
            )
        ))

        val featureResult = if (health.available) {
            plugin.mcpRuntimeClient.callTool("ainpc.feature.state")
        } else null
        val featureFlags = parseFeatureFlags(featureResult?.contentJson)
        if (featureFlags["read_only"] == "true") {
            context.item(5, GuiItemFactory.item(Material.BARRIER, "&cRead-only activ", listOf(
                "&7MCP raporteaza modul read-only.",
                "&7Scrierea in mapping este blocata.",
                "&8Inspectia si exportul raman disponibile."
            )))
        }

        context.item(13, GuiItemFactory.item(Material.PAPER,
            "&bFeature flags",
            listOf(
                "&7MCP: &f${featureFlags["mcp_enabled"] ?: "?"}",
                "&7Bridge: &f${featureFlags["runtime_bridge"] ?: "?"}",
                "&7Read-only: &f${featureFlags["read_only"] ?: "?"}",
                "&7Tools: &f${featureFlags["tool_count"] ?: "?"}"
            )
        ))

        val snapshot = if (health.available) {
            plugin.mcpRuntimeClient.callTool("ainpc.server.snapshot")
        } else null
        val runtimeInfo = parseSnapshot(snapshot?.contentJson)
        val buildModeResult = if (health.available) {
            plugin.mcpRuntimeClient.callTool("ainpc.build.mode.status")
        } else null
        val buildModeInfo = parseBuildModeStatus(buildModeResult?.contentJson)
        val buildModeHistoryResult = if (health.available) {
            plugin.mcpRuntimeClient.callTool("ainpc.build.mode.history")
        } else null
        val buildModeHistoryInfo = parseBuildModeHistory(buildModeHistoryResult?.contentJson)
        val buildModeExportResult = if (health.available) {
            plugin.mcpRuntimeClient.callTool("ainpc.build.mode.export")
        } else null
        val buildModeExportInfo = parseBuildModeExport(buildModeExportResult?.contentJson)

        context.item(22, GuiItemFactory.item(Material.CLOCK,
            "&eRuntime snapshot",
            listOf(
                "&7NPCs: &f${runtimeInfo["npcs"] ?: "?"}",
                "&7Regiuni: &f${runtimeInfo["regions"] ?: "?"}",
                "&7Places: &f${runtimeInfo["places"] ?: "?"}",
                "&7Questuri: &f${runtimeInfo["quests"] ?: "?"}",
                "&7Players: &f${runtimeInfo["players"] ?: "?"}"
            )
        ))

        context.item(30, GuiItemFactory.item(
            if ((buildModeInfo["build_players"] ?: runtimeInfo["build_players"] ?: "0").toIntOrNull() ?: 0 > 0) Material.LIME_DYE else Material.GRAY_DYE,
            "&dBuild mode",
            listOf(
                "&7Activi: &f${buildModeInfo["build_players"] ?: runtimeInfo["build_players"] ?: "0"}",
                "&7Style: &f${buildModeInfo["build_styles"] ?: runtimeInfo["build_styles"] ?: "?"}",
                "&7Target: &f${buildModeInfo["build_targets"] ?: runtimeInfo["build_targets"] ?: "?"}",
                "&7Players: &f${buildModeInfo["build_players_list"] ?: "?"}",
                "&7History: &f${buildModeHistoryInfo["build_history_count"] ?: "0"}",
                "&7Recent: &f${buildModeHistoryInfo["build_history_items"] ?: "[]"}",
                "&7Export: &f${buildModeExportInfo["build_export_status"] ?: "?"}"
            )
        ))

        context.button(29, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&cClear build history",
                listOf("&7Curata istoricul build mode pentru playerul curent.")),
        ) { click -> click.service().runCommand(click.player(), "ainpc build mode clear-history") })

        context.button(28, GuiButton.enabled(
            GuiItemFactory.item(Material.CLOCK, "&eBuild status",
                listOf("&7Afiseaza starea curenta a build mode.")),
        ) { click -> click.service().runCommand(click.player(), "ainpc build mode status") })

        context.button(38, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&bBuild history",
                listOf("&7Afiseaza ultimele schimbari ale build mode.")),
        ) { click -> click.service().runCommand(click.player(), "ainpc build mode history") })

        context.button(39, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&dBuild export",
                listOf("&7Afiseaza exportul compact al build mode.")),
        ) { click -> click.service().runCommand(click.player(), "ainpc build mode export") })

        context.button(10, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&6World context",
                listOf("&7Deschide debugdump world semantic context.")),
        ) { click -> click.service().runCommand(click.player(), "ainpc debugdump world summary") })

        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.BOOK, "&aStory context",
                listOf("&7Deschide debugdump story semantic context.")),
        ) { click -> click.service().runCommand(click.player(), "ainpc debugdump story summary") })

        context.button(14, GuiButton.enabled(
            GuiItemFactory.item(Material.KNOWLEDGE_BOOK, "&bQuest context",
                listOf("&7Deschide debugdump quest semantic context.")),
        ) { click -> click.service().runCommand(click.player(), "ainpc debugdump quest summary") })

        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.MAP, "&dMapping context",
                listOf("&7Deschide debugdump mapping semantic context.")),
        ) { click -> click.service().runCommand(click.player(), "ainpc debugdump mapping summary") })

        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh",
                listOf("&7Reincarca starea MCP.")),
        ) { click -> click.service().open(click.player(), GuiKey.MCP) })

        context.item(31, GuiItemFactory.item(
            if (health.available && featureResult != null) Material.LIME_DYE else Material.RED_DYE,
            "&bSnapshot path",
            listOf("&7Fisier: &fdata/mcp-runtime-snapshot.json")
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun parseFeatureFlags(contentJson: String?): Map<String, String> {
        val root = runCatching { JsonParser.parseString(contentJson ?: "{}").asJsonObject }.getOrNull() ?: return emptyMap()
        val mcp = root.getAsJsonObject("mcp")
        val tools = root.getAsJsonObject("tools")
        val bridge = root.getAsJsonObject("runtimeBridge")
        return mapOf(
            "mcp_enabled" to (mcp?.get("enabled")?.asBoolean?.toString() ?: "n/a"),
            "runtime_bridge" to (bridge?.get("status")?.asString ?: "n/a"),
            "read_only" to (tools?.get("readOnly")?.asBoolean?.toString() ?: "n/a"),
            "tool_count" to (tools?.entrySet()?.size?.toString() ?: "n/a")
        )
    }

    private fun parseSnapshot(contentJson: String?): Map<String, String> {
        val root = runCatching { JsonParser.parseString(contentJson ?: "{}").asJsonObject }.getOrNull() ?: return emptyMap()
        val available = root.get("available")?.asBoolean ?: false
        if (!available) return mapOf("error" to "bridge_indisponibil")
        val npc = root.getAsJsonObject("npc")
        val world = root.getAsJsonObject("world")
        val quests = root.getAsJsonObject("quests")
        val buildMode = root.getAsJsonObject("buildMode")
        val pluginData = root.getAsJsonObject("plugin")
        val buildPlayers = buildMode?.getAsJsonArray("players")
        return mapOf(
            "npcs" to (npc?.get("totalCount")?.asString ?: "?"),
            "regions" to (world?.get("regionCount")?.asString ?: "?"),
            "places" to (world?.get("placeCount")?.asString ?: "?"),
            "quests" to (quests?.get("activePlayerQuests")?.asString ?: "?"),
            "players" to (pluginData?.get("onlinePlayers")?.asString ?: "?"),
            "build_players" to (buildMode?.get("activePlayers")?.asString ?: "0"),
            "build_styles" to (buildMode?.get("byStyle")?.toString() ?: "{}"),
            "build_targets" to (buildMode?.get("byTarget")?.toString() ?: "{}"),
            "build_players_list" to (buildPlayers?.joinToString(", ") {
                val entry = it.asJsonObject
                "${entry.get("name")?.asString ?: "?"}:${entry.get("style")?.asString ?: "?"}/${entry.get("target")?.asString ?: "?"}"
            } ?: "[]")
        )
    }

    private fun parseBuildModeStatus(contentJson: String?): Map<String, String> {
        val root = runCatching { JsonParser.parseString(contentJson ?: "{}").asJsonObject }.getOrNull() ?: return emptyMap()
        val available = root.get("available")?.asBoolean ?: false
        if (!available) return emptyMap()
        val buildMode = root.getAsJsonObject("buildMode")
        val players = buildMode?.getAsJsonArray("players")
        return mapOf(
            "build_players" to (buildMode?.get("activePlayers")?.asString ?: "0"),
            "build_styles" to (buildMode?.get("byStyle")?.toString() ?: "{}"),
            "build_targets" to (buildMode?.get("byTarget")?.toString() ?: "{}"),
            "build_players_list" to (players?.joinToString(", ") {
                val entry = it.asJsonObject
                "${entry.get("name")?.asString ?: "?"}:${entry.get("style")?.asString ?: "?"}/${entry.get("target")?.asString ?: "?"}"
            } ?: "[]")
        )
    }

    private fun parseBuildModeHistory(contentJson: String?): Map<String, String> {
        val root = runCatching { JsonParser.parseString(contentJson ?: "{}").asJsonObject }.getOrNull() ?: return emptyMap()
        val history = root.getAsJsonArray("history")
        return mapOf(
            "build_history_count" to (history?.size()?.toString() ?: "0"),
            "build_history_items" to (history?.take(8)?.joinToString(" | ") {
                val entry = it.asJsonObject
                "${entry.get("playerName")?.asString ?: "?"}:${entry.get("action")?.asString ?: "?"}"
            } ?: "[]")
        )
    }

    private fun parseBuildModeExport(contentJson: String?): Map<String, String> {
        val root = runCatching { JsonParser.parseString(contentJson ?: "{}").asJsonObject }.getOrNull() ?: return emptyMap()
        val available = root.get("available")?.asBoolean ?: false
        val activePlayers = root.get("activePlayers")?.asString ?: "0"
        val history = root.getAsJsonArray("history")
        return mapOf(
            "build_export_status" to if (available) "ok:$activePlayers/${history?.size() ?: 0}" else "offline"
        )
    }
}
