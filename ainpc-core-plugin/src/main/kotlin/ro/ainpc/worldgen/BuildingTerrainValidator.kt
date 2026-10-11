package ro.ainpc.worldgen

class BuildingTerrainValidator {
    data class Cell(val empty: Boolean, val supportsBuilding: Boolean)

    fun validate(
        operations: List<BlockOperation>,
        minHeight: Int,
        maxHeight: Int,
        chunkLoaded: (Int, Int) -> Boolean,
        readCell: (Int, Int, Int) -> Cell,
    ): List<String> {
        if (operations.isEmpty() || operations.size > 10000) return listOf("Building must contain 1..10000 operations")
        if (operations.map { it.worldName }.distinct().size != 1) return listOf("Building must target exactly one world")
        if (operations.any { it.y <= minHeight || it.y >= maxHeight }) return listOf("Building or foundation support exceeds world height")
        val chunks = operations.map { (it.x shr 4) to (it.z shr 4) }.distinct()
        if (chunks.any { !chunkLoaded(it.first, it.second) }) return listOf("Building crosses an unloaded chunk")
        val floorY = operations.minOf { it.y }
        val columns = operations.map { it.x to it.z }.distinct()
        val errors = mutableListOf<String>()
        for ((blockX, blockZ) in columns) {
            if (!readCell(blockX, floorY - 1, blockZ).supportsBuilding) {
                errors.add("Unsupported foundation at $blockX,${floorY - 1},$blockZ")
                if (errors.size >= 20) return errors
            }
        }
        for (operation in operations) {
            if (!readCell(operation.x, operation.y, operation.z).empty) {
                errors.add("Occupied building cell at ${operation.x},${operation.y},${operation.z}")
                if (errors.size >= 20) return errors
            }
        }
        return errors
    }
}
