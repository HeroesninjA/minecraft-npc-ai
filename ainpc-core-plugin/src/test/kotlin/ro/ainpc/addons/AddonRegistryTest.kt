package ro.ainpc.addons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.api.AINPCPlatformApi
import ro.ainpc.api.AddonRegistryApi
import ro.ainpc.api.PlayerProgressionApi
import ro.ainpc.api.RelationshipApi
import ro.ainpc.api.ReputationApi
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.api.integration.IntegrationRegistryApi
import ro.ainpc.platform.RuntimeMode
import ro.ainpc.world.WorldNodeInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldMode
import java.nio.file.Path
import java.util.EnumSet
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

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

    @Test
    fun addonTypeAliasesMapToConstitutionalTypes() {
        assertEquals(AddonType.STORY, AddonType.fromId("story"))
        assertEquals(AddonType.RESOURCE, AddonType.fromId("resource_texture"))
        assertEquals(AddonType.TEXTURE, AddonType.fromId("texture-pack"))
        assertEquals(AddonType.DATAPACK, AddonType.fromId("data-pack"))
    }

    @Test
    fun registerAddonRunsLifecycleBeforeCommitCompletes() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()

        registry.registerAddon(lifecycleAddon("demo", "1.0.0", events))

        assertEquals(listOf("demo:1.0.0:load", "demo:1.0.0:enable"), events)
        assertEquals("1.0.0", registry.getDescriptor("demo")?.version)
    }

    @Test
    fun dependencyGateRejectsMissingDependencyBeforeLifecycle() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()

        registry.registerAddon(
            lifecycleAddon("consumer", "1.0.0", events, dependencies = listOf("missing-lib"))
        )

        assertTrue(events.isEmpty())
        assertNull(registry.getDescriptor("consumer"))
    }

    @Test
    fun dependencyGateAllowsAddonAfterDependencyIsRegistered() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("shared-lib", "1.0.0", events))

        registry.registerAddon(
            lifecycleAddon("consumer", "1.0.0", events, dependencies = listOf("shared-lib"))
        )

        assertEquals(
            listOf(
                "shared-lib:1.0.0:load",
                "shared-lib:1.0.0:enable",
                "consumer:1.0.0:load",
                "consumer:1.0.0:enable"
            ),
            events
        )
        assertNotNull(registry.getDescriptor("consumer"))
    }

    @Test
    fun dependencyGateRejectsCycleBeforeReplacingActiveAddon() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("addon-a", "1.0.0", events))
        registry.registerAddon(
            lifecycleAddon("addon-b", "1.0.0", events, dependencies = listOf("addon-a"))
        )
        events.clear()

        registry.registerAddon(
            lifecycleAddon("addon-a", "2.0.0", events, dependencies = listOf("addon-b"))
        )

        assertTrue(events.isEmpty())
        assertEquals("1.0.0", registry.getDescriptor("addon-a")?.version)
        assertEquals("1.0.0", registry.getDescriptor("addon-b")?.version)
    }

    @Test
    fun dependencyGateRejectsSecondPrimaryScenarioBeforeLifecycle() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(
            lifecycleAddon(
                "scenario-a",
                "1.0.0",
                events,
                type = AddonType.SCENARIO,
                primaryScenario = true
            )
        )
        events.clear()

        registry.registerAddon(
            lifecycleAddon(
                "scenario-b",
                "1.0.0",
                events,
                type = AddonType.SCENARIO,
                primaryScenario = true
            )
        )

        assertTrue(events.isEmpty())
        assertEquals("scenario-a", registry.primaryScenario?.id)
        assertNull(registry.getDescriptor("scenario-b"))
    }

    @Test
    fun registerDescriptorKeepsPrimaryScenarioConflictAsDiagnostic() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        registry.registerDescriptor(
            descriptor("scenario-a", AddonType.SCENARIO, true, EnumSet.allOf(RuntimeMode::class.java))
        )
        val records = mutableListOf<LogRecord>()
        val handler = recordingHandler(records)
        val registryLogger = Logger.getLogger(AddonRegistry::class.java.name)
        registryLogger.addHandler(handler)

        try {
            registry.registerDescriptor(
                descriptor("scenario-b", AddonType.SCENARIO, true, EnumSet.allOf(RuntimeMode::class.java))
            )
        } finally {
            registryLogger.removeHandler(handler)
        }

        assertNotNull(registry.getDescriptor("scenario-a"))
        assertNotNull(registry.getDescriptor("scenario-b"))
        assertEquals("scenario-a", registry.primaryScenario?.id)
        assertTrue(records.any { record ->
            record.level == Level.WARNING &&
                record.message.contains("Descriptor declarativ pastrat cu diagnostice") &&
                record.message.contains("Doua scenarii primare") &&
                record.message.contains("scenario-b")
        })
    }

    @Test
    fun dependencyGateIgnoresUnrelatedDeclarativeGraphErrors() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerDescriptor(
            AddonDescriptor(
                AddonDescriptor.ORIGIN_FEATURE_PACK,
                "orphan-pack",
                "Orphan pack",
                "1.0.0",
                dependencies = listOf("missing-pack")
            )
        )

        registry.registerAddon(lifecycleAddon("healthy", "1.0.0", events))

        assertEquals(listOf("healthy:1.0.0:load", "healthy:1.0.0:enable"), events)
        assertNotNull(registry.getDescriptor("healthy"))
    }

    @Test
    fun dependencyGateRejectsTransitivelyMissingDeclarativeDependency() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerDescriptor(
            AddonDescriptor(
                AddonDescriptor.ORIGIN_FEATURE_PACK,
                "bridge-pack",
                "Bridge pack",
                "1.0.0",
                dependencies = listOf("missing-pack")
            )
        )

        registry.registerAddon(
            lifecycleAddon("consumer", "1.0.0", events, dependencies = listOf("bridge-pack"))
        )

        assertTrue(events.isEmpty())
        assertNull(registry.getDescriptor("consumer"))
        assertNotNull(registry.getDescriptor("bridge-pack"))
    }

    @Test
    fun nonStrictValidationAllowsMissingCodeDependency() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.configure(true, false, emptyList())

        registry.registerAddon(
            lifecycleAddon("consumer", "1.0.0", events, dependencies = listOf("missing-lib"))
        )

        assertEquals(listOf("consumer:1.0.0:load", "consumer:1.0.0:enable"), events)
        assertNotNull(registry.getDescriptor("consumer"))
    }

    @Test
    fun unregisterCascadesActiveDependentsInReverseDependencyOrder() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("base", "1.0.0", events))
        registry.registerAddon(
            lifecycleAddon("mid", "1.0.0", events, dependencies = listOf("base"))
        )
        registry.registerAddon(
            lifecycleAddon("top", "1.0.0", events, dependencies = listOf("mid"))
        )
        registry.registerAddon(lifecycleAddon("unrelated", "1.0.0", events))
        events.clear()

        registry.unregisterAddon("base")

        assertEquals(
            listOf("top:1.0.0:disable", "mid:1.0.0:disable", "base:1.0.0:disable"),
            events
        )
        assertNull(registry.getDescriptor("top"))
        assertNull(registry.getDescriptor("mid"))
        assertNull(registry.getDescriptor("base"))
        assertNotNull(registry.getDescriptor("unrelated"))
    }

    @Test
    fun unregisterCascadesThroughDeclarativeDependencyBridge() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("base", "1.0.0", events))
        registry.registerDescriptor(
            AddonDescriptor(
                AddonDescriptor.ORIGIN_FEATURE_PACK,
                "bridge-pack",
                "Bridge pack",
                "1.0.0",
                dependencies = listOf("base")
            )
        )
        registry.registerAddon(
            lifecycleAddon("consumer", "1.0.0", events, dependencies = listOf("bridge-pack"))
        )
        events.clear()

        registry.unregisterAddon("base")

        assertEquals(listOf("consumer:1.0.0:disable", "base:1.0.0:disable"), events)
        assertNull(registry.getDescriptor("consumer"))
        assertNull(registry.getDescriptor("base"))
        assertNotNull(registry.getDescriptor("bridge-pack"))
    }

    @Test
    fun unregisterCascadeContinuesAndAggregatesDisableFailures() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("base", "1.0.0", events))
        registry.registerAddon(
            lifecycleAddon(
                "mid",
                "1.0.0",
                events,
                failOnDisable = true,
                dependencies = listOf("base")
            )
        )
        registry.registerAddon(
            lifecycleAddon(
                "top",
                "1.0.0",
                events,
                failOnDisable = true,
                dependencies = listOf("mid")
            )
        )
        events.clear()

        val failure = assertThrows(IllegalStateException::class.java) {
            registry.unregisterAddon("base")
        }

        assertEquals(
            listOf("top:1.0.0:disable", "mid:1.0.0:disable", "base:1.0.0:disable"),
            events
        )
        assertEquals("disable failed: top", failure.message)
        assertEquals(1, failure.suppressed.size)
        assertEquals("disable failed: mid", failure.suppressed.single().message)
        assertNull(registry.getDescriptor("top"))
        assertNull(registry.getDescriptor("mid"))
        assertNull(registry.getDescriptor("base"))
    }

    @Test
    fun replacementKeepsActiveDependentsRegistered() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("base", "1.0.0", events))
        registry.registerAddon(
            lifecycleAddon("consumer", "1.0.0", events, dependencies = listOf("base"))
        )
        events.clear()

        registry.registerAddon(lifecycleAddon("base", "2.0.0", events))

        assertEquals(
            listOf("base:1.0.0:disable", "base:2.0.0:load", "base:2.0.0:enable"),
            events
        )
        assertEquals("2.0.0", registry.getDescriptor("base")?.version)
        assertNotNull(registry.getDescriptor("consumer"))
    }

    @Test
    fun failedDependencyReplacementRestoresPreviousAddonWithoutCascade() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("base", "1.0.0", events))
        registry.registerAddon(
            lifecycleAddon("consumer", "1.0.0", events, dependencies = listOf("base"))
        )
        events.clear()

        assertThrows(IllegalStateException::class.java) {
            registry.registerAddon(lifecycleAddon("base", "2.0.0", events, failOnEnable = true))
        }

        assertEquals(
            listOf(
                "base:1.0.0:disable",
                "base:2.0.0:load",
                "base:2.0.0:enable",
                "base:2.0.0:disable",
                "base:1.0.0:load",
                "base:1.0.0:enable"
            ),
            events
        )
        assertEquals("1.0.0", registry.getDescriptor("base")?.version)
        assertNotNull(registry.getDescriptor("consumer"))
    }

    @Test
    fun configureDisablingDependencyCascadesActiveDependents() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("base", "1.0.0", events))
        registry.registerAddon(
            lifecycleAddon("consumer", "1.0.0", events, dependencies = listOf("base"))
        )
        events.clear()

        registry.configure(true, true, listOf("base"))

        assertEquals(listOf("consumer:1.0.0:disable", "base:1.0.0:disable"), events)
        assertNull(registry.getDescriptor("consumer"))
        assertNull(registry.getDescriptor("base"))
    }

    @Test
    fun featurePackRefreshDefersCascadeUntilFinalReconciliation() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        val packDescriptor = AddonDescriptor(
            AddonDescriptor.ORIGIN_FEATURE_PACK,
            "shared-pack",
            "Shared pack",
            "1.0.0"
        )
        registry.registerDescriptor(packDescriptor)
        registry.registerAddon(
            lifecycleAddon("consumer", "1.0.0", events, dependencies = listOf("shared-pack"))
        )
        events.clear()

        registry.removeByOriginForRefresh(AddonDescriptor.ORIGIN_FEATURE_PACK)
        assertNull(registry.getDescriptor("shared-pack"))
        assertNotNull(registry.getDescriptor("consumer"))

        registry.registerDescriptor(packDescriptor)
        registry.reconcileActiveDependencies()
        assertTrue(events.isEmpty())
        assertNotNull(registry.getDescriptor("consumer"))

        registry.removeByOriginForRefresh(AddonDescriptor.ORIGIN_FEATURE_PACK)
        registry.reconcileActiveDependencies()
        assertEquals(listOf("consumer:1.0.0:disable"), events)
        assertNull(registry.getDescriptor("consumer"))
    }

    @Test
    fun cascadeRejectsRegistrationDependingOnRemovingAddon() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(
            lifecycleAddon(
                "base",
                "1.0.0",
                events,
                onDisableAction = {
                    registry.registerAddon(
                        lifecycleAddon(
                            "late-consumer",
                            "1.0.0",
                            events,
                            dependencies = listOf("base")
                        )
                    )
                }
            )
        )
        events.clear()

        registry.unregisterAddon("base")

        assertEquals(listOf("base:1.0.0:disable"), events)
        assertNull(registry.getDescriptor("base"))
        assertNull(registry.getDescriptor("late-consumer"))
    }

    @Test
    fun nonStrictUnregisterKeepsDependentActive() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.configure(true, false, emptyList())
        registry.registerAddon(lifecycleAddon("base", "1.0.0", events))
        registry.registerAddon(
            lifecycleAddon("consumer", "1.0.0", events, dependencies = listOf("base"))
        )
        events.clear()

        registry.unregisterAddon("base")

        assertEquals(listOf("base:1.0.0:disable"), events)
        assertNull(registry.getDescriptor("base"))
        assertNotNull(registry.getDescriptor("consumer"))
    }

    @Test
    fun failedLoadIsCleanedUpAndNotRegistered() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()

        assertThrows(IllegalStateException::class.java) {
            registry.registerAddon(lifecycleAddon("broken", "1.0.0", events, failOnLoad = true))
        }

        assertEquals(listOf("broken:1.0.0:load", "broken:1.0.0:disable"), events)
        assertNull(registry.getDescriptor("broken"))
    }

    @Test
    fun failedEnableIsCleanedUpAndNotRegistered() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()

        assertThrows(IllegalStateException::class.java) {
            registry.registerAddon(lifecycleAddon("broken", "1.0.0", events, failOnEnable = true))
        }

        assertEquals(
            listOf("broken:1.0.0:load", "broken:1.0.0:enable", "broken:1.0.0:disable"),
            events
        )
        assertNull(registry.getDescriptor("broken"))
    }

    @Test
    fun replacementDisablesPreviousBeforeActivatingNewAddon() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("same", "1.0.0", events))

        registry.registerAddon(lifecycleAddon("same", "2.0.0", events))

        assertEquals(
            listOf(
                "same:1.0.0:load",
                "same:1.0.0:enable",
                "same:1.0.0:disable",
                "same:2.0.0:load",
                "same:2.0.0:enable"
            ),
            events
        )
        assertEquals("2.0.0", registry.getDescriptor("same")?.version)
    }

    @Test
    fun failedReplacementRestoresPreviousAddon() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("same", "1.0.0", events))

        assertThrows(IllegalStateException::class.java) {
            registry.registerAddon(lifecycleAddon("same", "2.0.0", events, failOnEnable = true))
        }

        assertEquals(
            listOf(
                "same:1.0.0:load",
                "same:1.0.0:enable",
                "same:1.0.0:disable",
                "same:2.0.0:load",
                "same:2.0.0:enable",
                "same:2.0.0:disable",
                "same:1.0.0:load",
                "same:1.0.0:enable"
            ),
            events
        )
        assertEquals("1.0.0", registry.getDescriptor("same")?.version)
    }

    @Test
    fun failedReplacementRestoresDescriptorOnlyRegistration() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerDescriptor(descriptor("same", EnumSet.allOf(RuntimeMode::class.java)))

        assertThrows(IllegalStateException::class.java) {
            registry.registerAddon(lifecycleAddon("same", "2.0.0", events, failOnEnable = true))
        }

        assertEquals("1.0.0", registry.getDescriptor("same")?.version)
        assertEquals(
            listOf("same:2.0.0:load", "same:2.0.0:enable", "same:2.0.0:disable"),
            events
        )
    }

    @Test
    fun failedPreviousDisableRestoresPreviousAddon() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("same", "1.0.0", events, failOnDisable = true))

        assertThrows(IllegalStateException::class.java) {
            registry.registerAddon(lifecycleAddon("same", "2.0.0", events))
        }

        assertEquals(
            listOf(
                "same:1.0.0:load",
                "same:1.0.0:enable",
                "same:1.0.0:disable",
                "same:1.0.0:load",
                "same:1.0.0:enable"
            ),
            events
        )
        assertEquals("1.0.0", registry.getDescriptor("same")?.version)
    }

    @Test
    fun registeringSameAddonInstanceIsIdempotent() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        val addon = lifecycleAddon("same", "1.0.0", events)

        registry.registerAddon(addon)
        registry.registerAddon(addon)

        assertEquals(listOf("same:1.0.0:load", "same:1.0.0:enable"), events)
    }

    @Test
    fun removeByOriginRunsAddonLifecycle() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("demo", "1.0.0", events))

        registry.removeByOrigin(AddonDescriptor.ORIGIN_PLUGIN_ADDON)

        assertEquals("demo:1.0.0:disable", events.last())
        assertNull(registry.getDescriptor("demo"))
    }

    @Test
    fun unregisterRemovesDescriptorWhenDisableFails() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("broken", "1.0.0", events, failOnDisable = true))

        assertThrows(IllegalStateException::class.java) {
            registry.unregisterAddon("broken")
        }

        assertNull(registry.getDescriptor("broken"))
    }

    @Test
    fun shutdownContinuesAfterDisableFailure() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(lifecycleAddon("broken", "1.0.0", events, failOnDisable = true))
        registry.registerAddon(lifecycleAddon("healthy", "1.0.0", events))

        registry.shutdown()

        assertEquals(0, registry.size())
        assertTrue(events.contains("broken:1.0.0:disable"))
        assertTrue(events.contains("healthy:1.0.0:disable"))
    }

    @Test
    fun callbackFailureIsLoggedAndDoesNotStopDispatch() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(storyCallbackAddon("broken") {
            events += "broken"
            throw IllegalStateException("story callback failed")
        })
        registry.registerAddon(storyCallbackAddon("healthy") {
            events += "healthy"
        })
        val records = mutableListOf<LogRecord>()
        val handler = recordingHandler(records)
        val registryLogger = Logger.getLogger(AddonRegistry::class.java.name)
        registryLogger.addHandler(handler)

        try {
            registry.dispatchStoryEvent("demo", "village", "Demo")
        } finally {
            registryLogger.removeHandler(handler)
        }

        assertEquals(listOf("broken", "healthy"), events)
        assertTrue(records.any { record ->
            record.level == Level.WARNING &&
                record.message.contains("onStoryEvent") &&
                record.message.contains("broken") &&
                record.thrown is IllegalStateException
        })
    }

    @Test
    fun callbackCanUnregisterItselfWithoutStoppingDispatch() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        registry.registerAddon(storyCallbackAddon("self") {
            events += "self"
            registry.unregisterAddon("self")
        })
        registry.registerAddon(storyCallbackAddon("healthy") {
            events += "healthy"
        })

        registry.dispatchStoryEvent("demo", "village", "Demo")

        assertEquals(listOf("self", "healthy"), events)
        assertNull(registry.getDescriptor("self"))
    }

    @Test
    fun dispatchRoutesEveryAddonCallbackType() {
        val registry = AddonRegistry(TestPlatform(RuntimeMode.STANDALONE))
        val events = mutableListOf<String>()
        val addonDescriptor = descriptor("callbacks", EnumSet.allOf(RuntimeMode::class.java))
        registry.registerAddon(object : AINPCAddon {
            override fun getDescriptor(): AddonDescriptor = addonDescriptor
            override fun onStoryEvent(eventType: String, scopeId: String, title: String) {
                events += "story"
            }
            override fun onRelationshipChange(npcUuidA: String, npcUuidB: String, newType: String) {
                events += "relationship"
            }
            override fun onNpcStateChange(npcUuid: String, oldState: String, newState: String) {
                events += "npc-state"
            }
            override fun onDailySalaryPaid(npcCount: Int, totalAmount: Int) {
                events += "salary"
            }
            override fun onSeasonChange(worldName: String, oldSeason: String, newSeason: String) {
                events += "season"
            }
        })

        registry.dispatchStoryEvent("demo", "village", "Demo")
        registry.dispatchRelationshipChange("a", "b", "friend")
        registry.dispatchNpcStateChange("a", "idle", "working")
        registry.dispatchSalaryPaid(2, 40)
        registry.dispatchSeasonChange("world", "Spring", "Summer")

        assertEquals(listOf("story", "relationship", "npc-state", "salary", "season"), events)
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
            if (type == AddonType.SCENARIO) listOf("scenarios", "demo") else listOf("demo"),
            listOf()
        )
    }

    private fun lifecycleAddon(
        id: String,
        version: String,
        events: MutableList<String>,
        failOnLoad: Boolean = false,
        failOnEnable: Boolean = false,
        failOnDisable: Boolean = false,
        dependencies: List<String> = emptyList(),
        type: AddonType = AddonType.FEATURE,
        primaryScenario: Boolean = false,
        onDisableAction: (() -> Unit)? = null
    ): AINPCAddon {
        val addonDescriptor = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            id,
            "Lifecycle $id",
            version,
            "",
            type,
            primaryScenario,
            EnumSet.allOf(RuntimeMode::class.java),
            if (type == AddonType.SCENARIO) listOf("scenarios", "demo") else listOf("demo"),
            dependencies
        )
        return object : AINPCAddon {
            override fun getDescriptor(): AddonDescriptor = addonDescriptor

            override fun onLoad(api: AINPCPlatformApi) {
                events += "$id:$version:load"
                if (failOnLoad) {
                    throw IllegalStateException("load failed: $id")
                }
            }

            override fun onEnable(api: AINPCPlatformApi) {
                events += "$id:$version:enable"
                if (failOnEnable) {
                    throw IllegalStateException("enable failed: $id")
                }
            }

            override fun onDisable(api: AINPCPlatformApi) {
                events += "$id:$version:disable"
                onDisableAction?.invoke()
                if (failOnDisable) {
                    throw IllegalStateException("disable failed: $id")
                }
            }
        }
    }

    private fun storyCallbackAddon(id: String, callback: () -> Unit): AINPCAddon {
        val addonDescriptor = descriptor(id, EnumSet.allOf(RuntimeMode::class.java))
        return object : AINPCAddon {
            override fun getDescriptor(): AddonDescriptor = addonDescriptor
            override fun onStoryEvent(eventType: String, scopeId: String, title: String) = callback()
        }
    }

    private fun recordingHandler(records: MutableList<LogRecord>): Handler {
        return object : Handler() {
            override fun publish(record: LogRecord) {
                records += record
            }

            override fun flush() {}

            override fun close() {}
        }.apply {
            level = Level.ALL
        }
    }

    private data class TestPlatform(private val runtimeModeValue: RuntimeMode) : AINPCPlatformApi {
        override val runtimeMode: RuntimeMode
            get() = runtimeModeValue
        override val worldMode: WorldMode
            get() = WorldMode.FINITE_DYNAMIC
        override val defaultStoryMode: StoryMode
            get() = StoryMode.EVOLUTIVE
        override val integrationRegistry: IntegrationRegistryApi
            get() = object : IntegrationRegistryApi {
                override val integrations: Collection<ro.ainpc.api.integration.ExternalPluginIntegration> = emptyList()
                override fun register(integration: ro.ainpc.api.integration.ExternalPluginIntegration) {}
                override fun unregister(pluginName: String) {}
                override fun findByType(type: ro.ainpc.api.integration.IntegrationType): List<ro.ainpc.api.integration.ExternalPluginIntegration> = emptyList()
                override fun findByCapability(capability: String): List<ro.ainpc.api.integration.ExternalPluginIntegration> = emptyList()
                override fun isRegistered(pluginName: String): Boolean = false
                override fun size(): Int = 0
            }
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
                override val isAutoIndexEnabled: Boolean = false
                override val indexedRegionChunkCount: Int = 0
                override val indexedPlaceChunkCount: Int = 0
                override val indexedNodeChunkCount: Int = 0
                override fun hasUnsavedChanges(): Boolean = false
                override fun getRegion(regionId: String?): WorldRegionInfo? = null
                override fun findRegion(worldName: String?, x: Int, y: Int, z: Int): WorldRegionInfo? = null
                override fun getPlaces(regionId: String?): Collection<WorldPlaceInfo> = emptyList()
                override fun getPlace(placeId: String?): WorldPlaceInfo? = null
                override fun findPlace(worldName: String?, x: Int, y: Int, z: Int): WorldPlaceInfo? = null
                override fun findPlacesByTag(regionId: String?, tag: String?): Collection<WorldPlaceInfo> = emptyList()
                override fun bindNpcToHomePlace(placeId: String?, npcId: String?, npcName: String?): WorldPlaceInfo =
                    throw UnsupportedOperationException("stub")
                override fun bindNpcToWorkPlace(placeId: String?, npcId: String?, npcName: String?): WorldPlaceInfo =
                    throw UnsupportedOperationException("stub")
                override fun bindNpcToSocialPlace(placeId: String?, npcId: String?, npcName: String?): WorldPlaceInfo =
                    throw UnsupportedOperationException("stub")
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
        override val reputation: ReputationApi
            get() = object : ReputationApi {
                override fun getReputation(playerUuid: String, scopeType: String, scopeId: String): Int = 0
                override fun addReputation(playerUuid: String, scopeType: String, scopeId: String, amount: Int) {}
                override fun setReputation(playerUuid: String, scopeType: String, scopeId: String, value: Int) {}
                override fun getTopReputations(scopeType: String, scopeId: String, limit: Int): List<ro.ainpc.api.ReputationEntry> = emptyList()
            }
        override val playerProgression: PlayerProgressionApi
            get() = object : PlayerProgressionApi {
                override fun getSnapshot(playerUuid: String): ro.ainpc.api.PlayerProgressionSnapshot = ro.ainpc.api.PlayerProgressionSnapshot(playerUuid, 1, 0, 100, 0, emptyMap(), 0)
                override fun grantXp(playerUuid: String, amount: Long): ro.ainpc.api.PlayerProgressionGrant = ro.ainpc.api.PlayerProgressionGrant(playerUuid, 0, 0, emptyMap(), getSnapshot(playerUuid))
                override fun addSkillXp(playerUuid: String, skillId: String, amount: Int): ro.ainpc.api.PlayerProgressionGrant = grantXp(playerUuid, amount.toLong())
                override fun setLevel(playerUuid: String, level: Int): ro.ainpc.api.PlayerProgressionSnapshot = getSnapshot(playerUuid)
                override fun resetPlayer(playerUuid: String): ro.ainpc.api.PlayerProgressionSnapshot = getSnapshot(playerUuid)
                override fun xpRequiredForLevel(level: Int): Long = 100
                override fun levelForTotalXp(totalXp: Long): Int = 1
            }
        override val relationships: RelationshipApi
            get() = object : RelationshipApi {
                override fun getAffection(npcA: java.util.UUID, npcB: java.util.UUID): Double = 0.0
                override fun getTrust(npcA: java.util.UUID, npcB: java.util.UUID): Double = 0.0
                override fun getRelationshipType(npcA: java.util.UUID, npcB: java.util.UUID): String = "stranger"
                override fun getInteractionCount(npcA: java.util.UUID, npcB: java.util.UUID): Int = 0
                override fun getTopRelationships(npcUuid: java.util.UUID, limit: Int): List<ro.ainpc.api.RelationshipEntry> = emptyList()
            }
        override val npcEconomy: ro.ainpc.api.NpcEconomyApi
            get() = object : ro.ainpc.api.NpcEconomyApi {
                override fun getBalance(npcKey: String): Int = 0
                override fun getSalary(occupation: String?): Int = 0
                override fun paySalaryForWork(npcUuid: java.util.UUID, npcDbId: Int): Boolean = false
                override fun deposit(npcKey: String, amount: Int): Boolean = false
                override fun withdraw(npcKey: String, amount: Int): Boolean = false
                override fun getBalanceCount(): Int = 0
                override fun getTotalEconomyValue(): Int = 0
            }
        override val storyAuthoring: ro.ainpc.api.StoryAuthoringApi
            get() = object : ro.ainpc.api.StoryAuthoringApi {
                override fun getAvailableTemplates(): List<String> = emptyList()
                override fun getRecentEvents(scopeType: String, scopeId: String, limit: Int): List<ro.ainpc.api.StoryEventSummary> = emptyList()
                override fun hasPendingEvents(scopeType: String, scopeId: String): Boolean = false
            }
        override val dataDirectory: Path
            get() = Path.of(".")
        override val packDirectory: Path
            get() = Path.of("packs")
        override fun reloadContent() {}
        override fun registerObjectiveHandler(type: String, handler: (playerUuid: String, currentProgress: Int, requiredAmount: Int) -> Int) {}
        override fun getNPCName(npcUuid: java.util.UUID): String? = null
        override fun getNPCProfession(npcUuid: java.util.UUID): String? = null
        override fun getPlayerBalance(playerUuid: java.util.UUID): Double = 0.0
    }
}
