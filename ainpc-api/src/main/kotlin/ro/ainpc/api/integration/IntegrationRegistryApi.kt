package ro.ainpc.api.integration

import ro.ainpc.addons.AddonType

interface IntegrationRegistryApi {
    val integrations: Collection<ExternalPluginIntegration>

    fun register(integration: ExternalPluginIntegration)

    fun unregister(pluginName: String)

    fun findByType(type: IntegrationType): List<ExternalPluginIntegration>

    fun findByCapability(capability: String): List<ExternalPluginIntegration>

    fun isRegistered(pluginName: String): Boolean

    fun size(): Int
}
