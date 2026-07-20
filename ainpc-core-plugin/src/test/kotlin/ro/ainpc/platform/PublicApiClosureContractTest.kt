package ro.ainpc.platform

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.addons.AddonDependencyResolver
import ro.ainpc.addons.AddonRegistry
import ro.ainpc.addons.DependencyResolver
import ro.ainpc.ai.RelationshipService
import ro.ainpc.api.AINPCPlatformApi
import ro.ainpc.api.AddonRegistryApi
import ro.ainpc.api.NpcEconomyApi
import ro.ainpc.api.PlayerProgressionApi
import ro.ainpc.api.RelationshipApi
import ro.ainpc.api.ReputationApi
import ro.ainpc.api.StoryAuthoringApi
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.api.integration.ExternalPluginIntegration
import ro.ainpc.api.integration.IntegrationRegistryApi
import ro.ainpc.economy.NpcEconomyService
import ro.ainpc.economy.VaultEconomyHook
import ro.ainpc.integration.IntegrationRegistry
import ro.ainpc.progression.PlayerProgressionService
import ro.ainpc.reputation.ReputationService
import ro.ainpc.world.WorldAdminService
import java.io.File

class PublicApiClosureContractTest {
    private val runtimeProviders = linkedMapOf<Class<*>, Class<*>>(
        AINPCPlatformApi::class.java to AINPCPlatform::class.java,
        AddonRegistryApi::class.java to AddonRegistry::class.java,
        IntegrationRegistryApi::class.java to IntegrationRegistry::class.java,
        WorldAdminApi::class.java to WorldAdminService::class.java,
        ReputationApi::class.java to ReputationService::class.java,
        PlayerProgressionApi::class.java to PlayerProgressionService::class.java,
        RelationshipApi::class.java to RelationshipService::class.java,
        NpcEconomyApi::class.java to NpcEconomyService::class.java,
        StoryAuthoringApi::class.java to StoryAuthoringApiAdapter::class.java,
    )

    @Test
    fun everyTopLevelPublicInterfaceHasAnExplicitRuntimeRole() {
        val interfacePattern = Regex(
            pattern = "(?m)^(?:public\\s+)?(?:(?:fun|sealed)\\s+)?interface\\s+([A-Za-z][A-Za-z0-9_]*)",
        )
        val declaredInterfaces = apiSourceFiles()
            .flatMap { file ->
                interfacePattern.findAll(file.readText())
                    .map { match -> match.groupValues[1] }
                    .toList()
            }
            .toSet()
        val extensionPoints = setOf("AINPCAddon", "ExternalPluginIntegration")
        val reservedContracts = setOf("DependencyResolver")
        val classifiedInterfaces = runtimeProviders.keys.mapTo(mutableSetOf()) { contract -> contract.simpleName }
            .apply {
                addAll(extensionPoints)
                addAll(reservedContracts)
            }

        assertEquals(classifiedInterfaces, declaredInterfaces)
    }

    @Test
    fun runtimeContractsHaveConcreteCoreProviders() {
        runtimeProviders.forEach { (contract, provider) ->
            assertTrue(
                contract.isAssignableFrom(provider),
                "${provider.name} must implement ${contract.name}",
            )
        }
        assertTrue(DependencyResolver::class.java.isAssignableFrom(AddonDependencyResolver::class.java))
        assertTrue(ExternalPluginIntegration::class.java.isAssignableFrom(VaultEconomyHook::class.java))
    }

    @Test
    fun platformExposesRuntimeProvidersWithoutReservedOrSettlementLeaks() {
        val expectedAccessors = linkedMapOf(
            "getAddonRegistry" to AddonRegistryApi::class.java,
            "getIntegrationRegistry" to IntegrationRegistryApi::class.java,
            "getWorldAdmin" to WorldAdminApi::class.java,
            "getReputation" to ReputationApi::class.java,
            "getPlayerProgression" to PlayerProgressionApi::class.java,
            "getRelationships" to RelationshipApi::class.java,
            "getNpcEconomy" to NpcEconomyApi::class.java,
            "getStoryAuthoring" to StoryAuthoringApi::class.java,
        )

        expectedAccessors.forEach { (methodName, contract) ->
            assertEquals(contract, AINPCPlatformApi::class.java.getMethod(methodName).returnType)
            assertTrue(contract.isAssignableFrom(AINPCPlatform::class.java.getMethod(methodName).returnType))
        }

        val apiMethods = AINPCPlatformApi::class.java.methods.toList()
        val platformMethods = AINPCPlatform::class.java.methods.toList()
        setOf("getDependencyResolver", "getDependencyGraph").forEach { reservedAccessor ->
            assertFalse(apiMethods.any { method -> method.name == reservedAccessor })
            assertFalse(platformMethods.any { method -> method.name == reservedAccessor })
        }
        assertFalse(
            apiMethods.flatMap { method -> listOf(method.returnType) + method.parameterTypes }
                .any { type -> type.name.startsWith("ro.ainpc.api.settlement.") },
        )
    }

    @Test
    fun extensionFixturesAndProviderSourcesRemainExecutableContracts() {
        val medievalSource = File(
            "../ainpc-scenario-medieval/src/main/kotlin/ro/ainpc/addons/medieval/MedievalScenarioAddon.kt",
        ).readText()
        val javaConsumerSource = File(
            "src/test/java/ro/ainpc/addons/AddonRegistryJavaConsumerTest.java",
        ).readText()
        val apiSource = File(
            "../ainpc-api/src/main/kotlin/ro/ainpc/api/AINPCPlatformApi.kt",
        ).readText()
        val platformSource = File(
            "src/main/kotlin/ro/ainpc/platform/AINPCPlatform.kt",
        ).readText()
        val placeholderPattern = Regex(
            pattern = "(?i)(?:TODO\\s*\\(|NotImplementedError|UnsupportedOperationException|(?:throw|error\\s*\\()\\s*[^\\n]*(?:not implemented|unsupported))",
        )
        val accessorThrowPattern = Regex(
            pattern = "(?s)override\\s+val\\s+\\w+.{0,160}?get\\(\\)\\s*=\\s*(?:throw\\b|error\\s*\\()",
        )

        assertTrue(medievalSource.contains("class MedievalScenarioAddon(version: String) : AINPCAddon"))
        assertTrue(javaConsumerSource.contains("class JavaAddon implements AINPCAddon"))
        assertTrue(javaConsumerSource.contains("registerAddon(addon)"))
        assertFalse(placeholderPattern.containsMatchIn(apiSource))
        assertFalse(placeholderPattern.containsMatchIn(platformSource))
        assertFalse(accessorThrowPattern.containsMatchIn(platformSource))
    }

    private fun apiSourceFiles(): List<File> {
        return File("../ainpc-api/src/main/kotlin")
            .walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .sortedBy(File::getPath)
            .toList()
    }
}
