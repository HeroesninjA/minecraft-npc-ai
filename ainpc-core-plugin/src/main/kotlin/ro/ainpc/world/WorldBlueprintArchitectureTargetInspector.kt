package ro.ainpc.world

import java.util.Locale

internal data class WorldBlueprintArchitectureObservedBlock(
    val isNonAir: Boolean,
    val materialId: String?,
    val hasPersistentState: Boolean,
)

internal data class WorldBlueprintArchitectureClearancePreview(
    val totalCount: Int,
    val blockedCount: Int,
    val unavailableCount: Int,
) {
    val airCount: Int get() = totalCount - blockedCount - unavailableCount
}

internal data class WorldBlueprintArchitectureTargetPreview(
    val totalCount: Int,
    val matchingMaterialCount: Int,
    val airCount: Int,
    val differentMaterialCount: Int,
    val unavailableCount: Int,
    val outsideBuildHeightCount: Int,
    val replacementMaterialCounts: Map<String, Int>,
    val persistentStateCount: Int,
    val persistentStateMaterialCounts: Map<String, Int>,
    val clearances: Map<WorldBlueprintArchitectureBlockPurpose, WorldBlueprintArchitectureClearancePreview>,
) {
    val clearanceCount: Int get() = clearances.values.sumOf { it.totalCount }
    val clearanceBlockedCount: Int get() = clearances.values.sumOf { it.blockedCount }
    val clearanceUnavailableCount: Int get() = clearances.values.sumOf { it.unavailableCount }
    val clearanceAirCount: Int get() = clearanceCount - clearanceBlockedCount - clearanceUnavailableCount
}

internal object WorldBlueprintArchitectureTargetInspector {
    fun inspect(
        plan: WorldBlueprintArchitecturePlan,
        minBuildY: Int,
        maxBuildYExclusive: Int,
        blockAt: (Int, Int, Int) -> WorldBlueprintArchitectureObservedBlock?,
    ): WorldBlueprintArchitectureTargetPreview {
        require(minBuildY < maxBuildYExclusive) { "Limitele verticale ale lumii sunt invalide." }
        require(plan.blocks.size <= WorldBlueprintArchitectureTemplate.MAX_BLOCKS) { "Planul depaseste limita de preview." }
        var matching = 0
        var air = 0
        var different = 0
        var unavailable = 0
        var outsideHeight = 0
        var persistentStates = 0
        val clearances = mutableMapOf<WorldBlueprintArchitectureBlockPurpose, WorldBlueprintArchitectureClearancePreview>()
        fun recordClearance(purpose: WorldBlueprintArchitectureBlockPurpose, observed: WorldBlueprintArchitectureObservedBlock?) {
            if (purpose == WorldBlueprintArchitectureBlockPurpose.BUILD) return
            val previous = clearances[purpose] ?: WorldBlueprintArchitectureClearancePreview(0, 0, 0)
            clearances[purpose] = WorldBlueprintArchitectureClearancePreview(previous.totalCount + 1,
                previous.blockedCount + if (observed?.isNonAir == true) 1 else 0,
                previous.unavailableCount + if (observed == null) 1 else 0)
        }
        val replacements = mutableMapOf<String, Int>()
        val persistentMaterials = mutableMapOf<String, Int>()
        for (target in plan.blocks) {
            if (target.y < minBuildY || target.y >= maxBuildYExclusive) {
                outsideHeight++
                recordClearance(target.purpose, null)
                continue
            }
            val observed = blockAt(target.x, target.y, target.z)
            recordClearance(target.purpose, observed)
            if (observed?.hasPersistentState == true) {
                persistentStates++
                observed.materialId?.let { materialId ->
                    val canonicalId = canonical(materialId)
                    persistentMaterials[canonicalId] = (persistentMaterials[canonicalId] ?: 0) + 1
                }
            }
            when {
                observed == null -> unavailable++
                !observed.isNonAir -> air++
                observed.materialId == null -> unavailable++
                canonical(observed.materialId) == canonical(target.blockId) -> matching++
                else -> {
                    different++
                    val materialId = canonical(observed.materialId)
                    replacements[materialId] = (replacements[materialId] ?: 0) + 1
                }
            }
        }
        return WorldBlueprintArchitectureTargetPreview(plan.blocks.size, matching, air, different,
            unavailable, outsideHeight, replacements.toSortedMap(), persistentStates, persistentMaterials.toSortedMap(),
            clearances.toSortedMap())
    }

    private fun canonical(materialId: String): String {
        val normalized = materialId.lowercase(Locale.ROOT)
        return if (':' in normalized) normalized else "minecraft:$normalized"
    }
}
