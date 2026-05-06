package ro.ainpc.gui.screens;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.gui.GuiButton;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;
import ro.ainpc.gui.GuiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfirmActionGui implements GuiScreen {

    @Override
    public GuiKey key() {
        return GuiKey.CONFIRM;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Confirmare";
    }

    @Override
    public int size(Player player) {
        return 27;
    }

    @Override
    public void render(GuiRenderContext context) {
        Optional<GuiService.ConfirmRequest> optionalRequest = context.service().getConfirmRequest(context.player());
        if (optionalRequest.isEmpty()) {
            context.item(13, GuiItemFactory.item(Material.BARRIER, "&cConfirmare expirata",
                "&7Actiunea nu mai este disponibila."));
            context.button(18, GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine la hub."),
                click -> click.service().open(click.player(), GuiKey.MAIN)
            ));
            context.fillEmpty(GuiItemFactory.filler());
            return;
        }

        GuiService.ConfirmRequest request = optionalRequest.get();
        List<String> lore = new ArrayList<>(request.warningLines());
        lore.add("&8Comanda: /" + request.command());
        context.item(13, GuiItemFactory.item(Material.REDSTONE_BLOCK, "&c" + request.title(), lore));

        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_CONCRETE, "&aConfirma", "&7Executa actiunea."),
            click -> click.service().runConfirmedCommand(click.player(), request)
        ));
        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.RED_CONCRETE, "&cAnuleaza", "&7Revine fara modificari."),
            click -> click.service().returnFromConfirm(click.player(), request)
        ));
        context.fillEmpty(GuiItemFactory.filler());
    }
}
