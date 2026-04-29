package ro.ainpc.spawn;

import org.bukkit.Location;
import ro.ainpc.npc.AINPC;

import java.util.Objects;

public record ResolvedNpcSpawnPlan(
    NpcSpawnPlan plan,
    Location spawnLocation,
    AINPC.OwnedLocation homeAnchor,
    AINPC.OwnedLocation workAnchor,
    AINPC.OwnedLocation socialAnchor
) {
    public ResolvedNpcSpawnPlan {
        Objects.requireNonNull(plan, "plan");
        spawnLocation = spawnLocation == null ? null : spawnLocation.clone();
    }
}
