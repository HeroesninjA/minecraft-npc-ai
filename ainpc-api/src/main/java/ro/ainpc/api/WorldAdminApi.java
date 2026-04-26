package ro.ainpc.api;

import ro.ainpc.world.WorldMode;

public interface WorldAdminApi {

    boolean isEnabled();

    WorldMode getWorldMode();

    int getRegionCount();

    int getNodeCount();
}
