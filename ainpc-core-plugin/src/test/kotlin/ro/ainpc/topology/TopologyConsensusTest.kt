package ro.ainpc.topology

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TopologyConsensusTest {

    @Test
    fun toPromptBlockContainsCategory() {
        val consensus = TopologyConsensus(
            category = TopologyCategory.INTERIOR,
            descriptions = listOf("Un interior linistit"),
            biomes = listOf("plains"),
            dialogueHints = listOf("salut"),
            suggestedTraits = listOf("calm"),
            sourcePacks = listOf("medieval")
        )
        val block = consensus.toPromptBlock()
        assertTrue(block.contains("Interior"))
    }

    @Test
    fun toPromptBlockHandlesEmptyLists() {
        val consensus = TopologyConsensus(
            category = TopologyCategory.PLAINS,
            descriptions = emptyList(),
            biomes = emptyList(),
            dialogueHints = emptyList(),
            suggestedTraits = emptyList(),
            sourcePacks = emptyList()
        )
        val block = consensus.toPromptBlock()
        assertTrue(block.isNotBlank())
        assertTrue(block.contains("Camp"))
    }

    @Test
    fun toPromptBlockContainsDescriptionWhenPresent() {
        val consensus = TopologyConsensus(
            category = TopologyCategory.FOREST,
            descriptions = listOf("Padurea satului", "Loc de vanatoare"),
            biomes = listOf("forest"),
            dialogueHints = listOf("bun venit in padure"),
            suggestedTraits = listOf("atent"),
            sourcePacks = listOf("medieval")
        )
        val block = consensus.toPromptBlock()
        assertTrue(block.contains("Padure"))
        assertTrue(block.contains("bun venit"))
    }
}
