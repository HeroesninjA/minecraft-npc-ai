package ro.ainpc.commands

import ro.ainpc.world.WorldBlueprintRegionBounds

internal data class WorldBlueprintStructurePlanRequest(
    val typeId: String,
    val baseId: String,
    val bounds: WorldBlueprintRegionBounds?,
    val exportFingerprint: String?,
    val verifyFingerprint: String? = null,
) {
    companion object {
        private val exportFingerprintPattern = Regex("[0-9a-fA-F]{64}")

        fun parse(args: Array<String>): WorldBlueprintStructurePlanRequest? {
            if (args.size != 7 && args.size != 14 && args.size != 16) return null
            if (!args[0].equals("world", ignoreCase = true)
                || !args[1].equals("blueprint", ignoreCase = true)
                || !args[3].equals("plan", ignoreCase = true)
                || !args[4].equals("structure", ignoreCase = true)) return null
            if (args.size >= 14 && !args[7].equals("bounds", ignoreCase = true)) return null
            if (args.size == 16 && !args[14].equals("export", ignoreCase = true) &&
                !args[14].equals("verify", ignoreCase = true)) return null

            val bounds = if (args.size >= 14) WorldBlueprintRegionBounds.parse(args.slice(8..13)) else null
            val fingerprint = if (args.size == 16) {
                require(exportFingerprintPattern.matches(args[15])) {
                    "Amprenta exportului trebuie sa contina exact 64 de caractere hexazecimale (SHA-256)."
                }
                args[15].lowercase()
            } else null
            return WorldBlueprintStructurePlanRequest(args[5], args[6], bounds,
                fingerprint.takeIf { args.getOrNull(14)?.equals("export", ignoreCase = true) == true },
                fingerprint.takeIf { args.getOrNull(14)?.equals("verify", ignoreCase = true) == true })
        }
    }
}
