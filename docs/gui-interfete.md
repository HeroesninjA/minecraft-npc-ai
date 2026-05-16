# GUI Interfete

Actualizat: 2026-05-11

## Scop

Acest document descrie implementarea unui strat GUI profesional pentru AINPC: quest GUI, world GUI, statistici, shop, manager admin, debug, audit si interactiune NPC.

Regula principala:

```text
GUI-ul este un strat de prezentare si control.
Serviciile existente raman sursa de adevar.
Click-ul din GUI nu modifica stare fara validare runtime.
```

GUI-ul trebuie sa faca sistemul mai usor de folosit pe server, dar nu trebuie sa mute logica din `ScenarioEngine`, `WorldAdminApi`, `DialogManager`, `StoryContextService`, audit sau debugdump in clasele de inventar.

## Status curent

Exista o prima fundatie GUI in `ainpc-core-plugin/src/main/java/ro/ainpc/gui/`.

Implementat initial:

- `GuiService`, `GuiSessionManager`, `AINPCGuiHolder`, `GuiInventoryListener`;
- anulare click/drag in inventarele AINPC si curatare sesiune la close/quit;
- `/ainpc gui [main|quest|story|world|stats|interact|routine|shop|manager|audit|debug]`;
- `/ainpc gui quest <filter>` pentru deschidere directa a quest log-ului filtrat;
- `/quest gui [filter]`;
- tab-completion pentru `/ainpc gui`, `/ainpc gui quest <filter>` si `/quest gui <filter>`;
- permisiuni `ainpc.gui.*` in `plugin.yml`;
- `ScenarioEngine.getQuestGuiSnapshot(...)` ca snapshot read-only pentru GUI;
- hub principal, quest log navigabil, detalii quest, story snapshot read-only, world context, statistici, interactiune NPC, manager NPC, audit si debug;
- filtre interactive in `QuestLogGui` pentru `all`, `active`, `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` si `ritual`, cu stare per jucator;
- `QuestLogGui` grupeaza randurile dupa mecanica si pagineaza lista prin `QuestLogGuiPage`, cu pagina curenta pastrata in `GuiService`;
- `QuestLogGui` combina intrarile curente si arhivate returnate de snapshot, iar `QuestDetailGui` pastreaza filtrul sursa pentru detalii stabile dupa click;
- cardurile din `QuestLogGui` au actiuni rapide: click pentru detalii, right click pentru track/status pe intrarea selectata si shift click pentru status in chat;
- actiunile rapide din `QuestLogGui` si `QuestDetailGui` folosesc comenzi kind-aware din `ProgressionGuiEntry`, de exemplu `/ainpc contract ...`, `/ainpc bounty ...` sau `/ainpc ritual ...`;
- `ProgressionGuiEntry` centralizeaza selectorul de detalii, filtrul GUI si comenzile kind-aware, astfel `QuestLogGui`, `QuestDetailGui` si `NpcInteractionGui` folosesc aceeasi regula;
- `QuestLogGui` foloseste etichete vizibile de progresie pentru header, empty state, tracking si ancore, pastrand comenzile existente ca fallback;
- `QuestLogGui` opreste tracking-ul prin comanda kind-aware a intrarii urmarite cand aceasta este vizibila, cu fallback generic cand filtrul curent nu o contine;
- `MainHubGui`, `StatsGui`, `GuiKey`, `/ainpc gui` si tab-completion folosesc etichete de progresie si accepta alias-uri GUI ca `progresii`, `progression`, `detalii_progresie` si `progression_details`;
- comenzile de mecanica pot deschide GUI-ul direct cu filtrul corect, de exemplu `/ainpc contract gui`, `/ainpc contract gui active` si `/ainpc ritual gui tracked`;
- help-ul pentru `/ainpc progression` si alias-urile de mecanica include intrarea `gui`, nu doar `log/status/progress`;
- tab-completion pentru comenzile de mecanica sugereaza filtre relative (`active`, `tracked`, `archived`) pentru `gui` si `log`, nu lista globala de kind-uri;
- `QuestDetailGui` revine in quest log cu filtrul sursa al detaliului, inclusiv cand intrarea a disparut sau a fost arhivata;
- `QuestDetailGui` foloseste texte generice de progresie in inventar, ca acelasi ecran sa nu afiseze "quest" pe contracte, bounty-uri, duty-uri, evenimente, tutoriale sau ritualuri;
- `QuestDetailGui` expune status, progress, tracking, abandon si debug prin aceleasi comenzi kind-aware, fara hardcodare pe `/ainpc quest ...`, si arata linii read-only de status/actiuni din snapshot;
- `QuestLogGuiPage` evita header-ele orfane la final de pagina si repeta header-ul cand un grup mare continua pe pagina urmatoare;
- `NpcInteractionGui` arata rutina NPC-ului, progresia relevanta din `ProgressionGuiSnapshot` si are actiuni rapide kind-aware pentru nearest/status/accept, detalii progresie, story si routine status;
- `RoutineGui` dedicat pentru `/ainpc gui routine`, cu preview pentru slotul curent al fiecarui NPC si programul zilnic home/work/social/idle;
- `NpcManagerGui` afiseaza pe carduri rutina calculata, nevoile si ancorele home/work/social, cu actiuni rapide pentru info, teleport, familie si routine status;
- `WorldHubGui` afiseaza sumarul `ProgressionGuiSnapshot`, deschide log-ul filtrat de progresii si expune ancorele locale citite prin `ProgressionService` pentru diagnostic mapping/quest;
- `StoryGui` afiseaza read-only region state, place state si ultimele story events pentru locatia curenta, folosind `StoryStateService`;
- `WorldHubGui` cere confirmare pentru scan sat, demo mapping si save mapping;
- `DebugGui` expune toate scope-urile principale de debugdump: all, npc, world, quest, story si openai;
- `QuestDetailGui` cu obiective, stage-uri, recompense, tracking, status, debug admin si abandon cu confirmare;
- `ConfirmActionGui` pentru actiuni destructive;
- shop GUI placeholder, pregatit pentru provider dedicat.

Comenzile text raman fallback si sunt folosite de butoanele GUI pentru actiuni validate:

- `/quest log`, `/quest status`, `/quest track`;
- `/ainpc quest anchors`, `/ainpc quest debug`;
- `/ainpc world whereami`, `places`, `region`, `place`, `node`, `scan`, `demo`, `bind`, `household`, `settlement`, `save`;
- `/ainpc story context`, `region`, `place`, `events`;
- `/ainpc audit`, `audit npc`, `audit world`, `audit db`, `audit spawn`, `audit quest`;
- `/ainpc debugdump all|npc|world|quest|story|openai`;
- `/ainpc info`, `list`, `family`, `routine`, `mood`, `tp`, `test`.

Prima implementare GUI este un wrapper sigur peste aceste servicii si comenzi, nu o rescriere completa.

## GUI in lucrul alternat

GUI-ul face parte din fiecare slice din `lucru-alternat-quest-mapping-progression.md`. Un slice nu este complet doar pentru ca exista comanda text; trebuie sa existe si o suprafata vizuala minima care arata aceeasi stare fara sa mute logica in inventare.

Reguli pentru alternanta:

- Mapping-ul expune contextul prin snapshot-uri sau API read-only, iar GUI-ul doar il prezinta.
- Questurile si mecanicile non-quest apar in `QuestLogGui` prin `ProgressionGuiSnapshot`, nu prin citiri separate din YAML sau DB.
- `ProgressionService` este sursa pentru status/progress/track/abandon cand exista fatada generica; GUI-ul nu trebuie sa aiba ramuri diferite pentru fiecare mecanica.
- `WorldHubGui`, `RoutineGui`, `NpcInteractionGui`, `AuditGui` si `DebugGui` sunt suprafete de verificare pentru acelasi slice, nu sisteme paralele.
- Orice click care schimba stare trebuie sa aiba validare runtime si fallback prin comanda text.

Checklist minim pe slice:

| Componenta | Verificare GUI |
|---|---|
| Mapping | `/ainpc gui world` poate arata contextul, progresiile vizibile si ancorele persistate sau trimite spre comenzile world relevante |
| Quest/progression | `/ainpc gui quest <filter>` listeaza intrarea corecta si detaliile stage/objective |
| NPC/rutina | `/ainpc gui routine` sau interactiunea NPC explica rutina curenta si ancorele |
| Audit/debug | `/ainpc gui audit` si `/ainpc gui debug` pot porni inspectiile fara actiuni destructive directe |
| Persistenta | dupa reload, aceleasi intrari apar cu status/tracking coerent |

Gate pentru demo:

- filtrele `quest`, `contract`, `duty`, `bounty`, `event`, `tutorial` si `ritual` afiseaza intrari corecte cand exista definitii/progres;
- cardurile nu ascund warnings importante din audit;
- detaliile nu depind de text hardcodat pentru stage-uri concrete;
- actiunile rapide au confirmare cand pot abandona, sterge, reseta sau genera date.

## Principii UI

Reguli pentru toate ecranele:

- foloseste inventory GUI Paper/Bukkit pentru compatibilitate;
- identifica GUI-ul prin `InventoryHolder`, nu prin titlu;
- anuleaza toate click-urile in inventarele AINPC;
- foloseste snapshot-uri read-only pentru randare;
- ruleaza citiri DB sau calcule grele async, apoi deschide inventarul pe main thread;
- valideaza din nou permisiunile si starea la click;
- pentru actiuni destructive foloseste confirmare separata;
- foloseste paginare pentru liste;
- foloseste culori consistente pentru status;
- nu ascunde erorile: afiseaza warnings/errors in GUI si lasa detalii in audit/debugdump;
- pastreaza comenzile text ca fallback si pentru administrare rapida.

Cod de culoare recomandat:

| Culoare | Sens |
|---|---|
| verde | valid, complet, functional, actiune sigura |
| galben | warning, incomplet, necesita atentie |
| rosu | eroare, blocaj, actiune periculoasa |
| albastru | informatie, navigare, context |
| mov | AI/story/context avansat |
| gri | dezactivat, fara permisiune, lipsa date |

## Pachet recomandat

```text
ainpc-core-plugin/src/main/java/ro/ainpc/gui/
  GuiService.java
  GuiRegistry.java
  GuiSessionManager.java
  GuiScreen.java
  GuiButton.java
  GuiRenderContext.java
  GuiClickContext.java
  GuiAction.java
  GuiPagination.java
  GuiItemFactory.java
  GuiPermissionPolicy.java
  GuiOpenRequest.java
  GuiResult.java

ainpc-core-plugin/src/main/java/ro/ainpc/gui/listeners/
  GuiInventoryListener.java

ainpc-core-plugin/src/main/java/ro/ainpc/gui/screens/
  MainHubGui.java
  QuestLogGui.java
  QuestDetailGui.java
  WorldHubGui.java
  WorldRegionGui.java
  WorldPlaceGui.java
  StatsGui.java
  ShopGui.java
  NpcManagerGui.java
  NpcInteractionGui.java
  RoutineGui.java
  DebugGui.java
  AuditGui.java
  ConfirmActionGui.java
```

Pentru prima versiune, nu este necesara o librarie externa de inventory GUI. API-ul Paper/Bukkit este suficient daca exista un strat intern mic si testabil.

## Contract minim

```java
public interface GuiScreen {
    GuiKey key();
    Component title(GuiRenderContext context);
    int size(GuiRenderContext context);
    void render(GuiRenderContext context);
    void handleClick(GuiClickContext context);
}
```

```java
public record GuiButton(
    ItemStack icon,
    List<String> lore,
    GuiAction action,
    boolean enabled
) {
}
```

```java
public record GuiClickContext(
    Player player,
    GuiSession session,
    int rawSlot,
    ClickType clickType,
    InventoryAction inventoryAction
) {
}
```

Regula: `GuiScreen` nu face query DB direct si nu modifica runtime direct. Ecranul apeleaza servicii clare sau un provider de snapshot.

## GuiService

Responsabilitati:

- deschide ecrane dupa cheie;
- creeaza si inchide sesiuni GUI;
- gestioneaza back stack-ul;
- ruleaza loader-ele async;
- face refresh controlat;
- expira sesiuni vechi;
- centralizeaza permisiunile;
- protejeaza impotriva click-urilor stale.

Exemplu de flux:

```text
/ainpc gui quest
-> GuiService.open(player, QUEST_LOG)
-> QuestGuiProvider.loadSnapshot(player) async
-> render pe main thread
-> player click Track
-> validare permisiune + quest inca activ
-> ScenarioEngine/QuestTrackingService executa actiunea
-> refresh QuestLogGui
```

## Listener GUI

`GuiInventoryListener` trebuie sa acopere:

- `InventoryClickEvent`;
- `InventoryDragEvent`;
- `InventoryCloseEvent`;
- optional `PlayerQuitEvent`.

Reguli:

- daca holder-ul nu este AINPC, nu atinge evenimentul;
- daca este AINPC, `event.setCancelled(true)`;
- click-urile in inventarul jucatorului sunt ignorate sau tratate doar pentru input explicit;
- `SHIFT_CLICK`, number keys, drop, swap offhand si drag sunt blocate;
- actiunile trebuie sa fie idempotente sau protejate de lock scurt per sesiune;
- daca snapshot-ul este expirat, se face refresh inainte de actiune.

## Comenzi noi recomandate

```text
/ainpc gui
/ainpc gui quest
/ainpc gui story
/ainpc gui world
/ainpc gui stats
/ainpc gui shop [npc]
/ainpc gui manager [npc]
/ainpc gui debug
/ainpc gui audit
/ainpc gui interact [nearest|npcName]
/quest gui
/npc gui [nearest|npcName]
```

Permisiuni recomandate:

| Permisiune | Rol |
|---|---|
| `ainpc.gui` | Acces la hub-ul GUI de baza |
| `ainpc.gui.quest` | Quest GUI pentru jucatori |
| `ainpc.gui.story` | Story GUI read-only pentru admini |
| `ainpc.gui.world` | World GUI read-only pentru admini |
| `ainpc.gui.stats` | Statistici personale |
| `ainpc.gui.shop` | Shop NPC |
| `ainpc.gui.interact` | Interactiune NPC avansata |
| `ainpc.gui.routine` | Preview rutine NPC |
| `ainpc.gui.manager` | Manager NPC admin |
| `ainpc.gui.debug` | Debug GUI admin |
| `ainpc.gui.audit` | Audit GUI admin |

Pentru compatibilitate, `ainpc.admin`, `ainpc.quest`, `ainpc.info`, `ainpc.talk` si permisiunile dedicate precum `ainpc.gui.story` pot activa implicit parti din GUI. `StoryGui` ramane read-only chiar si cand este deschis de admin.

## Hub principal

Ecran: 54 sloturi.

```text
0-8     header si status server
9-17    questuri, NPC, world, statistici, shop, rutina, story
18-26   shop, interactiuni, rutina, familie, mood
27-35   admin manager, audit, debug, OpenAI test, config
36-44   notificari, warnings, actiuni recomandate
45      back
49      refresh
53      close
```

Iteme recomandate:

| Slot | Item | Actiune |
|---:|---|---|
| 10 | `WRITABLE_BOOK` | Quest GUI |
| 11 | `VILLAGER_SPAWN_EGG` | NPC Interaction GUI |
| 12 | `COMPASS` | World GUI |
| 13 | `CLOCK` | Statistici |
| 14 | `EMERALD` | Shop |
| 15 | `CLOCK` | Rutine NPC |
| 16 | `AMETHYST_SHARD` | Story snapshot |
| 28 | `NAME_TAG` | Manager NPC |
| 29 | `REDSTONE_TORCH` | Audit |
| 30 | `SPYGLASS` | Debug |
| 49 | `SUNFLOWER` | Refresh |

Hub-ul trebuie sa ascunda sau sa dezactiveze butoanele fara permisiune. Pentru admini, este mai util sa fie vizibile dezactivate cu motiv in lore.

## Quest GUI

Scop:

- quest log vizual;
- status rapid;
- urmarire quest;
- detalii obiective/stage;
- abandon cu confirmare;
- debug admin pentru quest.

Snapshot recomandat:

```text
QuestListSnapshot
- player
- tracked_quest
- active_quests
- offered_quests
- completed_quests
- failed_or_abandoned_quests
- warnings
```

Layout `QuestLogGui`, 54 sloturi:

```text
0-8     header si context
9-17    filtre: all, active, quest, contract, duty, bounty, event, tutorial, ritual
19-43   headers de mecanica si quest/progression cards paginate
44      indicator pagina/lista, cand exista mai multe pagini
45      hub
46      pagina anterioara
47      pagina urmatoare
48      quest anchors admin
49      refresh
50      track quest activ fallback
51      stop tracking
53      close
```

Quest card lore:

```text
Titlu: Q06 - Urmele din forja
Status: ACTIVE
Stage: INVESTIGATION
Obiective:
- Viziteaza forja: 1/1
- Inspecteaza urmele: 0/1
Recompense: emerald x3, story event
Click: detalii
Right click: track/status pe intrarea selectata
Shift click: status in chat
```

`QuestDetailGui`, 54 sloturi:

```text
10      quest summary
12-16   objectives current stage
21-25   objectives next/locked stage
30      track/untrack
31      status in chat
32      abandon confirm
34      admin debug
45      inapoi la quest log
49      refresh
53      close
```

Reguli:

- abandonul cere `ConfirmActionGui`;
- `Track` valideaza ca questul este activ;
- obiectivele cu `quest.stages` trebuie grupate dupa stage;
- pentru admin, butonul debug poate deschide snapshotul echivalent cu `/ainpc quest debug`;
- GUI-ul nu completeaza questuri direct decat daca exista handler explicit de turn-in validat.

## World GUI

Scop:

- inspectie world mapping;
- regiuni, places, nodes;
- whereami vizual;
- scan/demo/create pentru admin;
- bind NPC la home/work/social;
- household/settlement plan si spawn controlat.

Snapshot recomandat:

```text
WorldGuiSnapshot
- current_region
- current_place
- nearby_nodes
- region_count
- place_count
- node_count
- unsaved_changes
- audit_warnings
```

`WorldHubGui`, 54 sloturi:

```text
10      whereami
11      regions
12      places nearby
13      progression summary
14      quest anchors
15      unsaved changes
19      scan current area
20      demo create
21      save mapping
28      household plan
29      household spawn
30      settlement plan
31      settlement spawn
40      audit world
49      refresh
53      close
```

Reguli:

- `save`, `demo create`, `scan`, `spawn` si `settlement spawn` cer permisiune admin si confirmare;
- planurile trebuie afisate ca preview inainte de spawn;
- GUI-ul nu trebuie sa creeze regiuni/places/nodes fara parametri clari; pentru create manual, foloseste wizard pe pasi sau ramai la comanda text;
- pentru `bind npc`, ecranul trebuie sa arate NPC-ul selectat si locurile candidate.

`WorldPlaceGui` exemplu:

```text
10      place info
11      region parent
12      type: house/work/social/forge/etc
13-25   nodes din place
28      residents metadata
29      npc_world_bindings
30      quest anchors
31      story state
40      teleport
41      bind selected NPC
42      inspect in chat
```

## Statistici GUI

Scop:

- sumar personal pentru jucator;
- sumar admin pentru server;
- sanatate runtime fara debug dump complet.

`StatsGui` pentru jucator:

```text
10      questuri active/completate
11      NPC-uri cunoscute
12      relatie medie cu NPC-uri
13      memorii relevante
14      reputatie/story flags vizibile
15      shop/tranzactii viitoare
16      timp de joc sesiune
```

`StatsGui` admin:

```text
19      NPC-uri active/spawned/persistate
20      quest templates/player quests
21      world regions/places/nodes
22      story state/events
23      simulation tick summary
24      routine assignments
25      OpenAI health summary
28      audit warnings/errors
29      debugdump last run
```

Reguli:

- statisticile trebuie incarcate prin snapshot-uri agregate, nu prin query-uri repetate la fiecare slot;
- datele sensibile OpenAI nu se afiseaza;
- pentru valori mari, afiseaza top N si link catre debugdump.

## Shop GUI

Scop:

- shop-uri NPC pe profesie/rol;
- servicii comerciale simple;
- confirmare tranzactie;
- extensibilitate catre economie optionala.

Prima versiune recomandata:

- fara dependinta hard de Vault;
- preturi in iteme vanilla sau emerald;
- tranzactii validate server-side;
- stoc optional in memorie/config, nu generat liber de AI;
- shop-ul deschis doar pentru NPC-uri cu rol/profesie compatibila.

Modele recomandate:

```text
NpcShopDefinition
- shop_id
- npc_role
- currency: ITEM | VAULT_OPTIONAL
- offers
- restock_policy
- permission

ShopOffer
- offer_id
- display_item
- cost_items
- result_items
- max_uses
- requirements
```

`ShopGui`, 54 sloturi:

```text
0-8     categorie: buy, sell, services, special
9-44    offer cards
45      pagina anterioara
48      inapoi la NPC
49      refresh
50      pagina urmatoare
53      close
```

`ShopConfirmGui`:

```text
20      cost
22      rezultat
24      confirm
31      cancel
```

Reguli:

- verifica inventarul jucatorului la confirmare, nu la randare;
- verifica spatiul liber pentru rezultat;
- aplica tranzactia atomic cat permite Bukkit inventory;
- logheaza tranzactiile importante;
- nu permite AI-ului sa creeze oferte live fara validare si audit.

## Manager GUI

Scop:

- panou admin pentru gestionare NPC;
- actiuni rapide peste comenzile existente;
- inspectie profil, rutina, familie, mood, bindings.

`NpcManagerGui`, 54 sloturi:

```text
10      profil NPC
11      locatie si stare spawned
12      profesie/rol
13      emotii dominante
14      rutina curenta
15      familie
16      world bindings
19      teleport la NPC
20      refresh profile
21      mood set/debug
22      routine status/tick
23      family inspect
24      bind home/work/social
31      delete confirm
40      debug NPC
41      audit NPC
```

Reguli:

- delete/despawn necesita confirmare;
- teleport este actiune admin separata;
- editarea campurilor complexe trebuie facuta prin wizard sau comenzi text pana exista validare buna;
- managerul nu trebuie sa permita date invalide in `profile_data` sau `npc_world_bindings`.

## Debug GUI

Scop:

- diagnostic rapid in joc;
- lansare debugdump;
- inspectie read-only a starii curente;
- directionare catre comenzi/loguri.

`DebugGui`, 54 sloturi:

```text
10      debug config status
11      OpenAI health/test
12      NPC runtime summary
13      world mapping summary
14      quest runtime summary
15      story context summary
16      simulation/routine summary
19      debugdump all
20      debugdump npc
21      debugdump world
22      debugdump quest
23      debugdump openai
31      last errors/warnings
40      run /ainpc test
```

Reguli:

- debug GUI este admin-only;
- nu afiseaza chei API sau prompturi complete in mod normal;
- `debugdump openai` trebuie sa respecte aceleasi reguli de redactare ca serviciul existent;
- pentru fisiere generate, GUI-ul trimite path-ul relativ in chat dupa finalizare;
- actiunile care pot dura ruleaza async si afiseaza stare "running".

## Audit GUI

Scop:

- audit rapid vizual;
- errors/warnings grupate;
- actiuni recomandate;
- legatura cu debugdump.

`AuditGui`, 54 sloturi:

```text
10      audit all
11      audit npc
12      audit world
13      audit db
14      audit spawn
15      audit quest
19-35   rezultate grupate
40      export debugdump all
41      export debugdump scope
49      refresh
53      close
```

Item rezultat:

```text
REDSTONE_BLOCK
Titlu: ERROR - quest template
Lore:
- Q08 objective stage necunoscut
- Scope: quest
- Click: detalii in chat
- Shift click: ruleaza debugdump quest
```

Reguli:

- auditul ramane read-only;
- GUI-ul nu executa repair automat;
- fiecare rezultat trebuie sa aiba scope si severitate;
- auditul complet poate fi cache-uit scurt ca sa evite spam;
- fixurile sugerate trebuie sa fie explicite si sa trimita la comanda/documentatie.

## Interactiune profesionala NPC

Scop:

- ecran polish pentru interactiunea cu un NPC;
- inlocuieste multe comenzi pentru jucator;
- pastreaza conversatia naturala prin chat.

Deschidere recomandata:

- click dreapta normal: comportamentul curent de dialog;
- shift + click dreapta: `NpcInteractionGui`;
- comanda: `/ainpc gui interact nearest`;
- buton din hub: NPC cel mai apropiat.

`NpcInteractionGui`, 54 sloturi:

```text
4       portret/profil NPC
10      vorbeste in chat
11      questuri disponibile/active
12      shop/servicii
13      relatie si incredere
14      memorii importante
15      emotie curenta
16      rutina/loc de munca
19      home/work/social place
20      story/context local
21      cere status quest
22      deschide shop
23      saluta/incepe sesiune
24      incheie sesiune
31      admin manager
32      admin debug
33      admin audit
45      hub
49      refresh
53      close
```

Reguli:

- ecranul nu trebuie sa intrerupa dialogul existent;
- butonul "vorbeste" deschide sau improspateaza `ConversationSessionManager`;
- questurile folosesc `ScenarioEngine.handleQuestInteraction(...)`;
- shop-ul se deschide doar daca NPC-ul are shop valid;
- relatie/memorie sunt read-only pentru jucator;
- actiunile admin apar doar cu permisiune.

Pentru o experienta profesionala:

- lore-ul trebuie sa fie scurt si util;
- butoanele dezactivate explica motivul;
- fiecare actiune are feedback imediat;
- meniul nu afiseaza informatii interne care ar strica gameplay-ul;
- administrarea si gameplay-ul sunt separate vizual.

## Snapshot-uri

Fiecare GUI trebuie sa primeasca un snapshot, nu obiecte brute:

```text
QuestGuiSnapshot
WorldGuiSnapshot
StatsGuiSnapshot
ShopGuiSnapshot
NpcManagerSnapshot
NpcInteractionSnapshot
DebugGuiSnapshot
AuditGuiSnapshot
```

Reguli:

- snapshot-ul este imutabil;
- include timestamp si versiune;
- include warnings;
- include permisiuni calculate;
- include doar datele necesare ecranului;
- nu contine chei secrete, prompturi brute sau DB connection.

## Confirmari

Actiuni care cer confirmare:

- abandon quest;
- delete/despawn NPC;
- world demo create;
- world scan cu modificari;
- world save;
- household spawn;
- settlement spawn;
- shop trade cu cost mare;
- debugdump complet pe server mare;
- orice actiune viitoare de repair.

`ConfirmActionGui` trebuie sa arate:

- actiunea exacta;
- tinta;
- riscuri;
- efecte persistente;
- buton confirm;
- buton cancel;
- link inapoi la ecranul initial.

## Politici de siguranta

GUI-ul trebuie sa respecte:

- toate actiunile admin cer `ainpc.admin` sau permisiune dedicata;
- actiunile destructive se confirma;
- audit/debug sunt read-only, cu exceptia exportului debugdump;
- shop-ul valideaza inventarul la confirmare;
- GUI-ul nu accepta iteme puse de jucator in ecrane;
- orice click stale se ignora sau forteaza refresh;
- datele afisate din AI/debug sunt redactate;
- fiecare actiune cu efect poate fi logata cu player, target, scope si rezultat.

## Integrare cu servicii existente

| GUI | Servicii/zone consumate |
|---|---|
| Quest GUI | `ScenarioEngine`, quest tracking, `quest_anchor_bindings`, `/ainpc quest debug` |
| World GUI | `WorldAdminApi`, mapping, household/settlement plan, spawn order |
| Stats GUI | `NPCManager`, DB summaries, quest progress, simulation/routine summaries |
| Shop GUI | NPC profile/role, inventory validation, shop definitions viitoare |
| Manager GUI | `NPCManager`, `FamilyManager`, `RoutineService`, `EmotionManager`, world bindings |
| Debug GUI | `DebugDumpService`, OpenAI test, runtime summaries |
| Audit GUI | audit logic din `AINPCCommand` sau viitor `AuditService` |
| NPC Interaction GUI | `ConversationSessionManager`, `ScenarioEngine`, `DialogManager`, `MemoryManager`, `EmotionManager` |

Recomandare importanta: auditul din `AINPCCommand` trebuie extras treptat intr-un `AuditService`, ca GUI-ul si comanda text sa foloseasca acelasi contract.

## Faze de implementare

### Faza GUI-1 - Fundatie

Scop: framework intern minimal si un ecran read-only.

Livrabile:

- `GuiService`;
- `GuiInventoryListener`;
- `GuiScreen`;
- `GuiItemFactory`;
- `GuiSessionManager`;
- `/ainpc gui`;
- `MainHubGui`;
- `QuestLogGui` read-only.

Teste:

- click-urile sunt anulate;
- sesiunea se inchide la quit;
- butoanele fara permisiune sunt dezactivate;
- paginarea nu depaseste sloturile.

Status implementare:

- fundatia exista in cod;
- `QuestLogGui` are rand de filtre interactive pentru mecanicile curente;
- filtrul ales este tinut in `GuiService` per jucator;
- `QuestLogGui` are paginare initiala si grupare dupa mecanica prin `QuestLogGuiPage`;
- testele `QuestLogGuiFilterTest` si `QuestLogGuiPageTest` acopera filtrele, ordinea de grupare si limitele de pagina.

### Faza GUI-2 - Quest si NPC interaction

Scop: gameplay util pentru jucator.

Livrabile:

- `QuestDetailGui`;
- track/untrack;
- status in chat;
- abandon cu confirmare;
- `NpcInteractionGui`;
- buton pentru deschiderea sesiunii de conversatie;
- buton quest/shop daca exista.

### Faza GUI-3 - World si manager admin

Scop: administrare vizuala peste mapping si NPC-uri.

Livrabile:

- `WorldHubGui`;
- `WorldRegionGui`;
- `WorldPlaceGui`;
- preview pentru household/settlement plan;
- `NpcManagerGui`;
- confirmari pentru actiuni destructive.

### Faza GUI-4 - Audit si debug

Scop: diagnostic profesional in joc.

Livrabile:

- `AuditGui`;
- `DebugGui`;
- export debugdump din GUI;
- cache scurt pentru audit results;
- extragere initiala `AuditService`, daca este fezabil.

### Faza GUI-5 - Shop

Scop: tranzactii NPC validate.

Livrabile:

- `NpcShopDefinition`;
- `ShopGui`;
- `ShopConfirmGui`;
- validare cost/result;
- log tranzactii;
- adapter optional pentru economie externa.

## Test matrix

| Test | Asteptare |
|---|---|
| jucator fara permisiune deschide admin GUI | acces refuzat sau butoane dezactivate |
| shift-click in GUI | itemele nu se muta |
| drag peste GUI | drag anulat |
| number key swap | swap anulat |
| click pe snapshot expirat | refresh sau actiune respinsa clar |
| quest abandon | cere confirmare si apoi apeleaza runtime validat |
| world save | cere confirmare si raporteaza rezultat |
| debugdump | ruleaza async si trimite rezultat in chat |
| audit all | afiseaza errors/warnings grupate |
| shop confirm fara iteme suficiente | tranzactie refuzata |
| player iese cu GUI deschis | sesiunea este curatata |

## Definition of Done

GUI-ul poate fi considerat pregatit pentru prima versiune cand:

- exista framework intern mic, fara duplicare pe fiecare ecran;
- quest GUI functioneaza pentru log, detalii si tracking;
- NPC interaction GUI poate deschide conversatie si quest/shop/context;
- world GUI este cel putin read-only si nu permite modificari accidentale;
- audit/debug GUI sunt admin-only si read-only in afara exporturilor;
- shop-ul valideaza fiecare tranzactie server-side;
- toate click-urile speciale sunt blocate;
- toate actiunile importante au confirmare;
- comenzile text raman fallback complet;
- testele acopera listener-ul GUI, permisiunile, paginarea si actiunile critice.
