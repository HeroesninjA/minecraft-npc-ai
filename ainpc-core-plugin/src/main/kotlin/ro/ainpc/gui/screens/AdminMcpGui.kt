package ro.ainpc.gui.screens

import com.google.gson.JsonParser
import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class AdminMcpGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.MCP

    override fun title(player: Player): String = "&0MCP Admin"

    override fun size(player: Player): Int = 27

    override fun render(context: GuiRenderContext) {
        val plugin = context.plugin()
        val health = plugin.mcpRuntimeClient.health()
        val featureFlags = if (health.available) {
            val featureState = plugin.mcpRuntimeClient.callTool("ainpc.feature.state")
            parseFeatureFlags(featureState.contentJson)
        } else {
            emptyFeatureFlags()
        }

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

        context.item(
            4,
            GuiItemFactory.item(
                statusMaterial,
                "${statusColor}MCP: ${health.status}",
                listOf(
                    "&7Enabled: &f${health.enabled}",
                    "&7Available: &f${health.available}",
                    "&7Endpoint: &f${health.endpoint}",
                    "&7Detail: &f${health.detail}",
                    "&7Health: &f${health.durationMillis}ms"
                )
            )
        )

        context.item(
            13,
            GuiItemFactory.item(
                Material.PAPER,
                "&bFeature state",
                listOf(
                    "&7semanticContextExport: &f${featureFlags["semanticContextExport"] ?: "n/a"}",
                    "&7semanticContextSummaryExport: &f${featureFlags["semanticContextSummaryExport"] ?: "n/a"}",
                    "&7semanticRoutingSummaryExport: &f${featureFlags["semanticRoutingSummaryExport"] ?: "n/a"}",
                    "&7routingSemanticContextSummaryExport: &f${featureFlags["routingSemanticContextSummaryExport"] ?: "n/a"}"
                )
            )
        )

        context.button(
            10,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.COMPASS,
                    "&6Routing summary",
                    listOf(
                        "&7Deschide debugdump routing summary.",
                        "&8Rezumat semantic agregat."
                    )
                ),
            ) { click -> click.service().runCommand(click.player(), "ainpc debugdump routing summary") }
        )

        context.button(
            11,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.BOOK,
                    "&aWorld summary",
                    listOf(
                        "&7Deschide debugdump world summary.",
                        "&8World lore + history."
                    )
                ),
            ) { click -> click.service().runCommand(click.player(), "ainpc debugdump world summary") }
        )

        context.button(
            12,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.WRITABLE_BOOK,
                    "&dStory summary",
                    listOf(
                        "&7Deschide debugdump story summary.",
                        "&8Story context + history."
                    )
                ),
            ) { click -> click.service().runCommand(click.player(), "ainpc debugdump story summary") }
        )

        context.button(
            14,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.KNOWLEDGE_BOOK,
                    "&bQuest summary",
                    listOf(
                        "&7Deschide debugdump quest authoring summary.",
                        "&8Quest lore + history."
                    )
                ),
            ) { click -> click.service().runCommand(click.player(), "ainpc authoring summary") }
        )

        context.button(
            16,
            GuiButton.enabled(
                GuiItemFactory.item(
                    Material.SUNFLOWER,
                    "&aRefresh",
                    listOf("&7Reincarca starea MCP.")
                ),
            ) { click -> click.service().open(click.player(), GuiKey.MCP) }
        )

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun parseFeatureFlags(contentJson: String): Map<String, String> {
        val keys = listOf(
            "semanticContextExport",
            "semanticContextSummaryExport",
            "semanticRoutingSummaryExport",
            "routingSemanticContextSummaryExport"
        )
        val root = runCatching { JsonParser.parseString(contentJson).asJsonObject }.getOrNull()
            ?: return keys.associateWith { "n/a" }
        val tools = root.getAsJsonObject("tools") ?: return keys.associateWith { "n/a" }
        return keys.associateWith { key ->
            tools.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean?.toString() ?: "n/a"
        }
    }

    private fun emptyFeatureFlags(): Map<String, String> = listOf(
        "semanticContextExport",
        "semanticContextSummaryExport",
        "semanticRoutingSummaryExport",
        "routingSemanticContextSummaryExport"
    ).associateWith { "n/a" }
}
