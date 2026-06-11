package ro.ainpc.commands

import org.bukkit.command.CommandSender
import ro.ainpc.AINPCPlugin
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.world.NpcWorldBinding
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldPlaceInfo
import java.sql.SQLException

private const val MAX_BINDINGS_TO_SCAN = 500

object DemoReadinessCommand {
    private const val DEFAULT_REGION_ID = "demo_sat"
    private const val DEFAULT_PLAYER_PLACEHOLDER = "<player>"

    @JvmStatic
    fun handle(plugin: AINPCPlugin, sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.messageUtils.sendMessage(sender, "no_permission")
            return true
        }

        val action = args.getOrNull(1)?.lowercase() ?: "status"
        if (!isKnownAction(action)) {
            sendUsage(plugin, sender)
            return true
        }

        if (isExperimentalTwentyFiveOpsAction(action)) {
            if (args.size > 4) {
                sendUsage(plugin, sender)
                return true
            }
            val regionId = args.getOrNull(2)?.takeIf { it.isNotBlank() } ?: DEFAULT_REGION_ID
            val playerName = args.getOrNull(3)?.takeIf { it.isNotBlank() } ?: DEFAULT_PLAYER_PLACEHOLDER
            sendExperimentalTwentyFiveOpsTaskPack(plugin, sender, regionId, playerName)
            return true
        }

        if (isExperimentalTwentyFiveDeepAction(action)) {
            if (args.size > 4) {
                sendUsage(plugin, sender)
                return true
            }
            val regionId = args.getOrNull(2)?.takeIf { it.isNotBlank() } ?: DEFAULT_REGION_ID
            val playerName = args.getOrNull(3)?.takeIf { it.isNotBlank() } ?: DEFAULT_PLAYER_PLACEHOLDER
            sendExperimentalTwentyFiveDeepTaskPack(plugin, sender, regionId, playerName)
            return true
        }

        if (isExperimentalTwentyFiveAction(action)) {
            if (args.size > 4) {
                sendUsage(plugin, sender)
                return true
            }
            val regionId = args.getOrNull(2)?.takeIf { it.isNotBlank() } ?: DEFAULT_REGION_ID
            val playerName = args.getOrNull(3)?.takeIf { it.isNotBlank() } ?: DEFAULT_PLAYER_PLACEHOLDER
            sendExperimentalTwentyFiveTaskPack(plugin, sender, regionId, playerName)
            return true
        }

        if (isExperimentalFiveAction(action)) {
            if (args.size > 4) {
                sendUsage(plugin, sender)
                return true
            }
            val regionId = args.getOrNull(2)?.takeIf { it.isNotBlank() } ?: DEFAULT_REGION_ID
            val playerName = args.getOrNull(3)?.takeIf { it.isNotBlank() } ?: DEFAULT_PLAYER_PLACEHOLDER
            sendExperimentalFiveTaskPack(plugin, sender, regionId, playerName)
            return true
        }

        if (isExperimentalAction(action)) {
            if (args.size > 4) {
                sendUsage(plugin, sender)
                return true
            }
            val regionId = args.getOrNull(2)?.takeIf { it.isNotBlank() } ?: DEFAULT_REGION_ID
            val playerName = args.getOrNull(3)?.takeIf { it.isNotBlank() } ?: DEFAULT_PLAYER_PLACEHOLDER
            sendExperimentalDemoPack(plugin, sender, regionId, playerName)
            return true
        }

        if (isDefinitionAction(action)) {
            if (args.size > 2) {
                sendUsage(plugin, sender)
                return true
            }
            sendDemoDefinition(plugin, sender)
            return true
        }

        if (isScriptAction(action) || isPhaseAction(action) || isEvidenceAction(action) || isRunbookAction(action) || isSmokeAction(action) || isSummaryAction(action) || isCommandsAction(action)) {
            if (args.size > 4) {
                sendUsage(plugin, sender)
                return true
            }
            val regionId = args.getOrNull(2)?.takeIf { it.isNotBlank() } ?: DEFAULT_REGION_ID
            val playerName = args.getOrNull(3)?.takeIf { it.isNotBlank() } ?: DEFAULT_PLAYER_PLACEHOLDER
            when {
                isPhaseAction(action) -> sendDemoPhases(plugin, sender, regionId, playerName)
                isEvidenceAction(action) -> sendDemoEvidence(plugin, sender, regionId, playerName)
                isRunbookAction(action) -> sendDemoRunbook(plugin, sender, regionId, playerName)
                isSmokeAction(action) -> sendDemoSmoke(plugin, sender, regionId, playerName)
                isSummaryAction(action) -> sendDemoSummary(plugin, sender, regionId, playerName)
                isCommandsAction(action) -> sendDemoCommands(plugin, sender, regionId, playerName)
                else -> sendDemoScript(plugin, sender, regionId, playerName)
            }
            return true
        }

        if (isRestartAction(action)) {
            if (args.size > 3) {
                sendUsage(plugin, sender)
                return true
            }
            val regionId = args.getOrNull(2)?.takeIf { it.isNotBlank() } ?: DEFAULT_REGION_ID
            sendDemoRestart(plugin, sender, regionId)
            return true
        }

        if (args.size > 3) {
            sendUsage(plugin, sender)
            return true
        }
        val regionId = args.getOrNull(2)?.takeIf { it.isNotBlank() } ?: DEFAULT_REGION_ID
        val report = DemoReadinessService(plugin).build(regionId)
        if (isNextAction(action)) {
            sendDemoNext(plugin, sender, report)
        } else {
            sendReport(plugin, sender, report)
        }
        return true
    }

    private fun isKnownAction(value: String): Boolean {
        return isStatusAction(value) || isNextAction(value) || isExperimentalAction(value) || isExperimentalFiveAction(value) || isExperimentalTwentyFiveAction(value) || isExperimentalTwentyFiveDeepAction(value) || isExperimentalTwentyFiveOpsAction(value) || isDefinitionAction(value) || isScriptAction(value) || isPhaseAction(value) || isEvidenceAction(value) || isRunbookAction(value) || isSmokeAction(value) || isSummaryAction(value) || isCommandsAction(value) || isRestartAction(value)
    }

    private fun isStatusAction(value: String): Boolean {
        return value == "status" || value == "check" || value == "readiness"
    }

    private fun isNextAction(value: String): Boolean {
        return value == "next" || value == "blockers" || value == "todo"
    }

    private fun isExperimentalAction(value: String): Boolean {
        return value == "experimental" || value == "exp" || value == "maxpack"
    }

    private fun isExperimentalFiveAction(value: String): Boolean {
        return value == "experimental5" || value == "exp5" || value == "fivepack"
    }

    private fun isExperimentalTwentyFiveAction(value: String): Boolean {
        return value == "experimental25" || value == "exp25" || value == "task25" || value == "twentyfivepack"
    }

    private fun isExperimentalTwentyFiveDeepAction(value: String): Boolean {
        return value == "experimental25deep" || value == "exp25deep" || value == "task25deep" || value == "deep25"
    }

    private fun isExperimentalTwentyFiveOpsAction(value: String): Boolean {
        return value == "experimental25ops" || value == "exp25ops" || value == "task25ops" || value == "ops25"
    }

    private fun isDefinitionAction(value: String): Boolean {
        return value == "definition" || value == "criteria" || value == "meaning"
    }

    private fun isScriptAction(value: String): Boolean {
        return value == "script" || value == "guide" || value == "flow"
    }

    private fun isPhaseAction(value: String): Boolean {
        return value == "phases" || value == "phase" || value == "checklist" || value == "roadmap"
    }

    private fun isEvidenceAction(value: String): Boolean {
        return value == "evidence" || value == "proof" || value == "artifacts" || value == "artefacts"
    }

    private fun isRunbookAction(value: String): Boolean {
        return value == "runbook" || value == "guidebook"
    }

    private fun isSmokeAction(value: String): Boolean {
        return value == "smoke" || value == "smoketest" || value == "quickcheck"
    }

    private fun isSummaryAction(value: String): Boolean {
        return value == "summary" || value == "overview" || value == "recap"
    }

    private fun isCommandsAction(value: String): Boolean {
        return value == "commands" || value == "cmds" || value == "copy"
    }

    private fun isRestartAction(value: String): Boolean {
        return value == "restart" || value == "reloadcheck" || value == "persistence"
    }

    private fun sendUsage(plugin: AINPCPlugin, sender: CommandSender) {
        plugin.messageUtils.send(
            sender,
            "&cUtilizare: /ainpc demo <status|next|definition|script|phases|evidence|runbook|smoke|summary|commands|restart|experimental|experimental5|experimental25|experimental25deep|experimental25ops> [regionId] [player]"
        )
    }

    private fun sendReport(plugin: AINPCPlugin, sender: CommandSender, report: DemoReadinessReport) {
        plugin.messageUtils.send(sender, "&6=== Demo readiness: &f${report.regionId}&6 ===")
        plugin.messageUtils.send(
            sender,
            "&7Status: ${report.overallStatus.color}${report.overallStatus.label}" +
                    " &7(&f${report.passedChecks}&7/&f${report.checks.size}&7 checks)"
        )
        plugin.messageUtils.send(
            sender,
            "&7World: &f${report.regionCount} regiuni / ${report.placeCount} places / ${report.nodeCount} nodes" +
                    " &7in region: &f${report.regionPlaceCount} places / ${report.regionNodeCount} nodes"
        )
        plugin.messageUtils.send(
            sender,
            "&7NPC demo: &f${report.demoNpcCount} legati / ${report.spawnedDemoNpcCount} spawnati / " +
                    "${report.regionBindingCount} bindings / ${report.completeRegionBindingCount} complete " +
                    "&8(total loaded=${report.totalNpcCount}, total bindings=${report.bindingCount})"
        )
        plugin.messageUtils.send(
            sender,
            "&7Progression: &f${report.questDefinitionCount} quest / " +
                    "${report.nonQuestDefinitionCount} non-quest definitions / ${report.storedProgressionCount} stored"
        )
        plugin.messageUtils.send(
            sender,
            "&7Story: &f${report.storyEventCount} events recente / state regiune=" +
                    if (report.regionStoryStatePresent) "&ada" else "&enu"
        )
        report.checks.forEach { check ->
            plugin.messageUtils.send(
                sender,
                "${check.status.color}${check.status.mark} &e${check.name}: &f${check.detail}"
            )
        }
        if (report.nextSteps.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&6Urmatorii pasi:")
            report.nextSteps.forEach { step -> plugin.messageUtils.send(sender, "&7- &f$step") }
        }
    }

    private fun sendDemoScript(plugin: AINPCPlugin, sender: CommandSender, regionId: String, playerName: String) {
        plugin.messageUtils.send(sender, "&6=== Demo script: &f$regionId&6 ===")
        plugin.messageUtils.send(sender, "&7Comanda este read-only: listeaza pasii, nu modifica date.")
        demoScript(regionId, playerName).forEachIndexed { index, step ->
            plugin.messageUtils.send(sender, "&e${index + 1}. &f${step.command}")
            plugin.messageUtils.send(sender, "&8   ${step.expected}")
        }
    }

    private fun demoScript(regionId: String, playerName: String): List<DemoScriptStep> {
        return listOf(
            DemoScriptStep("/plugins", "AINPCPlugin si addonul de scenariu apar enabled."),
            DemoScriptStep("/ainpc demo status $regionId", "Vezi PASS/WARN/FAIL inainte de setup."),
            DemoScriptStep(
                "/ainpc world demo create $regionId",
                "Ruleaza doar daca mapping-ul lipseste sau este mediu de test."
            ),
            DemoScriptStep(
                "/ainpc world places $regionId",
                "Confirma regiunea, casele, locurile de munca si social hub-ul."
            ),
            DemoScriptStep("/ainpc world settlement plan $regionId 5", "Planificare dry-run pentru 3-5 NPC-uri demo."),
            DemoScriptStep(
                "/ainpc world settlement spawn $regionId 5",
                "Populeaza satul demo daca planul este acceptabil."
            ),
            DemoScriptStep("/ainpc list", "Alege un NPC vizibil pentru interactiunea urmatoare."),
            DemoScriptStep("/ainpc routine status nearest", "Verifica home/work/social si slotul curent."),
            DemoScriptStep("click dreapta pe NPC + mesaj scurt", "Dialogul porneste sau cade pe fallback fara crash."),
            DemoScriptStep("/ainpc quest nearest", "Arata oferta de quest pentru NPC-ul apropiat."),
            DemoScriptStep("/ainpc quest accept nearest", "Accepta questul demo pentru playerul curent."),
            DemoScriptStep("/ainpc progression stored $playerName", "Arata progres quest si non-quest persistat."),
            DemoScriptStep("/ainpc story events $regionId", "Confirma story event sau explica lipsa lui ca WARN."),
            DemoScriptStep("/ainpc audit all", "Audit administrativ fara editare directa in DB."),
            DemoScriptStep("/ainpc debugdump all", "Export pentru investigatie, fara secrete."),
            DemoScriptStep("/ainpc demo status $regionId", "Raport final de readiness dupa flux.")
        )
    }

    private fun sendDemoDefinition(plugin: AINPCPlugin, sender: CommandSender) {
        plugin.messageUtils.send(sender, "&6=== Demo functional: definitie ===")
        plugin.messageUtils.send(sender, "&7Demo functional inseamna prima bucla interna jucabila si verificabila, nu release public.")
        demoDefinitionCriteria().forEachIndexed { index, criterion ->
            plugin.messageUtils.send(sender, "&e${index + 1}. &f$criterion")
        }
        plugin.messageUtils.send(sender, "&7Verifica starea curenta cu &f/ainpc demo status demo_sat &7si blocajele cu &f/ainpc demo next demo_sat&7.")
    }

    private fun demoDefinitionCriteria(): List<String> {
        return listOf(
            "pluginul core si addonul de scenariu pornesc fara erori critice",
            "exista un sat semantic demo, de exemplu demo_sat",
            "3-5 NPC-uri demo sunt vizibile, persistente si legate de home/work/social",
            "rutina NPC-ului este inspectabila prin /ainpc routine status nearest",
            "interactiunea prin click, chat sau comenzi nu produce crash",
            "un quest poate fi vazut, acceptat si inspectat",
            "o mecanica non-quest este disponibila: contract, duty, bounty, event, tutorial sau ritual",
            "story context, story event sau story state este inspectabil",
            "audit si debugdump ruleaza fara secrete expuse",
            "dupa restart raman mapping-ul, NPC-urile, bindings si progresul minim"
        )
    }

    private fun sendExperimentalDemoPack(plugin: AINPCPlugin, sender: CommandSender, regionId: String, playerName: String) {
        val report = DemoReadinessService(plugin).build(regionId)
        plugin.messageUtils.send(sender, "&c=== EXPERIMENTAL demo maxpack: &f$regionId&c ===")
        plugin.messageUtils.send(sender, "&7Cod experimental: formatul si criteriile pot fi schimbate fara compatibilitate.")
        sendExperimentalOverview(plugin, sender, report, playerName)
        sendExperimentalScorecard(plugin, sender, report)
        sendExperimentalGateMatrix(plugin, sender, report)
        sendExperimentalCommands(plugin, sender, regionId, playerName)
        sendExperimentalPaperRun(plugin, sender, regionId, playerName)
        sendExperimentalFallbackPlan(plugin, sender, regionId, playerName)
        sendExperimentalEvidenceMap(plugin, sender, regionId, playerName)
        sendExperimentalStopConditions(plugin, sender, regionId)
        sendExperimentalRiskNotes(plugin, sender, report)
    }

    private fun sendExperimentalFiveTaskPack(
        plugin: AINPCPlugin,
        sender: CommandSender,
        regionId: String,
        playerName: String
    ) {
        val report = DemoReadinessService(plugin).build(regionId)
        plugin.messageUtils.send(sender, "&c=== EXPERIMENTAL demo fivepack: &f$regionId&c ===")
        plugin.messageUtils.send(sender, "&7Cod experimental: ruleaza cinci task-uri de demo intr-un singur prompt operational.")
        experimentalFiveTasks(report, regionId, playerName).forEachIndexed { taskIndex, task ->
            plugin.messageUtils.send(sender, "&6Task ${taskIndex + 1}/5 - ${task.title}: ${task.status.color}${task.status.label}")
            plugin.messageUtils.send(sender, "&7Scop: &f${task.goal}")
            plugin.messageUtils.send(sender, "&7Acceptare: &f${task.acceptance}")
            task.commands.forEachIndexed { commandIndex, command ->
                plugin.messageUtils.send(sender, "&e${taskIndex + 1}.${commandIndex + 1}. &f$command")
            }
            if (task.risks.isNotEmpty()) {
                plugin.messageUtils.send(sender, "&7Riscuri:")
                task.risks.forEach { risk -> plugin.messageUtils.send(sender, "&8- $risk") }
            }
            if (task.fallbacks.isNotEmpty()) {
                plugin.messageUtils.send(sender, "&7Fallback:")
                task.fallbacks.forEach { fallback -> plugin.messageUtils.send(sender, "&8- $fallback") }
            }
        }
        sendExperimentalFiveSummary(plugin, sender, report)
    }

    private fun sendExperimentalTwentyFiveTaskPack(
        plugin: AINPCPlugin,
        sender: CommandSender,
        regionId: String,
        playerName: String
    ) {
        val report = DemoReadinessService(plugin).build(regionId)
        plugin.messageUtils.send(sender, "&c=== EXPERIMENTAL demo 25-task pack: &f$regionId&c ===")
        plugin.messageUtils.send(sender, "&7Cod experimental: output lung pentru testarea unui prompt operational cu 25 task-uri.")
        experimentalTwentyFiveTasks(report, regionId, playerName).forEachIndexed { index, task ->
            plugin.messageUtils.send(sender, "&6T${index + 1}. ${task.title}: ${task.status.color}${task.status.label}")
            plugin.messageUtils.send(sender, "&7DoD: &f${task.acceptance}")
            plugin.messageUtils.send(sender, "&7Cmd: &f${task.command}")
            plugin.messageUtils.send(sender, "&8Risk: ${task.risk}")
            plugin.messageUtils.send(sender, "&8Fallback: ${task.fallback}")
        }
        sendExperimentalTwentyFiveSummary(plugin, sender, report)
    }

    private fun sendExperimentalTwentyFiveDeepTaskPack(
        plugin: AINPCPlugin,
        sender: CommandSender,
        regionId: String,
        playerName: String
    ) {
        val report = DemoReadinessService(plugin).build(regionId)
        plugin.messageUtils.send(sender, "&c=== EXPERIMENTAL demo deep25: &f$regionId&c ===")
        plugin.messageUtils.send(sender, "&7Cod experimental: 25 task-uri deep pentru hardening intern, fara promisiune de API stabil.")
        experimentalTwentyFiveDeepTasks(report, regionId, playerName).forEachIndexed { index, task ->
            plugin.messageUtils.send(sender, "&6D25-${index + 1}. ${task.title}: ${task.status.color}${task.status.label}")
            plugin.messageUtils.send(sender, "&7Scop: &f${task.goal}")
            plugin.messageUtils.send(sender, "&7Actiune: &f${task.action}")
            plugin.messageUtils.send(sender, "&7Dovada: &f${task.evidence}")
            plugin.messageUtils.send(sender, "&8Risk: ${task.risk}")
            plugin.messageUtils.send(sender, "&8Next: ${task.next}")
        }
        sendExperimentalTwentyFiveDeepSummary(plugin, sender, report)
    }

    private fun sendExperimentalTwentyFiveOpsTaskPack(
        plugin: AINPCPlugin,
        sender: CommandSender,
        regionId: String,
        playerName: String
    ) {
        val report = DemoReadinessService(plugin).build(regionId)
        plugin.messageUtils.send(sender, "&c=== EXPERIMENTAL demo ops25: &f$regionId&c ===")
        plugin.messageUtils.send(sender, "&7Cod experimental: 25 task-uri operationale pentru repetitie Paper cap-coada.")
        experimentalTwentyFiveOpsTasks(report, regionId, playerName).forEachIndexed { index, task ->
            plugin.messageUtils.send(sender, "&6OPS-${index + 1}. ${task.title}: ${task.status.color}${task.status.label}")
            plugin.messageUtils.send(sender, "&7Cmd: &f${task.command}")
            plugin.messageUtils.send(sender, "&7Pass: &f${task.passSignal}")
            plugin.messageUtils.send(sender, "&8Fail: ${task.failSignal}")
            plugin.messageUtils.send(sender, "&8Owner: ${task.ownerHint}")
        }
        sendExperimentalTwentyFiveOpsSummary(plugin, sender, report)
    }

    private fun experimentalTwentyFiveOpsTasks(
        report: DemoReadinessReport,
        regionId: String,
        playerName: String
    ): List<ExperimentalOpsTask> {
        return listOf(
            ExperimentalOpsTask(
                "Server identity",
                DemoReadinessStatus.WARN,
                "/plugins",
                "serverul afiseaza pluginurile corecte pentru sesiunea curenta",
                "plugin lipsa sau disabled",
                "admin server"
            ),
            ExperimentalOpsTask(
                "Demo definition checkpoint",
                DemoReadinessStatus.PASS,
                "/ainpc demo definition",
                "testerul vede criteriile demo functional",
                "testerul cere feature-uri in afara scope-ului",
                "demo owner"
            ),
            ExperimentalOpsTask(
                "Readiness snapshot",
                report.overallStatus,
                "/ainpc demo status $regionId",
                "raportul initial este capturat",
                "status lipsa sau comanda necunoscuta",
                "demo owner"
            ),
            ExperimentalOpsTask(
                "Next action extraction",
                report.overallStatus,
                "/ainpc demo next $regionId",
                "urmatorii pasi sunt concreti",
                "blocajele sunt ambigue",
                "demo owner"
            ),
            ExperimentalOpsTask(
                "Compact command stream",
                report.overallStatus,
                "/ainpc demo commands $regionId $playerName",
                "flux compact de comenzi apare in chat",
                "testerul trebuie sa caute prin documentatie",
                "tester"
            ),
            ExperimentalOpsTask(
                "World position",
                DemoReadinessStatus.WARN,
                "/ainpc world whereami",
                "playerul stie in ce regiune/place se afla",
                "locatia nu este in zona demo",
                "tester"
            ),
            ExperimentalOpsTask(
                "Mapping inventory",
                report.worstStatus("region", "places", "nodes"),
                "/ainpc world places $regionId",
                "places si nodes sunt vizibile",
                "regiunea lipseste sau este prea saraca",
                "world admin"
            ),
            ExperimentalOpsTask(
                "Mapping creation fallback",
                report.worstStatus("region", "places"),
                "/ainpc world demo create $regionId",
                "mapping demo poate fi recreat pe lume de test",
                "comanda rulata accidental pe lume reala",
                "world admin"
            ),
            ExperimentalOpsTask(
                "Settlement dry run",
                report.worstStatus("places", "house_work_social"),
                "/ainpc world settlement plan $regionId 5",
                "planul arata alocari coerente",
                "case/work/social insuficiente",
                "world admin"
            ),
            ExperimentalOpsTask(
                "Settlement execution",
                report.checkStatus("npc_count"),
                "/ainpc world settlement spawn $regionId 5",
                "3-5 NPC-uri demo sunt create sau confirmate",
                "spawn partial sau duplicate",
                "world admin"
            ),
            ExperimentalOpsTask(
                "NPC list sanity",
                report.checkStatus("npc_count"),
                "/ainpc list",
                "NPC-urile demo pot fi identificate",
                "lista include doar NPC-uri nerelevante",
                "tester"
            ),
            ExperimentalOpsTask(
                "Binding list sanity",
                report.checkStatus("npc_bindings"),
                "/ainpc world bindings list 20",
                "home/work/social sunt persistate",
                "binding incomplet sau in alta regiune",
                "world admin"
            ),
            ExperimentalOpsTask(
                "NPC profile check",
                report.checkStatus("npc_count"),
                "/ainpc info nearest",
                "nearest are profil lizibil",
                "nearest selecteaza NPC gresit",
                "tester"
            ),
            ExperimentalOpsTask(
                "Routine status check",
                report.checkStatus("npc_bindings"),
                "/ainpc routine status nearest",
                "rutina explica slotul curent",
                "home/work/social lipsesc",
                "tester"
            ),
            ExperimentalOpsTask(
                "Routine tick smoke",
                report.checkStatus("npc_bindings"),
                "/ainpc routine tick",
                "tick-ul nu produce crash",
                "miscare haotica sau exceptie",
                "tester"
            ),
            ExperimentalOpsTask(
                "Manual dialogue smoke",
                DemoReadinessStatus.WARN,
                "click dreapta pe NPC + mesaj scurt",
                "raspuns sau fallback stabil",
                "server crash sau dialog blocat",
                "tester"
            ),
            ExperimentalOpsTask(
                "Quest log visibility",
                report.checkStatus("quest_definitions"),
                "/ainpc quest log",
                "quest log afiseaza stare curenta",
                "log gol fara explicatie",
                "quest owner"
            ),
            ExperimentalOpsTask(
                "Quest offer visibility",
                report.checkStatus("quest_definitions"),
                "/ainpc quest nearest",
                "oferta sau mesaj clar",
                "nearest nu poate oferi nimic si nu explica",
                "quest owner"
            ),
            ExperimentalOpsTask(
                "Quest accept smoke",
                report.checkStatus("quest_definitions"),
                "/ainpc quest accept nearest",
                "quest activ sau feedback clar",
                "accept crapa sau tace",
                "quest owner"
            ),
            ExperimentalOpsTask(
                "Quest status smoke",
                report.checkStatus("quest_definitions"),
                "/ainpc quest status nearest",
                "starea questului este inspectabila",
                "status ambiguu",
                "quest owner"
            ),
            ExperimentalOpsTask(
                "Progression catalog",
                report.checkStatus("progression_diversity"),
                "/ainpc progression definitions",
                "mecanici non-quest apar in catalog",
                "doar questuri, fara diversitate",
                "progression owner"
            ),
            ExperimentalOpsTask(
                "Stored progression",
                report.checkStatus("stored_progress"),
                "/ainpc progression stored $playerName",
                "progresul playerului este vizibil",
                "0 progres dupa interactiuni",
                "progression owner"
            ),
            ExperimentalOpsTask(
                "Story visibility",
                report.checkStatus("story"),
                "/ainpc story events $regionId",
                "story event/state sau WARN asumat",
                "story lipseste fara explicatie",
                "story owner"
            ),
            ExperimentalOpsTask(
                "Audit and export",
                report.checkStatus("audit_debugdump_next"),
                "/ainpc audit all -> /ainpc debugdump all",
                "audit si export ruleaza",
                "eroare critica sau secret expus",
                "admin server"
            ),
            ExperimentalOpsTask(
                "Restart rehearsal",
                report.overallStatus,
                "/ainpc demo restart $regionId",
                "checklist de persistenta este urmat",
                "stare pierduta dupa restart",
                "demo owner"
            )
        )
    }

    private fun sendExperimentalTwentyFiveOpsSummary(
        plugin: AINPCPlugin,
        sender: CommandSender,
        report: DemoReadinessReport
    ) {
        plugin.messageUtils.send(sender, "&6Experimental ops25 summary:")
        plugin.messageUtils.send(sender, "&7opsTasks=&f25 &7overall=${report.overallStatus.color}${report.overallStatus.label}")
        plugin.messageUtils.send(
            sender,
            "&7world=&f${report.regionPlaceCount}p/${report.regionNodeCount}n" +
                    " &7npc=&f${report.spawnedDemoNpcCount}/${report.demoNpcCount}" +
                    " &7bindings=&f${report.completeRegionBindingCount}/${report.regionBindingCount}"
        )
        if (report.nextSteps.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&6Ops next:")
            report.nextSteps.take(5).forEach { step -> plugin.messageUtils.send(sender, "&7- &f$step") }
        }
    }

    private fun experimentalTwentyFiveDeepTasks(
        report: DemoReadinessReport,
        regionId: String,
        playerName: String
    ): List<ExperimentalDeepTask> {
        return listOf(
            ExperimentalDeepTask(
                "Scope lock",
                DemoReadinessStatus.PASS,
                "ingheata ce inseamna demo functional pentru sesiunea curenta",
                "/ainpc demo definition",
                "output cu criteriile minime in chat",
                "testerul poate cere feature-uri in afara milestone-ului",
                "noteaza non-obiectivele in raportul manual"
            ),
            ExperimentalDeepTask(
                "Baseline readiness",
                report.overallStatus,
                "captureaza starea initiala inainte de orice actiune",
                "/ainpc demo status $regionId",
                "raport PASS/WARN/FAIL cu checks numerotate",
                "daca rulezi direct spawn, pierzi comparatia initiala",
                "salveaza output-ul status inainte de setup"
            ),
            ExperimentalDeepTask(
                "Immediate blockers",
                report.overallStatus,
                "reduce raportul mare la actiuni concrete",
                "/ainpc demo next $regionId",
                "lista cu blocaje, warning-uri si comenzi urmatoare",
                "warning-urile pot ascunde un FAIL de infrastructura",
                "rezolva doar FAIL-urile inainte de smoke"
            ),
            ExperimentalDeepTask(
                "Plugin inventory",
                DemoReadinessStatus.WARN,
                "confirma ca runtime-ul Paper vede pluginurile corecte",
                "/plugins",
                "AINPCPlugin si addonul de scenariu apar enabled",
                "un addon lipsa poate face quest/progression sa para bug de core",
                "reinstaleaza addonul si reporneste serverul"
            ),
            ExperimentalDeepTask(
                "DB health",
                report.checkStatus("world_admin"),
                "confirma ca baza si serviciile admin raspund",
                "/ainpc audit db",
                "audit DB fara eroare critica",
                "DB blocat strica mapping, NPC bindings si progress",
                "opreste demo-ul pana DB audit trece"
            ),
            ExperimentalDeepTask(
                "Region identity",
                report.checkStatus("region"),
                "confirma ca regiunea tinta este cea a demo-ului",
                "/ainpc world places $regionId",
                "lista include regiunea si places asociate",
                "o regiune veche cu acelasi id poate altera concluzia",
                "foloseste lume curata sau regionId nou pentru experiment"
            ),
            ExperimentalDeepTask(
                "Semantic density",
                report.worstStatus("places", "nodes"),
                "verifica densitatea minima de places si nodes",
                "/ainpc demo status $regionId",
                "places=${report.regionPlaceCount}, nodes=${report.regionNodeCount}",
                "densitatea mica reduce valoarea quest/story",
                "recreeaza mapping sau completeaza cu wand"
            ),
            ExperimentalDeepTask(
                "Role coverage",
                report.checkStatus("house_work_social"),
                "asigura combinatia casa, munca si social",
                "/ainpc world places $regionId",
                "case/work/social hub inspectabile",
                "fara roluri, rutina devine doar tehnica",
                "adauga place-uri sau taguri semantice corecte"
            ),
            ExperimentalDeepTask(
                "Spawn dry run",
                report.worstStatus("places", "house_work_social"),
                "valideaza planul inainte de scriere",
                "/ainpc world settlement plan $regionId 5",
                "plan citibil pentru 3-5 NPC-uri",
                "spawn direct poate crea partial state",
                "nu rula spawn pana planul nu este clar"
            ),
            ExperimentalDeepTask(
                "Spawn execution",
                report.checkStatus("npc_count"),
                "creeaza NPC-urile demo cand lipsesc",
                "/ainpc world settlement spawn $regionId 5",
                "3-5 NPC-uri legate de regiune",
                "rollback incomplet poate lasa duplicate",
                "ruleaza audit spawn si list dupa comanda"
            ),
            ExperimentalDeepTask(
                "Binding audit",
                report.checkStatus("npc_bindings"),
                "verifica home/work/social persistat",
                "/ainpc world bindings list 20",
                "${report.completeRegionBindingCount}/${report.regionBindingCount} bindings complete",
                "bindings globale pot include NPC-uri din alte regiuni",
                "coreleaza place IDs cu $regionId"
            ),
            ExperimentalDeepTask(
                "Live entity check",
                report.checkStatus("spawned_npc"),
                "confirma ca NPC-urile sunt entitati vizibile",
                "/ainpc list",
                "${report.spawnedDemoNpcCount}/${report.demoNpcCount} NPC-uri demo spawnate",
                "persistenta DB nu garanteaza entitati live",
                "verifica dupa restart si langa locatia demo"
            ),
            ExperimentalDeepTask(
                "Nearest target hygiene",
                report.checkStatus("npc_count"),
                "evita selectia gresita pentru nearest",
                "/ainpc info nearest",
                "NPC-ul ales are nume/rol corect pentru demo",
                "nearest poate selecta NPC din afara scenei",
                "muta playerul langa NPC-ul demo"
            ),
            ExperimentalDeepTask(
                "Routine visibility",
                report.checkStatus("npc_bindings"),
                "demonstreaza slotul curent sau urmator",
                "/ainpc routine status nearest",
                "home/work/social explicate in output",
                "rutina poate fi corecta dar neclara vizual",
                "foloseste GUI/interact pentru UX suplimentar"
            ),
            ExperimentalDeepTask(
                "Manual routine tick",
                report.checkStatus("npc_bindings"),
                "forteaza o verificare scurta de rutina",
                "/ainpc routine tick",
                "tick ruleaza fara crash",
                "tick-ul manual nu garanteaza stabilitate pe termen lung",
                "observa NPC-ul cateva minute dupa tick"
            ),
            ExperimentalDeepTask(
                "Dialogue fallback",
                DemoReadinessStatus.WARN,
                "confirma ca dialogul nu depinde critic de AI extern",
                "click dreapta pe NPC + mesaj scurt",
                "raspuns sau fallback fara crash",
                "lipsa cheii OpenAI poate fi interpretata ca esec total",
                "noteaza fallback-ul ca acceptabil pentru demo intern"
            ),
            ExperimentalDeepTask(
                "Quest catalog",
                report.checkStatus("quest_definitions"),
                "valideaza ca exista continut quest",
                "/ainpc audit quest",
                "${report.questDefinitionCount} quest definitions",
                "continutul poate exista dar sa nu fie oferit de nearest",
                "foloseste quest debug sau selector explicit"
            ),
            ExperimentalDeepTask(
                "Quest offer",
                report.checkStatus("quest_definitions"),
                "arata oferta de quest in scena demo",
                "/ainpc quest nearest",
                "oferta sau mesaj clar de lipsa",
                "availability poate bloca oferta din cauza progresului vechi",
                "testeaza cu player curat sau reset controlat"
            ),
            ExperimentalDeepTask(
                "Quest accept flow",
                report.checkStatus("quest_definitions"),
                "porneste progresul jucatorului",
                "/ainpc quest accept nearest",
                "quest activ pentru playerul demo",
                "nearest gresit poate accepta nimic",
                "alege manual NPC-ul daca nearest e ambiguu"
            ),
            ExperimentalDeepTask(
                "Progress inspection",
                report.checkStatus("stored_progress"),
                "confirma progres persistat",
                "/ainpc progression stored $playerName",
                "${report.storedProgressionCount} progresii persistate",
                "0 stored poate fi normal inainte de accept",
                "repeta dupa accept/progres quest"
            ),
            ExperimentalDeepTask(
                "Non-quest diversity",
                report.checkStatus("progression_diversity"),
                "arata mecanici non-quest disponibile",
                "/ainpc progression definitions",
                "${report.nonQuestDefinitionCount} non-quest definitions",
                "definitions nu inseamna gameplay complet",
                "arata macar un contract/duty/bounty/tutorial/ritual"
            ),
            ExperimentalDeepTask(
                "Story surface",
                report.checkStatus("story"),
                "verifica story context sau event",
                "/ainpc story events $regionId",
                "events=${report.storyEventCount}, regionState=${report.regionStoryStatePresent}",
                "story poate ramane WARN daca nu s-a finalizat scenariul",
                "accepta WARN doar cu nota de backlog"
            ),
            ExperimentalDeepTask(
                "Evidence bundle",
                report.checkStatus("audit_debugdump_next"),
                "produce dovezi pentru investigatie",
                "/ainpc demo evidence $regionId $playerName",
                "lista de capturi necesare pentru milestone",
                "fara dovezi, un demo reusit nu e reproductibil",
                "salveaza output-urile importante in raport manual"
            ),
            ExperimentalDeepTask(
                "Admin export",
                report.checkStatus("audit_debugdump_next"),
                "exporta starea pentru analiza fara DB live edit",
                "/ainpc audit all -> /ainpc debugdump all",
                "audit si debugdump ruleaza",
                "debugdump poate contine date sensibile in medii gresite",
                "verifica folderul exportat inainte de partajare"
            ),
            ExperimentalDeepTask(
                "Restart proof",
                report.overallStatus,
                "inchide bucla demo cu persistenta",
                "/ainpc demo restart $regionId",
                "checklist inainte si dupa restart",
                "shutdown fortat invalideaza rezultatul",
                "repeta status, places, bindings, list si progression dupa pornire"
            )
        )
    }

    private fun sendExperimentalTwentyFiveDeepSummary(
        plugin: AINPCPlugin,
        sender: CommandSender,
        report: DemoReadinessReport
    ) {
        val fail = report.checks.count { it.status == DemoReadinessStatus.FAIL }
        val warn = report.checks.count { it.status == DemoReadinessStatus.WARN }
        val pass = report.checks.count { it.status == DemoReadinessStatus.PASS }
        plugin.messageUtils.send(sender, "&6Experimental deep25 summary:")
        plugin.messageUtils.send(sender, "&7deepTasks=&f25 &7checks pass=&a$pass &7warn=&e$warn &7fail=&c$fail")
        plugin.messageUtils.send(sender, "&7world=&f${report.regionPlaceCount} places/${report.regionNodeCount} nodes &7npc=&f${report.demoNpcCount}")
        plugin.messageUtils.send(sender, "&7overall=${report.overallStatus.color}${report.overallStatus.label}")
    }

    private fun experimentalTwentyFiveTasks(
        report: DemoReadinessReport,
        regionId: String,
        playerName: String
    ): List<ExperimentalTwentyFiveTask> {
        return listOf(
            ExperimentalTwentyFiveTask(
                "Definitie demo functional",
                DemoReadinessStatus.PASS,
                "testerul poate explica ce se valideaza si ce nu este release public",
                "/ainpc demo definition",
                "scope-ul poate fi confundat cu release public",
                "repeta definitia si blocheaza feature creep-ul"
            ),
            ExperimentalTwentyFiveTask(
                "Plugin load",
                DemoReadinessStatus.WARN,
                "AINPCPlugin si addonul de scenariu apar enabled",
                "/plugins",
                "addonul poate lipsi desi core-ul este pornit",
                "reinstaleaza JAR-urile si reporneste Paper"
            ),
            ExperimentalTwentyFiveTask(
                "Readiness baseline",
                report.overallStatus,
                "exista raport PASS/WARN/FAIL initial",
                "/ainpc demo status $regionId",
                "raportul poate fi WARN din lipsa progress/story",
                "ruleaza fluxul complet si compara statusul final"
            ),
            ExperimentalTwentyFiveTask(
                "Blocaje imediate",
                report.overallStatus,
                "testerul vede urmatoarea actiune concreta",
                "/ainpc demo next $regionId",
                "prea multe warning-uri pot ascunde blocajul principal",
                "rezolva intai FAIL, apoi WARN"
            ),
            ExperimentalTwentyFiveTask(
                "World admin activ",
                report.checkStatus("world_admin"),
                "world_admin este disponibil pentru mapping demo",
                "/ainpc audit db",
                "DB sau config poate bloca toate fazele urmatoare",
                "activeaza world_admin.enabled si reia auditul"
            ),
            ExperimentalTwentyFiveTask(
                "Regiune demo",
                report.checkStatus("region"),
                "regiunea $regionId exista",
                "/ainpc world places $regionId",
                "date vechi pot masca o regiune incompleta",
                "/ainpc world demo create $regionId pe lume de test"
            ),
            ExperimentalTwentyFiveTask(
                "Places suficiente",
                report.checkStatus("places"),
                "exista minimul de places pentru case/work/social",
                "/ainpc world places $regionId",
                "places pot exista dar fara tipuri utile",
                "recreeaza mapping demo intr-o zona plata"
            ),
            ExperimentalTwentyFiveTask(
                "Nodes suficiente",
                report.checkStatus("nodes"),
                "exista nodes pentru ancore si inspectii",
                "/ainpc world places $regionId",
                "questurile pot ramane fara obiective semantice fine",
                "foloseste mapping wand sau recreeaza demo mapping"
            ),
            ExperimentalTwentyFiveTask(
                "House/work/social",
                report.checkStatus("house_work_social"),
                "regiunea are case, locuri de munca si social hub",
                "/ainpc demo status $regionId",
                "NPC-urile pot fi spawnate dar fara rutina coerenta",
                "verifica tipurile/tagurile place-urilor"
            ),
            ExperimentalTwentyFiveTask(
                "Settlement plan",
                report.worstStatus("places", "house_work_social"),
                "planul poate aloca NPC-uri pe casele demo",
                "/ainpc world settlement plan $regionId 5",
                "planul poate propune prea putine case valide",
                "reduce count-ul sau repara mapping-ul"
            ),
            ExperimentalTwentyFiveTask(
                "Settlement spawn",
                report.checkStatus("npc_count"),
                "3-5 NPC-uri demo sunt legate de regiune",
                "/ainpc world settlement spawn $regionId 5",
                "spawn partial poate lasa stare greu de citit",
                "ruleaza audit spawn si repair dryrun"
            ),
            ExperimentalTwentyFiveTask(
                "NPC list",
                report.checkStatus("npc_count"),
                "NPC-urile demo apar in lista",
                "/ainpc list",
                "lista include si NPC-uri din alte regiuni",
                "coreleaza cu world bindings list"
            ),
            ExperimentalTwentyFiveTask(
                "Bindings complete",
                report.checkStatus("npc_bindings"),
                "NPC-urile demo au home/work/social in $regionId",
                "/ainpc world bindings list 20",
                "binding-urile incomplete fac rutina confuza",
                "/ainpc world bind npc nearest <home> <work> <social>"
            ),
            ExperimentalTwentyFiveTask(
                "Spawn vizibil",
                report.checkStatus("spawned_npc"),
                "minim 3 NPC-uri demo sunt spawnate",
                "/ainpc demo status $regionId",
                "NPC-uri persistate pot sa nu fie entitati live",
                "repornește serverul sau reexecuta spawn controlat"
            ),
            ExperimentalTwentyFiveTask(
                "Routine inspect",
                report.checkStatus("npc_bindings"),
                "nearest NPC are rutina explicabila",
                "/ainpc routine status nearest",
                "nearest poate alege NPC gresit",
                "muta-te langa NPC-ul demo ales"
            ),
            ExperimentalTwentyFiveTask(
                "NPC info UX",
                report.checkStatus("npc_count"),
                "playerul intelege cine este NPC-ul",
                "/ainpc info nearest",
                "profilul poate fi tehnic, nu jucabil",
                "foloseste nume/roluri clare in spawn plan"
            ),
            ExperimentalTwentyFiveTask(
                "Dialog fallback",
                DemoReadinessStatus.WARN,
                "click/chat raspunde sau cade pe fallback fara crash",
                "click dreapta pe NPC + mesaj scurt",
                "AI extern indisponibil poate parea bug",
                "testeaza explicit fara OPENAI_API_KEY"
            ),
            ExperimentalTwentyFiveTask(
                "Quest definitions",
                report.checkStatus("quest_definitions"),
                "exista minim un quest jucabil",
                "/ainpc audit quest",
                "definitii invalide pot fi incarcate partial",
                "verifica addonul de scenariu si YAML-urile"
            ),
            ExperimentalTwentyFiveTask(
                "Quest offer",
                report.checkStatus("quest_definitions"),
                "nearest NPC poate oferi sau explica lipsa questului",
                "/ainpc quest nearest",
                "availability poate bloca oferta",
                "foloseste quest log/debug pentru selector explicit"
            ),
            ExperimentalTwentyFiveTask(
                "Quest accept",
                report.checkStatus("quest_definitions"),
                "playerul poate accepta questul demo",
                "/ainpc quest accept nearest",
                "accept poate esua daca nearest nu este giver",
                "alege NPC-ul corect si reia comanda"
            ),
            ExperimentalTwentyFiveTask(
                "Progress stored",
                report.checkStatus("stored_progress"),
                "progresul playerului este inspectabil",
                "/ainpc progression stored $playerName",
                "stored poate fi 0 inainte de interactiuni",
                "accepta/progreseaza quest sau mecanica non-quest"
            ),
            ExperimentalTwentyFiveTask(
                "Progression diversity",
                report.checkStatus("progression_diversity"),
                "exista mecanica non-quest disponibila",
                "/ainpc progression definitions",
                "definitions nu garanteaza progres real",
                "ruleaza contract/duty/bounty/tutorial definitions"
            ),
            ExperimentalTwentyFiveTask(
                "Story inspect",
                report.checkStatus("story"),
                "story event/state/context este verificabil",
                "/ainpc story events $regionId",
                "story poate ramane WARN inainte de quest completion",
                "noteaza backlog sau ruleaza scenariu cu story action"
            ),
            ExperimentalTwentyFiveTask(
                "Audit si debugdump",
                report.checkStatus("audit_debugdump_next"),
                "adminul poate investiga fara DB live edit",
                "/ainpc audit all -> /ainpc debugdump all",
                "debugdump poate expune prea mult daca configuratia e gresita",
                "verifica exportul si sterge date sensibile din medii necontrolate"
            ),
            ExperimentalTwentyFiveTask(
                "Restart gate",
                report.overallStatus,
                "mapping/NPC/bindings/progress raman dupa restart",
                "/ainpc demo restart $regionId",
                "oprirea fortata invalideaza concluzia",
                "shutdown normal, restart, apoi status final"
            )
        )
    }

    private fun sendExperimentalTwentyFiveSummary(
        plugin: AINPCPlugin,
        sender: CommandSender,
        report: DemoReadinessReport
    ) {
        val taskCount = 25
        val fail = report.checks.count { it.status == DemoReadinessStatus.FAIL }
        val warn = report.checks.count { it.status == DemoReadinessStatus.WARN }
        val pass = report.checks.count { it.status == DemoReadinessStatus.PASS }
        plugin.messageUtils.send(sender, "&6Experimental25 summary:")
        plugin.messageUtils.send(sender, "&7tasks=&f$taskCount &7checks pass=&a$pass &7warn=&e$warn &7fail=&c$fail")
        plugin.messageUtils.send(sender, "&7overall=${report.overallStatus.color}${report.overallStatus.label} &7region=&f${report.regionId}")
        if (report.nextSteps.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&6Next from readiness:")
            report.nextSteps.forEach { step -> plugin.messageUtils.send(sender, "&7- &f$step") }
        }
    }

    private fun experimentalFiveTasks(
        report: DemoReadinessReport,
        regionId: String,
        playerName: String
    ): List<ExperimentalFiveTask> {
        return listOf(
            experimentalPreflightTask(report, regionId),
            experimentalSettlementTask(report, regionId),
            experimentalInteractionTask(report, regionId),
            experimentalProgressionTask(report, regionId, playerName),
            experimentalPersistenceTask(report, regionId, playerName)
        )
    }

    private fun experimentalPreflightTask(report: DemoReadinessReport, regionId: String): ExperimentalFiveTask {
        val status = report.worstStatus("world_admin", "region")
        return ExperimentalFiveTask(
            title = "Preflight si definitie",
            status = status,
            goal = "stabileste daca serverul si regiunea pot sustine demo-ul functional",
            acceptance = "pluginul raspunde, world_admin este activ, iar testerul intelege criteriile demo-ului",
            commands = listOf(
                "/plugins",
                "/ainpc demo definition",
                "/ainpc demo status $regionId",
                "/ainpc demo next $regionId",
                "/ainpc audit db"
            ),
            risks = listOf(
                "serverul poate avea addonul lipsa chiar daca core-ul este enabled",
                "world_admin dezactivat face restul demo-ului irelevant",
                "o regiune cu acelasi nume dar date vechi poate masca lipsuri reale"
            ),
            fallbacks = listOf(
                "reinstaleaza JAR-urile si reporneste Paper daca /plugins nu este curat",
                "activeaza world_admin.enabled si reia /ainpc demo status $regionId",
                "foloseste /ainpc world demo create $regionId doar pe lume de test"
            )
        )
    }

    private fun experimentalSettlementTask(report: DemoReadinessReport, regionId: String): ExperimentalFiveTask {
        val status = report.worstStatus("places", "nodes", "house_work_social", "npc_count", "spawned_npc", "npc_bindings")
        return ExperimentalFiveTask(
            title = "Sat semantic si NPC",
            status = status,
            goal = "obtine un sat semantic demo cu 3-5 NPC-uri legate de home/work/social",
            acceptance = "places/nodes exista, NPC-urile sunt vizibile si binding-urile sunt complete in regiune",
            commands = listOf(
                "/ainpc world places $regionId",
                "/ainpc world demo create $regionId",
                "/ainpc world settlement plan $regionId 5",
                "/ainpc world settlement spawn $regionId 5",
                "/ainpc world bindings list 20",
                "/ainpc list"
            ),
            risks = listOf(
                "mapping-ul demo nu construieste blocuri fizice si poate arata abstract",
                "settlement spawn poate fi nepotrivit daca exista deja NPC-uri partiale",
                "NPC-uri din alte regiuni nu trebuie confundate cu NPC-urile demo"
            ),
            fallbacks = listOf(
                "foloseste /ainpc world bind npc nearest <home> <work> <social> pentru legare manuala",
                "ruleaza /ainpc repair npc-bindings dryrun daca profilurile si DB-ul par divergente",
                "foloseste /ainpc audit spawn pentru a identifica duplicate sau batch-uri problematice"
            )
        )
    }

    private fun experimentalInteractionTask(report: DemoReadinessReport, regionId: String): ExperimentalFiveTask {
        val status = report.worstStatus("npc_count", "spawned_npc", "npc_bindings")
        return ExperimentalFiveTask(
            title = "Rutina, UX si dialog",
            status = status,
            goal = "demonstreaza ca NPC-ul poate fi inteles si abordat de jucator",
            acceptance = "nearest NPC are status clar, click/chat nu crapa si fallback-ul este acceptabil",
            commands = listOf(
                "/ainpc list",
                "/ainpc info nearest",
                "/ainpc routine status nearest",
                "/ainpc routine tick",
                "click dreapta pe NPC",
                "trimite mesaj scurt in chat",
                "/ainpc demo smoke $regionId <player>"
            ),
            risks = listOf(
                "rutina poate parea valida in comenzi dar neclara vizual in joc",
                "dialogul cu AI extern nu trebuie sa fie necesar pentru progres logic",
                "un NPC prea departe poate face nearest sa aleaga tinta gresita"
            ),
            fallbacks = listOf(
                "muta-te langa NPC-ul ales si reia /ainpc routine status nearest",
                "testeaza fara OPENAI_API_KEY pentru fallback determinist",
                "foloseste /ainpc info <numeNpc> daca nearest este ambiguu"
            )
        )
    }

    private fun experimentalProgressionTask(
        report: DemoReadinessReport,
        regionId: String,
        playerName: String
    ): ExperimentalFiveTask {
        val status = report.worstStatus("quest_definitions", "progression_diversity", "stored_progress", "story")
        return ExperimentalFiveTask(
            title = "Quest, progression si story",
            status = status,
            goal = "arata gameplay minim: quest plus o mecanica non-quest si story inspectabil",
            acceptance = "questul poate fi acceptat, progress-ul este persistat, iar story-ul poate fi inspectat",
            commands = listOf(
                "/ainpc quest log",
                "/ainpc quest nearest",
                "/ainpc quest accept nearest",
                "/ainpc quest status nearest",
                "/ainpc quest track start",
                "/ainpc progression definitions",
                "/ainpc progression stored $playerName",
                "/ainpc contract definitions",
                "/ainpc duty definitions",
                "/ainpc bounty definitions",
                "/ainpc event definitions",
                "/ainpc tutorial definitions",
                "/ainpc ritual definitions",
                "/ainpc story context $playerName nearest",
                "/ainpc story events $regionId"
            ),
            risks = listOf(
                "definitions pot exista fara progres real pentru player",
                "quest nearest depinde de NPC-ul apropiat si de offer availability",
                "story poate ramane WARN daca nu s-a rulat un scenariu cu story action"
            ),
            fallbacks = listOf(
                "ruleaza /ainpc audit quest pentru validarea addonului de scenariu",
                "foloseste selector explicit in /ainpc quest status <questCode> daca nearest nu e stabil",
                "noteaza story ca backlog daca restul gameplay-ului trece"
            )
        )
    }

    private fun experimentalPersistenceTask(
        report: DemoReadinessReport,
        regionId: String,
        playerName: String
    ): ExperimentalFiveTask {
        return ExperimentalFiveTask(
            title = "Persistenta, audit si recovery",
            status = report.overallStatus,
            goal = "demonstreaza ca demo-ul ramane valid dupa save, debugdump si restart",
            acceptance = "mapping/NPC/bindings/progress pot fi reinspectate dupa restart fara DB edit manual",
            commands = listOf(
                "/ainpc audit all",
                "/ainpc debugdump all",
                "/ainpc world save",
                "/ainpc demo restart $regionId",
                "opreste serverul Paper normal",
                "porneste serverul Paper",
                "/ainpc world places $regionId",
                "/ainpc world bindings list 20",
                "/ainpc list",
                "/ainpc progression stored $playerName",
                "/ainpc demo status $regionId"
            ),
            risks = listOf(
                "debugdump poate confirma starea dar nu inlocuieste restart gate",
                "oprirea fortata a serverului poate invalida concluzia despre persistenta",
                "datele vechi din DB pot ascunde faptul ca fluxul curent nu a scris corect"
            ),
            fallbacks = listOf(
                "fa backup la plugins/AINPC inainte de curatare",
                "foloseste repair dryrun inainte de orice apply",
                "reia demo-ul pe lume curata daca datele vechi sunt ambigue"
            )
        )
    }

    private fun sendExperimentalFiveSummary(plugin: AINPCPlugin, sender: CommandSender, report: DemoReadinessReport) {
        val pass = report.checks.count { it.status == DemoReadinessStatus.PASS }
        val warn = report.checks.count { it.status == DemoReadinessStatus.WARN }
        val fail = report.checks.count { it.status == DemoReadinessStatus.FAIL }
        plugin.messageUtils.send(sender, "&6Experimental fivepack summary:")
        plugin.messageUtils.send(sender, "&7checks pass=&a$pass &7warn=&e$warn &7fail=&c$fail")
        plugin.messageUtils.send(sender, "&7overall=${report.overallStatus.color}${report.overallStatus.label} &7region=&f${report.regionId}")
        if (report.nextSteps.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&6Next compact:")
            report.nextSteps.take(5).forEach { step -> plugin.messageUtils.send(sender, "&7- &f$step") }
        }
    }

    private fun sendExperimentalOverview(
        plugin: AINPCPlugin,
        sender: CommandSender,
        report: DemoReadinessReport,
        playerName: String
    ) {
        plugin.messageUtils.send(sender, "&6Snapshot experimental:")
        plugin.messageUtils.send(
            sender,
            "&7status=${report.overallStatus.color}${report.overallStatus.label}" +
                    " &7checks=&f${report.passedChecks}/${report.checks.size}" +
                    " &7player=&f$playerName"
        )
        plugin.messageUtils.send(
            sender,
            "&7world regions=&f${report.regionCount}" +
                    " &7places=&f${report.regionPlaceCount}/${report.placeCount}" +
                    " &7nodes=&f${report.regionNodeCount}/${report.nodeCount}"
        )
        plugin.messageUtils.send(
            sender,
            "&7npc demo=&f${report.demoNpcCount}" +
                    " &7spawned=&f${report.spawnedDemoNpcCount}" +
                    " &7bindings=&f${report.completeRegionBindingCount}/${report.regionBindingCount}" +
                    " &8serverNpc=${report.totalNpcCount}"
        )
        plugin.messageUtils.send(
            sender,
            "&7progression questDefs=&f${report.questDefinitionCount}" +
                    " &7nonQuestDefs=&f${report.nonQuestDefinitionCount}" +
                    " &7stored=&f${report.storedProgressionCount}"
        )
        plugin.messageUtils.send(
            sender,
            "&7story events=&f${report.storyEventCount}" +
                    " &7regionState=&f${report.regionStoryStatePresent}"
        )
    }

    private fun sendExperimentalScorecard(plugin: AINPCPlugin, sender: CommandSender, report: DemoReadinessReport) {
        plugin.messageUtils.send(sender, "&6Scorecard experimental:")
        experimentalScoreRows(report).forEach { row ->
            plugin.messageUtils.send(sender, "${row.status.color}${row.status.mark} &e${row.label}: &f${row.value} &8${row.note}")
        }
    }

    private fun experimentalScoreRows(report: DemoReadinessReport): List<ExperimentalScoreRow> {
        return listOf(
            ExperimentalScoreRow(
                "World admin",
                report.checkStatus("world_admin"),
                "checks=${report.checkDetail("world_admin")}",
                "serviciu world mapping"
            ),
            ExperimentalScoreRow(
                "Regiune",
                report.checkStatus("region"),
                "region=${report.regionId}",
                "container demo"
            ),
            ExperimentalScoreRow(
                "Places",
                report.checkStatus("places"),
                "${report.regionPlaceCount}/${report.placeCount}",
                "case, munca, social"
            ),
            ExperimentalScoreRow(
                "Nodes",
                report.checkStatus("nodes"),
                "${report.regionNodeCount}/${report.nodeCount}",
                "ancore fine pentru quest/story"
            ),
            ExperimentalScoreRow(
                "NPC demo",
                report.checkStatus("npc_count"),
                "${report.demoNpcCount} legati",
                "tinta 3-5"
            ),
            ExperimentalScoreRow(
                "Spawn",
                report.checkStatus("spawned_npc"),
                "${report.spawnedDemoNpcCount}/${report.demoNpcCount}",
                "entitati vizibile"
            ),
            ExperimentalScoreRow(
                "Bindings",
                report.checkStatus("npc_bindings"),
                "${report.completeRegionBindingCount}/${report.regionBindingCount}",
                "home/work/social in regiune"
            ),
            ExperimentalScoreRow(
                "Quest",
                report.checkStatus("quest_definitions"),
                "${report.questDefinitionCount} definitions",
                "minim un quest"
            ),
            ExperimentalScoreRow(
                "Diversitate",
                report.checkStatus("progression_diversity"),
                "${report.nonQuestDefinitionCount} non-quest",
                "contract/duty/bounty/event/tutorial/ritual"
            ),
            ExperimentalScoreRow(
                "Progres",
                report.checkStatus("stored_progress"),
                "${report.storedProgressionCount} stored",
                "dupa interactiune"
            ),
            ExperimentalScoreRow(
                "Story",
                report.checkStatus("story"),
                "events=${report.storyEventCount} state=${report.regionStoryStatePresent}",
                "context/eveniment/state"
            )
        )
    }

    private fun sendExperimentalGateMatrix(plugin: AINPCPlugin, sender: CommandSender, report: DemoReadinessReport) {
        plugin.messageUtils.send(sender, "&6Gate matrix experimental:")
        experimentalGateRows(report).forEach { row ->
            plugin.messageUtils.send(sender, "${row.status.color}${row.status.mark} &e${row.phase} ${row.title}: &f${row.action}")
            plugin.messageUtils.send(sender, "&8   ${row.evidence}")
        }
    }

    private fun experimentalGateRows(report: DemoReadinessReport): List<ExperimentalGateRow> {
        return listOf(
            ExperimentalGateRow(
                "D0",
                "Scope",
                DemoReadinessStatus.PASS,
                "confirma server Paper dedicat si player de test",
                "ServerDir, RegionId=${report.regionId}, PlayerName"
            ),
            ExperimentalGateRow(
                "D1",
                "Build si config",
                report.checkStatus("world_admin"),
                "/plugins si /ainpc audit db",
                "core + addon enabled, world_admin functional"
            ),
            ExperimentalGateRow(
                "D2",
                "Sat semantic",
                report.worstStatus("region", "places", "nodes", "house_work_social"),
                "/ainpc world demo create ${report.regionId} daca lipseste",
                "places=${report.regionPlaceCount}, nodes=${report.regionNodeCount}"
            ),
            ExperimentalGateRow(
                "D3",
                "NPC si bindings",
                report.worstStatus("npc_count", "spawned_npc", "npc_bindings"),
                "/ainpc world settlement plan ${report.regionId} 5",
                "npc=${report.demoNpcCount}, spawned=${report.spawnedDemoNpcCount}, bindings=${report.completeRegionBindingCount}"
            ),
            ExperimentalGateRow(
                "D4",
                "Rutina si UX",
                report.checkStatus("npc_bindings"),
                "/ainpc routine status nearest si /ainpc info nearest",
                "home/work/social trebuie sa explice slotul curent"
            ),
            ExperimentalGateRow(
                "D5",
                "Quest si progression",
                report.worstStatus("quest_definitions", "progression_diversity", "stored_progress"),
                "/ainpc quest nearest si /ainpc progression stored <player>",
                "questDefs=${report.questDefinitionCount}, nonQuest=${report.nonQuestDefinitionCount}, stored=${report.storedProgressionCount}"
            ),
            ExperimentalGateRow(
                "D6",
                "Story",
                report.checkStatus("story"),
                "/ainpc story events ${report.regionId}",
                "events=${report.storyEventCount}, regionState=${report.regionStoryStatePresent}"
            ),
            ExperimentalGateRow(
                "D7",
                "Dialog",
                DemoReadinessStatus.WARN,
                "click dreapta + mesaj scurt",
                "necesita verificare manuala pe Paper"
            ),
            ExperimentalGateRow(
                "D8",
                "Restart",
                report.overallStatus,
                "/ainpc demo restart ${report.regionId}",
                "persistenta mapping/NPC/bindings/progress dupa restart"
            ),
            ExperimentalGateRow(
                "D9",
                "Tester",
                report.overallStatus,
                "/ainpc demo commands ${report.regionId} <player>",
                "flux manual compact fara editare DB"
            )
        )
    }

    private fun sendExperimentalCommands(
        plugin: AINPCPlugin,
        sender: CommandSender,
        regionId: String,
        playerName: String
    ) {
        plugin.messageUtils.send(sender, "&6Comenzi experimentale agregate:")
        experimentalCommandLines(regionId, playerName).forEachIndexed { index, command ->
            plugin.messageUtils.send(sender, "&e${index + 1}. &f$command")
        }
    }

    private fun experimentalCommandLines(regionId: String, playerName: String): List<String> {
        return listOf(
            "/ainpc demo definition",
            "/ainpc demo status $regionId",
            "/ainpc demo next $regionId",
            "/ainpc world whereami",
            "/ainpc world places $regionId",
            "/ainpc world settlement plan $regionId 5",
            "/ainpc world settlement spawn $regionId 5",
            "/ainpc world bindings list 20",
            "/ainpc list",
            "/ainpc routine status nearest",
            "/ainpc info nearest",
            "/ainpc quest log",
            "/ainpc quest nearest",
            "/ainpc quest accept nearest",
            "/ainpc quest status nearest",
            "/ainpc quest track start",
            "/ainpc progression definitions",
            "/ainpc progression stored $playerName",
            "/ainpc contract definitions",
            "/ainpc duty definitions",
            "/ainpc bounty definitions",
            "/ainpc event definitions",
            "/ainpc tutorial definitions",
            "/ainpc ritual definitions",
            "/ainpc story context $playerName nearest",
            "/ainpc story events $regionId",
            "/ainpc audit all",
            "/ainpc debugdump all",
            "/ainpc demo restart $regionId",
            "/ainpc demo status $regionId"
        )
    }

    private fun sendExperimentalPaperRun(
        plugin: AINPCPlugin,
        sender: CommandSender,
        regionId: String,
        playerName: String
    ) {
        plugin.messageUtils.send(sender, "&6Paper run experimental:")
        experimentalPaperRunSteps(regionId, playerName).forEachIndexed { index, step ->
            plugin.messageUtils.send(sender, "&e${index + 1}. &f${step.title}: &7${step.command}")
            plugin.messageUtils.send(sender, "&8   ${step.expected}")
        }
    }

    private fun experimentalPaperRunSteps(regionId: String, playerName: String): List<ExperimentalRunStep> {
        return listOf(
            ExperimentalRunStep("Preflight", "/plugins", "AINPCPlugin si addonul de scenariu sunt enabled"),
            ExperimentalRunStep("Definitie", "/ainpc demo definition", "testerul intelege criteriile milestone-ului"),
            ExperimentalRunStep("Baseline", "/ainpc demo status $regionId", "snapshot initial PASS/WARN/FAIL"),
            ExperimentalRunStep("Blocaje", "/ainpc demo next $regionId", "urmatoarea comanda reala este clara"),
            ExperimentalRunStep("Mapping", "/ainpc world places $regionId", "satul semantic este vizibil"),
            ExperimentalRunStep("Plan", "/ainpc world settlement plan $regionId 5", "spawn-ul este planificat fara scriere"),
            ExperimentalRunStep("Spawn", "/ainpc world settlement spawn $regionId 5", "NPC-urile demo apar daca lipseau"),
            ExperimentalRunStep("NPC", "/ainpc list", "alege un NPC pentru restul demo-ului"),
            ExperimentalRunStep("Rutina", "/ainpc routine status nearest", "NPC-ul explica home/work/social"),
            ExperimentalRunStep("Dialog", "click dreapta + mesaj scurt", "raspuns sau fallback stabil"),
            ExperimentalRunStep("Quest", "/ainpc quest nearest", "oferta sau mesaj clar"),
            ExperimentalRunStep("Accept", "/ainpc quest accept nearest", "progresia playerului porneste"),
            ExperimentalRunStep("Progress", "/ainpc progression stored $playerName", "progres inspectabil"),
            ExperimentalRunStep("Story", "/ainpc story events $regionId", "story event/state sau WARN asumat"),
            ExperimentalRunStep("Audit", "/ainpc audit all", "fara erori critice ascunse"),
            ExperimentalRunStep("Export", "/ainpc debugdump all", "dovezi pentru investigatie"),
            ExperimentalRunStep("Restart", "/ainpc demo restart $regionId", "gate explicit pentru persistenta")
        )
    }

    private fun sendExperimentalFallbackPlan(
        plugin: AINPCPlugin,
        sender: CommandSender,
        regionId: String,
        playerName: String
    ) {
        plugin.messageUtils.send(sender, "&6Fallback experimental:")
        experimentalFallbackRows(regionId, playerName).forEach { row ->
            plugin.messageUtils.send(sender, "&e${row.problem}: &f${row.fallback}")
            plugin.messageUtils.send(sender, "&8   ${row.command}")
        }
    }

    private fun experimentalFallbackRows(regionId: String, playerName: String): List<ExperimentalFallbackRow> {
        return listOf(
            ExperimentalFallbackRow(
                "Mapping lipsa",
                "creeaza mapping demo intr-o zona plata",
                "/ainpc world demo create $regionId"
            ),
            ExperimentalFallbackRow(
                "NPC lipsa",
                "foloseste settlement spawn sau creeaza manual 3 NPC-uri",
                "/ainpc world settlement spawn $regionId 5"
            ),
            ExperimentalFallbackRow(
                "Bindings incomplete",
                "leaga manual NPC-ul la home/work/social",
                "/ainpc world bind npc nearest <homePlaceId> <workPlaceId> <socialPlaceId>"
            ),
            ExperimentalFallbackRow(
                "Quest lipsa",
                "verifica addonul de scenariu si quest definitions",
                "/ainpc audit quest"
            ),
            ExperimentalFallbackRow(
                "Progress lipsa",
                "accepta o progresie si inspecteaza playerul curent",
                "/ainpc progression stored $playerName"
            ),
            ExperimentalFallbackRow(
                "Story lipsa",
                "ruleaza scenariu cu story action sau marcheaza WARN",
                "/ainpc story events $regionId"
            ),
            ExperimentalFallbackRow(
                "Dialog AI lipsa",
                "ruleaza fara cheie externa si accepta fallback determinist",
                "click dreapta pe NPC + mesaj scurt"
            ),
            ExperimentalFallbackRow(
                "Restart nesigur",
                "ruleaza gate-ul de persistenta inainte de milestone",
                "/ainpc demo restart $regionId"
            )
        )
    }

    private fun sendExperimentalEvidenceMap(
        plugin: AINPCPlugin,
        sender: CommandSender,
        regionId: String,
        playerName: String
    ) {
        plugin.messageUtils.send(sender, "&6Evidence map experimental:")
        experimentalEvidenceRows(regionId, playerName).forEachIndexed { index, row ->
            plugin.messageUtils.send(sender, "&e${index + 1}. &6${row.title}: &f${row.command}")
            plugin.messageUtils.send(sender, "&8   ${row.reason}")
        }
    }

    private fun experimentalEvidenceRows(regionId: String, playerName: String): List<ExperimentalEvidenceRow> {
        return listOf(
            ExperimentalEvidenceRow("Plugin load", "/plugins", "demonstreaza instalare core + addon"),
            ExperimentalEvidenceRow("Readiness", "/ainpc demo status $regionId", "arata PASS/WARN/FAIL si next steps"),
            ExperimentalEvidenceRow("World mapping", "/ainpc world places $regionId", "arata locurile semantice"),
            ExperimentalEvidenceRow("Bindings", "/ainpc world bindings list 20", "arata home/work/social persistat"),
            ExperimentalEvidenceRow("NPC lifecycle", "/ainpc list", "arata NPC-urile incarcate"),
            ExperimentalEvidenceRow("Routine", "/ainpc routine status nearest", "arata slotul curent"),
            ExperimentalEvidenceRow("Quest offer", "/ainpc quest nearest", "arata oferta sau lipsa controlata"),
            ExperimentalEvidenceRow("Quest state", "/ainpc quest status nearest", "arata progresul curent"),
            ExperimentalEvidenceRow("Progression", "/ainpc progression stored $playerName", "arata progresii persistate"),
            ExperimentalEvidenceRow("Definitions", "/ainpc progression definitions", "arata diversitatea mecanicilor"),
            ExperimentalEvidenceRow("Story", "/ainpc story events $regionId", "arata context/eveniment/state"),
            ExperimentalEvidenceRow("Audit", "/ainpc audit all", "arata lipsa erorilor critice"),
            ExperimentalEvidenceRow("Debugdump", "/ainpc debugdump all", "arata export investigabil"),
            ExperimentalEvidenceRow("Restart gate", "/ainpc demo restart $regionId", "arata pasii de persistenta")
        )
    }

    private fun sendExperimentalStopConditions(plugin: AINPCPlugin, sender: CommandSender, regionId: String) {
        plugin.messageUtils.send(sender, "&6Stop conditions experimentale:")
        experimentalStopConditions(regionId).forEachIndexed { index, condition ->
            plugin.messageUtils.send(sender, "&e${index + 1}. &f${condition}")
        }
    }

    private fun experimentalStopConditions(regionId: String): List<String> {
        return listOf(
            "opreste demo-ul daca /plugins nu arata AINPCPlugin enabled",
            "opreste demo-ul daca /ainpc audit db raporteaza eroare critica",
            "opreste demo-ul daca /ainpc world places $regionId nu gaseste regiunea dupa save/restart",
            "opreste demo-ul daca settlement spawn creeaza duplicate evidente",
            "opreste demo-ul daca NPC-urile demo dispar dupa restart",
            "opreste demo-ul daca /ainpc quest nearest sau accept crapa",
            "opreste demo-ul daca /ainpc progression stored <player> nu poate inspecta progresul",
            "opreste demo-ul daca debugdump expune secrete",
            "opreste demo-ul daca AI-ul extern este necesar pentru progres logic",
            "opreste demo-ul daca rollback-ul manual cere editare directa in DB live"
        )
    }

    private fun sendExperimentalRiskNotes(plugin: AINPCPlugin, sender: CommandSender, report: DemoReadinessReport) {
        val blockers = report.checks.filter { it.status == DemoReadinessStatus.FAIL }
        val warnings = report.checks.filter { it.status == DemoReadinessStatus.WARN }
        plugin.messageUtils.send(sender, "&6Riscuri experimentale:")
        when {
            blockers.isNotEmpty() -> {
                plugin.messageUtils.send(sender, "&cBlocaje detectate:")
                blockers.take(6).forEach { check ->
                    plugin.messageUtils.send(sender, "&7- &e${check.name}: &f${check.detail}")
                }
            }

            warnings.isNotEmpty() -> {
                plugin.messageUtils.send(sender, "&eWarnings detectate:")
                warnings.take(6).forEach { check ->
                    plugin.messageUtils.send(sender, "&7- &e${check.name}: &f${check.detail}")
                }
            }

            else -> {
                plugin.messageUtils.send(sender, "&aNu sunt blocaje in snapshot-ul experimental.")
            }
        }
        if (report.nextSteps.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&6Next experimental:")
            report.nextSteps.forEach { step -> plugin.messageUtils.send(sender, "&7- &f$step") }
        }
    }

    private fun sendDemoPhases(plugin: AINPCPlugin, sender: CommandSender, regionId: String, playerName: String) {
        plugin.messageUtils.send(sender, "&6=== Demo phases: &f$regionId&6 ===")
        plugin.messageUtils.send(sender, "&7Checklist read-only. Pentru flux procedural: &f/ainpc demo script $regionId $playerName")
        demoPhases(regionId, playerName).forEach { phase ->
            plugin.messageUtils.send(sender, "&e${phase.id} &6${phase.title}: &f${phase.gate}")
            plugin.messageUtils.send(sender, "&8   ${phase.commands.joinToString(" -> ")}")
        }
    }

    private fun demoPhases(regionId: String, playerName: String): List<DemoPhaseStep> {
        return listOf(
            DemoPhaseStep("D0", "Scope", "server Paper dedicat, lume de test, regionId stabilit", listOf("noteaza ServerDir/PlayerName")),
            DemoPhaseStep("D1", "Build si config", "pluginul si addonul pornesc fara erori critice", listOf("/plugins", "/ainpc audit db")),
            DemoPhaseStep("D2", "Sat semantic", "regiune, places si nodes inspectabile", listOf("/ainpc world demo create $regionId", "/ainpc world places $regionId")),
            DemoPhaseStep("D3", "NPC si bindings", "3-5 NPC-uri cu home/work/social", listOf("/ainpc world settlement plan $regionId 5", "/ainpc world settlement spawn $regionId 5", "/ainpc list")),
            DemoPhaseStep("D4", "Rutina si UX", "NPC-ul are rutina si actiune clara", listOf("/ainpc routine status nearest", "/ainpc info nearest")),
            DemoPhaseStep("D5", "Quest si progression", "un quest si o mecanica non-quest sunt inspectabile", listOf("/ainpc quest nearest", "/ainpc progression stored $playerName")),
            DemoPhaseStep("D6", "Story", "story context/event/state poate fi verificat", listOf("/ainpc story context $playerName nearest", "/ainpc story events $regionId")),
            DemoPhaseStep("D7", "Dialog", "click/chat merge cu fallback sigur", listOf("click dreapta pe NPC", "trimite mesaj scurt")),
            DemoPhaseStep("D8", "Repetitie", "audit/debugdump si restart nu pierd starea", listOf("/ainpc audit all", "/ainpc debugdump all", "/ainpc demo status $regionId")),
            DemoPhaseStep("D9", "Demo tester", "testerul poate urma pasii fara DB edit", listOf("/ainpc demo script $regionId $playerName"))
        )
    }

    private fun sendDemoEvidence(plugin: AINPCPlugin, sender: CommandSender, regionId: String, playerName: String) {
        plugin.messageUtils.send(sender, "&6=== Demo evidence: &f$regionId&6 ===")
        plugin.messageUtils.send(sender, "&7Checklist read-only pentru dovezile milestone-ului.")
        demoEvidence(regionId, playerName).forEachIndexed { index, item ->
            plugin.messageUtils.send(sender, "&e${index + 1}. &6${item.title}: &f${item.command}")
            plugin.messageUtils.send(sender, "&8   ${item.evidence}")
        }
    }

    private fun sendDemoRunbook(plugin: AINPCPlugin, sender: CommandSender, regionId: String, playerName: String) {
        plugin.messageUtils.send(sender, "&6=== Demo runbook: &f$regionId&6 ===")
        plugin.messageUtils.send(sender, "&7Ghid operational scurt. Foloseste modurile dedicate pentru detaliu.")
        demoRunbook(regionId, playerName).forEachIndexed { index, item ->
            plugin.messageUtils.send(sender, "&e${index + 1}. &6${item.title}: &f${item.command}")
            plugin.messageUtils.send(sender, "&8   ${item.note}")
        }
    }

    private fun demoRunbook(regionId: String, playerName: String): List<DemoRunbookItem> {
        return listOf(
            DemoRunbookItem("Baseline", "/ainpc demo status $regionId", "porneste de la readiness si noteaza PASS/WARN/FAIL"),
            DemoRunbookItem("Faze", "/ainpc demo phases $regionId $playerName", "verifica D0-D9 si vezi ce gate lipsea"),
            DemoRunbookItem("Script", "/ainpc demo script $regionId $playerName", "urmeaza fluxul de test cap-coada"),
            DemoRunbookItem("Dovezi", "/ainpc demo evidence $regionId $playerName", "captureaza output-urile necesare pentru milestone"),
            DemoRunbookItem("Final audit", "/ainpc audit all", "verifica rapid erorile critice"),
            DemoRunbookItem("Final export", "/ainpc debugdump all", "export pentru investigatie")
        )
    }

    private fun sendDemoSmoke(plugin: AINPCPlugin, sender: CommandSender, regionId: String, playerName: String) {
        plugin.messageUtils.send(sender, "&6=== Demo smoke: &f$regionId&6 ===")
        plugin.messageUtils.send(sender, "&7Verificare rapida read-only: snapshot readiness + checklist manual.")
        val report = DemoReadinessService(plugin).build(regionId)
        sendCompactReadinessSnapshot(plugin, sender, report)
        plugin.messageUtils.send(sender, "&6Checklist manual:")
        demoSmoke(regionId, playerName).forEachIndexed { index, item ->
            plugin.messageUtils.send(sender, "&e${index + 1}. &f${item.command}")
            plugin.messageUtils.send(sender, "&8   ${item.expected}")
        }
    }

    private fun sendCompactReadinessSnapshot(plugin: AINPCPlugin, sender: CommandSender, report: DemoReadinessReport) {
        plugin.messageUtils.send(
            sender,
            "&7Readiness: ${report.overallStatus.color}${report.overallStatus.label}" +
                    " &7(&f${report.passedChecks}&7/&f${report.checks.size}&7 checks)"
        )
        plugin.messageUtils.send(
            sender,
            "&7World &f${report.regionPlaceCount}&7 places/&f${report.regionNodeCount}&7 nodes, " +
                    "NPC demo &f${report.spawnedDemoNpcCount}&7/&f${report.demoNpcCount}&7 spawnati, " +
                    "bindings complete &f${report.completeRegionBindingCount}&7/&f${report.regionBindingCount}"
        )
        val blockers = report.checks.filter { it.status == DemoReadinessStatus.FAIL }
        val warnings = report.checks.filter { it.status == DemoReadinessStatus.WARN }
        if (blockers.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&cBlocaje:")
            blockers.take(4).forEach { check ->
                plugin.messageUtils.send(sender, "&7- &e${check.name}: &f${check.detail}")
            }
        } else if (warnings.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&eWarnings:")
            warnings.take(4).forEach { check ->
                plugin.messageUtils.send(sender, "&7- &e${check.name}: &f${check.detail}")
            }
        }
        if (report.nextSteps.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&6Next:")
            report.nextSteps.take(3).forEach { step ->
                plugin.messageUtils.send(sender, "&7- &f$step")
            }
        }
    }

    private fun sendDemoNext(plugin: AINPCPlugin, sender: CommandSender, report: DemoReadinessReport) {
        plugin.messageUtils.send(sender, "&6=== Demo next: &f${report.regionId}&6 ===")
        sendCompactReadinessSnapshot(plugin, sender, report)
        val blockers = report.checks.filter { it.status == DemoReadinessStatus.FAIL }
        val warnings = report.checks.filter { it.status == DemoReadinessStatus.WARN }
        if (blockers.isEmpty() && warnings.isEmpty()) {
            plugin.messageUtils.send(sender, "&aNu sunt blocaje in readiness. Continua cu smoke manual si restart gate.")
            plugin.messageUtils.send(sender, "&7- &f/ainpc demo smoke ${report.regionId} <player>")
            plugin.messageUtils.send(sender, "&7- &f/ainpc audit all")
            plugin.messageUtils.send(sender, "&7- &f/ainpc debugdump all")
            return
        }

        if (blockers.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&cBlocaje de rezolvat inainte de demo:")
            blockers.forEach { check ->
                plugin.messageUtils.send(sender, "&7- &e${check.name}: &f${check.detail}")
            }
        }
        if (warnings.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&eWarnings acceptabile doar cu nota de backlog:")
            warnings.forEach { check ->
                plugin.messageUtils.send(sender, "&7- &e${check.name}: &f${check.detail}")
            }
        }
        if (report.nextSteps.isNotEmpty()) {
            plugin.messageUtils.send(sender, "&6Comenzi urmatoare:")
            report.nextSteps.forEach { step -> plugin.messageUtils.send(sender, "&7- &f$step") }
        }
    }

    private fun demoSmoke(regionId: String, playerName: String): List<DemoScriptStep> {
        return listOf(
            DemoScriptStep("/plugins", "AINPCPlugin si addonul de scenariu sunt loaded"),
            DemoScriptStep("/ainpc demo status $regionId", "readiness baseline"),
            DemoScriptStep("/ainpc world places $regionId", "mapping semantic vizibil"),
            DemoScriptStep("/ainpc list", "3-5 NPC-uri prezente in demo"),
            DemoScriptStep("/ainpc routine status nearest", "home/work/social disponibil"),
            DemoScriptStep("/ainpc quest nearest", "oferta de quest sau mesaj clar"),
            DemoScriptStep("/ainpc progression stored $playerName", "progres inspectabil"),
            DemoScriptStep("/ainpc story events $regionId", "story event sau WARN explicit"),
            DemoScriptStep("/ainpc audit all", "fara erori critice"),
            DemoScriptStep("/ainpc debugdump all", "export disponibil"),
            DemoScriptStep("/ainpc demo status $regionId", "readiness final dupa smoke")
        )
    }

    private fun sendDemoSummary(plugin: AINPCPlugin, sender: CommandSender, regionId: String, playerName: String) {
        plugin.messageUtils.send(sender, "&6=== Demo summary: &f$regionId&6 ===")
        plugin.messageUtils.send(sender, "&7Rezumat rapid read-only pentru urmatorul pas operational.")
        plugin.messageUtils.send(sender, "&e1. &f/ainpc demo status $regionId")
        plugin.messageUtils.send(sender, "&e2. &f/ainpc demo next $regionId")
        plugin.messageUtils.send(sender, "&e3. &f/ainpc demo smoke $regionId $playerName")
        plugin.messageUtils.send(sender, "&e4. &f/ainpc demo evidence $regionId $playerName")
        plugin.messageUtils.send(sender, "&e5. &f/ainpc demo restart $regionId")
        plugin.messageUtils.send(sender, "&e6. &f/ainpc demo commands $regionId $playerName")
        plugin.messageUtils.send(sender, "&e7. &f/ainpc demo runbook $regionId $playerName")
    }

    private fun sendDemoCommands(plugin: AINPCPlugin, sender: CommandSender, regionId: String, playerName: String) {
        plugin.messageUtils.send(sender, "&6=== Demo commands: &f$regionId&6 ===")
        plugin.messageUtils.send(sender, "&7Lista compacta read-only/procedurala pentru rulare manuala.")
        demoCommandLines(regionId, playerName).forEach { line ->
            plugin.messageUtils.send(sender, "&f$line")
        }
    }

    private fun demoCommandLines(regionId: String, playerName: String): List<String> {
        return listOf(
            "/plugins",
            "/ainpc demo status $regionId",
            "/ainpc demo next $regionId",
            "/ainpc world demo create $regionId",
            "/ainpc world places $regionId",
            "/ainpc world settlement plan $regionId 5",
            "/ainpc world settlement spawn $regionId 5",
            "/ainpc list",
            "/ainpc routine status nearest",
            "/ainpc quest nearest",
            "/ainpc quest accept nearest",
            "/ainpc progression stored $playerName",
            "/ainpc story events $regionId",
            "/ainpc audit all",
            "/ainpc debugdump all",
            "/ainpc demo restart $regionId",
            "/ainpc demo status $regionId"
        )
    }

    private fun sendDemoRestart(plugin: AINPCPlugin, sender: CommandSender, regionId: String) {
        plugin.messageUtils.send(sender, "&6=== Demo restart gate: &f$regionId&6 ===")
        plugin.messageUtils.send(sender, "&7Checklist read-only pentru persistenta inainte si dupa restart.")
        demoRestartGate(regionId).forEachIndexed { index, step ->
            plugin.messageUtils.send(sender, "&e${index + 1}. &f${step.command}")
            plugin.messageUtils.send(sender, "&8   ${step.expected}")
        }
    }

    private fun demoRestartGate(regionId: String): List<DemoScriptStep> {
        return listOf(
            DemoScriptStep("/ainpc demo status $regionId", "baseline inainte de restart, cu PASS/WARN/FAIL notat"),
            DemoScriptStep("/ainpc audit all", "fara erori critice inainte de oprire"),
            DemoScriptStep("/ainpc debugdump all", "export disponibil pentru comparatie"),
            DemoScriptStep("/ainpc world save", "mapping-ul este salvat explicit"),
            DemoScriptStep("opreste serverul Paper", "shutdown normal, fara kill fortat"),
            DemoScriptStep("porneste serverul Paper", "pluginul si addonul pornesc fara stacktrace repetat"),
            DemoScriptStep("/ainpc world places $regionId", "regiunea si place-urile demo exista dupa restart"),
            DemoScriptStep("/ainpc list", "NPC-urile demo sunt incarcate dupa restart"),
            DemoScriptStep("/ainpc world bindings list 20", "bindings home/work/social sunt persistate"),
            DemoScriptStep("/ainpc demo status $regionId", "readiness final dupa restart")
        )
    }

    private fun demoEvidence(regionId: String, playerName: String): List<DemoEvidenceItem> {
        return listOf(
            DemoEvidenceItem("Plugin load", "/plugins", "captura cu AINPCPlugin si addonul de scenariu enabled"),
            DemoEvidenceItem("Readiness baseline", "/ainpc demo status $regionId", "raport PASS/WARN/FAIL inainte sau imediat dupa setup"),
            DemoEvidenceItem("World mapping", "/ainpc world places $regionId", "lista cu regiunea, casele, workplaces si social hub"),
            DemoEvidenceItem("NPC lifecycle", "/ainpc list", "3-5 NPC-uri vizibile si persistente pentru demo"),
            DemoEvidenceItem("Routine", "/ainpc routine status nearest", "home/work/social si slotul curent sau urmator"),
            DemoEvidenceItem("Quest", "/ainpc quest nearest", "oferta de quest sau mesaj clar daca lipseste oferta"),
            DemoEvidenceItem("Progression", "/ainpc progression stored $playerName", "progres quest/non-quest dupa interactiune"),
            DemoEvidenceItem("Story", "/ainpc story events $regionId", "story event sau dovada ca ramane WARN pentru backlog"),
            DemoEvidenceItem("Audit", "/ainpc audit all", "fara erori critice ascunse inainte de restart"),
            DemoEvidenceItem("Debugdump", "/ainpc debugdump all", "folder/export pastrat pentru investigatie, fara secrete"),
            DemoEvidenceItem("Final readiness", "/ainpc demo status $regionId", "raport final dupa flux si, daca e milestone, dupa restart")
        )
    }
}

private data class DemoScriptStep(
    val command: String,
    val expected: String
)

private data class DemoPhaseStep(
    val id: String,
    val title: String,
    val gate: String,
    val commands: List<String>
)

private data class DemoEvidenceItem(
    val title: String,
    val command: String,
    val evidence: String
)

private data class DemoRunbookItem(
    val title: String,
    val command: String,
    val note: String
)

private data class ExperimentalScoreRow(
    val label: String,
    val status: DemoReadinessStatus,
    val value: String,
    val note: String
)

private data class ExperimentalGateRow(
    val phase: String,
    val title: String,
    val status: DemoReadinessStatus,
    val action: String,
    val evidence: String
)

private data class ExperimentalRunStep(
    val title: String,
    val command: String,
    val expected: String
)

private data class ExperimentalFallbackRow(
    val problem: String,
    val fallback: String,
    val command: String
)

private data class ExperimentalEvidenceRow(
    val title: String,
    val command: String,
    val reason: String
)

private data class ExperimentalFiveTask(
    val title: String,
    val status: DemoReadinessStatus,
    val goal: String,
    val acceptance: String,
    val commands: List<String>,
    val risks: List<String>,
    val fallbacks: List<String>
)

private data class ExperimentalTwentyFiveTask(
    val title: String,
    val status: DemoReadinessStatus,
    val acceptance: String,
    val command: String,
    val risk: String,
    val fallback: String
)

private data class ExperimentalDeepTask(
    val title: String,
    val status: DemoReadinessStatus,
    val goal: String,
    val action: String,
    val evidence: String,
    val risk: String,
    val next: String
)

private data class ExperimentalOpsTask(
    val title: String,
    val status: DemoReadinessStatus,
    val command: String,
    val passSignal: String,
    val failSignal: String,
    val ownerHint: String
)

private class DemoReadinessService(private val plugin: AINPCPlugin) {
    fun build(regionId: String): DemoReadinessReport {
        val worldAdmin = plugin.platform.worldAdmin
        val regions = worldAdmin.regions.toList()
        val region = regions.firstOrNull { it.id().equals(regionId, ignoreCase = true) }
        val places = worldAdmin.places.toList()
        val nodes = worldAdmin.nodes.toList()
        val regionPlaces = places.filter { it.regionId().equals(regionId, ignoreCase = true) }
        val regionPlaceIds = regionPlaces.map { it.id().lowercase() }.toSet()
        val regionNodes = nodes.filter { node ->
            node.regionId().equals(regionId, ignoreCase = true) ||
                    (node.placeId().isNotBlank() && regionPlaceIds.contains(node.placeId().lowercase()))
        }

        val npcList = plugin.npcManager.allNPCs.toList()
        val bindings = loadBindings()
        val regionBindings = bindings.filter { it.touchesAnyPlace(regionPlaceIds) }
        val regionBindingNpcIds = regionBindings.map { it.npcId() }.toSet()
        val demoNpcCount = regionBindingNpcIds.size
        val spawnedDemoNpcCount = npcList.count { npc -> npc.databaseId in regionBindingNpcIds && npc.spawned }
        val definitions = plugin.progressionService.getDefinitions()
        val storedProgressionCount = loadStoredProgressionCount()
        val storyEventCount = loadStoryEventCount(regionId)
        val regionStoryStatePresent = loadRegionStoryStatePresent(regionId)

        val checkBuilder = DemoReadinessCheckBuilder()
        val houseCount = regionPlaces.count { it.placeType() == PlaceType.HOUSE }
        val workPlaceCount = regionPlaces.count { it.isWorkPlace() }
        val socialPlaceCount = regionPlaces.count { it.isSocialPlace() }
        val completeRegionBindingCount = regionBindings.count { it.hasCompleteRegionBinding(regionPlaceIds) }
        val questDefinitions = definitions.count { it.isQuestDefinition() }
        val nonQuestDefinitions = definitions.size - questDefinitions

        checkBuilder.require(
            "world_admin",
            worldAdmin.isEnabled,
            "world_admin.enabled=${worldAdmin.isEnabled}"
        )
        checkBuilder.require(
            "region",
            region != null,
            if (region != null) "regiunea $regionId exista" else "lipseste regiunea $regionId"
        )
        checkBuilder.require(
            "places",
            regionPlaces.size >= 5,
            "${regionPlaces.size} places in $regionId"
        )
        checkBuilder.require(
            "nodes",
            regionNodes.size >= 10,
            "${regionNodes.size} nodes in $regionId"
        )
        checkBuilder.require(
            "house_work_social",
            houseCount >= 3 && workPlaceCount >= 1 && socialPlaceCount >= 1,
            "case=$houseCount work=$workPlaceCount social=$socialPlaceCount"
        )
        checkBuilder.require(
            "npc_count",
            demoNpcCount in 3..5,
            "$demoNpcCount NPC-uri legate de $regionId; tinta demo este 3-5"
        )
        checkBuilder.warn(
            "spawned_npc",
            demoNpcCount > 0 && spawnedDemoNpcCount >= minOf(3, demoNpcCount),
            "$spawnedDemoNpcCount NPC-uri demo spawnate din $demoNpcCount"
        )
        checkBuilder.require(
            "npc_bindings",
            demoNpcCount > 0 && completeRegionBindingCount >= minOf(3, demoNpcCount),
            "$completeRegionBindingCount bindings complete in $regionId din ${regionBindings.size}"
        )
        checkBuilder.require(
            "quest_definitions",
            questDefinitions >= 1,
            "$questDefinitions quest definitions"
        )
        checkBuilder.require(
            "progression_diversity",
            nonQuestDefinitions >= 1,
            "$nonQuestDefinitions non-quest definitions"
        )
        checkBuilder.warn(
            "stored_progress",
            storedProgressionCount > 0,
            "$storedProgressionCount progresii persistate"
        )
        checkBuilder.warn(
            "story",
            storyEventCount > 0 || regionStoryStatePresent,
            "events=$storyEventCount region_state=$regionStoryStatePresent"
        )
        checkBuilder.require(
            "audit_debugdump_next",
            true,
            "ruleaza /ainpc audit all si /ainpc debugdump all dupa fluxul manual"
        )

        val checks = checkBuilder.checks()
        return DemoReadinessReport(
            regionId = regionId,
            regionCount = regions.size,
            placeCount = places.size,
            nodeCount = nodes.size,
            regionPlaceCount = regionPlaces.size,
            regionNodeCount = regionNodes.size,
            totalNpcCount = npcList.size,
            demoNpcCount = demoNpcCount,
            spawnedDemoNpcCount = spawnedDemoNpcCount,
            bindingCount = bindings.size,
            regionBindingCount = regionBindings.size,
            completeRegionBindingCount = completeRegionBindingCount,
            questDefinitionCount = questDefinitions,
            nonQuestDefinitionCount = nonQuestDefinitions,
            storedProgressionCount = storedProgressionCount,
            storyEventCount = storyEventCount,
            regionStoryStatePresent = regionStoryStatePresent,
            checks = checks,
            nextSteps = nextSteps(checks, regionId)
        )
    }

    private fun loadBindings() = try {
        plugin.npcWorldBindingService.listBindings(MAX_BINDINGS_TO_SCAN)
    } catch (_: SQLException) {
        emptyList()
    }

    private fun loadStoredProgressionCount() = try {
        plugin.progressionService.getStoredProgressionSummary(null, null).rowCount()
    } catch (_: SQLException) {
        0
    }

    private fun loadStoryEventCount(regionId: String) = try {
        plugin.storyStateService.listRecentEvents(regionId, null, 5).size
    } catch (_: SQLException) {
        0
    }

    private fun loadRegionStoryStatePresent(regionId: String) = try {
        plugin.storyStateService.getRegionState(regionId).isPresent
    } catch (_: SQLException) {
        false
    }

    private fun nextSteps(checks: List<DemoReadinessCheck>, regionId: String): List<String> {
        return checks.filter { it.status != DemoReadinessStatus.PASS }
            .map { check ->
                when (check.name) {
                    "region", "places", "nodes", "house_work_social" ->
                        "/ainpc world demo create $regionId -> /ainpc world save"

                    "npc_count", "spawned_npc", "npc_bindings" ->
                        "/ainpc world settlement plan $regionId 5 -> /ainpc world settlement spawn $regionId 5"

                    "quest_definitions", "progression_diversity" ->
                        "/ainpc audit quest si verifica addonul de scenariu"

                    "stored_progress" ->
                        "accepta/progreseaza un quest sau o progresie, apoi /ainpc progression stored <player>"

                    "story" ->
                        "ruleaza un scenariu cu story event, apoi /ainpc story events $regionId"

                    else ->
                        check.detail
                }
            }
            .distinct()
            .take(6)
    }

    private fun WorldPlaceInfo.isWorkPlace(): Boolean {
        return when (placeType()) {
            PlaceType.FORGE,
            PlaceType.SHOP,
            PlaceType.FARM,
            PlaceType.MARKET,
            PlaceType.TAVERN,
            PlaceType.CASTLE_ROOM,
            PlaceType.CAMP -> true

            else -> hasAnyTag("work", "workplace", "job", "forge", "farm", "market", "shop")
        }
    }

    private fun WorldPlaceInfo.isSocialPlace(): Boolean {
        return when (placeType()) {
            PlaceType.MARKET,
            PlaceType.TAVERN,
            PlaceType.CAMP -> true

            else -> hasAnyTag("social", "meeting", "meeting_point", "market", "tavern", "altar")
        }
    }

    private fun WorldPlaceInfo.hasAnyTag(vararg candidates: String): Boolean {
        return candidates.any { candidate -> hasTag(candidate) }
    }

    private fun NpcWorldBinding.touchesAnyPlace(regionPlaceIds: Set<String>): Boolean {
        return regionPlaceIds.contains(homePlaceId().lowercase()) ||
                regionPlaceIds.contains(workPlaceId().lowercase()) ||
                regionPlaceIds.contains(socialPlaceId().lowercase())
    }

    private fun NpcWorldBinding.hasCompleteRegionBinding(regionPlaceIds: Set<String>): Boolean {
        return regionPlaceIds.contains(homePlaceId().lowercase()) &&
                regionPlaceIds.contains(workPlaceId().lowercase()) &&
                regionPlaceIds.contains(socialPlaceId().lowercase())
    }

    private fun ProgressionDefinition.isQuestDefinition(): Boolean {
        return mechanicId().equals("quest", ignoreCase = true) ||
                kind().equals("quest", ignoreCase = true) ||
                baseType().equals("QUEST", ignoreCase = true)
    }
}

private class DemoReadinessCheckBuilder {
    private val checks = mutableListOf<DemoReadinessCheck>()

    fun require(name: String, passed: Boolean, detail: String) {
        checks += DemoReadinessCheck(name, detail, if (passed) DemoReadinessStatus.PASS else DemoReadinessStatus.FAIL)
    }

    fun warn(name: String, passed: Boolean, detail: String) {
        checks += DemoReadinessCheck(name, detail, if (passed) DemoReadinessStatus.PASS else DemoReadinessStatus.WARN)
    }

    fun checks(): List<DemoReadinessCheck> = checks.toList()
}

private data class DemoReadinessReport(
    val regionId: String,
    val regionCount: Int,
    val placeCount: Int,
    val nodeCount: Int,
    val regionPlaceCount: Int,
    val regionNodeCount: Int,
    val totalNpcCount: Int,
    val demoNpcCount: Int,
    val spawnedDemoNpcCount: Int,
    val bindingCount: Int,
    val regionBindingCount: Int,
    val completeRegionBindingCount: Int,
    val questDefinitionCount: Int,
    val nonQuestDefinitionCount: Int,
    val storedProgressionCount: Int,
    val storyEventCount: Int,
    val regionStoryStatePresent: Boolean,
    val checks: List<DemoReadinessCheck>,
    val nextSteps: List<String>
) {
    val passedChecks: Int = checks.count { it.status == DemoReadinessStatus.PASS }
    val overallStatus: DemoReadinessStatus = when {
        checks.any { it.status == DemoReadinessStatus.FAIL } -> DemoReadinessStatus.FAIL
        checks.any { it.status == DemoReadinessStatus.WARN } -> DemoReadinessStatus.WARN
        else -> DemoReadinessStatus.PASS
    }

    fun checkStatus(name: String): DemoReadinessStatus {
        return checks.firstOrNull { it.name == name }?.status ?: DemoReadinessStatus.WARN
    }

    fun checkDetail(name: String): String {
        return checks.firstOrNull { it.name == name }?.detail ?: "n/a"
    }

    fun worstStatus(vararg names: String): DemoReadinessStatus {
        val statuses = names.map { checkStatus(it) }
        return when {
            statuses.any { it == DemoReadinessStatus.FAIL } -> DemoReadinessStatus.FAIL
            statuses.any { it == DemoReadinessStatus.WARN } -> DemoReadinessStatus.WARN
            else -> DemoReadinessStatus.PASS
        }
    }
}

private data class DemoReadinessCheck(
    val name: String,
    val detail: String,
    val status: DemoReadinessStatus
)

private enum class DemoReadinessStatus(val label: String, val mark: String, val color: String) {
    PASS("PASS", "[OK]", "&a"),
    WARN("WARN", "[WARN]", "&e"),
    FAIL("FAIL", "[FAIL]", "&c")
}
