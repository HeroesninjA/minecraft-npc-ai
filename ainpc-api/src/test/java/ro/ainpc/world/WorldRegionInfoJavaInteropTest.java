package ro.ainpc.world;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldRegionInfoJavaInteropTest {
    @Test
    void javaCanUseLegacyConstructorAndAccessors() {
        WorldRegionInfo region = new WorldRegionInfo(
            "market", "Market", "world", "town",
            -10, 60, -20, 10, 80, 20,
            List.of("trade"), StoryMode.EVOLUTIVE, "arrival", List.of("welcome")
        );

        assertEquals("market", region.id());
        assertEquals("Market", region.name());
        assertEquals("world", region.worldName());
        assertEquals("town", region.typeId());
        assertEquals(-10, region.minX());
        assertEquals(60, region.minY());
        assertEquals(-20, region.minZ());
        assertEquals(10, region.maxX());
        assertEquals(80, region.maxY());
        assertEquals(20, region.maxZ());
        assertEquals(List.of("trade"), region.tags());
        assertEquals(StoryMode.EVOLUTIVE, region.storyMode());
        assertEquals("arrival", region.storyStateKey());
        assertEquals(List.of("welcome"), region.storyPool());
        assertEquals(Map.of(), region.storyDescriptions());
        assertTrue(region.hasTag("TRADE"));
        assertTrue(region.contains("WORLD", -10, 60, -20));
        assertTrue(region.contains("world", 10, 80, 20));
        assertFalse(region.contains("other", 0, 70, 0));
        assertFalse(region.contains("world", 11, 70, 0));
    }

    @Test
    void legacyConstructorKeepsNullableDefaults() {
        WorldRegionInfo region = new WorldRegionInfo(
            "market", "Market", "world", null,
            -10, 60, -20, 10, 80, 20,
            null, null, null, null
        );

        assertEquals("custom", region.typeId());
        assertEquals(List.of(), region.tags());
        assertEquals(StoryMode.EVOLUTIVE, region.storyMode());
        assertEquals("default", region.storyStateKey());
        assertEquals(List.of(), region.storyPool());
        assertEquals(Map.of(), region.storyDescriptions());
        assertFalse(region.hasTag(null));
    }

    @Test
    void javaCanUseDescriptionConstructorWithoutSharingInput() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("arrival", "Merchants gather in the market.");
        descriptions.put("evening", "The stalls are closing.");
        WorldRegionInfo region = new WorldRegionInfo(
            "market", "Market", "world", "town",
            -10, 60, -20, 10, 80, 20,
            List.of("trade"), StoryMode.EVOLUTIVE, "arrival", List.of("welcome"), descriptions
        );

        descriptions.put("arrival", "Changed after construction.");
        descriptions.remove("evening");

        assertEquals(Map.of(
            "arrival", "Merchants gather in the market.",
            "evening", "The stalls are closing."
        ), region.storyDescriptions());
        assertEquals("arrival", region.storyStateKey());
        assertEquals(List.of("welcome"), region.storyPool());
    }
}
