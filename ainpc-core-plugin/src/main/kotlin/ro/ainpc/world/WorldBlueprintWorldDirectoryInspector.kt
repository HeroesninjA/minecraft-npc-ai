package ro.ainpc.world

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal enum class WorldBlueprintWorldDirectoryStatus {
    UNSAFE_NAME,
    CONTAINER_UNAVAILABLE,
    AVAILABLE,
    EXISTING_DIRECTORY,
    EXISTING_FILE,
    SYMBOLIC_LINK,
    UNKNOWN,
}

internal object WorldBlueprintWorldDirectoryInspector {
    private val portableName = Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")
    private val reservedName = Regex("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])")

    fun inspect(worldContainer: Path, worldName: String): WorldBlueprintWorldDirectoryStatus {
        if (!portableName.matches(worldName) || reservedName.matches(worldName)) {
            return WorldBlueprintWorldDirectoryStatus.UNSAFE_NAME
        }
        return try {
            val container = worldContainer.toAbsolutePath().normalize()
            if (!Files.isDirectory(container)) return WorldBlueprintWorldDirectoryStatus.CONTAINER_UNAVAILABLE
            val candidate = container.resolve(worldName)
            when {
                Files.isSymbolicLink(candidate) -> WorldBlueprintWorldDirectoryStatus.SYMBOLIC_LINK
                Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS) -> WorldBlueprintWorldDirectoryStatus.EXISTING_DIRECTORY
                Files.exists(candidate, LinkOption.NOFOLLOW_LINKS) -> WorldBlueprintWorldDirectoryStatus.EXISTING_FILE
                Files.notExists(candidate, LinkOption.NOFOLLOW_LINKS) -> WorldBlueprintWorldDirectoryStatus.AVAILABLE
                else -> WorldBlueprintWorldDirectoryStatus.UNKNOWN
            }
        } catch (_: SecurityException) {
            WorldBlueprintWorldDirectoryStatus.UNKNOWN
        }
    }
}
