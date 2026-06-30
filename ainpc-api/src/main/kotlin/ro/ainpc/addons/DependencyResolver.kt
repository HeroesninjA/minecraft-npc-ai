package ro.ainpc.addons

interface DependencyResolver {
    fun resolve(
        descriptors: Collection<AddonDescriptor>,
        enabledIds: Set<String> = emptySet()
    ): AddonDependencyGraph

    fun validate(
        graph: AddonDependencyGraph
    ): List<String>

    fun resolveLoadOrder(
        descriptors: Collection<AddonDescriptor>
    ): List<String>
}
