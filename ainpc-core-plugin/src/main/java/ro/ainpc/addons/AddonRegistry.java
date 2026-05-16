package ro.ainpc.addons;

import ro.ainpc.api.AINPCPlatformApi;
import ro.ainpc.api.AddonRegistryApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AddonRegistry implements AddonRegistryApi {

    private final AINPCPlatformApi platformApi;

    private final Map<String, AddonDescriptor> descriptorsById;
    private final Map<String, AINPCAddon> addonsById;
    private final Map<AddonType, List<AddonDescriptor>> descriptorsByType;
    private boolean registryEnabled = true;
    private boolean strictValidation = true;
    private List<String> disabledAddonIds = List.of();
    private List<String> loadOrderIds = List.of();

    public AddonRegistry(AINPCPlatformApi platformApi) {
        this.platformApi = platformApi;
        this.descriptorsById = new LinkedHashMap<>();
        this.addonsById = new LinkedHashMap<>();
        this.descriptorsByType = new EnumMap<>(AddonType.class);
    }

    public synchronized void configure(boolean enabled, boolean strictValidation, List<String> disabledAddonIds) {
        configure(enabled, strictValidation, disabledAddonIds, List.of());
    }

    public synchronized void configure(boolean enabled,
                                       boolean strictValidation,
                                       List<String> disabledAddonIds,
                                       List<String> loadOrderIds) {
        this.registryEnabled = enabled;
        this.strictValidation = strictValidation;
        this.disabledAddonIds = normalizeIds(disabledAddonIds);
        this.loadOrderIds = normalizeIds(loadOrderIds);

        List<String> idsToRemove = descriptorsById.keySet().stream()
            .filter(id -> !isAddonEnabled(id))
            .toList();
        for (String id : idsToRemove) {
            unregisterAddon(id);
        }
        sortDescriptorLists();
    }

    @Override
    public synchronized void registerDescriptor(AddonDescriptor descriptor) {
        if (!canRegister(descriptor)) {
            return;
        }

        AddonDescriptor previous = descriptorsById.put(descriptor.getId(), descriptor);
        if (previous != null) {
            removeDescriptorFromType(previous);
        }

        descriptorsByType
            .computeIfAbsent(descriptor.getType(), ignored -> new ArrayList<>())
            .add(descriptor);
        descriptorsByType.get(descriptor.getType()).sort(descriptorComparator());
    }

    @Override
    public synchronized void registerAddon(AINPCAddon addon) {
        if (addon == null || addon.getDescriptor() == null) {
            return;
        }
        if (!canRegister(addon.getDescriptor())) {
            return;
        }

        unregisterAddon(addon.getDescriptor().getId());
        addon.onLoad(platformApi);
        registerDescriptor(addon.getDescriptor());
        addonsById.put(addon.getDescriptor().getId(), addon);
        addon.onEnable(platformApi);
    }

    @Override
    public synchronized void unregisterAddon(String addonId) {
        if (addonId == null || addonId.isBlank()) {
            return;
        }

        AINPCAddon addon = addonsById.remove(addonId);
        if (addon != null) {
            addon.onDisable(platformApi);
        }

        AddonDescriptor removed = descriptorsById.remove(addonId);
        if (removed != null) {
            removeDescriptorFromType(removed);
        }
    }

    @Override
    public synchronized void removeByOrigin(String origin) {
        List<String> idsToRemove = descriptorsById.values().stream()
            .filter(descriptor -> descriptor.getOrigin().equalsIgnoreCase(origin))
            .map(AddonDescriptor::getId)
            .toList();

        for (String id : idsToRemove) {
            AddonDescriptor removed = descriptorsById.remove(id);
            if (removed != null) {
                removeDescriptorFromType(removed);
            }
            addonsById.remove(id);
        }
    }

    @Override
    public synchronized Collection<AddonDescriptor> getDescriptors() {
        return Collections.unmodifiableCollection(sortedDescriptors());
    }

    @Override
    public synchronized List<AddonDescriptor> getDescriptors(AddonType type) {
        return Collections.unmodifiableList(
            new ArrayList<>(descriptorsByType.getOrDefault(type, Collections.emptyList()))
        );
    }

    @Override
    public synchronized AddonDescriptor getDescriptor(String id) {
        return descriptorsById.get(id);
    }

    @Override
    public synchronized AddonDescriptor getPrimaryScenario() {
        List<AddonDescriptor> descriptors = sortedDescriptors();
        return descriptors.stream()
            .filter(descriptor -> descriptor.getType() == AddonType.SCENARIO && descriptor.isPrimaryScenario())
            .findFirst()
            .orElseGet(() -> descriptors.stream()
                .filter(descriptor -> descriptor.getType() == AddonType.SCENARIO)
                .findFirst()
                .orElse(null));
    }

    @Override
    public synchronized int size() {
        return descriptorsById.size();
    }

    @Override
    public synchronized boolean isAddonEnabled(String addonId) {
        String normalizedId = normalizeId(addonId);
        if (normalizedId.isBlank()) {
            return false;
        }
        if (AddonDescriptor.ORIGIN_CORE.equals(normalizedId) || "ainpc-core".equals(normalizedId)) {
            return true;
        }
        return registryEnabled && !disabledAddonIds.contains(normalizedId);
    }

    public synchronized void shutdown() {
        List<AINPCAddon> addons = new ArrayList<>(addonsById.values());
        Collections.reverse(addons);
        for (AINPCAddon addon : addons) {
            addon.onDisable(platformApi);
        }
        addonsById.clear();
        descriptorsById.clear();
        descriptorsByType.clear();
    }

    private void removeDescriptorFromType(AddonDescriptor descriptor) {
        List<AddonDescriptor> descriptors = descriptorsByType.get(descriptor.getType());
        if (descriptors != null) {
            descriptors.removeIf(existing -> existing.getId().equals(descriptor.getId()));
            if (descriptors.isEmpty()) {
                descriptorsByType.remove(descriptor.getType());
            }
        }
    }

    private void sortDescriptorLists() {
        Comparator<AddonDescriptor> comparator = descriptorComparator();
        for (List<AddonDescriptor> descriptors : descriptorsByType.values()) {
            descriptors.sort(comparator);
        }
    }

    private List<AddonDescriptor> sortedDescriptors() {
        List<AddonDescriptor> descriptors = new ArrayList<>(descriptorsById.values());
        descriptors.sort(descriptorComparator());
        return descriptors;
    }

    private Comparator<AddonDescriptor> descriptorComparator() {
        return Comparator
            .comparingInt(this::loadOrderIndex)
            .thenComparing(AddonDescriptor::getId, String.CASE_INSENSITIVE_ORDER);
    }

    private int loadOrderIndex(AddonDescriptor descriptor) {
        if (descriptor == null) {
            return Integer.MAX_VALUE;
        }
        if (AddonDescriptor.ORIGIN_CORE.equalsIgnoreCase(descriptor.getOrigin())) {
            return -1;
        }
        int index = loadOrderIds.indexOf(normalizeId(descriptor.getId()));
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private boolean canRegister(AddonDescriptor descriptor) {
        if (descriptor == null) {
            return false;
        }
        if (AddonDescriptor.ORIGIN_CORE.equalsIgnoreCase(descriptor.getOrigin())) {
            return true;
        }
        if (!isAddonEnabled(descriptor.getId())) {
            return false;
        }
        if (!strictValidation) {
            return true;
        }
        if (descriptor.getId() == null || descriptor.getId().isBlank()
            || descriptor.getName() == null || descriptor.getName().isBlank()
            || descriptor.getType() == null) {
            return false;
        }
        return descriptor.supports(platformApi.getRuntimeMode());
    }

    private String normalizeId(String addonId) {
        return addonId == null ? "" : addonId.trim().toLowerCase();
    }

    private List<String> normalizeIds(List<String> ids) {
        return ids != null
            ? ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(this::normalizeId)
                .distinct()
                .toList()
            : List.of();
    }
}
