package ro.ainpc.spawn;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

public final class SpawnBatchPlanHasher {

    private SpawnBatchPlanHasher() {
    }

    public static String settlementBatchKey(List<HouseAllocation> allocations) {
        String scope = settlementScopeId(allocations);
        return "settlement:" + normalizeToken(scope) + ":" + shortHash(settlementPlanHash(allocations));
    }

    public static String dryRunSettlementBatchKey(List<HouseAllocation> allocations) {
        String scope = settlementScopeId(allocations);
        return "dryrun:settlement:" + normalizeToken(scope) + ":" + shortHash(settlementPlanHash(allocations));
    }

    public static String householdBatchKey(HouseAllocation allocation) {
        String scope = allocation == null ? "unknown" : allocation.placeId();
        return "household:" + normalizeToken(scope) + ":" + shortHash(householdPlanHash(allocation));
    }

    public static String dryRunHouseholdBatchKey(HouseAllocation allocation) {
        String scope = allocation == null ? "unknown" : allocation.placeId();
        return "dryrun:household:" + normalizeToken(scope) + ":" + shortHash(householdPlanHash(allocation));
    }

    public static String settlementScopeId(List<HouseAllocation> allocations) {
        List<HouseAllocation> safeAllocations = List.copyOf(allocations != null ? allocations : List.of());
        if (safeAllocations.isEmpty()) {
            return "unknown";
        }

        String commonRegion = "";
        boolean first = true;
        for (HouseAllocation allocation : safeAllocations) {
            String region = regionFromPlaceId(allocation.placeId());
            if (first) {
                commonRegion = region;
                first = false;
            } else if (!commonRegion.equals(region)) {
                return "mixed";
            }
        }
        return commonRegion.isBlank() ? "unknown" : commonRegion;
    }

    public static String settlementPlanHash(List<HouseAllocation> allocations) {
        StringBuilder canonical = new StringBuilder("settlement\n");
        List<HouseAllocation> sortedAllocations = List.copyOf(allocations != null ? allocations : List.of()).stream()
            .sorted(Comparator.comparing(HouseAllocation::placeId)
                .thenComparing(HouseAllocation::familyId)
                .thenComparing(HouseAllocation::primaryOwnerNpcKey))
            .toList();

        for (HouseAllocation allocation : sortedAllocations) {
            appendAllocation(canonical, allocation);
        }
        return sha256(canonical.toString());
    }

    public static String householdPlanHash(HouseAllocation allocation) {
        StringBuilder canonical = new StringBuilder("household\n");
        if (allocation != null) {
            appendAllocation(canonical, allocation);
        }
        return sha256(canonical.toString());
    }

    private static void appendAllocation(StringBuilder canonical, HouseAllocation allocation) {
        canonical.append("allocation|")
            .append(clean(allocation.placeId())).append('|')
            .append(clean(allocation.familyId())).append('|')
            .append(clean(allocation.primaryOwnerNpcKey())).append('|')
            .append(allocation.maxResidents()).append('\n');

        allocation.residentPlans().stream()
            .sorted(Comparator.comparing(HouseAllocation.ResidentPlan::npcKey)
                .thenComparing(HouseAllocation.ResidentPlan::name))
            .forEach(resident -> appendResident(canonical, resident));
    }

    private static void appendResident(StringBuilder canonical, HouseAllocation.ResidentPlan resident) {
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
            .append(clean(resident.socialNodeId())).append('\n');
    }

    private static String regionFromPlaceId(String placeId) {
        String cleanPlaceId = clean(placeId);
        int separator = cleanPlaceId.indexOf(':');
        return separator > 0 ? cleanPlaceId.substring(0, separator) : cleanPlaceId;
    }

    private static String normalizeToken(String value) {
        String cleaned = clean(value).toLowerCase(Locale.ROOT);
        if (cleaned.isBlank()) {
            return "unknown";
        }
        return cleaned.replaceAll("[^a-z0-9_.:-]+", "_");
    }

    private static String shortHash(String hash) {
        return hash.length() <= 16 ? hash : hash.substring(0, 16);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponibil", exception);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
