package ro.ainpc.gui.screens

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.debug.DebugDumpMappingText
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.gui.GuiService
import ro.ainpc.progression.ProgressionAnchorBinding
import ro.ainpc.progression.ProgressionFormatUtil
import ro.ainpc.progression.ProgressionGuiEntry
import ro.ainpc.progression.ProgressionGuiSnapshot
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.sql.SQLException
import java.util.ArrayList
import java.util.Comparator
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
            .stream()
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .toList()

        context.item(
            4,
            GuiItemFactory.item(
                Material.COMPASS,
                "&bWorld context",
                listOf(
                    "&7Coordonate: &f$worldName $x, $y, $z",
                    "&7Mapping: &f${worldAdmin.regionCount} regiuni / ${worldAdmin.placeCount} places / ${worldAdmin.nodeCount} noduri",
                    "&7Progresii vizibile: &f${progressionSnapshot.allEntries().size}",
                    "&7Ancore locale: &f${localAnchorBindings.size}",
                    if (worldAdmin.hasUnsavedChanges()) "&cModificari nesalvate!" else "&aToate modificarile sunt salvate",
                    "&8Actiuni principale: whereami / quest / admin"
                )
            )
        )
        context.button(
            5,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.PAPER, "&dMapping snapshot", mappingSnapshotLore(worldAdmin, region, place, node, nearbyNodes.size)),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc debugdump mapping") }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Mapping snapshot",
                        mappingSnapshotLore(worldAdmin, region, place, node, nearbyNodes.size)
                    )
                )
            }
        )

        context.button(10, if (region != null) {
            GuiButton.enabled(
                GuiItemFactory.item(Material.FILLED_MAP, "&eRegiune curenta", regionLore(region)),
                GuiAction { click -> click.service().openRegionDetail(click.player(), region.id()) }
            )
        } else {
            GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Regiune", regionLore(null)))
        })
        context.button(11, if (place != null) {
            GuiButton.enabled(
                GuiItemFactory.item(Material.OAK_DOOR, "&aPlace curent", placeLore(place)),
                GuiAction { click -> click.service().openPlaceDetail(click.player(), place.id()) }
            )
        } else {
            GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Place curent", placeLore(null)))
        })
        context.item(12, GuiItemFactory.item(Material.TARGET, "&dNode curent", nodeLore(node)))
        if (place != null) {
            val placeProgressions = runCatching {
                context.plugin().progressionService.getProgressionsByAnchor("place", place.id(), 3)
            }.getOrDefault(emptyList())
            if (placeProgressions.isNotEmpty()) {
                context.item(
                    16,
                    GuiItemFactory.item(
                        Material.LIME_DYE,
                        "&aProgresii pe acest place",
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
                        "&aProgresii pe acest node",
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
                    GuiAction { click ->
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
                        listOf("&7Nu ai acces la GUI-ul de progresii.")
                    )
                )
            }
        )
        context.button(
            14,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.MAP, "&6Ancore progresii", anchorLore(localAnchorBindings, true)),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc quest anchors all") }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Ancore progresii",
                        anchorLore(localAnchorBindings, false)
                    )
                )
            }
        )
        context.button(
            15,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.PAPER, "&eMapping diagnostics", mappingLore(context.plugin())),
                    GuiAction { click -> click.service().runCommand(click.player(), "ainpc debugdump mapping") }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Mapping diagnostics",
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
                GuiItemFactory.item(Material.ENDER_EYE, "&bWhere am I", "&7Ruleaza /ainpc world whereami.", "&8Actiune principala."),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc world whereami") }
            )
        )
        context.button(
            29,
            if (context.service().canOpen(player, GuiKey.STORY)) {
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.AMETHYST_SHARD,
                        "&dStory",
                        "&7Deschide snapshot-ul story pentru regiunea si place-ul curent."
                    ),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.STORY) }
                )
            } else {
                GuiButton.disabled(
                    GuiItemFactory.disabled(
                        Material.GRAY_DYE,
                        "&7Story",
                        listOf("&7Nu ai acces la GUI-ul story.")
                    )
                )
            }
        )

        context.button(
            18,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.COMMAND_BLOCK, "&6Admin Mapping",
                        listOf("&7Gestioneaza regiuni, places si noduri.", "&7Click: deschide panoul admin.", "&8Actiune admin separata.")),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_MAPPING) }
                )
            } else {
                GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Admin Mapping",
                    listOf("&7Necesita admin.")))
            }
        )
        context.button(
            33,
            if (adminView) {
                GuiButton.enabled(
                    GuiItemFactory.item(Material.KNOWLEDGE_BOOK, "&6Admin Quest",
                        listOf("&7Gestioneaza definitii, ancore si progresii.", "&7Click: deschide panoul admin quest.", "&8Actiune admin separata.")),
                    GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_QUEST) }
                )
            } else {
                GuiButton.disabled(GuiItemFactory.disabled(Material.GRAY_DYE, "&7Admin Quest",
                    listOf("&7Necesita admin.")))
            }
        )
        if (adminView) {
            context.button(
                30,
                GuiButton.enabled(
                    GuiItemFactory.item(
                        Material.SPYGLASS,
                        "&6Scan sat",
                        "&7Cere confirmare pentru scan vanilla pe raza 48."
                    ),
                    GuiAction { click ->
                        confirmWorldCommand(
                            click.player(),
                            click.service(),
                            "Scan sat",
                            "ainpc world scan village 48",
                            listOf(
                                "&7Raza: &f48 block-uri",
                                "&7Poate importa sau actualiza semantic mapping pentru zona curenta.",
                                "&7Verifica rezultatul prin audit/debugdump dupa rulare."
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
                        "&6Demo mapping",
                        "&7Cere confirmare inainte de creare mapping demo."
                    ),
                    GuiAction { click ->
                        confirmWorldCommand(
                            click.player(),
                            click.service(),
                            "Creeaza mapping demo",
                            "ainpc world demo create",
                            listOf(
                                "&7Tinta: &fzona curenta",
                                "&7Creeaza regiuni, places sau noduri demo in world mapping.",
                                "&cFoloseste doar pe lumi de test sau dupa backup."
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
                        "&aSalveaza mapping",
                        "&7Cere confirmare inainte de persistenta mapping."
                    ),
                    GuiAction { click ->
                        confirmWorldCommand(
                            click.player(),
                            click.service(),
                            "Salveaza world mapping",
                            "ainpc world save",
                            listOf(
                                "&7Persistenta mapping-ului curent.",
                                "&7Dupa confirmare, comenzile text raporteaza rezultatul in chat."
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
        return listOf(
            "&7ID: &f${region.id()}",
            "&7Nume: &f${region.name()}",
            "&7Tip: &f${region.typeId()}",
            "&7Story state: &f${region.storyStateKey()}",
            "&7Tags: &f${region.tags().joinToString(", ")}"
        )
    }

    private fun placeLore(place: WorldPlaceInfo?): List<String> {
        if (place == null) {
            return listOf("&7Niciun place mapat aici.")
        }
        return listOf(
            "&7ID: &f${place.id()}",
            "&7Regiune: &f${place.regionId()}",
            "&7Nume: &f${place.displayName()}",
            "&7Tip: &f${place.placeType().id}",
            "&7Access: &f${if (place.publicAccess()) "public" else "restrictionat"}"
        )
    }

    private fun nodeLore(node: WorldNodeInfo?): List<String> {
        if (node == null) {
            return listOf("&7Niciun node activ aici.")
        }
        val lore = ArrayList<String>()
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
        val lore = ArrayList<String>()
        lore.add("&7Regiuni: &f${worldAdmin.regionCount}")
        lore.add("&7Places: &f${worldAdmin.placeCount}")
        lore.add("&7Noduri: &f${worldAdmin.nodeCount}")
        lore.add("&7Index: &f${if (worldAdmin.isAutoIndexEnabled) "activa" else "dezactivata"} &8(${worldAdmin.indexedRegionChunkCount}r/${worldAdmin.indexedPlaceChunkCount}p/${worldAdmin.indexedNodeChunkCount}n)")
        lore.add("&7Nearby nodes: &f$nearbyNodeCount")
        lore.add("&7Curent region: &f${region?.id() ?: "<nemapat>"}")
        lore.add("&7Curent place: &f${place?.id() ?: "<nemapat>"}")
        lore.add("&7Curent node: &f${node?.id() ?: "<nemapat>"}")
        lore.add("&8Click: debugdump mapping")
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
                "&7Runtime-ul de progresie nu a returnat snapshot.",
                "&8Click: incearca log activ",
                "&8Right click: toate progresiile"
            )
        }

        val currentEntries = snapshot.currentEntries()
        val activeCount = currentEntries.stream().filter { e -> e.active() }.count()
        val offeredCount = currentEntries.stream().filter { e -> e.offered() }.count()
        val trackedEntry = currentEntries.stream().filter { e -> e.tracked() }.findFirst().orElse(null)

        val lore = ArrayList<String>()
        lore.add("&7Filtru snapshot: &f${valueOrUnknown(snapshot.filterLabel())}")
        lore.add("&7Curente: &f${currentEntries.size} &8(active $activeCount, offered $offeredCount)")
        lore.add("&7Arhivate vizibile: &f${snapshot.archivedEntries().size}")
        lore.add(
            if (trackedEntry != null) "&7Tracking: &f${GuiItemFactory.compact(trackedEntry.title(), 28)}"
            else "&7Tracking: &8niciuna vizibila"
        )
        lore.add("&8Click: progresii active")
        lore.add("&8Right click: toate progresiile")
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
            val rows = ArrayList<ProgressionAnchorBinding>()
            addAnchorBindings(context, rows, playerUuid, "node", node?.id().orEmpty())
            addAnchorBindings(context, rows, playerUuid, "place", place?.id().orEmpty())
            addAnchorBindings(context, rows, playerUuid, "region", region?.id().orEmpty())
            rows.stream().distinct().limit(12).toList()
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
        val lore = ArrayList<String>()
        lore.add("&7Potriviri pentru regiune/place/node: &f${safeRows.size}")
        if (safeRows.isEmpty()) {
            lore.add("&8Nu exista ancore persistate pentru contextul curent.")
        } else {
            safeRows.stream().limit(5).forEach { row ->
                lore.add(
                    "&7- &f${GuiItemFactory.compact(row.templateId(), 18)} &8${row.anchorSelector()} " +
                        "&7${valueOrUnknown(row.status())}"
                )
            }
        }
        lore.add(if (adminView) "&8Click: toate ancorele persistate" else "&8Necesita admin pentru lista completa.")
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
}
