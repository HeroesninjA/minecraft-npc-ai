package ro.ainpc.addons;

import org.junit.jupiter.api.Test;
import ro.ainpc.api.AINPCPlatformApi;
import ro.ainpc.api.AddonRegistryApi;
import ro.ainpc.platform.RuntimeMode;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonRegistryJavaConsumerTest {
    @Test
    void javaAddonRegistersAndRunsCompleteLifecycle() {
        AINPCPlatformApi platform = platformProxy();
        AddonRegistryApi registry = new AddonRegistry(platform);
        JavaAddon addon = new JavaAddon();

        registry.registerAddon(addon);

        assertEquals(List.of("load", "enable"), addon.lifecycle);
        assertSame(platform, addon.loadedWith);
        assertSame(platform, addon.enabledWith);
        assertEquals(AddonType.FEATURE, addon.descriptor.getType());
        assertSame(addon.descriptor, registry.getDescriptor("java-consumer"));
        assertEquals(List.of(addon.descriptor), registry.getDescriptors(AddonType.FEATURE));
        assertNull(registry.getPrimaryScenario());
        assertEquals(1, registry.size());
        assertTrue(registry.isAddonEnabled("java-consumer"));

        registry.unregisterAddon("java-consumer");

        assertEquals(List.of("load", "enable", "disable"), addon.lifecycle);
        assertSame(platform, addon.disabledWith);
        assertNull(registry.getDescriptor("java-consumer"));
        assertEquals(0, registry.size());
    }

    private static AINPCPlatformApi platformProxy() {
        return (AINPCPlatformApi) Proxy.newProxyInstance(
            AINPCPlatformApi.class.getClassLoader(),
            new Class<?>[]{AINPCPlatformApi.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("getRuntimeMode")) {
                    return RuntimeMode.STANDALONE;
                }
                Class<?> returnType = method.getReturnType();
                if (!returnType.isPrimitive()) {
                    return null;
                }
                if (returnType == boolean.class) {
                    return false;
                }
                if (returnType == char.class) {
                    return '\0';
                }
                if (returnType == byte.class) {
                    return (byte) 0;
                }
                if (returnType == short.class) {
                    return (short) 0;
                }
                if (returnType == int.class) {
                    return 0;
                }
                if (returnType == long.class) {
                    return 0L;
                }
                if (returnType == float.class) {
                    return 0.0F;
                }
                if (returnType == double.class) {
                    return 0.0D;
                }
                return null;
            }
        );
    }

    private static final class JavaAddon implements AINPCAddon {
        private final AddonDescriptor descriptor = new AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "java-consumer",
            "Java Consumer",
            "1.0.0"
        );
        private final List<String> lifecycle = new ArrayList<>();
        private AINPCPlatformApi loadedWith;
        private AINPCPlatformApi enabledWith;
        private AINPCPlatformApi disabledWith;

        @Override
        public AddonDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public void onLoad(AINPCPlatformApi api) {
            lifecycle.add("load");
            loadedWith = api;
        }

        @Override
        public void onEnable(AINPCPlatformApi api) {
            lifecycle.add("enable");
            enabledWith = api;
        }

        @Override
        public void onDisable(AINPCPlatformApi api) {
            lifecycle.add("disable");
            disabledWith = api;
        }
    }
}
