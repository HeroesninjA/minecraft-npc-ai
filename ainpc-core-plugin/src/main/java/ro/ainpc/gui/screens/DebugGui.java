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

public class DebugGui implements GuiScreen {

    @Override
    public GuiKey key() {
        return GuiKey.DEBUG;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Debug";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        context.item(4, GuiItemFactory.item(
            Material.SPYGLASS,
            "&9Debug tools",
            List.of(
                "&7Debugdump ramane read-only.",
                "&7Fisierele si rezultatele sunt raportate in chat/consola."
            )
        ));

        dumpButton(context, 10, "all", Material.NETHER_STAR, "&6Debugdump all");
        dumpButton(context, 11, "npc", Material.VILLAGER_SPAWN_EGG, "&eDebugdump NPC");
        dumpButton(context, 12, "world", Material.COMPASS, "&bDebugdump world");
        dumpButton(context, 13, "quest", Material.WRITABLE_BOOK, "&dDebugdump quest");
        dumpButton(context, 14, "openai", Material.ENDER_EYE, "&aDebugdump OpenAI");

        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aTest OpenAI", "&7Ruleaza /ainpc test."),
            click -> click.service().runCommand(click.player(), "ainpc test")
        ));

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private void dumpButton(GuiRenderContext context, int slot, String scope, Material material, String title) {
        context.button(slot, GuiButton.enabled(
            GuiItemFactory.item(material, title, "&7Ruleaza /ainpc debugdump " + scope + "."),
            click -> click.service().runCommand(click.player(), "ainpc debugdump " + scope)
        ));
    }
}
