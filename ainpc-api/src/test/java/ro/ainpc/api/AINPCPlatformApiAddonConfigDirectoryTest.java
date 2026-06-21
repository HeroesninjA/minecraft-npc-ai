package ro.ainpc.api;

import org.junit.jupiter.api.Test;
import ro.ainpc.platform.RuntimeMode;
import ro.ainpc.world.StoryMode;
import ro.ainpc.world.WorldMode;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AINPCPlatformApiAddonConfigDirectoryTest {
    @Test
    void addonConfigDirectoryUsesSafeFallbackForBlankAddonId() {
        TestPlatform platform = new TestPlatform(Paths.get("build", "test-data"));

        Path result = platform.getAddonConfigDirectory("   ");

        assertTrue(result.endsWith(Paths.get("addons", "unknown-addon")));
        assertEquals(Paths.get("build", "test-data", "addons", "unknown-addon"), result);
    }

    @Test
    void addonConfigDirectorySanitizesAddonIdBeforeResolving() {
        TestPlatform platform = new TestPlatform(Paths.get("build", "test-data"));

        Path result = platform.getAddonConfigDirectory("My Addon/../Alpha");

        assertTrue(result.endsWith(Paths.get("addons", "my-addon-..-alpha")));
        assertEquals(Paths.get("build", "test-data", "addons", "my-addon-..-alpha"), result);
    }

    private static final class TestPlatform implements AINPCPlatformApi {
        private final Path dataDirectory;

        private TestPlatform(Path dataDirectory) {
            this.dataDirectory = dataDirectory;
        }

        @Override
        public RuntimeMode getRuntimeMode() {
            return RuntimeMode.STANDALONE;
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
            return dataDirectory;
        }

        @Override
        public Path getPackDirectory() {
            return dataDirectory.resolve("packs");
        }

        @Override
        public void reloadContent() {
        }
    }
}
