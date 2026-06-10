package ro.ainpc.engine.runtime

import java.util.Collections
import java.util.LinkedHashMap
import java.util.Optional

open class ScenarioRuntimeRegistry<T : ScenarioRuntimeHandler> {
    private val handlers = LinkedHashMap<String, T>()

    fun register(handler: T?): ScenarioValidationReport {
        val report = ScenarioValidationReport()
        if (handler == null) {
            report.error("Handler null.")
            return report
        }
        val type = normalize(handler.type())
        if (type.isBlank()) {
            report.error("Handler fara type.")
            return report
        }
        if (handlers.containsKey(type)) {
            report.error("Handler duplicat pentru type: $type.")
            return report
        }
        handlers[type] = handler
        report.info("Handler inregistrat: $type.")
        return report
    }

    fun find(type: String?): Optional<T> = Optional.ofNullable(handlers[normalize(type)])

    fun supports(type: String?): Boolean = find(type).isPresent

    fun validateDefinition(definition: ScenarioRuntimeDefinition?, label: String?): ScenarioValidationReport {
        val report = ScenarioValidationReport()
        val safeLabel = normalizeLabel(label)
        if (definition == null) {
            report.error("$safeLabel lipsa.")
            return report
        }
        if (definition.id().isBlank()) {
            report.warn("$safeLabel fara id stabil.")
        }
        if (definition.type().isBlank()) {
            report.error("$safeLabel fara type.")
            return report
        }
        if (!supports(definition.type())) {
            report.error("Handler lipsa pentru $safeLabel type: ${normalize(definition.type())}.")
            return report
        }
        report.info("$safeLabel validata cu handler: ${normalize(definition.type())}.")
        return report
    }

    fun handlers(): Map<String, T> = Collections.unmodifiableMap(LinkedHashMap(handlers))

    private fun normalize(value: String?): String = value?.trim()?.lowercase().orEmpty()

    private fun normalizeLabel(value: String?): String {
        val normalized = normalize(value)
        return if (normalized.isBlank()) "definitie runtime" else normalized
    }
}
