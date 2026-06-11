package ro.ainpc.world.patch

enum class PatchBuildMode(private val idValue: String) {
    SEMANTIC_ONLY("semantic_only"),
    NATIVE_PATCH("native_patch"),
    WORLDEDIT_TEMPLATE("worldedit_template");

    fun id(): String = idValue
}
