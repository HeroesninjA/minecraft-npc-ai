package ro.ainpc.platform.features

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.addons.AddonDescriptor
import ro.ainpc.addons.AddonType
import ro.ainpc.platform.PlatformProfile
import ro.ainpc.platform.RuntimeMode
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldMode
import java.util.EnumSet

class RuntimeFeatureResolverTest {
    private val resolver = RuntimeFeatureResolver()

    @Test
    fun resolverUsesConservativeDefaultsWhenConfigIsMissing() {
        val snapshot = resolver.resolve(null)

        assertTrue(snapshot.enabled(RuntimeFeatureKey.QUEST))
        assertTrue(snapshot.enabled(RuntimeFeatureKey.GUI))
        assertFalse(snapshot.enabled(RuntimeFeatureKey.AI))
        assertFalse(snapshot.enabled(RuntimeFeatureKey.GENERATION))
        assertEquals(RuntimeFeatureKey.entries.size, snapshot.states().size)
        assertEquals("default features.ai=false", snapshot.get(RuntimeFeatureKey.AI).reasons().first())
    }

    @Test
    fun resolverAppliesConfigOverridesWithoutChangingRuntimeBehavior() {
        val config = YamlConfiguration()
        config.set("features.quest", false)
        config.set("features.ai", true)
        config.set("demo.enabled", true)

        val snapshot = resolver.resolve(config)

        assertFalse(snapshot.enabled(RuntimeFeatureKey.QUEST))
        assertTrue(snapshot.enabled(RuntimeFeatureKey.AI))
        assertTrue(snapshot.enabled(RuntimeFeatureKey.DEMO))
        assertEquals(RuntimeFeatureStatus.DISABLED, snapshot.get(RuntimeFeatureKey.QUEST).status())
        assertEquals(listOf("features.quest=false"), snapshot.get(RuntimeFeatureKey.QUEST).reasons())
    }

    @Test
    fun resolverIncludesAddonCapabilitySources() {
        val config = YamlConfiguration()
        config.set("features.quest", true)
        val descriptor = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "story-addon",
            "Story Addon",
            "1.0.0",
            "",
            AddonType.SCENARIO,
            true,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("scenarios", "story-hooks"),
            emptyList()
        )

        val snapshot = resolver.resolve(config, listOf(descriptor))
        val quest = snapshot.get(RuntimeFeatureKey.QUEST)
        val story = snapshot.get(RuntimeFeatureKey.STORY)
        val addons = snapshot.get(RuntimeFeatureKey.ADDONS)

        assertTrue(quest.reasons().contains("provided by addon capabilities: story-addon"))
        assertTrue(story.reasons().contains("provided by addon capabilities: story-addon"))
        assertTrue(addons.reasons().contains("active addon descriptors=1"))
        assertTrue(quest.sources().any { it.kind() == "addon-capability" && it.value() == "story-addon" })
    }

    @Test
    fun resolverIncludesServerProfileSources() {
        val profile = PlatformProfile(RuntimeMode.HYBRID, WorldMode.OPEN_DYNAMIC, StoryMode.ROTATIVE)

        val snapshot = resolver.resolve(YamlConfiguration(), profile, emptyList())
        val mapping = snapshot.get(RuntimeFeatureKey.MAPPING)

        assertTrue(mapping.reasons().contains("server profile runtime=hybrid, world=open_dynamic, story=rotative"))
        assertTrue(mapping.sources().any { it.kind() == "server-profile" && it.key() == "runtime_mode" && it.value() == "hybrid" })
    }
}
