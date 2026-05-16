package ro.ainpc.world.patch;

public enum PatchBuildMode {
    SEMANTIC_ONLY("semantic_only"),
    NATIVE_PATCH("native_patch"),
    WORLDEDIT_TEMPLATE("worldedit_template");

    private final String id;

    PatchBuildMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
