package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NpcProfessionFactResolverTest {
    private val context = """
        NPC_LORE:
        - Radu, occupation=Fierar, work=sat:atelier, lore=Repara unelte.
        - Ana, occupation=Brutar, work=sat:brutarie, lore=Pregateste paine.
        Raspuns factual pentru intrebari despre meserii: Radu are meseria Fierar si lucreaza la Atelier.; Ana are meseria Brutar si lucreaza la Brutarie.
    """.trimIndent()

    @Test
    fun `profession lookup returns only the requested occupation`() {
        assertEquals(
            "Ana are meseria Brutar si lucreaza la Brutarie.",
            NpcProfessionFactResolver.resolve("Salut, cine este brutarul din sat?", context)
        )
    }

    @Test
    fun `unrelated questions and unknown occupations do not pick the first npc`() {
        for (message in listOf(
            "Care este pretul fierarului?",
            "Cine a furat uneltele fierarului?",
            "Cine este invatatorul?",
            "Fiecare este fierar.",
        )) {
            assertNull(NpcProfessionFactResolver.resolve(message, context), message)
        }
    }

    @Test
    fun `diacritics capitalization and repeated spaces preserve occupation lookup`() {
        assertEquals(
            "Ana are meseria Meșteșugar si lucreaza la Brutarie.",
            NpcProfessionFactResolver.resolve("CINE  ESTE  MEȘTEȘUGARUL?", context
                .replace("occupation=Brutar", "occupation=Meșteșugar")
                .replace("meseria Brutar ", "meseria Meșteșugar "))
        )
    }

    @Test
    fun `multiple workers with the requested occupation are retained`() {
        assertEquals(
            "Radu are meseria Fierar si lucreaza la Atelier.; Ana are meseria Fierar si lucreaza la Brutarie.",
            NpcProfessionFactResolver.resolve("Care sunt fierarii?", context
                .replace("occupation=Brutar", "occupation=Fierar")
                .replace("meseria Brutar ", "meseria Fierar "))
        )
    }
}
