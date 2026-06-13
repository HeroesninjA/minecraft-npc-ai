@file:JvmName("AINPCCommandDisplay")

package ro.ainpc.commands

import org.bukkit.command.CommandSender
import ro.ainpc.AINPCPlugin
import ro.ainpc.world.mapping.MappingDraft
import ro.ainpc.world.mapping.MappingWandService
import java.util.Locale

lateinit var ainpcCommandDisplayPlugin: AINPCPlugin

fun initAinpcCommandDisplayPlugin(plugin: AINPCPlugin) {
    ainpcCommandDisplayPlugin = plugin
}

fun sendQuestUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc quest log [jucator] [active|current|tracked|quest|contract|duty|bounty|event|main|side|repeatable|completed|failed|archived|all]")
    msg.send(sender, "&e/ainpc quest track [start|stop] [questCode|templateId] [jucator]")
    msg.send(sender, "&e/ainpc quest status")
    msg.send(sender, "&e/ainpc quest <numeNpc> [jucator]")
    msg.send(sender, "&e/ainpc quest nearest [jucator]")
    msg.send(sender, "&e/ainpc quest accept|da [numeNpc|nearest] [jucator]")
    msg.send(sender, "&e/ainpc quest decline|nu [numeNpc|nearest] [jucator]")
    msg.send(sender, "&e/ainpc quest abandon <numeNpc>|nearest|tracked|<questCode|templateId> [jucator]")
    msg.send(sender, "&e/ainpc quest status <numeNpc>|nearest|<questCode|templateId> [jucator]")
    msg.send(sender, "&e/ainpc quest progress [tracked|questCode|templateId] [jucator]")
    msg.send(sender, "&e/ainpc quest debug <tracked|questCode|templateId> [jucator]")
    msg.send(sender, "&e/ainpc quest reset <numeNpc> [jucator]")
    msg.send(sender, "&e/ainpc quest complete <numeNpc> [jucator]")
    msg.send(sender, "&e/ainpc quest anchors [jucator|uuid|all] [templateId|questCode]")
}

fun sendProgressionUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc progression gui [quest|contract|duty|bounty|event|tutorial|ritual|active|all]")
    msg.send(sender, "&e/ainpc progression log [jucator] [quest|contract|duty|bounty|event|tutorial|ritual|active|completed|all]")
    msg.send(sender, "&e/ainpc progression definitions [filter]")
    msg.send(sender, "&e/ainpc progression stored [jucator|uuid|all] [filter] [limit]")
    msg.send(sender, "&e/ainpc progression status <tracked|selector> [jucator]")
    msg.send(sender, "&e/ainpc progression progress [tracked|selector] [jucator]")
    msg.send(sender, "&e/ainpc progression track [start|stop] [selector] [jucator]")
    msg.send(sender, "&e/ainpc progression abandon <tracked|selector> [jucator]")
    msg.send(sender, "&7Selector exemple: &fQ01&7, &fside_quests:Q07&7, &fvillage_contracts:C01&7, &fnpc_duties:D01&7, &flocal_bounties:B01&7, &fvillage_events:E01&7, &fonboarding:T01&7, &fvillage_rituals:R01&7.")
    msg.send(sender, "&7Filtre exemple: &fkind:contract&7, &fkind:duty&7, &fkind:bounty&7, &fkind:event&7, &fkind:tutorial&7, &fkind:ritual&7, &fscenario:investigation&7, &fbase:TRADE_DEAL&7.")
}

fun sendProgressionAliasUsage(sender: CommandSender, alias: ProgressionAliasConfig) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc ${alias.command()} gui [active|current|tracked|completed|failed|archived|all]")
    msg.send(sender, "&e/ainpc ${alias.command()} log [jucator] [active|current|tracked|completed|failed|archived|all]")
    msg.send(sender, "&e/ainpc ${alias.command()} definitions [filter]")
    msg.send(sender, "&e/ainpc ${alias.command()} stored [jucator|uuid|all] [filter] [limit]")
    msg.send(sender, "&e/ainpc ${alias.command()} status <selector> [jucator]")
    msg.send(sender, "&e/ainpc ${alias.command()} progress <selector> [jucator]")
    msg.send(sender, "&e/ainpc ${alias.command()} track [start|stop] [selector] [jucator]")
    msg.send(sender, "&e/ainpc ${alias.command()} abandon <selector> [jucator]")
    msg.send(sender, "&7Selector scurt: &f${alias.shortSelectorExample()} &7devine &f${alias.kind()}:${alias.shortSelectorExample()}&7.")
    msg.send(sender, "&7Filtre exemple: &fkind:${alias.kind()}&7, &fmechanic:${alias.mechanicExample()}&7, &fbase:${alias.baseTypeExample()}&7.")
}

fun sendStoryUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc story context [jucator] [numeNpc|nearest]")
    msg.send(sender, "&e/ainpc story region <regionId>")
    msg.send(sender, "&e/ainpc story place <placeId>")
    msg.send(sender, "&e/ainpc story events <regionId|placeId> [limit]")
    msg.send(sender, "&7Fara NPC tinta, contextul este construit pentru locatia jucatorului.")
}

fun sendWandUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc wand")
    msg.send(sender, "&e/ainpc wand mode <region|place|node|npc_bind|quest_anchor>")
    msg.send(sender, "&e/ainpc wand <pos1|pos2|point|status|inspect>")
    msg.send(sender, "&e/ainpc wand <clear|reset> [pos1|pos2|point|all]")
    msg.send(sender, "&7Click stanga/dreapta cu wand-ul seteaza pos1/pos2; in modurile node/npc_bind/quest_anchor seteaza punctul.")
}

fun sendMapUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc map <region|place|node|npc_bind|quest_anchor> <descriere>")
    msg.send(sender, "&e/ainpc map quest_anchor [player:<jucator|uuid>] <tracked|current|templateId|questCode> <objective_id> [objective_type] [reference]")
    msg.send(sender, "&e/ainpc map <descriere> &7(foloseste modul wand curent)")
    msg.send(sender, "&e/ainpc map preview")
    msg.send(sender, "&e/ainpc map confirm")
    msg.send(sender, "&e/ainpc map cancel")
}

fun sendPatchUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc patch analyze <regionId> [targetPopulation] [profesiiCSV]")
    msg.send(sender, "&e/ainpc patch plan <regionId> [targetPopulation] [profesiiCSV]")
    msg.send(sender, "&e/ainpc patch validate <regionId> [targetPopulation] [profesiiCSV]")
    msg.send(sender,
        "&7Read-only: produce GapReport si PatchPlan, fara constructie si fara scrieri in mapping.")
}

fun sendWorldUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc world whereami [jucator]")
    msg.send(sender, "&e/ainpc world places [regionId]")
    msg.send(sender, "&e/ainpc world region info <regionId>")
    msg.send(sender, "&e/ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>")
    msg.send(sender, "&e/ainpc world place info <placeId>")
    msg.send(sender, "&e/ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>")
    msg.send(sender, "&e/ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]")
    msg.send(sender, "&e/ainpc world scan village [radius] [import] [regionId]")
    msg.send(sender, "&e/ainpc world demo create [regionId]")
    msg.send(sender, "&7  Din consola/RCON foloseste spawn-ul lumii incarcate")
    msg.send(sender, "&e/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]")
    msg.send(sender, "&e/ainpc world bindings [limit]")
    msg.send(sender, "&e/ainpc world bindings npc <numeNpc|nearest|npcId|uuid>")
    msg.send(sender, "&e/ainpc world bindings place <placeId> [limit]")
    msg.send(sender, "&e/ainpc world household <plan|spawn> <homePlaceId> [count]")
    msg.send(sender, "&e/ainpc world household <status|place|resident|list> ...")
    msg.send(sender, "&e/ainpc world settlement <plan|spawn> <regionId> [maxHouses]")
    msg.send(sender, "&e/ainpc world save")
}

fun sendWorldBindingsUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc world bindings [limit]")
    msg.send(sender, "&e/ainpc world bindings list [limit]")
    msg.send(sender, "&e/ainpc world bindings npc <numeNpc|nearest|npcId|uuid>")
    msg.send(sender, "&e/ainpc world bindings place <placeId> [limit]")
    msg.send(sender, "&7Comanda este read-only si inspecteaza tabela persistenta npc_world_bindings.")
}

fun sendWorldHouseholdUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc world household <plan|spawn> <homePlaceId> [count]")
    msg.send(sender, "&e/ainpc world household status <householdId|homePlaceId>")
    msg.send(sender, "&e/ainpc world household place <homePlaceId>")
    msg.send(sender, "&e/ainpc world household resident <npcId|numeNpc|nearest>")
    msg.send(sender, "&e/ainpc world household list [limit]")
    msg.send(sender,
        "&7Comenzile status/place/resident/list sunt read-only si inspecteaza tabelele households.")
}

fun sendMigrationUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc migration households dryrun [limit]")
    msg.send(sender, "&e/ainpc migration households apply [limit]")
    msg.send(sender, "&7Backfill controlat din npc_world_bindings si metadata resident_npc_ids.")
}

fun sendAuditUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc audit &7- ruleaza toate verificarile")
    msg.send(sender, "&e/ainpc audit npc &7- verifica profilurile si ancorele NPC")
    msg.send(sender, "&e/ainpc audit world &7- verifica world mapping")
    msg.send(sender, "&e/ainpc audit db &7- verifica tabelele si profile_data")
    msg.send(sender, "&e/ainpc audit spawn &7- verifica ordinea casa/node/NPC/familie")
    msg.send(sender, "&e/ainpc audit quest &7- verifica quest templates si quest_anchor_bindings")
    msg.send(sender, "&e/ainpc audit quest <strict|full|offline> &7- verifica toate randurile quest_anchor_bindings")
    msg.send(sender, "&e/ainpc audit wand &7- verifica draft-urile wand confirmate recent")
}

fun sendRoutineUsage(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&cUtilizare:")
    msg.send(sender, "&e/ainpc routine tick &7- ruleaza manual rutina pentru NPC-urile active")
    msg.send(sender, "&e/ainpc routine status [numeNpc|nearest] &7- previzualizeaza rutina unui NPC")
}

fun sendHelp(sender: CommandSender) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&6=== AI NPC Plugin - Comenzi ===")
    msg.send(sender, "&e/ainpc create <nume> [ocupatie] [varsta] [gen] [arhetip]")
    msg.send(sender, "&7  Creeaza un NPC nou la locatia ta")
    msg.send(sender, "&e/ainpc delete <nume>")
    msg.send(sender, "&7  Sterge un NPC dupa nume; evita daca numele este duplicat")
    msg.send(sender, "&e/ainpc delete-id <id> confirm")
    msg.send(sender, "&7  Sterge sigur un NPC dupa ID numeric")
    msg.send(sender, "&e/ainpc duplicates")
    msg.send(sender, "&7  Raporteaza duplicate dupa source_key, nume+locatie si entitati live")
    msg.send(sender, "&e/ainpc repair duplicates [dryrun|apply]")
    msg.send(sender, "&7  Curata controlat randuri/entitati NPC duplicate; ruleaza dryrun inainte de apply")
    msg.send(sender, "&e/ainpc repair households [dryrun|apply]")
    msg.send(sender, "&7  Curata rezidenti household duplicati dupa NPC/source_key; ruleaza dryrun inainte de apply")
    msg.send(sender, "&e/ainpc repair npc-bindings [dryrun|apply]")
    msg.send(sender, "&7  Sincronizeaza profilul NPC catre npc_world_bindings; ruleaza dryrun inainte de apply")
    msg.send(sender, "&e/ainpc repair mapping-metadata [dryrun|apply]")
    msg.send(sender, "&7  Sincronizeaza npc_world_bindings catre metadata WorldAdmin; ruleaza dryrun inainte de apply")
    msg.send(sender, "&e/ainpc repair batch <batchKey> [dryrun|apply|inspect|mark-steps|mark-failed]")
    msg.send(sender, "&7  Inspecteaza sau ruleaza rollback controlat pentru un spawn batch esuat")
    msg.send(sender, "&e/ainpc info [nume]")
    msg.send(sender, "&7  Afiseaza informatii despre un NPC")
    msg.send(sender, "&e/ainpc gui [quest|story|world|stats|interact|routine|shop|manager|audit|debug] [questFilter]")
    msg.send(sender, "&7  Deschide hub-ul GUI sau un ecran specific; questFilter poate fi quest/contract/duty/bounty/event/tutorial/ritual")
    msg.send(sender, "&e/ainpc quest <numeNpc> [jucator]")
    msg.send(sender, "&7  Declanseaza manual quest-ul unui NPC")
    msg.send(sender, "&e/ainpc progression log [jucator] [quest|contract|duty|bounty|event|active|all]")
    msg.send(sender, "&7  Listeaza progresii generice peste questuri, contracte, sarcini, bounty-uri si evenimente")
    msg.send(sender, "&e/ainpc contract log [jucator]")
    msg.send(sender, "&7  Listeaza contractele locale prin runtime-ul comun")
    msg.send(sender, "&e/ainpc duty log [jucator]")
    msg.send(sender, "&7  Listeaza sarcinile NPC prin runtime-ul comun")
    msg.send(sender, "&e/ainpc bounty log [jucator]")
    msg.send(sender, "&7  Listeaza bounty-urile locale prin runtime-ul comun")
    msg.send(sender, "&e/ainpc event log [jucator]")
    msg.send(sender, "&7  Listeaza evenimentele locale prin runtime-ul comun")
    msg.send(sender, "&e/ainpc quest track [start|stop] [questCode|templateId] [jucator]")
    msg.send(sender, "&7  Arata sau mentine busola/actionbar/particule catre tinta questului")
    msg.send(sender, "&e/ainpc quest nearest [jucator]")
    msg.send(sender, "&7  Declanseaza quest-ul celui mai apropiat NPC")
    msg.send(sender, "&e/ainpc quest reset <numeNpc> [jucator]")
    msg.send(sender, "&7  Reseteaza progresul quest-ului pentru un jucator")
    msg.send(sender, "&e/ainpc quest complete <numeNpc> [jucator]")
    msg.send(sender, "&7  Marcheaza manual quest-ul ca finalizat si da recompensa")
    msg.send(sender, "&e/ainpc quest anchors [jucator|uuid|all] [templateId|questCode]")
    msg.send(sender, "&7  Listeaza ancorele semantice persistate pentru questuri")
    msg.send(sender, "&e/ainpc demo <definition|status|next|script|phases|evidence|runbook|smoke|summary|commands|restart|experimental|experimental5|experimental25|experimental25deep|experimental25ops> [regionId] [player]")
    msg.send(sender, "&7  Explica, verifica si ghideaza primul demo intern jucabil; modurile experimental sunt instabile")
    msg.send(sender, "&e/ainpc list")
    msg.send(sender, "&7  Lista toate NPC-urile")
    msg.send(sender, "&e/ainpc world whereami [jucator]")
    msg.send(sender, "&7  Arata regiunea, place-ul si node-urile active pentru o locatie")
    msg.send(sender, "&e/ainpc world places [regionId]")
    msg.send(sender, "&7  Listeaza place-urile mapate")
    msg.send(sender, "&e/ainpc world region info <regionId>")
    msg.send(sender, "&7  Arata detalii despre o regiune mapata")
    msg.send(sender, "&e/ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>")
    msg.send(sender, "&7  Creeaza o regiune noua in lumea jucatorului")
    msg.send(sender, "&e/ainpc world place info <placeId>")
    msg.send(sender, "&7  Arata detalii despre un place mapat")
    msg.send(sender, "&e/ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>")
    msg.send(sender, "&7  Creeaza un place nou in interiorul unei regiuni")
    msg.send(sender, "&e/ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]")
    msg.send(sender, "&7  Creeaza un node de regiune sau de place")
    msg.send(sender, "&e/ainpc world scan village [radius] [import] [regionId]")
    msg.send(sender, "&7  Scaneaza sat vanilla si poate importa mapping semantic AINPC")
    msg.send(sender, "&e/ainpc world demo create [regionId]")
    msg.send(sender, "&7  Creeaza mapping demo la pozitia ta; consola/RCON foloseste spawn-ul lumii")
    msg.send(sender, "&e/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]")
    msg.send(sender, "&7  Leaga un NPC la home/work/social places din mapping")
    msg.send(sender, "&e/ainpc world bindings [list|npc|place] ...")
    msg.send(sender, "&7  Inspecteaza read-only npc_world_bindings")
    msg.send(sender, "&e/ainpc world household <plan|spawn> <homePlaceId> [count]")
    msg.send(sender, "&7  Genereaza sau executa un household spawn plan din mapping")
    msg.send(sender, "&e/ainpc world household <status|place|resident|list> ...")
    msg.send(sender, "&7  Inspecteaza read-only household-uri persistente si rezidenti")
    msg.send(sender, "&e/ainpc world settlement <plan|spawn> <regionId> [maxHouses]")
    msg.send(sender, "&7  Genereaza sau executa household-uri pentru casele din regiune")
    msg.send(sender, "&e/ainpc world save")
    msg.send(sender, "&7  Salveaza modificarile runtime in config.yml")
    msg.send(sender, "&e/ainpc patch <analyze|plan|validate> <regionId> [targetPopulation] [profesiiCSV]")
    msg.send(sender, "&7  Produce gap report si patch plan read-only pentru completarea satului")
    msg.send(sender, "&e/ainpc wand [mode|pos1|pos2|point|status|inspect|clear|reset]")
    msg.send(sender, "&7  Selecteaza geometrie sau puncte pentru mapping manual asistat")
    msg.send(sender, "&e/ainpc map <region|place|node> <descriere>")
    msg.send(sender, "&7  Creeaza draft mapping cu preview si confirmare inainte de scriere")
    msg.send(sender, "&e/ainpc story context [jucator] [numeNpc|nearest]")
    msg.send(sender, "&7  Afiseaza contextul narativ curent din mapping si quest anchors")
    msg.send(sender, "&e/ainpc story region <regionId>")
    msg.send(sender, "&7  Afiseaza story state-ul persistent pentru o regiune")
    msg.send(sender, "&e/ainpc story place <placeId>")
    msg.send(sender, "&7  Afiseaza story state-ul persistent pentru un place")
    msg.send(sender, "&e/ainpc story events <regionId|placeId> [limit]")
    msg.send(sender, "&7  Listeaza evenimente story persistente")
    msg.send(sender, "&e/ainpc migration households <dryrun|apply> [limit]")
    msg.send(sender, "&7  Backfill controlat din npc_world_bindings catre household-uri persistente")
    msg.send(sender, "&e/ainpc audit [all|npc|world|db|spawn|quest]")
    msg.send(sender, "&7  Verifica probleme ascunse in NPC-uri, mapping si baza de date")
    msg.send(sender, "&e/ainpc debugdump [all|npc|world|quest|story|openai]")
    msg.send(sender, "&7  Genereaza un jurnal avansat read-only pentru debugging")
    msg.send(sender, "&e/ainpc family <nume>")
    msg.send(sender, "&7  Afiseaza familia unui NPC")
    msg.send(sender, "&e/ainpc routine <tick|status>")
    msg.send(sender, "&7  Verifica sau ruleaza rutina zilnica a NPC-urilor")
    msg.send(sender, "&e/ainpc mood <nume> <emotie> [intensitate]")
    msg.send(sender, "&7  Seteaza emotia unui NPC")
    msg.send(sender, "&e/ainpc tp <nume>")
    msg.send(sender, "&7  Teleporteaza-te la un NPC")
    msg.send(sender, "&e/ainpc test")
    msg.send(sender, "&7  Testeaza conexiunea OpenAI")
    msg.send(sender, "&e/ainpc reload")
    msg.send(sender, "&7  Reincarca configuratia")
}

fun sendWandStatus(sender: CommandSender, session: MappingWandService.MappingWandSession) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    val selection = session.selection()
    msg.send(sender, "&6=== Mapping Wand ===")
    msg.send(sender, "&eMod: &f${session.mode().id()}")
    msg.send(sender, "&ePos1: &f${formatMappingPoint(selection.pos1())}")
    msg.send(sender, "&ePos2: &f${formatMappingPoint(selection.pos2())}")
    msg.send(sender, "&ePoint: &f${formatMappingPoint(selection.point())}")
    selection.bounds().ifPresent { bounds ->
        msg.send(sender, "&eBounds: &f${bounds.format()}")
    }
    msg.send(sender, "&eDraft: &f${session.draft()?.qualifiedId() ?: "<nesetat>"}")
}

fun sendMappingDraft(sender: CommandSender, draft: MappingDraft) {
    val msg = ainpcCommandDisplayPlugin.messageUtils
    msg.send(sender, "&6=== Mapping Draft Preview ===")
    msg.send(sender, "&eTip draft: &f${draft.kind().id()}")
    msg.send(sender, "&eID propus: &f${draft.qualifiedId()}")
    msg.send(sender, "&eNume: &f${draft.displayName()}")
    msg.send(sender, "&eTip semantic: &f${draft.typeId()}")
    if (draft.isBox()) {
        msg.send(sender, "&eLume: &f${draft.worldName()}")
        msg.send(sender, "&eBounds: &f${formatBounds(draft.minX(), draft.minY(), draft.minZ(), draft.maxX(), draft.maxY(), draft.maxZ())}")
    } else if (draft.isNode()) {
        msg.send(sender, "&eRegiune: &f${draft.regionId()}")
        msg.send(sender, "&ePlace: &f${formatOptional(draft.placeId())}")
        msg.send(sender, "&ePozitie: &f${String.format(Locale.ROOT, "%.1f, %.1f, %.1f", draft.x(), draft.y(), draft.z())}")
        msg.send(sender, "&eRaza: &f${String.format(Locale.ROOT, "%.1f", draft.radius())}")
    } else if (draft.isNpcBind()) {
        msg.send(sender, "&eNPC selector: &f${draft.metadata().getOrDefault("npc_selector", "<nesetat>")}")
        msg.send(sender, "&eRol bind: &f${draft.metadata().getOrDefault("bind_role", "<nesetat>")}")
        msg.send(sender, "&eRegiune: &f${draft.regionId()}")
        msg.send(sender, "&ePlace: &f${formatOptional(draft.placeId())}")
    } else if (draft.isQuestAnchor()) {
        msg.send(sender, "&ePlayer selector: &f${draft.metadata().getOrDefault("player_selector", "self")}")
        msg.send(sender, "&eProgresie: &f${draft.metadata().getOrDefault("progression_selector", "<nesetat>")}")
        msg.send(sender, "&eObjective ID: &f${draft.metadata().getOrDefault("objective_key", "<nesetat>")}")
        msg.send(sender, "&eObjective type: &f${draft.metadata().getOrDefault("objective_type", "<nesetat>")}")
        msg.send(sender, "&eAncora: &f${draft.metadata().getOrDefault("anchor_type", "?")}:${draft.metadata().getOrDefault("anchor_id", "?")}")
    }
    msg.send(sender, "&eTag-uri: &f${formatList(draft.tags())}")
    msg.send(sender, "&eMetadata: &f${formatMap(draft.metadata())}")
    if (draft.warnings().isNotEmpty()) {
        for (warning in draft.warnings()) {
            msg.send(sender, "&eWarning: &f$warning")
        }
    }
    msg.send(sender, "&7Comanda de baza: &f${draft.confirmationCommand()}")
    msg.send(sender, "&7Confirma cu &f/ainpc map confirm &7sau anuleaza cu &f/ainpc map cancel&7.")
}
