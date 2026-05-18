package ro.ainpc.addons

import ro.ainpc.api.AINPCPlatformApi
import ro.ainpc.api.AddonRegistryApi
import java.util.Collections
import java.util.EnumMap
import java.util.LinkedHashMap

class AddonRegistry(private val platformApi: AINPCPlatformApi) : AddonRegistryApi {
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
            unregisterAddon(id)
        }
        sortDescriptorLists()
    }

    @Synchronized
    override fun registerDescriptor(descriptor: AddonDescriptor?) {
        if (!canRegister(descriptor)) {
            return
        }
        val safeDescriptor = descriptor!!

        val previous = descriptorsById.put(safeDescriptor.id, safeDescriptor)
        if (previous != null) {
            removeDescriptorFromType(previous)
        }

        descriptorsByType
            .computeIfAbsent(safeDescriptor.type) { mutableListOf() }
            .add(safeDescriptor)
        descriptorsByType[safeDescriptor.type]?.sortWith(descriptorComparator())
    }

    @Synchronized
    override fun registerAddon(addon: AINPCAddon?) {
        if (addon == null) {
            return
        }
        val descriptor = addon.getDescriptor()
        if (!canRegister(descriptor)) {
            return
        }

        unregisterAddon(descriptor.id)
        addon.onLoad(platformApi)
        registerDescriptor(descriptor)
        addonsById[descriptor.id] = addon
        addon.onEnable(platformApi)
    }

    @Synchronized
    override fun unregisterAddon(addonId: String?) {
        if (addonId.isNullOrBlank()) {
            return
        }

        val addon = addonsById.remove(addonId)
        if (addon != null) {
            addon.onDisable(platformApi)
        }

        val removed = descriptorsById.remove(addonId)
        if (removed != null) {
            removeDescriptorFromType(removed)
        }
    }

    @Synchronized
    override fun removeByOrigin(origin: String?) {
        val idsToRemove = descriptorsById.values
            .filter { descriptor -> descriptor.origin.equals(origin, ignoreCase = true) }
            .map { descriptor -> descriptor.id }

        for (id in idsToRemove) {
            val removed = descriptorsById.remove(id)
            if (removed != null) {
                removeDescriptorFromType(removed)
            }
            addonsById.remove(id)
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
    fun shutdown() {
        val addons = ArrayList(addonsById.values)
        addons.reverse()
        for (addon in addons) {
            addon.onDisable(platformApi)
        }
        addonsById.clear()
        descriptorsById.clear()
        descriptorsByType.clear()
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

    private fun canRegister(descriptor: AddonDescriptor?): Boolean {
        if (descriptor == null) {
            return false
        }
        if (AddonDescriptor.ORIGIN_CORE.equals(descriptor.origin, ignoreCase = true)) {
            return true
        }
        if (!isAddonEnabled(descriptor.id)) {
            return false
        }
        if (!strictValidation) {
            return true
        }
        if (descriptor.id.isBlank() || descriptor.name.isBlank() || descriptor.type == null) {
            return false
        }
        return descriptor.supports(platformApi.runtimeMode)
    }

    private fun normalizeId(addonId: String?): String = addonId?.trim()?.lowercase() ?: ""

    private fun normalizeIds(ids: List<String>?): List<String> {
        return ids?.asSequence()
            ?.filter { id -> id.isNotBlank() }
            ?.map { id -> normalizeId(id) }
            ?.distinct()
            ?.toList()
            ?: listOf()
    }
}
