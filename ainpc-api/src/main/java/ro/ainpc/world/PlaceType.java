package ro.ainpc.world;

public enum PlaceType {
    HOUSE("house"),
    SHOP("shop"),
    FORGE("forge"),
    TAVERN("tavern"),
    FARM("farm"),
    MARKET("market"),
    CASTLE_ROOM("castle_room"),
    CAVE_ROOM("cave_room"),
    CAMP("camp"),
    CUSTOM("custom");

    private final String id;

    PlaceType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static PlaceType fromId(String value) {
        if (value == null || value.isBlank()) {
            return CUSTOM;
        }

        for (PlaceType type : values()) {
            if (type.id.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }

        return CUSTOM;
    }
}
