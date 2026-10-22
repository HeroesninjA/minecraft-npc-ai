package ro.ainpc.world

import ro.ainpc.world.exterior.ExteriorStructureBlueprintCatalog
import ro.ainpc.world.exterior.ExteriorStructureType
import java.util.Locale

internal data class WorldBlueprintStructurePalettes(
    val byType: Map<ExteriorStructureType, List<String>>,
    val error: String?,
) {
    companion object {
        const val KEY = "architecture_palette_by_type"

        fun parse(
            raw: String?,
            isBuildableBlock: (String) -> Boolean = WorldBlueprintArchitecturePalette::isBuildableBlock,
        ): WorldBlueprintStructurePalettes? {
            if (raw == null) return null
            val entries = raw.split(';')
            if (entries.size > ExteriorStructureType.entries.size - 1) {
                return invalid("foloseste cel mult 12 tipuri de structura separate prin punct si virgula")
            }
            val palettes = linkedMapOf<ExteriorStructureType, List<String>>()
            entries.forEach { entry ->
                val pair = entry.split('=', limit = 2)
                val typeId = pair.first().trim().lowercase(Locale.ROOT)
                val type = ExteriorStructureType.entries.firstOrNull { it.id() == typeId }
                if (pair.size != 2 || type == null || ExteriorStructureBlueprintCatalog.get(type) == null) {
                    return invalid("foloseste tipuri canonice din catalog in forma tip=bloc,bloc")
                }
                if (type in palettes) return invalid("tipul $typeId apare de mai multe ori")
                val palette = WorldBlueprintArchitecturePalette.parse(pair[1], isBuildableBlock)
                if (palette?.error != null) return invalid("$typeId: ${palette.error}")
                palettes[type] = requireNotNull(palette).blockIds
            }
            return WorldBlueprintStructurePalettes(palettes, null)
        }

        private fun invalid(reason: String) = WorldBlueprintStructurePalettes(emptyMap(), "$KEY: $reason")
    }
}
