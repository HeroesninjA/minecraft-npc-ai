package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ro.ainpc.environment.EnvironmentContext

class EnvironmentFactResolverTest {
    private val environment = EnvironmentContext.empty("sat").copy(
        timeOfDay = EnvironmentContext.TimeOfDay.EVENING,
        weather = EnvironmentContext.Weather.RAIN,
        season = EnvironmentContext.Season.AUTUMN,
        temperature = EnvironmentContext.Temperature.COLD,
        specialEvents = listOf("Targul satului", "Festivalul recoltei"),
    )

    @Test
    fun `answers live weather and season from the same snapshot`() {
        assertEquals(
            "Acum ploua. Anotimpul este toamna.",
            EnvironmentFactResolver.resolve("Cum e vremea si ce anotimp este?", environment),
        )
        assertEquals("Aici temperatura este rece.", EnvironmentFactResolver.resolve("Ce temperatura este?", environment))
        assertEquals("Acum este seara.", EnvironmentFactResolver.resolve("Ce moment al zilei este?", environment))
    }

    @Test
    fun `answers current local light without confusing it with time of day`() {
        assertEquals(
            "Lumina locala este slaba (3/15).",
            EnvironmentFactResolver.resolve("Câtă lumină este aici?", environment.copy(lightLevel = 3)),
        )
        assertEquals(
            "Lumina locala este moderata (8/15).",
            EnvironmentFactResolver.resolve("Este întuneric aici?", environment.copy(lightLevel = 8)),
        )
        assertEquals(
            "Lumina locala este puternica (15/15).",
            EnvironmentFactResolver.resolve("Cât de luminat este locul?", environment.copy(lightLevel = 20)),
        )
        assertEquals(
            "Acum este seara. Lumina locala este slaba (2/15).",
            EnvironmentFactResolver.resolve("Este zi sau noapte și câtă lumină este?", environment.copy(lightLevel = 2)),
        )
    }

    @Test
    fun `active events stay separate from historical events`() {
        assertEquals(
            "Evenimente active acum: Targul satului, Festivalul recoltei.",
            EnvironmentFactResolver.resolve("Ce evenimente speciale sunt acum?", environment),
        )
        assertEquals(
            "Evenimente active acum: Targul satului, Festivalul recoltei.",
            EnvironmentFactResolver.resolve("Ce evenimente sunt in sat acum?", environment),
        )
        assertEquals(
            "Nu stiu sa fie vreun eveniment special activ acum.",
            EnvironmentFactResolver.resolve("Ce evenimente active sunt?", environment.copy(specialEvents = emptyList())),
        )
        assertNull(EnvironmentFactResolver.resolve("Ce evenimente au fost ieri?", environment))
        assertNull(EnvironmentFactResolver.resolve("Ce evenimente speciale au fost ieri?", environment))
    }

    @Test
    fun `does not invent facts without a world or on unrelated messages`() {
        assertNull(EnvironmentFactResolver.resolve("Cum e vremea?", EnvironmentContext.empty()))
        assertNull(EnvironmentFactResolver.resolve("Salut", environment))
        assertNull(EnvironmentFactResolver.resolve("Ploua", environment))
        assertNull(EnvironmentFactResolver.resolve("Va ploua maine?", environment))
        assertNull(EnvironmentFactResolver.resolve("De ce ploua?", environment))
        assertNull(EnvironmentFactResolver.resolve("A fost întuneric ieri?", environment))
    }
}
