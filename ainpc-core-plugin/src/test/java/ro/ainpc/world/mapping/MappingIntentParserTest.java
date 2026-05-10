package ro.ainpc.world.mapping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingIntentParserTest {

    @Test
    void suggestsBlacksmithHouseFromRomanianPrompt() {
        MappingDraftSuggestion suggestion = MappingIntentParser.suggest(
            MappingDraftKind.PLACE,
            "aici este casa fierarului"
        );

        assertEquals("house", suggestion.typeId());
        assertEquals("casa_fierarului", suggestion.localId());
        assertTrue(suggestion.tags().contains("home"));
        assertTrue(suggestion.tags().contains("blacksmith"));
        assertEquals("blacksmith", suggestion.metadata().get("profession"));
        assertEquals("false", suggestion.metadata().get("public_access_hint"));
    }

    @Test
    void suggestsQuestBoardNodeFromPrompt() {
        MappingDraftSuggestion suggestion = MappingIntentParser.suggest(
            MappingDraftKind.NODE,
            "acesta este avizierul"
        );

        assertEquals("quest_trigger", suggestion.typeId());
        assertEquals("quest_board", suggestion.localId());
        assertEquals("quest_board", suggestion.metadata().get("semantic"));
        assertEquals("quest_anchor", suggestion.metadata().get("role"));
    }

    @Test
    void suggestsMarketPlaceFromPrompt() {
        MappingDraftSuggestion suggestion = MappingIntentParser.suggest(
            MappingDraftKind.PLACE,
            "aici este piata satului"
        );

        assertEquals("market", suggestion.typeId());
        assertTrue(suggestion.tags().contains("trade"));
        assertTrue(suggestion.tags().contains("social"));
    }
}
