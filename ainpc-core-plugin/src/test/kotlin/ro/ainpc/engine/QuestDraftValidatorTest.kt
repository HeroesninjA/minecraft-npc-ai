package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestDraftValidatorTest {
    private val seed = QuestSeed(
        "demo_sat",
        "market",
        "village_contracts",
        "contract",
        "avizier piata",
        listOf("visit_place", "inspect_node"),
        listOf("item", "story"),
        "writes_story",
        listOf("market_unrest"),
        listOf("no_live_export")
    )

    @Test
    fun validatesReadOnlySemanticDraft() {
        val draft = QuestDraft(
            "draft_c03",
            "Umbra de pe avizier",
            "Investigatie scurta in piata.",
            "village_contracts",
            "contract",
            listOf(
                QuestDraftObjective("visit_market", "visit_place", "tag:market", "Mergi in piata.", "place:market"),
                QuestDraftObjective("inspect_board", "inspect_node", "node:quest_board", "Inspecteaza avizierul.", "node:quest_board")
            ),
            listOf(QuestDraftReward("item", "EMERALD", 2, "Plata mica.")),
            listOf(QuestDraftStoryAction("record_story_event", "place", "place:market", "notice_board_checked", "true")),
            listOf("tag:market", "node:quest_board"),
            false
        )

        val report = QuestDraftValidator().validate(seed, draft)

        assertTrue(report.valid())
        assertFalse(report.executable())
    }

    @Test
    fun rejectsRawCoordinateAnchors() {
        val draft = QuestDraft(
            "draft_bad_anchor",
            "Coordonate brute",
            "Draft invalid.",
            "village_contracts",
            "contract",
            listOf(QuestDraftObjective("inspect", "inspect_node", "10, 64, -20", "Inspecteaza.", "10,64,-20")),
            emptyList(),
            emptyList(),
            listOf("10,64,-20"),
            false
        )

        val report = QuestDraftValidator().validate(seed, draft)

        assertFalse(report.valid())
        assertTrue(report.errors().any { it.contains("coordonate brute") })
    }

    @Test
    fun rejectsStoryActionsWithoutScope() {
        val draft = QuestDraft(
            "draft_bad_story",
            "Story invalid",
            "Draft invalid.",
            "village_contracts",
            "contract",
            listOf(QuestDraftObjective("visit_market", "visit_place", "tag:market", "Mergi in piata.", "tag:market")),
            emptyList(),
            listOf(QuestDraftStoryAction("set_story_state", "", "place:market", "", "true")),
            listOf("tag:market"),
            false
        )

        val report = QuestDraftValidator().validate(seed, draft)

        assertFalse(report.valid())
        assertTrue(report.errors().any { it.contains("scope region sau place") })
        assertTrue(report.errors().any { it.contains("key") })
    }
}
