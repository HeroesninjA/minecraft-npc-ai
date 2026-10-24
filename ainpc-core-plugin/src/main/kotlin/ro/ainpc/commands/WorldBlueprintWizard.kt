package ro.ainpc.commands

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.world.WorldBlueprint
import ro.ainpc.world.WorldBlueprintStore
import ro.ainpc.world.WorldBlueprintPolicy
import ro.ainpc.world.WorldBlueprintRevisionConflictException
import ro.ainpc.world.WorldBlueprintWizardFlow
import ro.ainpc.world.WorldBlueprintWizardStep
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

internal object WorldBlueprintWizard {
    private const val TIMEOUT_MILLIS = 10 * 60 * 1_000L
    private const val MAX_SESSIONS = 16
    private val sessions = ConcurrentHashMap<UUID, Session>()

    private data class Session(
        val worldName: String,
        val step: WorldBlueprintWizardStep,
        val touchedAt: Long,
        val editOnly: Boolean,
        val fingerprint: String?,
    )

    fun hasSession(playerId: UUID): Boolean = sessions.containsKey(playerId)

    fun start(plugin: AINPCPlugin, player: Player, worldName: String, blueprint: WorldBlueprint?) {
        val step = WorldBlueprintWizardFlow.firstMissing(blueprint ?: WorldBlueprint(worldName))
        if (step == WorldBlueprintWizardStep.REVIEW) {
            sessions.remove(player.uniqueId)
            plugin.messageUtils.send(player, "&aDraftul este complet. Foloseste show, inspect, validate, plan si review pentru revizie.")
            return
        }
        begin(plugin, player, worldName, blueprint, step, false)
    }

    fun startEdit(
        plugin: AINPCPlugin, player: Player, worldName: String,
        blueprint: WorldBlueprint, step: WorldBlueprintWizardStep,
    ) {
        begin(plugin, player, worldName, blueprint, step, true)
    }

    private fun begin(
        plugin: AINPCPlugin, player: Player, worldName: String,
        blueprint: WorldBlueprint?, step: WorldBlueprintWizardStep, editOnly: Boolean,
    ) {
        if (plugin.guiService.hasTextInputRequest(player)) {
            plugin.messageUtils.send(player, "&cInchide mai intai introducerea de text din GUI.")
            return
        }
        val now = System.currentTimeMillis()
        sessions.entries.removeIf { now - it.value.touchedAt > TIMEOUT_MILLIS }
        if (!sessions.containsKey(player.uniqueId) && sessions.size >= MAX_SESSIONS) {
            plugin.messageUtils.send(player, "&cPrea multe wizard-uri active; incearca mai tarziu.")
            return
        }
        val store = WorldBlueprintStore(plugin.dataFolder.toPath().resolve("world-blueprints.json"))
        sessions[player.uniqueId] = Session(worldName, step, now, editOnly, blueprint?.let(store::fingerprint))
        val mode = if (editOnly) "Editare ghidata" else "Wizard"
        plugin.messageUtils.send(player, "&6$mode pentru ${worldName}. Raspunsurile din chat sunt private; scrie anuleaza pentru iesire.")
        if (editOnly) {
            plugin.messageUtils.send(player, "&7Valoare curenta: ${WorldBlueprintWizardFlow.currentSummary(requireNotNull(blueprint), step)}")
            plugin.messageUtils.send(player, "&7Pentru textul complet foloseste inspect; o editare schimba amprenta din review.")
        }
        plugin.messageUtils.send(player, "&e${WorldBlueprintWizardFlow.prompt(step)}")
    }

    fun stop(plugin: AINPCPlugin, player: Player) {
        val removed = sessions.remove(player.uniqueId)
        plugin.messageUtils.send(player, if (removed == null) "&7Nu ai un wizard activ."
            else "&7Wizard oprit; modificarile deja salvate raman in draft.")
    }

    fun clear(playerId: UUID) {
        sessions.remove(playerId)
    }

    fun consume(plugin: AINPCPlugin, player: Player, message: String) {
        val session = sessions[player.uniqueId] ?: return
        if (message.trim().equals("anuleaza", ignoreCase = true)) {
            stop(plugin, player)
            return
        }
        if (System.currentTimeMillis() - session.touchedAt > TIMEOUT_MILLIS) {
            sessions.remove(player.uniqueId)
            plugin.messageUtils.send(player, "&eWizard expirat; porneste-l din nou. Draftul a fost pastrat.")
            return
        }
        if (!player.hasPermission("ainpc.admin") || isRuntimeReadOnly(plugin)) {
            sessions.remove(player.uniqueId)
            plugin.messageUtils.send(player, "&cWizard oprit: permisiunea sau dreptul de scriere nu mai este disponibil.")
            return
        }
        sessions[player.uniqueId] = session.copy(touchedAt = System.currentTimeMillis())
        try {
            val store = WorldBlueprintStore(plugin.dataFolder.toPath().resolve("world-blueprints.json"))
            val progress = store.advanceWizard(session.worldName, session.step, message, session.fingerprint)
            val update = progress.update
            if (session.editOnly && update.nextStep != session.step) {
                sessions.remove(player.uniqueId)
                plugin.messageUtils.send(player, "&aEditarea categoriei s-a incheiat. Verifica /ainpc world blueprint ${session.worldName} review.")
                return
            }
            val nextStep = if (update.nextStep == WorldBlueprintWizardStep.REVIEW &&
                WorldBlueprintPolicy.completionIssues(update.blueprint).isNotEmpty()
            ) WorldBlueprintWizardFlow.firstMissing(update.blueprint) else update.nextStep
            if (nextStep == WorldBlueprintWizardStep.REVIEW) {
                sessions.remove(player.uniqueId)
                plugin.messageUtils.send(player, "&aDraft complet pentru revizie, nu este aplicat in lume.")
                plugin.messageUtils.send(player, "&7/ainpc world blueprint ${session.worldName} validate")
                plugin.messageUtils.send(player, "&7/ainpc world blueprint ${session.worldName} plan")
                plugin.messageUtils.send(player, "&7/ainpc world blueprint ${session.worldName} review")
            } else {
                sessions[player.uniqueId] = session.copy(step = nextStep, touchedAt = System.currentTimeMillis(),
                    fingerprint = progress.fingerprint)
                val status = if (progress.fingerprint == session.fingerprint) "Pas urmator." else "Raspuns salvat in draft."
                plugin.messageUtils.send(player, "&a$status &e${WorldBlueprintWizardFlow.prompt(nextStep)}")
            }
        } catch (error: WorldBlueprintRevisionConflictException) {
            sessions.remove(player.uniqueId)
            plugin.messageUtils.send(player, "&eWizard oprit: ${error.message} Raspunsul nu a fost aplicat.")
            plugin.messageUtils.send(player, "&7Revizuieste /ainpc world blueprint ${session.worldName} suggest gui si porneste din nou editarea.")
        } catch (error: IllegalArgumentException) {
            plugin.messageUtils.send(player, "&c${error.message} &e${WorldBlueprintWizardFlow.prompt(session.step)}")
        } catch (error: IOException) {
            plugin.logger.log(Level.WARNING, "Nu am putut salva world blueprint wizard pentru ${session.worldName}.", error)
            plugin.messageUtils.send(player, "&cDraftul nu a fost modificat: ${error.message}")
        }
    }
}
