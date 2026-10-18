package ro.ainpc.gui.screens

import com.google.gson.JsonObject
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.debug.DebugDumpStoryStateJson
import ro.ainpc.debug.DebugDumpStoryText
import ro.ainpc.gui.EnvironmentUi
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.progression.StoredProgressionCounts
import ro.ainpc.story.PlaceStoryState
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.story.StoryEvent
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Storygui.
 */
class StoryGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.STORY

    override fun title(player: Player): String = "&0AINPC Story"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        context.item(4, GuiItemFactory.item(Material.AMETHYST_SHARD, "&dStory Snapshot", "&7Se incarca datele story..."))

        val env = context.plugin().environmentEngine.getContext(player.location.world.name)
        context.item(5, GuiItemFactory.item(EnvironmentUi.icon(env), EnvironmentUi.title(env), EnvironmentUi.lore(env)))


        context.item(10, GuiItemFactory.item(Material.FILLED_MAP, "&eRegion story", "&7Se incarca..."))
        context.item(11, GuiItemFactory.item(Material.OAK_DOOR, "&aPlace story", "&7Se incarca..."))
        context.item(12, GuiItemFactory.item(Material.CLOCK, "&bEvenimente recente", "&7Se incarca..."))
        context.item(18, GuiItemFactory.item(Material.COMPASS, "&bProgression Verification", "&7Se verifica progresia..."))
        context.button(
            13,
            if (context.player().hasPermission("ainpc.admin")) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.PAPER, "&dStory Diagnostics", "&7Se incarca diagnosticul...")
                ) { click -> click.service().runCommand(click.player(), "ainpc debugdump story") }
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Story Diagnostics",
                        listOf("&7Permisiune necesara: ainpc.admin")
                    )
                )
            }
        )

        for (slot in 14..16) {
            context.button(slot, GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Se incarca...", listOf("&7Asteapta datele story."))))
        }
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
            listOf("&7Afiseaza diagnosticul story complet in chat.")
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

        for (slot in 19..25) {
            context.item(
                slot,
                GuiItemFactory.item(
                    Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    "&8Se incarca...",
                    "&7Asteapta evenimentele story."
                )
            )
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    override fun afterOpen(context: GuiRenderContext, sessionId: UUID) {
        val player = context.player()
        val plugin = context.plugin()
        val worldAdmin: WorldAdminApi = context.plugin().platform.worldAdmin
        val location: Location = player.location
        val worldName = location.world.name
        val region = worldAdmin.findRegion(worldName, location.blockX, location.blockY, location.blockZ)
        val place = worldAdmin.findPlace(worldName, location.blockX, location.blockY, location.blockZ)
        val playerId = player.uniqueId.toString()
        val future = try {
            plugin.databaseManager.supplyAsync {
                val snapshot = readSnapshot(plugin, region, place)
                val anchorBindings = if (region != null || place != null) {
                    runCatching { plugin.progressionService.getAnchorBindings(playerId, null, 200) }.getOrDefault(emptyList())
                } else emptyList()
                StoryLoaded(
                    snapshot,
                    region?.id()?.let { regionId ->
                        anchorBindings.count { it.anchorType() == "region" && it.anchorId().equals(regionId, ignoreCase = true) }
                    } ?: 0,
                    place?.id()?.let { placeId ->
                        anchorBindings.count { it.anchorType() == "place" && it.anchorId().equals(placeId, ignoreCase = true) }
                    } ?: 0,
                )
            }
        } catch (failure: RuntimeException) {
            CompletableFuture.failedFuture(failure)
        }
        future.whenComplete { loaded, failure ->
            scheduleIfCurrent(context, sessionId) { current ->
                val result = loaded ?: StoryLoaded(
                    StorySnapshot(region, place, null, null, emptyList(), failure?.cause?.message ?: failure?.message.orEmpty()),
                    0,
                    0,
                )
                renderLoaded(current, result)
            }
        }
        loadProgressionCard(context, sessionId, playerId)
        if (player.hasPermission("ainpc.admin")) loadDiagnosticsCard(context, sessionId)
    }

    private fun loadDiagnosticsCard(context: GuiRenderContext, sessionId: UUID) {
        val plugin = context.plugin()
        val activeScenarioCount = plugin.scenarioEngine.getActiveScenarios().size
        val future = try {
            plugin.databaseManager.supplyAsync {
                storyDiagnosticsLore(activeScenarioCount, DebugDumpStoryStateJson.buildStoryStatesJson(plugin))
            }
        } catch (failure: RuntimeException) {
            CompletableFuture.failedFuture(failure)
        }
        future.whenComplete { lore, failure ->
            scheduleIfCurrent(context, sessionId) { current ->
                if (!current.player().hasPermission("ainpc.admin")) {
                    current.button(13, GuiButton.disabled(GuiItemFactory.disabled(
                        Material.GRAY_DYE, "&7Story Diagnostics", listOf("&7Permisiune necesara: ainpc.admin")
                    )))
                    return@scheduleIfCurrent
                }
                val lines = lore ?: listOf(
                    "&cNu pot citi diagnosticul story.",
                    "&7${GuiItemFactory.compact(failure?.cause?.message ?: failure?.message.orEmpty(), 34)}"
                )
                current.button(13, GuiButton.enabled(
                    GuiItemFactory.item(Material.PAPER, "&dStory Diagnostics", lines)
                ) { click -> click.service().runCommand(click.player(), "ainpc debugdump story") })
            }
        }
    }

    private fun loadProgressionCard(context: GuiRenderContext, sessionId: UUID, playerId: String) {
        val plugin = context.plugin()
        val storyFuture = plugin.storyContextService.buildForPlayerAsync(context.player())
        val progressionFuture = try {
            plugin.databaseManager.supplyAsync {
                plugin.progressionService.getStoredProgressionCounts(playerId)
            }
        } catch (failure: RuntimeException) {
            CompletableFuture.failedFuture(failure)
        }
        storyFuture.thenCombine(progressionFuture, ::progressionVerificationLore).whenComplete { lore, failure ->
            scheduleIfCurrent(context, sessionId) { current ->
                val lines = lore ?: listOf(
                    "&cNu pot verifica progresia.",
                    "&7${GuiItemFactory.compact(failure?.cause?.message ?: failure?.message.orEmpty(), 34)}"
                )
                current.item(18, GuiItemFactory.item(Material.COMPASS, "&bProgression Verification", lines))
            }
        }
    }

    private fun scheduleIfCurrent(context: GuiRenderContext, sessionId: UUID, update: (GuiRenderContext) -> Unit) {
        val plugin = context.plugin()
        val task = Runnable { context.service().updateIfCurrent(context.player(), sessionId, key(), update) }
        try {
            if (plugin.server.isPrimaryThread) task.run()
            else plugin.server.scheduler.runTask(plugin, task)
        } catch (scheduleFailure: RuntimeException) {
            plugin.logger.warning("Nu s-a putut actualiza Story GUI: ${scheduleFailure.javaClass.simpleName}")
        }
    }

    private fun readSnapshot(plugin: AINPCPlugin, region: WorldRegionInfo?, place: WorldPlaceInfo?): StorySnapshot {
        val storyStateService = plugin.storyStateService
        return try {
            val regionState = if (region != null) storyStateService.getRegionState(region.id()) else null
            val placeState = if (place != null) storyStateService.getPlaceState(place.id()) else null
            val events = storyStateService.listRecentEvents(region?.id().orEmpty(), place?.id().orEmpty(), EVENT_LIMIT)
            StorySnapshot(region, place, regionState, placeState, events, "")
        } catch (exception: SQLException) {
            StorySnapshot(region, place, null, null, emptyList(), exception.message.orEmpty())
        }
    }

    private fun renderLoaded(context: GuiRenderContext, loaded: StoryLoaded) {
        val snapshot = loaded.snapshot
        context.item(4, GuiItemFactory.item(Material.AMETHYST_SHARD, "&dStory Snapshot", summaryLore(snapshot)))
        context.item(10, GuiItemFactory.item(Material.FILLED_MAP, "&eRegion story", regionLore(snapshot.region, snapshot.regionState)))
        context.item(11, GuiItemFactory.item(Material.OAK_DOOR, "&aPlace story", placeLore(snapshot.place, snapshot.placeState)))
        context.item(12, GuiItemFactory.item(Material.CLOCK, "&bEvenimente recente", eventSummaryLore(snapshot)))
        if (loaded.anchorRegionCount + loaded.anchorPlaceCount > 0) {
            context.item(3, GuiItemFactory.item(Material.LIME_DYE, "&aQuest Mapping", buildList {
                if (snapshot.region != null) add("&7Ancore regiune: &f${loaded.anchorRegionCount}")
                if (snapshot.place != null) add("&7Ancore place: &f${loaded.anchorPlaceCount}")
                add("&8Click: deschide Quest Map")
            }))
        }
        commandButton(context, 14, Material.FILLED_MAP, "&eComanda region",
            snapshot.region?.let { "ainpc story region ${it.id()}" },
            listOf("&7Afiseaza state-ul persistent pentru regiunea curenta."))
        commandButton(context, 15, Material.OAK_DOOR, "&aComanda place",
            snapshot.place?.let { "ainpc story place ${it.id()}" },
            listOf("&7Afiseaza state-ul persistent pentru place-ul curent."))
        commandButton(context, 16, Material.BOOK, "&bComanda events", storyEventsCommand(snapshot),
            listOf("&7Listeaza ultimele story events pentru scope-ul curent."))
        var slot = 19
        for (event in snapshot.events) {
            context.item(slot++, GuiItemFactory.item(eventMaterial(event), "&f${GuiItemFactory.compact(eventTitle(event), 28)}", eventLore(event)))
        }
        while (slot <= 25) {
            context.item(slot++, GuiItemFactory.item(
                Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                "&8Fara eveniment",
                "&7Nu exista event pentru slotul acesta."
            ))
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

    private fun storyDiagnosticsLore(activeScenarioCount: Int, states: JsonObject): List<String> {
        return DebugDumpStoryText.buildStoryStatePreviewText(activeScenarioCount, states)
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

    private fun progressionVerificationLore(
        storyContext: StoryContextSnapshot,
        progressionCounts: StoredProgressionCounts,
    ): List<String> {
        val lore = ArrayList<String>()
        lore.add("&7Story Context read-only: &f${!storyContext.isEmpty()}")
        lore.add("&7Story Anchors: &f${storyContext.activeQuestAnchors().size}")
        lore.add("&7Story Warnings: &f${storyContext.warnings().size}")
        lore.add("&7Stored progressions: &f${progressionCounts.total}")
        lore.add("&7Current progressions: &f${progressionCounts.current}")
        lore.add("&7Archived progressions: &f${progressionCounts.archived}")
        lore.add("&7Verification mode: &fread-only story + persisted progression")
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

    private data class StoryLoaded(
        val snapshot: StorySnapshot,
        val anchorRegionCount: Int,
        val anchorPlaceCount: Int,
    )

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        const val EVENT_LIMIT: Int = 7
    }
}
