package ro.ainpc.addons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.platform.RuntimeMode
import java.util.EnumSet

class AddonDependencyResolverTest {
    private val resolver = AddonDependencyResolver()

    @Test
    fun resolvesEmptyDescriptorList() {
        val graph = resolver.resolve(emptyList(), emptySet())

        assertTrue(graph.isResolvable)
        assertTrue(graph.topologicalOrder.isEmpty())
        assertTrue(graph.cycles.isEmpty())
        assertTrue(graph.missingDependencies.isEmpty())
    }

    @Test
    fun resolvesSingleDescriptor() {
        val desc = descriptor("core-addon")
        val graph = resolver.resolve(listOf(desc), setOf("core-addon"))

        assertTrue(graph.isResolvable)
        assertEquals(listOf("core-addon"), graph.topologicalOrder)
    }

    @Test
    fun sortsByDependencyOrder() {
        val base = descriptor("base-addon")
        val dependent = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "dependent-addon",
            "Dependent",
            "1.0.0", "",
            AddonType.FEATURE, false,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("demo"),
            listOf("base-addon")
        )
        val graph = resolver.resolve(listOf(dependent, base), setOf("base-addon", "dependent-addon"))

        assertTrue(graph.isResolvable)
        assertEquals(listOf("base-addon", "dependent-addon"), graph.topologicalOrder)
    }

    @Test
    fun detectsCycles() {
        val a = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "addon-a", "A", "1.0.0", "",
            AddonType.FEATURE, false,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("demo"), listOf("addon-b")
        )
        val b = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "addon-b", "B", "1.0.0", "",
            AddonType.FEATURE, false,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("demo"), listOf("addon-a")
        )
        val graph = resolver.resolve(listOf(a, b), setOf("addon-a", "addon-b"))

        assertFalse(graph.isResolvable)
        assertTrue(graph.cycles.isNotEmpty())
    }

    @Test
    fun reportsMissingDependencies() {
        val desc = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "consumer", "Consumer", "1.0.0", "",
            AddonType.FEATURE, false,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("demo"), listOf("missing-lib", "another-missing")
        )
        val graph = resolver.resolve(listOf(desc), setOf("consumer"))

        assertFalse(graph.isResolvable)
        assertTrue(graph.missingDependencies.containsKey("consumer"))
        assertEquals(2, graph.missingDependencies["consumer"]?.size)
    }

    @Test
    fun detectsDuplicateScenarioPrimaryConflict() {
        val scenarioA = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "scenario-a", "A", "1.0.0", "",
            AddonType.SCENARIO, true,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("scenarios"), listOf()
        )
        val scenarioB = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "scenario-b", "B", "1.0.0", "",
            AddonType.SCENARIO, true,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("scenarios"), listOf()
        )
        val graph = resolver.resolve(listOf(scenarioA, scenarioB), setOf("scenario-a", "scenario-b"))

        assertFalse(graph.isResolvable)
        assertTrue(graph.conflicts.any { it.reason == ConflictReason.MUTUALLY_EXCLUSIVE_TYPE })
    }

    @Test
    fun detectsDuplicateAddonIds() {
        val desc1 = descriptor("duplicate-id")
        val desc2 = descriptor("duplicate-id")
        val graph = resolver.resolve(listOf(desc1, desc2), setOf("duplicate-id"))

        assertTrue(graph.conflicts.any { it.reason == ConflictReason.SAME_ID })
    }

    @Test
    fun validateReturnsWarningsForUnresolvableGraph() {
        val desc = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "orphan", "Orphan", "1.0.0", "",
            AddonType.FEATURE, false,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("demo"), listOf("ghost")
        )
        val graph = resolver.resolve(listOf(desc), setOf("orphan"))
        val warnings = resolver.validate(graph)

        assertTrue(warnings.isNotEmpty())
        assertTrue(warnings.any { it.contains("orphan") })
    }

    @Test
    fun validateReturnsEmptyForCleanGraph() {
        val desc = descriptor("clean")
        val graph = resolver.resolve(listOf(desc), setOf("clean"))
        val warnings = resolver.validate(graph)

        assertTrue(warnings.isEmpty())
    }

    @Test
    fun resolveLoadOrderReturnsDescriptorsInOrder() {
        val base = descriptor("base")
        val mid = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "mid", "Mid", "1.0.0", "",
            AddonType.FEATURE, false,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("demo"), listOf("base")
        )
        val top = AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "top", "Top", "1.0.0", "",
            AddonType.FEATURE, false,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("demo"), listOf("mid")
        )
        val order = resolver.resolveLoadOrder(listOf(top, mid, base).shuffled())

        assertEquals(listOf("base", "mid", "top"), order)
    }

    private fun descriptor(id: String): AddonDescriptor {
        return AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            id, "Demo $id", "1.0.0", "",
            AddonType.FEATURE, false,
            EnumSet.allOf(RuntimeMode::class.java),
            listOf("demo"), listOf()
        )
    }
}
