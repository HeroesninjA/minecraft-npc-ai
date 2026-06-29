package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition

class FormatRewardLabelTest {

    @Test
    fun `experience reward has vanilla XP label`() {
        val reward = QuestEntryDefinition("experience", null, 50, "XP reward")
        assertEquals("50 XP vanilla", formatRewardLabel(reward))
    }

    @Test
    fun `economy money reward has monede label`() {
        val reward = QuestEntryDefinition("economy:money", null, 100, "Coins")
        assertEquals("100 monede", formatRewardLabel(reward))
    }

    @Test
    fun `progression xp reward has custom XP label`() {
        val reward = QuestEntryDefinition("progression:xp", null, 200, "Custom XP")
        assertEquals("200 XP progresie", formatRewardLabel(reward))
    }

    @Test
    fun `progression level reward has level assignment label`() {
        val reward = QuestEntryDefinition("progression:level", null, 5, "Level up")
        assertEquals("nivel progresie = 5", formatRewardLabel(reward))
    }

    @Test
    fun `progression skill reward has skill XP label`() {
        val reward = QuestEntryDefinition("progression:skill", "combat", 25, "Combat skill")
        assertEquals("25 XP skill combat", formatRewardLabel(reward))
    }

    @Test
    fun `progression skill without itemId shows placeholder`() {
        val reward = QuestEntryDefinition("progression:skill", null, 25, "Skill XP")
        assertEquals("25 XP skill ?", formatRewardLabel(reward))
    }

    @Test
    fun `progression skill level reward shows level assignment label`() {
        val reward = QuestEntryDefinition("progression:skill_level", "combat", 3, "Combat lvl 3")
        assertEquals("nivel skill combat = 3", formatRewardLabel(reward))
    }

    @Test
    fun `progression skill level without itemId shows placeholder`() {
        val reward = QuestEntryDefinition("progression:skill_level", null, 5, "Skill level 5")
        assertEquals("nivel skill ? = 5", formatRewardLabel(reward))
    }

    @Test
    fun `quest rewards can be enumerated with formatRewardLabel`() {
        val rewards = listOf(
            QuestEntryDefinition("item", "EMERALD", 5, "5 smaralde"),
            QuestEntryDefinition("experience", null, 50, "50 XP"),
            QuestEntryDefinition("progression:xp", null, 100, "100 XP progresie"),
            QuestEntryDefinition("progression:skill", "combat", 25, "Combat skill"),
            QuestEntryDefinition("progression:skill_level", "mining", 2, "Mining lvl 2"),
            QuestEntryDefinition("reputation:region", "demo_sat", 10, "Region rep"),
        )
        val labels = rewards.map { formatRewardLabel(it) }
        assertEquals(6, labels.size)
        assertEquals("5 smaralde", labels[0])
        assertEquals("50 XP vanilla", labels[1])
        assertEquals("100 XP progresie", labels[2])
        assertEquals("25 XP skill combat", labels[3])
        assertEquals("nivel skill mining = 2", labels[4])
        assertEquals("+10 reputatie region:demo_sat", labels[5])
    }

    @Test
    fun `reputation region reward has positive sign`() {
        val reward = QuestEntryDefinition("reputation:region", "demo_sat", 10, "Region rep")
        assertEquals("+10 reputatie region:demo_sat", formatRewardLabel(reward))
    }

    @Test
    fun `reputation faction reward has positive sign`() {
        val reward = QuestEntryDefinition("reputation:faction", "knights", 5, "Faction rep")
        assertEquals("+5 reputatie faction:knights", formatRewardLabel(reward))
    }

    @Test
    fun `reputation reward with penalty metadata shows negative sign`() {
        val reward = QuestEntryDefinition(
            "reputation:faction", "bandits", 15, "Bandit penalty",
            mapOf("penalty" to "true"), emptyMap(), emptyMap()
        )
        assertEquals("-15 reputatie faction:bandits", formatRewardLabel(reward))
    }

    @Test
    fun `reputation reward with sign negative metadata shows negative sign`() {
        val reward = QuestEntryDefinition(
            "reputation:region", "demo_sat", 10, "Tax",
            mapOf("sign" to "-"), emptyMap(), emptyMap()
        )
        assertEquals("-10 reputatie region:demo_sat", formatRewardLabel(reward))
    }

    @Test
    fun `reputation reward without itemId shows placeholder`() {
        val reward = QuestEntryDefinition("reputation:region", null, 10, "Region rep")
        assertEquals("+10 reputatie region:?", formatRewardLabel(reward))
    }

    @Test
    fun `item reward uses description when present`() {
        val reward = QuestEntryDefinition("item", "EMERALD", 5, "5 smaralde")
        assertEquals("5 smaralde", formatRewardLabel(reward))
    }

    @Test
    fun `item reward without description falls back to humanized type`() {
        val reward = QuestEntryDefinition("item", "EMERALD", 5, "")
        assertEquals("item", formatRewardLabel(reward))
    }

    @Test
    fun `set_story_state reward shows scope`() {
        val reward = QuestEntryDefinition("set_story_state", "demo_sat:plot:open", 1, "Open plot")
        assertEquals("story state demo_sat:plot:open = 1", formatRewardLabel(reward))
    }

    @Test
    fun `record_story_event reward shows event key`() {
        val reward = QuestEntryDefinition("record_story_event", "demo_sat:event:aid", 1, "")
        assertEquals("story event demo_sat:event:aid", formatRewardLabel(reward))
    }

    @Test
    fun `null reward returns fallback`() {
        assertEquals("recompensa", formatRewardLabel(null))
    }

    @Test
    fun `unknown reward type with description uses description`() {
        val reward = QuestEntryDefinition("custom_thing", "my_item", 3, "Custom reward")
        assertEquals("Custom reward", formatRewardLabel(reward))
    }

    @Test
    fun `unknown reward type without description uses humanized type`() {
        val reward = QuestEntryDefinition("custom_thing", "my_item", 3, "")
        assertEquals("custom thing", formatRewardLabel(reward))
    }

    @Test
    fun `progression skill level with level 1 works`() {
        val reward = QuestEntryDefinition("progression:skill_level", "combat", 1, "Combat level 1")
        assertEquals("nivel skill combat = 1", formatRewardLabel(reward))
    }
}
