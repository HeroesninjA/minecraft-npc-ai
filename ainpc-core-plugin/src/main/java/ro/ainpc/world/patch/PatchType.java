package ro.ainpc.world.patch;

public enum PatchType {
    ADD_HOUSE("add_house"),
    ADD_WORKPLACE("add_workplace"),
    ADD_SOCIAL_PLACE("add_social_place"),
    ADD_NODE("add_node"),
    EXPAND_HOUSE("expand_house"),
    DECORATE_PLACE("decorate_place"),
    CONNECT_PATH("connect_path"),
    MARK_EXISTING_PLACE("mark_existing_place");

    private final String id;

    PatchType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
