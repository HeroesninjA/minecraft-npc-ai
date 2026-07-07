package ro.ainpc.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Random

class NPCNameGeneratorTest {
    @Test
    fun predefinedNamePoolContainsManyUniqueNames() {
        val maleNames = NPCNameGenerator.predefinedNames("male")
        val femaleNames = NPCNameGenerator.predefinedNames("female")
        val allNames = HashSet<String>()
        allNames.addAll(maleNames)
        allNames.addAll(femaleNames)

        assertTrue(NPCNameGenerator.predefinedNameCount() >= 200)
        assertEquals(NPCNameGenerator.predefinedNameCount(), allNames.size)
    }

    @Test
    fun randomNameReturnsValidName() {
        val rand = Random(42)
        val male = NPCNameGenerator.randomName("male", rand)
        val female = NPCNameGenerator.randomName("female", rand)
        assertTrue(male.isNotBlank())
        assertTrue(female.isNotBlank())
        assertFalse(male.length < 2)
        assertFalse(female.length < 2)
    }

    @Test
    fun randomFullNameContainsTwoParts() {
        val rand = Random(42)
        val name = NPCNameGenerator.randomFullName("male", rand)
        val parts = name.split(" ")
        assertEquals(2, parts.size, "Numele complet ar trebui sa aiba 2 parti: $name")
        assertTrue(parts[0].isNotBlank())
        assertTrue(parts[1].isNotBlank())
    }

    @Test
    fun randomFullNameIsDifferentEachTime() {
        val rand = Random(42)
        val names = (1..10).map { NPCNameGenerator.randomFullName("male", Random(it.toLong())) }
        val unique = names.toSet()
        assertTrue(unique.size > 5, "Ar trebui sa avem nume diverse, avem doar ${unique.size}")
    }

    @Test
    fun randomNicknameHasPrefix() {
        val rand = Random(42)
        val nickname = NPCNameGenerator.randomNickname("male", rand)
        assertTrue(nickname.contains(" "), "Porecla ar trebui sa contina un spatiu: $nickname")
    }

    @Test
    fun randomNameWithTitleIncludesOccupation() {
        val rand = Random(42)
        val name = NPCNameGenerator.randomNameWithTitle("mayor", "male", rand)
        assertTrue(name.contains("Primarul"), "Numele cu titlu ar trebui sa includa 'Primarul': $name")
    }

    @Test
    fun predefinedSurnamesListIsNotEmpty() {
        val surnames = NPCNameGenerator.predefinedSurnames()
        assertTrue(surnames.isNotEmpty())
        assertTrue(surnames.size >= 100)
    }

    @Test
    fun totalNameCombinationsIsLarge() {
        val count = NPCNameGenerator.totalNameCombinations()
        assertTrue(count > 10000, "Ar trebui sa avem peste 10k combinatii, avem $count")
    }

    @Test
    fun randomNameWithoutGenderDefaultsToMale() {
        val rand = Random(42)
        val maleNames = NPCNameGenerator.predefinedNames("male")
        val name = NPCNameGenerator.randomName(null, rand)
        assertTrue(maleNames.contains(name), "Numele default ar trebui sa fie masculin: $name")
    }
}
