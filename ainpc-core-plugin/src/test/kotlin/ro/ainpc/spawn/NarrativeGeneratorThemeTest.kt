package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NarrativeGeneratorThemeTest {
    @Test
    fun resolvesThemeTagCaseInsensitivelyAndNormalizesId() {
        assertEquals(
            "dark_fantasy",
            resolveTaggedPopulationTheme(listOf("village", " Theme: Dark Fantasy "))
        )
        assertEquals("generic", resolveTaggedPopulationTheme(listOf("theme:generic")))
    }

    @Test
    fun ignoresMalformedThemeTags() {
        assertNull(
            resolveTaggedPopulationTheme(
                listOf("biome:plains", "theme", "theme: ", "theme: !!!")
            )
        )
    }

    @Test
    fun selectsTheSameThemeRegardlessOfTagOrder() {
        val tags = listOf("theme:rustic", "THEME: Alpine", "theme:rustic")

        assertEquals("alpine", resolveTaggedPopulationTheme(tags))
        assertEquals("alpine", resolveTaggedPopulationTheme(tags.reversed()))
    }
}
