package ro.ainpc.world.mapping;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum MappingWandMode {
    REGION("region"),
    PLACE("place"),
    NODE("node"),
    NPC_BIND("npc_bind"),
    QUEST_ANCHOR("quest_anchor");

    private final String id;

    MappingWandMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public MappingDraftKind draftKind() {
        return switch (this) {
            case REGION -> MappingDraftKind.REGION;
            case PLACE -> MappingDraftKind.PLACE;
            case NODE -> MappingDraftKind.NODE;
            case NPC_BIND -> MappingDraftKind.NPC_BIND;
            case QUEST_ANCHOR -> MappingDraftKind.QUEST_ANCHOR;
        };
    }

    public boolean usesPointSelection() {
        return this == NODE || this == NPC_BIND || this == QUEST_ANCHOR;
    }

    public static Optional<MappingWandMode> fromId(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(values())
            .filter(mode -> mode.id.equalsIgnoreCase(normalized) || mode.name().equalsIgnoreCase(normalized))
            .findFirst();
    }
}
