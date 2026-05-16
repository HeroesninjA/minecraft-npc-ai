package ro.ainpc.spawn;

import ro.ainpc.npc.AINPC;

import java.util.List;

public record FamilyBindingPlan(
    String familyId,
    List<Member> members
) {
    public FamilyBindingPlan {
        familyId = clean(familyId);
        members = List.copyOf(members != null ? members : List.of());
    }

    public static Member member(String npcKey, AINPC npc, String familyRole) {
        return new Member(
            clean(npcKey),
            npc != null ? npc.getDatabaseId() : 0,
            npc != null ? npc.getName() : "",
            familyRole
        );
    }

    public record Member(
        String npcKey,
        int npcId,
        String npcName,
        String familyRole
    ) {
        public Member {
            npcKey = clean(npcKey);
            npcName = clean(npcName);
            familyRole = clean(familyRole);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
