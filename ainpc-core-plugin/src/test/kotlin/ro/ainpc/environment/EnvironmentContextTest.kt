package ro.ainpc.environment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnvironmentContextTest {
    @Test
    fun `prompt block exposes world day and local light`() {
        val context = EnvironmentContext.empty("sat").copy(dayNumber = 42, lightLevel = 4)

        val promptBlock = context.toPromptBlock()

        assertTrue(promptBlock.contains("world=sat day=42 light=4"))
    }

    @Test
    fun `snowy biome turns precipitation into snow`() {
        assertEquals(
            EnvironmentContext.Weather.SNOW,
            EnvironmentContext.Weather.fromBukkit(true, false, "MINECRAFT:SNOWY_PLAINS")
        )
    }
}
