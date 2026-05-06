package ro.ainpc.gui;

import org.bukkit.Material;

public final class GuiNavigation {

    private GuiNavigation() {
    }

    public static void addStandardControls(GuiRenderContext context, GuiKey refreshKey) {
        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la hub-ul principal."),
            click -> click.service().open(click.player(), GuiKey.MAIN)
        ));
        context.button(49, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca ecranul curent."),
            click -> click.service().open(click.player(), refreshKey)
        ));
        context.button(53, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&cInchide", "&7Inchide interfata."),
            click -> click.player().closeInventory()
        ));
    }
}
