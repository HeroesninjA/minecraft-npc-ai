package ro.ainpc.routine;

import ro.ainpc.npc.AINPC;
import ro.ainpc.npc.NPCState;

import java.util.List;

public class RoutineEngine {

    private static final List<SchedulePoint> DAY_PREVIEW_POINTS = List.of(
        new SchedulePoint("Noapte", 19000L),
        new SchedulePoint("Dimineata", 6000L),
        new SchedulePoint("Pranz", 13000L),
        new SchedulePoint("Seara", 17000L)
    );

    public RoutineAssignment assign(AINPC npc, long worldTime) {
        if (npc == null) {
            return idle("nu exista NPC valid");
        }

        long time = normalizeWorldTime(worldTime);
        if (time >= 18000 || time < 2000) {
            return home(npc, time >= 18000
                ? "doarme acasa"
                : "se trezeste si isi pregateste ziua");
        }

        if (npc.getEnergyLevel() < 25 || npc.getHungerLevel() < 25 || npc.getSafetyLevel() < 35) {
            return home(npc, "revine acasa pentru siguranta si refacere");
        }

        if (npc.getSocialNeedLevel() < 35 && hasAnchor(npc.getSocialAnchor()) && time >= 8000 && time < 18000) {
            return social(npc, "merge la punctul social pentru a vorbi cu localnicii");
        }

        if (time >= 2000 && time < 12000) {
            return hasAnchor(npc.getWorkAnchor())
                ? work(npc, "merge la lucru")
                : fallbackDay(npc, "nu are loc de munca mapat");
        }

        if (time >= 12000 && time < 16000) {
            return hasAnchor(npc.getWorkAnchor())
                ? work(npc, "inchide treburile principale ale zilei")
                : fallbackDay(npc, "nu are loc de munca mapat pentru dupa-amiaza");
        }

        if (time >= 16000 && time < 18000) {
            return hasAnchor(npc.getSocialAnchor())
                ? social(npc, "se intalneste cu localnicii seara")
                : home(npc, "se intoarce acasa seara");
        }

        return idle("isi urmeaza rutina obisnuita");
    }

    public List<RoutineScheduleEntry> previewDay(AINPC npc) {
        return DAY_PREVIEW_POINTS.stream()
            .map(point -> new RoutineScheduleEntry(point.label(), point.worldTime(), assign(npc, point.worldTime())))
            .toList();
    }

    private RoutineAssignment fallbackDay(AINPC npc, String reason) {
        if (hasAnchor(npc.getSocialAnchor())) {
            return social(npc, reason + "; foloseste punctul social");
        }
        if (hasAnchor(npc.getHomeAnchor())) {
            return home(npc, reason + "; ramane aproape de casa");
        }
        return idle(reason);
    }

    private RoutineAssignment home(AINPC npc, String activity) {
        NPCState state = activity.contains("doarme") ? NPCState.SLEEPING : NPCState.RESTING;
        return new RoutineAssignment(
            RoutineSlot.HOME,
            activity,
            "sa fie acasa",
            state,
            npc.getHomeAnchor()
        );
    }

    private RoutineAssignment work(AINPC npc, String activity) {
        return new RoutineAssignment(
            RoutineSlot.WORK,
            activity,
            "sa lucreze la " + describeAnchor(npc.getWorkAnchor(), "locul de munca"),
            workStateFor(npc.getOccupation()),
            npc.getWorkAnchor()
        );
    }

    private RoutineAssignment social(AINPC npc, String activity) {
        return new RoutineAssignment(
            RoutineSlot.SOCIAL,
            activity,
            "sa socializeze la " + describeAnchor(npc.getSocialAnchor(), "punctul social"),
            NPCState.SOCIALIZING,
            npc.getSocialAnchor()
        );
    }

    private RoutineAssignment idle(String reason) {
        return new RoutineAssignment(
            RoutineSlot.IDLE,
            reason,
            "sa astepte pana exista o ancora utila",
            NPCState.IDLE,
            null
        );
    }

    private NPCState workStateFor(String occupation) {
        String normalized = occupation == null ? "" : occupation.toLowerCase();
        if (containsAny(normalized, "fermier", "farmer", "pastor")) {
            return NPCState.FARMING;
        }
        if (containsAny(normalized, "miner")) {
            return NPCState.MINING;
        }
        if (containsAny(normalized, "pescar", "fisher")) {
            return NPCState.FISHING;
        }
        if (containsAny(normalized, "fierar", "tamplar", "mason", "pietrar", "croitor", "brutar")) {
            return NPCState.CRAFTING;
        }
        if (containsAny(normalized, "soldat", "guard", "garda", "paznic")) {
            return NPCState.GUARDING;
        }
        return NPCState.WORKING;
    }

    private boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnchor(AINPC.OwnedLocation anchor) {
        return anchor != null && anchor.worldName() != null && !anchor.worldName().isBlank();
    }

    private String describeAnchor(AINPC.OwnedLocation anchor, String fallback) {
        if (anchor == null || anchor.label() == null || anchor.label().isBlank()) {
            return fallback;
        }
        return anchor.label();
    }

    private long normalizeWorldTime(long worldTime) {
        long normalized = worldTime % 24000L;
        return normalized < 0 ? normalized + 24000L : normalized;
    }

    private record SchedulePoint(String label, long worldTime) {
    }
}
