package ro.ainpc.world

import java.util.Locale

internal data class WorldBlueprintArchitectureMaterialRoles(val floor: String, val wall: String, val roof: String)

internal data class WorldBlueprintArchitectureMaterials(
    val roles: WorldBlueprintArchitectureMaterialRoles?,
    val error: String?,
) {
    companion object {
        const val KEY = "architecture_material_roles"
        private val requiredRoles = setOf("floor", "wall", "roof")

        fun parse(
            raw: String?,
            isBuildableBlock: (String) -> Boolean = WorldBlueprintArchitecturePalette::isBuildableBlock,
        ): WorldBlueprintArchitectureMaterials? {
            if (raw == null) return null
            val entries = raw.split(';')
            if (entries.size != 3) return WorldBlueprintArchitectureMaterials(null,
                "$KEY: defineste exact floor=bloc;wall=bloc;roof=bloc")
            val materials = linkedMapOf<String, String>()
            for (entry in entries) {
                val parts = entry.split('=', limit = 2)
                val role = parts.first().trim().lowercase(Locale.ROOT)
                if (parts.size != 2 || role !in requiredRoles || role in materials) {
                    return WorldBlueprintArchitectureMaterials(null, "$KEY: rol necunoscut, repetat sau fara valoare")
                }
                val palette = WorldBlueprintArchitecturePalette.parse(parts[1], isBuildableBlock)
                val blockId = palette?.blockIds?.singleOrNull()
                if (palette?.error != null || blockId == null) {
                    return WorldBlueprintArchitectureMaterials(null, "$KEY: $role necesita exact un ID de bloc valid")
                }
                materials[role] = blockId
            }
            return WorldBlueprintArchitectureMaterials(WorldBlueprintArchitectureMaterialRoles(
                materials.getValue("floor"), materials.getValue("wall"), materials.getValue("roof")), null)
        }
    }
}
