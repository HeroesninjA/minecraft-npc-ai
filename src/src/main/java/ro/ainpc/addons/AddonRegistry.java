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

    public AddonRegistry(AINPCPlatformApi platformApi) {
        this.platformApi = platformApi;
        this.descriptorsById = new LinkedHashMap<>();
        this.addonsById = new LinkedHashMap<>();
        this.descriptorsByType = new EnumMap<>(AddonType.class);
    }

    @Override
    public synchronized void registerDescriptor(AddonDescriptor descriptor) {
        if (descriptor == null) {
            return;
        }

        AddonDescriptor previous = descriptorsById.put(descriptor.getId(), descriptor);
        if (previous != null) {
            removeDescriptorFromType(previous);
        }

        descriptorsByType
            .computeIfAbsent(descriptor.getType(), ignored -> new ArrayList<>())
            .add(descriptor);
        descriptorsByType.get(descriptor.getType()).sort(Comparator.comparing(AddonDescriptor::getId));
    }

    @Override
    public synchronized void registerAddon(AINPCAddon addon) {
        if (addon == null || addon.getDescriptor() == null) {
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
        return Collections.unmodifiableCollection(new ArrayList<>(descriptorsById.values()));
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
        return descriptorsById.values().stream()
            .filter(descriptor -> descriptor.getType() == AddonType.SCENARIO && descriptor.isPrimaryScenario())
            .findFirst()
            .orElseGet(() -> descriptorsById.values().stream()
                .filter(descriptor -> descriptor.getType() == AddonType.SCENARIO)
                .findFirst()
                .orElse(null));
    }

    @Override
    public synchronized int size() {
        return descriptorsById.size();
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
}
