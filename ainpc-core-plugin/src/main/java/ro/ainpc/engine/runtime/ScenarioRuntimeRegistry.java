package ro.ainpc.engine.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ScenarioRuntimeRegistry<T extends ScenarioRuntimeHandler> {

    private final Map<String, T> handlers = new LinkedHashMap<>();

    public ScenarioValidationReport register(T handler) {
        ScenarioValidationReport report = new ScenarioValidationReport();
        if (handler == null) {
            report.error("Handler null.");
            return report;
        }
        String type = normalize(handler.type());
        if (type.isBlank()) {
            report.error("Handler fara type.");
            return report;
        }
        if (handlers.containsKey(type)) {
            report.error("Handler duplicat pentru type: " + type + ".");
            return report;
        }
        handlers.put(type, handler);
        report.info("Handler inregistrat: " + type + ".");
        return report;
    }

    public Optional<T> find(String type) {
        return Optional.ofNullable(handlers.get(normalize(type)));
    }

    public boolean supports(String type) {
        return find(type).isPresent();
    }

    public ScenarioValidationReport validateDefinition(ScenarioRuntimeDefinition definition, String label) {
        ScenarioValidationReport report = new ScenarioValidationReport();
        String safeLabel = normalizeLabel(label);
        if (definition == null) {
            report.error(safeLabel + " lipsa.");
            return report;
        }
        if (definition.id().isBlank()) {
            report.warn(safeLabel + " fara id stabil.");
        }
        if (definition.type().isBlank()) {
            report.error(safeLabel + " fara type.");
            return report;
        }
        if (!supports(definition.type())) {
            report.error("Handler lipsa pentru " + safeLabel + " type: " + normalize(definition.type()) + ".");
            return report;
        }
        report.info(safeLabel + " validata cu handler: " + normalize(definition.type()) + ".");
        return report;
    }

    public Map<String, T> handlers() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(handlers));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeLabel(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? "definitie runtime" : normalized;
    }
}
