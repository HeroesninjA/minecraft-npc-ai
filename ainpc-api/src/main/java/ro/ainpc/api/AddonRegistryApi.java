package ro.ainpc.api;

import ro.ainpc.addons.AINPCAddon;
import ro.ainpc.addons.AddonDescriptor;
import ro.ainpc.addons.AddonType;

import java.util.Collection;
import java.util.List;

public interface AddonRegistryApi {

    void registerDescriptor(AddonDescriptor descriptor);

    void registerAddon(AINPCAddon addon);

    void unregisterAddon(String addonId);

    void removeByOrigin(String origin);

    Collection<AddonDescriptor> getDescriptors();

    List<AddonDescriptor> getDescriptors(AddonType type);

    AddonDescriptor getDescriptor(String id);

    AddonDescriptor getPrimaryScenario();

    default boolean isAddonEnabled(String addonId) {
        return true;
    }

    int size();
}
