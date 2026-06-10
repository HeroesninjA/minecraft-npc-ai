package ro.ainpc.platform.features

import java.util.Collections
import java.util.EnumMap

class RuntimeFeatureSnapshot(states: Collection<RuntimeFeatureState>?) {
    private val statesByFeatureValue: Map<RuntimeFeatureKey, RuntimeFeatureState>
    private val statesValue: List<RuntimeFeatureState>

    init {
        val byFeature = EnumMap<RuntimeFeatureKey, RuntimeFeatureState>(RuntimeFeatureKey::class.java)
        for (state in states ?: emptyList()) {
            byFeature[state.feature()] = state
        }
        for (feature in RuntimeFeatureKey.entries) {
            byFeature.putIfAbsent(
                feature,
                RuntimeFeatureState.fromBoolean(
                    feature,
                    feature.defaultEnabled,
                    RuntimeFeatureSource.of("default", feature.configPath, feature.defaultEnabled.toString()),
                    "default ${feature.configPath}=${feature.defaultEnabled}"
                )
            )
        }
        statesByFeatureValue = Collections.unmodifiableMap(EnumMap(byFeature))
        statesValue = Collections.unmodifiableList(RuntimeFeatureKey.entries.map { feature -> byFeature.getValue(feature) })
    }

    fun get(feature: RuntimeFeatureKey): RuntimeFeatureState = statesByFeatureValue.getValue(feature)

    fun enabled(feature: RuntimeFeatureKey): Boolean = get(feature).enabled()

    fun states(): List<RuntimeFeatureState> = statesValue

    fun statesByFeature(): Map<RuntimeFeatureKey, RuntimeFeatureState> = statesByFeatureValue
}
