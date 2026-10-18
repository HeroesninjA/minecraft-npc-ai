package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.ai.PlayerRecognition

class DialogueIntentSelectorTest {
    @Test
    fun `negated requests do not start trade or help dialogue`() {
        for (message in listOf("Nu vreau sa cumpar.", "Nu cumpăr nimic.", "Nu doresc să vând.")) {
            assertEquals(DialogueEngine.DialogueIntent.CONVERSATION, DialogueIntentSelector.select(message, "STRANGER", false, null), message)
        }
        for (message in listOf("Nu am nevoie de ajutor.", "Nu vreau ajutor.")) {
            assertEquals(DialogueEngine.DialogueIntent.CONVERSATION, DialogueIntentSelector.select(message, "STRANGER", false, null), message)
        }
        assertEquals(
            DialogueEngine.DialogueIntent.TRADE,
            DialogueIntentSelector.select("Nu vreau sa cumpar, dar ce preturi ai?", "STRANGER", false, null),
        )
        assertEquals(
            DialogueEngine.DialogueIntent.HELP,
            DialogueIntentSelector.select("Nu vreau sa cumpar, dar am nevoie de ajutor.", "STRANGER", false, null),
        )
    }

    @Test
    fun `meaning questions do not activate mentioned domains`() {
        assertEquals(
            DialogueEngine.DialogueIntent.QUESTION,
            DialogueIntentSelector.select("Ce inseamna sa cumpar?", "STRANGER", false, null),
        )
        assertEquals(
            DialogueEngine.DialogueIntent.QUESTION,
            DialogueIntentSelector.select("Ce vrea sa spuna ajutor?", "STRANGER", false, null),
        )
        assertEquals(
            DialogueEngine.DialogueIntent.QUESTION,
            DialogueIntentSelector.select("Bună, ce înseamnă marfă?", "STRANGER", false, null),
        )
    }

    @Test
    fun `farewell intent requires a complete closing utterance`() {
        for (message in listOf("Pa!", "La revedere!", "Pe curand!", "Multumesc, pa!", "Bye!")) {
            assertEquals(DialogueEngine.DialogueIntent.FAREWELL, DialogueIntentSelector.select(message, "STRANGER", false, null), message)
        }
        assertEquals(DialogueEngine.DialogueIntent.QUESTION, DialogueIntentSelector.select("Ce inseamna la revedere?", "STRANGER", false, null))
        assertEquals(DialogueEngine.DialogueIntent.QUESTION, DialogueIntentSelector.select("Pa, unde este piata?", "STRANGER", false, null))
        assertEquals(DialogueEngine.DialogueIntent.CONVERSATION, DialogueIntentSelector.select("Nu spun pa.", "STRANGER", false, null))
        assertEquals(DialogueEngine.DialogueIntent.HELP, DialogueIntentSelector.select("Pa, am nevoie de ajutor!", "STRANGER", false, null))
        assertEquals(DialogueEngine.DialogueIntent.FAREWELL, DialogueIntentSelector.select("Maria, la revedere!", "STRANGER", false, null, listOf("Maria")))
    }

    @Test
    fun `farewell needs a complete word instead of a fragment in parere`() {
        assertEquals(
            DialogueEngine.DialogueIntent.QUESTION,
            DialogueIntentSelector.select("Ce parere ai despre sat?", "STRANGER", false, null)
        )
        assertEquals(
            DialogueEngine.DialogueIntent.FAREWELL,
            DialogueIntentSelector.select("Pa, ne vedem!", "STRANGER", false, null)
        )
    }

    @Test
    fun `unrecognized questions and statements do not become greetings`() {
        assertEquals(
            DialogueEngine.DialogueIntent.QUESTION,
            DialogueIntentSelector.select("Unde pot merge acum?", "STRANGER", false, null)
        )
        assertEquals(
            DialogueEngine.DialogueIntent.CONVERSATION,
            DialogueIntentSelector.select("Am vazut ceva neobisnuit astazi.", "STRANGER", false, null)
        )
        assertEquals(
            DialogueEngine.DialogueIntent.QUESTION,
            DialogueIntentSelector.select("Stii unde e satul?", "STRANGER", false, null)
        )
    }

    @Test
    fun `greeting preserves personal recognition and trade variants`() {
        val known = PlayerRecognition.from(null, 2, 0)
        assertEquals(
            DialogueEngine.DialogueIntent.GREET,
            DialogueIntentSelector.select("Bună!", "STRANGER", false, known)
        )
        assertEquals(
            DialogueEngine.DialogueIntent.TRADE,
            DialogueIntentSelector.select("Care este prețul?", "STRANGER", false, null)
        )
    }

    @Test
    fun `greeting prefix does not hide the actual question or request`() {
        assertEquals(
            DialogueEngine.DialogueIntent.QUESTION,
            DialogueIntentSelector.select("Salut, ce parere ai despre sat?", "STRANGER", false, null)
        )
        assertEquals(
            DialogueEngine.DialogueIntent.HELP,
            DialogueIntentSelector.select("Buna, am nevoie de ajutor", "STRANGER", false, null)
        )
        assertEquals(
            DialogueEngine.DialogueIntent.CONVERSATION,
            DialogueIntentSelector.select("Salut, am vazut ceva neobisnuit.", "STRANGER", false, null)
        )
    }
}
