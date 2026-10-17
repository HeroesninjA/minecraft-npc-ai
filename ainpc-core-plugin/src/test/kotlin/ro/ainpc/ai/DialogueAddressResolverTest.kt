package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class DialogueAddressResolverTest {
    @Test
    fun `farewells resolve the named recipient without accepting reported speech`() {
        for (message in listOf("Pa, Maria!", "La revedere, Maria!", "Multumesc, pe curand, Maria!", "Maria, pa!")) {
            assertEquals(maria.npcId, DialogueAddressResolver.resolve(message, candidates), message)
        }
        assertNull(DialogueAddressResolver.resolve("El a spus pa, Maria!", candidates))
        assertNull(DialogueAddressResolver.resolve("\"Pa, Maria!\"", candidates))
        assertEquals(
            DialogueAddressStatus.AMBIGUOUS,
            DialogueAddressResolver.resolveDetailed("Pa, Maria!", candidates + DialogueAddressCandidate(UUID(0L, 99L), "Maria")).status,
        )
    }

    private val bob = DialogueAddressCandidate(UUID(0L, 1L), "Bob")
    private val maria = DialogueAddressCandidate(UUID(0L, 2L), "Maria", "Măria Fierarul")
    private val candidates = listOf(bob, maria)

    @Test
    fun `leading named address selects the intended npc`() {
        assertEquals(maria.npcId, DialogueAddressResolver.resolve("Maria, ce faci?", candidates))
        assertEquals(bob.npcId, DialogueAddressResolver.resolve("Bob, ești idiot!", candidates))
    }

    @Test
    fun `trailing named address follows direct greetings thanks insults and questions`() {
        for (message in listOf("Salut, Maria!", "Mulțumesc, Maria!", "Ești prost, Maria!", "Ce vinzi, Maria?")) {
            assertEquals(maria.npcId, DialogueAddressResolver.resolve(message, candidates), message)
        }
    }

    @Test
    fun `display names support multiple words case and diacritics`() {
        assertEquals(maria.npcId, DialogueAddressResolver.resolve("MĂRIA FIERARUL, ce vinzi?", candidates))
        assertEquals(maria.npcId, DialogueAddressResolver.resolve("Salut, maria fierarul!", candidates))
    }

    @Test
    fun `ordinary mentions and reported speech do not redirect dialogue`() {
        for (message in listOf(
            "Maria are o sabie.",
            "Îl caut pe Bob.",
            "Am vorbit cu Maria, ce părere ai?",
            "Am auzit că Maria, ce vinzi, este o întrebare frecventă.",
            "Maria a zis: Salut, Bob!",
            "Am spus: Mulțumesc, Maria!",
            "Tu ai spus: salut, Maria!",
            "Ai zis mulțumesc, Maria!",
        )) {
            assertNull(DialogueAddressResolver.resolve(message, candidates), message)
        }
    }

    @Test
    fun `quoted addresses do not redirect dialogue`() {
        for (message in listOf("\"Maria, ce faci?\"", "„Salut, Maria!”", "«Bob, ești prost!»")) {
            assertNull(DialogueAddressResolver.resolve(message, candidates), message)
        }
    }

    @Test
    fun `conflicting leading and trailing addresses remain ambiguous`() {
        assertNull(DialogueAddressResolver.resolve("Bob, ce vinzi, Maria?", candidates))
        assertNull(DialogueAddressResolver.resolve("Maria, Bob, ce faci?", candidates))
        assertEquals(
            DialogueAddressStatus.AMBIGUOUS,
            DialogueAddressResolver.resolveDetailed("Maria, Bob, ce faci?", candidates).status,
        )
        assertEquals(bob.npcId, DialogueAddressResolver.resolve("Bob, ce vinzi, Bob?", candidates))
    }

    @Test
    fun `explicit leading address remains valid when discussing reported speech`() {
        assertEquals(maria.npcId, DialogueAddressResolver.resolve("Maria, Bob a spus ceva despre tine.", candidates))
    }

    @Test
    fun `shared aliases across different npcs remain ambiguous`() {
        val duplicateAlias = DialogueAddressCandidate(UUID(0L, 3L), "Maria")
        val duplicateDisplayName = DialogueAddressCandidate(UUID(0L, 4L), "Other", "Măria Fierarul")

        assertNull(DialogueAddressResolver.resolve("Maria, ce faci?", candidates + duplicateAlias))
        assertNull(DialogueAddressResolver.resolve("Salut, Maria Fierarul!", candidates + duplicateDisplayName))
        assertEquals(
            DialogueAddressStatus.AMBIGUOUS,
            DialogueAddressResolver.resolveDetailed("Maria, ce faci?", candidates + duplicateAlias).status,
        )
    }

    @Test
    fun `duplicate candidates for the same npc are not ambiguous`() {
        val repeatedCandidate = DialogueAddressCandidate(maria.npcId, "Măria", "Maria Fierarul")

        assertEquals(maria.npcId, DialogueAddressResolver.resolve("Maria, ce faci?", candidates + repeatedCandidate))
    }

    @Test
    fun `names match exactly and regex characters remain literal`() {
        val ana = DialogueAddressCandidate(UUID(0L, 5L), "Ana")
        val literalName = DialogueAddressCandidate(UUID(0L, 6L), "A.b [smith]")

        assertNull(DialogueAddressResolver.resolve("Mariana, ce faci?", listOf(ana)))
        assertEquals(literalName.npcId, DialogueAddressResolver.resolve("A.b [smith], ce faci?", listOf(literalName)))
        assertNull(DialogueAddressResolver.resolve("Axb smith, ce faci?", listOf(literalName)))
    }

    @Test
    fun `unknown names missing candidates and blank aliases produce no target`() {
        val blankCandidate = DialogueAddressCandidate(UUID(0L, 7L), " ", "")

        assertNull(DialogueAddressResolver.resolve("Andrei, ce faci?", candidates))
        assertNull(DialogueAddressResolver.resolve("Salut, Andrei!", candidates))
        assertNull(DialogueAddressResolver.resolve("Maria, ce faci?", listOf(bob)))
        assertNull(DialogueAddressResolver.resolve("Maria, ce faci?", emptyList()))
        assertNull(DialogueAddressResolver.resolve(", ce faci?", listOf(blankCandidate)))
        assertNull(DialogueAddressResolver.resolve("", candidates))
        assertEquals(DialogueAddressStatus.NONE, DialogueAddressResolver.resolveDetailed("Andrei, ce faci?", candidates).status)
    }

    @Test
    fun `detailed resolution distinguishes unique target from missing address`() {
        val unique = DialogueAddressResolver.resolveDetailed("Maria, ce faci?", candidates)

        assertEquals(DialogueAddressStatus.UNIQUE, unique.status)
        assertEquals(maria.npcId, unique.npcId)
        assertEquals(setOf(maria.npcId), unique.matchedNpcIds)
        assertEquals(emptySet<UUID>(), DialogueAddressResolver.resolveDetailed("Ce faci?", candidates).matchedNpcIds)
    }

    @Test
    fun `ambiguous resolution only offers matched identities`() {
        val duplicateMaria = DialogueAddressCandidate(UUID(0L, 8L), "Maria")
        val nearbyCandidates = candidates + duplicateMaria

        val sharedName = DialogueAddressResolver.resolveDetailed("Maria, ce faci?", nearbyCandidates)
        assertEquals(DialogueAddressStatus.AMBIGUOUS, sharedName.status)
        assertEquals(setOf(maria.npcId, duplicateMaria.npcId), sharedName.matchedNpcIds)

        val multipleNames = DialogueAddressResolver.resolveDetailed("Bob, ce vinzi, Maria?", nearbyCandidates)
        assertEquals(DialogueAddressStatus.AMBIGUOUS, multipleNames.status)
        assertEquals(setOf(bob.npcId, maria.npcId, duplicateMaria.npcId), multipleNames.matchedNpcIds)
    }
}
