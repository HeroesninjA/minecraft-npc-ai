package ro.ainpc.spawn;

import java.util.List;

public record FamilyBindingResult(
    boolean success,
    String familyId,
    int relationsCreated,
    List<String> errors,
    List<String> warnings
) {
    public FamilyBindingResult {
        familyId = familyId == null ? "" : familyId.trim();
        errors = List.copyOf(errors != null ? errors : List.of());
        warnings = List.copyOf(warnings != null ? warnings : List.of());
    }
}
