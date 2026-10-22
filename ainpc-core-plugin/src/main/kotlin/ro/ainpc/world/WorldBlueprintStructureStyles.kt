package ro.ainpc.world

import ro.ainpc.world.exterior.ExteriorStructureBlueprintCatalog
import ro.ainpc.world.exterior.ExteriorStructureType
import java.util.Locale

internal data class WorldBlueprintStructureStyles(
    val byType: Map<ExteriorStructureType, String>,
    val error: String?,
) {
    companion object {
        const val KEY = "architecture_style_by_type"
        private const val MAX_STYLE_LENGTH = 200

        fun parse(raw: String?): WorldBlueprintStructureStyles? {
            if (raw == null) return null
            val entries = raw.split(';')
            if (entries.size > ExteriorStructureType.entries.size - 1) {
                return invalid("foloseste cel mult 12 tipuri de structura separate prin punct si virgula")
            }
            val styles = linkedMapOf<ExteriorStructureType, String>()
            entries.forEach { entry ->
                val pair = entry.split('=', limit = 2)
                val typeId = pair.first().trim().lowercase(Locale.ROOT)
                val type = ExteriorStructureType.entries.firstOrNull { it.id() == typeId }
                if (pair.size != 2 || type == null || ExteriorStructureBlueprintCatalog.get(type) == null) {
                    return invalid("foloseste tipuri canonice din catalog in forma tip=descriere")
                }
                if (type in styles) return invalid("tipul $typeId apare de mai multe ori")
                val description = pair[1].trim()
                if (description.isBlank() || description.length > MAX_STYLE_LENGTH ||
                    description.any { it.isISOControl() }) {
                    return invalid("$typeId: descrierea trebuie sa aiba 1..$MAX_STYLE_LENGTH caractere, fara caractere de control")
                }
                styles[type] = description
            }
            return WorldBlueprintStructureStyles(styles, null)
        }

        private fun invalid(reason: String) = WorldBlueprintStructureStyles(emptyMap(), "$KEY: $reason")
    }
}
