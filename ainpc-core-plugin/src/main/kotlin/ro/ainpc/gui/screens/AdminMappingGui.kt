package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.world.RegionIdentityProvider
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldRegionInfo

class AdminMappingGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.ADMIN_MAPPING

    override fun title(player: Player): String = "&0Admin Mapping"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val worldAdmin: WorldAdminApi = context.plugin().platform.worldAdmin
        val player = context.player()
        val location = player.location
        val regions = worldAdmin.regions.sortedBy { it.id() }

        val worldMode = worldAdmin.worldMode
        val isAutoIndex = worldAdmin.isAutoIndexEnabled
        val currentRegion = worldAdmin.findRegion(location.world.name, location.blockX, location.blockY, location.blockZ)
        val currentPlace = worldAdmin.findPlace(location.world.name, location.blockX, location.blockY, location.blockZ)
        val currentNodes = worldAdmin.findNodesNear(location.world.name, location.x, location.y, location.z, 16.0, 5)

        context.button(0, GuiButton.enabled(
            GuiItemFactory.item(Material.STICK, "&6Wand", "&7Togleaza modul wand pentru mapping."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc wand") }
        ))
        context.button(1, GuiButton.enabled(
            GuiItemFactory.item(Material.CHAINMAIL_BOOTS, "&eBindings", "&7Listeaza NPC-world bindings."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world bindings") }
        ))
        context.button(2, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&bExterior", "&7Analizeaza structurile exterioare."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world outside") }
        ))

        context.button(8, GuiButton.enabled(
            GuiItemFactory.item(Material.REDSTONE, "&cReload config", "&7Reincarca world admin din config."),
            GuiAction { click ->
                click.service().runCommand(click.player(), "ainpc world reload")
            }
        ))

        context.item(4, GuiItemFactory.item(
            Material.COMPASS,
            "&6Admin Mapping",
            buildMappingStatusLines(
                worldAdmin.regionCount,
                worldAdmin.placeCount,
                worldAdmin.nodeCount,
                worldMode.id,
                isAutoIndex,
                currentRegion?.id(),
                currentPlace?.displayName(),
                currentNodes.size,
                worldAdmin.hasUnsavedChanges()
            )
        ))

        context.item(10, GuiItemFactory.item(
            Material.ENDER_EYE,
            "&bLocatie curenta",
            listOf(
                "&7Lume: &f${location.world.name}",
                "&7X: &f${location.blockX} Y: &f${location.blockY} Z: &f${location.blockZ}",
                "&7Regiune: &f${currentRegion?.name() ?: "<nemapat>"}",
                "&7Place: &f${currentPlace?.displayName() ?: "<nemapat>"}",
                "&7Noduri in raza 16m: &f${currentNodes.size}"
            )
        ))

        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.OAK_DOOR, "&aWhere am I", "&7Ruleaza /ainpc world whereami.", "&8Actiuni principale."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world whereami") }
        ))

        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&dMapping dump", "&7Debug dump mapping text in chat."),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc debugdump mapping") }
        ))

        context.button(13, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&6Scan sat", "&7Scaneaza vanilla village in raza 48.", "&8Admin separat."),
            GuiAction { click ->
                click.service().openConfirmCommand(
                    click.player(),
                    "Scan sat",
                    "ainpc world scan village 48",
                    GuiKey.ADMIN_MAPPING,
                    "",
                    listOf("&7Raza: &f48 block-uri", "&7Poate importa mapping semantic.")
                )
            }
        ))

        context.button(14, GuiButton.enabled(
            GuiItemFactory.item(Material.GRASS_BLOCK, "&6Demo mapping", "&7Creeaza mapping demo la pozitia ta.", "&8Admin separat."),
            GuiAction { click ->
                click.service().openConfirmCommand(
                    click.player(),
                    "Creeaza mapping demo",
                    "ainpc world demo create",
                    GuiKey.ADMIN_MAPPING,
                    "",
                    listOf("&7Creeaza regiuni, places si noduri demo.", "&cDoar pe lumi de test.")
                )
            }
        ))

        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&aSalveaza mapping", "&7Persista modificarile in config.", "&8Actiune persistenta separata."),
            GuiAction { click ->
                click.service().openConfirmCommand(
                    click.player(),
                    "Salveaza world mapping",
                    "ainpc world save",
                    GuiKey.ADMIN_MAPPING,
                    "",
                    listOf("&7Salveaza toate modificarile curente.")
                )
            }
        ))

        if (currentRegion != null) {
            val regionType = RegionType.fromId(currentRegion.typeId())
            val identity = RegionIdentityProvider.identity(regionType)
            context.item(6, GuiItemFactory.item(
                Material.AMETHYST_SHARD,
                "&dIdentitate: ${identity.displayName}",
                listOf(
                    "&7Tip: &f${identity.displayName} &8(${regionType.id})",
                    "&7Descriere: &f${identity.description}",
                    "&7Poveste: &f${identity.defaultStoryKey} &8(${identity.mood})",
                    "&7Threat: &f${identity.threatLevel}",
                    "&7Atmosfera: &f${identity.ambiance}"
                )
            ))
        }

        context.button(16, GuiButton.enabled(
            GuiItemFactory.item(
                if (isAutoIndex) Material.REDSTONE_TORCH else Material.TORCH,
                if (isAutoIndex) "&cDezactiveaza auto-index" else "&aActiveaza auto-index",
                "&7Auto-index: &f${if (isAutoIndex) "ACTIV" else "INACTIV"}"
            ),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world autoindex ${if (isAutoIndex) "off" else "on"}") }
        ))

        context.button(17, GuiButton.enabled(
            GuiItemFactory.item(Material.COMMAND_BLOCK, "&6Admin Quest", "&7Deschide panoul admin quest.", "&8Admin separat."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.ADMIN_QUEST) }
        ))

        context.button(18, GuiButton.enabled(
            GuiItemFactory.item(Material.MAP, "&eTipuri regiuni", regionTypeLore(worldAdmin)),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world region summary") }
        ))

        context.button(22, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPARATOR, "&6Patch analyze", "&7Analizeaza decalajele regiunii curente."),
            GuiAction { click ->
                val regionId = currentRegion?.id() ?: "demo_sat"
                click.service().runCommand(click.player(), "ainpc patch analyze $regionId")
            }
        ))
        context.button(23, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPARATOR, "&6Patch plan", "&7Planifica patch-uri pentru regiunea curenta."),
            GuiAction { click ->
                val regionId = currentRegion?.id() ?: "demo_sat"
                click.service().runCommand(click.player(), "ainpc patch plan $regionId")
            }
        ))

        if (currentRegion != null) {
            context.item(9, GuiItemFactory.item(
                Material.ENDER_EYE,
                "&bNoduri apropiate (${currentNodes.size})",
                currentNodes.map { node ->
                    "&7- &f${node.id()} &8[${node.typeId()}] &7(${node.x().toInt()},${node.y().toInt()},${node.z().toInt()})"
                }.ifEmpty { listOf("&8Niciun nod in raza 16m.") }
            ))
        }

        context.item(26, GuiItemFactory.item(
            Material.WATER_BUCKET,
            "&bLumi",
            worldAdmin.regions.groupBy { it.worldName().ifBlank { "unknown" } }.map { (world, regs) ->
                "&7- &f${world}: &e${regs.size}"
            }.toList().ifEmpty { listOf("&8Nicio regiune.") }
        ))

        val nodeTypeDist = worldAdmin.nodes.groupBy { it.typeId() }.mapValues { it.value.size }
        context.item(27, GuiItemFactory.item(
            Material.LODESTONE,
            "&bTipuri noduri",
            nodeTypeDist.entries.sortedByDescending { it.value }.map { (type, count) ->
                "&7- &f${type}: &e${count}"
            }.ifEmpty { listOf("&8Niciun nod.") }
        ))

        val pageSize = 21
        val pageCount = maxOf(1, (regions.size + pageSize - 1) / pageSize)
        val currentPage = minOf(context.service().getAdminMappingPage(player), pageCount - 1)
        val pageStart = currentPage * pageSize
        val pageRegions = regions.drop(pageStart).take(pageSize)

        val regionSlots = listOf(
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        )

        for ((index, region) in pageRegions.withIndex()) {
            if (index >= regionSlots.size) break
            val slot = regionSlots[index]
            val places = worldAdmin.getPlaces(region.id())
            val nodes = worldAdmin.getNodes(region.id())
            val regionType = RegionType.fromId(region.typeId())
            val identity = RegionIdentityProvider.identity(regionType)
            context.button(slot, GuiButton.enabled(
                GuiItemFactory.item(regionIcon(region.typeId()), "&e${region.name()} &7(${region.id()})", listOf(
                    "&7Tip: &f${identity.displayName} &8(${region.typeId()})",
                    "&7Places: &f${places.size} &7| Noduri: &f${nodes.size}",
                    "&7Lume: &f${region.worldName()}",
                    "&7Story: &f${region.storyMode().id} &8(${region.storyStateKey()})",
                    "&7Mood: &f${identity.mood} &7| Threat: &f${identity.threatLevel}",
                    "&7Click: detalii regiune"
                )),
                GuiAction { click -> click.service().openRegionDetail(click.player(), region.id()) }
            ))
        }

        if (regions.isEmpty()) {
            context.item(22, GuiItemFactory.item(Material.BARRIER, "&cNicio regiune", listOf(
                "&7World admin nu are regiuni.",
                "&7Foloseste butonul Demo mapping de mai sus."
            )))
        }

        if (pageCount > 1) {
            context.button(44, GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&eAnterioara", "&7Pag ${currentPage + 1}/$pageCount"),
                GuiAction { click -> click.service().openAdminMappingPage(click.player(), currentPage - 1) }
            ))
            context.button(46, GuiButton.enabled(
                GuiItemFactory.item(Material.ARROW, "&eUrmatoarea", "&7Pag ${currentPage + 1}/$pageCount"),
                GuiAction { click -> click.service().openAdminMappingPage(click.player(), currentPage + 1) }
            ))
        }

        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.AMETHYST_SHARD, "&dIdentitate regiune", listOf(
                "&7Arata identitatea tipului de regiune curent.",
                "&7Click: /ainpc world region identity ${currentRegion?.typeId() ?: "settlement"}"
            )),
            GuiAction { click ->
                click.service().runCommand(click.player(), "ainpc world region identity ${currentRegion?.typeId() ?: "settlement"}")
            }
        ))

        context.button(47, GuiButton.enabled(
            GuiItemFactory.item(Material.OAK_DOOR, "&aCreaza place", listOf(
                "&7Creaza un place in regiunea curenta.",
                "&7Foloseste: /ainpc world place create <regiune> <id> <tip> <x1> <y1> <z1> <x2> <y2> <z2>"
            )),
            GuiAction { click ->
                val regionId = currentRegion?.id() ?: ""
                val placeId = "p_${location.blockX}_${location.blockZ}"
                val minX = location.blockX - 8
                val maxX = location.blockX + 8
                val minZ = location.blockZ - 8
                val maxZ = location.blockZ + 8
                val minY = maxOf(0, location.blockY - 4)
                val maxY = minOf(319, location.blockY + 4)
                click.service().runCommand(click.player(),
                    "ainpc world place create $regionId $placeId house $minX $minY $minZ $maxX $maxY $maxZ")
            }
        ))

        context.button(48, GuiButton.enabled(
            GuiItemFactory.item(Material.LODESTONE, "&bCreaza node", listOf(
                "&7Creaza un nod la pozitia curenta.",
                "&7Foloseste: /ainpc world node create <regiune> <placeId|-> <id> <tip> <x> <y> <z> [radius]"
            )),
            GuiAction { click ->
                val regionId = currentRegion?.id() ?: ""
                val nodeId = "n_${location.blockX}_${location.blockZ}"
                click.service().runCommand(click.player(),
                    "ainpc world node create $regionId - $nodeId meeting_point ${location.x} ${location.y} ${location.z} 2.5")
            }
        ))

        context.button(50, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&cRemove place", listOf(
                "&7Elimina place-ul curent (daca exista).",
                "&7Click: /ainpc world place remove <placeId>"
            )),
            GuiAction { click ->
                val placeId = currentPlace?.id() ?: ""
                if (placeId.isNotBlank()) {
                    click.service().openConfirmCommand(
                        click.player(),
                        "Remove place",
                        "ainpc world place remove $placeId",
                        GuiKey.ADMIN_MAPPING,
                        "",
                        listOf("&7Place: &f$placeId", "&cAceasta actiune sterge place-ul si nodurile asociate.")
                    )
                } else {
                    click.service().runCommand(click.player(), "ainpc world place remove")
                }
            }
        ))

        context.button(51, GuiButton.enabled(
            GuiItemFactory.item(Material.WRITABLE_BOOK, "&dListeaza places", listOf(
                "&7Afiseaza toate place-urile in chat.",
                "&7Click: /ainpc world places"
            )),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world places") }
        ))

        context.button(52, GuiButton.enabled(
            GuiItemFactory.item(Material.PAPER, "&dListeaza noduri", listOf(
                "&7Afiseaza nodurile regiunii curente in chat.",
                "&7Click: /ainpc world nodes ${currentRegion?.id() ?: ""}"
            )),
            GuiAction { click -> click.service().runCommand(click.player(), "ainpc world nodes ${currentRegion?.id() ?: ""}") }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun regionTypeLore(worldAdmin: WorldAdminApi): List<String> {
        val byType = worldAdmin.regions.groupBy { it.typeId().ifBlank { "custom" } }
        val lore = mutableListOf<String>()
        lore.add("&7Distributia tipurilor de regiuni:")
        for ((type, list) in byType.entries.sortedByDescending { it.value.size }) {
            lore.add("&7- &f${type}: &e${list.size}")
        }
        lore.add("&8Click: listeaza places in chat")
        return lore
    }

    private fun regionIcon(typeId: String): Material = when (typeId.lowercase()) {
        "settlement", "village" -> Material.VILLAGER_SPAWN_EGG
        "wilderness", "forest" -> Material.OAK_SAPLING
        "dungeon" -> Material.SPAWNER
        "city", "town" -> Material.STONE_BRICKS
        "water", "ocean" -> Material.WATER_BUCKET
        "mountain" -> Material.STONE
        "farm" -> Material.WHEAT
        else -> Material.FILLED_MAP
    }

    private fun buildMappingStatusLines(
        regionCount: Int,
        placeCount: Int,
        nodeCount: Int,
        worldMode: String,
        autoIndexEnabled: Boolean,
        currentRegionId: String?,
        currentPlaceName: String?,
        nearbyNodeCount: Int,
        hasUnsavedChanges: Boolean
    ): List<String> {
        return buildList {
            add("&7Regiuni: &f$regionCount")
            add("&7Places: &f$placeCount")
            add("&7Noduri: &f$nodeCount")
            add("&7World mode: &f$worldMode")
            add("&7Indexare automata: &f${if (autoIndexEnabled) "activa" else "dezactivata"}")
            add("&7Current region: &f${currentRegionId ?: "<niciuna>"}")
            add("&7Current place: &f${currentPlaceName ?: "<niciunul>"}")
            add("&7Noduri in raza: &f$nearbyNodeCount")
            add(if (hasUnsavedChanges) "&cModificari nesalvate!" else "&aToate salvate")
            add(if (hasUnsavedChanges) "&eStatus: necesita save" else "&aStatus: curat")
            add("&8Actiuni principale: whereami / scan / save")
        }
    }
}
