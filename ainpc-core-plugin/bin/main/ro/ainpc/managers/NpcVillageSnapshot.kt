package ro.ainpc.managers

import org.bukkit.Location

class NpcVillageSnapshot(
    private val center: Location,
    private val bedLocations: List<Location>,
    private val villagerCount: Int,
) {
    fun center(): Location = center

    fun bedLocations(): List<Location> = bedLocations

    fun villagerCount(): Int = villagerCount
}
