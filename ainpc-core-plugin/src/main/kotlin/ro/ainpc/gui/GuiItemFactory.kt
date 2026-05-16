package ro.ainpc.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.ArrayList

object GuiItemFactory {
    private val LEGACY: LegacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand()
    private const val DEFAULT_WRAP_LENGTH = 34

    @JvmStatic
    fun item(material: Material?, name: String?, vararg lore: String?): ItemStack =
        item(material, name, if (lore.isNotEmpty()) java.util.List.of(*lore) else emptyList())

    @JvmStatic
    fun item(material: Material?, name: String?, lore: List<String?>?): ItemStack {
        val stack = ItemStack(material ?: Material.STONE)
        val meta = stack.itemMeta
        if (meta != null) {
            meta.displayName(text(name ?: "&f"))
            meta.lore((lore ?: emptyList()).map { text(it) })
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            stack.itemMeta = meta
        }
        return stack
    }

    @JvmStatic
    fun disabled(material: Material?, name: String?, lore: List<String?>?): ItemStack {
        val disabledLore = ArrayList(lore ?: emptyList())
        disabledLore.add("&cIndisponibil sau fara permisiune.")
        return item(material ?: Material.BARRIER, "&8${stripLegacy(name)}", disabledLore)
    }

    @JvmStatic
    fun filler(): ItemStack = item(Material.GRAY_STAINED_GLASS_PANE, "&8 ", emptyList())

    @JvmStatic
    fun text(value: String?): Component = LEGACY.deserialize(value ?: "")

    @JvmStatic
    fun wrapLore(text: String?, color: String): List<String> = wrapLore(text, color, DEFAULT_WRAP_LENGTH)

    @JvmStatic
    fun wrapLore(text: String?, color: String, maxLineLength: Int): List<String> {
        val clean = stripLegacy(text)
        if (clean.isBlank()) {
            return listOf(color)
        }

        val lines = ArrayList<String>()
        var current = StringBuilder()
        for (token in clean.split(Regex("\\s+"))) {
            if (current.isNotEmpty() && current.length + token.length + 1 > maxLineLength) {
                lines.add(color + current.toString())
                current = StringBuilder()
            }
            if (current.isNotEmpty()) {
                current.append(' ')
            }
            current.append(token)
        }
        if (current.isNotEmpty()) {
            lines.add(color + current.toString())
        }
        return lines
    }

    @JvmStatic
    fun compact(text: String?, maxLength: Int): String {
        val clean = stripLegacy(text)
        if (clean.length <= maxLength) {
            return clean
        }
        return clean.substring(0, maxOf(0, maxLength - 3)).trim() + "..."
    }

    @JvmStatic
    fun stripLegacy(text: String?): String {
        if (text == null) {
            return ""
        }
        return text
            .replace(Regex("(?i)&[0-9A-FK-ORX]"), "")
            .replace(Regex("(?i)\\u00A7[0-9A-FK-ORX]"), "")
    }
}
