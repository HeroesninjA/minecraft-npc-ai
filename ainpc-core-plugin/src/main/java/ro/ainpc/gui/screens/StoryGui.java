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
import ro.ainpc.story.PlaceStoryState;
import ro.ainpc.story.RegionStoryState;
import ro.ainpc.story.StoryEvent;
import ro.ainpc.story.StoryStateService;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StoryGui implements GuiScreen {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int EVENT_LIMIT = 7;

    @Override
    public GuiKey key() {
        return GuiKey.STORY;
    }

    @Override
    public String title(Player player) {
        return "&0AINPC Story";
    }

    @Override
    public int size(Player player) {
        return 54;
    }

    @Override
    public void render(GuiRenderContext context) {
        StorySnapshot snapshot = readSnapshot(context);
        Player player = context.player();

        context.item(4, GuiItemFactory.item(
            Material.AMETHYST_SHARD,
            "&dStory snapshot",
            summaryLore(snapshot)
        ));

        context.item(10, GuiItemFactory.item(
            Material.FILLED_MAP,
            "&eRegion story",
            regionLore(snapshot.region(), snapshot.regionState())
        ));
        context.item(11, GuiItemFactory.item(
            Material.OAK_DOOR,
            "&aPlace story",
            placeLore(snapshot.place(), snapshot.placeState())
        ));
        context.item(12, GuiItemFactory.item(
            Material.CLOCK,
            "&bEvenimente recente",
            eventSummaryLore(snapshot)
        ));

        commandButton(
            context,
            14,
            Material.FILLED_MAP,
            "&eComanda region",
            snapshot.region() != null ? "ainpc story region " + snapshot.region().id() : "",
            List.of("&7Afiseaza state-ul persistent pentru regiunea curenta.")
        );
        commandButton(
            context,
            15,
            Material.OAK_DOOR,
            "&aComanda place",
            snapshot.place() != null ? "ainpc story place " + snapshot.place().id() : "",
            List.of("&7Afiseaza state-ul persistent pentru place-ul curent.")
        );
        commandButton(
            context,
            16,
            Material.BOOK,
            "&bComanda events",
            storyEventsCommand(snapshot),
            List.of("&7Listeaza ultimele story events pentru scope-ul curent.")
        );
        commandButton(
            context,
            17,
            Material.NETHER_STAR,
            "&dContext nearest",
            "ainpc story context " + player.getName() + " nearest",
            List.of("&7Construieste context story pentru cel mai apropiat NPC.")
        );
        commandButton(
            context,
            31,
            Material.SPYGLASS,
            "&9Debugdump story",
            "ainpc debugdump story",
            List.of("&7Exporta story-states.json si story-events.json.")
        );

        int slot = 19;
        for (StoryEvent event : snapshot.events()) {
            context.item(slot++, GuiItemFactory.item(
                eventMaterial(event),
                "&f" + GuiItemFactory.compact(eventTitle(event), 28),
                eventLore(event)
            ));
        }
        while (slot <= 25) {
            context.item(slot++, GuiItemFactory.item(
                Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                "&8Fara eveniment",
                "&7Nu exista event pentru slotul acesta."
            ));
        }

        GuiNavigation.addStandardControls(context, key());
        context.fillEmpty(GuiItemFactory.filler());
    }

    private StorySnapshot readSnapshot(GuiRenderContext context) {
        Player player = context.player();
        WorldAdminApi worldAdmin = context.plugin().getPlatform().getWorldAdmin();
        Location location = player.getLocation();
        String worldName = location.getWorld().getName();
        WorldRegionInfo region = worldAdmin.findRegion(
            worldName,
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
        WorldPlaceInfo place = worldAdmin.findPlace(
            worldName,
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );

        StoryStateService storyStateService = context.plugin().getStoryStateService();
        if (storyStateService == null) {
            return new StorySnapshot(region, place, null, null, List.of(), "StoryStateService indisponibil.");
        }

        try {
            RegionStoryState regionState = region != null
                ? storyStateService.getRegionState(region.id()).orElse(null)
                : null;
            PlaceStoryState placeState = place != null
                ? storyStateService.getPlaceState(place.id()).orElse(null)
                : null;
            List<StoryEvent> events = storyStateService.listRecentEvents(
                region != null ? region.id() : "",
                place != null ? place.id() : "",
                EVENT_LIMIT
            );
            return new StorySnapshot(region, place, regionState, placeState, events, "");
        } catch (SQLException exception) {
            return new StorySnapshot(region, place, null, null, List.of(), exception.getMessage());
        }
    }

    private List<String> summaryLore(StorySnapshot snapshot) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Regiune: &f" + (snapshot.region() != null ? snapshot.region().id() : "<nemapata>"));
        lore.add("&7Place: &f" + (snapshot.place() != null ? snapshot.place().id() : "<nemapat>"));
        lore.add("&7Region state: &f" + (snapshot.regionState() != null ? snapshot.regionState().stateKey() : "<nepersistat>"));
        lore.add("&7Place state: &f" + (snapshot.placeState() != null ? snapshot.placeState().stateKey() : "<nepersistat>"));
        lore.add("&7Evenimente recente: &f" + snapshot.events().size());
        if (!snapshot.error().isBlank()) {
            lore.add("&cEroare: &f" + GuiItemFactory.compact(snapshot.error(), 32));
        }
        return lore;
    }

    private List<String> regionLore(WorldRegionInfo region, RegionStoryState state) {
        if (region == null) {
            return List.of("&7Nu exista regiune mapata aici.");
        }

        List<String> lore = new ArrayList<>();
        lore.add("&7ID: &f" + region.id());
        lore.add("&7Nume: &f" + region.name());
        lore.add("&7Mapping mode: &f" + region.storyMode().getId());
        lore.add("&7Mapping state: &f" + region.storyStateKey());
        lore.add("&7Mapping pool: &f" + compactList(region.storyPool(), 26));
        if (state == null) {
            lore.add("&7Persistent: &f<nepersistat>");
            return lore;
        }
        lore.add("&7Persistent mode: &f" + state.storyMode().getId());
        lore.add("&7Persistent state: &f" + state.stateKey());
        lore.add("&7Updated: &f" + formatTime(state.updatedAt()));
        lore.add("&7Source: &f" + valueOrUnknown(state.source()));
        addMapPreview(lore, "Vars", state.variables(), 2);
        return lore;
    }

    private List<String> placeLore(WorldPlaceInfo place, PlaceStoryState state) {
        if (place == null) {
            return List.of("&7Nu exista place mapat aici.");
        }

        List<String> lore = new ArrayList<>();
        lore.add("&7ID: &f" + place.id());
        lore.add("&7Regiune: &f" + place.regionId());
        lore.add("&7Nume: &f" + place.displayName());
        lore.add("&7Tip: &f" + place.placeType().getId());
        if (state == null) {
            lore.add("&7Persistent: &f<nepersistat>");
            return lore;
        }
        lore.add("&7Persistent state: &f" + state.stateKey());
        lore.add("&7Updated: &f" + formatTime(state.updatedAt()));
        lore.add("&7Source: &f" + valueOrUnknown(state.source()));
        addMapPreview(lore, "Vars", state.variables(), 3);
        return lore;
    }

    private List<String> eventSummaryLore(StorySnapshot snapshot) {
        if (!snapshot.error().isBlank()) {
            return List.of("&cNu pot citi story events.", "&7" + GuiItemFactory.compact(snapshot.error(), 34));
        }
        if (snapshot.events().isEmpty()) {
            return List.of("&7Nu exista evenimente recente pentru scope-ul curent.");
        }
        List<String> lore = new ArrayList<>();
        lore.add("&7Ultimele evenimente afisate: &f" + snapshot.events().size());
        for (StoryEvent event : snapshot.events().stream().limit(4).toList()) {
            lore.add("&8- &f" + GuiItemFactory.compact(event.eventType() + " " + event.eventKey(), 28));
        }
        return lore;
    }

    private List<String> eventLore(StoryEvent event) {
        List<String> lore = new ArrayList<>();
        lore.add("&7ID: &f" + event.id());
        lore.add("&7Tip: &f" + event.eventType());
        lore.add("&7Key: &f" + valueOrUnknown(event.eventKey()));
        lore.add("&7Scope: &f" + event.scopeType() + ":" + event.scopeId());
        lore.add("&7Created: &f" + formatTime(event.createdAt()));
        if (!event.playerUuid().isBlank()) {
            lore.add("&7Player: &f" + GuiItemFactory.compact(event.playerUuid(), 18));
        }
        if (!event.actorType().isBlank()) {
            lore.add("&7Actor: &f" + event.actorType() + ":" + GuiItemFactory.compact(event.actorId(), 16));
        }
        addPayloadValue(lore, event.payload(), "quest_template");
        addPayloadValue(lore, event.payload(), "quest_code");
        if (!event.description().isBlank()) {
            lore.addAll(GuiItemFactory.wrapLore(event.description(), "&8", 32));
        }
        return lore;
    }

    private void commandButton(GuiRenderContext context,
                               int slot,
                               Material material,
                               String title,
                               String command,
                               List<String> lore) {
        if (command == null || command.isBlank()) {
            context.button(slot, GuiButton.disabled(GuiItemFactory.disabled(
                Material.GRAY_DYE,
                title,
                List.of("&7Nu exista scope mapat pentru aceasta comanda.")
            )));
            return;
        }
        List<String> buttonLore = new ArrayList<>(lore);
        buttonLore.add("&8/" + command);
        if (!context.player().hasPermission("ainpc.admin")) {
            List<String> disabledLore = new ArrayList<>(buttonLore);
            disabledLore.add("&8Necesita ainpc.admin pentru comanda text.");
            context.button(slot, GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, title, disabledLore)));
            return;
        }
        context.button(slot, GuiButton.enabled(
            GuiItemFactory.item(material, title, buttonLore),
            click -> click.service().runCommand(click.player(), command)
        ));
    }

    private String storyEventsCommand(StorySnapshot snapshot) {
        if (snapshot.place() != null) {
            return "ainpc story events " + snapshot.place().id() + " 10";
        }
        if (snapshot.region() != null) {
            return "ainpc story events " + snapshot.region().id() + " 10";
        }
        return "";
    }

    private Material eventMaterial(StoryEvent event) {
        String type = event.eventType().toLowerCase();
        if (type.contains("quest")) {
            return Material.WRITABLE_BOOK;
        }
        if (type.contains("complete") || type.contains("ritual")) {
            return Material.AMETHYST_SHARD;
        }
        if (type.contains("alert") || type.contains("alarm")) {
            return Material.REDSTONE_TORCH;
        }
        return Material.PAPER;
    }

    private String eventTitle(StoryEvent event) {
        return !event.title().isBlank()
            ? event.title()
            : event.eventType() + " " + valueOrUnknown(event.eventKey());
    }

    private void addPayloadValue(List<String> lore, Map<String, String> payload, String key) {
        String value = payload.getOrDefault(key, "");
        if (!value.isBlank()) {
            lore.add("&7" + key + ": &f" + GuiItemFactory.compact(value, 24));
        }
    }

    private void addMapPreview(List<String> lore, String label, Map<String, String> values, int limit) {
        if (values == null || values.isEmpty()) {
            lore.add("&7" + label + ": &f{}");
            return;
        }
        lore.add("&7" + label + ":");
        values.entrySet().stream()
            .limit(limit)
            .forEach(entry -> lore.add("&8- &f" + GuiItemFactory.compact(entry.getKey() + "=" + entry.getValue(), 30)));
        if (values.size() > limit) {
            lore.add("&8- ... +" + (values.size() - limit));
        }
    }

    private String compactList(List<String> values, int maxLength) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return GuiItemFactory.compact(String.join(", ", values), maxLength);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "<necunoscut>" : value;
    }

    private String formatTime(long epochMillis) {
        if (epochMillis <= 0L) {
            return "<necunoscut>";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
            .format(TIME_FORMAT);
    }

    private record StorySnapshot(
        WorldRegionInfo region,
        WorldPlaceInfo place,
        RegionStoryState regionState,
        PlaceStoryState placeState,
        List<StoryEvent> events,
        String error
    ) {
        private StorySnapshot {
            events = List.copyOf(events != null ? events : List.of());
            error = error != null ? error : "";
        }
    }
}
