package ro.ainpc.spawn;

import ro.ainpc.npc.AINPC;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record HouseAllocation(
    String placeId,
    String familyId,
    String primaryOwnerNpcKey,
    int maxResidents,
    List<ResidentPlan> residentPlans
) {
    public HouseAllocation {
        placeId = clean(placeId);
        familyId = clean(familyId);
        primaryOwnerNpcKey = clean(primaryOwnerNpcKey);
        residentPlans = List.copyOf(residentPlans != null ? residentPlans : List.of());
        maxResidents = maxResidents > 0 ? maxResidents : residentPlans.size();
    }

    public static Builder builder(String placeId) {
        return new Builder(placeId);
    }

    public List<NpcSpawnPlan> toNpcSpawnPlans() {
        List<NpcSpawnPlan> plans = new ArrayList<>();
        for (ResidentPlan resident : residentPlans) {
            plans.add(resident.toNpcSpawnPlan(placeId, familyId));
        }
        return List.copyOf(plans);
    }

    public Map<String, String> toPlaceMetadata() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("role", "home");
        if (!familyId.isBlank()) {
            metadata.put("family_id", familyId);
        }
        metadata.put("max_residents", Integer.toString(maxResidents));
        metadata.put("residents", String.join(",", residentNpcKeys()));
        return Map.copyOf(metadata);
    }

    public FamilyBindingPlan toFamilyBindingPlan(Map<String, AINPC> spawnedNpcsByKey) {
        List<FamilyBindingPlan.Member> members = new ArrayList<>();
        Map<String, AINPC> resolvedNpcs = spawnedNpcsByKey != null ? spawnedNpcsByKey : Map.of();

        for (ResidentPlan resident : residentPlans) {
            AINPC npc = resolvedNpcs.get(normalizeKey(resident.npcKey()));
            if (npc == null) {
                npc = resolvedNpcs.get(resident.npcKey());
            }
            members.add(FamilyBindingPlan.member(resident.npcKey(), npc, resident.relationRole()));
        }

        return new FamilyBindingPlan(familyId, members);
    }

    public List<String> residentNpcKeys() {
        return residentPlans.stream()
            .map(ResidentPlan::npcKey)
            .filter(key -> !key.isBlank())
            .toList();
    }

    public static final class Builder {
        private final String placeId;
        private String familyId = "";
        private String primaryOwnerNpcKey = "";
        private int maxResidents = 0;
        private final List<ResidentPlan> residentPlans = new ArrayList<>();

        private Builder(String placeId) {
            this.placeId = placeId;
        }

        public Builder familyId(String familyId) {
            this.familyId = familyId;
            return this;
        }

        public Builder primaryOwnerNpcKey(String primaryOwnerNpcKey) {
            this.primaryOwnerNpcKey = primaryOwnerNpcKey;
            return this;
        }

        public Builder maxResidents(int maxResidents) {
            this.maxResidents = maxResidents;
            return this;
        }

        public Builder addResident(ResidentPlan residentPlan) {
            if (residentPlan != null) {
                this.residentPlans.add(residentPlan);
            }
            return this;
        }

        public Builder residents(List<ResidentPlan> residentPlans) {
            this.residentPlans.clear();
            if (residentPlans != null) {
                this.residentPlans.addAll(residentPlans);
            }
            return this;
        }

        public HouseAllocation build() {
            return new HouseAllocation(placeId, familyId, primaryOwnerNpcKey, maxResidents, residentPlans);
        }
    }

    public record ResidentPlan(
        String npcKey,
        String name,
        String relationRole,
        String occupation,
        String backstory,
        int age,
        String gender,
        String archetype,
        String spawnNodeId,
        String homeNodeId,
        String bedNodeId,
        String workPlaceId,
        String workNodeId,
        String socialPlaceId,
        String socialNodeId
    ) {
        public ResidentPlan {
            npcKey = clean(npcKey);
            name = clean(name);
            relationRole = clean(relationRole);
            occupation = clean(occupation);
            backstory = clean(backstory);
            age = age > 0 ? age : 30;
            gender = clean(gender);
            archetype = clean(archetype);
            spawnNodeId = clean(spawnNodeId);
            homeNodeId = clean(homeNodeId);
            bedNodeId = clean(bedNodeId);
            workPlaceId = clean(workPlaceId);
            workNodeId = clean(workNodeId);
            socialPlaceId = clean(socialPlaceId);
            socialNodeId = clean(socialNodeId);
        }

        public static ResidentBuilder builder(String npcKey, String name) {
            return new ResidentBuilder(npcKey, name);
        }

        public String effectiveHomeNodeId() {
            return !homeNodeId.isBlank() ? homeNodeId : bedNodeId;
        }

        public NpcSpawnPlan toNpcSpawnPlan(String homePlaceId, String familyId) {
            return NpcSpawnPlan.builder(npcKey, !name.isBlank() ? name : humanizeId(npcKey))
                .occupation(occupation)
                .backstory(backstory)
                .age(age)
                .gender(gender)
                .archetype(archetype)
                .homePlaceId(homePlaceId)
                .workPlaceId(workPlaceId)
                .socialPlaceId(socialPlaceId)
                .spawnNodeId(spawnNodeId)
                .homeNodeId(effectiveHomeNodeId())
                .workNodeId(workNodeId)
                .socialNodeId(socialNodeId)
                .familyId(familyId)
                .build();
        }
    }

    public static final class ResidentBuilder {
        private final String npcKey;
        private final String name;
        private String relationRole = "";
        private String occupation = "";
        private String backstory = "";
        private int age = 30;
        private String gender = "male";
        private String archetype = "";
        private String spawnNodeId = "";
        private String homeNodeId = "";
        private String bedNodeId = "";
        private String workPlaceId = "";
        private String workNodeId = "";
        private String socialPlaceId = "";
        private String socialNodeId = "";

        private ResidentBuilder(String npcKey, String name) {
            this.npcKey = npcKey;
            this.name = name;
        }

        public ResidentBuilder relationRole(String relationRole) {
            this.relationRole = relationRole;
            return this;
        }

        public ResidentBuilder occupation(String occupation) {
            this.occupation = occupation;
            return this;
        }

        public ResidentBuilder backstory(String backstory) {
            this.backstory = backstory;
            return this;
        }

        public ResidentBuilder age(int age) {
            this.age = age;
            return this;
        }

        public ResidentBuilder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public ResidentBuilder archetype(String archetype) {
            this.archetype = archetype;
            return this;
        }

        public ResidentBuilder spawnNodeId(String spawnNodeId) {
            this.spawnNodeId = spawnNodeId;
            return this;
        }

        public ResidentBuilder homeNodeId(String homeNodeId) {
            this.homeNodeId = homeNodeId;
            return this;
        }

        public ResidentBuilder bedNodeId(String bedNodeId) {
            this.bedNodeId = bedNodeId;
            return this;
        }

        public ResidentBuilder workPlaceId(String workPlaceId) {
            this.workPlaceId = workPlaceId;
            return this;
        }

        public ResidentBuilder workNodeId(String workNodeId) {
            this.workNodeId = workNodeId;
            return this;
        }

        public ResidentBuilder socialPlaceId(String socialPlaceId) {
            this.socialPlaceId = socialPlaceId;
            return this;
        }

        public ResidentBuilder socialNodeId(String socialNodeId) {
            this.socialNodeId = socialNodeId;
            return this;
        }

        public HouseAllocation.ResidentPlan build() {
            return new HouseAllocation.ResidentPlan(
                npcKey,
                name,
                relationRole,
                occupation,
                backstory,
                age,
                gender,
                archetype,
                spawnNodeId,
                homeNodeId,
                bedNodeId,
                workPlaceId,
                workNodeId,
                socialPlaceId,
                socialNodeId
            );
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeKey(String value) {
        return clean(value).toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static String humanizeId(String rawId) {
        String value = clean(rawId);
        int scopeSeparator = value.lastIndexOf(':');
        if (scopeSeparator >= 0) {
            value = value.substring(scopeSeparator + 1);
        }

        StringBuilder builder = new StringBuilder();
        for (String part : value.split("[_-]+")) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() > 0 ? builder.toString() : value;
    }
}
