package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorldRegionInfoTest {
    @Test
    fun legacyNamedArgumentsRemainAvailable() {
        val region = WorldRegionInfo(
            id = "market",
            name = "Market",
            worldName = "world",
            typeId = null,
            minX = -10,
            minY = 60,
            minZ = -20,
            maxX = 10,
            maxY = 80,
            maxZ = 20,
            tags = null,
            storyMode = null,
            storyStateKey = null,
            storyPool = null
        )

        assertEquals("market", region.id())
        assertEquals("custom", region.typeId())
        assertEquals(StoryMode.EVOLUTIVE, region.storyMode())
        assertEquals(emptyMap<String, String>(), region.storyDescriptions())
    }

    @Test
    fun descriptionConstructorAcceptsNullableNamedArgument() {
        val region = WorldRegionInfo(
            id = "market",
            name = "Market",
            worldName = "world",
            typeId = null,
            minX = -10,
            minY = 60,
            minZ = -20,
            maxX = 10,
            maxY = 80,
            maxZ = 20,
            tags = null,
            storyMode = null,
            storyStateKey = null,
            storyPool = null,
            storyDescriptions = null
        )

        assertEquals("default", region.storyStateKey())
        assertEquals(emptyMap<String, String>(), region.storyDescriptions())
    }
}
