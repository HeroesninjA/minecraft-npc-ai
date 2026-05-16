package ro.ainpc.world.mapping;

public record MappingPoint(String worldName, int x, int y, int z) {

    public MappingPoint {
        worldName = worldName == null ? "" : worldName.trim();
    }

    public boolean hasWorld() {
        return !worldName.isBlank();
    }

    public String format() {
        return worldName + " " + x + " " + y + " " + z;
    }
}
