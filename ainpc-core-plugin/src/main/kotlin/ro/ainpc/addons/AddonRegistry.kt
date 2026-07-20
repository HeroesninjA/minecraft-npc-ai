package ro.ainpc.addons

import ro.ainpc.api.AINPCPlatformApi
import ro.ainpc.api.AddonRegistryApi
import java.util.Collections
import java.util.EnumMap
import java.util.LinkedHashMap
import java.util.Locale
import java.util.logging.Level
import java.util.logging.Logger

class AddonRegistry(private val platformApi: AINPCPlatformApi) : AddonRegistryApi {
    private val logger: Logger = Logger.getLogger(AddonRegistry::class.java.name)
    private val dependencyResolver = AddonDependencyResolver()
    private val addonIdsBeingRemoved: MutableSet<String> = LinkedHashSet()
    private val descriptorsById: MutableMap<String, AddonDescriptor> = LinkedHashMap()
    private val addonsById: MutableMap<String, AINPCAddon> = LinkedHashMap()
    private val descriptorsByType: MutableMap<AddonType, MutableList<AddonDescriptor>> = EnumMap(AddonType::class.java)
    private var registryEnabled = true
    private var strictValidation = true
    private var disabledAddonIds: List<String> = listOf()
    private var loadOrderIds: List<String> = listOf()

    @Synchronized
    fun configure(enabled: Boolean, strictValidation: Boolean, disabledAddonIds: List<String>?) {
        configure(enabled, strictValidation, disabledAddonIds, listOf())
    }

    @Synchronized
    fun configure(
        enabled: Boolean,
        strictValidation: Boolean,
        disabledAddonIds: List<String>?,
        loadOrderIds: List<String>?
    ) {
        registryEnabled = enabled
        this.strictValidation = strictValidation
        this.disabledAddonIds = normalizeIds(disabledAddonIds)
        this.loadOrderIds = normalizeIds(loadOrderIds)

        val idsToRemove = descriptorsById.keys.filter { id -> !isAddonEnabled(id) }
        for (id in idsToRemove) {
            unregisterBestEffort(id, "reconfigurare")
        }
        sortDescriptorLists()
    }

    @Synchronized
    override fun registerDescriptor(descriptor: AddonDescriptor?) {
        val validationErrors = registrationErrors(descriptor)
        if (validationErrors.isNotEmpty()) {
            logRegistrationRejection(descriptor, validationErrors)
            return
        }
        val safeDescriptor = descriptor!!
        val graphDiagnostics = dependencyGraphErrors(safeDescriptor)
        registerDescriptorInternal(safeDescriptor)
        logDescriptorDiagnostics(safeDescriptor, graphDiagnostics)
    }

    @Synchronized
    override fun registerAddon(addon: AINPCAddon?) {
        if (addon == null) {
            return
        }
        val descriptor = addon.getDescriptor()
        val validationErrors = registrationErrors(descriptor)
        if (validationErrors.isNotEmpty()) {
            logRegistrationRejection(descriptor, validationErrors)
            return
        }

        val addonId = descriptor.id
        val previousAddon = addonsById[addonId]
        if (previousAddon === addon) {
            return
        }
        val dependencyErrors = dependencyGraphErrors(descriptor)
        if (dependencyErrors.isNotEmpty()) {
            logRegistrationRejection(descriptor, dependencyErrors)
            return
        }
        val previousDescriptor = descriptorsById[addonId] ?: previousAddon?.getDescriptor()

        if (previousAddon != null || previousDescriptor != null) {
            try {
                unregisterSingleAddon(addonId)
            } catch (exception: Exception) {
                restoreRegistration(previousAddon, previousDescriptor, exception)
                reconcileActiveDependencies("rollback dezactivare addon '$addonId'")
                throw exception
            }
        }

        try {
            activateAddon(addon, descriptor)
        } catch (exception: Exception) {
            restoreRegistration(previousAddon, previousDescriptor, exception)
            reconcileActiveDependencies("rollback activare addon '$addonId'")
            throw exception
        }
    }

    @Synchronized
    override fun unregisterAddon(addonId: String?) {
        if (addonId.isNullOrBlank()) {
            return
        }
        unregisterInDependencyOrder(dependencyRemovalOrder(addonId))
    }

    @Synchronized
    override fun removeByOrigin(origin: String?) {
        val idsToRemove = descriptorsById.values
            .filter { descriptor -> descriptor.origin.equals(origin, ignoreCase = true) }
            .map { descriptor -> descriptor.id }

        for (id in idsToRemove) {
            unregisterBestEffort(id, "removeByOrigin($origin)")
        }
    }

    @Synchronized
    internal fun removeByOriginForRefresh(origin: String?) {
        val idsToRemove = descriptorsById.values
            .filter { descriptor -> descriptor.origin.equals(origin, ignoreCase = true) }
            .map { descriptor -> descriptor.id }

        for (id in idsToRemove) {
            unregisterSingleBestEffort(id, "refresh descriptori origin=$origin")
        }
    }

    @Synchronized
    internal fun reconcileActiveDependencies(operation: String = "reconciliere dependinte") {
        if (!strictValidation) {
            return
        }
        fun hasMissingDependencyPath(addonId: String, visiting: MutableSet<String>): Boolean {
            val descriptor = descriptorsById[addonId] ?: return true
            if (!visiting.add(addonId)) {
                return false
            }
            val hasMissingDependency = descriptor.dependencies.any { dependency ->
                hasMissingDependencyPath(dependency, visiting)
            }
            visiting.remove(addonId)
            return hasMissingDependency
        }

        val invalidAddonIds = addonsById.keys.filter { addonId ->
            if (addonIdsBeingRemoved.contains(addonId)) {
                return@filter false
            }
            hasMissingDependencyPath(addonId, mutableSetOf())
        }
        for (addonId in invalidAddonIds) {
            unregisterBestEffort(addonId, operation)
        }
    }

    override val descriptors: Collection<AddonDescriptor>
        @Synchronized
        get() = Collections.unmodifiableCollection(sortedDescriptors())

    @Synchronized
    override fun getDescriptors(type: AddonType?): List<AddonDescriptor> {
        val descriptors = if (type != null) descriptorsByType[type] ?: listOf() else listOf()
        return Collections.unmodifiableList(ArrayList(descriptors))
    }

    @Synchronized
    override fun getDescriptor(id: String?): AddonDescriptor? = descriptorsById[id]

    override val primaryScenario: AddonDescriptor?
        @Synchronized
        get() {
            val descriptors = sortedDescriptors()
            return descriptors.firstOrNull { descriptor ->
                descriptor.type == AddonType.SCENARIO && descriptor.isPrimaryScenario
            } ?: descriptors.firstOrNull { descriptor ->
                descriptor.type == AddonType.SCENARIO
            }
        }

    @Synchronized
    override fun size(): Int = descriptorsById.size

    @Synchronized
    override fun isAddonEnabled(addonId: String?): Boolean {
        val normalizedId = normalizeId(addonId)
        if (normalizedId.isBlank()) {
            return false
        }
        if (AddonDescriptor.ORIGIN_CORE == normalizedId || "ainpc-core" == normalizedId) {
            return true
        }
        return registryEnabled && !disabledAddonIds.contains(normalizedId)
    }

    @Synchronized
    fun dispatchStoryEvent(eventType: String, scopeId: String, title: String) {
        dispatchToAddons("onStoryEvent") { addon ->
            addon.onStoryEvent(eventType, scopeId, title)
        }
    }

    @Synchronized
    fun dispatchRelationshipChange(npcUuidA: String, npcUuidB: String, newType: String) {
        dispatchToAddons("onRelationshipChange") { addon ->
            addon.onRelationshipChange(npcUuidA, npcUuidB, newType)
        }
    }

    @Synchronized
    fun dispatchNpcStateChange(npcUuid: String, oldState: String, newState: String) {
        dispatchToAddons("onNpcStateChange") { addon ->
            addon.onNpcStateChange(npcUuid, oldState, newState)
        }
    }

    @Synchronized
    fun dispatchSalaryPaid(npcCount: Int, totalAmount: Int) {
        dispatchToAddons("onDailySalaryPaid") { addon ->
            addon.onDailySalaryPaid(npcCount, totalAmount)
        }
    }

    @Synchronized
    fun dispatchSeasonChange(worldName: String, oldSeason: String, newSeason: String) {
        dispatchToAddons("onSeasonChange") { addon ->
            addon.onSeasonChange(worldName, oldSeason, newSeason)
        }
    }

    @Synchronized
    fun shutdown() {
        val addonIds = addonsById.keys.toList().asReversed()
        for (addonId in addonIds) {
            unregisterBestEffort(addonId, "shutdown")
        }
        addonsById.clear()
        descriptorsById.clear()
        descriptorsByType.clear()
    }

    private fun dependencyRemovalOrder(addonId: String): List<String> {
        if (!descriptorsById.containsKey(addonId) && !addonsById.containsKey(addonId)) {
            return emptyList()
        }
        if (!strictValidation) {
            return listOf(addonId)
        }

        val visited = mutableSetOf<String>()
        val addonIdsToRemove = mutableSetOf(addonId)
        val removalOrder = mutableListOf<String>()

        fun visit(dependencyId: String) {
            if (!visited.add(dependencyId)) {
                return
            }
            val dependentDescriptors = descriptorsById.values
                .filter { descriptor -> descriptor.dependencies.contains(dependencyId) }
            for (dependentDescriptor in dependentDescriptors) {
                if (addonsById.containsKey(dependentDescriptor.id)) {
                    addonIdsToRemove.add(dependentDescriptor.id)
                }
                visit(dependentDescriptor.id)
            }
            if (addonIdsToRemove.contains(dependencyId)) {
                removalOrder.add(dependencyId)
            }
        }

        visit(addonId)
        return removalOrder
    }

    private fun unregisterInDependencyOrder(addonIds: List<String>) {
        if (addonIds.isEmpty()) {
            return
        }
        val newlyMarkedIds = addonIds.filter { addonId -> addonIdsBeingRemoved.add(addonId) }
        var failure: Exception? = null
        try {
            for (addonId in addonIds) {
                try {
                    unregisterSingleAddon(addonId)
                } catch (exception: Exception) {
                    val previousFailure = failure
                    if (previousFailure == null) {
                        failure = exception
                    } else {
                        previousFailure.addSuppressed(exception)
                    }
                }
            }
        } finally {
            for (addonId in newlyMarkedIds) {
                addonIdsBeingRemoved.remove(addonId)
            }
        }
        failure?.let { exception -> throw exception }
    }

    private fun unregisterSingleAddon(addonId: String) {
        val addon = addonsById.remove(addonId)
        val descriptor = descriptorsById[addonId]
        try {
            addon?.onDisable(platformApi)
        } finally {
            if (descriptor != null && descriptorsById[addonId] === descriptor) {
                removeDescriptorInternal(addonId)
            }
        }
    }

    private fun activateAddon(addon: AINPCAddon, descriptor: AddonDescriptor) {
        try {
            addon.onLoad(platformApi)
            registerDescriptorInternal(descriptor)
            addonsById[descriptor.id] = addon
            addon.onEnable(platformApi)
        } catch (exception: Exception) {
            if (addonsById[descriptor.id] === addon) {
                addonsById.remove(descriptor.id)
            }
            try {
                addon.onDisable(platformApi)
            } catch (cleanupException: Exception) {
                exception.addSuppressed(cleanupException)
            }
            if (descriptorsById[descriptor.id] === descriptor) {
                removeDescriptorInternal(descriptor.id)
            }
            throw exception
        }
    }

    private fun restoreRegistration(
        previousAddon: AINPCAddon?,
        previousDescriptor: AddonDescriptor?,
        failure: Exception
    ) {
        if (previousDescriptor == null) {
            return
        }
        try {
            if (previousAddon == null) {
                registerDescriptorInternal(previousDescriptor)
            } else {
                activateAddon(previousAddon, previousDescriptor)
            }
        } catch (rollbackException: Exception) {
            addonsById.remove(previousDescriptor.id)
            removeDescriptorInternal(previousDescriptor.id)
            failure.addSuppressed(rollbackException)
        }
    }

    private fun unregisterBestEffort(addonId: String, operation: String) {
        try {
            unregisterAddon(addonId)
        } catch (exception: Exception) {
            logger.log(
                Level.WARNING,
                "[AINPC AddonRegistry] Eroare la dezactivarea addonului '$addonId' in timpul operatiei $operation",
                exception
            )
        }
    }

    private fun unregisterSingleBestEffort(addonId: String, operation: String) {
        try {
            unregisterSingleAddon(addonId)
        } catch (exception: Exception) {
            logger.log(
                Level.WARNING,
                "[AINPC AddonRegistry] Eroare la dezactivarea addonului '$addonId' in timpul operatiei $operation",
                exception
            )
        }
    }

    private fun dispatchToAddons(callbackName: String, callback: (AINPCAddon) -> Unit) {
        val addonIds = addonsById.keys.toList()
        for (addonId in addonIds) {
            val addon = addonsById[addonId] ?: continue
            try {
                callback(addon)
            } catch (exception: Exception) {
                logger.log(
                    Level.WARNING,
                    "[AINPC AddonRegistry] Callback '$callbackName' esuat pentru addon '$addonId'",
                    exception
                )
            }
        }
    }

    private fun removeDescriptorInternal(addonId: String): AddonDescriptor? {
        val removed = descriptorsById.remove(addonId)
        if (removed != null) {
            removeDescriptorFromType(removed)
        }
        return removed
    }

    private fun removeDescriptorFromType(descriptor: AddonDescriptor) {
        val descriptors = descriptorsByType[descriptor.type]
        if (descriptors != null) {
            descriptors.removeIf { existing -> existing.id == descriptor.id }
            if (descriptors.isEmpty()) {
                descriptorsByType.remove(descriptor.type)
            }
        }
    }

    private fun sortDescriptorLists() {
        val comparator = descriptorComparator()
        for (descriptors in descriptorsByType.values) {
            descriptors.sortWith(comparator)
        }
    }

    private fun sortedDescriptors(): List<AddonDescriptor> {
        val descriptors = ArrayList(descriptorsById.values)
        descriptors.sortWith(descriptorComparator())
        return descriptors
    }

    private fun registerDescriptorInternal(descriptor: AddonDescriptor) {
        val previous = descriptorsById.put(descriptor.id, descriptor)
        if (previous != null) {
            removeDescriptorFromType(previous)
        }

        descriptorsByType
            .computeIfAbsent(descriptor.type) { mutableListOf() }
            .add(descriptor)
        descriptorsByType[descriptor.type]?.sortWith(descriptorComparator())
    }

    private fun descriptorComparator(): Comparator<AddonDescriptor> {
        return compareBy<AddonDescriptor> { descriptor -> loadOrderIndex(descriptor) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { descriptor -> descriptor.id }
    }

    private fun loadOrderIndex(descriptor: AddonDescriptor?): Int {
        if (descriptor == null) {
            return Int.MAX_VALUE
        }
        if (AddonDescriptor.ORIGIN_CORE.equals(descriptor.origin, ignoreCase = true)) {
            return -1
        }
        val index = loadOrderIds.indexOf(normalizeId(descriptor.id))
        return if (index >= 0) index else Int.MAX_VALUE
    }

    private fun registrationErrors(descriptor: AddonDescriptor?): List<String> {
        val errors = mutableListOf<String>()
        if (descriptor == null) {
            errors.add("descriptor lipsa")
            return errors
        }
        if (AddonDescriptor.ORIGIN_CORE.equals(descriptor.origin, ignoreCase = true)) {
            return errors
        }
        if (!isAddonEnabled(descriptor.id)) {
            errors.add("addon dezactivat prin configuratie: ${descriptor.id}")
            return errors
        }
        if (!strictValidation) {
            return errors
        }
        if (descriptor.id.isBlank()) {
            errors.add("id addon lipsa")
        }
        if (descriptor.name.isBlank()) {
            errors.add("name addon lipsa")
        }
        if (!descriptor.supports(platformApi.runtimeMode)) {
            errors.add(
                "runtime incompatibil: curent=${platformApi.runtimeMode.id}, " +
                    "suportat=${descriptor.supportedRuntimeModes.joinToString(",") { it.id }}"
            )
        }

        val normalizedCapabilities = LinkedHashMap<String, String>()
        for (capability in descriptor.capabilities) {
            val normalized = normalizeToken(capability)
            if (normalized.isBlank()) {
                errors.add("capability invalida (goala)")
                continue
            }
            if (normalizedCapabilities.containsKey(normalized)) {
                errors.add("capability duplicata: $normalized")
                continue
            }
            normalizedCapabilities[normalized] = capability
        }

        if (descriptor.type == AddonType.SCENARIO && !normalizedCapabilities.containsKey("scenarios")) {
            errors.add("addon SCENARIO fara capability 'scenarios'")
        }

        val normalizedDescriptorId = normalizeId(descriptor.id)
        val seenDependencies = mutableSetOf<String>()
        for (dependency in descriptor.dependencies) {
            val normalizedDependency = normalizeId(dependency)
            if (normalizedDependency.isBlank()) {
                errors.add("dependency invalida (goala)")
                continue
            }
            if (!seenDependencies.add(normalizedDependency)) {
                errors.add("dependency duplicata: $normalizedDependency")
                continue
            }
            if (normalizedDependency == normalizedDescriptorId) {
                errors.add("dependency circulara pe sine: $normalizedDependency")
            }
            if (disabledAddonIds.contains(normalizedDependency)) {
                errors.add("dependency dezactivata prin configuratie: $normalizedDependency")
            }
        }

        return errors
    }

    private fun dependencyGraphErrors(descriptor: AddonDescriptor): List<String> {
        if (!strictValidation || AddonDescriptor.ORIGIN_CORE.equals(descriptor.origin, ignoreCase = true)) {
            return emptyList()
        }

        if (addonIdsBeingRemoved.contains(descriptor.id)) {
            return listOf("addon in curs de dezactivare: ${descriptor.id}")
        }

        val candidateDescriptors = descriptorsById.values
            .filterNot { registered ->
                registered.id == descriptor.id || addonIdsBeingRemoved.contains(registered.id)
            }
            .toMutableList()
        candidateDescriptors.add(descriptor)
        val enabledIds = candidateDescriptors.mapTo(LinkedHashSet()) { candidate -> candidate.id }
        val graph = dependencyResolver.resolve(candidateDescriptors, enabledIds)
        val descriptorsByCandidateId = candidateDescriptors.associateBy { candidate -> candidate.id }
        val dependencyClosureIds = mutableSetOf<String>()
        fun collectDependencyClosure(addonId: String) {
            if (!dependencyClosureIds.add(addonId)) {
                return
            }
            descriptorsByCandidateId[addonId]?.dependencies?.forEach(::collectDependencyClosure)
        }
        collectDependencyClosure(descriptor.id)
        val candidateId = normalizeId(descriptor.id)
        val errors = mutableListOf<String>()

        graph.missingDependencies.entries
            .filter { (addonId, _) -> dependencyClosureIds.contains(addonId) }
            .forEach { (addonId, dependencies) ->
                val prefix = if (normalizeId(addonId) == candidateId) {
                    "dependinte lipsa"
                } else {
                    "dependinte tranzitive lipsa prin $addonId"
                }
                errors.add("$prefix: ${dependencies.joinToString(", ")}")
            }

        graph.cycles
            .filter { cycle -> cycle.any(dependencyClosureIds::contains) }
            .distinctBy { cycle -> cycle.map(::normalizeId).sorted().joinToString("|") }
            .forEach { cycle ->
                val closedCycle = if (cycle.isEmpty()) cycle else cycle + cycle.first()
                errors.add("ciclu de dependinte: ${closedCycle.joinToString(" -> ")}")
            }

        graph.conflicts
            .filter { conflict ->
                normalizeId(conflict.addonId) == candidateId ||
                    normalizeId(conflict.conflictingAddonId) == candidateId
            }
            .distinctBy { conflict ->
                val pair = listOf(
                    normalizeId(conflict.addonId),
                    normalizeId(conflict.conflictingAddonId)
                ).sorted()
                "${conflict.reason}:${pair.joinToString("|")}"
            }
            .forEach { conflict -> errors.add("conflict addon: ${conflict.description}") }

        return errors
    }

    private fun logDescriptorDiagnostics(descriptor: AddonDescriptor, diagnostics: List<String>) {
        if (diagnostics.isEmpty()) {
            return
        }
        logger.warning(
            "[AINPC AddonRegistry] Descriptor declarativ pastrat cu diagnostice " +
                "id=${descriptor.id} origin=${descriptor.origin}: ${diagnostics.joinToString(" | ")}"
        )
    }

    private fun logRegistrationRejection(descriptor: AddonDescriptor?, errors: List<String>) {
        if (errors.isEmpty()) {
            return
        }
        val descriptorId = descriptor?.id?.ifBlank { "<fara-id>" } ?: "<descriptor-null>"
        val origin = descriptor?.origin?.ifBlank { "<fara-origin>" } ?: "<fara-origin>"
        logger.warning(
            "[AINPC AddonRegistry] Descriptor respins id=$descriptorId origin=$origin: " +
                errors.joinToString(" | ")
        )
    }

    private fun normalizeId(addonId: String?): String = normalizeToken(addonId)

    private fun normalizeToken(value: String?): String = value?.trim()?.lowercase(Locale.ROOT) ?: ""

    private fun normalizeIds(ids: List<String>?): List<String> {
        return ids?.asSequence()
            ?.filter { id -> id.isNotBlank() }
            ?.map { id -> normalizeId(id) }
            ?.distinct()
            ?.toList()
            ?: listOf()
    }
}
