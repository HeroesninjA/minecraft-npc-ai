package ro.ainpc.api

interface PlayerProgressionApi {
    fun getSnapshot(playerUuid: String): PlayerProgressionSnapshot
    fun grantXp(playerUuid: String, amount: Long): PlayerProgressionGrant
    fun addSkillXp(playerUuid: String, skillId: String, amount: Int): PlayerProgressionGrant
    fun setLevel(playerUuid: String, level: Int): PlayerProgressionSnapshot
    fun resetPlayer(playerUuid: String): PlayerProgressionSnapshot
    fun xpRequiredForLevel(level: Int): Long
    fun levelForTotalXp(totalXp: Long): Int
}

data class PlayerProgressionSnapshot(
    val playerUuid: String,
    val level: Int,
    val xp: Long,
    val xpToNextLevel: Long,
    val totalXp: Long,
    val skills: Map<String, Int>,
    val lastUpdated: Long,
)

data class PlayerProgressionGrant(
    val playerUuid: String,
    val xpGranted: Long,
    val levelsGained: Int,
    val skillsGained: Map<String, Int>,
    val snapshot: PlayerProgressionSnapshot,
)
