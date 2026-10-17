package ro.ainpc.managers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MemoryRecallSelectorTest {
    @Test
    fun `recorded date is normalized without changing source or inventing an event date`() {
        val recordedOn = MemoryRecallCandidate.parseRecordedOn("2026-10-15 12:30:00")
        val candidate = MemoryRecallCandidate("Jucatorul a spus ca a pierdut sabia ieri.", "conversation_quote", recordedOn)

        assertEquals(LocalDate.of(2026, 10, 15), recordedOn)
        assertEquals(
            "[source=conversation_quote] [recorded_on=2026-10-15] Jucatorul a spus ca a pierdut sabia ieri.",
            candidate.promptText(),
        )
        assertNull(MemoryRecallCandidate.parseRecordedOn("data invalida"))
        assertEquals("[source=game_event] Eveniment vechi.", MemoryRecallCandidate("Eveniment vechi.", "game_event").promptText())
    }

    @Test
    fun `recalls a topical memory before an unrelated earlier candidate`() {
        val unrelated = MemoryRecallCandidate("Jucătorul mi-a oferit pâine.", "game_event")
        val relevant = MemoryRecallCandidate("Jucătorul a pierdut sabia în atelier.", "game_event")

        val selected = MemoryRecallSelector.select("Ai văzut sabia mea?", listOf(unrelated, relevant), 1)

        assertEquals(listOf(relevant), selected)
    }

    @Test
    fun `preserves database order when the topic is absent or unmatched`() {
        val first = MemoryRecallCandidate("Am lucrat la fierărie.", "game_event")
        val second = MemoryRecallCandidate("Am vizitat piața.", "game_event")
        val candidates = listOf(first, second)

        assertEquals(candidates, MemoryRecallSelector.select("Salut!", candidates, 2))
        assertEquals(listOf(first), MemoryRecallSelector.select("Unde sunt bandiții?", candidates, 1))
    }

    @Test
    fun `keeps database order for equally relevant memories and respects the limit`() {
        val first = MemoryRecallCandidate("Sabia a fost reparată ieri.", "game_event")
        val second = MemoryRecallCandidate("Sabia era veche.", "conversation_quote")

        assertEquals(listOf(first), MemoryRecallSelector.select("Ce știi despre sabie?", listOf(first, second), 1))
        assertEquals(emptyList<MemoryRecallCandidate>(), MemoryRecallSelector.select("sabie", listOf(first), 0))
    }

    @Test
    fun `skips oversized memories without truncation and fills slots from later candidates`() {
        val oversized = MemoryRecallCandidate("Sabia a disparut. " + "Am cercetat atelierul. ".repeat(200) + "Nu stiu cine a luat-o.", "game_event")
        val unrelated = MemoryRecallCandidate("Am oferit paine.", "game_event")
        val relevant = MemoryRecallCandidate("Sabia era veche.", "game_event")
        val remaining = MemoryRecallCandidate("Am mers in sat.", "game_event")

        val selected = MemoryRecallSelector.select("sabie", listOf(oversized, unrelated, relevant, remaining), 2, 78)

        assertEquals(listOf(relevant, unrelated), selected)
        assertEquals(listOf(relevant), MemoryRecallSelector.select("sabie", listOf(oversized, relevant), 1))
        assertEquals(
            "- [source=game_event] Sabia era veche.\n- [source=game_event] Am oferit paine.\n",
            selected.joinToString("") { "- ${it.promptText()}\n" },
        )
    }

    @Test
    fun `budget includes source date and list formatting at the exact boundary`() {
        val candidate = MemoryRecallCandidate("Sabia era veche.", "game_event", LocalDate.of(2026, 10, 15))
        val rendered = "- [source=game_event] [recorded_on=2026-10-15] Sabia era veche.\n"

        assertEquals(listOf(candidate), MemoryRecallSelector.select("sabie", listOf(candidate), 1, rendered.length))
        assertEquals(emptyList<MemoryRecallCandidate>(), MemoryRecallSelector.select("sabie", listOf(candidate), 1, rendered.length - 1))
    }

    @Test
    fun `budget also applies without a useful topic and ignores blank content`() {
        val blank = MemoryRecallCandidate(" \n ", "game_event")
        val first = MemoryRecallCandidate("Am oferit paine.", "game_event")
        val second = MemoryRecallCandidate("Am mers in sat.", "game_event")

        assertEquals(listOf(first), MemoryRecallSelector.select("...", listOf(blank, first, second), 5, 40))
    }

    @Test
    fun `zero and negative budgets omit ordinary memories`() {
        val candidates = listOf(MemoryRecallCandidate("Am oferit paine.", "game_event"))

        for (budget in listOf(0, -1, Int.MIN_VALUE)) {
            assertEquals(emptyList<MemoryRecallCandidate>(), MemoryRecallSelector.select("paine", candidates, 5, budget))
        }
    }

    @Test
    fun `repeated records leave room for other memories and retain their count`() {
        val repeated = MemoryRecallCandidate("Am oferit paine.", "game_event", LocalDate.of(2026, 10, 15))
        val distinct = MemoryRecallCandidate("Am mers in sat.", "game_event", repeated.recordedOn)
        val candidates = listOf(repeated, repeated.copy(), distinct, repeated.copy())

        val selected = MemoryRecallSelector.select("...", candidates, 2)

        assertEquals(listOf(repeated.copy(recordCount = 3), distinct), selected)
        assertEquals(
            "[source=game_event] [recorded_on=2026-10-15] [record_count=3] Am oferit paine.",
            selected.first().promptText(),
        )
        assertEquals(listOf(1, 1, 1, 1), candidates.map { it.recordCount })
    }

    @Test
    fun `repetition does not outrank a more relevant memory`() {
        val repeated = MemoryRecallCandidate("Am oferit paine.", "game_event")
        val relevant = MemoryRecallCandidate("Sabia era veche.", "game_event")

        assertEquals(listOf(relevant), MemoryRecallSelector.select("sabie", List(10) { repeated } + relevant, 1))
    }

    @Test
    fun `different sources dates and exact wording remain separate`() {
        val quote = MemoryRecallCandidate("Sabia era veche.", "conversation_quote", LocalDate.of(2026, 10, 15))
        val candidates = listOf(
            quote,
            quote.copy(source = "game_event"),
            quote.copy(source = "unclassified"),
            quote.copy(recordedOn = LocalDate.of(2026, 10, 14)),
            quote.copy(recordedOn = null),
            quote.copy(content = "Sabia nu era veche."),
            quote.copy(content = "sabia era veche."),
        )

        assertEquals(candidates, MemoryRecallSelector.select("sabie", candidates, 10))
    }

    @Test
    fun `record count metadata consumes budget before selecting a grouped memory`() {
        val repeated = MemoryRecallCandidate("Sabia era veche.", "conversation_quote", LocalDate.of(2026, 10, 15))
        val fallback = MemoryRecallCandidate("Am mers in sat.", "game_event")
        val rendered = "- [source=conversation_quote] [recorded_on=2026-10-15] [record_count=2] Sabia era veche.\n"
        val candidates = listOf(repeated, repeated.copy(), fallback)

        assertEquals(
            rendered,
            MemoryRecallSelector.select("sabie", candidates, 1, rendered.length).joinToString("") { "- ${it.promptText()}\n" },
        )
        assertEquals(listOf(fallback), MemoryRecallSelector.select("sabie", candidates, 1, rendered.length - 1))
    }
}
