package ro.ainpc.addons;

import ro.ainpc.api.AINPCPlatformApi;

public interface AINPCAddon {

    AddonDescriptor getDescriptor();

    default void onLoad(AINPCPlatformApi api) {
    }

    default void onEnable(AINPCPlatformApi api) {
    }

    default void onDisable(AINPCPlatformApi api) {
    }
}
