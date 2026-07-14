@file:Suppress("SENSELESS_COMPARISON")
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
import ro.ainpc.story.StoryAuthoringService.StoryEventDraft
import ro.ainpc.story.StoryAuthoringService.StoryTemplate
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.util.Locale

class StoryAuthoringGui : GuiScreen {
    private var selectedTemplate: StoryTemplate? = null
    private var selectedRegion: WorldRegionInfo? = null
    private var selectedPlace: WorldPlaceInfo? = null
    private var step = 0

    override fun key(): GuiKey = GuiKey.STORY_AUTHORING

    override fun title(player: Player): String = "&0Authoring Story"

    override fun size(player: Player): Int = 54

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        val plugin = context.plugin()
        val authoring = plugin.storyAuthoringService
        val adminView = player.hasPermission("ainpc.admin")

        context.button(0, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&7Inapoi la Story",
                "&7Click: revino la ecranul story"),
            GuiAction { click -> click.service().open(click.player(), GuiKey.STORY) }
        ))

        context.item(4, GuiItemFactory.item(
            Material.WRITABLE_BOOK,
            "&6Authoring Evenimente Story",
            listOf(
                "&7Creeaza evenimente story pentru regiuni/place-uri.",
                if (step == 0) "&aPas 1: Alege un template sau tip eveniment"
                else if (step == 1) "&aPas 2: Alege o regiune sau un place"
                else "&aPas 3: Review si aplica"
            )
        ))

        context.button(8, GuiButton.enabled(
            GuiItemFactory.item(Material.BARRIER, "&7Reseteaza",
                "&7Click: reincepe procesul"),
            GuiAction { click ->
                selectedTemplate = null; selectedRegion = null; selectedPlace = null; step = 0
                click.service().open(click.player(), GuiKey.STORY_AUTHORING)
            }
        ))
        context.button(2, GuiButton.enabled(
            GuiItemFactory.item(Material.FILLED_MAP, "&6Quest Map",
                "&7Leaga obiectivele de locatii."),
            GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
        ))

        when (step) {
            0 -> renderTemplateSelection(context, authoring.getTemplates())
            1 -> renderScopeSelection(context, plugin)
            2 -> renderReviewAndApply(context, plugin)
        }
    }

    private fun renderTemplateSelection(context: GuiRenderContext, templates: List<StoryTemplate>) {
        context.item(18, GuiItemFactory.item(Material.BOOK, "&eTemplate-uri", listOf(
            "&7Selecteaza un template predefinit",
            "&7sau foloseste un tip direct mai jos."
        )))

        val slots = intArrayOf(19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32, 33, 34, 35)
        for (i in templates.indices) {
            if (i >= slots.size) break
            val tpl = templates[i]
            context.button(slots[i], GuiButton.enabled(
                GuiItemFactory.item(Material.PAPER, "&f${tpl.name}",
                    listOf("&7${tpl.description}", "&7Tip: &f${tpl.eventType}", "&7Click: selecteaza")),
                GuiAction {
                    selectedTemplate = tpl; step = 1
                    context.service().open(context.player(), GuiKey.STORY_AUTHORING)
                }
            ))
        }

        context.button(45, GuiButton.enabled(
            GuiItemFactory.item(Material.FEATHER, "&fTip direct",
                listOf("&7Foloseste un tip de eveniment fara template")),
            GuiAction {
                selectedTemplate = StoryTemplate("", "Tip direct", "", "", "", "")
                step = 1
                context.service().open(context.player(), GuiKey.STORY_AUTHORING)
            }
        ))
    }

    private fun renderScopeSelection(context: GuiRenderContext, plugin: ro.ainpc.AINPCPlugin) {
        val worldAdmin = plugin.platform.worldAdmin
        val regions = worldAdmin.regions.take(36)

        val totalAnchors = runCatching {
            plugin.progressionService.getAnchorBindings(null, null, 500).size
        }.getOrDefault(0)
        val regionAnchorCounts = runCatching {
            val all = plugin.progressionService.getAnchorBindings(null, null, 500)
            all.filter { it.anchorType() == "region" }.groupBy { it.anchorId() }.mapValues { it.value.size }
        }.getOrDefault(emptyMap())

        context.item(4, GuiItemFactory.item(
            Material.MAP, "&eSelecteaza o regiune",
            listOf(
                "&7Sunt disponibile ${worldAdmin.regionCount} regiuni",
                "&7Ancore quest totale: &f$totalAnchors",
                "&7Regiuni cu ancore: &f${regionAnchorCounts.size}"
            )
        ))

        if (selectedRegion != null) {
            renderPlaceSelection(context, plugin, selectedRegion!!)
            return
        }

        val slots = intArrayOf(
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
        )

        for (i in regions.indices) {
            if (i >= slots.size) break
            val region = regions.elementAt(i)
            context.button(slots[i], GuiButton.enabled(
                GuiItemFactory.item(Material.GRASS_BLOCK, "&f${region.name()}",
                    listOf("&7ID: ${region.id()}", "&7Tip: ${region.typeId()}",
                        "&7Click: selecteaza aceasta regiune")),
                GuiAction {
                    selectedRegion = region; step = 2
                    context.service().open(context.player(), GuiKey.STORY_AUTHORING)
                }
            ))
        }
    }

    private fun renderPlaceSelection(context: GuiRenderContext, plugin: ro.ainpc.AINPCPlugin, region: WorldRegionInfo) {
        val places = plugin.platform.worldAdmin.getPlaces(region.id()).take(36)
        val regionAnchors = runCatching {
            plugin.progressionService.getAnchorBindings(null, null, 200)
                .count { it.anchorType() == "region" && it.anchorId().equals(region.id(), ignoreCase = true) }
        }.getOrDefault(0)
        val placeAnchorCounts = runCatching {
            val all = plugin.progressionService.getAnchorBindings(null, null, 500)
            all.filter { it.anchorType() == "place" }.groupBy { it.anchorId() }.mapValues { it.value.size }
        }.getOrDefault(emptyMap())
        val slots = intArrayOf(
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
        )

        context.item(4, GuiItemFactory.item(
            Material.MAP, "&eRegiune: ${region.name()}",
            listOf(
                "&7Alege un place sau foloseste regiunea.",
                "&7Ancore regiune: &f$regionAnchors",
                "&7Places disponibile: &f${places.size}"
            )
        ))

        context.button(8, GuiButton.enabled(
            GuiItemFactory.item(Material.ARROW, "&7Foloseste regiunea (fara place)",
                "&7Click: aplica evenimentul pe regiune"),
            GuiAction { step = 2; selectedPlace = null; context.service().open(context.player(), GuiKey.STORY_AUTHORING) }
        ))

        for (i in places.indices) {
            if (i >= slots.size) break
            val place = places.elementAt(i)
            context.button(slots[i], GuiButton.enabled(
                GuiItemFactory.item(Material.OAK_DOOR, "&f${place.displayName()}",
                    listOf("&7ID: ${place.id()}", "&7Tip: ${place.placeType().id}",
                        "&7Click: selecteaza acest place")),
                GuiAction {
                    selectedPlace = place; step = 2
                    context.service().open(context.player(), GuiKey.STORY_AUTHORING)
                }
            ))
        }
    }

    private fun renderReviewAndApply(context: GuiRenderContext, plugin: ro.ainpc.AINPCPlugin) {
        val authoring = plugin.storyAuthoringService
        val template = selectedTemplate
        val region = selectedRegion
        val place = selectedPlace

        if (template == null || region == null && place == null) {
            context.item(22, GuiItemFactory.item(Material.BARRIER, "&cSelectie incompleta",
                listOf("&7Selecteaza un template si o regiune/place.")))
            return
        }

        val scopeId = place?.id() ?: region?.id() ?: ""
        val templateName = if (template.id.isNotBlank()) template.name else "Tip direct"
        val eventType = if (template.id.isNotBlank()) template.eventType else "story_event"
        val scopeType = if (place != null) "place: ${place.displayName()}" else "region: ${region?.name()}"

        val anchorType = if (place != null) "place" else "region"
        val anchorId = place?.id() ?: region?.id() ?: ""
        val anchorCount = runCatching {
            plugin.progressionService.getAnchorBindings(null, null, 200)
                .count { it.anchorType().equals(anchorType, ignoreCase = true) && it.anchorId().equals(anchorId, ignoreCase = true) }
        }.getOrDefault(0)

        val reviewLore = buildList {
            add("&7Template: &f$templateName")
            add("&7Tip: &f$eventType")
            add("&7Aplicat pe: &f$scopeType")
            add("&7ID: &f$scopeId")
            if (anchorCount > 0) {
                add("&7Ancore quest la aceasta locatie: &f$anchorCount")
                add("&8Story event-ul poate interactiona cu questurile existente.")
            } else {
                add("&7Ancore quest: &f0")
            }
            add("")
            add("&7Click &aConfirma&7 pentru a crea evenimentul.")
        }
        context.item(22, GuiItemFactory.item(Material.FILLED_MAP,
            "&6Review Eveniment", reviewLore
        ))

        context.button(38, GuiButton.enabled(
            GuiItemFactory.item(Material.LIME_DYE, "&aConfirma",
                listOf("&7Creaza evenimentul story")),
            GuiAction {
                if (template.id.isNotBlank()) {
                    authoring.applyTemplate(template.id, scopeId)
                } else {
                    authoring.recordEvent(StoryEventDraft(
                        scopeType = if (place != null) "place" else "region",
                        scopeId = scopeId,
                        regionId = region?.id() ?: "",
                        placeId = place?.id() ?: "",
                        eventType = eventType,
                        title = "Eveniment ${eventType}",
                        description = "Creat din StoryAuthoringGUI",
                        actorType = "player",
                        actorId = context.player().name
                    ))
                }
                selectedTemplate = null; selectedRegion = null; selectedPlace = null; step = 0
                context.service().open(context.player(), GuiKey.STORY)
            }
        ))

        if (anchorCount > 0) {
            context.button(39, GuiButton.enabled(
                GuiItemFactory.item(Material.FILLED_MAP, "&6Quest Map",
                    listOf("&7Vezi ancorele quest pentru aceasta locatie.",
                        "&7Ancore existente: &f$anchorCount")),
                GuiAction { click -> click.service().open(click.player(), GuiKey.QUEST_MAP) }
            ))
        }

        context.button(42, GuiButton.enabled(
            GuiItemFactory.item(Material.RED_DYE, "&cAnuleaza",
                listOf("&7Revino la selectia de template")),
            GuiAction {
                selectedTemplate = null; selectedRegion = null; selectedPlace = null; step = 0
                context.service().open(context.player(), GuiKey.STORY_AUTHORING)
            }
        ))
    }
}
