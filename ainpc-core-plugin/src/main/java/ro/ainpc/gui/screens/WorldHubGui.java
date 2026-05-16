package ro.ainpc.gui.screens;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.gui.GuiButton;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.gui.GuiNavigation;
import ro.ainpc.gui.GuiRenderContext;
import ro.ainpc.gui.GuiScreen;
import ro.ainpc.progression.ProgressionAnchorBinding;
import ro.ainpc.progression.ProgressionGuiEntry;
import ro.ainpc.progression.ProgressionGuiSnapshot;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class WorldHubGui implements GuiScreen {

    @Override
    public GuiKey key() {
        return GuiKey.WORLD;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC World";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        Player player = context.player();
        boolean adminView = player.hasPermission("ainpc.admin");
        WorldAdminApi worldAdmin = context.plugin().getPlatform().getWorldAdmin();
        Location location = player.getLocation();
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        ProgressionGuiSnapshot progressionSnapshot =
            context.plugin().getProgressionService().getProgressionGuiSnapshot(player, "all", adminView);

        WorldRegionInfo region = worldAdmin.findRegion(worldName, x, y, z);
        WorldPlaceInfo place = worldAdmin.findPlace(worldName, x, y, z);
        WorldNodeInfo node = worldAdmin.findNode(worldName, x, y, z);
        List<ProgressionAnchorBinding> localAnchorBindings =
            localAnchorBindings(context, player, adminView, region, place, node);
        List<WorldNodeInfo> nearbyNodes = worldAdmin.findNodesNear(worldName, location.getX(), location.getY(), location.getZ(), 24.0, 7)
            .stream()
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .toList();

        context.item(4, GuiItemFactory.item(
            Material.COMPASS,
            "&bWorld context",
            List.of(
                "&7Coordonate: &f" + worldName + " " + x + ", " + y + ", " + z,
                "&7Mapping: &f" + worldAdmin.getRegionCount() + " regiuni / "
                    + worldAdmin.getPlaceCount() + " places / " + worldAdmin.getNodeCount() + " noduri",
                "&7Progresii vizibile: &f" + progressionSnapshot.allEntries().size(),
                "&7Ancore locale: &f" + localAnchorBindings.size()
            )
        ));

        context.item(10, GuiItemFactory.item(Material.FILLED_MAP, "&eRegiune curenta", regionLore(region)));
        context.item(11, GuiItemFactory.item(Material.OAK_DOOR, "&aPlace curent", placeLore(place)));
        context.item(12, GuiItemFactory.item(Material.TARGET, "&dNode curent", nodeLore(node)));
        context.button(13, context.service().canOpen(player, GuiKey.QUEST)
            ? GuiButton.enabled(
                GuiItemFactory.item(Material.WRITABLE_BOOK, "&eProgresii active",
                    progressionLore(progressionSnapshot)),
                click -> click.service().openQuestLog(click.player(),
                    click.clickType().isRightClick() ? "all" : "active")
            )
            : GuiButton.disabled(GuiItemFactory.disabled(Material.BARRIER, "&7Progresii active",
                List.of("&7Nu ai acces la GUI-ul de progresii.")))
        );
        context.button(14, adminView
            ? GuiButton.enabled(
                GuiItemFactory.item(Material.MAP, "&6Ancore progresii",
                    anchorLore(localAnchorBindings, true)),
                click -> click.service().runCommand(click.player(), "ainpc quest anchors all")
            )
            : GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Ancore progresii",
                anchorLore(localAnchorBindings, false)))
        );

        int slot = 19;
        for (WorldNodeInfo nearbyNode : nearbyNodes) {
            context.item(slot++, GuiItemFactory.item(Material.LODESTONE, "&f" + nearbyNode.id(), nodeLore(nearbyNode)));
        }

        context.button(28, GuiButton.enabled(
            GuiItemFactory.item(Material.ENDER_EYE, "&bWhere am I", "&7Ruleaza /ainpc world whereami."),
            click -> click.service().runCommand(click.player(), "ainpc world whereami")
        ));
        context.button(29, context.service().canOpen(player, GuiKey.STORY)
            ? GuiButton.enabled(
                GuiItemFactory.item(Material.AMETHYST_SHARD, "&dStory",
                    "&7Deschide snapshot-ul story pentru regiunea si place-ul curent."),
                click -> click.service().open(click.player(), GuiKey.STORY)
            )
            : GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Story",
                List.of("&7Nu ai acces la GUI-ul story.")))
        );

        if (adminView) {
            context.button(30, GuiButton.enabled(
                GuiItemFactory.item(Material.SPYGLASS, "&6Scan sat",
                    "&7Cere confirmare pentru scan vanilla pe raza 48."),
                click -> confirmWorldCommand(
                    click.player(),
                    click.service(),
                    "Scan sat",
                    "ainpc world scan village 48",
                    List.of(
                        "&7Raza: &f48 block-uri",
                        "&7Poate importa sau actualiza semantic mapping pentru zona curenta.",
                        "&7Verifica rezultatul prin audit/debugdump dupa rulare."
                    )
                )
            ));
            context.button(31, GuiButton.enabled(
                GuiItemFactory.item(Material.GRASS_BLOCK, "&6Demo mapping",
                    "&7Cere confirmare inainte de creare mapping demo."),
                click -> confirmWorldCommand(
                    click.player(),
                    click.service(),
                    "Creeaza mapping demo",
                    "ainpc world demo create",
                    List.of(
                        "&7Tinta: &fzona curenta",
                        "&7Creeaza regiuni, places sau noduri demo in world mapping.",
                        "&cFoloseste doar pe lumi de test sau dupa backup."
                    )
                )
            ));
            context.button(32, GuiButton.enabled(
                GuiItemFactory.item(Material.WRITABLE_BOOK, "&aSalveaza mapping",
                    "&7Cere confirmare inainte de persistenta mapping."),
                click -> confirmWorldCommand(
                    click.player(),
                    click.service(),
                    "Salveaza world mapping",
                    "ainpc world save",
                    List.of(
                        "&7Persistenta mapping-ului curent.",
                        "&7Dupa confirmare, comenzile text raporteaza rezultatul in chat."
                    )
                )
            ));
        }

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private List<String> regionLore(WorldRegionInfo region) {
        if (region == null) {
            return List.of("&7Nicio regiune mapata aici.");
        }
        return List.of(
            "&7ID: &f" + region.id(),
            "&7Nume: &f" + region.name(),
            "&7Tip: &f" + region.typeId(),
            "&7Story state: &f" + region.storyStateKey(),
            "&7Tags: &f" + String.join(", ", region.tags())
        );
    }

    private List<String> placeLore(WorldPlaceInfo place) {
        if (place == null) {
            return List.of("&7Niciun place mapat aici.");
        }
        return List.of(
            "&7ID: &f" + place.id(),
            "&7Regiune: &f" + place.regionId(),
            "&7Nume: &f" + place.displayName(),
            "&7Tip: &f" + place.placeType().getId(),
            "&7Access: &f" + (place.publicAccess() ? "public" : "restrictionat")
        );
    }

    private List<String> nodeLore(WorldNodeInfo node) {
        if (node == null) {
            return List.of("&7Niciun node activ aici.");
        }
        List<String> lore = new ArrayList<>();
        lore.add("&7ID: &f" + node.id());
        lore.add("&7Regiune: &f" + node.regionId());
        if (!node.placeId().isBlank()) {
            lore.add("&7Place: &f" + node.placeId());
        }
        lore.add("&7Tip: &f" + node.typeId());
        lore.add("&7Raza: &f" + String.format(Locale.ROOT, "%.1f", node.radius()));
        return lore;
    }

    private List<String> progressionLore(ProgressionGuiSnapshot snapshot) {
        if (snapshot == null || !snapshot.handled()) {
            return List.of(
                "&7Runtime-ul de progresie nu a returnat snapshot.",
                "&8Click: incearca log activ",
                "&8Right click: toate progresiile"
            );
        }

        List<ProgressionGuiEntry> currentEntries = snapshot.currentEntries();
        long activeCount = currentEntries.stream().filter(ProgressionGuiEntry::active).count();
        long offeredCount = currentEntries.stream().filter(ProgressionGuiEntry::offered).count();
        ProgressionGuiEntry trackedEntry = currentEntries.stream()
            .filter(ProgressionGuiEntry::tracked)
            .findFirst()
            .orElse(null);

        List<String> lore = new ArrayList<>();
        lore.add("&7Filtru snapshot: &f" + valueOrUnknown(snapshot.filterLabel()));
        lore.add("&7Curente: &f" + currentEntries.size()
            + " &8(active " + activeCount + ", offered " + offeredCount + ")");
        lore.add("&7Arhivate vizibile: &f" + snapshot.archivedEntries().size());
        lore.add(trackedEntry != null
            ? "&7Tracking: &f" + GuiItemFactory.compact(trackedEntry.title(), 28)
            : "&7Tracking: &8niciuna vizibila");
        lore.add("&8Click: progresii active");
        lore.add("&8Right click: toate progresiile");
        return lore;
    }

    private List<ProgressionAnchorBinding> localAnchorBindings(GuiRenderContext context,
                                                               Player player,
                                                               boolean adminView,
                                                               WorldRegionInfo region,
                                                               WorldPlaceInfo place,
                                                               WorldNodeInfo node) {
        try {
            String playerUuid = adminView ? "" : player.getUniqueId().toString();
            List<ProgressionAnchorBinding> rows = new ArrayList<>();
            addAnchorBindings(context, rows, playerUuid, "node", node != null ? node.id() : "");
            addAnchorBindings(context, rows, playerUuid, "place", place != null ? place.id() : "");
            addAnchorBindings(context, rows, playerUuid, "region", region != null ? region.id() : "");
            return rows.stream()
                .distinct()
                .limit(12)
                .toList();
        } catch (SQLException exception) {
            context.plugin().getLogger().warning("Nu pot incarca ancorele locale pentru World GUI: " + exception.getMessage());
            return List.of();
        }
    }

    private void addAnchorBindings(GuiRenderContext context,
                                   List<ProgressionAnchorBinding> rows,
                                   String playerUuid,
                                   String anchorType,
                                   String anchorId) throws SQLException {
        if (anchorId == null || anchorId.isBlank()) {
            return;
        }
        rows.addAll(context.plugin().getProgressionService()
            .getAnchorBindingsForAnchor(playerUuid, anchorType, anchorId, 6));
    }

    private List<String> anchorLore(List<ProgressionAnchorBinding> rows, boolean adminView) {
        List<ProgressionAnchorBinding> safeRows = rows != null ? rows : List.of();
        List<String> lore = new ArrayList<>();
        lore.add("&7Potriviri pentru regiune/place/node: &f" + safeRows.size());
        if (safeRows.isEmpty()) {
            lore.add("&8Nu exista ancore persistate pentru contextul curent.");
        } else {
            safeRows.stream().limit(5).forEach(row -> lore.add("&7- &f"
                + GuiItemFactory.compact(row.templateId(), 18)
                + " &8" + row.anchorSelector()
                + " &7" + valueOrUnknown(row.status())));
        }
        lore.add(adminView
            ? "&8Click: toate ancorele persistate"
            : "&8Necesita admin pentru lista completa.");
        return lore;
    }

    private void confirmWorldCommand(Player player,
                                     ro.ainpc.gui.GuiService service,
                                     String title,
                                     String command,
                                     List<String> warningLines) {
        service.openConfirmCommand(player, title, command, GuiKey.WORLD, "", warningLines);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "necunoscut" : value;
    }
}
