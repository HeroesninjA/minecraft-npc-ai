package ro.ainpc.engine

import java.text.Normalizer
import java.util.Locale

/**
 * Interpreteaza raspunsurile scurte ale jucatorilor doar cand exista o oferta de quest activa.
 */
class QuestDecisionIntentResolver {
    enum class Intent {
        NONE,
        OFFER,
        ACCEPT,
        DECLINE,
        ABANDON,
        STATUS,
        COMPLETE
    }

    fun resolve(message: String?, questOfferActive: Boolean): Intent {
        val normalizedMessage = normalize(message)
        if (normalizedMessage.isBlank()) {
            return Intent.NONE
        }

        if (containsQuestAbandonKeyword(normalizedMessage)) {
            return Intent.ABANDON
        }
        if (containsQuestDeclineKeyword(normalizedMessage, questOfferActive)) {
            return Intent.DECLINE
        }
        if (containsQuestAcceptKeyword(normalizedMessage, questOfferActive)) {
            return Intent.ACCEPT
        }
        if (containsQuestStatusKeyword(normalizedMessage)) {
            return Intent.STATUS
        }
        if (containsQuestCompletionKeyword(normalizedMessage)) {
            return Intent.COMPLETE
        }
        if (containsQuestKeyword(normalizedMessage)) {
            return Intent.OFFER
        }

        return Intent.NONE
    }

    private fun containsQuestKeyword(normalizedMessage: String): Boolean {
        return containsWord(normalizedMessage, "misiune") ||
            containsWord(normalizedMessage, "misiuni") ||
            containsWord(normalizedMessage, "quest") ||
            containsWord(normalizedMessage, "quests") ||
            containsWord(normalizedMessage, "sarcina") ||
            containsWord(normalizedMessage, "treaba")
    }

    private fun containsQuestAcceptKeyword(normalizedMessage: String, questOfferActive: Boolean): Boolean {
        if (containsWord(normalizedMessage, "accept") ||
            containsWord(normalizedMessage, "accepta") ||
            containsWord(normalizedMessage, "confirm") ||
            normalizedMessage.contains("sunt de acord")
        ) {
            return true
        }

        if (!questOfferActive) {
            return false
        }

        return SHORT_ACCEPTS.contains(normalizedMessage) ||
            normalizedMessage.contains("ma bag") ||
            normalizedMessage.contains("ma ocup") ||
            normalizedMessage.contains("o fac") ||
            normalizedMessage.contains("fac asta") ||
            normalizedMessage.contains("fac misiunea") ||
            normalizedMessage.contains("fac quest") ||
            normalizedMessage.contains("iau misiunea") ||
            normalizedMessage.contains("iau quest") ||
            normalizedMessage.contains("vreau misiunea") ||
            normalizedMessage.contains("vreau quest") ||
            normalizedMessage.contains("suna bine") ||
            normalizedMessage.contains("incep") ||
            normalizedMessage.contains("pornesc")
    }

    private fun containsQuestDeclineKeyword(normalizedMessage: String, questOfferActive: Boolean): Boolean {
        if (containsWord(normalizedMessage, "refuz") ||
            containsWord(normalizedMessage, "refuza") ||
            containsWord(normalizedMessage, "resping") ||
            containsWord(normalizedMessage, "reject") ||
            containsWord(normalizedMessage, "decline") ||
            containsWord(normalizedMessage, "deny") ||
            normalizedMessage.contains("nu accept")
        ) {
            return true
        }

        if (!questOfferActive) {
            return false
        }

        return SHORT_DECLINES.contains(normalizedMessage) ||
            normalizedMessage.contains("nu acum") ||
            normalizedMessage.contains("nu pot") ||
            normalizedMessage.contains("nu vreau") ||
            normalizedMessage.contains("nu am timp") ||
            normalizedMessage.contains("n am timp") ||
            normalizedMessage.contains("poate mai tarziu") ||
            normalizedMessage.contains("mai tarziu") ||
            normalizedMessage.contains("alta data") ||
            normalizedMessage.contains("las pe alta data")
    }

    private fun containsQuestAbandonKeyword(normalizedMessage: String): Boolean {
        return containsWord(normalizedMessage, "renunt") ||
            containsWord(normalizedMessage, "abandonez") ||
            containsWord(normalizedMessage, "abandon")
    }

    private fun containsQuestStatusKeyword(normalizedMessage: String): Boolean {
        return containsWord(normalizedMessage, "status") ||
            containsWord(normalizedMessage, "progres")
    }

    private fun containsQuestCompletionKeyword(normalizedMessage: String): Boolean {
        return containsWord(normalizedMessage, "gata") ||
            containsWord(normalizedMessage, "terminat") ||
            containsWord(normalizedMessage, "finalizat") ||
            normalizedMessage.contains("am adus")
    }

    private fun containsWord(normalizedMessage: String?, token: String?): Boolean {
        if (normalizedMessage.isNullOrBlank() || token.isNullOrBlank()) {
            return false
        }

        for (word in normalizedMessage.split(" ")) {
            if (word == token) {
                return true
            }
        }

        return false
    }

    companion object {
        private val SHORT_ACCEPTS = listOf("da", "yes", "y", "ok", "okay", "sigur", "bine", "hai")
        private val SHORT_DECLINES = listOf("nu", "no", "n", "pas")

        @JvmStatic
        fun normalize(value: String?): String {
            if (value == null) {
                return ""
            }

            val withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "")
            return withoutDiacritics.lowercase(Locale.ROOT)
                .replace(Regex("[^\\p{L}\\p{Nd}\\s]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}
