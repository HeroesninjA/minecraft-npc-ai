package ro.ainpc.spawn;

import java.util.List;

public record HouseAllocationValidationResult(
    boolean valid,
    List<String> errors,
    List<String> warnings
) {
    public HouseAllocationValidationResult {
        errors = List.copyOf(errors != null ? errors : List.of());
        warnings = List.copyOf(warnings != null ? warnings : List.of());
        valid = errors.isEmpty();
    }
}
