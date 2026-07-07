package ro.ainpc.addons

import ro.ainpc.api.AINPCPlatformApi

interface AINPCAddon {
    fun getDescriptor(): AddonDescriptor

    fun onLoad(api: AINPCPlatformApi) {}

    fun onEnable(api: AINPCPlatformApi) {}

    fun onDisable(api: AINPCPlatformApi) {}

    fun onStoryEvent(eventType: String, scopeId: String, title: String) {}

    fun onRelationshipChange(npcUuidA: String, npcUuidB: String, newType: String) {}

    fun onNpcStateChange(npcUuid: String, oldState: String, newState: String) {}

    fun onDailySalaryPaid(npcCount: Int, totalAmount: Int) {}

    fun onSeasonChange(worldName: String, oldSeason: String, newSeason: String) {}
}
