package ro.ainpc.gui.screens;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.GuiNavigation;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;

import java.util.List;

public class PlaceholderGui implements GuiScreen {

    private final GuiKey key;
    private final String title;
    private final String message;

    public PlaceholderGui(GuiKey key, String title, String message) {
        this.key = key;
        this.title = title;
        this.message = message;
    }

    @Override
    public GuiKey key() {
        return key;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC " + title;
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        context.item(22, GuiItemFactory.item(
            Material.CHEST,
            "&e" + title,
            List.of(
                "&7" + message,
                "&7Ecranul este conectat la hub si gata pentru provider dedicat."
            )
        ));
        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }
}
