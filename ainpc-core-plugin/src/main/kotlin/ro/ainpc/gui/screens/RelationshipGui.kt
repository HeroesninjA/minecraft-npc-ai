@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.npc.AINPC
import java.util.Locale
import java.util.UUID

class RelationshipGui : GuiScreen {
    private var selectedNpcUuid: UUID? = null

    override fun key(): GuiKey = GuiKey.RELATIONSHIP

    override fun title(player: Player): String = "&0Relatii NPC"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val plugin = context.plugin()
        val adminView = player.hasPermission("ainpc.admin")
        val allNpcs = plugin.npcManager.getAllNPCs()
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .take(36)

        context.item(4, GuiItemFactory.item(
            Material.NAME_TAG,
            "&eRelatii NPC-NPC",
            listOf(
                "&7Total relatii: &f${plugin.relationshipService.getRelationshipCount()}",
                "&7NPC-uri listate: &f${allNpcs.size}",
                "&7Click pe NPC: vezi relatiile",
                "&7Right click: info NPC",
                "&8Shift click: meniu NPC"
            )
        ))

        context.button(8, GuiButton.enabled(
            GuiItemFactory.item(Material.FILLED_MAP, "&6Quest Map",
                "&7Leaga obiective de locatii."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
        ))

        if (selectedNpcUuid != null) {
            renderNpcDetail(context, selectedNpcUuid!!)
        } else {
            renderNpcList(context, allNpcs)
        }
    }

    private fun renderNpcList(context: GuiRenderContext, npcs: List<AINPC>) {
        val slots = intArrayOf(
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
        )

        for (i in npcs.indices) {
            if (i >= slots.size) break
            val npc = npcs[i]
            val npcUuid = npc.uuid
            val relCount = if (npcUuid != null) {
                plugin.relationshipService.getNPCInteractions(npcUuid).size
            } else 0
            val topRel = if (npcUuid != null) {
                plugin.relationshipService.getNPCInteractions(npcUuid).firstOrNull()
            } else null

            context.button(slots[i], GuiButton.enabled(
                GuiItemFactory.item(
                    if (relCount > 0) Material.EMERALD else Material.PLAYER_HEAD,
                    "&f${npc.name}",
                    listOf(
                        "&7Ocupatie: &f${npc.occupation ?: "N/A"}",
                        "&7Relatii: &f$relCount",
                        if (topRel != null) {
                            val partnerName = plugin.npcManager.getNPCByUuid(topRel.first)?.name ?: "Unknown"
                            "&7Cel mai apropiat: &f$partnerName"
                        } else "&7Nu are relatii",
                        "&7Click: vezi detalii relatii",
                        "&8Right click: info NPC"
                    )
                ),
                GuiAction { click ->
                    if (click.clickType().isRightClick) {
                        click.service().runCommand(click.player(), "ainpc info ${npc.name}")
                    } else if (npcUuid != null) {
                        selectedNpcUuid = npcUuid
                        click.service().open(click.player(), GuiKey.RELATIONSHIP)
                    }
                }
            ))
        }
    }

    private fun renderNpcDetail(context: GuiRenderContext, npcUuid: UUID) {
        val plugin = context.plugin()
        val npc = plugin.npcManager.getNPCByUuid(npcUuid)
        if (npc == null) { selectedNpcUuid = null; return }

        context.button(0, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&7Inapoi la lista", "&7Click: vezi toti NPC-urile"),
            GuiAction { click -> selectedNpcUuid = null; click.service().open(click.player(), GuiKey.RELATIONSHIP) }
        ))

        context.item(4, GuiItemFactory.item(
            Material.PLAYER_HEAD,
            "&f${npc.name}",
            listOf(
                "&7Ocupatie: &f${npc.occupation ?: "N/A"}",
                "&7Stare: &f${npc.currentState.displayName}"
            )
        ))

        val interactions = plugin.relationshipService.getNPCInteractions(npcUuid)
        context.item(8, GuiItemFactory.item(
            Material.BOOK,
            "&eTotal relatii: ${interactions.size}",
            if (interactions.isEmpty()) listOf("&7Acest NPC nu are relatii.")
            else listOf("&7Cele mai puternice relatii:")
        ))

        val slots = intArrayOf(
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
        )

        for (i in interactions.indices) {
            if (i >= slots.size) break
            val (partnerUuid, rel) = interactions[i]
            val partnerName = plugin.npcManager.getNPCByUuid(partnerUuid)?.name ?: "Unknown"
            val typeColor = when (rel.relationshipType) {
                "close_friend" -> "&a"
                "friend" -> "&e"
                "acquaintance" -> "&7"
                "rival" -> "&c"
                else -> "&8"
            }

            context.button(slots[i], GuiButton.enabled(
                GuiItemFactory.item(
                    Material.PLAYER_HEAD,
                    "&f$partnerName",
                    listOf(
                        "${typeColor}Tip: &f${rel.relationshipType ?: "stranger"}",
                        "&aAfecțiune: ${rel.affection.toInt()} &7| &bÎncredere: ${rel.trust.toInt()}",
                        "&eRespect: ${rel.respect.toInt()} &7| &dFamiliaritate: ${rel.familiarity.toInt()}",
                        "&7Interacțiuni: &f${rel.interactionCount}",
                        "&7Click: vezi detaliile partenerului"
                    )
                ),
                GuiAction { click ->
                    selectedNpcUuid = partnerUuid
                    click.service().open(click.player(), GuiKey.RELATIONSHIP)
                }
            ))
        }

        if (interactions.isEmpty()) {
            context.item(22, GuiItemFactory.item(
                Material.BARRIER, "&7Nu exista relatii",
                listOf("&8NPC-ul nu are relatii cu alte NPC-uri.")
            ))
        }
    }

    companion object {
        private val plugin: ro.ainpc.AINPCPlugin get() = ro.ainpc.AINPCPlugin.getInstance()
    }
}
