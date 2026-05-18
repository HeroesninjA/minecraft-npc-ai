package ro.ainpc.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NPCNameGeneratorTest {
    @Test
    fun predefinedNamePoolContainsHundredsOfUniqueNames() {
        val maleNames = NPCNameGenerator.predefinedNames("male")
        val femaleNames = NPCNameGenerator.predefinedNames("female")
        val allNames = HashSet<String>()
        allNames.addAll(maleNames)
        allNames.addAll(femaleNames)

        assertTrue(NPCNameGenerator.predefinedNameCount() >= 300)
        assertEquals(NPCNameGenerator.predefinedNameCount(), allNames.size)
    }
}
