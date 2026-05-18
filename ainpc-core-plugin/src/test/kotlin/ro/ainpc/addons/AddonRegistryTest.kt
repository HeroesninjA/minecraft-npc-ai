package ro.ainpc.addons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.api.AINPCPlatformApi
import ro.ainpc.api.AddonRegistryApi
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.platform.RuntimeMode
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
        override fun getRuntimeMode(): RuntimeMode = runtimeModeValue
        override fun getWorldMode(): WorldMode = WorldMode.FINITE_DYNAMIC
        override fun getDefaultStoryMode(): StoryMode = StoryMode.EVOLUTIVE
        override fun getAddonRegistry(): AddonRegistryApi? = null
        override fun getWorldAdmin(): WorldAdminApi? = null
        override fun getDataDirectory(): Path = Path.of(".")
        override fun getPackDirectory(): Path = Path.of("packs")
        override fun reloadContent() {}
    }
}
