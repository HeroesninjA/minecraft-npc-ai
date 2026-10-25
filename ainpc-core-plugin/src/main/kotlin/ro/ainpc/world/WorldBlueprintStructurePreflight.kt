package ro.ainpc.world

internal data class WorldBlueprintStructurePreflight(
    val conflicts: List<String>,
    val unknowns: List<String>,
)

internal object WorldBlueprintStructurePreflightInspector {
    fun inspect(
        mappingAvailable: Boolean,
        collisionCount: Int,
        overlappingRegionIds: List<String>?,
        placement: WorldBlueprintPlacementAssessment,
        chunks: WorldBlueprintChunkCoverage,
        rules: WorldBlueprintStructurePreflight = WorldBlueprintStructurePreflight(emptyList(), emptyList()),
        configuration: WorldBlueprintStructurePreflight = WorldBlueprintStructurePreflight(emptyList(), emptyList()),
        architectureTargets: WorldBlueprintArchitectureTargetPreview? = null,
    ): WorldBlueprintStructurePreflight {
        val conflicts = buildList {
            if (collisionCount > 0) add("ID-uri de mapping ocupate")
            if (!overlappingRegionIds.isNullOrEmpty()) add("regiuni mapate suprapuse")
            if (!placement.withinBuildHeight) add("inaltime construibila depasita")
            if (!placement.withinWorldBorder) add("world border depasit")
            addAll(rules.conflicts)
            addAll(configuration.conflicts)
            if (architectureTargets != null && architectureTargets.differentMaterialCount > 0)
                add("blocuri non-aer de inlocuit in plan")
            if (architectureTargets != null && architectureTargets.persistentStateCount > 0)
                add("tinte arhitecturale cu date persistente")
            if ((architectureTargets?.clearances?.get(WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE)?.blockedCount ?: 0) > 0)
                add("intrare arhitecturala blocata")
            if ((architectureTargets?.clearances?.get(WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE)?.blockedCount ?: 0) > 0)
                add("interior arhitectural ocupat")
        }
        val unknowns = buildList {
            if (!mappingAvailable) add("mapping incomplet sau indisponibil")
            if (overlappingRegionIds == null) add("suprapunere regionala necunoscuta")
            if (!chunks.complete) add("chunk-uri nescanate peste limita")
            else if (!chunks.allLoaded) add("chunk-uri neincarcate")
            addAll(rules.unknowns)
            addAll(configuration.unknowns)
            if (architectureTargets != null && architectureTargets.unavailableCount > 0)
                add("tinte arhitecturale neobservabile")
        }
        return WorldBlueprintStructurePreflight(conflicts, unknowns)
    }
}

internal object WorldBlueprintStructureConfigurationPreflightInspector {
    fun inspect(
        preview: WorldBlueprintStructurePreview,
        unknownBiomeIdsByRule: Map<String, List<String>>?,
    ): WorldBlueprintStructurePreflight {
        val conflicts = buildList {
            if (preview.architecturePaletteIssue != null) add("configuratie de paleta arhitecturala incompatibila")
            if (preview.architectureStyleIssue != null) add("configuratie de stil arhitectural incompatibila")
            if (preview.storyRegionRuleIssue != null) add("referinte regionale de poveste invalide")
            if (preview.architecturePlanIssue != null) add("sablon arhitectural necompilabil")
            unknownBiomeIdsByRule.orEmpty().toSortedMap().forEach { (key, ids) ->
                if (ids.isNotEmpty()) add("$key: biomi inexistenti in registru")
            }
        }
        val unknowns = if (unknownBiomeIdsByRule == null) listOf("registrul de biomi indisponibil") else emptyList()
        return WorldBlueprintStructurePreflight(conflicts, unknowns)
    }
}

internal object WorldBlueprintStructureRulePreflightInspector {
    fun inspect(
        mapRules: Map<String, String>,
        bounds: WorldBlueprintRegionBounds,
        terrain: WorldBlueprintTerrainPreview,
        foundation: WorldBlueprintFoundationPreview?,
        occupancy: WorldBlueprintBlockOccupancyPreview?,
        biomes: WorldBlueprintBiomePreview?,
        exactFoundation: WorldBlueprintFoundationPreview? = null,
        isValidBlock: (String) -> Boolean = { WorldBlueprintForbiddenStructureBlocks.isValidBlock(it) },
    ): WorldBlueprintStructurePreflight {
        val conflicts = mutableListOf<String>()
        val unknowns = mutableListOf<String>()
        fun record(key: String, valid: Boolean, exceeds: Boolean?, complete: Boolean = true) {
            when {
                !valid -> conflicts += "$key invalid"
                exceeds == true -> conflicts += "$key incalcat"
                exceeds == null || !complete -> unknowns += "$key neverificat complet"
            }
        }

        val positions = WorldBlueprintStructureRegionPositionsRule.parse(mapRules)
        if (positions.configured) record(WorldBlueprintStructureRegionPositionsRule.KEY,
            positions.isValid, positions.exceeds(bounds))

        val surface = WorldBlueprintStructureSurfaceSpanRule.parse(mapRules)
        if (surface.configured) record(WorldBlueprintStructureSurfaceSpanRule.KEY,
            surface.isValid, surface.exceeds(terrain),
            terrain.coversEntireFootprint && terrain.observedCount == terrain.sampleCount)

        val support = WorldBlueprintBaseSupportRule.parse(mapRules)
        if (support.configured) record(WorldBlueprintBaseSupportRule.KEY,
            support.isValid, support.exceeds(foundation))

        val basePositions = WorldBlueprintBaseUnsupportedPositionsRule.parse(mapRules)
        if (basePositions.configured) record(WorldBlueprintBaseUnsupportedPositionsRule.KEY,
            basePositions.isValid, basePositions.exceeds(bounds, exactFoundation ?: foundation))

        val nonAir = WorldBlueprintStructureNonAirRule.parse(mapRules)
        if (nonAir.configured) record(WorldBlueprintStructureNonAirRule.KEY,
            nonAir.isValid, nonAir.exceeds(bounds, occupancy))

        val fluids = WorldBlueprintStructureFluidRule.parse(mapRules)
        if (fluids.configured) record(WorldBlueprintStructureFluidRule.KEY,
            fluids.isValid, fluids.exceeds(bounds, occupancy))

        val forbidden = WorldBlueprintForbiddenStructureBlocks.parse(mapRules, isValidBlock)
        if (forbidden.configured) {
            val violations = occupancy?.let { forbidden.observedViolations(it.materialCounts) }
            record(WorldBlueprintForbiddenStructureBlocks.KEY, forbidden.isValid,
                violations?.isNotEmpty(), occupancy?.coversEntireVolume == true && occupancy.unknownCount == 0)
        }

        val biomeRules = WorldBlueprintStructureBiomeRules.parse(mapRules)
        if (!biomeRules.isValid) {
            conflicts += "reguli de biom invalide"
        } else if (biomeRules.isConfigured) {
            val violations = biomes?.takeIf { it.observedCount > 0 }
                ?.let { biomeRules.violations(it.biomeCounts.keys) }
            record("reguli de biom", true,
                violations?.let { it.first.isNotEmpty() || it.second.isNotEmpty() },
                biomes?.coversEntireVolume == true && biomes.observedCount == biomes.sampleCount)
        }
        return WorldBlueprintStructurePreflight(conflicts, unknowns)
    }
}
