package ro.ainpc.spawn

import org.bukkit.Location
import ro.ainpc.AINPCPlugin
import ro.ainpc.world.scan.SemanticVillageMapper
import ro.ainpc.world.scan.VanillaVillageScanner

class AutoSettlementGenerator(private val plugin: AINPCPlugin) {
    private val scanner = VanillaVillageScanner()
    private val mapper = SemanticVillageMapper()
    private val planner = HouseAllocationPlanner()

    data class AutoSettlementResult(
        val regionId: String,
        val scanWarnings: List<String>,
        val scanErrors: List<String>,
        val createdPlaceIds: List<String>,
        val createdNodeIds: List<String>,
        val allocations: List<HouseAllocation>,
        val planningWarnings: List<String>,
        val planningErrors: List<String>
    ) {
        val success: Boolean get() = scanErrors.isEmpty() && planningErrors.isEmpty()
        val allWarnings: List<String> get() = scanWarnings + planningWarnings
        val allErrors: List<String> get() = scanErrors + planningErrors
    }

    fun generate(
        center: Location?,
        horizontalRadius: Int = VanillaVillageScanner.DEFAULT_HORIZONTAL_RADIUS,
        verticalRadius: Int = VanillaVillageScanner.DEFAULT_VERTICAL_RADIUS,
        requestedRegionId: String? = null,
        maxHouses: Int = 0
    ): AutoSettlementResult {
        val worldAdmin = plugin.platform.worldAdminService
        val scan = scanner.scan(center, horizontalRadius, verticalRadius)
        val scanWarnings = scan.warnings()
        val scanErrors = mutableListOf<String>()

        if (!scan.hasVillageSignals()) {
            scanErrors.add("Scanarea nu a detectat un sat in zona specificata.")
            return AutoSettlementResult(
                regionId = "",
                scanWarnings = scanWarnings,
                scanErrors = scanErrors,
                createdPlaceIds = emptyList(),
                createdNodeIds = emptyList(),
                allocations = emptyList(),
                planningWarnings = emptyList(),
                planningErrors = emptyList()
            )
        }

        val import = mapper.importScan(worldAdmin, scan, requestedRegionId)
        val regionId = import.regionId()

        if (import.errors().isNotEmpty()) {
            return AutoSettlementResult(
                regionId = regionId,
                scanWarnings = scanWarnings + import.warnings(),
                scanErrors = import.errors(),
                createdPlaceIds = import.createdPlaceIds(),
                createdNodeIds = import.createdNodeIds(),
                allocations = emptyList(),
                planningWarnings = emptyList(),
                planningErrors = emptyList()
            )
        }

        if (regionId.isBlank()) {
            scanErrors.add("Nu s-a putut crea regiunea pentru settlement.")
            return AutoSettlementResult(
                regionId = "",
                scanWarnings = scanWarnings,
                scanErrors = scanErrors,
                createdPlaceIds = import.createdPlaceIds(),
                createdNodeIds = import.createdNodeIds(),
                allocations = emptyList(),
                planningWarnings = emptyList(),
                planningErrors = emptyList()
            )
        }

        val planning = planner.planSettlement(worldAdmin, regionId, maxHouses)

        return AutoSettlementResult(
            regionId = regionId,
            scanWarnings = scanWarnings + import.warnings(),
            scanErrors = scanErrors,
            createdPlaceIds = import.createdPlaceIds(),
            createdNodeIds = import.createdNodeIds(),
            allocations = planning.allocations(),
            planningWarnings = planning.warnings(),
            planningErrors = planning.errors()
        )
    }
}
