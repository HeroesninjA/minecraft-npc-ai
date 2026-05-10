package ro.ainpc.world.mapping;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum MappingDraftKind {
    REGION("region"),
    PLACE("place"),
    NODE("node"),
    NPC_BIND("npc_bind"),
    QUEST_ANCHOR("quest_anchor");

    private final String id;

    MappingDraftKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<MappingDraftKind> fromId(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(values())
            .filter(kind -> kind.id.equalsIgnoreCase(normalized) || kind.name().equalsIgnoreCase(normalized))
            .findFirst();
    }
}
