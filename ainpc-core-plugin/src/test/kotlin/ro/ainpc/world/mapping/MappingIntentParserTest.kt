package ro.ainpc.world.mapping

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MappingIntentParserTest {
    @Test
    fun suggestsBlacksmithHouseFromRomanianPrompt() {
        val suggestion = MappingIntentParser.suggest(
            MappingDraftKind.PLACE,
            "aici este casa fierarului"
        )

        assertEquals("house", suggestion.typeId())
        assertEquals("casa_fierarului", suggestion.localId())
        assertTrue(suggestion.tags().contains("home"))
        assertTrue(suggestion.tags().contains("blacksmith"))
        assertEquals("blacksmith", suggestion.metadata()["profession"])
        assertEquals("false", suggestion.metadata()["public_access_hint"])
    }

    @Test
    fun suggestsQuestBoardNodeFromPrompt() {
        val suggestion = MappingIntentParser.suggest(
            MappingDraftKind.NODE,
            "acesta este avizierul"
        )

        assertEquals("quest_trigger", suggestion.typeId())
        assertEquals("quest_board", suggestion.localId())
        assertEquals("quest_board", suggestion.metadata()["semantic"])
        assertEquals("quest_anchor", suggestion.metadata()["role"])
    }

    @Test
    fun suggestsMarketPlaceFromPrompt() {
        val suggestion = MappingIntentParser.suggest(
            MappingDraftKind.PLACE,
            "aici este piata satului"
        )

        assertEquals("market", suggestion.typeId())
        assertTrue(suggestion.tags().contains("trade"))
        assertTrue(suggestion.tags().contains("social"))
    }
}
