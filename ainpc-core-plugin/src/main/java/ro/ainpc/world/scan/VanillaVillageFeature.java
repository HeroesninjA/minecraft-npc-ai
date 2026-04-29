package ro.ainpc.world.scan;

public record VanillaVillageFeature(
    VanillaVillageFeatureType type,
    String material,
    int x,
    int y,
    int z
) {
    public int horizontalDistanceSquared(VanillaVillageFeature other) {
        int dx = x - other.x;
        int dz = z - other.z;
        return dx * dx + dz * dz;
    }
}
