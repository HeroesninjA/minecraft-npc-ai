package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DialogueAiIntentClassificationTest {
    @Test
    fun optInContextPrefetchCoversAmbiguousMessagesButNotKnownIntents() {
        val classifier = DialogueAiIntentClassification

        assertTrue(classifier.shouldPrefetchDomainContext("Taraba?", "STRANGER", true, true))
        assertFalse(classifier.shouldPrefetchDomainContext("Taraba?", "STRANGER", true, false))
        assertFalse(classifier.shouldPrefetchDomainContext("Preturi?", "STRANGER", true, true))
        assertFalse(classifier.shouldPrefetchDomainContext("Buna!", "STRANGER", true, true))
    }

    @Test
    fun workFactsAreCapturedForKnownWorkIntentWithoutAiAndAmbiguousIntentWithAi() {
        val classifier = DialogueAiIntentClassification

        assertTrue(classifier.shouldSnapshotWorkFacts("Munca?", "STRANGER", true, false))
        assertTrue(classifier.shouldSnapshotWorkFacts("Munca?", "STRANGER", false, false))
        assertTrue(classifier.shouldSnapshotWorkFacts("Atelier?", "STRANGER", true, true))
        assertFalse(classifier.shouldSnapshotWorkFacts("Atelier?", "STRANGER", true, false))
        assertFalse(classifier.shouldSnapshotWorkFacts("Atelier?", "STRANGER", false, true))
        assertFalse(classifier.shouldSnapshotWorkFacts("Buna!", "STRANGER", true, true))
    }

    @Test
    fun shopsAreInspectedForKnownTradeAndLookupWithoutAi() {
        val classifier = DialogueAiIntentClassification

        assertTrue(classifier.shouldInspectShops("Comert?", "STRANGER", false, false))
        assertTrue(classifier.shouldInspectShops("Preturi?", "STRANGER", false, false))
        assertTrue(classifier.shouldInspectShops("Ce vinzi?", "STRANGER", false, false))
        assertTrue(classifier.shouldInspectShops("Taraba?", "STRANGER", false, true))
        assertFalse(classifier.shouldInspectShops("Taraba?", "STRANGER", false, false))
        assertFalse(classifier.shouldInspectShops("Buna!", "STRANGER", false, true))
    }

    @Test
    fun onlyAmbiguousLocalMessagesAreEligible() {
        val classifier = DialogueAiIntentClassification

        assertTrue(classifier.shouldClassify(DialogueEngine.DialogueIntent.QUESTION, "Taraba?", false))
        assertFalse(classifier.shouldClassify(DialogueEngine.DialogueIntent.TRADE, "Preturi?", false))
        assertFalse(classifier.shouldClassify(DialogueEngine.DialogueIntent.CONVERSATION, "Atelier", true))
        assertFalse(classifier.shouldClassify(DialogueEngine.DialogueIntent.QUESTION, "da", false))
        assertFalse(classifier.shouldClassify(DialogueEngine.DialogueIntent.QUESTION, "x".repeat(81), false))
    }

    @Test
    fun tradeRequiresAnAccessibleConfiguredShop() {
        val classifier = DialogueAiIntentClassification

        assertEquals(DialogueEngine.DialogueIntent.TRADE, classifier.parse("TRADE", true, false))
        assertNull(classifier.parse("TRADE", false, true))
    }

    @Test
    fun workIntentRequiresAnOccupationAndRejectsUnapprovedActions() {
        val classifier = DialogueAiIntentClassification

        assertEquals(DialogueEngine.DialogueIntent.WORK_TALK, classifier.parse(" work_talk ", false, true))
        assertNull(classifier.parse("WORK_TALK", true, false))
        assertNull(classifier.parse("QUEST_OFFER", true, true))
        assertNull(classifier.parse("TRADE\nBUY", true, true))
        assertNull(classifier.parse("[EXECUTED] TRADE", true, true))
    }
}
