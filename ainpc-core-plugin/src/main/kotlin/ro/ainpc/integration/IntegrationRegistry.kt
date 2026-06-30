package ro.ainpc.integration

import ro.ainpc.api.integration.ExternalPluginIntegration
import ro.ainpc.api.integration.IntegrationRegistryApi
import ro.ainpc.api.integration.IntegrationType
import java.util.LinkedHashMap
import java.util.Locale
import java.util.logging.Logger

class IntegrationRegistry : IntegrationRegistryApi {
    private val logger = Logger.getLogger(IntegrationRegistry::class.java.name)
    private val integrationsByName: MutableMap<String, ExternalPluginIntegration> = LinkedHashMap()

    @Synchronized
    override fun register(integration: ExternalPluginIntegration) {
        val key = normalizeName(integration.pluginName)
        val previous = integrationsByName.put(key, integration)
        if (previous != null) {
            logger.info("[AINPC Integration] Inlocuit integrarea '${integration.pluginName}' (${integration.integrationType})")
        } else {
            logger.info("[AINPC Integration] Inregistrata integrarea '${integration.pluginName}' (${integration.integrationType})")
        }
        integration.onRegister()
    }

    @Synchronized
    override fun unregister(pluginName: String) {
        val key = normalizeName(pluginName)
        val removed = integrationsByName.remove(key)
        if (removed != null) {
            removed.onUnregister()
            logger.info("[AINPC Integration] Eliminata integrarea '${pluginName}'")
        }
    }

    @Synchronized
    override fun findByType(type: IntegrationType): List<ExternalPluginIntegration> {
        return integrationsByName.values.filter { it.integrationType == type }
    }

    @Synchronized
    override fun findByCapability(capability: String): List<ExternalPluginIntegration> {
        val normalized = capability.trim().lowercase(Locale.ROOT)
        return integrationsByName.values.filter { it.canProvide(normalized) }
    }

    @Synchronized
    override fun isRegistered(pluginName: String): Boolean {
        return normalizeName(pluginName) in integrationsByName
    }

    override val integrations: Collection<ExternalPluginIntegration>
        @Synchronized get() = integrationsByName.values.toList()

    @Synchronized
    override fun size(): Int = integrationsByName.size

    @Synchronized
    fun clear() {
        val names = integrationsByName.keys.toList()
        for (name in names) {
            unregister(name)
        }
    }

    private fun normalizeName(name: String): String = name.trim().lowercase(Locale.ROOT)
}
