package ro.ainpc.addons

/**
 * Reserved graph-analysis contract without a provider in [ro.ainpc.api.AINPCPlatformApi].
 * Addons declare dependencies through [AddonDescriptor]; core owns lifecycle validation.
 */
interface DependencyResolver {
    fun resolve(
        descriptors: Collection<AddonDescriptor>,
        enabledIds: Set<String> = emptySet()
    ): AddonDependencyGraph

    fun resolve(descriptors: Collection<AddonDescriptor>): AddonDependencyGraph =
        resolve(descriptors, emptySet())

    fun validate(
        graph: AddonDependencyGraph
    ): List<String>

    fun resolveLoadOrder(
        descriptors: Collection<AddonDescriptor>
    ): List<String>
}
