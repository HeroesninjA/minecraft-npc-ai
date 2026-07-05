package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.ai.OpenAIConnectionProbe
import ro.ainpc.gui.GuiButton
import ro.ainpc.version.BuildVersionInfo
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen

class DebugGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.DEBUG

    override fun title(player: Player): String = "&0AINPC Debug"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val versionSnapshot = BuildVersionInfo.capture(context.plugin())
        val probeResult = OpenAIConnectionProbe.getLastProbeResult()
        val connectionMaterial: Material
        val connectionColor: String
        val connectionStatusText: String
        if (probeResult == null) {
            connectionMaterial = Material.GRAY_DYE
            connectionColor = "&7"
            connectionStatusText = "Nefacuta"
        } else if (probeResult.isReachable() && probeResult.isModelAvailable()) {
            connectionMaterial = Material.LIME_DYE
            connectionColor = "&a"
            connectionStatusText = "Connected"
        } else if (probeResult.isReachable()) {
            connectionMaterial = Material.ORANGE_DYE
            connectionColor = "&6"
            connectionStatusText = "Degraded"
        } else {
            connectionMaterial = Material.RED_DYE
            connectionColor = "&c"
            connectionStatusText = "Failed"
        }
        context.item(
            0,
            GuiItemFactory.item(
                connectionMaterial,
                "${connectionColor}OpenAI: $connectionStatusText",
                listOf(
                    "&7Model: &f${if (probeResult != null) "(see debugdump)" else "necunoscut"}",
                    "&7Status: &f$connectionStatusText",
                    if (probeResult != null && probeResult.errors.isNotEmpty()) {
                        "&7Erori: &f${probeResult.errors.joinToString("; ")}"
                    } else {
                        "&8Fara erori."
                    },
                    "&8Foloseste debugdump openai pentru detalii."
                )
            )
        )
        context.item(
            4,
            GuiItemFactory.item(
                Material.SPYGLASS,
                "&9Debug tools",
                listOf(
                    "&7Debugdump-ul este read-only.",
                    "&7Fisierele si rezultatele sunt raportate in chat/consola.",
                    "&8Actiuni principale: debugdump / authoring / audit"
                )
            )
        )
        context.item(
            5,
            GuiItemFactory.item(
                Material.PAPER,
                "&aVersion snapshot",
                listOf(
                    "&7Versiune: &f${versionSnapshot.version}",
                    "&7Build: &f${versionSnapshot.buildHash}",
                    "&8Snapshot de suport."
                )
            )
        )
        if (ro.ainpc.commands.isRuntimeReadOnly(context.plugin())) {
            context.item(6, GuiItemFactory.item(Material.BARRIER, "&cRead-only activ", listOf(
                "&7MCP ruleaza in mod read-only.",
                "&8Inspectia ramane disponibila: debug / status / export."
            )))
        }

        dumpButton(context, 10, "all", Material.NETHER_STAR, "&6Debugdump all")
        dumpButton(context, 11, "npc", Material.VILLAGER_SPAWN_EGG, "&eDebugdump NPC")
        dumpButton(context, 12, "world", Material.COMPASS, "&bDebugdump world")
        dumpButton(context, 13, "quest", Material.WRITABLE_BOOK, "&dDebugdump quest")
        dumpButton(context, 14, "story", Material.AMETHYST_SHARD, "&dDebugdump story")
        dumpButton(context, 15, "openai", Material.ENDER_EYE, "&aDebugdump OpenAI")
        context.button(
            18,
            GuiButton.enabled(
                GuiItemFactory.item(Material.LIME_DYE, "&aQuick debugdump", "&7Ruleaza /ainpc debugdump all si raporteaza in chat."),
            ) { click -> click.service().runCommand(click.player(), "ainpc debugdump all") }
        )
        context.button(
            17,
            GuiButton.enabled(
                GuiItemFactory.item(Material.ENCHANTED_BOOK, "&bQuest Authoring", "&7Snapshot read-only pentru story, mapping si progresie."),
            ) { click -> click.service().open(click.player(), GuiKey.AUTHORING) }
        )

        context.button(
            6,
            GuiButton.enabled(
                GuiItemFactory.item(Material.NAME_TAG, "&eVersion", "&7Ruleaza /npc version."),
            ) { click -> click.service().runCommand(click.player(), "npc version") }
        )
        context.button(
            7,
            GuiButton.enabled(
                GuiItemFactory.item(Material.REDSTONE_TORCH, "&cQuest Audit", "&7Ruleaza /ainpc audit quest."),
            ) { click -> click.service().runCommand(click.player(), "ainpc audit quest") }
        )
        context.button(
            8,
            GuiButton.enabled(
                GuiItemFactory.item(Material.COMPASS, "&bWorld Audit", "&7Ruleaza /ainpc audit world."),
            ) { click -> click.service().runCommand(click.player(), "ainpc audit world") }
        )

        context.button(
            16,
            GuiButton.enabled(
                GuiItemFactory.item(Material.LIME_DYE, "&aTest OpenAI", "&7Ruleaza /ainpc test."),
            ) { click -> click.service().runCommand(click.player(), "ainpc test") }
        )
        context.button(
            20,
            GuiButton.enabled(
                GuiItemFactory.item(Material.ENDER_EYE, "&bMCP", listOf("&7Deschide MCP admin.", "&7Health, flags, routing.")),
            ) { click -> click.service().open(click.player(), GuiKey.MCP) }
        )
        context.button(21, GuiButton.enabled(
            GuiItemFactory.item(Material.CLOCK, "&dBuild status", "&7Inspecteaza rapid build mode."),
        ) { click -> click.service().runCommand(click.player(), "ainpc build mode status") })
        context.button(22, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&bBuild history", "&7Ultimele schimbari build mode."),
        ) { click -> click.service().runCommand(click.player(), "ainpc build mode history") })
        context.button(23, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&dBuild export", "&7Export compact al build mode."),
        ) { click -> click.service().runCommand(click.player(), "ainpc build mode export") })
        context.button(24, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&cClear build history", "&7Curata istoricul local build mode."),
        ) { click -> click.service().runCommand(click.player(), "ainpc build mode clear-history") })

        context.button(28, GuiButton.enabled(
            GuiItemFactory.item(Material.COMMAND_BLOCK, "&6Admin Mapping", "&7Deschide panoul admin mapping.", "&8Admin separat."),
        ) { click -> click.service().open(click.player(), GuiKey.ADMIN_MAPPING) })
        context.button(29, GuiButton.enabled(
            GuiItemFactory.item(Material.KNOWLEDGE_BOOK, "&6Admin Quest", "&7Deschide panoul admin quest.", "&8Admin separat."),
        ) { click -> click.service().open(click.player(), GuiKey.ADMIN_QUEST) })

        context.button(19, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&6Audit NPC", "&7Ruleaza /ainpc audit npc."),
        ) { click -> click.service().runCommand(click.player(), "ainpc audit npc") })

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun dumpButton(context: GuiRenderContext, slot: Int, scope: String, material: Material, title: String) {
        context.button(
            slot,
            GuiButton.enabled(
                GuiItemFactory.item(material, title, "&7Ruleaza /ainpc debugdump $scope."),
            ) { click -> click.service().runCommand(click.player(), "ainpc debugdump $scope") }
        )
    }
}
