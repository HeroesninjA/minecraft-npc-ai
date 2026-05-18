package ro.ainpc.api

import ro.ainpc.addons.AINPCAddon
import ro.ainpc.addons.AddonDescriptor
import ro.ainpc.addons.AddonType

interface AddonRegistryApi {
    val descriptors: Collection<AddonDescriptor>

    val primaryScenario: AddonDescriptor?

    fun registerDescriptor(descriptor: AddonDescriptor?)

    fun registerAddon(addon: AINPCAddon?)

    fun unregisterAddon(addonId: String?)

    fun removeByOrigin(origin: String?)

    fun getDescriptors(type: AddonType?): List<AddonDescriptor>

    fun getDescriptor(id: String?): AddonDescriptor?

    fun isAddonEnabled(addonId: String?): Boolean = true

    fun size(): Int
}
