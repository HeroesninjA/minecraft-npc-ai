package ro.ainpc.routine;

public record RoutineScheduleEntry(
    String label,
    long worldTime,
    RoutineAssignment assignment
) {
    public RoutineScheduleEntry {
        label = label == null ? "" : label.trim();
        worldTime = Math.max(0L, worldTime);
    }
}
