package ro.ainpc.bootstrap;

import ro.ainpc.AINPCPlugin;

public class SchedulerCoordinator {

    private final AINPCPlugin plugin;

    public SchedulerCoordinator(AINPCPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        scheduleInitialNpcRestore();
        scheduleLifeSimulation();
        scheduleRoutine();
        scheduleEmotionDecay();
        scheduleMemoryCleanup();
        scheduleNpcStatePersistence();
        scheduleVillageRebalance();
        scheduleQuestTracking();
    }

    private void scheduleInitialNpcRestore() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getNpcManager().discoverExistingVillagers();
            plugin.getNpcManager().reconcileDuplicateLiveNPCEntities("initial delayed restore");
            plugin.getNpcManager().restoreMissingNPCsInLoadedChunks();
            plugin.getNpcManager().enforceControlledEntitySettings("initial delayed restore");
            int ensuredProfiles = plugin.getNpcManager().ensureAllNPCsHaveProfiles();
            if (ensuredProfiles > 0) {
                plugin.getLogger().info("Profiluri NPC create dupa restaurarea villagerilor: " + ensuredProfiles);
            }
            plugin.getNpcManager().rebalanceLoadedVillages();
        }, 20L);
    }

    private void scheduleLifeSimulation() {
        int simulationTickSeconds = Math.max(10, plugin.getConfig().getInt("simulation.tick_seconds", 30));

        if (plugin.getConfig().getBoolean("simulation.enabled", true)) {
            plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                plugin.getNpcManager().runLifeSimulationTick();
            }, 20L * 15, 20L * simulationTickSeconds);
        }
    }

    private void scheduleRoutine() {
        int routineTickSeconds = Math.max(20, plugin.getConfig().getInt("routine.tick_seconds", 60));

        if (plugin.getConfig().getBoolean("routine.enabled", true)) {
            plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                var summary = plugin.getRoutineService().runRoutineTick();
                if (plugin.getConfig().getBoolean("debug")) {
                    plugin.getLogger().info("[Debug] Routine tick: evaluated=" + summary.evaluatedNpcs()
                        + ", moved=" + summary.movedNpcs()
                        + ", busy=" + summary.skippedBusy()
                        + ", missingTarget=" + summary.skippedMissingTarget()
                        + ", invalidTarget=" + summary.skippedInvalidTarget());
                }
            }, 20L * 20, 20L * routineTickSeconds);
        }
    }

    private void scheduleEmotionDecay() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            plugin.getEmotionManager().decayEmotions();
        }, 20L * 60, 20L * 60);
    }

    private void scheduleMemoryCleanup() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            plugin.getDatabaseManager().runAsync(() -> plugin.getMemoryManager().cleanOldMemories());
        }, 20L * 60 * 60, 20L * 60 * 60);
    }

    private void scheduleNpcStatePersistence() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            plugin.getNpcManager().syncAllNPCEntityState();
            plugin.getDatabaseManager().runAsync(() -> {
                plugin.getNpcManager().saveAllNPCs(false);
                if (plugin.getConfig().getBoolean("debug")) {
                    plugin.getLogger().info("[Debug] Salvare automata completata.");
                }
            });
        }, 20L * 60 * 5, 20L * 60 * 5);
    }

    private void scheduleVillageRebalance() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            plugin.getNpcManager().rebalanceLoadedVillages();
        }, 20L * 45, 20L * 120);
    }

    private void scheduleQuestTracking() {
        int refreshSeconds = Math.max(2, plugin.getConfig().getInt("quest.tracking_refresh_seconds", 5));
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int updated = plugin.getScenarioEngine().tickQuestTrackingMarkers();
            if (updated > 0 && plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info("[Debug] Quest tracking refresh: updated=" + updated);
            }
        }, 20L * refreshSeconds, 20L * refreshSeconds);
    }
}
