package ro.ainpc.settlement

import ro.ainpc.api.settlement.BuildingAnchorDefinition
import ro.ainpc.api.settlement.BuildingTemplateDefinition

object BuildingTemplateRegistry {
    private val templates: MutableMap<String, BuildingTemplateDefinition> = LinkedHashMap()

    fun register(template: BuildingTemplateDefinition) {
        templates[template.templateId] = template
    }

    fun get(id: String): BuildingTemplateDefinition? = templates[id]

    fun getAll(): Collection<BuildingTemplateDefinition> = templates.values

    fun count(): Int = templates.size

    fun clear() {
        templates.clear()
    }

    fun loadDefaults() {
        register(BuildingTemplateDefinition(
            templateId = "house_small",
            displayName = "Casa mica",
            placeType = "house",
            footprintWidth = 5, footprintDepth = 5, footprintHeight = 4,
            anchors = listOf(
                BuildingAnchorDefinition("bed", "bed", 2, 1, 2),
                BuildingAnchorDefinition("entrance", "entrance", 2, 0, 0, 1.5, "main"),
                BuildingAnchorDefinition("spawn", "npc_spawn", 2, 0, 2, 1.0, "spawn")
            ),
            supportedRotations = listOf(0, 90, 180, 270)
        ))
        register(BuildingTemplateDefinition(
            templateId = "forge",
            displayName = "Fierarie",
            placeType = "forge",
            footprintWidth = 7, footprintDepth = 6, footprintHeight = 5,
            anchors = listOf(
                BuildingAnchorDefinition("workstation", "workstation", 3, 1, 3),
                BuildingAnchorDefinition("entrance", "entrance", 3, 0, 0, 2.0, "main"),
                BuildingAnchorDefinition("spawn", "npc_spawn", 3, 0, 3, 1.0, "spawn")
            ),
            supportedRotations = listOf(0, 90, 180, 270)
        ))
        register(BuildingTemplateDefinition(
            templateId = "farm",
            displayName = "Ferma",
            placeType = "farm",
            footprintWidth = 10, footprintDepth = 8, footprintHeight = 4,
            anchors = listOf(
                BuildingAnchorDefinition("workstation", "workstation", 5, 1, 4),
                BuildingAnchorDefinition("entrance", "entrance", 5, 0, 0, 2.0, "main"),
                BuildingAnchorDefinition("spawn", "npc_spawn", 5, 0, 4, 1.0, "spawn")
            ),
            supportedRotations = listOf(0, 90)
        ))
    }
}
