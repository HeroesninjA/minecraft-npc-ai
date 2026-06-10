package ro.ainpc.world.scan

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import java.util.EnumSet

class VanillaVillageScanner {
    fun scan(center: Location?, horizontalRadius: Int, verticalRadius: Int): VanillaVillageScanResult {
        require(!(center == null || center.world == null)) {
            "Locatia de scanare trebuie sa aiba o lume valida."
        }

        val world: World = center.world
        val safeHorizontalRadius = clamp(horizontalRadius, 8, MAX_HORIZONTAL_RADIUS)
        val safeVerticalRadius = clamp(verticalRadius, 4, MAX_VERTICAL_RADIUS)
        val centerX = center.blockX
        val centerY = center.blockY
        val centerZ = center.blockZ
        val minY = maxOf(world.minHeight, centerY - safeVerticalRadius)
        val maxY = minOf(world.maxHeight - 1, centerY + safeVerticalRadius)

        val features = mutableListOf<VanillaVillageFeature>()
        val warnings = mutableListOf<String>()
        for (x in centerX - safeHorizontalRadius..centerX + safeHorizontalRadius) {
            for (z in centerZ - safeHorizontalRadius..centerZ + safeHorizontalRadius) {
                for (y in minY..maxY) {
                    val block = world.getBlockAt(x, y, z)
                    val material = block.type
                    val type = classify(material)
                    if (type != null) {
                        features.add(VanillaVillageFeature(type, material.name, x, y, z))
                    }
                }
            }
        }

        if (features.isEmpty()) {
            warnings.add("Nu au fost gasite semnale vanilla de sat in raza scanata.")
        } else if (features.none { it.type() == VanillaVillageFeatureType.BELL }) {
            warnings.add("Nu a fost gasit niciun clopot. Satul poate fi incomplet sau scanarea este prea mica.")
        }
        if (horizontalRadius > MAX_HORIZONTAL_RADIUS) {
            warnings.add("Raza orizontala a fost limitata la $MAX_HORIZONTAL_RADIUS blocuri.")
        }
        if (verticalRadius > MAX_VERTICAL_RADIUS) {
            warnings.add("Raza verticala a fost limitata la $MAX_VERTICAL_RADIUS blocuri.")
        }

        return VanillaVillageScanResult(
            world.name,
            centerX,
            centerY,
            centerZ,
            safeHorizontalRadius,
            safeVerticalRadius,
            minY,
            maxY,
            features,
            warnings
        )
    }

    private fun classify(material: Material?): VanillaVillageFeatureType? {
        if (material == null || material.isAir) {
            return null
        }
        if (material == Material.BELL) {
            return VanillaVillageFeatureType.BELL
        }
        if (material == Material.FARMLAND) {
            return VanillaVillageFeatureType.FARMLAND
        }
        if (WORKSTATIONS.contains(material)) {
            return VanillaVillageFeatureType.WORKSTATION
        }

        val name = material.name
        if (name.endsWith("_BED")) {
            return VanillaVillageFeatureType.BED
        }
        if (name.endsWith("_DOOR")) {
            return VanillaVillageFeatureType.DOOR
        }
        return null
    }

    private fun clamp(value: Int, min: Int, max: Int): Int = maxOf(min, minOf(max, value))

    companion object {
        @JvmField
        val DEFAULT_HORIZONTAL_RADIUS = 48

        @JvmField
        val DEFAULT_VERTICAL_RADIUS = 16

        @JvmField
        val MAX_HORIZONTAL_RADIUS = 96

        @JvmField
        val MAX_VERTICAL_RADIUS = 32

        private val WORKSTATIONS = EnumSet.of(
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
        )
    }
}
