package ro.ainpc.addons.medieval;

import ro.ainpc.addons.AINPCAddon;
import ro.ainpc.addons.AddonDescriptor;
import ro.ainpc.addons.AddonType;
import ro.ainpc.platform.RuntimeMode;

import java.util.EnumSet;
import java.util.List;

public class MedievalScenarioAddon implements AINPCAddon {

    private final AddonDescriptor descriptor;

    public MedievalScenarioAddon(String version) {
        this.descriptor = new AddonDescriptor(
            AddonDescriptor.ORIGIN_PLUGIN_ADDON,
            "ainpc-scenario-medieval",
            "AINPC Scenario Medieval",
            version,
            "Addon plugin separat care livreaza pack-ul medieval_quest pentru core.",
            AddonType.FEATURE,
            false,
            EnumSet.allOf(RuntimeMode.class),
            List.of("scenario-pack", "pack-installer", "addon-config-template"),
            List.of("ainpc-core")
        );
    }

    @Override
    public AddonDescriptor getDescriptor() {
        return descriptor;
    }
}
