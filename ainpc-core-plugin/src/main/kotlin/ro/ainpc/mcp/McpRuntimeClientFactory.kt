package ro.ainpc.mcp

import ro.ainpc.AINPCPlugin

/**
 * Mcpruntimeclientfactory.
 */
object McpRuntimeClientFactory {
    fun create(plugin: AINPCPlugin): McpRuntimeClient {
        val config = McpRuntimeConfig.from(plugin.config)
        return if (config.enabled) {
            HttpMcpRuntimeClient(config)
        } else {
            NoopMcpRuntimeClient(config)
        }
    }
}
