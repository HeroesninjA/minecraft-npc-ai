package ro.ainpc.gui.screens

import org.bukkit.Material
import org.bukkit.entity.Player
import ro.ainpc.commands.WorldBlueprintSuggestionCommand
import ro.ainpc.commands.WorldBlueprintWizard
import ro.ainpc.commands.isRuntimeReadOnly
import ro.ainpc.gui.GuiAction
import ro.ainpc.gui.GuiButton
import ro.ainpc.gui.GuiItemFactory
import ro.ainpc.gui.GuiKey
import ro.ainpc.gui.GuiRenderContext
import ro.ainpc.gui.GuiScreen
import ro.ainpc.world.WorldBlueprint
import ro.ainpc.world.WorldBlueprintPolicy
import ro.ainpc.world.WorldBlueprintWorldCreationGuard
import ro.ainpc.world.WorldBlueprintWorldCreationAttempt
import ro.ainpc.world.WorldBlueprintWorldCreationJournal
import ro.ainpc.world.WorldBlueprintWorldCreationStatus
import ro.ainpc.world.WorldBlueprintWorldDirectoryInspector
import ro.ainpc.world.WorldBlueprintWorldGenerationRules
import ro.ainpc.world.WorldBlueprintWorldSeedRule
import ro.ainpc.world.WorldBlueprintSavedSuggestion
import ro.ainpc.world.WorldBlueprintStore
import ro.ainpc.world.WorldBlueprintSuggestion
import ro.ainpc.world.WorldBlueprintSuggestionField

internal data class WorldBlueprintReviewCandidate(
    val field: String,
    val key: String?,
    val value: String,
    val source: String,
    val provider: String,
    val currentRevision: Boolean,
    val fingerprint: String,
    val saveable: Boolean,
)

internal data class WorldBlueprintReviewDraft(
    val fingerprint: String,
    val approved: Boolean,
    val completionIssues: List<String>,
) {
    val canConfirm: Boolean get() = !approved && completionIssues.isEmpty()
}

internal object WorldBlueprintReviewSelection {
    fun draft(current: WorldBlueprint?, fingerprint: (WorldBlueprint) -> String): WorldBlueprintReviewDraft? {
        if (current == null) return null
        val revision = fingerprint(current)
        return WorldBlueprintReviewDraft(revision, current.approval?.fingerprint == revision,
            WorldBlueprintPolicy.completionIssues(current))
    }

    fun select(
        current: WorldBlueprint?,
        preview: WorldBlueprintSuggestion?,
        saved: WorldBlueprintSavedSuggestion?,
        fingerprint: (WorldBlueprint) -> String,
    ): WorldBlueprintReviewCandidate? {
        val suggestion = preview ?: saved?.suggestion ?: return null
        val value = when (suggestion.field) {
            WorldBlueprintSuggestionField.STORY -> suggestion.candidate.story
            WorldBlueprintSuggestionField.ARCHITECTURE -> suggestion.candidate.architecture
            WorldBlueprintSuggestionField.PROPERTY -> suggestion.candidate.properties.getValue(requireNotNull(suggestion.key))
            WorldBlueprintSuggestionField.MAP_RULE -> suggestion.candidate.mapRules.getValue(requireNotNull(suggestion.key))
            WorldBlueprintSuggestionField.PROFESSION_LIMIT -> suggestion.candidate.professionTypeLimit.toString()
            WorldBlueprintSuggestionField.PROFESSION -> suggestion.candidate.professions.getValue(requireNotNull(suggestion.key))
                .let { "max_npcs=${it.maxNpcs}; ${it.description}" }
        }
        val currentRevision = suggestion.baseFingerprint == (current?.let(fingerprint) ?: "none")
        return WorldBlueprintReviewCandidate(suggestion.field.id, suggestion.key, value,
            if (preview != null) "preview temporar" else "sugestie salvata",
            "${suggestion.provenance.provider}/${suggestion.provenance.model}",
            currentRevision, fingerprint(suggestion.candidate), preview != null && currentRevision)
    }
}

class WorldBlueprintReviewGui : GuiScreen {
    override fun key(): GuiKey = GuiKey.BLUEPRINT_REVIEW

    override fun title(player: Player): String = "&0Revizie Blueprint"

    override fun size(player: Player): Int = 27

    override fun render(context: GuiRenderContext) {
        val player = context.player()
        if (!player.hasPermission("ainpc.admin")) {
            context.item(4, GuiItemFactory.item(Material.BARRIER, "&cAcces restrictionat"))
            context.fillEmpty(GuiItemFactory.filler())
            return
        }
        val worldName = context.service().getBlueprintReviewWorld(player)
        if (worldName == null) {
            context.item(4, GuiItemFactory.item(Material.BARRIER, "&cNicio lume selectata",
                "&7Foloseste /ainpc world blueprint <lume> suggest gui."))
            context.fillEmpty(GuiItemFactory.filler())
            return
        }
        val store = WorldBlueprintStore(context.plugin().dataFolder.toPath().resolve("world-blueprints.json"))
        val data = runCatching {
            val current = store.get(worldName)
            val preview = WorldBlueprintSuggestionCommand.previewForReview(player.uniqueId, worldName)
            val saved = store.getSavedSuggestion(player.uniqueId, worldName)
            Triple(current, WorldBlueprintReviewSelection.select(current, preview, saved, store::fingerprint), saved != null)
        }.getOrElse {
            context.item(4, GuiItemFactory.item(Material.BARRIER, "&cNu pot citi draftul",
                "&7Verifica fisierul world-blueprints.json."))
            context.fillEmpty(GuiItemFactory.filler())
            return
        }
        val (draft, candidate, hasSaved) = data
        val command = "ainpc world blueprint $worldName"
        context.item(4, GuiItemFactory.item(Material.WRITABLE_BOOK, "&6Revizie: $worldName", listOf(
            "&7Revizia nu aplica nimic automat.",
            "&7Click pe o categorie: introdu valoarea in chat.",
        )))
        if (draft == null) {
            context.item(10, GuiItemFactory.item(Material.BARRIER, "&eDraft absent", "&7Poti revizui o sugestie pentru un draft nou."))
            wizardButton(context, 11, Material.WRITABLE_BOOK, "&aIncepe draftul", "$command wizard",
                listOf("&7Ghid pas cu pas pentru aceasta lume."))
        } else {
            val review = requireNotNull(WorldBlueprintReviewSelection.draft(draft, store::fingerprint))
            context.item(10, GuiItemFactory.item(Material.BOOK, "&bDraft curent", listOf(
                when {
                    review.completionIssues.isNotEmpty() -> "&eDraft incomplet"
                    review.approved -> "&aRevizie confirmata"
                    else -> "&eRevizie neconfirmata"
                },
                "&7Amprenta: ${review.fingerprint.take(12)}...",
            )))
            if (review.canConfirm) {
                context.button(8, GuiButton.enabled(GuiItemFactory.item(Material.LIME_CONCRETE,
                    "&aConfirma revizia draftului", listOf(
                        "&7Confirma continutul draftului salvat.",
                        "&7Sugestiile neacceptate raman separate.",
                    )), GuiAction { click -> click.service().runNextTickIfCurrent(click) {
                    click.service().openConfirmCommand(click.player(), "Confirma revizia draftului",
                        "$command confirm ${review.fingerprint}", GuiKey.BLUEPRINT_REVIEW, "",
                        listOf("&7Confirma revizia draftului afisat.",
                            "&7Sugestiile neacceptate nu sunt incluse.",
                            "&7Crearea lumii si aplicarea necesita actiuni separate."))
                } }))
            } else if (review.completionIssues.isNotEmpty()) {
                readOnlyButton(context, 8, Material.BARRIER,
                    "&eDraft incomplet: ${review.completionIssues.size} probleme", "$command validate")
            }
            val latestCreation = runCatching {
                WorldBlueprintWorldCreationJournal(context.plugin().dataFolder.toPath()
                    .resolve("world-blueprint-creations.json")).latest(worldName, 1).firstOrNull()
            }
            readOnlyButton(context, 5, Material.SPYGLASS, "&bVerifica ultima creare", "$command creation verify")
            context.button(7, GuiButton.enabled(GuiItemFactory.item(Material.CLOCK, "&bIstoric creare", listOf(
                WorldBlueprintReviewCreationSelection.historyLine(latestCreation),
                "&7Click: arata jurnalul in chat; nu modifica date.",
            )), GuiAction { click -> click.service().runNextTickIfCurrent(click) {
                click.service().runCommand(click.player(), "$command creation")
            } }))
            if (review.approved && context.plugin().server.getWorld(worldName) == null) {
                val preflightIssue = if (isRuntimeReadOnly(context.plugin())) "MCP read_only este activ."
                    else WorldBlueprintWorldCreationGuard.issue(draft, review.fingerprint, review.fingerprint,
                        false, WorldBlueprintWorldDirectoryInspector.inspect(
                            context.plugin().server.worldContainer.toPath(), worldName))
                val creationIssue = if (preflightIssue != null) preflightIssue else
                    WorldBlueprintReviewCreationSelection.issue(latestCreation)
                if (creationIssue != null) {
                    context.item(6, GuiItemFactory.item(Material.BARRIER, "&cCreare indisponibila", "&7$creationIssue"))
                } else {
                    val generationLines = WorldBlueprintReviewCreationSelection.generationLines(draft)
                    context.button(6, GuiButton.enabled(GuiItemFactory.item(Material.GRASS_BLOCK,
                        "&aCreeaza lumea vanilla", listOf(
                            "&7Lume noua: $worldName",
                            "&7Revizie: ${review.fingerprint.take(12)}...",
                        ) + generationLines + listOf(
                            "&eNu aplica povestea, arhitectura sau NPC-uri.",
                            "&cNecesita confirmare explicita.",
                        )), GuiAction { click -> click.service().runNextTickIfCurrent(click) {
                        click.service().openConfirmCommand(click.player(), "Creeaza lumea vanilla",
                            "$command create-world ${review.fingerprint}", GuiKey.BLUEPRINT_REVIEW, "",
                            listOf("&cCreeaza definitiv o lume vanilla noua: $worldName.") + generationLines + listOf(
                                "&7Nu aplica povestea, arhitectura sau NPC-uri.",
                                "&eVerificarile sunt refacute la executie; nu exista rollback automat."),
                            forceConfirmation = true)
                    } }))
                }
            }
            textEditButton(context, 9, Material.COMPARATOR, "&eLimita tipurilor de meserii",
                "blueprint_edit_profession_limit", worldName, review.fingerprint,
                listOf("&7Limita curenta: ${draft.professionTypeLimit}", "&7Scrie un numar intre 1 si 64."))
            textEditButton(context, 11, Material.PAPER, "&dPoveste", "blueprint_edit_story", worldName,
                review.fingerprint, listOf("&7${summary(draft.story)}", "&7Scrie noua poveste a lumii."))
            textEditButton(context, 12, Material.MAP, "&bProprietati", "blueprint_edit_property",
                worldName, review.fingerprint,
                listOf("&7${draft.properties.size} intrari", "&7Scrie cheie: descriere sau sterge <cheie>."))
            textEditButton(context, 13, Material.NAME_TAG, "&eMeserii", "blueprint_edit_profession",
                worldName, review.fingerprint,
                listOf("&7${draft.professions.size}/${draft.professionTypeLimit} tipuri",
                    "&7Scrie id maxNpc descriere sau sterge <id>.",
                    "&7Limitele NPC sunt doar in draft."))
            textEditButton(context, 14, Material.BRICKS, "&6Arhitectura", "blueprint_edit_architecture",
                worldName, review.fingerprint,
                listOf("&7${summary(draft.architecture)}", "&7Scrie noul stil arhitectural."))
            textEditButton(context, 15, Material.FILLED_MAP, "&aReguli harta", "blueprint_edit_map_rule",
                worldName, review.fingerprint,
                listOf("&7${draft.mapRules.size} intrari", "&7Scrie cheie: descriere sau sterge <cheie>."))
        }
        if (candidate == null) {
            context.item(16, GuiItemFactory.item(Material.GRAY_DYE, "&7Nicio sugestie", "&7Cere una cu suggest <camp> ..."))
        } else {
            context.item(16, GuiItemFactory.item(Material.AMETHYST_SHARD,
                "&d${candidate.field}${candidate.key?.let { "/$it" } ?: ""}", listOf(
                    "&7${candidate.source}; ${candidate.provider}",
                    if (candidate.currentRevision) "&aRevizie curenta" else "&cRevizie depasita; nu accepta",
                    "&7Amprenta candidat: ${candidate.fingerprint.take(12)}...",
                    "&f${summary(candidate.value)}",
                    "&7Textul complet: click pe detalii.",
                )))
        }
        if (draft != null) {
            readOnlyButton(context, 19, Material.BOOK, "&bArata draftul", "$command show")
            readOnlyButton(context, 20, Material.COMPASS, "&ePlan read-only", "$command plan")
            readOnlyButton(context, 21, Material.PAPER, "&aRevizie si amprenta", "$command review")
        }
        if (candidate != null) {
            readOnlyButton(context, 23, Material.SPYGLASS, "&dDetalii sugestie", "$command suggest show")
        }
        if (candidate?.saveable == true) {
            context.button(22, GuiButton.enabled(GuiItemFactory.item(Material.EMERALD,
                "&aSalveaza preview-ul", listOf(
                    "&7Cere confirmare; nu schimba draftul.",
                    "&eInlocuieste sugestia ta salvata anterior.",
                )), GuiAction { click -> click.service().runNextTickIfCurrent(click) {
                click.service().openConfirmCommand(click.player(), "Salveaza sugestia",
                    "$command suggest save ${candidate.fingerprint}", GuiKey.BLUEPRINT_REVIEW, "",
                    listOf("&eInlocuieste sugestia ta salvata anterior.",
                        "&7Nu accepta propunerea si nu modifica draftul."))
            } }))
        }
        if (candidate?.currentRevision == true) {
            context.button(18, GuiButton.enabled(GuiItemFactory.item(Material.LIME_DYE,
                "&aAccepta in draft", listOf(
                    "&7Cere confirmare; modifica un singur camp.",
                    "&eRetrage confirmarea reviziei draftului.",
                    "&7Nu aplica nimic in lume.",
                )), GuiAction { click -> click.service().runNextTickIfCurrent(click) {
                click.service().openConfirmCommand(click.player(), "Accepta sugestia in draft",
                    "$command suggest accept ${candidate.fingerprint}", GuiKey.BLUEPRINT_REVIEW, "",
                    listOf("&eSchimba ${candidate.field}${candidate.key?.let { "/$it" } ?: ""} in draft.",
                        "&eRetrage confirmarea precedenta; cere review/confirm nou.",
                        "&7Nu modifica lumea, mapping-ul ori NPC-urile."))
            } }))
        }
        if (hasSaved) {
            readOnlyButton(context, 24, Material.CHEST, "&bSugestie salvata", "$command suggest saved")
        }
        context.button(17, GuiButton.enabled(GuiItemFactory.item(Material.BOOKSHELF,
            "&bLumi si drafturi", "&7Deschide lista lumilor si drafturilor."),
            GuiAction { click -> click.service().runNextTickIfCurrent(click) {
                click.service().openBlueprintList(click.player(), click.service().getBlueprintListPage(click.player()))
            } }))
        context.button(25, GuiButton.enabled(GuiItemFactory.item(Material.SUNFLOWER, "&aReincarca"),
            GuiAction { click -> click.service().runNextTickIfCurrent(click) {
                click.service().open(click.player(), GuiKey.BLUEPRINT_REVIEW)
            } }))
        context.button(26, GuiButton.enabled(GuiItemFactory.item(Material.BARRIER, "&cInchide"),
            GuiAction { click -> click.service().runNextTickIfCurrent(click) {
                click.player().closeInventory()
            } }))
        context.fillEmpty(GuiItemFactory.filler())
    }

    private fun readOnlyButton(context: GuiRenderContext, slot: Int, material: Material, label: String, command: String) {
        context.button(slot, GuiButton.enabled(GuiItemFactory.item(material, label, "&7Afiseaza detalii in chat; nu modifica date."),
            GuiAction { click -> click.service().runNextTickIfCurrent(click) {
                click.service().runCommand(click.player(), command)
            } }))
    }

    private fun wizardButton(
        context: GuiRenderContext, slot: Int, material: Material, label: String, command: String, lore: List<String>,
    ) {
        context.button(slot, GuiButton.enabled(GuiItemFactory.item(material, label, lore + listOf(
            "&7Click: deschide wizard-ul in chat.",
            "&7Fiecare raspuns valid se salveaza imediat.",
        )), GuiAction { click -> click.service().runNextTickIfCurrent(click) {
            click.service().runCommand(click.player(), command)
        } }))
    }

    private fun textEditButton(
        context: GuiRenderContext, slot: Int, material: Material, label: String,
        formKey: String, worldName: String, fingerprint: String, lore: List<String>,
    ) {
        context.button(slot, GuiButton.enabled(GuiItemFactory.item(material, label, lore + listOf(
            "&7Click: introdu valoarea in chat privat.",
            "&7Se salveaza imediat dupa validare.",
        )), GuiAction { click -> click.service().runNextTickIfCurrent(click) {
            val player = click.player()
            if (!player.hasPermission("ainpc.admin")) {
                click.plugin().messageUtils.send(player, "&cNu ai permisiunea de a edita draftul.")
            } else if (WorldBlueprintWizard.hasSession(player.uniqueId)) {
                click.plugin().messageUtils.send(player, "&eOpreste mai intai wizard-ul activ.")
            } else if (isRuntimeReadOnly(click.plugin())) {
                click.plugin().messageUtils.send(player, "&cMCP read_only este activ; editarea este blocata.")
            } else {
                click.service().openTextInput(player, label, formKey, GuiKey.BLUEPRINT_REVIEW,
                    returnSelector = worldName, promptLines = lore, expectedRevision = fingerprint)
            }
        } }))
    }

    private fun summary(value: String): String {
        val normalized = value.replace(Regex("\\s+"), " ").trim()
        return normalized.take(90).let { if (normalized.length > 90) "$it..." else it }
            .ifBlank { "nespecificat" }
    }
}

internal object WorldBlueprintReviewCreationSelection {
    fun historyLine(latestAttempt: Result<WorldBlueprintWorldCreationAttempt?>): String =
        latestAttempt.fold(
            onSuccess = { attempt -> when (attempt?.status) {
                WorldBlueprintWorldCreationStatus.PENDING -> "&cUltima incercare: PENDING"
                WorldBlueprintWorldCreationStatus.CREATED -> "&aUltima incercare: CREATED"
                WorldBlueprintWorldCreationStatus.FAILED -> "&eUltima incercare: FAILED"
                null -> "&7Nicio incercare in jurnal"
            } },
            onFailure = { "&cJurnalul nu poate fi citit" },
        )

    fun generationLines(blueprint: WorldBlueprint): List<String> {
        val seed = WorldBlueprintWorldSeedRule.parse(blueprint.mapRules).seed
        val generation = WorldBlueprintWorldGenerationRules.parse(blueprint.mapRules)
        return listOf(
            "&7Seed: ${seed ?: "implicit server"}",
            "&7Tip: ${generation.worldType?.name?.lowercase() ?: "implicit server"}; dimensiune: ${generation.environment?.name?.lowercase() ?: "implicit server"}",
            "&7Structuri vanilla: ${generation.generateStructures?.toString() ?: "implicit server"}",
        )
    }

    fun issue(latestAttempt: Result<WorldBlueprintWorldCreationAttempt?>): String? {
        val latest = latestAttempt.getOrElse { return "Jurnalul de creare nu poate fi citit; verifica fisierul." }
        return if (latest?.status == WorldBlueprintWorldCreationStatus.PENDING)
            "Incercare PENDING; verifica creation verify si foloseste retry explicit din comanda."
        else null
    }
}
