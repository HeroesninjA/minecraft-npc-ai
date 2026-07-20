package ro.ainpc.api;

import org.junit.jupiter.api.Test;
import ro.ainpc.addons.AddonDependencyGraph;
import ro.ainpc.addons.DependencyResolver;
import ro.ainpc.api.integration.IntegrationRegistryApi;
import ro.ainpc.platform.RuntimeMode;
import ro.ainpc.world.StoryMode;
import ro.ainpc.world.WorldMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void objectiveHandlerSamOverloadDelegatesFromJava() {
        TestPlatform platform = new TestPlatform(Paths.get("build", "test-data"));
        AINPCPlatformApi.ObjectiveProgressHandler handler =
            (playerUuid, currentProgress, requiredAmount) -> currentProgress + 2;

        platform.registerObjectiveHandler("collect", handler);

        assertEquals("collect", platform.registeredObjectiveType);
        assertEquals(7, platform.registeredObjectiveHandler.invoke("player", 5, 10));
    }

    @Test
    void dependencyResolverContractsRemainReservedWithoutPlatformProvider() {
        assertFalse(Arrays.stream(AINPCPlatformApi.class.getMethods()).anyMatch(method ->
            method.getName().equals("getDependencyResolver")
                || method.getName().equals("getDependencyGraph")
                || method.getReturnType().equals(DependencyResolver.class)
                || method.getReturnType().equals(AddonDependencyGraph.class)
        ));
    }

    private static final class TestPlatform implements AINPCPlatformApi {
        private final Path dataDirectory;
        private String registeredObjectiveType;
        private kotlin.jvm.functions.Function3<? super String, ? super Integer, ? super Integer, Integer>
            registeredObjectiveHandler;

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
        public IntegrationRegistryApi getIntegrationRegistry() {
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

        @Override
        public void registerObjectiveHandler(String type, kotlin.jvm.functions.Function3<? super String, ? super Integer, ? super Integer, Integer> handler) {
            registeredObjectiveType = type;
            registeredObjectiveHandler = handler;
        }

        @Override
        public double getPlayerBalance(java.util.UUID playerUuid) {
            return 0.0;
        }

        @Override
        public String getNPCName(java.util.UUID npcUuid) {
            return null;
        }

        @Override
        public String getNPCProfession(java.util.UUID npcUuid) {
            return null;
        }

        @Override
        public ro.ainpc.api.ReputationApi getReputation() {
            return null;
        }

        @Override
        public ro.ainpc.api.PlayerProgressionApi getPlayerProgression() {
            return null;
        }

        @Override
        public ro.ainpc.api.RelationshipApi getRelationships() {
            return null;
        }

        @Override
        public ro.ainpc.api.NpcEconomyApi getNpcEconomy() {
            return null;
        }

        @Override
        public ro.ainpc.api.StoryAuthoringApi getStoryAuthoring() {
            return null;
        }
    }
}
