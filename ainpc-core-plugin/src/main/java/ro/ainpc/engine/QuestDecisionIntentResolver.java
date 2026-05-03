package ro.ainpc.engine;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Interpreteaza raspunsurile scurte ale jucatorilor doar cand exista o oferta de quest activa.
 */
public final class QuestDecisionIntentResolver {

    private static final List<String> SHORT_ACCEPTS = List.of("da", "yes", "y", "ok", "okay", "sigur", "bine", "hai");
    private static final List<String> SHORT_DECLINES = List.of("nu", "no", "n", "pas");

    public enum Intent {
        NONE,
        OFFER,
        ACCEPT,
        DECLINE,
        ABANDON,
        STATUS,
        COMPLETE
    }

    public Intent resolve(String message, boolean questOfferActive) {
        String normalizedMessage = normalize(message);
        if (normalizedMessage.isBlank()) {
            return Intent.NONE;
        }

        if (containsQuestAbandonKeyword(normalizedMessage)) {
            return Intent.ABANDON;
        }
        if (containsQuestDeclineKeyword(normalizedMessage, questOfferActive)) {
            return Intent.DECLINE;
        }
        if (containsQuestAcceptKeyword(normalizedMessage, questOfferActive)) {
            return Intent.ACCEPT;
        }
        if (containsQuestStatusKeyword(normalizedMessage)) {
            return Intent.STATUS;
        }
        if (containsQuestCompletionKeyword(normalizedMessage)) {
            return Intent.COMPLETE;
        }
        if (containsQuestKeyword(normalizedMessage)) {
            return Intent.OFFER;
        }

        return Intent.NONE;
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return withoutDiacritics.toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private boolean containsQuestKeyword(String normalizedMessage) {
        return containsWord(normalizedMessage, "misiune")
            || containsWord(normalizedMessage, "misiuni")
            || containsWord(normalizedMessage, "quest")
            || containsWord(normalizedMessage, "quests")
            || containsWord(normalizedMessage, "sarcina")
            || containsWord(normalizedMessage, "treaba");
    }

    private boolean containsQuestAcceptKeyword(String normalizedMessage, boolean questOfferActive) {
        if (containsWord(normalizedMessage, "accept")
            || containsWord(normalizedMessage, "accepta")
            || containsWord(normalizedMessage, "confirm")
            || normalizedMessage.contains("sunt de acord")) {
            return true;
        }

        if (!questOfferActive) {
            return false;
        }

        return SHORT_ACCEPTS.contains(normalizedMessage)
            || normalizedMessage.contains("ma bag")
            || normalizedMessage.contains("ma ocup")
            || normalizedMessage.contains("o fac")
            || normalizedMessage.contains("fac asta")
            || normalizedMessage.contains("fac misiunea")
            || normalizedMessage.contains("fac quest")
            || normalizedMessage.contains("iau misiunea")
            || normalizedMessage.contains("iau quest")
            || normalizedMessage.contains("vreau misiunea")
            || normalizedMessage.contains("vreau quest")
            || normalizedMessage.contains("suna bine")
            || normalizedMessage.contains("incep")
            || normalizedMessage.contains("pornesc");
    }

    private boolean containsQuestDeclineKeyword(String normalizedMessage, boolean questOfferActive) {
        if (containsWord(normalizedMessage, "refuz")
            || containsWord(normalizedMessage, "refuza")
            || containsWord(normalizedMessage, "resping")
            || containsWord(normalizedMessage, "reject")
            || containsWord(normalizedMessage, "decline")
            || containsWord(normalizedMessage, "deny")
            || normalizedMessage.contains("nu accept")) {
            return true;
        }

        if (!questOfferActive) {
            return false;
        }

        return SHORT_DECLINES.contains(normalizedMessage)
            || normalizedMessage.contains("nu acum")
            || normalizedMessage.contains("nu pot")
            || normalizedMessage.contains("nu vreau")
            || normalizedMessage.contains("nu am timp")
            || normalizedMessage.contains("n am timp")
            || normalizedMessage.contains("poate mai tarziu")
            || normalizedMessage.contains("mai tarziu")
            || normalizedMessage.contains("alta data")
            || normalizedMessage.contains("las pe alta data");
    }

    private boolean containsQuestAbandonKeyword(String normalizedMessage) {
        return containsWord(normalizedMessage, "renunt")
            || containsWord(normalizedMessage, "abandonez")
            || containsWord(normalizedMessage, "abandon");
    }

    private boolean containsQuestStatusKeyword(String normalizedMessage) {
        return containsWord(normalizedMessage, "status")
            || containsWord(normalizedMessage, "progres");
    }

    private boolean containsQuestCompletionKeyword(String normalizedMessage) {
        return containsWord(normalizedMessage, "gata")
            || containsWord(normalizedMessage, "terminat")
            || containsWord(normalizedMessage, "finalizat")
            || normalizedMessage.contains("am adus");
    }

    private boolean containsWord(String normalizedMessage, String token) {
        if (normalizedMessage == null || normalizedMessage.isBlank() || token == null || token.isBlank()) {
            return false;
        }

        for (String word : normalizedMessage.split(" ")) {
            if (word.equals(token)) {
                return true;
            }
        }

        return false;
    }
}
