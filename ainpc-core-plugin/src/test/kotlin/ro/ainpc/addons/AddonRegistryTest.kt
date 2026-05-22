package ro.ainpc.addons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.api.AINPCPlatformApi
import ro.ainpc.api.AddonRegistryApi
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.platform.RuntimeMode
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldMode
import java.nio.file.Path
import java.util.EnumSet

class AddonRegistryTest {
    @Test
    fun skipsDisabledAddonDescriptors() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        registry.configure(true, true, listOf("demo-addon"))

        registry.registerDescriptor(descriptor("demo-addon", EnumSet.allOf(RuntimeMode::class.java)))

        assertNull(registry.getDescriptor("demo-addon"))
        assertEquals(0, registry.size())
    }

    @Test
    fun strictValidationRejectsUnsupportedRuntimeMode() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        registry.configure(true, true, listOf())

        registry.registerDescriptor(descriptor("advanced-only", EnumSet.of(RuntimeMode.ADVANCED)))

        assertNull(registry.getDescriptor("advanced-only"))
    }

    @Test
    fun strictValidationRejectsScenarioWithoutScenariosCapability() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        registry.configure(true, true, listOf())

        registry.registerDescriptor(
            AddonDescriptor(
                AddonDescriptor.ORIGIN_PLUGIN_ADDON,
                "scenario-without-capability",
                "Scenario Demo",
                "1.0.0",
                "",
                AddonType.SCENARIO,
                false,
                EnumSet.allOf(RuntimeMode::class.java),
                listOf("dialogues"),
                listOf()
            )
        )

        assertNull(registry.getDescriptor("scenario-without-capability"))
    }

    @Test
    fun strictValidationRejectsSelfDependency() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        registry.configure(true, true, listOf())

        registry.registerDescriptor(
            AddonDescriptor(
                AddonDescriptor.ORIGIN_PLUGIN_ADDON,
                "self-dependency-addon",
                "Self Dependency Demo",
                "1.0.0",
                "",
                AddonType.FEATURE,
                false,
                EnumSet.allOf(RuntimeMode::class.java),
                listOf("demo"),
                listOf("self-dependency-addon")
            )
        )

        assertNull(registry.getDescriptor("self-dependency-addon"))
    }

    @Test
    fun strictValidationRejectsDependencyDisabledInConfig() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        registry.configure(true, true, listOf("shared-lib"))

        registry.registerDescriptor(
            AddonDescriptor(
                AddonDescriptor.ORIGIN_PLUGIN_ADDON,
                "consumer-addon",
                "Consumer Demo",
                "1.0.0",
                "",
                AddonType.FEATURE,
                false,
                EnumSet.allOf(RuntimeMode::class.java),
                listOf("demo"),
                listOf("shared-lib")
            )
        )

        assertNull(registry.getDescriptor("consumer-addon"))
    }

    @Test
    fun nonStrictValidationAllowsMinimalDescriptorMetadata() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        registry.configure(true, false, listOf())

        registry.registerDescriptor(
            AddonDescriptor(
                AddonDescriptor.ORIGIN_PLUGIN_ADDON,
                "minimal-addon",
                "Minimal Demo",
                "1.0.0",
                "",
                AddonType.SCENARIO,
                false,
                EnumSet.of(RuntimeMode.ADVANCED),
                listOf(),
                listOf("minimal-addon")
            )
        )

        assertNotNull(registry.getDescriptor("minimal-addon"))
    }

    @Test
    fun disabledRegistryStillKeepsCoreDescriptorEnabled() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        registry.configure(false, true, listOf())

        registry.registerDescriptor(
            AddonDescriptor(
                AddonDescriptor.ORIGIN_CORE,
                "ainpc-core",
                "AINPC Core",
                "1.0.0",
                "",
                AddonType.CORE,
                false,
                EnumSet.allOf(RuntimeMode::class.java),
                listOf(),
                listOf()
            )
        )

        assertTrue(registry.isAddonEnabled("ainpc-core"))
        assertEquals(1, registry.size())
    }

    @Test
    fun loadOrderSortsScenariosAndPrimaryFallback() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        registry.configure(true, true, listOf(), listOf("scenario-b", "scenario-a"))

        registry.registerDescriptor(descriptor("scenario-a", AddonType.SCENARIO, false, EnumSet.allOf(RuntimeMode::class.java)))
        registry.registerDescriptor(descriptor("scenario-b", AddonType.SCENARIO, false, EnumSet.allOf(RuntimeMode::class.java)))

        assertEquals("scenario-b", registry.getDescriptors(AddonType.SCENARIO).first().id)
        assertEquals("scenario-b", registry.primaryScenario!!.id)
    }

    private fun descriptor(id: String, runtimeModes: EnumSet<RuntimeMode>): AddonDescriptor {
        return descriptor(id, AddonType.FEATURE, false, runtimeModes)
    }

    private fun descriptor(id: String, type: AddonType, primaryScenario: Boolean, runtimeModes: EnumSet<RuntimeMode>): AddonDescriptor {
        return AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            id,
            "Demo",
            "1.0.0",
            "",
            type,
            primaryScenario,
            runtimeModes,
            listOf("demo"),
            listOf()
        )
    }

    private data class TestPlatform(private val runtimeModeValue: RuntimeMode) : AINPCPlatformApi {
        override val runtimeMode: RuntimeMode
            get() = runtimeModeValue
        override val worldMode: WorldMode
            get() = WorldMode.FINITE_DYNAMIC
        override val defaultStoryMode: StoryMode
            get() = StoryMode.EVOLUTIVE
        override val addonRegistry: AddonRegistryApi
            get() = object : AddonRegistryApi {
                override val descriptors: Collection<AddonDescriptor> = emptyList()
                override val primaryScenario: AddonDescriptor? = null
                override fun registerDescriptor(descriptor: AddonDescriptor?) {}
                override fun registerAddon(addon: AINPCAddon?) {}
                override fun unregisterAddon(addonId: String?) {}
                override fun removeByOrigin(origin: String?) {}
                override fun getDescriptors(type: AddonType?): List<AddonDescriptor> = emptyList()
                override fun getDescriptor(id: String?): AddonDescriptor? = null
                override fun isAddonEnabled(addonId: String?): Boolean = true
                override fun size(): Int = 0
            }
        override val worldAdmin: WorldAdminApi
            get() = object : WorldAdminApi {
                override val isEnabled: Boolean = false
                override val worldMode: WorldMode = WorldMode.FINITE_DYNAMIC
                override val regions: Collection<WorldRegionInfo> = emptyList()
                override val places: Collection<WorldPlaceInfo> = emptyList()
                override val nodes: Collection<WorldNodeInfo> = emptyList()
                override val regionCount: Int = 0
                override val placeCount: Int = 0
                override val nodeCount: Int = 0
                override fun getRegion(regionId: String?): WorldRegionInfo? = null
                override fun findRegion(worldName: String?, x: Int, y: Int, z: Int): WorldRegionInfo? = null
                override fun getPlaces(regionId: String?): Collection<WorldPlaceInfo> = emptyList()
                override fun getPlace(placeId: String?): WorldPlaceInfo? = null
                override fun findPlace(worldName: String?, x: Int, y: Int, z: Int): WorldPlaceInfo? = null
                override fun findPlacesByTag(regionId: String?, tag: String?): Collection<WorldPlaceInfo> = emptyList()
                override fun getNodes(regionId: String?): Collection<WorldNodeInfo> = emptyList()
                override fun getNodesForPlace(placeId: String?): Collection<WorldNodeInfo> = emptyList()
                override fun getNode(nodeId: String?): WorldNodeInfo? = null
                override fun findNode(worldName: String?, x: Int, y: Int, z: Int): WorldNodeInfo? = null
                override fun findNodesNear(
                    worldName: String?,
                    x: Double,
                    y: Double,
                    z: Double,
                    radius: Double,
                    limit: Int
                ): Collection<WorldNodeInfo> = emptyList()
            }
        override val dataDirectory: Path
            get() = Path.of(".")
        override val packDirectory: Path
            get() = Path.of("packs")
        override fun reloadContent() {}
    }
}
