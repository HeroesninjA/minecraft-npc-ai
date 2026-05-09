package ro.ainpc.spawn;

import ro.ainpc.npc.AINPC;

import java.util.List;

public record NpcSpawnResult(
    boolean success,
    boolean created,
    AINPC npc,
    List<String> errors,
    List<String> warnings
) {
    public NpcSpawnResult {
        errors = List.copyOf(errors != null ? errors : List.of());
        warnings = List.copyOf(warnings != null ? warnings : List.of());
    }

    public static NpcSpawnResult success(AINPC npc, List<String> warnings) {
        return created(npc, warnings);
    }

    public static NpcSpawnResult created(AINPC npc, List<String> warnings) {
        return new NpcSpawnResult(true, true, npc, List.of(), warnings);
    }

    public static NpcSpawnResult reused(AINPC npc, List<String> warnings) {
        return new NpcSpawnResult(true, false, npc, List.of(), warnings);
    }

    public static NpcSpawnResult failed(List<String> errors, List<String> warnings) {
        return new NpcSpawnResult(false, false, null, errors, warnings);
    }
}
