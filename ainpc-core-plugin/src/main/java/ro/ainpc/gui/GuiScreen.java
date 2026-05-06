package ro.ainpc.gui;

import org.bukkit.entity.Player;

public interface GuiScreen {
    GuiKey key();

    String title(Player player);

    int size(Player player);

    void render(GuiRenderContext context);
}
