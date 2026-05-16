package ro.ainpc.gui.screens;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.gui.GuiButton;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.GuiNavigation;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;

import java.util.List;

public class AuditGui implements GuiScreen {

    @Override
    public GuiKey key() {
        return GuiKey.AUDIT;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Audit";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        context.item(4, GuiItemFactory.item(
            Material.REDSTONE_TORCH,
            "&cAudit operational",
            List.of(
                "&7Click-urile ruleaza comenzile audit existente.",
                "&7Rezultatele apar in chat."
            )
        ));

        auditButton(context, 10, "all", Material.NETHER_STAR, "&6Audit complet");
        auditButton(context, 11, "npc", Material.VILLAGER_SPAWN_EGG, "&eAudit NPC");
        auditButton(context, 12, "world", Material.COMPASS, "&bAudit world");
        auditButton(context, 13, "db", Material.BOOKSHELF, "&aAudit database");
        auditButton(context, 14, "spawn", Material.GRASS_BLOCK, "&2Audit spawn");
        auditButton(context, 15, "quest", Material.WRITABLE_BOOK, "&dAudit quest");

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private void auditButton(GuiRenderContext context, int slot, String mode, Material material, String title) {
        context.button(slot, GuiButton.enabled(
            GuiItemFactory.item(material, title, "&7Ruleaza /ainpc audit " + mode + "."),
            click -> click.service().runCommand(click.player(), "ainpc audit " + mode)
        ));
    }
}
