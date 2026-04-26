package ro.ainpc.api;

import ro.ainpc.platform.RuntimeMode;
import ro.ainpc.world.StoryMode;
import ro.ainpc.world.WorldMode;

import java.nio.file.Path;

public interface AINPCPlatformApi {

    RuntimeMode getRuntimeMode();

    WorldMode getWorldMode();

    StoryMode getDefaultStoryMode();

    AddonRegistryApi getAddonRegistry();

    WorldAdminApi getWorldAdmin();

    Path getDataDirectory();

    Path getPackDirectory();

    void reloadContent();
}
