package ro.ainpc.environment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnvironmentContextTest {
    @Test
    fun `local snapshot combines biome weather temperature and observed light`() {
        val base = EnvironmentContext.empty("sat").copy(
            weather = EnvironmentContext.Weather.RAIN,
            season = EnvironmentContext.Season.WINTER,
            lightLevel = 15,
        )

        val local = base.localized("minecraft:snowy_plains", 3)

        assertEquals(EnvironmentContext.Weather.SNOW, local.weather)
        assertEquals(EnvironmentContext.Temperature.FREEZING, local.temperature)
        assertEquals(3, local.lightLevel)
        assertEquals("sat", local.worldName)
        assertEquals(base.dayNumber, local.dayNumber)
    }

    @Test
    fun `local snapshot clamps light and can update it without biome data`() {
        val base = EnvironmentContext.empty("sat").copy(
            weather = EnvironmentContext.Weather.THUNDER,
            temperature = EnvironmentContext.Temperature.HOT,
            lightLevel = 8,
        )

        assertEquals(15, base.localized(null, 40).lightLevel)
        assertEquals(0, base.localized("", -4).lightLevel)
        assertEquals(base, base.localized(null, null))
        assertEquals(base.weather, base.localized(null, 2).weather)
        assertEquals(base.temperature, base.localized(null, 2).temperature)
    }

    @Test
    fun `prompt block exposes world day and local light`() {
        val context = EnvironmentContext.empty("sat").copy(dayNumber = 42, lightLevel = 4)

        val promptBlock = context.toPromptBlock()

        assertTrue(promptBlock.contains("world=sat day=42 light=4"))
        assertTrue(context.toDescription().contains("Lumina locala: 4/15."))
    }

    @Test
    fun `snowy biome turns precipitation into snow`() {
        assertEquals(
            EnvironmentContext.Weather.SNOW,
            EnvironmentContext.Weather.fromBukkit(true, false, "MINECRAFT:SNOWY_PLAINS")
        )
    }
}
