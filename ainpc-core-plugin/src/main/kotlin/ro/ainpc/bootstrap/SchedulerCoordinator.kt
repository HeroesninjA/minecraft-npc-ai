package ro.ainpc.bootstrap

import ro.ainpc.AINPCPlugin

class SchedulerCoordinator(
    private val plugin: AINPCPlugin
) {
    fun start() {
        scheduleInitialNpcRestore()
        scheduleLifeSimulation()
        scheduleRoutine()
        scheduleEmotionDecay()
        scheduleMemoryCleanup()
        scheduleNpcStatePersistence()
        scheduleVillageRebalance()
        scheduleQuestTracking()
    }

    private fun scheduleInitialNpcRestore() {
        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                plugin.npcManager.discoverExistingVillagers()
                plugin.npcManager.reconcileDuplicateLiveNPCEntities("initial delayed restore")
                plugin.npcManager.restoreMissingNPCsInLoadedChunks()
                plugin.npcManager.enforceControlledEntitySettings("initial delayed restore")
                val ensuredProfiles = plugin.npcManager.ensureAllNPCsHaveProfiles()
                if (ensuredProfiles > 0) {
                    plugin.logger.info("Profiluri NPC create dupa restaurarea villagerilor: $ensuredProfiles")
                }
                plugin.npcManager.rebalanceLoadedVillages()
            },
            20L
        )
    }

    private fun scheduleLifeSimulation() {
        val simulationTickSeconds = maxOf(10, plugin.config.getInt("simulation.tick_seconds", 30))

        if (featureEnabled("features.simulation", false) && plugin.config.getBoolean("simulation.enabled", false)) {
            plugin.server.scheduler.runTaskTimer(
                plugin,
                Runnable { plugin.npcManager.runLifeSimulationTick() },
                20L * 15,
                20L * simulationTickSeconds
            )
        }
    }

    private fun scheduleRoutine() {
        val routineTickSeconds = maxOf(20, plugin.config.getInt("routine.tick_seconds", 60))

        if (featureEnabled("features.routine", false) && plugin.config.getBoolean("routine.enabled", false)) {
            plugin.server.scheduler.runTaskTimer(
                plugin,
                Runnable {
                    val summary = plugin.routineService.runRoutineTick()
                    if (plugin.config.getBoolean("debug")) {
                        plugin.logger.info(
                            "[Debug] Routine tick: evaluated=" + summary.evaluatedNpcs() +
                                ", moved=" + summary.movedNpcs() +
                                ", busy=" + summary.skippedBusy() +
                                ", missingTarget=" + summary.skippedMissingTarget() +
                                ", invalidTarget=" + summary.skippedInvalidTarget()
                        )
                    }
                },
                20L * 20,
                20L * routineTickSeconds
            )
        }
    }

    private fun scheduleEmotionDecay() {
        plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { plugin.emotionManager.decayEmotions() },
            20L * 60,
            20L * 60
        )
    }

    private fun scheduleMemoryCleanup() {
        plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { plugin.databaseManager.runAsync { plugin.memoryManager.cleanOldMemories() } },
            20L * 60 * 60,
            20L * 60 * 60
        )
    }

    private fun scheduleNpcStatePersistence() {
        plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable {
                plugin.npcManager.syncAllNPCEntityState()
                plugin.databaseManager.runAsync {
                    plugin.npcManager.saveAllNPCs(false)
                    if (plugin.config.getBoolean("debug")) {
                        plugin.logger.info("[Debug] Salvare automata completata.")
                    }
                }
            },
            20L * 60 * 5,
            20L * 60 * 5
        )
    }

    private fun scheduleVillageRebalance() {
        plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { plugin.npcManager.rebalanceLoadedVillages() },
            20L * 45,
            20L * 120
        )
    }

    private fun scheduleQuestTracking() {
        if (!featureEnabled("features.quest", true)) {
            return
        }
        val refreshSeconds = maxOf(2, plugin.config.getInt("quest.tracking_refresh_seconds", 5))
        plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable {
                val updated = plugin.scenarioEngine.tickQuestTrackingMarkers()
                if (updated > 0 && plugin.config.getBoolean("debug")) {
                    plugin.logger.info("[Debug] Quest tracking refresh: updated=$updated")
                }
            },
            20L * refreshSeconds,
            20L * refreshSeconds
        )
    }

    private fun featureEnabled(path: String, defaultValue: Boolean): Boolean =
        plugin.config.getBoolean(path, defaultValue)
}
