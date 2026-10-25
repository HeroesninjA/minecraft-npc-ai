package ro.ainpc.world

internal enum class WorldBlueprintWorldCreationObservation {
    LOADED,
    DIRECTORY_PRESENT,
    OTHER_ENTRY,
    ENTRY_ABSENT,
    UNKNOWN;

    companion object {
        fun from(loaded: Boolean, directory: WorldBlueprintWorldDirectoryStatus): WorldBlueprintWorldCreationObservation =
            when {
                loaded -> LOADED
                directory == WorldBlueprintWorldDirectoryStatus.EXISTING_DIRECTORY -> DIRECTORY_PRESENT
                directory == WorldBlueprintWorldDirectoryStatus.EXISTING_FILE ||
                    directory == WorldBlueprintWorldDirectoryStatus.SYMBOLIC_LINK -> OTHER_ENTRY
                directory == WorldBlueprintWorldDirectoryStatus.AVAILABLE -> ENTRY_ABSENT
                else -> UNKNOWN
            }
    }
}
