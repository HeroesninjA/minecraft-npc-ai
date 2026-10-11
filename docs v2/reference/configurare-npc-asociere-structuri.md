# Configurare NPC și asociere cu structuri

Verificat în cod: 2026-10-05. Descrie configurația și implementarea curentă, inclusiv limitele de persistență; fără test pe server live.
Flux complet: [inițiere lume, hartă, structuri, NPC și story](../guides/initiere-lume-harta-structuri-npc-story.md).

## 1. Cum apar NPC-urile

| Cale | Intrare | Inițializare |
|---|---|---|
| Creare manuală | `/ainpc create` | NPC la locația jucătorului, cu proprietățile transmise |
| Household | `world household plan/spawn` | Planner pentru o casă mapată și un număr de rezidenți |
| Settlement | `world settlement plan/spawn` | Alocări pentru casele eligibile dintr-o regiune |
| Restaurare | Pornire și chunk-uri încărcate | Încărcare NPC-uri persistente, reconciliere și restaurare |
| Villageri existenți/repopulare | Managerul de NPC și `villagers.auto_repopulate` | Flux distinct de alocarea household; nu presupune aceleași relații |
| Actori de scenariu | Pack-uri și `/ainpc scenario ...` | Lifecycle specific actorului; vezi [NPC temporare](../architecture/npc-uri-temporare-si-episodice.md) |

`PopulationPlan` narativ poate fi planificat/persistat/inspectat, dar nu este planul executat automat de `world settlement spawn`. Calea activă folosește alocările de case.

## 2. Proprietăți globale din `config.yml`

Valorile implicite sunt cele livrate în [config.yml](../../ainpc-core-plugin/src/main/resources/config.yml).

| Cheie | Implicit | Rol |
|---|---|---|
| `npc.interaction_distance` | `5` | Distanță de interacțiune, în blocuri |
| `npc.natural_movement` | `true` | AI vanilla pentru mișcare naturală |
| `npc.gravity` | `true` | Gravitația entității |
| `npc.collidable` | `true` | Coliziune |
| `npc.silent` | `false` | Sunetele entității |
| `npc.invulnerable` | `true` | Protecție față de damage |
| `npc.auto_cleanup_duplicate_entities` | `true` | Reconciliere de entități duplicate la startup/restaurare |
| `npc.message_cooldown` | `2` | Cooldown mesaje, secunde |
| `npc.max_memories_per_player` | `50` | Limită de memorii per jucător |
| `npc.memory_decay_days` | `30` | Durata implicită de expirare a memoriilor |
| `npc.memory_decay_types.<tip>` | absent | Override în zile pentru un tip de memorie |
| `spawn.batches.track_dry_runs` | `false` | Persistă separat jurnalul unui dry-run; planificarea nu creează NPC-uri |
| `family.auto_generate` | `false` | Generare automată de familie pe calea care o consultă |
| `family.spouse_chance` | `60` | Șansă de soț/soție, procent |
| `family.max_children` | `4` | Limita copiilor pentru generarea familiei |
| `family.living_parents_chance` | `40` | Șansa părinților în viață, procent |

Acestea sunt setări globale, nu o schemă `npcs.<id>` care permite suprascrierea tuturor parametrilor individual. Familia generată opțional de `FamilyManager` și familia dintr-o alocare household sunt căi distincte: `family.auto_generate: false` nu transformă automat planul household într-un plan fără relații.

### Rutine și simulare

| Cheie | Implicit | Rol |
|---|---|---|
| `features.routine` / `routine.enabled` | `true` / `false` | Activarea rutinei necesită gate-urile relevante |
| `routine.tick_seconds` | `120` | Reevaluare; schedulerul aplică minimum 20 secunde |
| `routine.teleport_enabled` | `true` | Fallback de teleport |
| `routine.natural_movement.enabled` | `true` | Încercare pathfinding |
| `routine.natural_movement.max_distance` / `speed` | `48.0` / `1.0` | Distanță și viteză pentru mișcarea naturală |
| `routine.arrival_radius` | `8.0` | Prag pentru sosirea la țintă |
| `routine.min_teleport_distance` | `18.0` | Prag de teleport |
| `routine.move_cooldown_seconds` | `300` | Interval între mutări |
| `routine.force_teleport_distance` | `96.0` | Distanță pentru corecție forțată |
| `routine.coordinator_only` | `true` | Preferință pentru coordonatorul rutinei |
| `features.simulation` / `simulation.enabled` | `true` / `false` | Gate-uri pentru simulare |
| `simulation.tick_seconds` | `30` | Tick; schedulerul aplică minimum 10 secunde |
| `simulation.needs.hunger_decay` / `energy_decay` | `1.5` / `1.2` | Parametri ai nevoilor |
| `simulation.needs.social_decay` / `comfort_decay` | `0.8` / `0.6` | Parametri ai nevoilor |
| `simulation.needs.safety_recovery` | `1.0` | Recuperarea siguranței |

Pentru repopulare vanilla, secțiunea `villagers.auto_repopulate` livrează `enabled: false`, `min_population: 6`, `max_population: 12`, `max_new_per_cycle: 2`, `scan_radius: 24`, `vertical_radius: 8`, `cooldown_seconds: 180`. Aceasta nu construiește case și nu înlocuiește planner-ul household.

Exemplu pentru activarea rutinei după ce ancorele au fost validate; fragment de integrat în secțiunile existente:

```yaml
features:
  routine: true
  simulation: true
routine:
  enabled: true
  tick_seconds: 120
simulation:
  enabled: true
  tick_seconds: 30
spawn:
  batches:
    track_dry_runs: true
```

Schimbările de activare/interval ale task-urilor necesită restart pentru aplicare sigură: `/ainpc reload` nu reconstruiește scheduler-ele. Înainte de restart, [sincronizează mapping-ul și overlay-ul](configurare-lume-harta-generare.md).

## 3. Proprietățile inițiale ale unui NPC

`NpcSpawnPlan` este un model din cod, nu un fișier YAML general încărcat pentru fiecare NPC.

| Grup | Proprietăți |
|---|---|
| Identitate în plan | `npcKey`, `name`, `occupation`, `age`, `gender` |
| Personalitate/narațiune | `archetype`, `backstory`, `backstorySeed`, `socialRole`, `questRole` |
| Familie | `relationRole`, `ageGroup`, `familyId` |
| Comportament | `routineProfile` |
| Structuri | `homePlaceId`, `workPlaceId`, `socialPlaceId` |
| Puncte | `spawnNodeId`, `homeNodeId`, `workNodeId`, `socialNodeId` |

Faptul că un câmp există în model nu garantează că orice comportament/scenariu îl consumă. Nu inventa chei în `config.yml` pentru aceste câmpuri.

Pentru NPC-urile create din plan, `socialRole` și `questRole` sunt salvate în `profile_data.narrative`. Contextul story le expune ca `npc_social_role` și `npc_quest_role` dacă valorile sunt ID-uri valide; aceste semnale pot influența selecția questului, dar nu creează definiții de quest. Nu există un bloc YAML universal `npc.story` pentru setarea lor.

Ancorele `homeAnchor`, `workAnchor` și `socialAnchor` pot furniza contextului story ID-urile place-urilor asociate, rezolvate din mapping la coordonatele ancorelor. Un punct social aflat direct în regiune, fără place, nu primește un ID de clădire. Semnalele sunt descriptive; nu mută NPC-ul și nu repară asocieri lipsă.

### Creare manuală

Comanda este pentru un jucător administrator; poziția lui devine locația inițială:

```text
/ainpc create Ion fierar 36 male creator
```

Sintaxă: `/ainpc create <nume> [ocupatie] [varsta] [gen] [arhetip]`. Vârsta omisă este `30`; genul omis/invalid devine `male`, valorile acceptate fiind `male` și `female`. Numele și ocupația sunt argumente individuale; exemplul folosește token-uri fără spații. Nu adăuga `--confirm` acestei comenzi.

Crearea manuală nu produce de la sine o alocare completă a casei, a locului de muncă și a locului social.

## 4. Proprietățile casei care controlează generarea

`HouseAllocationPlanner` citește casa și node-urile din mapping:

- Este necesar cel puțin un node `npc_spawn` și unul `bed` sau `home` compatibil.
- Capacitatea node-urilor este `min(număr spawn-uri, număr puncte bed/home)`.
- `metadata.max_residents`, alternativ `maxResidents` sau `capacity`, stabilește capacitatea explicită pozitivă. Aceasta nu înlocuiește necesitatea punctelor individuale.
- Numărul cerut prin comandă este limitat de capacități, cu avertismente când este redus.
- Workplaces sunt căutate în aceeași regiune și ordonate după prioritate semantică și ID; locurile sociale sunt căutate separat.
- Destinația socială este locul eligibil cu cele mai puține ID-uri distincte deja asociate în `metadata.social_npc_ids` plus rezervările planului curent; egalitățile folosesc prioritatea semantică și ID-ul. `metadata.max_social_associations` limitează opțional aceste asocieri, iar `0` dezactivează altele noi. `planSettlement` păstrează rezervările între case, astfel încât piața, taverna și grădina să poată fi folosite fără concentrarea tuturor rezidenților la primul loc. Asocierea existentă este persistentă în mapping, dar rezervarea preview-ului nu este; limita nu reprezintă ocupare în timp real sau program de activități.
- Generatorul narativ de populație folosește aceeași numărare a asocierilor persistente și a rezervărilor din propriul preview; la egalitate rotește ordinea locurilor după seed-ul planului. Astfel, un seed identic și un mapping neschimbat produc aceeași asociere, fără a scrie în mapping.
- Pentru locurile de muncă, generatorul narativ scade și el ID-urile distincte din `worker_npc_ids` din `max_workers` (implicit `1`) și rezervă posturile între gospodăriile preview-ului. Un `max_workers` invalid oprește planul; dacă posturile sunt epuizate, rezidenții rămași nu primesc loc de muncă. Preview-ul nu modifică angajații persistați.
- Generatorul narativ omite casele cu `resident_npc_ids` deja populate și limitează fiecare gospodărie la perechi fizice distincte de noduri spawn și bed/home, chiar dacă `max_residents` este mai mare. Dacă nu rămâne nicio casă disponibilă sau niciuna nu are noduri utilizabile, refuză planul în loc să persiste unul gol.
- `metadata.max_workers` stabilește numărul de posturi al workplace-ului: întreg nenegativ, implicit `1`; `0` dezactivează alocarea la muncă fără să dezactiveze rolul social. O valoare invalidă oprește planificarea casei cu eroare.
- Planner-ul scade din `max_workers` ID-urile distincte din `metadata.worker_npc_ids`, apoi rezervările pentru casele anterioare din același apel. Listele acceptă virgulă/punct și virgulă, ignoră spațiile/elementele goale și deduplică fără diferențiere de majuscule, folosind aceeași regulă ca binding-ul. După epuizare, rezidenții noi rămași nu primesc workplace și au ocupația `locuitor`, cu avertisment.
- Pentru fiecare workplace ocupat, warning-ul arată separat angajații existenți, posturile deja rezervate în plan și `max_workers`. Astfel poți distinge lipsa de capacitate persistentă de rezervarea creată doar în preview.
- Binding-urile verifică tipul destinației înainte de scriere: `home` cere locuință, `work` cere workplace, iar `social` cere loc social conform regulilor semantice de spawn. Dacă locuința declară `max_residents` (sau aliasul `maxResidents`/`capacity`), binding-ul home refuză rezidenți noi peste limită și permite reasocierea aceluiași ID. Binding-ul work verifică ocuparea: refuză NPC-uri noi peste `max_workers`, dar permite reasocierea unui ID deja prezent. Reducerea capacității nu elimină automat NPC-uri existente; disponibilitatea pentru alocări noi devine zero. Preview-ul nu rezervă persistent posturi și nu coordonează comenzi separate sau concurente; un plan depășit poate încă eșua la binding, fără rollback automat al întregului spawn.
- Planificarea este pentru case fără rezidenți în mapping: o casă cu `resident_npc_ids` nevid nu este realocată automat. Planul individual este refuzat cu explicație; planul regional omite casa cu avertisment și aplică `maxHouses` după filtrare. Dacă toate casele sunt populate, rezultatul este un refuz explicit, fără alocări noi. Aceasta evită confundarea angajaților existenți cu rezidenți noi care ar ocupa încă o dată aceleași posturi.
- Ocuparea folosește metadata din mapping, nu o reconciliere cu SQL sau entitățile live. ID-urile vechi/necunoscute rămân ocupare până la verificarea administrativă; planner-ul nu le șterge. O casă populată numai în DB, dar nemarcată în mapping, nu poate fi recunoscută prin această verificare. Inspectează și repară relațiile existente înainte de replanificare; planul nou nu este un instrument de migrare sau completare parțială a familiilor existente.
- Ocupația este preluată din `metadata.profession` sau `metadata.occupation` a workplace-ului. Tipul `forge` singur nu setează automat textul ocupației la „fierar”; fallback-ul metodei este `Locuitor`.
- Proprietarul principal al alocării este primul `npcKey` planificat. Pentru mai mulți rezidenți se creează un `familyId` și roluri deterministe: `father`, `mother`, apoi `child`.
- Vârstele implicite din acest planner sunt `30` pentru un singur rezident, respectiv `36`, `35`, apoi `16`; genurile alternează `male/female`. Acestea sunt alegeri din cod, nu valori configurabile per casă prin chei YAML dedicate.

Exemple de fragmente de metadata, de inserat în place-urile deja definite:

```yaml
metadata:
  max_residents: "2"
```

```yaml
metadata:
  profession: fierar
  max_workers: "2"
```

Primul fragment aparține casei; al doilea, fierăriei/workplace-ului. Pentru doi rezidenți pregătește două perechi de node-uri, nu doar `max_residents: "2"`.

## 5. Planificare și spawn

Pentru o singură casă cu un loc disponibil, precum [exemplul de hartă](configurare-lume-harta-generare.md):

```text
/ainpc world household plan sat_demo:casa_01 1
/ainpc world household spawn sat_demo:casa_01 1 --confirm
/ainpc world household place sat_demo:casa_01
/ainpc world bindings place sat_demo:casa_01 20
/ainpc audit spawn
/ainpc world save
```

Alternativ, pentru până la trei case dintr-o regiune care are capacitatea necesară:

```text
/ainpc world settlement plan sat_demo 3
/ainpc world settlement spawn sat_demo 3 --confirm
/ainpc world household list 20
/ainpc world bindings list 20
/ainpc audit spawn
```

`plan` este dry-run; nu folosi `spawn --dry-run` ca substitut. Parametrul `3` al settlement-ului înseamnă `maxHouses`, nu trei NPC-uri. Nu executa ambele variante ca două metode independente de populare a acelorași case fără să inspectezi populația deja existentă.

## 6. Legătura NPC–structură

| Relație | Reprezentare | Semnificație |
|---|---|---|
| Locuință | `homePlaceId`, `homeNodeId`, ancora NPC | Unde locuiește/doarme |
| Muncă | `workPlaceId`, `workNodeId`, ancora NPC | Unde lucrează |
| Social | `socialPlaceId`, `socialNodeId`, ancora NPC | Unde se întâlnește cu alții |
| Proprietar | `WorldPlace.owner_npc_id` | Cine deține structura; diferit de rezident/angajat/vizitator |
| Household | Alocare și rezidenți persistenți | Grupul de rezidenți ai unei case |
| Familie | `familyId` și relații | Legături familiale; nu echivalează cu proprietatea |

Binding-urile persistente din `npc_world_bindings` conțin `npc_id`, `npc_uuid`, `npc_name`, cele trei place-uri, cele trei node-uri, `family_id`, `source`, `created_at`, `updated_at`. Identificatorul de mapping `npc_12` derivă din ID-ul DB `12`; UUID-ul entității este alt identificator.

Pentru un NPC manual numit `Ion`, după ce casa, fierăria și piața există în mapping:

```text
/ainpc world bind npc Ion sat_demo:casa_01 sat_demo:fierarie sat_demo:piata --confirm
/ainpc world bindings npc Ion
/ainpc world save
```

`world bind npc` acceptă numele sau `nearest`, nu ID numeric/`npc_12`/UUID. `world bindings npc`, care inspectează, acceptă mai multe tipuri de selector. `world household resident` acceptă ID numeric, nume sau `nearest`. Pentru numele generate cu spații folosește un selector compatibil precum `nearest` la bind.

`-` sau omiterea work/social păstrează valorile existente; nu este o comandă de ștergere a binding-ului. Alegerea node-urilor/ancorelor este făcută de cod după tip și semantică, cu fallback-uri; verifică rezultatul, nu numai place-ul transmis.

Comanda manuală și confirmarea cu wand folosesc aceeași selecție de ancore ca planner-ul. Pentru muncă, `workstation`/`work` și `metadata.semantic: work_anchor` au prioritate față de `interaction`, apoi `npc_spawn`. Un punct de spawn aflat mai aproape de centru sau o intrare aflată mai jos nu înlocuiesc postul explicit. Un node marcat `semantic: work_anchor` este acceptat și la validarea alocării; metadata se păstrează la salvarea și reîncărcarea mapping-ului.

`world bind npc` și confirmarea cu wand verifică tipul și capacitatea tuturor destinațiilor cerute înainte de salvarea profilului NPC. Dacă verificarea refuză o destinație, ancorele NPC și metadata din mapping rămân neschimbate. La scriere, `world bind npc` revalidează și aplică împreună asocierile home/work/social sub același lock, astfel încât un workplace devenit plin nu adaugă parțial rezidentul în casă. Un refuz apărut după salvarea profilului restaurează ancorele anterioare. Persistența profilului, mapping-ul și `npc_world_bindings` nu formează încă o tranzacție comună pentru erori neprevăzute.

Pentru spawn household, `HouseholdOwnerBinding` rezolvă `primaryOwnerNpcKey` la un singur NPC persistent creat/reutilizat. Nu atribui proprietatea tuturor rezidenților prin copierea lui `owner_npc_id` în fiecare plan.

## 7. Persistență, reutilizare și eșecuri

`NpcSpawnPlan.sourceKey()` construiește o identitate de sursă pornind din scope și cheia planului. Orchestratorul poate reutiliza NPC-uri compatibile; repetarea comenzii nu trebuie folosită ca mecanism de reparare fără inspectarea stării existente.

Persistența are straturi distincte: NPC/profil, household/rezidenți, binding DB, metadata/owner din mapping și jurnalul batch-ului. Spawn-ul și binding-ul post-spawn nu formează o singură tranzacție.

- Rollback-ul de spawn urmărește NPC-urile create de operația curentă; nu trebuie interpretat ca ștergere a tuturor NPC-urilor reutilizate.
- În cazul eșecului de persistență household, implementarea curentă poate opri settlement-ul fără rollback global și fără ștergerea household-urilor existente, pentru recuperare. Operația este incompletă.
- Binding-ul post-spawn raportează `COMPLETE`, `PARTIAL`, `FAILED` sau `NOT_APPLICABLE`. Mesajul „Succes parțial” poate însemna că NPC-urile au rămas spawnate.
- `/ainpc world save` nu substituie salvarea în DB și nici sincronizarea overlay-ului.

După spawn verifică household-ul, binding-urile, proprietarul și `/ainpc audit spawn`. După [salvare și sincronizarea overlay-ului](configurare-lume-harta-generare.md), verifică aceleași relații după restart.

## 8. Destinații valide pentru rutine

Sloturile `HOME`, `WORK` și `SOCIAL` consumă ancorele `homeAnchor`, `workAnchor` și `socialAnchor` ale NPC-ului. Dacă destinația lipsește, numele lumii este gol sau coordonatele nu sunt finite, resolverul întoarce slotul și starea `IDLE`, fără destinație, cu motiv explicit. Un program de muncă fără workplace nu mai raportează artificial `WORKING` și nu selectează automat o activitate socială în locul muncii.

După configurarea unei ancore valide, următoarea rezolvare poate selecta din nou slotul programat. Verificarea este logică: nu confirmă că lumea este încărcată, că traseul este accesibil sau că NPC-ul a ajuns la destinație. Nu activează automat rutinele și nu creează binding-uri noi.

### Execuție: deplasare înaintea activității

Pentru programul normal, `RoutineService` verifică suplimentar lumea și chunk-ul destinației, fără să încarce chunk-ul prin verificare. Aplică starea de activitate numai când poziția reală a NPC-ului este în aceeași lume, în raza 3D `routine.arrival_radius` (limita inclusă). Configurația livrată folosește `8.0`; această rază poate include exteriorul unei clădiri mici și nu înseamnă ocuparea exactă a mobilierului.

| Situație | Rezultat al execuției rutinei |
|---|---|
| Poziție în raza destinației încărcate | Starea programată, de exemplu `WORKING`, `RESTING` sau `SOCIALIZING` |
| Navigație acceptată, încă departe | `WALKING`; activitatea începe la o reevaluare care confirmă sosirea |
| Navigație refuzată, fără teleport reușit | `IDLE`, fără raportarea falsă a activității |
| Teleport acceptat și poziție reală confirmată în rază | Starea programată; se actualizează cooldown-ul de teleport |
| Teleport anulat sau redirecționat în afara razei | `IDLE`; nu este contabilizat ca sosire și nu actualizează cooldown-ul |
| Lume lipsă, chunk neîncărcat sau coordonate invalide | `IDLE`, fără încercare de deplasare |

Contorul existent `movedNpcs` include pornirile de navigație acceptate, nu numai sosirile. `plannedRoutineActivity`, preview-ul și evenimentul de schimbare a rutinei descriu intenția programată; starea curentă reflectă execuția. Somnul poate fi încheiat la reevaluarea programului, dar conversațiile, comerțul și stările ocupate/de pericol rămân protejate.

Override-urile coordonatorului folosesc acum aceeași validare a ancorei, lumii, chunk-ului și sosirii. Navigația lor respectă `routine.natural_movement.enabled` și `max_distance`, numai în aceeași lume. Un drum acceptat produce `WALKING`; un drum refuzat nu mărește `movedNpcs`. Sosirea la ancora impusă produce `IDLE`, păstrând semantica existentă: `forcedSlot` etichetează intenția, nu autorizează automat muncă, comerț sau socializare. Lipsa ancorei produce așteptare, fără alegerea unei destinații implicite. Override-urile nu folosesc teleport; expirarea și pauza rutinei își păstrează comportamentul existent. Contorul `evaluatedNpcs` include acum fiecare override activ evaluat, inclusiv unul deja sosit sau fără destinație.

Validarea nu acoperă simularea independentă `DecisionEngine`. Nu certifică un traseu complet accesibil, un bloc sigur de staționare, interiorul clădirii sau o tranzacție comercială. Nu schimbă setările de activare ori teleport și nu opresc navigația pornită de alte sisteme. Validează pe un server de test traseul și tranziția `WALKING` → activitate înainte de producție.

### Trasee complete înainte de pornire

Navigația naturală, atât pentru programul normal cât și pentru override-uri, calculează mai întâi un traseu. Îl pornește numai dacă rezultatul există, `canReachFinalPoint()` este adevărat, iar punctul final are coordonate finite, în aceeași lume și în raza 3D `routine.arrival_radius` a ancorei. Este transmis exact traseul verificat, nu se cere recalcularea lui printr-un nou apel cu destinația. Semantica rezultatului este descrisă în [API-ul Paper pentru PathResult](https://jd.papermc.io/paper/1.21.11/com/destroystokyo/paper/entity/Pathfinder.PathResult.html).

Un traseu absent, parțial sau cu punct final prea îndepărtat este refuzat. Pentru programul normal rămâne posibil fallback-ul de teleport, numai dacă setările și condițiile existente îl permit; override-urile nu teleportează. Pornirea refuzată de motor rămâne un eșec, chiar dacă traseul calculat era valid. Un traseu pornit nu confirmă sosirea: starea rămâne `WALKING` până la verificarea poziției reale.

Verificarea este conservatoare: un traseu raportat inaccesibil este refuzat chiar dacă punctul său final pare apropiat. Nu garantează că traseul rămâne liber după calcul, nu certifică siguranța teleportului și nu adaugă dependențe sau modificări automate de configurație.

Probă pe Paper înainte de producție: într-o lume de test, cu teleportul rutinei dezactivat, verifică o destinație accesibilă (`WALKING`, apoi activitatea), una blocată (fără activitate falsă) și un override blocat (fără deplasare contabilizată). Deschide apoi accesul și verifică reluarea la următoarea reevaluare. Testele JVM simulează răspunsurile motorului; nu înlocuiesc această probă cu geometria reală.

### Diagnostic: intenție și execuție

GUI-ul rutinelor separă acum `Goal planificat` și `Stare tinta` din preview de `Stare efectiva` și `Obiectiv imediat` ale NPC-ului. Preview-ul descrie programul, nu dovedește că NPC-ul muncește deja sau a ajuns la destinație.

La execuție, obiectivul imediat explică situația: ancoră lipsă/invalidă, lume sau destinație indisponibilă, poziție NPC invalidă, rază de sosire invalidă ori chunk neîncărcat. În mers indică destinația și faptul că sosirea nu este încă confirmată. După sosire, obiectivul programat îl înlocuiește pe cel de așteptare; pentru override, NPC-ul așteaptă la destinația impusă, fără a inventa muncă.

`deplasare indisponibila; sosire neconfirmata` nu înseamnă obligatoriu un drum blocat: deplasarea poate fi dezactivată, distanța poate depăși limita, traseul poate fi refuzat sau teleportul poate fi neeligibil/anulat. `teleport acceptat; sosire neconfirmata` indică un rezultat acceptat fără apropierea reală necesară. Diagnosticul nu pretinde că distinge cauze pe care callback-urile nu le raportează.

Pentru textul complet folosește `/ainpc routine status <nume>` și câmpul `Obiectiv curent`; GUI-ul limitează lungimea afișată. Acesta este `currentGoal`, un obiectiv runtime comun cu simularea, nu un jurnal imuabil de erori. Alte sisteme îl pot modifica, iar o rutină dezactivată/pauzată nu îl reîmprospătează. Deschiderea GUI-ului sau a statusului nu execută rutina. Nu se adaugă chei de configurare, tabele ori istoric persistent.

### Verificarea destinației înainte de teleport

Fallback-ul de teleport al rutinei normale verifică acum destinația înainte de apelul Bukkit. Nu modifică setările de activare, distanțele sau cooldown-ul. Override-urile rămân fără teleport, iar celelalte comenzi/sisteme de teleport nu sunt schimbate.

- Volumul real al entității este translatat la țintă, fără modificarea bounding box-ului original. Coordonatele, dimensiunile și limitele verticale ale lumii sunt verificate; volumul de inspectat este limitat la 512 celule, iar fiecare dimensiune a entității la 16 blocuri.
- Chunk-urile acoperite de volum și o margine orizontală de un bloc trebuie să fie deja încărcate. Verificarea lor precedă orice citire de bloc; nu se solicită încărcarea explicită a chunk-urilor.
- Fiecare coloană de sub NPC trebuie să ofere sprijin la înălțimea picioarelor. Se folosesc formele reale de coliziune, nu numai tipul materialului: o dală inferioară nu susține o ancoră aflată cu jumătate de bloc deasupra ei. Criteriul este conservator și poate refuza suprafețe compuse pe care NPC-ul ar putea sta manual.
- Sunt refuzate lichidele, blocurile waterlogged și o listă explicită de pericole din volum/sprijin: foc, lavă, cactus, magma, focuri de tabără (inclusiv stinse), tufe de fructe, wither rose, powder snow și portaluri.
- Ultima verificare folosește coliziunea volumului în lumea destinației, inclusiv la schimbarea lumii. [API-ul Paper](https://jd.papermc.io/paper/1.21.11/org/bukkit/RegionAccessor.html#hasCollisionsIn(org.bukkit.util.BoundingBox)) include blocurile, world border-ul și entitățile cu coliziune rigidă; aceasta nu este o rezervare exclusivă a spațiului față de toate NPC-urile.

Refuzul nu apelează teleportul și nu este contabilizat drept sosire. Obiectivul generic `deplasare indisponibila; sosire neconfirmata` poate însemna și o destinație respinsă de aceste verificări. Anularea Bukkit și confirmarea poziției reale după teleport își păstrează verificările existente.

Aceste condiții verifică ținta solicitată la momentul apelului, nu constituie o garanție universală de siguranță: un plugin poate redirecționa teleportul în eveniment, geometria se poate schimba ulterior, iar lista de pericole nu acoperă toate efectele posibile. Nu există căutare automată a unei poziții alternative, rollback de teleport sau validare globală pentru toate sistemele de mișcare.

Pe un server Paper de test, verifică separat o podea completă, un obstacol la cap, o gaură sub jumătate din NPC, apă, o dală la înălțime greșită și o margine de chunk neîncărcată. Cu fallback-ul permis și distanța eligibilă, numai cazul valid trebuie să teleporteze; celelalte trebuie să rămână fără teleport și fără activitate falsă. Testele JVM folosesc răspunsuri Paper simulate; această probă nu este executată automat.

## Surse și continuare

- [HouseAllocationPlanner](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseAllocationPlanner.kt), [NpcSpawnPlan](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/NpcSpawnPlan.kt), [NpcSpawnOrchestrator](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/NpcSpawnOrchestrator.kt).
- [Comenzi și binding manual](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt), [creare manuală](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandMisc.kt), [politica de confirmare](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandMutationPolicy.kt).
- [NpcWorldBindingService](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/NpcWorldBindingService.kt), [HouseholdOwnerBinding](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/HouseholdOwnerBinding.kt), [SettlementSpawnFailureHandler](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/SettlementSpawnFailureHandler.kt).
- [Arhitectura binding-urilor](../architecture/npc-world-bindings.md), [households persistente](../architecture/households-persistente.md), [configurare story](configurare-story-lume-npc.md).
