package ro.ainpc.addons;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.AINPCPlatformApi;
import ro.ainpc.api.AddonRegistryApi;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.platform.RuntimeMode;
import ro.ainpc.world.StoryMode;
import ro.ainpc.world.WorldMode;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonRegistryTest {

    @Test
    void skipsDisabledAddonDescriptors() {
        AddonRegistry registry = new AddonRegistry(new TestPlatform(RuntimeMode.STANDALONE));
        registry.configure(true, true, List.of("demo-addon"));

        registry.registerDescriptor(descriptor("demo-addon", EnumSet.allOf(RuntimeMode.class)));

        assertNull(registry.getDescriptor("demo-addon"));
        assertEquals(0, registry.size());
    }

    @Test
    void strictValidationRejectsUnsupportedRuntimeMode() {
        AddonRegistry registry = new AddonRegistry(new TestPlatform(RuntimeMode.STANDALONE));
        registry.configure(true, true, List.of());

        registry.registerDescriptor(descriptor("advanced-only", EnumSet.of(RuntimeMode.ADVANCED)));

        assertNull(registry.getDescriptor("advanced-only"));
    }

    @Test
    void disabledRegistryStillKeepsCoreDescriptorEnabled() {
        AddonRegistry registry = new AddonRegistry(new TestPlatform(RuntimeMode.STANDALONE));
        registry.configure(false, true, List.of());

        registry.registerDescriptor(new AddonDescriptor(
            AddonDescriptor.ORIGIN_CORE,
            "ainpc-core",
            "AINPC Core",
            "1.0.0",
            "",
            AddonType.CORE,
            false,
            EnumSet.allOf(RuntimeMode.class),
            List.of(),
            List.of()
        ));

        assertTrue(registry.isAddonEnabled("ainpc-core"));
        assertEquals(1, registry.size());
    }

    @Test
    void loadOrderSortsScenariosAndPrimaryFallback() {
        AddonRegistry registry = new AddonRegistry(new TestPlatform(RuntimeMode.STANDALONE));
        registry.configure(true, true, List.of(), List.of("scenario-b", "scenario-a"));

        registry.registerDescriptor(descriptor("scenario-a", AddonType.SCENARIO, false, EnumSet.allOf(RuntimeMode.class)));
        registry.registerDescriptor(descriptor("scenario-b", AddonType.SCENARIO, false, EnumSet.allOf(RuntimeMode.class)));

        assertEquals("scenario-b", registry.getDescriptors(AddonType.SCENARIO).getFirst().getId());
        assertEquals("scenario-b", registry.getPrimaryScenario().getId());
    }

    private AddonDescriptor descriptor(String id, EnumSet<RuntimeMode> runtimeModes) {
        return descriptor(id, AddonType.FEATURE, false, runtimeModes);
    }

    private AddonDescriptor descriptor(String id,
                                       AddonType type,
                                       boolean primaryScenario,
                                       EnumSet<RuntimeMode> runtimeModes) {
        return new AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            id,
            "Demo",
            "1.0.0",
            "",
            type,
            primaryScenario,
            runtimeModes,
            List.of("demo"),
            List.of()
        );
    }

    private record TestPlatform(RuntimeMode runtimeMode) implements AINPCPlatformApi {
        @Override
        public RuntimeMode getRuntimeMode() {
            return runtimeMode;
        }

        @Override
        public WorldMode getWorldMode() {
            return WorldMode.FINITE_DYNAMIC;
        }

        @Override
        public StoryMode getDefaultStoryMode() {
            return StoryMode.EVOLUTIVE;
        }

        @Override
        public AddonRegistryApi getAddonRegistry() {
            return null;
        }

        @Override
        public WorldAdminApi getWorldAdmin() {
            return null;
        }

        @Override
        public Path getDataDirectory() {
            return Path.of(".");
        }

        @Override
        public Path getPackDirectory() {
            return Path.of("packs");
        }

        @Override
        public void reloadContent() {
        }
    }
}
