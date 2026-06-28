package ro.ainpc.mcp

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class McpRuntimeConfigTest {
    @Test
    fun `mcp is enabled by default with fail open`() {
        val runtimeConfig = McpRuntimeConfig.from(YamlConfiguration())

        assertTrue(runtimeConfig.enabled)
        assertTrue(runtimeConfig.failOpen)
        assertEquals("http://127.0.0.1:39841/mcp", runtimeConfig.baseUrl)
    }

    @Test
    fun `mcp remains disabled unless both feature flags are enabled`() {
        val config = YamlConfiguration()
        config.set("features.mcp", true)
        config.set("mcp.enabled", false)

        val runtimeConfig = McpRuntimeConfig.from(config)

        assertFalse(runtimeConfig.enabled)
    }

    @Test
    fun `mcp config resolves health endpoint from base url`() {
        val config = YamlConfiguration()
        config.set("features.mcp", true)
        config.set("mcp.enabled", true)
        config.set("mcp.base_url", "http://127.0.0.1:39841/mcp")
        config.set("mcp.connect_timeout_seconds", 2)
        config.set("mcp.read_timeout_seconds", 3)

        val runtimeConfig = McpRuntimeConfig.from(config)

        assertTrue(runtimeConfig.enabled)
        assertEquals("http://127.0.0.1:39841/mcp", runtimeConfig.baseUrl)
        assertEquals("http://127.0.0.1:39841/actuator/health", runtimeConfig.healthUrl.toString())
        assertEquals(2, runtimeConfig.connectTimeout.seconds)
        assertEquals(3, runtimeConfig.readTimeout.seconds)
    }
}
