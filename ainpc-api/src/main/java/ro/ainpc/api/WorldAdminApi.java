package ro.ainpc.api;

import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;
import ro.ainpc.world.WorldMode;

import java.util.Collection;

public interface WorldAdminApi {

    boolean isEnabled();

    WorldMode getWorldMode();

    Collection<WorldRegionInfo> getRegions();

    WorldRegionInfo getRegion(String regionId);

    WorldRegionInfo findRegion(String worldName, int x, int y, int z);

    Collection<WorldPlaceInfo> getPlaces();

    Collection<WorldPlaceInfo> getPlaces(String regionId);

    WorldPlaceInfo getPlace(String placeId);

    WorldPlaceInfo findPlace(String worldName, int x, int y, int z);

    Collection<WorldPlaceInfo> findPlacesByTag(String regionId, String tag);

    Collection<WorldNodeInfo> getNodes();

    Collection<WorldNodeInfo> getNodes(String regionId);

    Collection<WorldNodeInfo> getNodesForPlace(String placeId);

    WorldNodeInfo getNode(String nodeId);

    WorldNodeInfo findNode(String worldName, int x, int y, int z);

    Collection<WorldNodeInfo> findNodesNear(String worldName, double x, double y, double z, double radius, int limit);

    int getRegionCount();

    int getPlaceCount();

    int getNodeCount();
}
