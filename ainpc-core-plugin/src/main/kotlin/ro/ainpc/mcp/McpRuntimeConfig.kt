package ro.ainpc.mcp

import org.bukkit.configuration.file.FileConfiguration
import java.net.URI
import java.time.Duration

data class McpRuntimeConfig(
    val enabled: Boolean,
    val baseUrl: String,
    val token: String,
    val connectTimeout: Duration,
    val readTimeout: Duration,
    val failOpen: Boolean
) {
    val healthUrl: URI = URI.create(baseUrl).resolve("/actuator/health")

    companion object {
        fun from(config: FileConfiguration): McpRuntimeConfig {
            val featureEnabled = config.getBoolean("features.mcp", true)
            val mcpEnabled = config.getBoolean("mcp.enabled", true)
            val baseUrl = config.getString("mcp.base_url", "http://127.0.0.1:39841/mcp")
                ?.trim()
                ?.trimEnd('/')
                ?.takeIf { it.isNotBlank() }
                ?: "http://127.0.0.1:39841/mcp"
            return McpRuntimeConfig(
                enabled = featureEnabled && mcpEnabled,
                baseUrl = baseUrl,
                token = config.getString("mcp.token", "") ?: "",
                connectTimeout = Duration.ofSeconds(config.getLong("mcp.connect_timeout_seconds", 5L).coerceAtLeast(1L)),
                readTimeout = Duration.ofSeconds(config.getLong("mcp.read_timeout_seconds", 20L).coerceAtLeast(1L)),
                failOpen = config.getBoolean("mcp.fail_open", true)
            )
        }
    }
}
