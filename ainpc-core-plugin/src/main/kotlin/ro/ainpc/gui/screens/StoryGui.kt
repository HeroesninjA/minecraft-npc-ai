package ro.ainpc.gui.screens

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.story.PlaceStoryState
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.StoryEvent
import ro.ainpc.story.StoryStateService
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class StoryGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.STORY

    override fun title(player: Player): String = "&0AINPC Story"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val snapshot = readSnapshot(context)
        val player = context.player()

        context.item(4, GuiItemFactory.item(Material.AMETHYST_SHARD, "&dStory snapshot", summaryLore(snapshot)))
        context.item(10, GuiItemFactory.item(Material.FILLED_MAP, "&eRegion story", regionLore(snapshot.region, snapshot.regionState)))
        context.item(11, GuiItemFactory.item(Material.OAK_DOOR, "&aPlace story", placeLore(snapshot.place, snapshot.placeState)))
        context.item(12, GuiItemFactory.item(Material.CLOCK, "&bEvenimente recente", eventSummaryLore(snapshot)))

        commandButton(
            context,
            14,
            Material.FILLED_MAP,
            "&eComanda region",
            if (snapshot.region != null) "ainpc story region ${snapshot.region.id()}" else "",
            listOf("&7Afiseaza state-ul persistent pentru regiunea curenta.")
        )
        commandButton(
            context,
            15,
            Material.OAK_DOOR,
            "&aComanda place",
            if (snapshot.place != null) "ainpc story place ${snapshot.place.id()}" else "",
            listOf("&7Afiseaza state-ul persistent pentru place-ul curent.")
        )
        commandButton(
            context,
            16,
            Material.BOOK,
            "&bComanda events",
            storyEventsCommand(snapshot),
            listOf("&7Listeaza ultimele story events pentru scope-ul curent.")
        )
        commandButton(
            context,
            17,
            Material.NETHER_STAR,
            "&dContext nearest",
            "ainpc story context ${player.name} nearest",
            listOf("&7Construieste context story pentru cel mai apropiat NPC.")
        )
        commandButton(
            context,
            31,
            Material.SPYGLASS,
            "&9Debugdump story",
            "ainpc debugdump story",
            listOf("&7Exporta story-states.json si story-events.json.")
        )

        var slot = 19
        for (event in snapshot.events) {
            context.item(slot++, GuiItemFactory.item(eventMaterial(event), "&f${GuiItemFactory.compact(eventTitle(event), 28)}", eventLore(event)))
        }
        while (slot <= 25) {
            context.item(
                slot++,
                GuiItemFactory.item(
                    Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    "&8Fara eveniment",
                    "&7Nu exista event pentru slotul acesta."
                )
            )
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun readSnapshot(context: GuiRenderContext): StorySnapshot {
        val player = context.player()
        val worldAdmin: WorldAdminApi = context.plugin().platform.worldAdmin
        val location: Location = player.location
        val worldName = location.world.name
        val region = worldAdmin.findRegion(worldName, location.blockX, location.blockY, location.blockZ)
        val place = worldAdmin.findPlace(worldName, location.blockX, location.blockY, location.blockZ)

        val storyStateService: StoryStateService? = context.plugin().storyStateService
        if (storyStateService == null) {
            return StorySnapshot(region, place, null, null, emptyList(), "StoryStateService indisponibil.")
        }

        return try {
            val regionState = if (region != null) storyStateService.getRegionState(region.id()).orElse(null) else null
            val placeState = if (place != null) storyStateService.getPlaceState(place.id()).orElse(null) else null
            val events = storyStateService.listRecentEvents(region?.id().orEmpty(), place?.id().orEmpty(), EVENT_LIMIT)
            StorySnapshot(region, place, regionState, placeState, events, "")
        } catch (exception: SQLException) {
            StorySnapshot(region, place, null, null, emptyList(), exception.message.orEmpty())
        }
    }

    private fun summaryLore(snapshot: StorySnapshot): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7Regiune: &f${snapshot.region?.id() ?: "<nemapata>"}")
        lore.add("&7Place: &f${snapshot.place?.id() ?: "<nemapat>"}")
        lore.add("&7Region state: &f${snapshot.regionState?.stateKey() ?: "<nepersistat>"}")
        lore.add("&7Place state: &f${snapshot.placeState?.stateKey() ?: "<nepersistat>"}")
        lore.add("&7Evenimente recente: &f${snapshot.events.size}")
        if (snapshot.error.isNotBlank()) {
            lore.add("&cEroare: &f${GuiItemFactory.compact(snapshot.error, 32)}")
        }
        return lore
    }

    private fun regionLore(region: WorldRegionInfo?, state: RegionStoryState?): List<String> {
        if (region == null) {
            return listOf("&7Nu exista regiune mapata aici.")
        }

        val lore = ArrayList<String>()
        lore.add("&7ID: &f${region.id()}")
        lore.add("&7Nume: &f${region.name()}")
        lore.add("&7Mapping mode: &f${region.storyMode().id}")
        lore.add("&7Mapping state: &f${region.storyStateKey()}")
        lore.add("&7Mapping pool: &f${compactList(region.storyPool(), 26)}")
        if (state == null) {
            lore.add("&7Persistent: &f<nepersistat>")
            return lore
        }
        lore.add("&7Persistent mode: &f${state.storyMode().id}")
        lore.add("&7Persistent state: &f${state.stateKey()}")
        lore.add("&7Updated: &f${formatTime(state.updatedAt())}")
        lore.add("&7Source: &f${valueOrUnknown(state.source())}")
        addMapPreview(lore, "Vars", state.variables(), 2)
        return lore
    }

    private fun placeLore(place: WorldPlaceInfo?, state: PlaceStoryState?): List<String> {
        if (place == null) {
            return listOf("&7Nu exista place mapat aici.")
        }

        val lore = ArrayList<String>()
        lore.add("&7ID: &f${place.id()}")
        lore.add("&7Regiune: &f${place.regionId()}")
        lore.add("&7Nume: &f${place.displayName()}")
        lore.add("&7Tip: &f${place.placeType().id}")
        if (state == null) {
            lore.add("&7Persistent: &f<nepersistat>")
            return lore
        }
        lore.add("&7Persistent state: &f${state.stateKey()}")
        lore.add("&7Updated: &f${formatTime(state.updatedAt())}")
        lore.add("&7Source: &f${valueOrUnknown(state.source())}")
        addMapPreview(lore, "Vars", state.variables(), 3)
        return lore
    }

    private fun eventSummaryLore(snapshot: StorySnapshot): List<String> {
        if (snapshot.error.isNotBlank()) {
            return listOf("&cNu pot citi story events.", "&7${GuiItemFactory.compact(snapshot.error, 34)}")
        }
        if (snapshot.events.isEmpty()) {
            return listOf("&7Nu exista evenimente recente pentru scope-ul curent.")
        }
        val lore = ArrayList<String>()
        lore.add("&7Ultimele evenimente afisate: &f${snapshot.events.size}")
        for (event in snapshot.events.take(4)) {
            lore.add("&8- &f${GuiItemFactory.compact("${event.eventType()} ${event.eventKey()}", 28)}")
        }
        return lore
    }

    private fun eventLore(event: StoryEvent): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7ID: &f${event.id()}")
        lore.add("&7Tip: &f${event.eventType()}")
        lore.add("&7Key: &f${valueOrUnknown(event.eventKey())}")
        lore.add("&7Scope: &f${event.scopeType()}:${event.scopeId()}")
        lore.add("&7Created: &f${formatTime(event.createdAt())}")
        if (event.playerUuid().isNotBlank()) {
            lore.add("&7Player: &f${GuiItemFactory.compact(event.playerUuid(), 18)}")
        }
        if (event.actorType().isNotBlank()) {
            lore.add("&7Actor: &f${event.actorType()}:${GuiItemFactory.compact(event.actorId(), 16)}")
        }
        addPayloadValue(lore, event.payload(), "quest_template")
        addPayloadValue(lore, event.payload(), "quest_code")
        if (event.description().isNotBlank()) {
            lore.addAll(GuiItemFactory.wrapLore(event.description(), "&8", 32))
        }
        return lore
    }

    private fun commandButton(
        context: GuiRenderContext,
        slot: Int,
        material: Material,
        title: String,
        command: String?,
        lore: List<String>
    ) {
        if (command.isNullOrBlank()) {
            context.button(
                slot,
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        title,
                        listOf("&7Nu exista scope mapat pentru aceasta comanda.")
                    )
                )
            )
            return
        }
        val buttonLore = ArrayList(lore)
        buttonLore.add("&8/$command")
        if (!context.player().hasPermission("ainpc.admin")) {
            val disabledLore = ArrayList(buttonLore)
            disabledLore.add("&8Necesita ainpc.admin pentru comanda text.")
            context.button(slot, GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, title, disabledLore)))
            return
        }
        context.button(
            slot,
            GuiButton.enabled(GuiItemFactory.item(material, title, buttonLore)) { click ->
                click.service().runCommand(click.player(), command)
            }
        )
    }

    private fun storyEventsCommand(snapshot: StorySnapshot): String {
        if (snapshot.place != null) {
            return "ainpc story events ${snapshot.place.id()} 10"
        }
        if (snapshot.region != null) {
            return "ainpc story events ${snapshot.region.id()} 10"
        }
        return ""
    }

    private fun eventMaterial(event: StoryEvent): Material {
        val type = event.eventType().lowercase(Locale.ROOT)
        if (type.contains("quest")) {
            return Material.WRITABLE_BOOK
        }
        if (type.contains("complete") || type.contains("ritual")) {
            return Material.AMETHYST_SHARD
        }
        if (type.contains("alert") || type.contains("alarm")) {
            return Material.REDSTONE_TORCH
        }
        return Material.PAPER
    }

    private fun eventTitle(event: StoryEvent): String {
        return if (event.title().isNotBlank()) event.title() else "${event.eventType()} ${valueOrUnknown(event.eventKey())}"
    }

    private fun addPayloadValue(lore: MutableList<String>, payload: Map<String, String>, key: String) {
        val value = payload.getOrDefault(key, "")
        if (value.isNotBlank()) {
            lore.add("&7$key: &f${GuiItemFactory.compact(value, 24)}")
        }
    }

    private fun addMapPreview(lore: MutableList<String>, label: String, values: Map<String, String>?, limit: Int) {
        if (values.isNullOrEmpty()) {
            lore.add("&7$label: &f{}")
            return
        }
        lore.add("&7$label:")
        values.entries.take(limit).forEach { entry ->
            lore.add("&8- &f${GuiItemFactory.compact("${entry.key}=${entry.value}", 30)}")
        }
        if (values.size > limit) {
            lore.add("&8- ... +${values.size - limit}")
        }
    }

    private fun compactList(values: List<String>?, maxLength: Int): String {
        if (values.isNullOrEmpty()) {
            return "[]"
        }
        return GuiItemFactory.compact(values.joinToString(", "), maxLength)
    }

    private fun valueOrUnknown(value: String?): String = if (value.isNullOrBlank()) "<necunoscut>" else value

    private fun formatTime(epochMillis: Long): String {
        if (epochMillis <= 0L) {
            return "<necunoscut>"
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(TIME_FORMAT)
    }

    private data class StorySnapshot(
        val region: WorldRegionInfo?,
        val place: WorldPlaceInfo?,
        val regionState: RegionStoryState?,
        val placeState: PlaceStoryState?,
        val events: List<StoryEvent>,
        val error: String
    ) {
        init {
            requireNotNull(events)
            requireNotNull(error)
        }
    }

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        const val EVENT_LIMIT: Int = 7
    }
}
