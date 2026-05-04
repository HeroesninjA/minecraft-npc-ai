package ro.ainpc.world;

import ro.ainpc.npc.AINPC;
import ro.ainpc.spawn.NpcSpawnPlan;

public record NpcWorldBinding(
    int npcId,
    String npcUuid,
    String npcName,
    String homePlaceId,
    String workPlaceId,
    String socialPlaceId,
    String homeNodeId,
    String workNodeId,
    String socialNodeId,
    String familyId,
    String source,
    long createdAt,
    long updatedAt
) {
    public NpcWorldBinding {
        npcUuid = clean(npcUuid);
        npcName = clean(npcName);
        homePlaceId = clean(homePlaceId);
        workPlaceId = clean(workPlaceId);
        socialPlaceId = clean(socialPlaceId);
        homeNodeId = clean(homeNodeId);
        workNodeId = clean(workNodeId);
        socialNodeId = clean(socialNodeId);
        familyId = clean(familyId);
        source = clean(source);
    }

    public static NpcWorldBinding fromSpawnPlan(AINPC npc, NpcSpawnPlan plan, String source) {
        if (npc == null || plan == null) {
            throw new IllegalArgumentException("NPC-ul si planul de spawn sunt obligatorii pentru binding.");
        }

        return new NpcWorldBinding(
            npc.getDatabaseId(),
            npc.getUuid() != null ? npc.getUuid().toString() : "",
            npc.getName(),
            plan.homePlaceId(),
            plan.workPlaceId(),
            plan.socialPlaceId(),
            plan.homeNodeId(),
            plan.workNodeId(),
            plan.socialNodeId(),
            plan.familyId(),
            source,
            0L,
            0L
        );
    }

    public NpcWorldBinding mergeMissingFrom(NpcWorldBinding previous) {
        if (previous == null) {
            return this;
        }

        return new NpcWorldBinding(
            npcId,
            firstNonBlank(npcUuid, previous.npcUuid),
            firstNonBlank(npcName, previous.npcName),
            firstNonBlank(homePlaceId, previous.homePlaceId),
            firstNonBlank(workPlaceId, previous.workPlaceId),
            firstNonBlank(socialPlaceId, previous.socialPlaceId),
            firstNonBlank(homeNodeId, previous.homeNodeId),
            firstNonBlank(workNodeId, previous.workNodeId),
            firstNonBlank(socialNodeId, previous.socialNodeId),
            firstNonBlank(familyId, previous.familyId),
            firstNonBlank(source, previous.source),
            previous.createdAt,
            updatedAt
        );
    }

    public boolean hasAnyPlaceBinding() {
        return !homePlaceId.isBlank() || !workPlaceId.isBlank() || !socialPlaceId.isBlank();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        String cleanPreferred = clean(preferred);
        return cleanPreferred.isBlank() ? clean(fallback) : cleanPreferred;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
