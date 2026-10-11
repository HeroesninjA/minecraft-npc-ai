# Mecanicile MCP pentru o simulare AINPC

## Scop

MCP-ul nu trebuie să fie doar un API pentru citirea unui snapshot. Pentru o simulare credibilă, el trebuie să fie stratul de observare și control al unei simulări deterministe, în timp ce motorul jocului rămâne responsabil pentru aplicarea regulilor și efectelor.

## Mecanicile principale

### 1. Timp și tick-uri

- tick de simulare, zi/noapte și anotimpuri;
- programe pentru NPC-uri: muncă, odihnă, hrană și socializare;
- pauză, accelerare și reluare;
- durată și rezultat pentru fiecare acțiune.

### 2. NPC-uri autonome

- nevoi: foame, energie, siguranță, bani și apartenență;
- personalitate, memorie și priorități;
- scopuri pe termen scurt și lung;
- planificare bazată pe obiective, posibilități și riscuri.

### 3. Tranziții de stare

- stări precum `IDLE`, `WORKING`, `TIRED`, `SLEEPING`, `SUSPICIOUS` și `HOSTILE`;
- tranziții declanșate de evenimente sau condiții ale lumii;
- motivul fiecărei tranziții înregistrat în jurnal.

### 4. Relații și memorie

- relații NPC–jucător și NPC–NPC;
- încredere, afecțiune, respect și familiaritate;
- memorie pozitivă sau negativă cu expirare;
- reputație și zvonuri care se propagă.

### 5. Economie

- venituri, costuri, salarii, taxe și comerț;
- stocuri și prețuri dinamice;
- datorii, faliment și investiții;
- validare strictă pentru sume și durate.

### 6. Lume reactivă

- vreme, resurse, pericole și populație;
- facțiuni și control teritorial;
- consecințe persistente pentru acțiunile jucătorului;
- evenimente emergente, nu doar questuri scriptate.

### 7. Questuri dinamice

- obiective generate din starea lumii;
- efecte asupra NPC-urilor, economiei și relațiilor;
- succes, eșec și abandon;
- încărcarea progresului la reconnect și protecție la reload invalid.

### 8. Evenimente și cauzalitate

Fiecare eveniment ar trebui să conțină:

- cauză;
- efect;
- autor sau sursă;
- timestamp și tick;
- entitățile afectate.

Exemplu: furt → lipsă de marfă → preț crescut → conflict → quest nou.

### 9. Persistență și replay

- snapshot atomic;
- jurnal de evenimente;
- seed determinist;
- replay pentru reproducerea bugurilor;
- rollback pe tick sau pe eveniment.

## Tool-uri MCP recomandate

- `simulation.status`;
- `simulation.pause` / `simulation.resume` / `simulation.step`;
- `simulation.advance`;
- `npc.observe` / `npc.plan` / `npc.act`;
- `world.observe`;
- `economy.inspect`;
- `quest.inspect`;
- `event.list` / `event.inject`;
- `timeline.replay`;
- `state.diff`.

Tool-urile care modifică starea trebuie să aibă permisiuni explicite, validare, idempotency key, actor și audit. MCP nu trebuie să permită executarea arbitrară de comenzi de consolă din conținutul unui feature pack.

## Ordinea recomandată de implementare

1. `SimulationClock` controlabil;
2. event log și event bus;
3. tranziții de stare pentru NPC-uri;
4. planner autonom pentru NPC-uri;
5. consecințe integrate în lume și economie;
6. replay, diff și instrumente de debugging.

## Priorități pentru proiectul actual

Proiectul are deja bază pentru NPC-uri, relații, questuri, economie și snapshot-uri. Cele mai importante completări sunt:

- timp simulabil și controlabil;
- jurnal de evenimente;
- replay determinist;
- încărcarea sigură a progresului la reconnect;
- acțiuni MCP controlate, nu doar tool-uri read-only;
- conectarea providerului AI la fluxul real de dialog;
- testare de integrare pentru lanțurile de cauzalitate.

## Prioritatea simulării lumii și a NPC-urilor

Ordinea recomandată este:

1. motor comun de simulare;
2. simularea minimă a lumii;
3. simularea autonomă a NPC-urilor.

NPC-urile trebuie să ia decizii pe baza unei realități simulate: timp, resurse, locații, economie, pericole și evenimente. O lume statică limitează comportamentul NPC-urilor, iar NPC-urile autonome fără reguli de lume produc rezultate greu de controlat.

### Structura simulării lumii

#### Starea lumii

Include:

- timpul curent, ziua/noaptea și anotimpul;
- vremea;
- regiuni, sate, clădiri și resurse;
- populație și facțiuni;
- economie și prețuri;
- pericole, boli și conflicte;
- questuri și evenimente active.

#### Regulile de evoluție

La fiecare tick:

```text
timpul avansează
vremea se actualizează
resursele se consumă sau se regenerează
prețurile se ajustează
evenimentele își verifică condițiile
NPC-urile primesc noua stare a lumii
```

Simularea trebuie să fie deterministă: același seed și aceleași acțiuni trebuie să producă același rezultat.

#### Evenimente

Modificările importante ar trebui reprezentate prin evenimente precum:

- `ResourceDepleted`;
- `WeatherChanged`;
- `NpcArrived`;
- `NpcDied`;
- `TradeCompleted`;
- `FactionConflictStarted`;
- `QuestCompleted`.

Fiecare eveniment trebuie să păstreze timestamp-ul, sursa, cauza și efectele.

### MVP recomandat

Prima versiune poate conține:

- un sat cu 3–5 locații;
- ciclu zi/noapte;
- hrană, bani și materiale;
- trei meserii NPC;
- piață cu prețuri simple;
- NPC-uri care muncesc, consumă hrană și dorm;
- eveniment de lipsă de hrană;
- efecte în lanț: preț crescut, nemulțumire și quest nou.

După acest MVP se pot adăuga vremea complexă, facțiunile, conflictele și economia avansată.

Principiul de bază este: lumea oferă constrângerile, iar NPC-urile produc comportamentul emergent.

## Biomuri, structuri, sate și orașe

Structurile, biomurile, orașele și satele fac parte din simularea lumii.

```text
WorldSimulation
├── Geography
│   ├── Biome
│   ├── Terrain
│   ├── Climate
│   └── Resources
├── Settlements
│   ├── Village
│   ├── Town
│   └── City
├── Structures
│   ├── House
│   ├── Farm
│   ├── Shop
│   ├── Fortress
│   └── Ruins
└── DynamicState
    ├── Population
    ├── Ownership
    ├── Damage
    ├── Activity
    └── Events
```

### Roluri

- **Biomul** definește mediul: climă, resurse, animale și pericole.
- **Structura** este un obiect construit sau generat în lume.
- **Satul sau orașul** este o entitate organizată, cu structuri, NPC-uri, economie și reguli.
- **Lumea Minecraft** este stratul fizic în care starea simulată este reprezentată prin blocuri, entități și locații.

Un sat trebuie să includă mai mult decât clădirile sale:

```text
Settlement
├── locație
├── biom
├── populație
├── facțiune
├── resurse
├── meserii
├── economie
├── relații
├── reguli
└── evenimente
```

### Separarea modelului de reprezentare

AINPC ar trebui să aibă două niveluri:

1. **World Model** — datele logice despre biomuri, sate, orașe, structuri, resurse și proprietari;
2. **Minecraft Adapter** — blocuri, entități, chunk-uri și locații reale.

Simularea poate continua astfel și când chunk-ul nu este încărcat, iar lumea Minecraft rămâne reprezentarea vizuală a stării simulate.

## Generarea unui sat complet la prima inițializare

### Comportamentul dorit

La prima pornire pe o lume aleasă, sistemul ar trebui să creeze un sat recognoscibil, cu clădiri fizice bine definite: case, cârciumă, școală și, dacă profilul satului o cere, castel. Fiecare clădire trebuie să aibă un ID stabil, limite, tip, intrare și noduri utile. O casă trebuie să poată fi asociată explicit cu proprietarul și locuitorii ei, de exemplu „Casa lui Gigel”. NPC-urile primesc locuință, loc de muncă și loc de socializare, apoi urmează o rutină zilnică între aceste destinații.

```text
alege terenul → planifică satul → construiește și verifică structurile
→ creează mapping pentru regiune, clădiri și noduri
→ alocă gospodării și locuri de muncă → creează și leagă NPC-urile
→ validează ancorele home/work/social → activează rutinele
```

### Starea actuală a proiectului

- `AINPCPlugin.onEnable` salvează resursele și inițializează serviciile, dar nu pornește acest flux complet la prima inițializare.
- `/ainpc world settlement auto <regionId>` scanează un sat vanilla existent și produce mapping semantic și planuri `HouseAllocation`. Generarea NPC-urilor este un pas separat: `/ainpc world settlement spawn <regionId>`.
- Scannerul construiește case semantice din grupuri de paturi, cu nume generice `House 1`, `House 2` și noduri de tip pat, home și intrare. Nu atribuie singur numele sau proprietarul casei.
- `routine.enabled` este `false` în configurația implicită. `worldgen.enabled` este tot `false` în fișier, dar nu a fost identificată o citire a acestei chei în codul Kotlin; activarea ei singură nu garantează construcția. `family.auto_generate` privește familiile, nu generarea satului.
- Definiția `demo_sat` din `settlements.yml` conține centru, rază și profil; nu este un inventar complet al clădirilor și proprietarilor. `building_templates.yml` are profiluri semantice pentru casă, fierărie și fermă, însă nu pentru școală sau un castel complet. Executorul fizic are și template-uri implicite pentru tavernă.
- Motorul rutinei folosește ancorele `homeAnchor`, `workAnchor` și `socialAnchor`. NPC-urile nespawnate, fără țintă sau cu țintă invalidă nu ajung la destinație.

### Ce trebuie implementat

1. **Bootstrap pe lume:** detectează prima inițializare pentru lumea selectată și persistă un ID de sat, un seed și versiunea planului; la repornire reia pașii incompleți fără dubluri.
2. **Plan de sat verificabil:** alege teren potrivit și definește pozițiile, amprentele, orientarea, căile dintre clădiri și template-urile necesare pentru fiecare tip de structură.
3. **Construcție și mapping coerente:** după construirea și verificarea blocurilor, creează regiunea, locurile și nodurile corespunzătoare clădirilor reale. Păstrează ID-uri stabile și validează intrările, paturile și punctele de lucru.
4. **Identitate și alocare:** creează gospodării persistente, asociază proprietarul și locuitorii cu fiecare casă, apoi leagă NPC-urile de casă, locul de muncă și locul social. Numele afișat poate deveni „Casa lui Gigel” după stabilirea proprietarului.
5. **Pornirea rutinei:** verifică existența ancorelor și accesibilitatea traseelor; activează rutina după ce satul și NPC-urile sunt pregătite. Raportează explicit NPC-urile fără home/work/social și motivul pentru care nu se deplasează.
6. **Recuperare și testare:** înregistrează starea fiecărei etape, erorile și rezultatele. Testează prima pornire, repornirea, un pas eșuat, lipsa unui template și un NPC care merge de acasă la lucru și revine.

Prioritatea este conectarea acestor etape într-un singur flux sigur la prima inițializare. Setarea unui flag în configurație nu creează de una singură clădirile, mapping-ul, proprietarii și rutinele.

### Blocaje suplimentare confirmate în cod

1. **Incompatibilitate între scanner și planner:** `SemanticVillageMapper.createHousePlaces` creează pentru case noduri `bed`, `home` și, când există ușă detectată, `entrance`, dar nu creează `npc_spawn`. `HouseAllocationPlanner.plan` cere atât `npc_spawn`/`spawn`, cât și `bed`/`home`; în lipsa nodului de spawn respinge planul. Acesta este un blocaj direct pentru fluxul scanare → alocare → spawn NPC.
2. **Dependență de mapping-ul complet:** `planSettlement` cere cel puțin o casă mapată în regiune. Fără workplace, locuitorii primesc ocupația generică `locuitor`; fără loc social, planul nu setează destinația socială.
3. **Plasare geometrică simplificată:** `BuildingAutoPlaceService.findFreeOffset` caută un dreptunghi fără suprapuneri cu locurile mapate, iar `autoPlace` folosește `region.minY + 1` ca înălțime. Din această cale nu rezultă validarea terenului real, a pantei sau a accesului dintre clădiri; o poziție acceptată geometric poate fi nepotrivită în Minecraft.
4. **Succes semantic fără confirmarea construcției:** `WorldGenService.applyNativePatch` poate construi blocuri după aplicarea mapping-ului, dar returnează rezultatul patch-ului fără a integra rezultatele `buildPlacePhysically`. O construcție fizică eșuată poate lăsa mapping-ul raportat drept reușit.
5. **Condiții runtime ale rutinei:** `RoutineService.runRoutineTick` sare peste NPC-urile nespawnate și peste țintele invalide sau aflate în chunk-uri neîncărcate. O ancoră în mapping nu garantează singură deplasarea NPC-ului.

### Verificări prioritare

1. Adaugă un nod `npc_spawn` sigur pentru fiecare casă importată sau definește un fallback explicit în planner, apoi testează fluxul scanare → planificare → spawn.
2. Validează pe teren fizic amprenta, înălțimea, intrarea și accesul înainte de confirmarea clădirii.
3. Propagă eșecul construcției fizice în rezultatul final și păstrează dovezi pentru diagnostic.
4. După spawn, verifică ancorele și raportează pe NPC de ce o rutină nu produce deplasare.

## Analize suplimentare: construcție, gospodării și rutine

Data consolidării: 2026-10-01.
Status: constatări ale auditului inițial; progresul ulterior al remedierilor este consemnat separat la finalul documentului.

Această secțiune păstrează cele trei analize suplimentare ale fluxului de sat. Completează blocajele anterioare; nu înlocuiește verificarea configurației sau a versiunii instalate. Ordinea intervențiilor și criteriile de acceptare sunt în [planul de remediere în pași](../planning/plan-remediere-generare-sat-si-rutine.md).

### Metodă și limite

- Surse locale Kotlin și configurații, inclusiv un worktree cu modificări preexistente.
- Probe izolate prin JShell pe JAR-ul existent `ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar`, fără rebuild pentru aceste probe și fără server Paper pornit.
- Proba de persistență a folosit exclusiv SQLite în memorie; baza reală și lumea Minecraft nu au fost modificate.
- Proba sesiunii de scanare a folosit materiale simulate și un clasificator de test pentru pat/aer. Clasificatorul Bukkit complet nu a putut fi executat offline, deoarece cere registrele serverului. Concluzia despre numărarea blocurilor este susținută și de sursă.
- Rezultatele izolate confirmă comportamentele descrise, nu validează integral pluginul. Exemplele nu reprezintă teste de regresie adăugate în repository.
- Referințele indică sursa analizată; simbolurile sunt reperul stabil dacă liniile se schimbă.

### A01–A06: construcția și definițiile clădirilor

**A01. Fluxurile de comandă verificate nu execută construcția fizică.** `building auto-place` folosește `BuildingAutoPlaceService`, iar aplicarea patch-ului nativ apelează direct `VillagePatchApplier`. Inițializarea `WorldGenService` nu conectează automat aceste comenzi la plasarea blocurilor. Comenzile curente declară intenționat rezultat semantic; lipsa este integrarea necesară pentru un sat fizic complet, nu promisiunea că aceste comenzi existente ar construi deja.

Surse: [AINPCCommand.kt:1007](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt#L1007), [AINPCCommand.kt:2376](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt#L2376).

**A02. ID-urile template-urilor implicite sunt incompatibile.** Registry-ul semantic definește `house_small`, executorul fizic definește `small_house`, iar calea de build cere același ID în ambele. `tavern` există fizic, dar lipsește din registry-ul semantic implicit. Doar conectarea executorului nu elimină eroarea `Template not found`.

Surse: [BuildingTemplateRegistry.kt:28](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/settlement/BuildingTemplateRegistry.kt#L28), [WorldGenService.kt:107](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/worldgen/WorldGenService.kt#L107), [BuildingTemplateExecutor.kt:97](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/worldgen/BuildingTemplateExecutor.kt#L97).

**A03. Dimensiunile și ancorele diferă între definițiile semantice și fizice.** Fierăria semantică are amprentă declarată `7×6`, iar fundația fizică are `9×7`. Pentru casă, ancora semantică de pat este `(2,1,2)`, iar patul fizic este la `(5,1,1)`. Uniformizarea ID-urilor fără reconcilierea geometriei păstrează mapping-ul greșit.

Surse: [BuildingTemplateRegistry.kt:31](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/settlement/BuildingTemplateRegistry.kt#L31), [BuildingTemplateExecutor.kt:130](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/worldgen/BuildingTemplateExecutor.kt#L130).

**A04. Template-urile implicite sunt rescrise la inițializare.** Provider-ul înregistrează din nou template-urile, iar `saveTemplate` folosește `writeText` fără protejarea unui fișier existent. Personalizările fișierelor implicite, precum `small_house.json`, `forge.json`, `farm.json` și `tavern.json`, se pot pierde la restart. Constatarea nu privește automat toate fișierele custom.

Surse: [WorldGenService.kt:295](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/worldgen/WorldGenService.kt#L295), [BuildingTemplateExecutor.kt:70](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/worldgen/BuildingTemplateExecutor.kt#L70).

**A05. `building_templates.yml` nu alimentează registry-ul utilizat în calea analizată.** Fișierul este copiat la inițializare, însă registry-ul încarcă definiții hardcodate. Editarea YAML-ului nu modifică acele definiții runtime.

Surse: [AINPCPlugin.kt:128](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/AINPCPlugin.kt#L128), [BuildingTemplateRegistry.kt:26](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/settlement/BuildingTemplateRegistry.kt#L26).

**A06. Rutina singură nu poate face tranziția somn → muncă.** Prioritatea `SLEEPING` este 80, iar `WORKING` are 40; `changeState` respinge coborârea. Rutina ignoră rezultatul și continuă actualizarea activității. Proba a returnat `accepted=false, final=SLEEPING`. Un alt mecanism, precum tranziția internă a simulării, poate schimba starea; nu rezultă că orice NPC rămâne permanent adormit.

Surse: [AINPC.kt:297](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/AINPC.kt#L297), [RoutineService.kt:136](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineService.kt#L136).

### A07–A11: persistența și repartizarea populației

**A07. ID-urile gospodăriilor se ciocnesc între sate.** Pentru mai mulți rezidenți, planner-ul derivă `family_house_1` din ID-ul local `house_1`, fără regiune. `HouseAllocation.householdId()` reutilizează ID-ul familiei. Salvarea celei de-a doua gospodării face upsert peste prima și elimină rezidenții considerați vechi. În SQLite temporar, după salvarea `sat_a:house_1` și `sat_b:house_1`, au rămas o gospodărie și doi rezidenți, numai pentru `sat_b`. Sunt pierdute asocierile gospodăriei, nu șterse automat NPC-urile.

Surse: [HouseAllocationPlanner.kt:72](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseAllocationPlanner.kt#L72), [HouseAllocation.kt:37](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseAllocation.kt#L37), [HouseholdPersistenceServiceState.kt:810](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseholdPersistenceServiceState.kt#L810).

**A08. Eșecul salvării gospodăriei poate rămâne doar avertisment.** `persistHousehold` prinde `SQLException` și adaugă un warning; apelantul poate întoarce în continuare `HouseholdSpawnResult.success`. NPC-urile pot exista fără înregistrarea persistentă completă a gospodăriei. Acest rezultat este distinct de raportarea deja existentă a binding-ului post-spawn.

Sursă: [NpcSpawnOrchestrator.kt:436](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/NpcSpawnOrchestrator.kt#L436).

**A09. Repartizarea meseriilor reîncepe pentru fiecare casă.** `workplaces.getOrNull(index)` utilizează indexul rezidentului din casă, fără rezervare globală a capacității atelierelor. În satul demonstrativ, primii și singurii locuitori ai celor patru case au primit aceeași fierărie și ocupația `Fierar`, deși există și alte workplaces.

Sursă: [HouseAllocationPlanner.kt:81](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseAllocationPlanner.kt#L81).

**A10. Reutilizarea regiunii nu face importul repetabil.** Cu `allowExistingRegion=true`, un al doilea import al aceluiași sat încearcă să recreeze `bell_1` și eșuează. Reutilizarea unei regiuni existente, introdusă anterior, nu este echivalentă cu reconcilierea clădirilor și nodurilor deja importate. Repetarea comenzii după un rezultat parțial nu repară automat fluxul.

Surse: [SemanticVillageMapper.kt:149](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/scan/SemanticVillageMapper.kt#L149), [AutoSettlementGenerator.kt:32](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/AutoSettlementGenerator.kt#L32).

**A11. Profilurile de comportament sunt recitite la fiecare NPC evaluat.** `resolveProfile` apelează `loadAll` când există plugin, iar acesta golește colecția și parsează din nou fișierul YAML. Este un cost repetat pe calea rutinei; impactul asupra latenței nu a fost măsurat.

Surse: [RoutineProfileResolver.kt:16](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineProfileResolver.kt#L16), [BehaviorProfileLoader.kt:36](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/BehaviorProfileLoader.kt#L36).

### A12–A17: case, proprietari, meserii și decizii

**A12. Ultimul rezident legat de casă devine proprietar.** Binding-ul post-spawn apelează `bindNpcToHomePlace` pentru fiecare rezident, iar această operație suprascrie `ownerNpcId`. `primaryOwnerNpcKey` nu conduce această atribuire. Proba cu Gigel urmat de Maria a păstrat ambii rezidenți, dar proprietarul a devenit Maria.

Surse: [AINPCCommand.kt:4389](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt#L4389), [WorldAdminService.kt:309](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/WorldAdminService.kt#L309).

**A13. Capacitatea locuinței numără blocuri de pat, nu paturi distincte.** Scanarea clasifică materialul fiecărui bloc, fără deduplicarea jumătăților unui pat. Mapper-ul folosește numărul de features pentru `max_residents`. Proba sesiunii cu două blocuri reprezentând un pat a produs două features, două ancore `bed` și capacitate 2. Lipsa separată a nodurilor `npc_spawn` poate opri ulterior alocarea; capacitatea greșită nu înseamnă că sunt deja create automat două NPC-uri.

Surse: [VanillaVillageScanner.kt:146](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/scan/VanillaVillageScanner.kt#L146), [SemanticVillageMapper.kt:203](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/scan/SemanticVillageMapper.kt#L203).

**A14. Delimitarea caselor este o euristică, nu recunoașterea clădirilor.** Gruparea paturilor folosește distanța X/Z, fără diferența de înălțime și fără pereți sau încăperi. Paturi la Y=64 și Y=78, cu aceleași X/Z, au produs o singură casă. Acest lucru poate fi corect pentru o casă cu etaje, dar poate reuni greșit locuințe independente; scannerul nu are informația necesară pentru a decide.

Sursă: [SemanticVillageMapper.kt:553](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/scan/SemanticVillageMapper.kt#L553).

**A15. Atelierul dintr-o locuință nu este repartizat automat ca workplace.** Mapper-ul exclude workstation-urile din limitele caselor când creează clădiri de muncă separate; planner-ul exclude casa curentă și caută places eligibile, nu toate nodurile de lucru. În probă, un furnal în casă a rămas nod mapat, fără `workPlaceId` pentru rezident. Un furnal separat a produs `forge_1`, dar ocupația a rămas `Locuitor`, deoarece mapping-ul scanat nu setează metadata `profession`/`occupation` utilizată de planner.

Surse: [SemanticVillageMapper.kt:316](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/scan/SemanticVillageMapper.kt#L316), [HouseAllocationPlanner.kt:61](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseAllocationPlanner.kt#L61), [HouseAllocationPlanner.kt:272](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/spawn/HouseAllocationPlanner.kt#L272).

**A16. Denumirile meseriilor nu corespund ID-urilor profilurilor.** `Fierar`, `Fermier` și `Negustor` nu se potrivesc cu `smith`, `farmer`, `merchant`. Lookup-ul ignoră majusculele, fără traducere sau alias. Proba cu YAML-ul curent a selectat profilul implicit pentru denumirile românești și profilurile specializate pentru ID-urile englezești.

Sursă: [BehaviorProfileLoader.kt:70](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/BehaviorProfileLoader.kt#L70).

**A17. Orarul are prioritate necondiționată față de regulile de rezervă.** Resolverul returnează intervalul programat înainte să evalueze nevoile sau lipsa ancorei. La tick-ul 9000, cu energie, hrană și siguranță zero, proba a returnat `WORKING`; fără ancora de muncă a returnat `WORK` cu țintă nulă. Pentru profilul implicit, resolverul nu a produs niciun tick `SOCIAL` pe intervalul 0–23999. Alte mecanisme pot interveni; constatarea privește acest resolver și profilul analizat, nu imposibilitatea oricărei socializări în plugin.

Sursă: [RoutineDecisionResolver.kt:21](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineDecisionResolver.kt#L21).

### Concluzie și prioritate

Sunt necesare contracte comune între clădirea fizică, mapping, gospodărie, proprietar, meserie și rutină. Înainte de generarea automată la startup trebuie protejate datele existente, eliminate coliziunile de identitate și făcut explicit succesul parțial. Activarea globală a generării sau rutinei înaintea acestor verificări ar amplifica problemele.

Continuarea recomandată: [planul de remediere în 10 pași](../planning/plan-remediere-generare-sat-si-rutine.md). Primul lot funcțional este repararea identității și persistenței gospodăriilor, după backup și teste de regresie.

## Progresul remedierilor — 2026-10-02

A07 are o protecție preventivă implementată local: `HouseholdPersistenceServiceState.saveHouseholdRow` refuză reutilizarea unui ID legat deja de altă casă, înaintea ștergerilor sau upsert-ului. Testele verifică păstrarea exactă a gospodăriilor și rezidenților, inclusiv când casa țintă are deja propria gospodărie. Actualizarea aceleiași case și asocierea inițială a unei înregistrări legacy fără casă rămân permise.

Această protecție A07 nu migrează ID-uri și nu repară planner-ul. Nu este o tranzacție completă și nu pretinde rezolvarea scrierilor concurente. Probe de regresie: trei eșecuri înaintea fixului și toate cele 11 teste de persistență trecute după fix, cu SQLite în memorie.

A08 are acum o remediere locală separată: eșecul SQL al persistenței sau numărul incomplet de rezidenți produce un `HouseholdSpawnResult` eșuat, marcat explicit `persistenceFailed`. NPC-urile create/reutilizate și diagnosticul sunt păstrate. Settlement-ul se oprește fără rollback global și fără ștergerea compensatorie a gospodăriilor pentru acest eșec; astfel, refuzul unei coliziuni A07 nu declanșează ștergerea gospodăriei originale. Tracker-ul primește `FAILED`, fără a pretinde că poate salva istoricul când însăși baza este indisponibilă.

Validare A08: 13 teste noi și 154 de teste relevante trecute în total, inclusiv eroare SQL injectată după scrierea primului rezident și verificarea stării rămase. Nu s-a introdus atomicitate, recuperare automată sau o garanție nouă pentru celelalte tipuri de rollback. Nu s-au făcut deploy ori probe într-o lume Paper. Detaliile backup-urilor, politicii de recuperare și lucrărilor rămase sunt în [progresul planului](../planning/plan-remediere-generare-sat-si-rutine.md#progres--2026-10-02).

A12 are o remediere locală: binding-ul gospodăriei separă rezidența de proprietate și folosește `primaryOwnerNpcKey` pentru a selecta NPC-ul persistent, indiferent de ordinea locuitorilor. Proprietatea este atribuită după reușita binding-urilor rezidenților; un conflict cu proprietarul existent este raportat drept binding parțial, fără transfer automat. Apelul general `bindNpcToHomePlace` păstrează atribuirea inițială pentru casa fără proprietar, dar nu mai lasă al doilea rezident să suprascrie proprietarul sau numele acestuia. Cache-ul de places este invalidat după aceste modificări.

Validare A12: trei regresii reproduse înaintea fixului, 21 de teste noi și 250 de teste relevante trecute în total. Sunt verificate ordinea inversată, NPC-ul reutilizat, rerularea și salvarea/reîncărcarea YAML; acestea nu reprezintă un test complet pe server Paper. Proprietarii greșiți deja salvați și identificatorii legacy ambigui nu sunt migrați automat. Contractul actual este descris în [NPC World Bindings](../architecture/npc-world-bindings.md#rezidență-și-proprietate), iar lucrările rămase sunt în plan.
