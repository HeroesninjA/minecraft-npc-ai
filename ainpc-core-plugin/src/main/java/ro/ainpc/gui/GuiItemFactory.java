package ro.ainpc.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class GuiItemFactory {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final int DEFAULT_WRAP_LENGTH = 34;

    private GuiItemFactory() {
    }

    public static ItemStack item(Material material, String name, String... lore) {
        return item(material, name, lore != null ? List.of(lore) : List.of());
    }

    public static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material != null ? material : Material.STONE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(text(name == null ? "&f" : name));
            meta.lore(lore.stream().map(GuiItemFactory::text).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack disabled(Material material, String name, List<String> lore) {
        List<String> disabledLore = new ArrayList<>(lore != null ? lore : List.of());
        disabledLore.add("&cIndisponibil sau fara permisiune.");
        return item(material != null ? material : Material.BARRIER, "&8" + stripLegacy(name), disabledLore);
    }

    public static ItemStack filler() {
        return item(Material.GRAY_STAINED_GLASS_PANE, "&8 ", List.of());
    }

    public static Component text(String value) {
        return LEGACY.deserialize(value == null ? "" : value);
    }

    public static List<String> wrapLore(String text, String color) {
        return wrapLore(text, color, DEFAULT_WRAP_LENGTH);
    }

    public static List<String> wrapLore(String text, String color, int maxLineLength) {
        String clean = stripLegacy(text);
        if (clean.isBlank()) {
            return List.of(color);
        }

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String token : clean.split("\\s+")) {
            if (current.length() > 0 && current.length() + token.length() + 1 > maxLineLength) {
                lines.add(color + current);
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(token);
        }
        if (current.length() > 0) {
            lines.add(color + current);
        }
        return lines;
    }

    public static String compact(String text, int maxLength) {
        String clean = stripLegacy(text);
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    public static String stripLegacy(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replaceAll("(?i)&[0-9A-FK-ORX]", "")
            .replaceAll("(?i)\\u00A7[0-9A-FK-ORX]", "");
    }
}
