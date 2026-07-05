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

class MappingCreateNodeGui : GuiScreen {
    private val nodeTypes = listOf("interaction", "npc_spawn", "bed", "home", "work", "workstation", "social", "meeting_point", "quest_trigger", "entrance", "ritual_circle", "altar")

    override fun key(): GuiKey = GuiKey.MAPPING_CREATE_NODE
    override fun title(player: Player): String = "&0Creaza Node"
    override fun size(player: Player): Int = 36

    override fun render(context: GuiRenderContext) {
        val loc = context.player().location
        val wa = context.plugin().platform.worldAdmin
        val aiMode = context.service().getCreatorFormValue(context.player(), "mc_ai_mode").ifBlank { "" }
        val aiKind = context.service().getCreatorFormValue(context.player(), "mc_ai_kind").ifBlank { "" }
        val aiName = context.service().getCreatorFormValue(context.player(), "mc_ai_name").ifBlank { "" }
        val aiSummary = context.service().getCreatorFormValue(context.player(), "mc_ai_summary").ifBlank { "" }
        val aiNodeName = context.service().getCreatorFormValue(context.player(), "mc_ai_node_name").ifBlank { "" }
        val aiNodeType = context.service().getCreatorFormValue(context.player(), "mc_ai_node_type").ifBlank { "" }
        val aiNodeRadius = context.service().getCreatorFormValue(context.player(), "mc_ai_node_radius").ifBlank { "" }
        val nodeX = context.service().getCreatorFormValue(context.player(), "node_x").toDoubleOrNull() ?: loc.x
        val nodeY = context.service().getCreatorFormValue(context.player(), "node_y").toDoubleOrNull() ?: loc.y
        val nodeZ = context.service().getCreatorFormValue(context.player(), "node_z").toDoubleOrNull() ?: loc.z
        val nodeName = context.service().getCreatorFormValue(context.player(), "node_name")
            .ifBlank { "node_${loc.blockX}_${loc.blockZ}" }
        val nodeType = context.service().getCreatorFormValue(context.player(), "node_type")
            .ifBlank { "interaction" }
        val radius = context.service().getCreatorFormValue(context.player(), "node_radius").ifBlank { "2.0" }
        val radiusValue = radius.toDoubleOrNull()
        val nodeValid = nodeName.isNotBlank() && nodeType.isNotBlank() && radiusValue != null && radiusValue in 0.5..5.0
        val nodeIssue = when {
            nodeName.isBlank() -> "&cLipseste numele node-ului."
            nodeType.isBlank() -> "&cLipseste tipul node-ului."
            radiusValue == null -> "&cRaza trebuie sa fie un numar."
            radiusValue < 0.5 -> "&cRaza minima este 0.5."
            radiusValue > 5.0 -> "&cRaza maxima este 5.0."
            else -> "&aValid pentru creare."
        }
        val currentRegion = wa.findRegion(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)
        val currentPlace = wa.findPlace(loc.world.name, loc.blockX, loc.blockY, loc.blockZ)
        val conflictWarnings = nodeCreateConflictWarnings(
            wa,
            loc.world.name,
            currentRegion?.id().orEmpty(),
            currentPlace?.id(),
            nodeName,
            nodeX,
            nodeY,
            nodeZ,
            radiusValue ?: 2.0
        )
        val canCreate = nodeValid && conflictWarnings.isEmpty()
        val duplicateNodeName = conflictWarnings.any { it.contains("acest id", ignoreCase = true) || it.contains("numele", ignoreCase = true) }
        val safeNodeRadius = suggestSafeNodeRadius(
            wa,
            loc.world.name,
            currentRegion?.id().orEmpty(),
            currentPlace?.id(),
            nodeName,
            nodeX,
            nodeY,
            nodeZ,
            radiusValue ?: 2.0
        )
        val safeNodeLocation = suggestSafeNodeLocation(
            wa,
            loc.world.name,
            currentRegion?.id().orEmpty(),
            currentPlace?.id(),
            nodeName,
            nodeX,
            nodeY,
            nodeZ,
            radiusValue ?: 2.0
        )

        context.item(4, GuiItemFactory.item(Material.TARGET, "&6Creaza Node", listOf(
            "&7Completeaza si apasa Creaza.",
            if (aiMode.isBlank()) "&7AI mode: &f(nu)" else "&7AI mode: &f$aiMode",
            if (aiKind.isBlank()) "&7AI kind: &f(nu)" else "&7AI kind: &f$aiKind",
            if (aiName.isBlank()) "&7AI name: &f(nu)" else "&7AI name: &f$aiName",
            if (aiSummary.isBlank()) "&7AI summary: &f(nu)" else "&7AI summary: &f$aiSummary"
        )))

        context.button(10, GuiButton.enabled(
            GuiItemFactory.item(Material.NAME_TAG, "&eNume: &f$nodeName", listOf("&7Click: schimba prefixul.")),
            GuiAction { click ->
                val newName = if (nodeName.startsWith("node_")) "pct_${nodeName.removePrefix("node_")}" else "node_${loc.blockX}_${loc.blockZ}"
                context.service().setCreatorFormValue(click.player(), "node_name", newName)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE)
            }
        ))

        context.button(11, GuiButton.enabled(
            GuiItemFactory.item(Material.SPYGLASS, "&eTip: &f$nodeType", listOf("&7Click: schimba tipul.")),
            GuiAction { click ->
                val idx = nodeTypes.indexOf(nodeType)
                val next = nodeTypes[(idx + 1) % nodeTypes.size]
                context.service().setCreatorFormValue(click.player(), "node_type", next)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE)
            }
        ))

        context.button(12, GuiButton.enabled(
            GuiItemFactory.item(Material.COMPASS, "&eRaza: &f$radius", listOf("&7Click: mareste raza cu 0.5.")),
            GuiAction { click ->
                val r = (radius.toDoubleOrNull() ?: 2.0) + 0.5
                context.service().setCreatorFormValue(click.player(), "node_radius",
                    (if (r > 5.0) 1.0 else r).toString())
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE)
            }
        ))

        context.button(13, GuiButton.enabled(
            GuiItemFactory.item(Material.MAGENTA_DYE, "&dAI refine node", listOf(
                "&7Regenereaza presetul AI din campurile curente.",
                "&7Click: ruleaza world create ai cu hint-uri de node."
            )),
            GuiAction { click ->
                val command = buildNodeAiRefineCommand(aiNodeName, aiNodeType, aiNodeRadius, nodeName, nodeType, radius, aiMode, aiKind, aiName, aiSummary)
                click.service().runCommand(click.player(), command)
            }
        ))

        context.button(14, GuiButton.enabled(
            if (canCreate) GuiItemFactory.item(Material.LIME_DYE, "&aCreaza Node", listOf(
                "&7Node: $nodeName ($nodeType)",
                "&7La: ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}",
                "&7Raza: $radius",
                "&7Click: creeaza."
            )) else GuiItemFactory.item(Material.GRAY_DYE, "&7Creaza Node", listOf(
                "&7Node: $nodeName ($nodeType)",
                "&7La: ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}",
                "&7Raza: $radius",
                nodeIssue,
                *conflictWarnings.map { "&c$it" }.toTypedArray()
            )),
            if (canCreate) GuiAction { click ->
                click.service().runCommand(click.player(),
                    "ainpc world node create ${nodeName} ${nodeType} $nodeX $nodeY $nodeZ $radius")
            } else null
        ))

        context.button(15, GuiButton.enabled(
            GuiItemFactory.item(Material.MAGENTA_DYE, "&dReaplica AI", listOf(
                "&7Reface campurile initiale din sugestia AI.",
                "&7Nume: ${if (aiNodeName.isBlank()) nodeName else aiNodeName}",
                "&7Tip: ${if (aiNodeType.isBlank()) nodeType else aiNodeType}",
                "&7Raza: ${if (aiNodeRadius.isBlank()) radius else aiNodeRadius}"
            )),
            GuiAction { click ->
                if (aiNodeName.isNotBlank()) context.service().setCreatorFormValue(click.player(), "node_name", aiNodeName)
                if (aiNodeType.isNotBlank()) context.service().setCreatorFormValue(click.player(), "node_type", aiNodeType)
                if (aiNodeRadius.isNotBlank()) context.service().setCreatorFormValue(click.player(), "node_radius", aiNodeRadius)
                click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE)
            }
        ))

        if (duplicateNodeName) {
            context.button(16, GuiButton.enabled(
                GuiItemFactory.item(Material.ANVIL, "&eAuto rename", listOf(
                    "&7Genereaza un nume unic pentru node.",
                    "&7Nu modifica tipul sau raza."
                )),
                GuiAction { click ->
                    val nextName = suggestUniqueNodeId(wa, nodeName)
                    context.service().setCreatorFormValue(click.player(), "node_name", nextName)
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE)
                }
            ))
        }

        if (safeNodeRadius != null && safeNodeRadius < (radiusValue ?: 2.0)) {
            context.button(17, GuiButton.enabled(
                GuiItemFactory.item(Material.LIME_DYE, "&aAuto fit", listOf(
                    "&7Micșoreaza raza pana cand nu mai intra in conflict.",
                    "&7Noua raza: $safeNodeRadius"
                )),
                GuiAction { click ->
                    context.service().setCreatorFormValue(click.player(), "node_radius", safeNodeRadius.toString())
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE)
                }
            ))
        }

        if (safeNodeLocation != null && (
            safeNodeLocation.x != nodeX || safeNodeLocation.y != nodeY || safeNodeLocation.z != nodeZ
        )) {
            context.button(18, GuiButton.enabled(
                GuiItemFactory.item(Material.ENDER_PEARL, "&bAuto move", listOf(
                    "&7Mută node-ul la o poziție liberă.",
                    "&7Noua pozitie: ${safeNodeLocation.x.toInt()}, ${safeNodeLocation.y.toInt()}, ${safeNodeLocation.z.toInt()}"
                )),
                GuiAction { click ->
                    context.service().setCreatorFormValue(click.player(), "node_x", safeNodeLocation.x.toString())
                    context.service().setCreatorFormValue(click.player(), "node_y", safeNodeLocation.y.toString())
                    context.service().setCreatorFormValue(click.player(), "node_z", safeNodeLocation.z.toString())
                    click.service().open(click.player(), GuiKey.MAPPING_CREATE_NODE)
                }
            ))
        }

        GuiNavigation.addStandardControls(context, key())
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun buildNodeAiRefineCommand(
        aiNodeName: String,
        aiNodeType: String,
        aiNodeRadius: String,
        nodeName: String,
        nodeType: String,
        radius: String,
        aiMode: String,
        aiKind: String,
        aiName: String,
        aiSummary: String
    ): String {
        val hints = mutableListOf<String>()
        fun addHint(key: String, value: String?) {
            val normalized = value.orEmpty().trim()
            if (normalized.isNotBlank()) {
                hints += "$key=${normalized.replace(' ', '_')}"
            }
        }

        addHint("kind", if (aiKind.isBlank()) "node" else aiKind)
        addHint("name", if (aiNodeName.isBlank()) nodeName else aiNodeName)
        addHint("type", if (aiNodeType.isBlank()) nodeType else aiNodeType)
        addHint("radius", if (aiNodeRadius.isBlank()) radius else aiNodeRadius)
        addHint("mode", aiMode)
        addHint("ai_name", aiName)
        addHint("summary", aiSummary)

        return buildString {
            append("ainpc world create ai node ")
            append(nodeName)
            append(' ')
            append(hints.joinToString(" "))
        }.trim()
    }
}
