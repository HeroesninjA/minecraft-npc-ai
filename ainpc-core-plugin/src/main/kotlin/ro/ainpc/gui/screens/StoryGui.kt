package ro.ainpc.gui.screens

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.debug.DebugDumpStoryText
import ro.ainpc.gui.EnvironmentUi
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.story.PlaceStoryState
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.StoryEvent
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

        context.item(4, GuiItemFactory.item(Material.AMETHYST_SHARD, "&dStory Snapshot", summaryLore(snapshot)))

        val env = context.plugin().environmentEngine.getContext(player.location.world.name)
        context.item(5, GuiItemFactory.item(EnvironmentUi.icon(env), EnvironmentUi.title(env), EnvironmentUi.lore(env)))


        context.item(10, GuiItemFactory.item(Material.FILLED_MAP, "&eRegion story", regionLore(snapshot.region, snapshot.regionState)))
        context.item(11, GuiItemFactory.item(Material.OAK_DOOR, "&aPlace story", placeLore(snapshot.place, snapshot.placeState)))
        context.item(12, GuiItemFactory.item(Material.CLOCK, "&bEvenimente recente", eventSummaryLore(snapshot)))
        context.item(18, GuiItemFactory.item(Material.COMPASS, "&bProgression Verification", progressionVerificationLore(context)))

        val anchorRegionCount = if (snapshot.region != null) {
            val regionId = snapshot.region.id()
            runCatching {
                context.plugin().progressionService.getAnchorBindings(player.uniqueId.toString(), null, 200)
                    .count { it.anchorType() == "region" && it.anchorId().equals(regionId, ignoreCase = true) }
            }.getOrDefault(0)
        } else 0
        val anchorPlaceCount = if (snapshot.place != null) {
            val placeId = snapshot.place.id()
            runCatching {
                context.plugin().progressionService.getAnchorBindings(player.uniqueId.toString(), null, 200)
                    .count { it.anchorType() == "place" && it.anchorId().equals(placeId, ignoreCase = true) }
            }.getOrDefault(0)
        } else 0
        if (anchorRegionCount + anchorPlaceCount > 0) {
            context.item(3, GuiItemFactory.item(
                Material.LIME_DYE,
                "&aQuest Mapping",
                buildList {
                    if (snapshot.region != null) add("&7Ancore regiune: &f$anchorRegionCount")
                    if (snapshot.place != null) add("&7Ancore place: &f$anchorPlaceCount")
                    add("&8Click: deschide Quest Map")
                }
            ))
        }
        context.item(18, GuiItemFactory.item(Material.COMPASS, "&bProgression Verification", progressionVerificationLore(context)))
        context.button(
            13,
            if (context.player().hasPermission("ainpc.admin")) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.PAPER, "&dStory Diagnostics", storyDiagnosticsLore(context.plugin()))
                ) { click -> click.service().runCommand(click.player(), "ainpc debugdump story") }
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Story Diagnostics",
                        storyDiagnosticsLore(context.plugin())
                    )
                )
            }
        )

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

        context.button(6, GuiButton.enabled(
            GuiItemFactory.item(Material.COMMAND_BLOCK, "&6Admin Mapping", "&7Deschide panoul admin mapping.")
        ) { click -> click.service().open(click.player(), GuiKey.ADMIN_MAPPING) })
        context.button(7, GuiButton.enabled(
            GuiItemFactory.item(Material.KNOWLEDGE_BOOK, "&6Admin Quest", "&7Deschide panoul admin quest.")
        ) { click -> click.service().open(click.player(), GuiKey.ADMIN_QUEST) })
        context.button(8, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&aAuthoring Story",
                "&7Creeaza evenimente story visual.")
        ) { click -> click.service().open(click.player(), GuiKey.STORY_AUTHORING) })
        context.button(2, GuiButton.enabled(
            GuiItemFactory.item(Material.FILLED_MAP, "&6Quest Map",
                "&7Leaga obiective de locatii.")
        ) { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) })

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

        val storyStateService = context.plugin().storyStateService
        return try {
            val regionState = if (region != null) storyStateService.getRegionState(region.id()) else null
            val placeState = if (place != null) storyStateService.getPlaceState(place.id()) else null
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

    private fun storyDiagnosticsLore(plugin: AINPCPlugin): List<String> {
        return DebugDumpStoryText.buildStoryText(plugin)
            .lineSequence()
            .filter { it.isNotBlank() }
            .take(6)
            .map { "&7$it" }
            .toList()
    }

    private fun regionLore(region: WorldRegionInfo?, state: RegionStoryState?): List<String> {
        if (region == null) {
            return listOf("&7Nu exista regiune mapata aici.")
        }

        val lore = ArrayList<String>()
        lore.add("&7ID: &f${region.id()}")
        lore.add("&7Nume: &f${region.name()}")
        lore.add("&7Mapping Mode: &f${region.storyMode().id}")
        lore.add("&7Mapping State: &f${region.storyStateKey()}")
        lore.add("&7Mapping Pool: &f${compactList(region.storyPool())}")
        if (state == null) {
            lore.add("&7Persistent: &f<nepersistat>")
            return lore
        }
        lore.add("&7Persistent mode: &f${state.storyMode().id}")
        lore.add("&7Persistent state: &f${state.stateKey()}")
        lore.add("&7Updated: &f${formatTime(state.updatedAt())}")
        lore.add("&7Source: &f${valueOrUnknown(state.source())}")
        addMapPreview(lore, state.variables(), 2)
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
        addMapPreview(lore, state.variables(), 3)
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
        val questEvents = snapshot.events.count { it.eventType().startsWith("quest_") }
        if (questEvents > 0) {
            lore.add("&7Quest Events: &f$questEvents")
        }
        for (event in snapshot.events.take(4)) {
            val label = if (event.eventType().startsWith("quest_")) {
                "${event.eventType()} ${valueOrUnknown(event.payload()["quest_title"] ?: event.eventKey())}"
            } else {
                "${event.eventType()} ${event.eventKey()}"
            }
            lore.add("&8- &f${GuiItemFactory.compact(label, 28)}")
        }
        return lore
    }

    private fun progressionVerificationLore(context: GuiRenderContext): List<String> {
        val player = context.player()
        val storyContext = context.plugin().storyContextService.buildForPlayer(player)
        val progressionSnapshot = context.plugin().progressionService.getProgressionGuiSnapshot(
            player,
            "all",
            player.hasPermission("ainpc.admin")
        )
        val lore = ArrayList<String>()
        lore.add("&7Story Context read-only: &f${!storyContext.isEmpty()}")
        lore.add("&7Story Anchors: &f${storyContext.activeQuestAnchors().size}")
        lore.add("&7Story Warnings: &f${storyContext.warnings().size}")
        lore.add("&7Progression snapshot handled: &f${progressionSnapshot.handled()}")
        lore.add("&7Current progressions: &f${progressionSnapshot.currentEntries().size}")
        lore.add("&7Archived progressions: &f${progressionSnapshot.archivedEntries().size}")
        lore.add("&7Verification mode: &fread-only story + read-only progression")
        lore.add("&8Nu exista cale de scriere in acest card.")
        return lore
    }

    private fun eventLore(event: StoryEvent): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7ID: &f${event.id()}")
        lore.add("&7Tip: &f${event.eventType()}")
        lore.add("&7Key: &f${valueOrUnknown(event.eventKey())}")
        lore.add("&7Scope: &f${event.scopeType()}:${event.scopeId()}")
        lore.add("&7Created: &f${formatTime(event.createdAt())}")
        if (event.eventType().startsWith("quest_")) {
            lore.add("&7Quest: &f${valueOrUnknown(event.payload()["quest_title"])}")
            lore.add("&7Quest Code: &f${valueOrUnknown(event.payload()["quest_code"])}")
            lore.add("&7Quest Status: &f${valueOrUnknown(event.payload()["quest_status"])}")
        }
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
        return event.title().ifBlank { "${event.eventType()} ${valueOrUnknown(event.eventKey())}" }
    }

    private fun addPayloadValue(lore: MutableList<String>, payload: Map<String, String>, key: String) {
        val value = payload.getOrDefault(key, "")
        if (value.isNotBlank()) {
            lore.add("&7$key: &f${GuiItemFactory.compact(value, 24)}")
        }
    }

    private fun addMapPreview(lore: MutableList<String>, values: Map<String, String>?, limit: Int) {
        if (values.isNullOrEmpty()) {
            lore.add("&7Vars: &f{}")
            return
        }
        lore.add("&7Vars:")
        values.entries.take(limit).forEach { entry ->
            lore.add("&8- &f${GuiItemFactory.compact("${entry.key}=${entry.value}", 30)}")
        }
        if (values.size > limit) {
            lore.add("&8- ... +${values.size - limit}")
        }
    }

    private fun compactList(values: List<String>?): String {
        if (values.isNullOrEmpty()) {
            return "[]"
        }
        return GuiItemFactory.compact(values.joinToString(", "), 26)
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
    )

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        const val EVENT_LIMIT: Int = 7
    }
}
