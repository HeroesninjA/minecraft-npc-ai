package ro.ainpc.engine.runtime;

import java.util.ArrayList;
import java.util.List;

public class ScenarioValidationReport {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> infos = new ArrayList<>();

    public void error(String message) {
        add(errors, message);
    }

    public void warn(String message) {
        add(warnings, message);
    }

    public void info(String message) {
        add(infos, message);
    }

    public boolean valid() {
        return errors.isEmpty();
    }

    public boolean isValid() {
        return valid();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public void merge(ScenarioValidationReport other) {
        if (other == null) {
            return;
        }
        errors.addAll(other.errors());
        warnings.addAll(other.warnings());
        infos.addAll(other.infos());
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    public List<String> infos() {
        return List.copyOf(infos);
    }

    private void add(List<String> target, String message) {
        if (message != null && !message.isBlank()) {
            target.add(message.trim());
        }
    }
}
