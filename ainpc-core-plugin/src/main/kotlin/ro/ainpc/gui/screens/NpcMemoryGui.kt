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
import ro.ainpc.managers.MemoryManager
import ro.ainpc.npc.AINPC
import java.time.format.DateTimeFormatter
import java.util.Locale

class NpcMemoryGui : GuiScreen {
    private var targetNpc: AINPC? = null

    override fun key(): GuiKey = GuiKey.NPC_MEMORY

    override fun title(player: Player): String = "&0Memorii NPC"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val plugin = context.plugin()
        val adminView = player.hasPermission("ainpc.admin")

        context.item(4, GuiItemFactory.item(
            Material.WRITABLE_BOOK,
            "&eMemorii NPC",
            listOf("&7Click pe un NPC pentru a vedea amintirile lui.")
        ))

        if (targetNpc == null) {
            renderNpcSelection(context, plugin)
        } else {
            renderMemoryDetail(context, plugin, player)
        }
    }

    private fun renderNpcSelection(context: GuiRenderContext, plugin: ro.ainpc.AINPCPlugin) {
        val npcs = plugin.npcManager.getAllNPCs()
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .take(36)

        val slots = intArrayOf(
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
        )

        for (i in npcs.indices) {
            if (i >= slots.size) break
            val npc = npcs[i]
            val memCount = if (npc.databaseId > 0) {
                runCatching {
                    val sql = "SELECT COUNT(*) as cnt FROM npc_memories WHERE npc_id = ?"
                    plugin.databaseManager.prepareStatement(sql).use { stmt ->
                        stmt.setInt(1, npc.databaseId)
                        stmt.executeQuery().use { rs -> if (rs.next()) rs.getInt("cnt") else 0 }
                    }
                }.getOrDefault(0)
            } else 0

            context.button(slots[i], GuiButton.enabled(
                GuiItemFactory.item(
                    if (memCount > 0) Material.BOOK else Material.PLAYER_HEAD,
                    "&f${npc.name}",
                    listOf(
                        "&7Amintiri: &f$memCount",
                        "&7Ocupatie: &f${npc.occupation ?: "N/A"}",
                        "&7Click: vezi amintirile"
                    )
                ),
                GuiAction {
                    targetNpc = npc
                    context.service().open(context.player(), GuiKey.NPC_MEMORY)
                }
            ))
        }
    }

    private fun renderMemoryDetail(context: GuiRenderContext, plugin: ro.ainpc.AINPCPlugin, player: Player) {
        val npc = targetNpc ?: return
        val isAdmin = player.hasPermission("ainpc.admin")

        context.button(0, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&7Inapoi la lista NPC",
                "&7Click: vezi toti NPC-urile"),
            GuiAction { targetNpc = null; context.service().open(context.player(), GuiKey.NPC_MEMORY) }
        ))

        val memories = runCatching {
            plugin.memoryManager.getAllMemories(npc, player.uniqueId)
        }.getOrDefault(emptyList())

        context.item(4, GuiItemFactory.item(
            Material.PLAYER_HEAD,
            "&f${npc.name}",
            listOf(
                "&7Total amintiri: &f${memories.size}",
                "&7Tipuri: &f${memories.groupBy { it.getMemoryType() }.entries.joinToString(", ") { "${it.key}(${it.value.size})" }}"
            )
        ))

        val slots = intArrayOf(
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
        )

        for (i in memories.indices) {
            if (i >= slots.size) break
            val mem = memories[i]
            val dateStr = mem.getCreatedAt()?.format(DateTimeFormatter.ofPattern("dd-MMM HH:mm")) ?: "?"
            context.button(slots[i], GuiButton.enabled(
                GuiItemFactory.item(
                    Material.MAP,
                    "&f${mem.getTypeEmoji()} ${mem.getMemoryType()}",
                    listOf(
                        "&7${mem.getContent().take(50)}${if (mem.getContent().length > 50) "..." else ""}",
                        "&7Impact: &f${"%.2f".format(mem.getEmotionalImpact())} &7| Importanta: &f${mem.getImportanceStars()}",
                        "&7Data: &f$dateStr"
                    )
                ),
                GuiAction { click ->
                    if (isAdmin && click.clickType().isShiftClick) {
                        runCatching {
                            val sql = "DELETE FROM npc_memories WHERE id = ?"
                            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                                stmt.setInt(1, mem.getId())
                                stmt.executeUpdate()
                            }
                        }
                        context.service().open(context.player(), GuiKey.NPC_MEMORY)
                    }
                }
            ))
        }

        if (memories.isEmpty()) {
            context.item(22, GuiItemFactory.item(
                Material.BARRIER, "&7Nu exista amintiri",
                listOf("&8Acest NPC nu are amintiri despre tine.")
            ))
        }
    }
}
