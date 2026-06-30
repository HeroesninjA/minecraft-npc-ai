package ro.ainpc.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.addons.AddonDescriptor
import ro.ainpc.api.integration.ExternalPluginIntegration
import ro.ainpc.api.integration.IntegrationType

class IntegrationRegistryTest {
    private val registry = IntegrationRegistry()

    @Test
    fun registersAndTracksIntegration() {
        val integration = testIntegration("Essentials", IntegrationType.ECONOMY)
        registry.register(integration)

        assertTrue(registry.isRegistered("Essentials"))
        assertEquals(1, registry.size())
    }

    @Test
    fun unregistersIntegration() {
        val integration = testIntegration("WorldGuard", IntegrationType.PROTECTION)
        registry.register(integration)
        registry.unregister("WorldGuard")

        assertFalse(registry.isRegistered("WorldGuard"))
        assertEquals(0, registry.size())
    }

    @Test
    fun findByTypeReturnsCorrectIntegrations() {
        registry.register(testIntegration("Vault", IntegrationType.ECONOMY))
        registry.register(testIntegration("WorldGuard", IntegrationType.PROTECTION))
        registry.register(testIntegration("Essentials", IntegrationType.ECONOMY))

        val economy = registry.findByType(IntegrationType.ECONOMY)
        assertEquals(2, economy.size)
        assertTrue(economy.any { it.pluginName == "Vault" })
        assertTrue(economy.any { it.pluginName == "Essentials" })
    }

    @Test
    fun findByCapabilityReturnsMatchingIntegrations() {
        val ecoIntegration = object : ExternalPluginIntegration {
            override val pluginName = "Vault"
            override val pluginVersion = "1.0"
            override val integrationType = IntegrationType.ECONOMY
            override fun canProvide(capability: String) = capability == "currency" || capability == "economy"
            override fun getDescriptor(): AddonDescriptor? = null
        }
        registry.register(ecoIntegration)

        val result = registry.findByCapability("currency")
        assertEquals(1, result.size)
    }

    @Test
    fun clearRemovesAllIntegrations() {
        registry.register(testIntegration("A", IntegrationType.CUSTOM))
        registry.register(testIntegration("B", IntegrationType.CUSTOM))
        registry.clear()

        assertEquals(0, registry.size())
    }

    @Test
    fun replaceExistingIntegration() {
        registry.register(testIntegration("Same", IntegrationType.ECONOMY))
        registry.register(testIntegration("Same", IntegrationType.PROTECTION))

        assertEquals(1, registry.size())
        assertEquals(IntegrationType.PROTECTION, registry.findByType(IntegrationType.PROTECTION).first().integrationType)
    }

    private fun testIntegration(name: String, type: IntegrationType): ExternalPluginIntegration {
        return object : ExternalPluginIntegration {
            override val pluginName = name
            override val pluginVersion = "1.0"
            override val integrationType = type
            override fun getDescriptor(): AddonDescriptor? = null
        }
    }
}
