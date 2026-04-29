package ro.ainpc.spawn;

public record NpcSpawnPlan(
    String npcKey,
    String name,
    String occupation,
    String backstory,
    int age,
    String gender,
    String archetype,
    String homePlaceId,
    String workPlaceId,
    String socialPlaceId,
    String spawnNodeId,
    String homeNodeId,
    String workNodeId,
    String socialNodeId,
    String familyId
) {
    public NpcSpawnPlan {
        npcKey = clean(npcKey);
        name = clean(name);
        occupation = clean(occupation);
        backstory = clean(backstory);
        age = age > 0 ? age : 30;
        gender = clean(gender).isBlank() ? "male" : clean(gender).toLowerCase();
        archetype = clean(archetype);
        homePlaceId = clean(homePlaceId);
        workPlaceId = clean(workPlaceId);
        socialPlaceId = clean(socialPlaceId);
        spawnNodeId = clean(spawnNodeId);
        homeNodeId = clean(homeNodeId);
        workNodeId = clean(workNodeId);
        socialNodeId = clean(socialNodeId);
        familyId = clean(familyId);
    }

    public static Builder builder(String npcKey, String name) {
        return new Builder(npcKey, name);
    }

    public static final class Builder {
        private final String npcKey;
        private final String name;
        private String occupation = "";
        private String backstory = "";
        private int age = 30;
        private String gender = "male";
        private String archetype = "";
        private String homePlaceId = "";
        private String workPlaceId = "";
        private String socialPlaceId = "";
        private String spawnNodeId = "";
        private String homeNodeId = "";
        private String workNodeId = "";
        private String socialNodeId = "";
        private String familyId = "";

        private Builder(String npcKey, String name) {
            this.npcKey = npcKey;
            this.name = name;
        }

        public Builder occupation(String occupation) {
            this.occupation = occupation;
            return this;
        }

        public Builder backstory(String backstory) {
            this.backstory = backstory;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder archetype(String archetype) {
            this.archetype = archetype;
            return this;
        }

        public Builder homePlaceId(String homePlaceId) {
            this.homePlaceId = homePlaceId;
            return this;
        }

        public Builder workPlaceId(String workPlaceId) {
            this.workPlaceId = workPlaceId;
            return this;
        }

        public Builder socialPlaceId(String socialPlaceId) {
            this.socialPlaceId = socialPlaceId;
            return this;
        }

        public Builder spawnNodeId(String spawnNodeId) {
            this.spawnNodeId = spawnNodeId;
            return this;
        }

        public Builder homeNodeId(String homeNodeId) {
            this.homeNodeId = homeNodeId;
            return this;
        }

        public Builder workNodeId(String workNodeId) {
            this.workNodeId = workNodeId;
            return this;
        }

        public Builder socialNodeId(String socialNodeId) {
            this.socialNodeId = socialNodeId;
            return this;
        }

        public Builder familyId(String familyId) {
            this.familyId = familyId;
            return this;
        }

        public NpcSpawnPlan build() {
            return new NpcSpawnPlan(
                npcKey,
                name,
                occupation,
                backstory,
                age,
                gender,
                archetype,
                homePlaceId,
                workPlaceId,
                socialPlaceId,
                spawnNodeId,
                homeNodeId,
                workNodeId,
                socialNodeId,
                familyId
            );
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
