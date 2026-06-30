package ro.ainpc.api.integration

import ro.ainpc.addons.AddonDescriptor

interface ExternalPluginIntegration {
    val pluginName: String
    val pluginVersion: String
    val integrationType: IntegrationType

    fun onRegister() {}
    fun onUnregister() {}

    fun canProvide(capability: String): Boolean = false

    fun getDescriptor(): AddonDescriptor?
}

enum class IntegrationType {
    ECONOMY,
    PROTECTION,
    CHAT,
    PERMISSION,
    WORLD_GUARD,
    FACTIONS,
    SHOP,
    DUNGEON,
    MINIGAME,
    CUSTOM
}
