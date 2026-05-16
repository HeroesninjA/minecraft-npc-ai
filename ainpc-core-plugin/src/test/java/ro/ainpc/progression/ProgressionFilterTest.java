package ro.ainpc.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionFilterTest {

    @Test
    void matchesDefinitionByStructuredMetadataFields() {
        ProgressionDefinition definition = definition();

        assertTrue(ProgressionFilter.matchesDefinition(definition, "scenario:investigation"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "base:trade-deal"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "mechanic:village_contracts"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "category:side"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "code:C02"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "medieval:village_contracts:C02"));
        assertFalse(ProgressionFilter.matchesDefinition(definition, "scenario:delivery"));
        assertFalse(ProgressionFilter.matchesDefinition(definition, "kind:quest"));
    }

    @Test
    void matchesStoredProgressionByStatusAndStructuredMetadataFields() {
        StoredProgression progression = storedProgression();

        assertTrue(ProgressionFilter.matchesStored(progression, "active"));
        assertTrue(ProgressionFilter.matchesStored(progression, "current"));
        assertTrue(ProgressionFilter.matchesStored(progression, "tracked:true"));
        assertTrue(ProgressionFilter.matchesStored(progression, "resolved:true"));
        assertTrue(ProgressionFilter.matchesStored(progression, "status:active"));
        assertTrue(ProgressionFilter.matchesStored(progression, "player:player-1"));
        assertTrue(ProgressionFilter.matchesStored(progression, "scenario:investigation"));
        assertTrue(ProgressionFilter.matchesStored(progression, "base:TRADE_DEAL"));
        assertTrue(ProgressionFilter.matchesStored(progression, "phase:MARKET_CHECK"));
        assertFalse(ProgressionFilter.matchesStored(progression, "archived"));
        assertFalse(ProgressionFilter.matchesStored(progression, "resolved:false"));
        assertFalse(ProgressionFilter.matchesStored(progression, "scenario:hunt"));
    }

    @Test
    void matchesDutyProgressionsByGenericMetadata() {
        ProgressionDefinition definition = new ProgressionDefinition(
            "medieval:npc_duties:D01",
            "medieval",
            "npc_duties",
            "duty",
            "D01",
            "medieval:D01",
            "D01",
            "Rondul Strajerului",
            "Sarcina NPC de patrula.",
            "repeatable",
            "duty",
            "DUTY",
            "Sarcini NPC",
            "sarcina",
            "sarcini",
            2,
            3,
            2,
            2,
            true,
            true
        );

        assertTrue(ProgressionFilter.matchesDefinition(definition, "duty"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "kind:duty"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "scenario:duty"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "base:DUTY"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "mechanic:npc_duties"));
        assertFalse(ProgressionFilter.matchesDefinition(definition, "kind:contract"));
    }

    @Test
    void matchesBountyProgressionsByGenericMetadata() {
        ProgressionDefinition definition = new ProgressionDefinition(
            "medieval:local_bounties:B01",
            "medieval",
            "local_bounties",
            "bounty",
            "B01",
            "medieval:B01",
            "B01",
            "Recompensa Drumului Vechi",
            "Bounty local de vanatoare.",
            "repeatable",
            "hunt",
            "BOUNTY",
            "Bounty locale",
            "bounty",
            "bounty-uri",
            1,
            3,
            2,
            2,
            true,
            true
        );

        assertTrue(ProgressionFilter.matchesDefinition(definition, "bounty"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "kind:bounty"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "scenario:hunt"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "base:BOUNTY"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "mechanic:local_bounties"));
        assertFalse(ProgressionFilter.matchesDefinition(definition, "kind:duty"));
    }

    @Test
    void matchesEventProgressionsByGenericMetadata() {
        ProgressionDefinition definition = new ProgressionDefinition(
            "medieval:village_events:E01",
            "medieval",
            "village_events",
            "event",
            "E01",
            "medieval:E01",
            "E01",
            "Alarma Fantanii din Piata",
            "Eveniment local din piata.",
            "repeatable",
            "event",
            "WORLD_EVENT",
            "Evenimente locale",
            "eveniment",
            "evenimente",
            2,
            3,
            2,
            2,
            true,
            true
        );

        assertTrue(ProgressionFilter.matchesDefinition(definition, "event"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "kind:event"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "scenario:event"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "base:WORLD_EVENT"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "mechanic:village_events"));
        assertFalse(ProgressionFilter.matchesDefinition(definition, "kind:bounty"));
    }

    @Test
    void matchesTutorialProgressionsByGenericMetadata() {
        ProgressionDefinition definition = new ProgressionDefinition(
            "medieval:onboarding:T01",
            "medieval",
            "onboarding",
            "tutorial",
            "T01",
            "medieval:T01",
            "T01",
            "Indrumarea Avizierului",
            "Tutorial de onboarding.",
            "side",
            "tutorial",
            "TUTORIAL",
            "Tutoriale",
            "tutorial",
            "tutoriale",
            1,
            3,
            2,
            2,
            false,
            true
        );

        assertTrue(ProgressionFilter.matchesDefinition(definition, "tutorial"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "kind:tutorial"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "scenario:tutorial"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "base:TUTORIAL"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "mechanic:onboarding"));
        assertFalse(ProgressionFilter.matchesDefinition(definition, "kind:event"));
    }

    @Test
    void matchesRitualProgressionsByGenericMetadata() {
        ProgressionDefinition definition = new ProgressionDefinition(
            "medieval:village_rituals:R01",
            "medieval",
            "village_rituals",
            "ritual",
            "R01",
            "medieval:R01",
            "R01",
            "Luminile Vechiului Altar",
            "Ritual local.",
            "repeatable",
            "ritual",
            "RITUAL",
            "Ritualuri locale",
            "ritual",
            "ritualuri",
            1,
            4,
            3,
            2,
            true,
            true
        );

        assertTrue(ProgressionFilter.matchesDefinition(definition, "ritual"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "kind:ritual"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "scenario:ritual"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "base:RITUAL"));
        assertTrue(ProgressionFilter.matchesDefinition(definition, "mechanic:village_rituals"));
        assertFalse(ProgressionFilter.matchesDefinition(definition, "kind:tutorial"));
    }

    private ProgressionDefinition definition() {
        return new ProgressionDefinition(
            "medieval:village_contracts:C02",
            "medieval",
            "village_contracts",
            "contract",
            "C02",
            "medieval:C02",
            "C02",
            "Avizierul Pietei",
            "Contract de verificare a avizierului pietei.",
            "side",
            "investigation",
            "TRADE_DEAL",
            "Contracte de sat",
            "contract",
            "contracte",
            3,
            3,
            2,
            2,
            true,
            true
        );
    }

    private StoredProgression storedProgression() {
        return new StoredProgression(
            "player-1",
            "medieval:village_contracts:C02",
            "medieval",
            "village_contracts",
            "contract",
            "side",
            "investigation",
            "TRADE_DEAL",
            "C02",
            "medieval:C02",
            "C02",
            "active",
            100L,
            0L,
            "MARKET_CHECK",
            "MARKET_CHECK",
            "{}",
            "{}",
            150L,
            true,
            true,
            "Contracte de sat",
            "contract",
            "contracte",
            "player_quests"
        );
    }
}
