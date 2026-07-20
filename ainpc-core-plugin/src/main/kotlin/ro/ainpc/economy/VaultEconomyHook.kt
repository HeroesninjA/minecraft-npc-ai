package ro.ainpc.economy

import org.bukkit.Bukkit
import org.bukkit.plugin.ServicePriority
import ro.ainpc.AINPCPlugin
import ro.ainpc.addons.AddonDescriptor
import ro.ainpc.api.integration.ExternalPluginIntegration
import ro.ainpc.api.integration.IntegrationType
import java.lang.reflect.Proxy
import java.util.logging.Level

class VaultEconomyHook(private val plugin: AINPCPlugin) : ExternalPluginIntegration {
    override val pluginName: String = "Vault"
    override val pluginVersion: String
        get() = plugin.server.pluginManager.getPlugin(pluginName)?.pluginMeta?.version ?: "unknown"
    override val integrationType: IntegrationType = IntegrationType.ECONOMY

    private var vaultRegistered = false
    private var vaultPresent = false
    private var proxyRef: Any? = null

    override fun getDescriptor(): AddonDescriptor? = null

    override fun canProvide(capability: String): Boolean =
        capability == "economy" || capability == "vault"

    override fun onRegister() {
        if (vaultRegistered) {
            return
        }
        vaultPresent = false
        proxyRef = null
        try {
            Class.forName("net.milkbowl.vault.economy.Economy")
            vaultPresent = true
            if (registerVaultEconomy()) {
                plugin.logger.info("[Vault] Integrare Vault Economy activata cu succes.")
            }
        } catch (e: ClassNotFoundException) {
            vaultPresent = false
            plugin.logger.info("[Vault] Vault nu este prezent - integrarea este dezactivata.")
        } catch (e: Exception) {
            vaultRegistered = false
            proxyRef = null
            plugin.logger.log(Level.WARNING, "[Vault] Eroare la initializarea integrarii Vault", e)
        }
    }

    override fun onUnregister() {
        try {
            if (!vaultRegistered || proxyRef == null) {
                return
            }
            try {
                val servicesManager = Bukkit.getServicesManager()
                val unregisterMethod = servicesManager.javaClass.getMethod(
                    "unregister", Class::class.java, Any::class.java
                )
                unregisterMethod.invoke(servicesManager, 
                    Class.forName("net.milkbowl.vault.economy.Economy"), proxyRef)
            } catch (_: Exception) {}
        } finally {
            vaultRegistered = false
            vaultPresent = false
            proxyRef = null
        }
    }

    private fun registerVaultEconomy(): Boolean {
        val economyInterface = Class.forName("net.milkbowl.vault.economy.Economy")
        val handler = VaultEconomyInvocationHandler(plugin)
        val proxy = Proxy.newProxyInstance(
            economyInterface.classLoader,
            arrayOf<Class<*>>(economyInterface),
            handler
        )
        proxyRef = proxy
        try {
            val servicesManager = Bukkit.getServicesManager()
            val registerMethod = servicesManager.javaClass.getMethod(
                "register", Class::class.java, Any::class.java,
                org.bukkit.plugin.Plugin::class.java, ServicePriority::class.java
            )
            registerMethod.invoke(servicesManager, economyInterface, proxy, plugin, ServicePriority.Normal)
            vaultRegistered = true
            plugin.logger.info("[Vault] Provider Vault Economy inregistrat in ServicesManager.")
            return true
        } catch (e: Exception) {
            vaultRegistered = false
            proxyRef = null
            plugin.logger.log(Level.WARNING, "[Vault] Eroare la inregistrarea provider-ului Vault", e)
            return false
        }
    }

    fun isVaultPresent(): Boolean = vaultPresent

    fun isRegistered(): Boolean = vaultRegistered
}
