package ro.ainpc.engine

import ro.ainpc.ai.NpcShopFactResolver
import ro.ainpc.ai.PlayerConversationContinuation
import ro.ainpc.ai.PlayerConversationRecall
import java.util.Locale

internal object DialogueAiIntentClassification {
    fun shouldPrefetchDomainContext(
        playerMessage: String,
        relationshipStatus: String,
        hasOccupation: Boolean,
        classificationEnabled: Boolean,
    ): Boolean =
        classificationEnabled && shouldClassify(
            DialogueIntentSelector.select(playerMessage, relationshipStatus, hasOccupation, null),
            playerMessage,
            false,
        )

    fun shouldSnapshotWorkFacts(
        playerMessage: String,
        relationshipStatus: String,
        hasOccupation: Boolean,
        classificationEnabled: Boolean,
    ): Boolean {
        if (referencesConversation(playerMessage)) return false
        val localIntent = DialogueIntentSelector.select(playerMessage, relationshipStatus, hasOccupation, null)
        return localIntent == DialogueEngine.DialogueIntent.WORK_TALK ||
            (hasOccupation && classificationEnabled && shouldClassify(localIntent, playerMessage, false))
    }

    fun shouldInspectShops(
        playerMessage: String,
        relationshipStatus: String,
        hasOccupation: Boolean,
        classificationEnabled: Boolean,
    ): Boolean {
        if (referencesConversation(playerMessage)) return false
        val localIntent = DialogueIntentSelector.select(playerMessage, relationshipStatus, hasOccupation, null)
        return NpcShopFactResolver.isLookupQuestion(playerMessage) ||
            localIntent == DialogueEngine.DialogueIntent.TRADE ||
            (classificationEnabled && shouldClassify(localIntent, playerMessage, false))
    }

    fun shouldClassify(
        deterministicIntent: DialogueEngine.DialogueIntent,
        playerMessage: String,
        contextualAiNeeded: Boolean,
    ): Boolean =
        !contextualAiNeeded &&
            !referencesConversation(playerMessage) &&
            deterministicIntent in setOf(
                DialogueEngine.DialogueIntent.QUESTION,
                DialogueEngine.DialogueIntent.CONVERSATION,
            ) &&
            playerMessage.trim().length in 5..80

    private fun referencesConversation(playerMessage: String): Boolean =
        PlayerConversationContinuation.isFollowUp(playerMessage) || PlayerConversationRecall.isRecallQuestion(playerMessage) ||
            PlayerConversationRecall.isReplayRequest(playerMessage) || PlayerConversationContinuation.isShortAnswer(playerMessage)

    fun parse(
        response: String,
        hasAccessibleShop: Boolean,
        hasOccupation: Boolean,
    ): DialogueEngine.DialogueIntent? =
        when (response.trim().uppercase(Locale.ROOT)) {
            "TRADE" -> DialogueEngine.DialogueIntent.TRADE.takeIf { hasAccessibleShop }
            "WORK_TALK" -> DialogueEngine.DialogueIntent.WORK_TALK.takeIf { hasOccupation }
            else -> null
        }
}
