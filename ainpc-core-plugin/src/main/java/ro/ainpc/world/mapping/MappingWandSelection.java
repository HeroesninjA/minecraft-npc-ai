package ro.ainpc.world.mapping;

import java.util.Optional;

public record MappingWandSelection(MappingPoint pos1, MappingPoint pos2, MappingPoint point) {

    public MappingWandSelection {
    }

    public static MappingWandSelection empty() {
        return new MappingWandSelection(null, null, null);
    }

    public MappingWandSelection withPos1(MappingPoint value) {
        return new MappingWandSelection(value, pos2, point);
    }

    public MappingWandSelection withPos2(MappingPoint value) {
        return new MappingWandSelection(pos1, value, point);
    }

    public MappingWandSelection withPoint(MappingPoint value) {
        return new MappingWandSelection(pos1, pos2, value);
    }

    public Optional<MappingBounds> bounds() {
        if (pos1 == null || pos2 == null || !pos1.hasWorld() || !pos2.hasWorld()) {
            return Optional.empty();
        }
        if (!pos1.worldName().equalsIgnoreCase(pos2.worldName())) {
            return Optional.empty();
        }
        return Optional.of(new MappingBounds(
            pos1.worldName(),
            Math.min(pos1.x(), pos2.x()),
            Math.min(pos1.y(), pos2.y()),
            Math.min(pos1.z(), pos2.z()),
            Math.max(pos1.x(), pos2.x()),
            Math.max(pos1.y(), pos2.y()),
            Math.max(pos1.z(), pos2.z())
        ));
    }

    public boolean hasPoint() {
        return point != null && point.hasWorld();
    }

    public record MappingBounds(String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public int centerX() {
            return minX + ((maxX - minX) / 2);
        }

        public int centerY() {
            return minY + ((maxY - minY) / 2);
        }

        public int centerZ() {
            return minZ + ((maxZ - minZ) / 2);
        }

        public String format() {
            return worldName + " " + minX + " " + minY + " " + minZ
                + " -> " + maxX + " " + maxY + " " + maxZ;
        }
    }
}
