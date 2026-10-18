package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DialogueAiIntentClassificationTest {
    @Test
    fun quotedDomainPhrasesDoNotTriggerCommercialClassificationOrPrefetch() {
        val message = "Maria a zis \"ce vinzi?\""
        val localIntent = DialogueIntentSelector.select(message, "STRANGER", true, null)

        assertFalse(DialogueAiIntentClassification.shouldClassify(localIntent, message, false))
        assertFalse(DialogueAiIntentClassification.shouldInspectShops(message, "STRANGER", true, true))
        assertFalse(DialogueAiIntentClassification.shouldPrefetchDomainContext(message, "STRANGER", true, true))
        assertTrue(DialogueAiIntentClassification.shouldInspectShops("A zis \"ce vinzi?\", dar ce vinzi tu?", "STRANGER", true, true))
    }

    @Test
    fun reportedDomainPhrasesDoNotTriggerCommercialClassificationOrPrefetch() {
        val message = "Maria a intrebat ce vinzi?"
        val localIntent = DialogueIntentSelector.select(message, "STRANGER", true, null)

        assertFalse(DialogueAiIntentClassification.shouldClassify(localIntent, message, false))
        assertFalse(DialogueAiIntentClassification.shouldInspectShops(message, "STRANGER", true, true))
        assertFalse(DialogueAiIntentClassification.shouldPrefetchDomainContext(message, "STRANGER", true, true))
        assertTrue(DialogueAiIntentClassification.shouldInspectShops("Maria a intrebat ce vinzi; dar ce vinzi tu?", "STRANGER", true, true))
    }

    @Test
    fun rejectedRequestsAndMeaningQuestionsDoNotBecomeCommercialAiIntents() {
        val classifier = DialogueAiIntentClassification

        for (message in listOf("Nu vreau să cumpăr.", "Nu am nevoie de ajutor.", "Ce inseamna sa cumpar?")) {
            val localIntent = DialogueIntentSelector.select(message, "STRANGER", true, null)
            assertFalse(classifier.shouldClassify(localIntent, message, false), message)
            assertFalse(classifier.shouldPrefetchDomainContext(message, "STRANGER", true, true), message)
            assertFalse(classifier.shouldInspectShops(message, "STRANGER", true, true), message)
        }
        assertTrue(classifier.shouldInspectShops("Nu vreau sa cumpar, dar ce vinzi?", "STRANGER", true, true))
    }

    @Test
    fun relativeChoicesDoNotEnterAClassifierWithoutTheOptionsHistory() {
        val classifier = DialogueAiIntentClassification
        for (message in listOf("Prima opțiune", "A doua", "Ultima", "Ambele", "Niciuna")) {
            assertFalse(classifier.shouldClassify(DialogueEngine.DialogueIntent.CONVERSATION, message, false), message)
            assertFalse(classifier.shouldPrefetchDomainContext(message, "STRANGER", true, true), message)
            assertFalse(classifier.shouldInspectShops(message, "STRANGER", true, true), message)
            assertFalse(classifier.shouldSnapshotWorkFacts(message, "STRANGER", true, true), message)
        }
        assertTrue(classifier.shouldInspectShops("Prima opțiune, ce vinzi?", "STRANGER", true, false))
    }

    @Test
    fun shortAnswersAreNotClassifiedAsTradeOrWorkWithoutConversationHistory() {
        val classifier = DialogueAiIntentClassification
        for (message in listOf("Desigur", "De acord", "Da, te rog", "Nu, mulțumesc", "Mai bine nu")) {
            assertFalse(classifier.shouldClassify(DialogueEngine.DialogueIntent.CONVERSATION, message, false), message)
            assertFalse(classifier.shouldSnapshotWorkFacts(message, "STRANGER", true, true), message)
            assertFalse(classifier.shouldInspectShops(message, "STRANGER", true, true), message)
        }
        assertTrue(classifier.shouldInspectShops("Da, ce vinzi?", "STRANGER", true, false))
        assertTrue(classifier.shouldSnapshotWorkFacts("Nu, ce meserie ai?", "STRANGER", true, false))
    }

    @Test
    fun replayRequestsDoNotTriggerClassificationOrDomainPrefetch() {
        val classifier = DialogueAiIntentClassification

        for (message in listOf("Ce ai spus?", "Te rog, repetă exact ultimul răspuns.")) {
            for (enabled in listOf(false, true)) {
                assertFalse(classifier.shouldClassify(DialogueEngine.DialogueIntent.QUESTION, message, false))
                assertFalse(classifier.shouldPrefetchDomainContext(message, "STRANGER", true, enabled))
                assertFalse(classifier.shouldSnapshotWorkFacts(message, "STRANGER", true, enabled))
                assertFalse(classifier.shouldInspectShops(message, "STRANGER", true, enabled))
            }
        }
    }

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
    fun conversationReferencesDoNotBecomeCommercialOrWorkRequestsWhenContextualAiIsUnavailable() {
        val classifier = DialogueAiIntentClassification
        for (message in listOf(
            "Continuă!", "De ce?", "Nu mi-ai răspuns", "Te rog, ai repetat răspunsul.",
            "Ce am discutat?", "Ce am discutat despre comerț?", "Ce am discutat despre meserie?",
            "Nu mi-ai răspuns, de ce?", "Ai repetat răspunsul, explică mai clar.",
        )) {
            val localIntent = DialogueIntentSelector.select(message, "STRANGER", true, null)
            assertFalse(classifier.shouldClassify(localIntent, message, false), message)
            for (classificationEnabled in listOf(false, true)) {
                assertFalse(classifier.shouldPrefetchDomainContext(message, "STRANGER", true, classificationEnabled), message)
                assertFalse(classifier.shouldSnapshotWorkFacts(message, "STRANGER", true, classificationEnabled), message)
                assertFalse(classifier.shouldInspectShops(message, "STRANGER", true, classificationEnabled), message)
            }
        }
    }

    @Test
    fun complaintWithANewDomainQuestionStillPreparesTheRequestedFacts() {
        val classifier = DialogueAiIntentClassification

        assertTrue(classifier.shouldInspectShops("Nu mi-ai raspuns, ce vinzi?", "STRANGER", true, false))
        assertTrue(classifier.shouldSnapshotWorkFacts("Nu mi-ai raspuns, ce meserie ai?", "STRANGER", true, false))
        assertTrue(classifier.shouldPrefetchDomainContext("Nu mi-ai raspuns, taraba?", "STRANGER", true, true))
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
