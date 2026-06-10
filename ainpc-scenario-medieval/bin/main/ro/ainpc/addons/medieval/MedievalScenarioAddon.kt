package ro.ainpc.addons.medieval

import ro.ainpc.addons.AINPCAddon
import ro.ainpc.addons.AddonDescriptor
import ro.ainpc.addons.AddonType
import ro.ainpc.platform.RuntimeMode
import java.util.EnumSet

class MedievalScenarioAddon(version: String) : AINPCAddon {
    private val descriptor = AddonDescriptor(
        AddonDescriptor.ORIGIN_PLUGIN_ADDON,
        "ainpc-scenario-medieval",
        "AINPC Scenario Medieval",
        version,
        "Addon plugin separat care livreaza pack-ul medieval_quest pentru core.",
        AddonType.SCENARIO,
        true,
        EnumSet.allOf(RuntimeMode::class.java),
        listOf("scenarios", "scenario-pack", "pack-installer", "addon-config-template"),
        listOf("ainpc-core")
    )

    override fun getDescriptor(): AddonDescriptor = descriptor
}
