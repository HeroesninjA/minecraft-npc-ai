package ro.ainpc.addons

import ro.ainpc.api.AINPCPlatformApi

interface AINPCAddon {
    fun getDescriptor(): AddonDescriptor

    fun onLoad(api: AINPCPlatformApi) {}

    fun onEnable(api: AINPCPlatformApi) {}

    fun onDisable(api: AINPCPlatformApi) {}
}
