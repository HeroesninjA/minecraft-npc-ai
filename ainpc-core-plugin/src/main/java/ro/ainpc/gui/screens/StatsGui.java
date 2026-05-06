package ro.ainpc.gui.screens;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.gui.GuiButton;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.GuiNavigation;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;
import ro.ainpc.npc.AINPC;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class StatsGui implements GuiScreen {

    @Override
    public GuiKey key() {
        return GuiKey.STATS;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Statistici";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        Player player = context.player();
        Location location = player.getLocation();
        List<AINPC> nearbyNpcs = context.plugin().getNpcManager().getNPCsNear(location, 24.0).stream()
            .sorted(Comparator.comparing(npc -> npc.getName().toLowerCase(Locale.ROOT)))
            .toList();

        context.item(4, GuiItemFactory.item(
            Material.CLOCK,
            "&dSnapshot jucator",
            List.of(
                "&7Jucator: &f" + player.getName(),
                "&7World: &f" + location.getWorld().getName(),
                "&7Coordonate: &f" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(),
                "&7Level: &f" + player.getLevel(),
                "&7Health: &f" + String.format(Locale.ROOT, "%.1f", player.getHealth()),
                "&7Food: &f" + player.getFoodLevel()
            )
        ));

        context.item(10, GuiItemFactory.item(
            Material.VILLAGER_SPAWN_EGG,
            "&aNPC-uri",
            List.of(
                "&7Total incarcat: &f" + context.plugin().getNpcManager().getNPCCount(),
                "&7In apropiere: &f" + nearbyNpcs.size(),
                "&7Raza snapshot: &f24 block-uri"
            )
        ));
        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&eQuesturi", "&7Deschide quest log."),
            click -> click.service().open(click.player(), GuiKey.QUEST)
        ));
        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&bWorld", "&7Deschide contextul world."),
            click -> click.service().open(click.player(), GuiKey.WORLD)
        ));

        int slot = 19;
        for (AINPC npc : nearbyNpcs.stream().limit(14).toList()) {
            Location npcLocation = npc.getLocation();
            double distance = npcLocation != null ? npcLocation.distance(location) : -1.0D;
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(Material.PLAYER_HEAD, "&f" + npc.getName(), List.of(
                    "&7Ocupatie: &f" + valueOrUnknown(npc.getOccupation()),
                    "&7Varsta: &f" + npc.getAge(),
                    "&7Spawned: &f" + (npc.isSpawned() ? "da" : "nu"),
                    "&7Emotie: &f" + npc.getEmotions().getDominantEmotion(),
                    "&7Distanta: &f" + (distance >= 0 ? String.format(Locale.ROOT, "%.1f", distance) : "necunoscuta"),
                    "&8Click: /ainpc info"
                )),
                click -> click.service().runCommand(click.player(), "ainpc info " + npc.getName())
            ));
        }

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "necunoscut" : value;
    }
}
