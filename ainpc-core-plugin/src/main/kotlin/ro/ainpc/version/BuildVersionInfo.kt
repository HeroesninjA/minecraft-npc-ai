package ro.ainpc.version

import org.bukkit.plugin.java.JavaPlugin
import java.util.Properties

object BuildVersionInfo {
    private const val RESOURCE_PATH = "/build-info.properties"

    @JvmStatic
    fun capture(plugin: JavaPlugin): BuildVersionSnapshot {
        val properties = Properties()
        BuildVersionInfo::class.java.getResourceAsStream(RESOURCE_PATH)?.use { inputStream ->
            properties.load(inputStream)
        }
        val version = properties.getProperty("version").resolvedOr(plugin.pluginMeta.version)
        val buildHash = properties.getProperty("buildHash").resolvedOr("unknown")
        val buildTimestamp = properties.getProperty("buildTimestamp").resolvedOr("unknown")
        return BuildVersionSnapshot(version, buildHash, buildTimestamp)
    }

    @JvmStatic
    fun formatSnapshot(snapshot: BuildVersionSnapshot): List<String> {
        return listOf(
            "&6=== AINPC Version ===",
            "&eUltima versiune: &f${snapshot.version}",
            "&eHash ultimul build: &f${snapshot.buildHash}",
            "&eData si ora buildului: &f${snapshot.buildTimestamp}",
        )
    }

    private fun String?.resolvedOr(fallback: String): String {
        val trimmed = this?.trim().orEmpty()
        if (trimmed.isBlank()) {
            return fallback
        }
        return if (trimmed.contains("\${")) fallback else trimmed
    }
}

data class BuildVersionSnapshot(
    val version: String,
    val buildHash: String,
    val buildTimestamp: String,
)
