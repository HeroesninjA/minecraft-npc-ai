package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiNavigation
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.progression.ProgressionAnchorBinding

class QuestMapGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.QUEST_MAP

    override fun title(player: Player): String = "&0Quest Mapping"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val mechanic = context.service().getQuestMapMechanicFilter(player)
        val template = context.service().getQuestMapTemplateId(player)
        val objective = context.service().getQuestMapObjectiveKey(player)

        when {
            objective.isNotBlank() -> renderDetail(context, player, template, objective)
            template.isNotBlank() -> renderObjectives(context, player, template, mechanic)
            mechanic.isNotBlank() -> renderDefinitions(context, player, mechanic)
            else -> renderMechanics(context, player)
        }
    }

    private fun renderMechanics(context: GuiRenderContext, player: Player) {
        val definitions = context.plugin().progressionService.getDefinitions()
        val byMechanic = definitions.groupBy { it.mechanicId().ifBlank { "other" } }
        val mechanics = byMechanic.keys.sorted()

        context.item(4, GuiItemFactory.item(
            Material.MAP,
            "&6Quest Mapping — alege tipul",
            listOf("&7Definitii: &f${definitions.size}", "&7Ancorele create sunt folosite de quest engine!", "&7Click: vezi questurile din mecanica")
        ))

        var slot = 19
        for (m in mechanics.take(21)) {
            val entries = byMechanic[m] ?: emptyList()
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(mechanicMaterial(m), "&e$m &7(${entries.size})",
                    listOf("&7Active: &f${entries.count { it.enabled() }}", "&7Click: vezi questurile")),
                GuiAction { click ->
                    context.service().setQuestMapMechanicFilter(player, m)
                    context.service().setQuestMapTemplateId(player, null)
                    context.service().setQuestMapObjectiveKey(player, null)
                    click.service().open(click.player(), GuiKey.QUEST_MAP)
                }
            ))
        }

        if (definitions.isEmpty()) {
            context.item(22, GuiItemFactory.item(Material.BARRIER, "&cNicio definitie", listOf("&7Nu exista definitii.")))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun renderDefinitions(context: GuiRenderContext, player: Player, mechanic: String) {
        val definitions = context.plugin().progressionService.getDefinitions(mechanic)

        context.item(4, GuiItemFactory.item(
            Material.WRITABLE_BOOK,
            "&6$mechanic — alege un quest",
            listOf("&7Definitii: &f${definitions.size}", "&7Click: vezi obiectivele si ancorele")
        ))

        context.button(0, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi la tipuri", "&7Revine."),
            GuiAction { click ->
                context.service().setQuestMapMechanicFilter(player, null)
                click.service().open(click.player(), GuiKey.QUEST_MAP)
            }
        ))

        var slot = 19
        for (def in definitions.take(21)) {
            val anchors = loadAnchors(context, player.uniqueId.toString(), def.templateId())
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(
                    if (anchors.isNotEmpty()) Material.LIME_DYE else Material.PAPER,
                    "&f${GuiItemFactory.compact(def.displayName(), 32)}",
                    listOf(
                        "&7Template: &f${def.templateId()}",
                        "&7Obiective: &f${def.objectiveCount()}",
                        "&7Ancore: &f${anchors.size}",
                        "&8Click: vezi obiectivele"
                    )
                ),
                GuiAction { click ->
                    context.service().setQuestMapTemplateId(player, def.templateId())
                    context.service().setQuestMapObjectiveKey(player, null)
                    click.service().open(click.player(), GuiKey.QUEST_MAP)
                }
            ))
        }

        context.button(8, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun renderObjectives(context: GuiRenderContext, player: Player, templateId: String, mechanic: String) {
        val anchors = loadAnchors(context, player.uniqueId.toString(), templateId)
        val suggestions = runCatching {
            context.plugin().progressionService.getObjectiveIdSuggestions(player, templateId)
        }.getOrDefault(emptyList())

        context.item(4, GuiItemFactory.item(
            Material.KNOWLEDGE_BOOK,
            "&6Obiective",
            listOf(
                "&7Template: &f$templateId",
                "&7Ancore: &f${anchors.size}",
                if (suggestions.isNotEmpty()) "&7Sugestii: &f${suggestions.size}" else "&7Fara sugestii"
            )
        ))

        context.button(0, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi la ${mechanic.ifBlank { "questuri" }}", "&7Revine."),
            GuiAction { click ->
                context.service().setQuestMapTemplateId(player, null)
                click.service().open(click.player(), GuiKey.QUEST_MAP)
            }
        ))

        context.button(8, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
        ))

        var slot = 19
        val allKeys = (suggestions + anchors.map { it.objectiveKey() }).distinct().take(14)
        for (key in allKeys) {
            val anchor = anchors.firstOrNull { it.objectiveKey().equals(key, ignoreCase = true) }
            context.button(slot++, GuiButton.enabled(
                GuiItemFactory.item(
                    if (anchor != null) Material.LIME_DYE else Material.PAPER,
                    if (anchor != null) "&a$key" else "&f$key",
                    if (anchor != null) {
                        listOf("&7Ancora: &f${anchor.anchorType()}:${GuiItemFactory.compact(anchor.anchorId(), 20)}",
                            "&7Label: &f${anchor.displayLabel()}", "&8Click: editeaza sau sterge")
                    } else {
                        listOf("&7Fara ancora", "&8Click: creaza ancora")
                    }
                ),
                GuiAction { click ->
                    context.service().setQuestMapObjectiveKey(player, key)
                    click.service().open(click.player(), GuiKey.QUEST_MAP)
                }
            ))
        }

        if (allKeys.isEmpty()) {
            context.item(22, GuiItemFactory.item(Material.BARRIER, "&cNiciun obiectiv",
                listOf("&7Nu exista obiective.", "&7Click mai jos: foloseste templateId ca obiectiv.")))
            context.button(22, GuiButton.enabled(
                GuiItemFactory.item(Material.NAME_TAG, "&eFoloseste templateId",
                    listOf("&7Creeaza ancora direct pe template.")),
                GuiAction { click ->
                    context.service().setQuestMapObjectiveKey(player, templateId)
                    click.service().open(click.player(), GuiKey.QUEST_MAP)
                }
            ))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun renderDetail(context: GuiRenderContext, player: Player, templateId: String, objectiveKey: String) {
        val anchors = loadAnchors(context, player.uniqueId.toString(), templateId)
        val existing = anchors.firstOrNull { it.objectiveKey().equals(objectiveKey, ignoreCase = true) }
        val worldAdmin = context.plugin().platform.worldAdmin
        val loc = player.location
        val place = worldAdmin.findPlace(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)
        val region = worldAdmin.findRegion(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)

        val globalMode = context.service().getQuestMapGlobalMode(player)
        context.item(4, GuiItemFactory.item(
            Material.KNOWLEDGE_BOOK,
            "&6${GuiItemFactory.compact(objectiveKey, 30)}",
            listOf(
                "&7Template: &f$templateId",
                if (existing != null) "&7Ancora: &f${existing.anchorType()}:${existing.anchorId()}"
                else "&7Ancora: &cniciuna",
                "&7Locatie: &f${place?.displayName() ?: region?.name() ?: "<nemapat>"}",
                if (globalMode) "&6Mod GLOBAL — ancorele functioneaza pentru toti jucatorii" else "&aMod PERSONAL — ancorele functioneaza doar pentru tine"
            )
        ))

        context.button(0, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&eInapoi", "&7Revine."),
            GuiAction { click ->
                context.service().setQuestMapObjectiveKey(player, null)
                click.service().open(click.player(), GuiKey.QUEST_MAP)
            }
        ))

        context.button(8, GuiButton.enabled(
            GuiItemFactory.item(Material.SUNFLOWER, "&aRefresh", "&7Reincarca."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
        ))

        if (existing != null) {
            context.item(10, GuiItemFactory.item(
                Material.FILLED_MAP, "&aAncora: ${existing.anchorType()}:${existing.anchorId()}",
                listOf("&7Label: &f${existing.displayLabel()}", "&7Status: &f${existing.status()}")
            ))
            if (place != null) {
                context.button(12, GuiButton.enabled(
                    GuiItemFactory.item(Material.OAK_DOOR, "&6Editeaza — place curent",
                        listOf("&7${place.displayName()}")),
                    GuiAction { click -> saveBinding(context, player, templateId, objectiveKey, "place", place.id(), place.displayName()) }
                ))
            }
            if (region != null) {
                context.button(13, GuiButton.enabled(
                    GuiItemFactory.item(Material.FILLED_MAP, "&6Editeaza — regiune curenta",
                        listOf("&7${region.name()}")),
                    GuiAction { click -> saveBinding(context, player, templateId, objectiveKey, "region", region.id(), region.name()) }
                ))
            }
            context.button(14, GuiButton.enabled(
                GuiItemFactory.item(Material.BARRIER, "&cDelete binding", listOf("&7Click: confirma")),
                GuiAction { click ->
                    click.service().openConfirmCommand(click.player(), "Sterge ancora",
                        "ainpc quest anchors remove ${player.uniqueId} $templateId $objectiveKey",
                        GuiKey.QUEST_MAP, "",
                        listOf("&7Template: &f$templateId", "&7Obiectiv: &f$objectiveKey"))
                }
            ))
            context.button(15, GuiButton.enabled(
                GuiItemFactory.item(Material.ENDER_PEARL, "&6Edit binding",
                    listOf("&7Navigheaza la ancora.", "&7Re-salveaza prin selectia curenta.")),
                GuiAction { click ->
                    val cmd = if (existing.anchorType() == "place") "ainpc world place ${existing.anchorId()}"
                    else "ainpc world region info ${existing.anchorId()}"
                    click.service().runCommand(click.player(), cmd)
                }
            ))
        } else {
            context.item(10, GuiItemFactory.item(Material.GRAY_DYE, "&7Nicio ancora", listOf("&7Creaza una mai jos.")))
            var createSlot = 11
            if (place != null) {
            context.button(createSlot++, GuiButton.enabled(
                    GuiItemFactory.item(Material.OAK_DOOR, "&aCreate binding — place: ${GuiItemFactory.compact(place.displayName(), 20)}",
                        listOf("&7Click: creeaza binding")),
                    GuiAction { click -> saveBinding(context, player, templateId, objectiveKey, "place", place.id(), place.displayName()) }
                ))
            }
            if (region != null) {
                context.button(createSlot++, GuiButton.enabled(
                    GuiItemFactory.item(Material.FILLED_MAP, "&aCreate binding — region: ${GuiItemFactory.compact(region.name(), 20)}",
                        listOf("&7Click: creeaza binding")),
                    GuiAction { click -> saveBinding(context, player, templateId, objectiveKey, "region", region.id(), region.name()) }
                ))
            }
            context.button(16, GuiButton.enabled(
                GuiItemFactory.item(Material.MAP, "&bAlege din mapping", listOf("&7Click: /ainpc world places")),
                GuiAction { click -> click.service().runCommand(click.player(), "ainpc world places") }
            ))
        }

        context.button(17, GuiButton.enabled(
            GuiItemFactory.item(
                if (globalMode) Material.ENDER_EYE else Material.PLAYER_HEAD,
                if (globalMode) "&6Mod: GLOBAL — click pentru PERSONAL" else "&aMod: PERSONAL — click pentru GLOBAL",
                listOf(
                    if (globalMode) "&7Ancorele create functioneaza pentru TOTI jucatorii."
                    else "&7Ancorele create functioneaza doar pentru TINE.",
                    "&8Click: schimba modul"
                )
            ),
            GuiAction { click ->
                context.service().toggleQuestMapGlobalMode(click.player())
                click.service().open(click.player(), GuiKey.QUEST_MAP)
            }
        ))

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun saveBinding(context: GuiRenderContext, player: Player, templateId: String, objectiveKey: String, anchorType: String, anchorId: String, label: String) {
        val def = context.plugin().progressionService.getDefinitions(templateId).firstOrNull()
        val globalMode = context.service().getQuestMapGlobalMode(player)
        val playerUuid = if (globalMode) "" else player.uniqueId.toString()
        val modeLabel = if (globalMode) "&6[GLOBAL]" else "&a[PERSONAL]"
        runCatching {
            context.plugin().progressionService.saveAnchorBinding(ProgressionAnchorBinding(
                playerUuid, templateId, objectiveKey, def?.code() ?: "",
                "location", "", anchorType, anchorId, label,
                System.currentTimeMillis(), System.currentTimeMillis(), "active"
            ))
            context.service().open(player, GuiKey.QUEST_MAP)
        }.onSuccess {
            context.plugin().messageUtils.send(player, "$modeLabel Ancora salvata: &f$anchorType:$anchorId &a-> &f$objectiveKey")
        }.onFailure { e ->
            context.plugin().logger.warning("Nu am putut salva anchor binding: ${e.message}")
            context.plugin().messageUtils.send(player, "&cEroare la salvarea ancorei: ${e.message}")
        }
    }

    private fun loadAnchors(context: GuiRenderContext, playerUuid: String, templateId: String): List<ProgressionAnchorBinding> {
        return runCatching {
            context.plugin().progressionService.getAnchorBindings(playerUuid, templateId, 50)
        }.getOrDefault(emptyList())
    }

    private fun mechanicMaterial(m: String): Material = when (m) {
        "quest" -> Material.WRITABLE_BOOK
        "contract" -> Material.PAPER
        "duty" -> Material.SHIELD
        "bounty" -> Material.IRON_SWORD
        "event" -> Material.BELL
        "tutorial", "onboarding" -> Material.COMPASS
        "ritual" -> Material.AMETHYST_SHARD
        else -> Material.NAME_TAG
    }
}
