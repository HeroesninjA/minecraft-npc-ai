package ro.ainpc.mcp.bridge;

import java.util.List;
import java.util.Map;

public class RuntimeSnapshot {
    private int schemaVersion = 1;
    private String timestamp;
    private PluginSnapshot plugin;
    private FeatureSnapshot features;
    private NpcSnapshot npc;
    private WorldSnapshot world;
    private QuestSnapshot quests;

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int v) { this.schemaVersion = v; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String v) { this.timestamp = v; }
    public PluginSnapshot getPlugin() { return plugin; }
    public void setPlugin(PluginSnapshot v) { this.plugin = v; }
    public FeatureSnapshot getFeatures() { return features; }
    public void setFeatures(FeatureSnapshot v) { this.features = v; }
    public NpcSnapshot getNpc() { return npc; }
    public void setNpc(NpcSnapshot v) { this.npc = v; }
    public WorldSnapshot getWorld() { return world; }
    public void setWorld(WorldSnapshot v) { this.world = v; }
    public QuestSnapshot getQuests() { return quests; }
    public void setQuests(QuestSnapshot v) { this.quests = v; }

    public static class PluginSnapshot {
        private String version;
        private String serverType;
        private int onlinePlayers;
        private long uptimeMinutes;

        public String getVersion() { return version; }
        public void setVersion(String v) { this.version = v; }
        public String getServerType() { return serverType; }
        public void setServerType(String v) { this.serverType = v; }
        public int getOnlinePlayers() { return onlinePlayers; }
        public void setOnlinePlayers(int v) { this.onlinePlayers = v; }
        public long getUptimeMinutes() { return uptimeMinutes; }
        public void setUptimeMinutes(long v) { this.uptimeMinutes = v; }
    }

    public static class FeatureSnapshot {
        private boolean ai;
        private boolean mcp;
        private boolean aiOrchestration;

        public boolean isAi() { return ai; }
        public void setAi(boolean v) { this.ai = v; }
        public boolean isMcp() { return mcp; }
        public void setMcp(boolean v) { this.mcp = v; }
        public boolean isAiOrchestration() { return aiOrchestration; }
        public void setAiOrchestration(boolean v) { this.aiOrchestration = v; }
    }

    public static class NpcSnapshot {
        private int totalCount;
        private Map<String, Integer> byRegion;
        private List<NpcSample> samples;

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int v) { this.totalCount = v; }
        public Map<String, Integer> getByRegion() { return byRegion; }
        public void setByRegion(Map<String, Integer> v) { this.byRegion = v; }
        public List<NpcSample> getSamples() { return samples; }
        public void setSamples(List<NpcSample> v) { this.samples = v; }
    }

    public static class NpcSample {
        private int npcId;
        private String name;
        private String profession;
        private String regionId;
        private String worldName;

        public int getNpcId() { return npcId; }
        public void setNpcId(int v) { this.npcId = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getProfession() { return profession; }
        public void setProfession(String v) { this.profession = v; }
        public String getRegionId() { return regionId; }
        public void setRegionId(String v) { this.regionId = v; }
        public String getWorldName() { return worldName; }
        public void setWorldName(String v) { this.worldName = v; }
    }

    public static class WorldSnapshot {
        private int regionCount;
        private int placeCount;
        private int nodeCount;
        private List<String> regionNames;
        private List<String> placeNames;

        public int getRegionCount() { return regionCount; }
        public void setRegionCount(int v) { this.regionCount = v; }
        public int getPlaceCount() { return placeCount; }
        public void setPlaceCount(int v) { this.placeCount = v; }
        public int getNodeCount() { return nodeCount; }
        public void setNodeCount(int v) { this.nodeCount = v; }
        public List<String> getRegionNames() { return regionNames; }
        public void setRegionNames(List<String> v) { this.regionNames = v; }
        public List<String> getPlaceNames() { return placeNames; }
        public void setPlaceNames(List<String> v) { this.placeNames = v; }
    }

    public static class QuestSnapshot {
        private int activePlayerQuests;
        private int activeGlobalQuests;
        private List<QuestSample> samples;

        public int getActivePlayerQuests() { return activePlayerQuests; }
        public void setActivePlayerQuests(int v) { this.activePlayerQuests = v; }
        public int getActiveGlobalQuests() { return activeGlobalQuests; }
        public void setActiveGlobalQuests(int v) { this.activeGlobalQuests = v; }
        public List<QuestSample> getSamples() { return samples; }
        public void setSamples(List<QuestSample> v) { this.samples = v; }
    }

    public static class QuestSample {
        private String templateId;
        private String mechanic;
        private String playerUuid;
        private String status;

        public String getTemplateId() { return templateId; }
        public void setTemplateId(String v) { this.templateId = v; }
        public String getMechanic() { return mechanic; }
        public void setMechanic(String v) { this.mechanic = v; }
        public String getPlayerUuid() { return playerUuid; }
        public void setPlayerUuid(String v) { this.playerUuid = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
    }
}
