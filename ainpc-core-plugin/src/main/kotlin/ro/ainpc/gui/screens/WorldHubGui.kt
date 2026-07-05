package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.debug.DebugDumpMappingText
import ro.ainpc.gui.EnvironmentUi
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.gui.GuiService
import ro.ainpc.progression.ProgressionAnchorBinding
import ro.ainpc.progression.ProgressionFormatUtil
import ro.ainpc.progression.ProgressionGuiSnapshot
import ro.ainpc.world.RegionIdentityProvider
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.sql.SQLException
import java.util.Locale

class WorldHubGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.WORLD

    override fun title(player: Player): String = "&0AINPC World"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val adminView = player.hasPermission("ainpc.admin")
        val worldAdmin: WorldAdminApi = context.plugin().platform.worldAdmin
        val location = player.location
        val worldName = location.world.name
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ
        val progressionSnapshot =
            context.plugin().progressionService.getProgressionGuiSnapshot(player, "all", adminView)

        val region = worldAdmin.findRegion(worldName, x, y, z)
        val place = worldAdmin.findPlace(worldName, x, y, z)
        val node = worldAdmin.findNode(worldName, x, y, z)
        val localAnchorBindings = localAnchorBindings(context, player, adminView, region, place, node)
        val nearbyNodes = worldAdmin.findNodesNear(worldName, location.x, location.y, location.z, 24.0, 7)
            .sortedBy { it.id() }

        context.item(
            4,
            GuiItemFactory.item(
                Material.COMPASS,
                "&bWorld Context",
                buildWorldStatusLines(
                    worldName,
                    x,
                    y,
                    z,
                    worldAdmin.regionCount,
                    worldAdmin.placeCount,
                    worldAdmin.nodeCount,
                    progressionSnapshot.allEntries().size,
                    localAnchorBindings.size,
                    nearbyNodes.size,
                    worldAdmin.hasUnsavedChanges(),
                    adminView
                )
            )
        )
        context.button(
            5,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.PAPER, "&dMapping Snapshot", mappingSnapshotLore(worldAdmin, region, place, node, nearbyNodes.size)),
                    action = { click -> click.service().runCommand(click.player(), "ainpc debugdump mapping") }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Mapping Snapshot",
                        mappingSnapshotLore(worldAdmin, region, place, node, nearbyNodes.size)
                    )
                )
            }
        )

        val env = context.plugin().environmentEngine.getContext(worldName)
        context.item(7, GuiItemFactory.item(EnvironmentUi.icon(env), EnvironmentUi.title(env), EnvironmentUi.lore(env)))

        context.button(10, if (region != null) {
            GuiButton.enabled(
                GuiItemFactory.item(Material.FILLED_MAP, "&eRegiune Curenta", regionLore(region)),
                action = { click -> click.service().openRegionDetail(click.player(), region.id()) }
            )
        } else {
            GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Regiune", regionLore(null)))
        })
        context.item(6, GuiItemFactory.item(
            Material.AMETHYST_SHARD,
            "&dIdentitatea regiunii",
            if (region != null) RegionIdentityProvider.summaryLore(
                ro.ainpc.world.RegionType.fromId(region.typeId())
            ) else listOf("&7Nu Exista Regiune Aici.")
        ))
        context.button(11, if (place != null) {
            GuiButton.enabled(
                GuiItemFactory.item(Material.OAK_DOOR, "&aPlace Curent", placeLore(place)),
                action = { click -> click.service().openPlaceDetail(click.player(), place.id()) }
            )
        } else {
            GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Place Curent", placeLore(null)))
        })
        context.item(12, GuiItemFactory.item(Material.TARGET, "&dNode Curent", nodeLore(node)))
        if (place != null) {
            val placeProgressions = runCatching {
                context.plugin().progressionService.getProgressionsByAnchor("place", place.id(), 3)
            }.getOrDefault(emptyList())
            if (placeProgressions.isNotEmpty()) {
                context.item(
                    16,
                    GuiItemFactory.item(
                        Material.LIME_DYE,
                        "&aProgresii Pe Acest Place",
                        placeProgressions.map { "&7- &f${ProgressionFormatUtil.formatOptional(it.progressionId())} &7(${ProgressionFormatUtil.formatOptional(it.status())})" }
                    )
                )
            }
        }
        if (node != null) {
            val nodeProgressions = runCatching {
                context.plugin().progressionService.getProgressionsByAnchor("node", node.id(), 3)
            }.getOrDefault(emptyList())
            if (nodeProgressions.isNotEmpty()) {
                context.item(
                    17,
                    GuiItemFactory.item(
                        Material.LIME_DYE,
                        "&aProgresii Pe Acest Node",
                        nodeProgressions.map { "&7- &f${ProgressionFormatUtil.formatOptional(it.progressionId())} &7(${ProgressionFormatUtil.formatOptional(it.status())})" }
                    )
                )
            }
        }
        context.button(
            13,
            if (context.service().canOpen(player, GuiKey.QUEST)) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.WRITABLE_BOOK, "&eProgresii active", progressionLore(progressionSnapshot)),
                    action = { click ->
                        click.service().openQuestLog(
                            click.player(),
                            if (click.clickType().isRightClick) "all" else "active"
                        )
                    }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.BARRIER,
                        "&7Progresii active",
                        listOf("&7Nu Ai Acces La GUI-ul De Progresii.")
                    )
                )
            }
        )
        context.button(
            14,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.MAP, "&6Ancore progresii", anchorLore(localAnchorBindings, true)),
                    action = { click -> click.service().runCommand(click.player(), "ainpc quest anchors all") }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Ancore Progresii",
                        anchorLore(localAnchorBindings, false)
                    )
                )
            }
        )
        context.button(
            15,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.PAPER, "&eMapping Diagnostics", mappingLore(context.plugin())),
                    action = { click -> click.service().runCommand(click.player(), "ainpc debugdump mapping") }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Mapping Diagnostics",
                        mappingLore(context.plugin())
                    )
                )
            }
        )

        var slot = 19
        for (nearbyNode in nearbyNodes) {
            context.item(slot++, GuiItemFactory.item(Material.LODESTONE, "&f${nearbyNode.id()}", nodeLore(nearbyNode)))
        }

        context.button(
            28,
            GuiButton.enabled(
                GuiItemFactory.item(Material.ENDER_EYE, "&bWhere Am I", "&7Ruleaza /ainpc world whereami.", "&8Actiuni principale."),
                action = { click -> click.service().runCommand(click.player(), "ainpc world whereami") }
            )
        )
        context.button(
            29,
            if (context.service().canOpen(player, GuiKey.STORY)) {
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.AMETHYST_SHARD,
                        "&dStory",
                        "&7Deschide Snapshot-ul Story Pentru Regiunea Si Place-ul Curent."
                    ),
                    action = { click -> click.service().open(click.player(), GuiKey.STORY) }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Story",
                        listOf("&7Nu Ai Acces La GUI-ul Story.")
                    )
                )
            }
        )

        context.button(
            18,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.COMMAND_BLOCK, "&6Admin Mapping",
                        listOf("&7Gestioneaza Regiuni, Places Si Noduri.", "&7Click: Deschide Panoul Admin.", "&8Actiune Admin Separata.")),
                    action = { click -> click.service().open(click.player(), GuiKey.ADMIN_MAPPING) }
                )
            } else {
                GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Admin Mapping",
                    listOf("&7Necesita Admin.")))
            }
        )
        context.button(
            33,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.KNOWLEDGE_BOOK, "&6Admin Quest",
                        listOf("&7Gestioneaza Definitii, Ancore Si Progresii.", "&7Click: Deschide Panoul Admin Quest.", "&8Actiune Admin Separata.")),
                    action = { click -> click.service().open(click.player(), GuiKey.ADMIN_QUEST) }
                )
            } else {
                GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Admin Quest",
                    listOf("&7Necesita Admin.")))
            }
        )
        if (adminView) {
            context.button(
                30,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.SPYGLASS,
                        "&6Scan sat",
                        "&7Cere Confirmare Pentru Scan Vanilla Pe Raza 48."
                    ),
                    action = { click ->
                        confirmWorldCommand(
                            click.player(),
                            click.service(),
                            "Scan sat",
                            "ainpc world scan village 48",
                            listOf(
                                "&7Raza: &f48 block-uri",
                                "&7Poate Importa Sau Actualiza Semantic Mapping Pentru Zona Curenta.",
                                "&7Verifica Rezultatul Prin Audit/Debugdump Dupa Rulare."
                            )
                        )
                    }
                )
            )
            context.button(
                31,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.GRASS_BLOCK,
                        "&6Demo Mapping",
                        "&7Cere Confirmare Inainte De Creare Mapping Demo."
                    ),
                    action = { click ->
                        confirmWorldCommand(
                            click.player(),
                            click.service(),
                            "Creeaza Mapping Demo",
                            "ainpc world demo create",
                            listOf(
                                "&7Tinta: &fzona curenta",
                                "&7Creeaza Regiuni, Places Sau Noduri Demo In World Mapping.",
                                "&cFoloseste Doar Pe Lumi De Test Sau Dupa Backup."
                            )
                        )
                    }
                )
            )
            context.button(
                32,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.WRITABLE_BOOK,
                        "&aSalveaza Mapping",
                        "&7Cere Confirmare Inainte De Persistenta Mapping."
                    ),
                    action = { click ->
                        confirmWorldCommand(
                            click.player(),
                            click.service(),
                            "Salveaza World Mapping",
                            "ainpc world save",
                            listOf(
                                "&7Persistenta Mapping-ului Curent.",
                                "&7Dupa Confirmare, Comenzile Text Raporteaza Rezultatul In Chat."
                            )
                        )
                    }
                )
            )
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun regionLore(region: WorldRegionInfo?): List<String> {
        if (region == null) {
            return listOf("&7Nicio regiune mapata aici.")
        }
        val type = ro.ainpc.world.RegionType.fromId(region.typeId())
        val identity = RegionIdentityProvider.identity(type)
        return listOf(
            "&7ID: &f${region.id()}",
            "&7Nume: &f${region.name()}",
            "&7Tip: &f${identity.displayName} &8(${type.id})",
            "&7Identitate: &f${identity.description}",
            "&7Poveste: &f${identity.defaultStoryKey} &8(${identity.mood})",
            "&7Threat: &f${identity.threatLevel}",
            "&7Atmosfera: &f${identity.ambiance}",
            "&7Tags: &f${region.tags().joinToString(", ")}"
        )
    }

    private fun placeLore(place: WorldPlaceInfo?): List<String> {
        if (place == null) {
            return listOf("&7Niciun Place Mapat Aici.")
        }
        return listOf(
            "&7ID: &f${place.id()}",
            "&7Regiune: &f${place.regionId()}",
            "&7Nume: &f${place.displayName()}",
            "&7Tip: &f${place.placeType().id}",
            "&7Acces: &f${if (place.publicAccess()) "public" else "restrictionat"}"
        )
    }

    private fun nodeLore(node: WorldNodeInfo?): List<String> {
        if (node == null) {
            return listOf("&7Niciun node activ aici.")
        }
        val lore = mutableListOf<String>()
        lore.add("&7ID: &f${node.id()}")
        lore.add("&7Regiune: &f${node.regionId()}")
        if (node.placeId().isNotBlank()) {
            lore.add("&7Place: &f${node.placeId()}")
        }
        lore.add("&7Tip: &f${node.typeId()}")
        lore.add("&7Raza: &f${String.format(Locale.ROOT, "%.1f", node.radius())}")
        return lore
    }

    private fun mappingSnapshotLore(
        worldAdmin: WorldAdminApi,
        region: WorldRegionInfo?,
        place: WorldPlaceInfo?,
        node: WorldNodeInfo?,
        nearbyNodeCount: Int
    ): List<String> {
        val lore = mutableListOf<String>()
        lore.add("&7Regiuni: &f${worldAdmin.regionCount}")
        lore.add("&7Places: &f${worldAdmin.placeCount}")
        lore.add("&7Noduri: &f${worldAdmin.nodeCount}")
        lore.add("&7Index: &f${if (worldAdmin.isAutoIndexEnabled) "activa" else "dezactivata"} &8(${worldAdmin.indexedRegionChunkCount}r/${worldAdmin.indexedPlaceChunkCount}p/${worldAdmin.indexedNodeChunkCount}n)")
        lore.add("&7Nearby Nodes: &f$nearbyNodeCount")
        lore.add("&7Current Region: &f${region?.id() ?: "<nemapat>"}")
        lore.add("&7Current Place: &f${place?.id() ?: "<nemapat>"}")
        lore.add("&7Current Node: &f${node?.id() ?: "<nemapat>"}")
        lore.add("&8Click: Debugdump Mapping")
        return lore
    }

    private fun mappingLore(plugin: AINPCPlugin): List<String> {
        return DebugDumpMappingText.buildMappingText(plugin)
            .lineSequence()
            .filter { it.isNotBlank() }
            .take(6)
            .map { "&7$it" }
            .toList()
    }

    private fun progressionLore(snapshot: ProgressionGuiSnapshot?): List<String> {
        if (snapshot == null || !snapshot.handled()) {
            return listOf(
                "&7Runtime-ul De Progresie Nu A Returnat Snapshot.",
                "&8Click: Incearca Log Activ",
                "&8Right Click: Toate Progresiile"
            )
        }

        val currentEntries = snapshot.currentEntries()
        val activeCount = currentEntries.count { it.active() }
        val offeredCount = currentEntries.count { it.offered() }
        val trackedEntry = currentEntries.firstOrNull { it.tracked() }

        val lore = mutableListOf<String>()
        lore.add("&7Filtru Snapshot: &f${valueOrUnknown(snapshot.filterLabel())}")
        lore.add("&7Curente: &f${currentEntries.size} &8(active $activeCount, offered $offeredCount)")
        lore.add("&7Arhivate Vizibile: &f${snapshot.archivedEntries().size}")
        lore.add(
            if (trackedEntry != null) "&7Tracking: &f${GuiItemFactory.compact(trackedEntry.title(), 28)}"
            else "&7Tracking: &8Niciuna Vizibila"
        )
        lore.add("&8Click: Progresii Active")
        lore.add("&8Right Click: Toate Progresiile")
        return lore
    }

    private fun localAnchorBindings(
        context: GuiRenderContext,
        player: Player,
        adminView: Boolean,
        region: WorldRegionInfo?,
        place: WorldPlaceInfo?,
        node: WorldNodeInfo?
    ): List<ProgressionAnchorBinding> {
        return try {
            val playerUuid = if (adminView) "" else player.uniqueId.toString()
            val rows = mutableListOf<ProgressionAnchorBinding>()
            addAnchorBindings(context, rows, playerUuid, "node", node?.id().orEmpty())
            addAnchorBindings(context, rows, playerUuid, "place", place?.id().orEmpty())
            addAnchorBindings(context, rows, playerUuid, "region", region?.id().orEmpty())
            rows.distinct().take(12)
        } catch (exception: SQLException) {
            context.plugin().logger.warning("Nu pot incarca ancorele locale pentru World GUI: ${exception.message}")
            emptyList()
        }
    }

    @Throws(SQLException::class)
    private fun addAnchorBindings(
        context: GuiRenderContext,
        rows: MutableList<ProgressionAnchorBinding>,
        playerUuid: String,
        anchorType: String,
        anchorId: String
    ) {
        if (anchorId.isBlank()) {
            return
        }
        rows.addAll(context.plugin().progressionService.getAnchorBindingsForAnchor(playerUuid, anchorType, anchorId, 6))
    }

    private fun anchorLore(rows: List<ProgressionAnchorBinding>?, adminView: Boolean): List<String> {
        val safeRows = rows ?: emptyList()
        val lore = mutableListOf<String>()
        lore.add("&7Potriviri Pentru Regiune/Place/Node: &f${safeRows.size}")
        if (safeRows.isEmpty()) {
            lore.add("&8Nu Exista Ancore Persistate Pentru Contextul Curent.")
        } else {
            safeRows.take(5).forEach { row ->
                lore.add(
                    "&7- &f${GuiItemFactory.compact(row.templateId(), 18)} &8${row.anchorSelector()} " +
                        "&7${valueOrUnknown(row.status())}"
                )
            }
        }
        lore.add(if (adminView) "&8Click: Toate Ancorele Persistate" else "&8Necesita Admin Pentru Lista Completa.")
        return lore
    }

    private fun confirmWorldCommand(
        player: Player,
        service: GuiService,
        title: String,
        command: String,
        warningLines: List<String>
    ) {
        service.openConfirmCommand(player, title, command, GuiKey.WORLD, "", warningLines)
    }

    private fun valueOrUnknown(value: String?): String = if (value.isNullOrBlank()) "necunoscut" else value

    private fun buildWorldStatusLines(
        worldName: String,
        x: Int,
        y: Int,
        z: Int,
        regionCount: Int,
        placeCount: Int,
        nodeCount: Int,
        visibleProgressions: Int,
        localAnchorCount: Int,
        nearbyNodeCount: Int,
        hasUnsavedChanges: Boolean,
        adminView: Boolean
    ): List<String> {
        return buildList {
            add("&7Coordonate: &f$worldName $x, $y, $z")
            add("&7Mapping: &f$regionCount regiuni / $placeCount places / $nodeCount noduri")
            add("&7Progresii Vizibile: &f$visibleProgressions")
            add("&7Ancore Locale: &f$localAnchorCount")
            add("&7Noduri In Raza: &f$nearbyNodeCount")
            add(if (hasUnsavedChanges) "&cModificari Nesalvate!" else "&aToate Modificarile Sunt Salvate")
            add(if (hasUnsavedChanges) "&eStatus: necesita save" else "&aStatus: curat")
            add(if (adminView) "&8Actiuni Principale: whereami / quest / admin" else "&8Actiuni Principale: whereami / quest")
        }
    }
}
