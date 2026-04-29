package ro.ainpc.world.scan;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class VanillaVillageScanner {

    public static final int DEFAULT_HORIZONTAL_RADIUS = 48;
    public static final int DEFAULT_VERTICAL_RADIUS = 16;
    public static final int MAX_HORIZONTAL_RADIUS = 96;
    public static final int MAX_VERTICAL_RADIUS = 32;

    private static final Set<Material> WORKSTATIONS = EnumSet.of(
        Material.BARREL,
        Material.BLAST_FURNACE,
        Material.BREWING_STAND,
        Material.CARTOGRAPHY_TABLE,
        Material.CAULDRON,
        Material.COMPOSTER,
        Material.FLETCHING_TABLE,
        Material.GRINDSTONE,
        Material.LECTERN,
        Material.LOOM,
        Material.SMITHING_TABLE,
        Material.SMOKER,
        Material.STONECUTTER
    );

    public VanillaVillageScanResult scan(Location center, int horizontalRadius, int verticalRadius) {
        if (center == null || center.getWorld() == null) {
            throw new IllegalArgumentException("Locatia de scanare trebuie sa aiba o lume valida.");
        }

        World world = center.getWorld();
        int safeHorizontalRadius = clamp(horizontalRadius, 8, MAX_HORIZONTAL_RADIUS);
        int safeVerticalRadius = clamp(verticalRadius, 4, MAX_VERTICAL_RADIUS);
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        int minY = Math.max(world.getMinHeight(), centerY - safeVerticalRadius);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + safeVerticalRadius);

        List<VanillaVillageFeature> features = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (int x = centerX - safeHorizontalRadius; x <= centerX + safeHorizontalRadius; x++) {
            for (int z = centerZ - safeHorizontalRadius; z <= centerZ + safeHorizontalRadius; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material material = block.getType();
                    VanillaVillageFeatureType type = classify(material);
                    if (type != null) {
                        features.add(new VanillaVillageFeature(type, material.name(), x, y, z));
                    }
                }
            }
        }

        if (features.isEmpty()) {
            warnings.add("Nu au fost gasite semnale vanilla de sat in raza scanata.");
        } else if (features.stream().noneMatch(feature -> feature.type() == VanillaVillageFeatureType.BELL)) {
            warnings.add("Nu a fost gasit niciun clopot. Satul poate fi incomplet sau scanarea este prea mica.");
        }
        if (horizontalRadius > MAX_HORIZONTAL_RADIUS) {
            warnings.add("Raza orizontala a fost limitata la " + MAX_HORIZONTAL_RADIUS + " blocuri.");
        }
        if (verticalRadius > MAX_VERTICAL_RADIUS) {
            warnings.add("Raza verticala a fost limitata la " + MAX_VERTICAL_RADIUS + " blocuri.");
        }

        return new VanillaVillageScanResult(
            world.getName(),
            centerX,
            centerY,
            centerZ,
            safeHorizontalRadius,
            safeVerticalRadius,
            minY,
            maxY,
            features,
            warnings
        );
    }

    private VanillaVillageFeatureType classify(Material material) {
        if (material == null || material.isAir()) {
            return null;
        }
        if (material == Material.BELL) {
            return VanillaVillageFeatureType.BELL;
        }
        if (material == Material.FARMLAND) {
            return VanillaVillageFeatureType.FARMLAND;
        }
        if (WORKSTATIONS.contains(material)) {
            return VanillaVillageFeatureType.WORKSTATION;
        }

        String name = material.name();
        if (name.endsWith("_BED")) {
            return VanillaVillageFeatureType.BED;
        }
        if (name.endsWith("_DOOR")) {
            return VanillaVillageFeatureType.DOOR;
        }
        return null;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
