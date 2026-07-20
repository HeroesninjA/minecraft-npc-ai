package ro.ainpc.addons

import ro.ainpc.platform.RuntimeMode
import java.util.Collections
import java.util.EnumSet

class AddonDescriptor @JvmOverloads constructor(
    val origin: String,
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val type: AddonType = AddonType.FEATURE,
    val isPrimaryScenario: Boolean = false,
    supportedRuntimeModes: Set<RuntimeMode>? = null,
    capabilities: List<String>? = null,
    dependencies: List<String>? = null
) {
    val supportedRuntimeModes: Set<RuntimeMode> =
        if (supportedRuntimeModes.isNullOrEmpty()) {
            EnumSet.allOf(RuntimeMode::class.java)
        } else {
            EnumSet.copyOf(supportedRuntimeModes)
        }

    val capabilities: List<String> = Collections.unmodifiableList(ArrayList(capabilities ?: emptyList()))
    val dependencies: List<String> = Collections.unmodifiableList(ArrayList(dependencies ?: emptyList()))

    fun supports(runtimeMode: RuntimeMode): Boolean = supportedRuntimeModes.contains(runtimeMode)

    companion object {
        const val ORIGIN_CORE = "core"
        const val ORIGIN_FEATURE_PACK = "feature-pack"
        const val ORIGIN_PLUGIN_ADDON = "plugin-addon"
    }
}
