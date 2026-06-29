package ro.ainpc.progression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerProgressionMathTest {

    @Test
    fun `xpRequiredForLevel grows quadratically`() {
        val math = PlayerProgressionMath()
        assertEquals(100L, math.xpRequiredForLevel(1))
        assertEquals(300L, math.xpRequiredForLevel(2))
        assertEquals(600L, math.xpRequiredForLevel(3))
        assertEquals(1000L, math.xpRequiredForLevel(4))
    }

    @Test
    fun `cumulativeXpForLevel sums prior levels`() {
        val math = PlayerProgressionMath()
        assertEquals(0L, math.cumulativeXpForLevel(1))
        assertEquals(100L, math.cumulativeXpForLevel(2))
        assertEquals(100L + 300L, math.cumulativeXpForLevel(3))
        assertEquals(100L + 300L + 600L, math.cumulativeXpForLevel(4))
    }

    @Test
    fun `levelForTotalXp returns correct level`() {
        val math = PlayerProgressionMath()
        assertEquals(1, math.levelForTotalXp(0L))
        assertEquals(1, math.levelForTotalXp(99L))
        assertEquals(2, math.levelForTotalXp(100L))
        assertEquals(2, math.levelForTotalXp(399L))
        assertEquals(3, math.levelForTotalXp(400L))
        assertEquals(4, math.levelForTotalXp(100L + 300L + 600L))
    }

    @Test
    fun `level is clamped to sane values`() {
        val math = PlayerProgressionMath()
        assertEquals(100L, math.xpRequiredForLevel(0))
        assertEquals(100L, math.xpRequiredForLevel(-5))
        assertEquals(1, math.levelForTotalXp(-1L))
    }

    @Test
    fun `cumulativeXpForLevel rejects bogus levels`() {
        val math = PlayerProgressionMath()
        assertEquals(0L, math.cumulativeXpForLevel(0))
        assertEquals(0L, math.cumulativeXpForLevel(-1))
    }
}

class PlayerProgressionInMemoryStoreTest {

    @Test
    fun `grantXp applies and tracks level changes`() {
        val store = InMemoryProgressionStore()
        val grant = store.grantXp("player1", 100L)
        assertEquals(2, store.snapshots["player1"]?.level)
        assertEquals(100L, grant.xpGranted)
        val second = store.grantXp("player1", 50L)
        assertEquals(0, second.levelsGained)
        val third = store.grantXp("player1", 600L)
        assertTrue(third.levelsGained >= 1)
    }

    @Test
    fun `addSkillXp accumulates per skill`() {
        val store = InMemoryProgressionStore()
        store.addSkillXp("player1", "combat", 10)
        store.addSkillXp("player1", "combat", 5)
        val snap = store.snapshots["player1"]
        assertEquals(15, snap?.skills?.get("combat"))
    }

    @Test
    fun `setLevel rewrites totalXp to match target`() {
        val store = InMemoryProgressionStore()
        store.grantXp("player1", 500L)
        val result = store.setLevel("player1", 3)
        assertEquals(3, result.level)
        assertTrue(result.totalXp >= 100L + 300L)
    }

    @Test
    fun `setSkillLevel sets skill XP to match target level`() {
        val store = InMemoryProgressionStore()
        store.addSkillXp("player1", "combat", 50)
        val result = store.setSkillLevel("player1", "combat", 2)
        val skillXp = result.skills["combat"] ?: 0
        assertEquals(2, store.getSkillLevel("player1", "combat"))
        assertTrue(skillXp >= 100L, "skill XP should be at least 100 (cumulative for level 2), was $skillXp")
    }

    @Test
    fun `setSkillLevel does not change player level`() {
        val store = InMemoryProgressionStore()
        store.grantXp("player1", 350L)
        val before = store.snapshots["player1"]?.level ?: 0
        val result = store.setSkillLevel("player1", "combat", 3)
        assertEquals(before, result.level)
    }

    @Test
    fun `setSkillLevel with empty skillId returns current snapshot without changes`() {
        val store = InMemoryProgressionStore()
        store.addSkillXp("player1", "combat", 50)
        val result = store.setSkillLevel("player1", "", 5)
        assertEquals(50, result.skills["combat"])
        assertEquals(emptyList<String>(), result.skills.keys.filter { it != "combat" })
    }

    @Test
    fun `getSkillLevel returns 1 for unknown skill`() {
        val store = InMemoryProgressionStore()
        assertEquals(1, store.getSkillLevel("player1", "unknown"))
    }

    @Test
    fun `getSkillLevel with empty skillId returns 1`() {
        val store = InMemoryProgressionStore()
        assertEquals(1, store.getSkillLevel("player1", ""))
    }

    @Test
    fun `skill levels are independent across skills`() {
        val store = InMemoryProgressionStore()
        store.addSkillXp("player1", "combat", 100)
        store.addSkillXp("player1", "mining", 250)
        assertEquals(2, store.getSkillLevel("player1", "combat"))
        assertEquals(2, store.getSkillLevel("player1", "mining"))
        store.addSkillXp("player1", "combat", 50)
        assertEquals(2, store.getSkillLevel("player1", "combat"))
        assertEquals(2, store.getSkillLevel("player1", "mining"))
    }

    @Test
    fun `setSkillLevel overwrites existing skill xp`() {
        val store = InMemoryProgressionStore()
        store.setSkillLevel("player1", "combat", 5)
        assertEquals(5, store.getSkillLevel("player1", "combat"))
        store.setSkillLevel("player1", "combat", 1)
        assertEquals(1, store.getSkillLevel("player1", "combat"))
    }

    @Test
    fun `multiple players have independent skill levels`() {
        val store = InMemoryProgressionStore()
        store.setSkillLevel("player1", "combat", 5)
        store.setSkillLevel("player2", "combat", 2)
        assertEquals(5, store.getSkillLevel("player1", "combat"))
        assertEquals(2, store.getSkillLevel("player2", "combat"))
    }

    @Test
    fun `resetPlayer returns a fresh level 1 snapshot`() {
        val store = InMemoryProgressionStore()
        store.grantXp("player1", 500L)
        val snap = store.resetPlayer("player1")
        assertEquals(1, snap.level)
        assertEquals(0L, snap.totalXp)
        assertTrue(snap.skills.isEmpty())
    }
}

class PlayerProgressionMath {
    fun xpRequiredForLevel(level: Int): Long {
        val safe = level.coerceAtLeast(1)
        return (100L * safe) + (50L * (safe - 1) * safe)
    }

    fun cumulativeXpForLevel(level: Int): Long {
        val safe = level.coerceAtLeast(1)
        var total = 0L
        for (lvl in 1 until safe) {
            total += xpRequiredForLevel(lvl)
        }
        return total
    }

    fun levelForTotalXp(totalXp: Long): Int {
        val safe = totalXp.coerceAtLeast(0L)
        var level = 1
        var accumulated = 0L
        while (true) {
            val required = xpRequiredForLevel(level)
            if (accumulated + required > safe) return level
            accumulated += required
            level += 1
            if (level > 200) return 200
        }
    }
}

class InMemoryProgressionStore {
    private val math = PlayerProgressionMath()
    val snapshots: MutableMap<String, ro.ainpc.api.PlayerProgressionSnapshot> = mutableMapOf()

    fun grantXp(playerUuid: String, amount: Long): ro.ainpc.api.PlayerProgressionGrant {
        val before = snapshots[playerUuid] ?: freshSnapshot(playerUuid)
        val newTotalXp = (before.totalXp + amount.coerceAtLeast(0L)).coerceAtLeast(0L)
        val newLevel = math.levelForTotalXp(newTotalXp)
        val snap = ro.ainpc.api.PlayerProgressionSnapshot(
            playerUuid = playerUuid,
            level = newLevel,
            xp = newTotalXp - math.cumulativeXpForLevel(newLevel),
            xpToNextLevel = math.xpRequiredForLevel(newLevel),
            totalXp = newTotalXp,
            skills = before.skills,
            lastUpdated = System.currentTimeMillis(),
        )
        snapshots[playerUuid] = snap
        return ro.ainpc.api.PlayerProgressionGrant(
            playerUuid = playerUuid,
            xpGranted = amount,
            levelsGained = (newLevel - before.level).coerceAtLeast(0),
            skillsGained = emptyMap(),
            snapshot = snap,
        )
    }

    fun addSkillXp(playerUuid: String, skillId: String, amount: Int): ro.ainpc.api.PlayerProgressionGrant {
        val normalized = skillId.trim().lowercase()
        val before = snapshots[playerUuid] ?: freshSnapshot(playerUuid)
        val skills = LinkedHashMap(before.skills)
        val current = skills[normalized] ?: 0
        skills[normalized] = current + amount
        val snap = before.copy(skills = skills, lastUpdated = System.currentTimeMillis())
        snapshots[playerUuid] = snap
        return ro.ainpc.api.PlayerProgressionGrant(
            playerUuid = playerUuid,
            xpGranted = 0L,
            levelsGained = 0,
            skillsGained = mapOf(normalized to amount),
            snapshot = snap,
        )
    }

    fun setLevel(playerUuid: String, level: Int): ro.ainpc.api.PlayerProgressionSnapshot {
        val before = snapshots[playerUuid] ?: freshSnapshot(playerUuid)
        val totalXp = math.cumulativeXpForLevel(level)
        val snap = ro.ainpc.api.PlayerProgressionSnapshot(
            playerUuid = playerUuid,
            level = level.coerceAtLeast(1),
            xp = totalXp - math.cumulativeXpForLevel(level.coerceAtLeast(1)),
            xpToNextLevel = math.xpRequiredForLevel(level.coerceAtLeast(1)),
            totalXp = totalXp,
            skills = before.skills,
            lastUpdated = System.currentTimeMillis(),
        )
        snapshots[playerUuid] = snap
        return snap
    }

    fun setSkillLevel(playerUuid: String, skillId: String, level: Int): ro.ainpc.api.PlayerProgressionSnapshot {
        val normalizedSkill = skillId.trim().lowercase()
        if (normalizedSkill.isEmpty()) return snapshots[playerUuid] ?: freshSnapshot(playerUuid)
        val before = snapshots[playerUuid] ?: freshSnapshot(playerUuid)
        val targetSkillXp = math.cumulativeXpForLevel(level.coerceAtLeast(1)).toInt()
        val newSkills = LinkedHashMap(before.skills)
        newSkills[normalizedSkill] = targetSkillXp
        val snap = before.copy(skills = newSkills, lastUpdated = System.currentTimeMillis())
        snapshots[playerUuid] = snap
        return snap
    }

    fun getSkillLevel(playerUuid: String, skillId: String): Int {
        val normalizedSkill = skillId.trim().lowercase()
        if (normalizedSkill.isEmpty()) return 1
        val snapshot = snapshots[playerUuid] ?: return 1
        val skillXp = snapshot.skills[normalizedSkill] ?: 0
        return math.levelForTotalXp(skillXp.toLong())
    }

    fun resetPlayer(playerUuid: String): ro.ainpc.api.PlayerProgressionSnapshot {
        val fresh = freshSnapshot(playerUuid)
        snapshots[playerUuid] = fresh
        return fresh
    }

    private fun freshSnapshot(playerUuid: String): ro.ainpc.api.PlayerProgressionSnapshot {
        return ro.ainpc.api.PlayerProgressionSnapshot(
            playerUuid = playerUuid,
            level = 1,
            xp = 0L,
            xpToNextLevel = math.xpRequiredForLevel(1),
            totalXp = 0L,
            skills = emptyMap(),
            lastUpdated = 0L,
        )
    }
}
