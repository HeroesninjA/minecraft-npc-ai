package ro.ainpc.addons

import java.util.LinkedHashMap
import java.util.Locale

class AddonDependencyResolver : DependencyResolver {

    override fun resolve(
        descriptors: Collection<AddonDescriptor>,
        enabledIds: Set<String>
    ): AddonDependencyGraph {
        val enabled = descriptors.filter { d ->
            d.origin == AddonDescriptor.ORIGIN_CORE || enabledIds.isEmpty() || d.id in enabledIds
        }
        val ids = enabled.map { it.id }.toSet()
        val cycles = detectCycles(enabled)
        val missing = mutableMapOf<String, List<String>>()
        for (desc in enabled) {
            val deps = desc.dependencies.filter { it !in ids }
            if (deps.isNotEmpty()) {
                missing[desc.id] = deps
            }
        }
        val conflicts = detectConflicts(enabled)
        val isResolvable = cycles.isEmpty() && missing.isEmpty() && conflicts.isEmpty()
        val order = if (isResolvable) topologicalSort(enabled) else enabled.map { it.id }

        return AddonDependencyGraph(
            nodes = enabled.toList(),
            topologicalOrder = order,
            cycles = cycles,
            missingDependencies = missing,
            conflicts = conflicts,
            isResolvable = isResolvable
        )
    }

    override fun validate(graph: AddonDependencyGraph): List<String> {
        val warnings = mutableListOf<String>()
        if (!graph.isResolvable) {
            warnings.add("Graful de dependinte nu este rezolvabil:")
            for (cycle in graph.cycles) {
                warnings.add("  Ciclu: ${cycle.joinToString(" -> ")}")
            }
            for ((id, deps) in graph.missingDependencies) {
                warnings.add("  $id: dependinte lipsa: ${deps.joinToString(", ")}")
            }
            for (conflict in graph.conflicts) {
                warnings.add("  ${conflict.addonId} <-> ${conflict.conflictingAddonId}: ${conflict.description}")
            }
        }
        return warnings
    }

    override fun resolveLoadOrder(descriptors: Collection<AddonDescriptor>): List<String> {
        val graph = resolve(descriptors, descriptors.map { it.id }.toSet())
        return graph.topologicalOrder
    }

    private fun detectCycles(descriptors: List<AddonDescriptor>): List<List<String>> {
        val adj = mutableMapOf<String, MutableList<String>>()
        for (desc in descriptors) {
            adj.getOrPut(desc.id) { mutableListOf() }
            for (dep in desc.dependencies) {
                adj.getOrPut(desc.id) { mutableListOf() }.add(dep)
            }
        }
        val cycles = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()
        val stack = mutableListOf<String>()

        fun dfs(node: String) {
            if (node in inStack) {
                val cycleStart = stack.indexOf(node)
                if (cycleStart >= 0) {
                    cycles.add(stack.subList(cycleStart, stack.size).toList())
                }
                return
            }
            if (node in visited) return
            visited.add(node)
            inStack.add(node)
            stack.add(node)
            for (neighbor in adj[node].orEmpty()) {
                dfs(neighbor)
            }
            stack.removeAt(stack.lastIndex)
            inStack.remove(node)
        }

        for (desc in descriptors) {
            dfs(desc.id)
        }
        return cycles.distinct()
    }

    private fun topologicalSort(descriptors: List<AddonDescriptor>): List<String> {
        val adj = mutableMapOf<String, MutableList<String>>()
        val inDegree = mutableMapOf<String, Int>()
        for (desc in descriptors) {
            adj.getOrPut(desc.id) { mutableListOf() }
            inDegree.putIfAbsent(desc.id, 0)
            for (dep in desc.dependencies) {
                adj.getOrPut(dep) { mutableListOf() }.add(desc.id)
                inDegree[desc.id] = (inDegree[desc.id] ?: 0) + 1
            }
        }
        val queue = ArrayDeque<String>()
        for ((id, degree) in inDegree) {
            if (degree == 0) queue.add(id)
        }
        val order = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            order.add(node)
            for (neighbor in adj[node].orEmpty()) {
                inDegree[neighbor] = (inDegree[neighbor] ?: 1) - 1
                if (inDegree[neighbor] == 0) queue.add(neighbor)
            }
        }
        val remaining = descriptors.map { it.id }.filter { it !in order }
        order.addAll(remaining)
        return order
    }

    private fun detectConflicts(descriptors: List<AddonDescriptor>): List<AddonConflict> {
        val conflicts = mutableListOf<AddonConflict>()
        val seen = mutableMapOf<String, AddonDescriptor>()

        for (desc in descriptors) {
            val existing = seen.put(desc.id.lowercase(Locale.ROOT), desc)
            if (existing != null) {
                conflicts.add(
                    AddonConflict(
                        addonId = desc.id,
                        conflictingAddonId = existing.id,
                        reason = ConflictReason.SAME_ID,
                        description = "Addon duplicat: '${desc.id}' (${existing.origin}) si '${desc.id}' (${desc.origin})"
                    )
                )
            }

            val mainScenario = descriptors.filter { it.type == AddonType.SCENARIO && it.isPrimaryScenario }
            if (mainScenario.size > 1) {
                for (a in mainScenario) {
                    for (b in mainScenario) {
                        if (a.id != b.id) {
                            conflicts.add(
                                AddonConflict(
                                    addonId = a.id,
                                    conflictingAddonId = b.id,
                                    reason = ConflictReason.MUTUALLY_EXCLUSIVE_TYPE,
                                    description = "Doua scenarii primare: '${a.id}' si '${b.id}'. Poate exista doar unul."
                                )
                            )
                        }
                    }
                }
            }
        }
        return conflicts.distinct()
    }
}
