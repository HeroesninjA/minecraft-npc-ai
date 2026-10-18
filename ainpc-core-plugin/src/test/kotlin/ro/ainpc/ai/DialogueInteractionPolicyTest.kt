package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DialogueInteractionPolicyTest {
    @Test
    fun `questions and general sentiment do not create social effects`() {
        for (sentiment in listOf("neutral", "question", "positive", "negative")) {
            assertEquals(DialogueInteractionEffect(), DialogueInteractionPolicy.forMessage("Cum este vremea?", sentiment, true, "Bob"))
        }
    }

    @Test
    fun `greeting affects the current mood without changing relationship values`() {
        val effect = DialogueInteractionPolicy.forMessage("Bună ziua!", "greeting", true, "Bob")

        assertEquals(0.0, effect.affectionChange)
        assertEquals(0.0, effect.trustChange)
        assertNull(effect.memoryImportance)
        assertEquals("greeting", effect.emotionType)
    }

    @Test
    fun `explicit social acts have distinct persistent and emotional effects`() {
        val compliment = DialogueInteractionPolicy.forMessage("Îți mulțumesc!", "compliment", true, "Bob")
        val insult = DialogueInteractionPolicy.forMessage("Ești tâmpit!", "insult", true, "Bob")
        val threat = DialogueInteractionPolicy.forMessage("Îți rup uneltele!", "threat", true, "Bob")

        assertEquals(3, compliment.memoryImportance)
        assertEquals("compliment", compliment.emotionType)
        assertEquals(-0.1, insult.affectionChange)
        assertEquals(4, insult.memoryImportance)
        assertEquals("insult", insult.emotionType)
        assertEquals(-0.2, threat.trustChange)
        assertEquals(4, threat.memoryImportance)
        assertEquals("threat", threat.emotionType)
    }

    @Test
    fun `reported insults and threats do not change the relationship`() {
        val messages = listOf(
            "Maria m-a numit prost.",
            "Maria a zis: \"Te omor!\"",
            "Maria spune că te omor.",
            "De ce mi-ai spus că sunt idiot?",
            "Nu ești prost.",
        )

        for (message in messages) {
            val sentiment = OpenAITextSupport.analyzeSentimentFast(message)
            assertEquals(DialogueInteractionEffect(), DialogueInteractionPolicy.forMessage(message, sentiment, true, "Bob"), message)
        }
    }

    @Test
    fun `direct social act after a reported clause uses only the direct sentiment`() {
        val compliment = DialogueInteractionPolicy.forMessage(
            "Maria a zis \"te omor!\", dar iti multumesc.", "threat", true, "Bob",
        )
        val insult = DialogueInteractionPolicy.forMessage(
            "Am auzit ca esti prost; dar esti idiot!", "insult", true, "Bob",
        )
        val threat = DialogueInteractionPolicy.forMessage(
            "Maria a zis ca esti prost. Te omor!", "insult", true, "Bob",
        )

        assertEquals("compliment", compliment.emotionType)
        assertEquals(3, compliment.memoryImportance)
        assertEquals(0.05, compliment.affectionChange)
        assertEquals("insult", insult.emotionType)
        assertEquals(4, insult.memoryImportance)
        assertEquals("threat", threat.emotionType)
        assertEquals(-0.2, threat.trustChange)
        assertEquals(
            DialogueInteractionEffect(),
            DialogueInteractionPolicy.forMessage("Maria a zis ca esti prost; dar esti idiot, Ana!", "insult", true, "Bob"),
        )
    }

    @Test
    fun `direct social act after a leading quote uses only unquoted words`() {
        val compliment = DialogueInteractionPolicy.forMessage(
            "\"te omor!\", dar multumesc.", "threat", true, "Bob",
        )
        val insult = DialogueInteractionPolicy.forMessage(
            "'te respect', dar esti idiot!", "compliment", true, "Bob",
        )
        val unicodeInsult = DialogueInteractionPolicy.forMessage(
            "„te respect”, dar ești idiot!", "compliment", true, "Bob",
        )

        assertEquals("compliment", compliment.emotionType)
        assertEquals(3, compliment.memoryImportance)
        assertEquals("insult", insult.emotionType)
        assertEquals(4, insult.memoryImportance)
        assertEquals("insult", unicodeInsult.emotionType)
        assertEquals(
            DialogueInteractionEffect(),
            DialogueInteractionPolicy.forMessage("\"te omor!\", dar esti idiot, Maria!", "threat", true, "Bob"),
        )
    }

    @Test
    fun `leading quotes alone and unclosed quotes do not affect relationships`() {
        for (message in listOf("\"te omor!\"", "'esti idiot'", "\"te omor, dar multumesc.")) {
            val sentiment = OpenAITextSupport.analyzeSentimentFast(message)
            assertEquals(DialogueInteractionEffect(), DialogueInteractionPolicy.forMessage(message, sentiment, true, "Bob"), message)
        }
    }

    @Test
    fun `reported and negated clauses still do not become social acts`() {
        for (message in listOf(
            "Maria a zis ca esti idiot!",
            "Maria a zis \"te omor!\"",
            "Maria a zis \"te omor; dar iti multumesc.",
            "Maria a zis ca esti idiot; dar nu esti prost.",
            "Maria a zis ca esti idiot; dar Maria a spus ca esti prost.",
        )) {
            val sentiment = OpenAITextSupport.analyzeSentimentFast(message)
            assertEquals(DialogueInteractionEffect(), DialogueInteractionPolicy.forMessage(message, sentiment, true, "Bob"), message)
        }
    }

    @Test
    fun `social words about another subject and passive speech do not target the npc`() {
        val messages = listOf(
            "Respect sabia aceasta.",
            "Negustorul este prost.",
            "Prietenul tău este idiot.",
            "Tu crezi că Maria e idiot?",
            "Dă-mi voie să distrug poarta.",
        )

        for (message in messages) {
            val sentiment = OpenAITextSupport.analyzeSentimentFast(message)
            assertEquals(DialogueInteractionEffect(), DialogueInteractionPolicy.forMessage(message, sentiment, true, "Bob"), message)
        }
        assertEquals(
            DialogueInteractionEffect(),
            DialogueInteractionPolicy.forMessage("Ești tâmpit!", "insult", false, "Bob"),
        )
    }

    @Test
    fun `direct short address still counts as a social act`() {
        assertEquals(4, DialogueInteractionPolicy.forMessage("Prost!", "insult", true, "Bob").memoryImportance)
        assertEquals(3, DialogueInteractionPolicy.forMessage("Mulțumesc!", "compliment", true, "Bob").memoryImportance)
    }

    @Test
    fun `named address to another person does not affect the active npc`() {
        val messages = listOf(
            "Maria, ești idiot!",
            "Maria, te omor!",
            "Maria, mulțumesc!",
            "Ești prost, Maria!",
            "Mulțumesc, Maria!",
            "Mulțumesc, Maria !",
            "Salut, Maria!",
            "Bună ziua, Maria!",
        )

        for (message in messages) {
            val sentiment = OpenAITextSupport.analyzeSentimentFast(message)
            assertEquals(DialogueInteractionEffect(), DialogueInteractionPolicy.forMessage(message, sentiment, true, "Bob"), message)
        }
    }

    @Test
    fun `named address to active npc or display name retains social effect`() {
        assertEquals(4, DialogueInteractionPolicy.forMessage("Bob, ești idiot!", "insult", true, "Bob").memoryImportance)
        assertEquals(4, DialogueInteractionPolicy.forMessage("Maria, te omor!", "threat", true, "Bob", "Maria").memoryImportance)
        assertEquals(3, DialogueInteractionPolicy.forMessage("Mulțumesc, Măria!", "compliment", true, "Bob", "Maria").memoryImportance)
        assertEquals(3, DialogueInteractionPolicy.forMessage("Maria, mulțumesc!", "compliment", true, "Bob", "Maria").memoryImportance)
        assertEquals("greeting", DialogueInteractionPolicy.forMessage("Salut, Bob!", "greeting", true, "Bob").emotionType)
    }

    @Test
    fun `clarified recipient receives social effect despite other names in original address`() {
        val message = "Bob, ești idiot, Maria!"

        assertEquals(
            DialogueInteractionEffect(),
            DialogueInteractionPolicy.forMessage(message, "insult", true, "Maria"),
        )
        val clarifiedEffect = DialogueInteractionPolicy.forMessage(
            message, "insult", true, "Maria", addressClarified = true,
        )

        assertEquals(-0.1, clarifiedEffect.affectionChange)
        assertEquals(4, clarifiedEffect.memoryImportance)
        assertEquals("insult", clarifiedEffect.emotionType)
    }

    @Test
    fun `clarifying recipient does not turn reported negated or third person speech into social acts`() {
        val messages = listOf(
            "\"Bob, ești idiot, Maria!\"",
            "Am auzit că ești idiot, Maria!",
            "Bob, nu ești idiot, Maria!",
            "Bob, negustorul este idiot, Maria!",
        )

        for (message in messages) {
            assertEquals(
                DialogueInteractionEffect(),
                DialogueInteractionPolicy.forMessage(message, "insult", true, "Maria", addressClarified = true),
                message,
            )
        }
    }

    @Test
    fun `clarifying recipient does not permit social effects for passive speech`() {
        assertEquals(
            DialogueInteractionEffect(),
            DialogueInteractionPolicy.forMessage(
                "Bob, ești idiot, Maria!", "insult", false, "Maria", addressClarified = true,
            ),
        )
    }
}
