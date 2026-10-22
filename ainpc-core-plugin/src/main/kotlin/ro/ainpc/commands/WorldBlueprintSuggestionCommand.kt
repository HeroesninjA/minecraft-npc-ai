package ro.ainpc.commands

import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.platform.features.RuntimeFeatureKey
import ro.ainpc.world.WorldBlueprint
import ro.ainpc.world.WorldBlueprintPolicy
import ro.ainpc.world.WorldBlueprintStore
import ro.ainpc.world.WorldBlueprintSuggestion
import ro.ainpc.world.WorldBlueprintSuggestionField
import ro.ainpc.world.WorldBlueprintSuggestionParser
import ro.ainpc.world.WorldBlueprintSuggestionProvenance
import ro.ainpc.world.WorldBlueprintSuggestionRequest
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal object WorldBlueprintSuggestionCommand {
    private const val LIFETIME_MILLIS = 10 * 60 * 1_000L
    private const val MAX_PENDING = 16
    private val pending = ConcurrentHashMap<UUID, Pending>()
    private val previews = ConcurrentHashMap<UUID, WorldBlueprintSuggestion>()

    private data class Pending(val token: UUID, val worldName: String, val startedAt: Long, val discarded: Boolean = false)

    fun handle(
        plugin: AINPCPlugin, sender: CommandSender, worldName: String,
        current: WorldBlueprint?, store: WorldBlueprintStore, args: Array<String>,
    ): Boolean {
        val player = sender as? Player ?: run {
            plugin.messageUtils.send(sender, "&cSugestiile AI necesita un jucator administrator.")
            return true
        }
        val action = args.getOrNull(4)?.lowercase(Locale.ROOT) ?: return usage(plugin, sender)
        return when (action) {
            "show" -> if (args.size == 5) show(plugin, player, worldName, current, store) else usage(plugin, sender)
            "saved" -> if (args.size == 5) show(plugin, player, worldName, current, store, true) else usage(plugin, sender)
            "save" -> if (args.size == 6) save(plugin, player, worldName, store, args[5]) else usage(plugin, sender)
            "discard" -> if (args.size == 5) discard(plugin, player, worldName, store) else usage(plugin, sender)
            "accept" -> if (args.size == 6) accept(plugin, player, worldName, store, args[5]) else usage(plugin, sender)
            else -> generate(plugin, player, worldName, current, store, args)
        }
    }

    fun clear(playerId: UUID) {
        pending.remove(playerId)
        previews.remove(playerId)
    }

    private fun generate(
        plugin: AINPCPlugin, player: Player, worldName: String,
        current: WorldBlueprint?, store: WorldBlueprintStore, args: Array<String>,
    ): Boolean {
        val field = WorldBlueprintSuggestionField.fromId(args[4]) ?: return usage(plugin, player)
        val needsKey = field in setOf(WorldBlueprintSuggestionField.PROPERTY,
            WorldBlueprintSuggestionField.PROFESSION, WorldBlueprintSuggestionField.MAP_RULE)
        val key = if (needsKey) args.getOrNull(5)?.lowercase(Locale.ROOT) else null
        if (needsKey && (key == null || !WorldBlueprintPolicy.validKey(key))) {
            plugin.messageUtils.send(player, "&cCheia trebuie sa foloseasca litere mici, cifre, _ sau -.")
            return true
        }
        val instruction = args.drop(if (needsKey) 6 else 5).joinToString(" ").trim()
        if (instruction.isBlank() || instruction.length > 1_000 || instruction.any { it.isISOControl() }) {
            plugin.messageUtils.send(player, "&cDescrierea trebuie sa aiba 1..1000 caractere fara caractere de control.")
            return true
        }
        if (!plugin.platform.runtimeFeatures().enabled(RuntimeFeatureKey.AI) ||
            !plugin.openAIService.canGenerateContextualResponse
        ) {
            plugin.messageUtils.send(player, "&cFurnizorul AI nu este disponibil; draftul nu a fost modificat.")
            return true
        }
        val now = System.currentTimeMillis()
        pending.entries.removeIf { now - it.value.startedAt > LIFETIME_MILLIS }
        if (pending.containsKey(player.uniqueId)) {
            plugin.messageUtils.send(player, "&eAi deja o cerere AI in curs. Asteapta raspunsul inainte de alta cerere.")
            return true
        }
        if (pending.size >= MAX_PENDING) {
            plugin.messageUtils.send(player, "&cPrea multe cereri AI in curs; incearca mai tarziu.")
            return true
        }
        val provider = if (plugin.config.getString("ai_provider", "openai")?.equals("ollama", true) == true)
            "ollama" else "openai"
        val defaultModel = if (provider == "ollama") "llama3" else "gpt-5.4-nano"
        val model = plugin.config.getString("$provider.model", defaultModel) ?: defaultModel
        val request = WorldBlueprintSuggestionRequest(
            worldName, field, key, instruction, current?.let(store::fingerprint) ?: "none",
        )
        val provenance = WorldBlueprintSuggestionProvenance(
            provider, model.take(80), now, WorldBlueprintSuggestionParser.requestFingerprint(instruction),
        )
        val token = UUID.randomUUID()
        pending[player.uniqueId] = Pending(token, worldName, now)
        previews.remove(player.uniqueId)
        plugin.messageUtils.send(player, "&7Trimit descrierea la $provider/$model pentru o sugestie de ${field.id}; nimic nu va fi salvat automat.")
        try {
            plugin.openAIService.generateAsync(WorldBlueprintSuggestionParser.prompt(request)).whenComplete { response, error ->
                if (!plugin.isEnabled) return@whenComplete
                runCatching {
                    plugin.server.scheduler.runTask(plugin, Runnable {
                        finish(plugin, player, request, current ?: WorldBlueprint(worldName), provenance, token, response, error)
                    })
                }
            }
        } catch (_: RuntimeException) {
            pending.remove(player.uniqueId)
            plugin.messageUtils.send(player, "&cCererea AI nu a putut fi pornita; draftul nu a fost modificat.")
        }
        return true
    }

    private fun finish(
        plugin: AINPCPlugin, player: Player, request: WorldBlueprintSuggestionRequest,
        base: WorldBlueprint, provenance: WorldBlueprintSuggestionProvenance,
        token: UUID, response: String?, error: Throwable?,
    ) {
        val active = pending[player.uniqueId]?.takeIf { it.token == token } ?: return
        pending.remove(player.uniqueId)
        if (active.discarded) return
        if (!player.isOnline || !player.hasPermission("ainpc.admin") || plugin.server.getWorld(request.worldName) == null) return
        if (error != null || response.isNullOrBlank()) {
            plugin.messageUtils.send(player, "&cFurnizorul AI nu a returnat o sugestie; draftul nu a fost modificat.")
            return
        }
        val suggestion = try {
            WorldBlueprintSuggestionParser.parse(request, base, response, provenance)
        } catch (invalid: IllegalArgumentException) {
            plugin.messageUtils.send(player, "&cSugestia AI a fost respinsa: ${invalid.message}")
            return
        }
        previews[player.uniqueId] = suggestion
        plugin.messageUtils.send(player, "&aSugestie validata pentru ${request.field.id}; doar preview, nesalvata.")
        show(plugin, player, request.worldName, null,
            WorldBlueprintStore(plugin.dataFolder.toPath().resolve("world-blueprints.json")))
    }

    private fun show(
        plugin: AINPCPlugin, player: Player, worldName: String,
        current: WorldBlueprint?, store: WorldBlueprintStore, savedOnly: Boolean = false,
    ): Boolean {
        val preview = if (savedOnly) null else livePreview(player.uniqueId, worldName)
        val saved = try {
            if (preview == null) store.getSavedSuggestion(player.uniqueId, worldName) else null
        } catch (_: IOException) {
            plugin.messageUtils.send(player, "&cNu pot citi sugestia salvata; verifica fisierul de blueprint-uri.")
            return true
        }
        val suggestion = preview ?: saved?.suggestion
        if (suggestion == null) {
            val status = if (pending[player.uniqueId]?.worldName == worldName) "Cererea AI este inca in curs."
                else "Nu exista sugestie valida pentru aceasta lume."
            plugin.messageUtils.send(player, "&7$status")
            return true
        }
        val value = when (suggestion.field) {
            WorldBlueprintSuggestionField.STORY -> suggestion.candidate.story
            WorldBlueprintSuggestionField.ARCHITECTURE -> suggestion.candidate.architecture
            WorldBlueprintSuggestionField.PROPERTY -> suggestion.candidate.properties.getValue(suggestion.key!!)
            WorldBlueprintSuggestionField.MAP_RULE -> suggestion.candidate.mapRules.getValue(suggestion.key!!)
            WorldBlueprintSuggestionField.PROFESSION_LIMIT -> suggestion.candidate.professionTypeLimit.toString()
            WorldBlueprintSuggestionField.PROFESSION -> {
                val profession = suggestion.candidate.professions.getValue(suggestion.key!!)
                "max NPC ${profession.maxNpcs}; ${profession.description}"
            }
        }
        plugin.messageUtils.send(player, "&6=== Sugestie ${suggestion.field.id}${suggestion.key?.let { "/$it" } ?: ""} ===")
        val retention = if (saved == null) "Preview temporar; expira dupa 10 minute."
            else "Sugestie salvata la ${saved.savedAt}; disponibila si dupa restart."
        plugin.messageUtils.send(player, "&7$retention")
        plugin.messageUtils.send(player, "&7Sursa: ${suggestion.provenance.provider}/${suggestion.provenance.model}; cerere ${suggestion.provenance.instructionFingerprint.take(12)}; timp ${suggestion.provenance.requestedAt}")
        value.chunked(180).forEach { part -> player.sendMessage(Component.text(part)) }
        val actual = try {
            (current ?: store.get(worldName))?.let(store::fingerprint) ?: "none"
        } catch (_: IOException) {
            plugin.messageUtils.send(player, "&eNu pot verifica revizia draftului; citirea fisierului a esuat.")
            return true
        }
        if (actual != suggestion.baseFingerprint) {
            plugin.messageUtils.send(player, "&eDraftul s-a schimbat intre timp; sugestia se bazeaza pe o revizie veche.")
        }
        val fingerprint = store.fingerprint(suggestion.candidate)
        plugin.messageUtils.send(player, "&7Amprenta candidatului: &f$fingerprint")
        plugin.messageUtils.send(player, "&7Acceptare explicita: /ainpc world blueprint $worldName suggest accept $fingerprint")
        if (saved == null) {
            plugin.messageUtils.send(player, "&7Salvare (inlocuieste propunerea ta salvata): /ainpc world blueprint $worldName suggest save $fingerprint")
        }
        plugin.messageUtils.send(player, "&7Preview-ul nu se aplica automat in draft sau in lume.")
        return true
    }

    private fun livePreview(playerId: UUID, worldName: String): WorldBlueprintSuggestion? {
        val suggestion = previews[playerId]?.takeIf { it.worldName == worldName } ?: return null
        if (System.currentTimeMillis() - suggestion.provenance.requestedAt > LIFETIME_MILLIS) {
            previews.remove(playerId, suggestion)
            return null
        }
        return suggestion
    }

    private fun save(
        plugin: AINPCPlugin, player: Player, worldName: String,
        store: WorldBlueprintStore, expectedFingerprint: String,
    ): Boolean {
        if (isRuntimeReadOnly(plugin)) {
            plugin.messageUtils.send(player, "&cMCP read_only este activ; salvarea sugestiei este blocata.")
            return true
        }
        val suggestion = livePreview(player.uniqueId, worldName) ?: run {
            plugin.messageUtils.send(player, "&cNu exista preview temporar valid; cere o sugestie noua.")
            return true
        }
        store.saveSuggestion(player.uniqueId, suggestion, expectedFingerprint)
        previews.remove(player.uniqueId, suggestion)
        plugin.messageUtils.send(player, "&aSugestia a fost pastrata pentru revizie dupa restart. Inlocuieste sugestia ta salvata anterior.")
        plugin.messageUtils.send(player, "&7O poti revedea cu suggest saved si accepta cu suggest accept $expectedFingerprint.")
        return true
    }

    private fun accept(
        plugin: AINPCPlugin, player: Player, worldName: String,
        store: WorldBlueprintStore, expectedFingerprint: String,
    ): Boolean {
        if (isRuntimeReadOnly(plugin)) {
            plugin.messageUtils.send(player, "&cMCP read_only este activ; acceptarea sugestiei este blocata.")
            return true
        }
        val suggestion = livePreview(player.uniqueId, worldName)
        if (suggestion != null && store.fingerprint(suggestion.candidate) == expectedFingerprint) {
            store.acceptSuggestion(suggestion, expectedFingerprint, player.name, player.uniqueId)
            previews.remove(player.uniqueId, suggestion)
        } else {
            store.acceptSavedSuggestion(player.uniqueId, worldName, expectedFingerprint, player.name)
        }
        plugin.messageUtils.send(player, "&aSugestia a fost salvata in draft, cu provenienta; confirmarea anterioara a fost retrasa.")
        plugin.messageUtils.send(player, "&7Ruleaza review si confirm daca doresti. Nu s-a aplicat nimic in lume.")
        return true
    }

    private fun discard(plugin: AINPCPlugin, player: Player, worldName: String, store: WorldBlueprintStore): Boolean {
        if (store.getSavedSuggestion(player.uniqueId, worldName) != null) {
            if (isRuntimeReadOnly(plugin)) {
                plugin.messageUtils.send(player, "&cMCP read_only este activ; stergerea sugestiei salvate este blocata.")
                return true
            }
            store.discardSavedSuggestion(player.uniqueId, worldName)
        }
        pending.computeIfPresent(player.uniqueId) { _, entry ->
            if (entry.worldName == worldName) entry.copy(discarded = true) else entry
        }
        previews.computeIfPresent(player.uniqueId) { _, entry -> if (entry.worldName == worldName) null else entry }
        plugin.messageUtils.send(player, "&7Sugestia pentru $worldName a fost eliminata; un raspuns in curs va fi ignorat. Draftul ramane neschimbat.")
        return true
    }

    private fun usage(plugin: AINPCPlugin, sender: CommandSender): Boolean {
        plugin.messageUtils.send(sender, "&6/ainpc world blueprint <lume> suggest <story|architecture|profession-limit|property|profession|map-rule> [cheie] <descriere>")
        plugin.messageUtils.send(sender, "&7suggest show | suggest saved | suggest save <amprenta> | suggest accept <amprenta> | suggest discard")
        return true
    }
}
