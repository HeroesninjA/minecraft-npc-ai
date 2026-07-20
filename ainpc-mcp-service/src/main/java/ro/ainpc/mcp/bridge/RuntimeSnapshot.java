package ro.ainpc.mcp.bridge;

import java.util.List;
import java.util.Map;

public class RuntimeSnapshot {
    private int schemaVersion = 3;
    private String timestamp;
    private PluginSnapshot plugin;
    private FeatureSnapshot features;
    private NpcSnapshot npc;
    private WorldSnapshot world;
    private QuestSnapshot quests;
    private BuildModeSnapshot buildMode;
    private HealthSnapshot health;

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
    public BuildModeSnapshot getBuildMode() { return buildMode; }
    public void setBuildMode(BuildModeSnapshot v) { this.buildMode = v; }
    public HealthSnapshot getHealth() { return health; }
    public void setHealth(HealthSnapshot v) { this.health = v; }

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
        private int relationshipCount;
        private int economyNpcCount;
        private int economyTotalValue;
        private int socialGatherings;

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int v) { this.totalCount = v; }
        public Map<String, Integer> getByRegion() { return byRegion; }
        public void setByRegion(Map<String, Integer> v) { this.byRegion = v; }
        public List<NpcSample> getSamples() { return samples; }
        public void setSamples(List<NpcSample> v) { this.samples = v; }
        public int getRelationshipCount() { return relationshipCount; }
        public void setRelationshipCount(int v) { this.relationshipCount = v; }
        public int getEconomyNpcCount() { return economyNpcCount; }
        public void setEconomyNpcCount(int v) { this.economyNpcCount = v; }
        public int getEconomyTotalValue() { return economyTotalValue; }
        public void setEconomyTotalValue(int v) { this.economyTotalValue = v; }
        public int getSocialGatherings() { return socialGatherings; }
        public void setSocialGatherings(int v) { this.socialGatherings = v; }
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
        private int storyEventCount;
        private List<String> recentStoryEvents;

        public int getActivePlayerQuests() { return activePlayerQuests; }
        public void setActivePlayerQuests(int v) { this.activePlayerQuests = v; }
        public int getActiveGlobalQuests() { return activeGlobalQuests; }
        public void setActiveGlobalQuests(int v) { this.activeGlobalQuests = v; }
        public List<QuestSample> getSamples() { return samples; }
        public void setSamples(List<QuestSample> v) { this.samples = v; }
        public int getStoryEventCount() { return storyEventCount; }
        public void setStoryEventCount(int v) { this.storyEventCount = v; }
        public List<String> getRecentStoryEvents() { return recentStoryEvents; }
        public void setRecentStoryEvents(List<String> v) { this.recentStoryEvents = v; }
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

    public static class BuildModeSnapshot {
        private int activePlayers;
        private Map<String, Integer> byStyle;
        private Map<String, Integer> byTarget;
        private List<BuildModePlayerSnapshot> players;
        private List<BuildModeHistorySnapshot> history;

        public int getActivePlayers() { return activePlayers; }
        public void setActivePlayers(int v) { this.activePlayers = v; }
        public Map<String, Integer> getByStyle() { return byStyle; }
        public void setByStyle(Map<String, Integer> v) { this.byStyle = v; }
        public Map<String, Integer> getByTarget() { return byTarget; }
        public void setByTarget(Map<String, Integer> v) { this.byTarget = v; }
        public List<BuildModePlayerSnapshot> getPlayers() { return players; }
        public void setPlayers(List<BuildModePlayerSnapshot> v) { this.players = v; }
        public List<BuildModeHistorySnapshot> getHistory() { return history; }
        public void setHistory(List<BuildModeHistorySnapshot> v) { this.history = v; }
    }

    public static class BuildModePlayerSnapshot {
        private String name;
        private String style;
        private String target;

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getStyle() { return style; }
        public void setStyle(String v) { this.style = v; }
        public String getTarget() { return target; }
        public void setTarget(String v) { this.target = v; }
    }

    public static class BuildModeHistorySnapshot {
        private String playerName;
        private long timestampMillis;
        private String action;
        private String style;
        private String target;

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String v) { this.playerName = v; }
        public long getTimestampMillis() { return timestampMillis; }
        public void setTimestampMillis(long v) { this.timestampMillis = v; }
        public String getAction() { return action; }
        public void setAction(String v) { this.action = v; }
        public String getStyle() { return style; }
        public void setStyle(String v) { this.style = v; }
        public String getTarget() { return target; }
        public void setTarget(String v) { this.target = v; }
    }

    public static class HealthSnapshot {
        private int schemaVersion;
        private String status;
        private String generatedAt;
        private int activeSeries;
        private long droppedMeasurements;
        private int maxSeries;
        private int samplesPerSeries;
        private Map<String, MetricBudgetSnapshot> budgets;
        private List<MetricSnapshot> metrics;

        public int getSchemaVersion() { return schemaVersion; }
        public void setSchemaVersion(int v) { this.schemaVersion = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public String getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(String v) { this.generatedAt = v; }
        public int getActiveSeries() { return activeSeries; }
        public void setActiveSeries(int v) { this.activeSeries = v; }
        public long getDroppedMeasurements() { return droppedMeasurements; }
        public void setDroppedMeasurements(long v) { this.droppedMeasurements = v; }
        public int getMaxSeries() { return maxSeries; }
        public void setMaxSeries(int v) { this.maxSeries = v; }
        public int getSamplesPerSeries() { return samplesPerSeries; }
        public void setSamplesPerSeries(int v) { this.samplesPerSeries = v; }
        public Map<String, MetricBudgetSnapshot> getBudgets() { return budgets; }
        public void setBudgets(Map<String, MetricBudgetSnapshot> v) { this.budgets = v; }
        public List<MetricSnapshot> getMetrics() { return metrics; }
        public void setMetrics(List<MetricSnapshot> v) { this.metrics = v; }
    }

    public static class MetricBudgetSnapshot {
        private double warnMillis;
        private double failMillis;

        public double getWarnMillis() { return warnMillis; }
        public void setWarnMillis(double v) { this.warnMillis = v; }
        public double getFailMillis() { return failMillis; }
        public void setFailMillis(double v) { this.failMillis = v; }
    }

    public static class MetricSnapshot {
        private String name;
        private String domain;
        private String status;
        private long totalCount;
        private int windowSamples;
        private int windowFailures;
        private double lastDurationMillis;
        private double avgDurationMillis;
        private double maxDurationMillis;
        private String lastObservedAt;
        private int itemCount;
        private double warnBudgetMillis;
        private double failBudgetMillis;

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getDomain() { return domain; }
        public void setDomain(String v) { this.domain = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long v) { this.totalCount = v; }
        public int getWindowSamples() { return windowSamples; }
        public void setWindowSamples(int v) { this.windowSamples = v; }
        public int getWindowFailures() { return windowFailures; }
        public void setWindowFailures(int v) { this.windowFailures = v; }
        public double getLastDurationMillis() { return lastDurationMillis; }
        public void setLastDurationMillis(double v) { this.lastDurationMillis = v; }
        public double getAvgDurationMillis() { return avgDurationMillis; }
        public void setAvgDurationMillis(double v) { this.avgDurationMillis = v; }
        public double getMaxDurationMillis() { return maxDurationMillis; }
        public void setMaxDurationMillis(double v) { this.maxDurationMillis = v; }
        public String getLastObservedAt() { return lastObservedAt; }
        public void setLastObservedAt(String v) { this.lastObservedAt = v; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int v) { this.itemCount = v; }
        public double getWarnBudgetMillis() { return warnBudgetMillis; }
        public void setWarnBudgetMillis(double v) { this.warnBudgetMillis = v; }
        public double getFailBudgetMillis() { return failBudgetMillis; }
        public void setFailBudgetMillis(double v) { this.failBudgetMillis = v; }
    }
}
