package ro.ainpc.spawn

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.HexFormat
import java.util.Locale

object SpawnBatchPlanHasher {
    @JvmStatic
    fun settlementBatchKey(allocations: List<HouseAllocation>?): String {
        val scope = settlementScopeId(allocations)
        return "settlement:${normalizeToken(scope)}:${shortHash(settlementPlanHash(allocations))}"
    }

    @JvmStatic
    fun dryRunSettlementBatchKey(allocations: List<HouseAllocation>?): String {
        val scope = settlementScopeId(allocations)
        return "dryrun:settlement:${normalizeToken(scope)}:${shortHash(settlementPlanHash(allocations))}"
    }

    @JvmStatic
    fun householdBatchKey(allocation: HouseAllocation?): String {
        val scope = allocation?.placeId() ?: "unknown"
        return "household:${normalizeToken(scope)}:${shortHash(householdPlanHash(allocation))}"
    }

    @JvmStatic
    fun dryRunHouseholdBatchKey(allocation: HouseAllocation?): String {
        val scope = allocation?.placeId() ?: "unknown"
        return "dryrun:household:${normalizeToken(scope)}:${shortHash(householdPlanHash(allocation))}"
    }

    @JvmStatic
    fun settlementScopeId(allocations: List<HouseAllocation>?): String {
        val safeAllocations = allocations?.toList().orEmpty()
        if (safeAllocations.isEmpty()) {
            return "unknown"
        }

        var commonRegion = ""
        var first = true
        for (allocation in safeAllocations) {
            val region = regionFromPlaceId(allocation.placeId())
            if (first) {
                commonRegion = region
                first = false
            } else if (commonRegion != region) {
                return "mixed"
            }
        }
        return commonRegion.ifBlank { "unknown" }
    }

    @JvmStatic
    fun settlementPlanHash(allocations: List<HouseAllocation>?): String {
        val canonical = StringBuilder("settlement\n")
        val sortedAllocations = allocations?.toList().orEmpty()
            .sortedWith(
                compareBy<HouseAllocation> { it.placeId() }
                    .thenBy { it.familyId() }
                    .thenBy { it.primaryOwnerNpcKey() }
            )

        for (allocation in sortedAllocations) {
            appendAllocation(canonical, allocation)
        }
        return sha256(canonical.toString())
    }

    @JvmStatic
    fun householdPlanHash(allocation: HouseAllocation?): String {
        val canonical = StringBuilder("household\n")
        if (allocation != null) {
            appendAllocation(canonical, allocation)
        }
        return sha256(canonical.toString())
    }

    private fun appendAllocation(canonical: StringBuilder, allocation: HouseAllocation) {
        canonical.append("allocation|")
            .append(clean(allocation.placeId())).append('|')
            .append(clean(allocation.familyId())).append('|')
            .append(clean(allocation.primaryOwnerNpcKey())).append('|')
            .append(allocation.maxResidents()).append('\n')

        allocation.residentPlans()
            .sortedWith(
                compareBy<HouseAllocation.ResidentPlan> { it.npcKey() }
                    .thenBy { it.name() }
            )
            .forEach { resident -> appendResident(canonical, resident) }
    }

    private fun appendResident(canonical: StringBuilder, resident: HouseAllocation.ResidentPlan) {
        canonical.append("resident|")
            .append(clean(resident.npcKey())).append('|')
            .append(clean(resident.name())).append('|')
            .append(clean(resident.relationRole())).append('|')
            .append(clean(resident.occupation())).append('|')
            .append(clean(resident.backstory())).append('|')
            .append(resident.age()).append('|')
            .append(clean(resident.gender())).append('|')
            .append(clean(resident.archetype())).append('|')
            .append(clean(resident.spawnNodeId())).append('|')
            .append(clean(resident.homeNodeId())).append('|')
            .append(clean(resident.bedNodeId())).append('|')
            .append(clean(resident.workPlaceId())).append('|')
            .append(clean(resident.workNodeId())).append('|')
            .append(clean(resident.socialPlaceId())).append('|')
            .append(clean(resident.socialNodeId())).append('\n')
    }

    private fun regionFromPlaceId(placeId: String?): String {
        val cleanPlaceId = clean(placeId)
        val separator = cleanPlaceId.indexOf(':')
        return if (separator > 0) cleanPlaceId.substring(0, separator) else cleanPlaceId
    }

    private fun normalizeToken(value: String?): String {
        val cleaned = clean(value).lowercase(Locale.ROOT)
        if (cleaned.isBlank()) {
            return "unknown"
        }
        return cleaned.replace(Regex("[^a-z0-9_.:-]+"), "_")
    }

    private fun shortHash(hash: String): String = if (hash.length <= 16) hash else hash.substring(0, 16)

    private fun sha256(value: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashed = digest.digest(value.toByteArray(StandardCharsets.UTF_8))
            return HexFormat.of().formatHex(hashed)
        } catch (exception: NoSuchAlgorithmException) {
            throw IllegalStateException("SHA-256 indisponibil", exception)
        }
    }

    private fun clean(value: String?): String = value?.trim().orEmpty()
}
