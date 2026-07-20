package ro.ainpc.world.scan

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import java.util.EnumSet

class VanillaVillageScanner {
    @Deprecated("Fluxurile de productie trebuie sa foloseasca VanillaVillageScanService.submit.")
    fun scan(center: Location?, horizontalRadius: Int, verticalRadius: Int): VanillaVillageScanResult {
        val session = beginScan(center, horizontalRadius, verticalRadius)
        while (!session.isComplete) {
            session.scanNext(Int.MAX_VALUE)
        }
        return session.result()
    }

    fun beginScan(center: Location?, horizontalRadius: Int, verticalRadius: Int): VanillaVillageScanSession {
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

        return VanillaVillageScanSession(
            worldName = world.name,
            centerX = centerX,
            centerY = centerY,
            centerZ = centerZ,
            horizontalRadius = safeHorizontalRadius,
            verticalRadius = safeVerticalRadius,
            minY = minY,
            maxY = maxY,
            horizontalRadiusClamped = horizontalRadius > MAX_HORIZONTAL_RADIUS,
            verticalRadiusClamped = verticalRadius > MAX_VERTICAL_RADIUS,
            materialAt = { x, y, z -> world.getBlockAt(x, y, z).type },
            classifier = ::classify,
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
        const val DEFAULT_HORIZONTAL_RADIUS = 48

        const val DEFAULT_VERTICAL_RADIUS = 16

        const val MAX_HORIZONTAL_RADIUS = 96

        const val MAX_VERTICAL_RADIUS = 32

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

class VanillaVillageScanSession internal constructor(
    private val worldName: String,
    private val centerX: Int,
    private val centerY: Int,
    private val centerZ: Int,
    private val horizontalRadius: Int,
    private val verticalRadius: Int,
    private val minY: Int,
    private val maxY: Int,
    private val horizontalRadiusClamped: Boolean,
    private val verticalRadiusClamped: Boolean,
    private val materialAt: (x: Int, y: Int, z: Int) -> Material,
    private val classifier: (Material?) -> VanillaVillageFeatureType?,
) {
    private val ownerThread = Thread.currentThread()
    private val features = mutableListOf<VanillaVillageFeature>()
    private val cursor = VanillaVillageScanCursor(
        minX = centerX - horizontalRadius,
        maxX = centerX + horizontalRadius,
        minY = minY,
        maxY = maxY,
        minZ = centerZ - horizontalRadius,
        maxZ = centerZ + horizontalRadius,
    )

    val totalBlocks: Long get() = cursor.totalBlocks

    val scannedBlocks: Long get() = cursor.scannedBlocks

    val isComplete: Boolean get() = cursor.isComplete

    fun scanNext(blockBudget: Int): Int {
        check(Thread.currentThread() === ownerThread) {
            "Sesiunea de scanare trebuie continuata pe acelasi thread pe care a fost creata."
        }
        return cursor.consume(blockBudget) { x, y, z ->
            val material = materialAt(x, y, z)
            val type = classifier(material)
            if (type != null) {
                features.add(VanillaVillageFeature(type, material.name, x, y, z))
            }
        }
    }

    fun result(): VanillaVillageScanResult {
        check(isComplete) { "Scanarea nu este finalizata." }
        val warnings = mutableListOf<String>()
        if (features.isEmpty()) {
            warnings.add("Nu au fost gasite semnale vanilla de sat in raza scanata.")
        } else if (features.none { it.type() == VanillaVillageFeatureType.BELL }) {
            warnings.add("Nu a fost gasit niciun clopot. Satul poate fi incomplet sau scanarea este prea mica.")
        }
        if (horizontalRadiusClamped) {
            warnings.add("Raza orizontala a fost limitata la ${VanillaVillageScanner.MAX_HORIZONTAL_RADIUS} blocuri.")
        }
        if (verticalRadiusClamped) {
            warnings.add("Raza verticala a fost limitata la ${VanillaVillageScanner.MAX_VERTICAL_RADIUS} blocuri.")
        }

        return VanillaVillageScanResult(
            worldName,
            centerX,
            centerY,
            centerZ,
            horizontalRadius,
            verticalRadius,
            minY,
            maxY,
            features,
            warnings,
        )
    }
}

internal class VanillaVillageScanCursor(
    private val minX: Int,
    private val maxX: Int,
    private val minY: Int,
    private val maxY: Int,
    private val minZ: Int,
    private val maxZ: Int,
) {
    private var nextX = minX
    private var nextY = minY
    private var nextZ = minZ

    val totalBlocks: Long =
        (maxX.toLong() - minX + 1L) *
            (maxY.toLong() - minY + 1L) *
            (maxZ.toLong() - minZ + 1L)

    var scannedBlocks: Long = 0L
        private set

    val isComplete: Boolean get() = scannedBlocks >= totalBlocks

    init {
        require(minX <= maxX && minY <= maxY && minZ <= maxZ) {
            "Volumul de scanare trebuie sa aiba limite valide."
        }
    }

    fun consume(blockBudget: Int, consumer: (x: Int, y: Int, z: Int) -> Unit): Int {
        require(blockBudget > 0) { "Bugetul de blocuri trebuie sa fie pozitiv." }
        var processed = 0
        while (processed < blockBudget && !isComplete) {
            consumer(nextX, nextY, nextZ)
            scannedBlocks++
            processed++
            advance()
        }
        return processed
    }

    private fun advance() {
        nextY++
        if (nextY <= maxY) return
        nextY = minY
        nextZ++
        if (nextZ <= maxZ) return
        nextZ = minZ
        nextX++
    }
}
