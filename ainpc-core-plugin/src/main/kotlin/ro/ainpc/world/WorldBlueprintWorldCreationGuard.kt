package ro.ainpc.world

internal object WorldBlueprintWorldCreationGuard {
    fun issue(
        blueprint: WorldBlueprint?, currentFingerprint: String?, expectedFingerprint: String,
        worldLoaded: Boolean, directoryStatus: WorldBlueprintWorldDirectoryStatus,
    ): String? {
        if (blueprint == null || currentFingerprint == null) return "Nu exista draft pentru aceasta lume."
        if (worldLoaded) return "Lumea este deja incarcata; comanda nu reincarca lumi."
        val completionIssues = WorldBlueprintPolicy.completionIssues(blueprint)
        if (completionIssues.isNotEmpty()) return "Draft incomplet: ${completionIssues.joinToString("; ")}"
        if (blueprint.approval?.fingerprint != currentFingerprint) {
            return "Draftul curent nu este confirmat; ruleaza review si confirm."
        }
        if (expectedFingerprint != currentFingerprint) {
            return "Amprenta nu corespunde reviziei confirmate; ruleaza review din nou."
        }
        if (!WorldBlueprintWorldSeedRule.parse(blueprint.mapRules).isValid) {
            return "Regula world_seed este invalida; foloseste un intreg zecimal pe 64 de biti."
        }
        val invalidGenerationRules = WorldBlueprintWorldGenerationRules.parse(blueprint.mapRules).invalidKeys
        if (invalidGenerationRules.isNotEmpty()) {
            return "Reguli de generare invalide: ${invalidGenerationRules.joinToString()}; consulta plan."
        }
        return when (directoryStatus) {
            WorldBlueprintWorldDirectoryStatus.AVAILABLE -> null
            WorldBlueprintWorldDirectoryStatus.UNSAFE_NAME -> "Numele nu este portabil pentru crearea unei lumi noi; consulta plan."
            WorldBlueprintWorldDirectoryStatus.CONTAINER_UNAVAILABLE -> "Containerul lumilor este indisponibil."
            WorldBlueprintWorldDirectoryStatus.EXISTING_DIRECTORY -> "Directorul lumii exista deja; comanda nu incarca lumi existente."
            WorldBlueprintWorldDirectoryStatus.EXISTING_FILE -> "Exista deja un fisier cu acest nume in containerul lumilor."
            WorldBlueprintWorldDirectoryStatus.SYMBOLIC_LINK -> "Exista deja o legatura simbolica cu acest nume."
            WorldBlueprintWorldDirectoryStatus.UNKNOWN -> "Starea directorului lumii nu poate fi determinata."
        }
    }
}
