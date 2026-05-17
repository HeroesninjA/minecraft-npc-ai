package ro.ainpc.addons

import ro.ainpc.platform.RuntimeMode
import java.util.Collections
import java.util.EnumSet

class AddonDescriptor(
    val origin: String,
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val type: AddonType,
    val isPrimaryScenario: Boolean,
    supportedRuntimeModes: Set<RuntimeMode>?,
    capabilities: List<String>?,
    dependencies: List<String>?
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
