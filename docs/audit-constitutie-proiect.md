# Audit Conformitate cu Constitutia Proiectului

Actualizat: 2026-05-29

Importanta: ridicata. Acest document compara codul curent cu `constitutie-proiect.md` si listeaza zonele care nu respecta complet regulile constitutionale sau care sunt doar partial conforme.

## Scop

Auditul verifica in special:

- core neutru si compatibil cu orice addon valid;
- continut demo/fallback dezactivabil;
- tipuri de addon: scenariu, story, resursa/textura, datapack;
- feature flags pentru caracteristici majore;
- rezolvare centralizata pentru starea finala a feature-urilor, configului si addonurilor;
- storage SQLite/MySQL/HikariCP;
- AI ca sistem asistiv, nu autoritate finala;
- generare harti/structuri ca plan validat, nu executie directa necontrolata.

Acesta este un audit static peste fisierele sursa si resurse. Nu inlocuieste testele, smoke testul pe Paper sau auditul runtime.

## Rezumat executiv

Proiectul respecta partial constitutia: exista config pentru multe sisteme (`simulation.enabled`, `routine.enabled`, `world_admin.enabled`, `ai.orchestration.enabled`, `addons.disabled`), addonul medieval separat are config propriu si exista validare de metadata pentru feature packs.

Neconformitatile principale sunt:

1. Core-ul nu mai livreaza pack-uri tematice in resursele proprii; `medieval` si `social` au fost mutate in addonul medieval, iar `modern` a fost arhivat ca demo istoric.
2. Core-ul nu mai are fallback medieval hardcodat; fallback-ul intern a fost inlocuit cu `core_minimal`.
3. `demo.enabled` controleaza quest fallback-ul simplu, comanda `/ainpc world demo create` si ignorarea pack-urilor demo root ramase din instalari vechi.
4. API-ul de addon include acum tipurile constitutionale `STORY`, `RESOURCE`, `TEXTURE`, `DATAPACK`, cu aliases validate.
5. Addonul medieval separat este inregistrat acum ca `SCENARIO`.
6. `database.type` este respectat initial: SQLite ramane default, iar MySQL foloseste HikariCP configurabil. Portarea SQL completa ramane partiala.
7. Exista `features.*` si gating initial pentru comenzile majore, dar serviciile interne sunt inca initializate chiar cand feature-ul este dezactivat.
8. Euristicile tematice identificate in serviciile core analizate au fost neutralizate initial; in `src/main` mai raman doar denumiri tehnice/de loc precum `fierarie/forge`, nu profesii tematice hardcodate.
9. Regula constitutionala noua pentru `RuntimeFeatureState` este partial pornita: modelul read-only si resolverul minim exista, dar codul inca foloseste citiri directe din `plugin.config`; nu exista inca audit/debugdump dedicat, migrare call sites sau graph complet pentru conflicte.

## Conformitati Confirmate

| Regula constitutionala | Stare in cod | Dovezi |
|---|---|---|
| Addon registry dezactivabil | Conform partial | `ainpc-core-plugin/src/main/resources/config.yml` are `addons.enabled`, `addons.disabled`, `addons.strict_validation`; `AddonRegistry.isAddonEnabled` respinge addonuri dezactivate |
| Feature packs cu metadata | Conform partial | `FeaturePackMetadataValidator` valideaza `addon.type`, `runtime_modes`, `capabilities`, `dependencies` |
| Addon medieval separat | Conform partial | `ainpc-scenario-medieval` are plugin separat, config template si pack instalat in `packs/addons/ainpc-scenario-medieval` |
| Tipuri addon constitutionale | Conform initial | `AddonType` include `STORY`, `RESOURCE`, `TEXTURE`, `DATAPACK`; validatorul accepta aliases ca `resource_texture` si `data-pack` |
| Fallback intern neutru | Conform initial | `FeaturePackDefaults.loadNeutralFallbackPack` foloseste `core_minimal`, profesii generice si topologii neutre |
| AI orchestration dezactivata implicit | Conform | `config.yml` are `ai.orchestration.enabled: false` |
| Rutina si simulare dezactivabile | Conform partial | `config.yml` are `simulation.enabled` si `routine.enabled`; schedulerul verifica aceste flag-uri |
| World admin dezactivabil | Conform partial | `config.yml` are `world_admin.enabled`; debugdump raporteaza disabled cand service-ul lipseste/dezactivat |
| Generare/patch planner controlat | Conform partial | patch planner si mapping sunt orientate pe analyze/plan/validate; nu apare executie directa larga in auditul static |
| Config/addon metadata initiala | Conform partial | `AddonDescriptor` declara type/capabilities/dependencies; `FeaturePackMetadataValidator` valideaza addon type, runtime modes, capabilities si dependencies; `FeaturePackDependencyValidator` elimina pack-uri cu dependinte lipsa |

## Neconformitati si Riscuri

### C-001: Pack-uri tematice in core

Severitate: remediata initial
Status: remediat initial
Reguli afectate: Core neutru; continut demo dezactivabil; addonurile ofera tema si poveste.

Dovezi:

- `ainpc-core-plugin/src/main/resources/packs/` nu mai contine pack-uri tematice.
- `ainpc-scenario-medieval/src/main/resources/packs/medieval.yml` si `social.yml` sunt livrate de addonul medieval.
- `docs/arhiva/core-demo-packs/modern.yml` pastreaza pack-ul modern ca artefact istoric, nu ca resursa runtime core.
- `FeaturePackLoader.saveDefaultPacks()` nu mai instaleaza pack-uri tematice implicite din core.
- `FeaturePackLoader.isDisabledCoreDemoPack(...)` ignora pack-urile root `medieval`, `modern`, `social` cand `demo.enabled=false`, inclusiv daca au ramas copiate dintr-o instalare veche.

Problema:

Constitutia spune ca `core` trebuie sa ofere infrastructura, iar addonurile continutul. Mutarea fizica a pack-urilor tematice scoate continutul de scenariu din JAR-ul core. Riscul ramas este operational: serverele existente pot avea inca fisiere vechi in `plugins/AINPC/packs`, dar acestea sunt ignorate cand `demo.enabled=false`.

Recomandare:

- Pastreaza continutul tematic exclusiv in addonuri sau arhive docs, nu in resursele runtime core.
- Adauga in viitor un addon demo dedicat pentru `modern` daca acel scenariu trebuie reactivat.
- Adauga avertizare vizibila in audit/runtime pentru fisiere demo vechi aflate in root `packs/`.

### C-002: Fallback medieval hardcodat in cod

Severitate: remediata initial
Status: remediat initial
Reguli afectate: Core neutru; demo/fallback dezactivabil.

Dovezi:

- `FeaturePackLoader.loadAllPacks()` apeleaza `FeaturePackDefaults.loadNeutralFallbackPack(...)` cand nu exista pack-uri si `feature_packs.allow_builtin_fallbacks=true`.
- `FeaturePackDefaults.loadNeutralFallbackPack` creeaza `FeaturePack("core_minimal", "Core Minimal", ...)` cu profesii generice ca `worker`, `caretaker`, `guide`.

Problema:

Fallback-ul medieval compilat a fost inlocuit. Ramane de verificat in smoke test ca serverul fara pack-uri porneste cu `core_minimal` si ca experienta ramane utila.

Recomandare:

- Test dedicat pentru `FeaturePackDefaults.loadNeutralFallbackPack` exista initial.
- Pastreaza orice continut tematic in addonuri, nu in fallback-ul compilat.

### C-003: Switch unic pentru oprirea demo-ului core

Severitate: remediata initial
Status: remediat initial
Reguli afectate: Dezactivare completa; continut demo dezactivabil cand se activeaza alt addon.

Dovezi:

- `quests.yml` are `simple_for_all_npcs: true`, dar `ScenarioEngine.shouldUseSimpleQuestForAllNpcs()` returneaza false cand `demo.enabled=false`.
- `FeaturePackLoader.saveDefaultPacks()` nu mai instaleaza continut tematic din core.
- `AINPCPlugin.loadQuestConfig()` salveaza `quests.yml` daca lipseste.
- `/ainpc world demo create` este blocat explicit cand `demo.enabled=false`.

Problema:

Regula operationala "core demo off" exista initial pentru quest fallback, comanda de mapping demo si pack-uri demo root ramase din instalari vechi.

Recomandare:

- Adauga audit/avertizare vizibil in `/ainpc audit` pentru pack-uri demo deja existente in folderul de date cand `demo.enabled=false`.
- Cand un addon `SCENARIO` activ este primary, core demo ar trebui sa se opreasca automat sau sa afiseze avertizare explicita.

### C-004: Tipurile constitutionale de addon lipsesc din API

Severitate: remediata initial
Status: remediat initial
Reguli afectate: tipuri addon scenariu, story, resursa/textura, datapack.

Dovezi:

- `AddonType` contine `CORE`, `SCENARIO`, `STORY`, `RESOURCE`, `TEXTURE`, `DATAPACK`, `FEATURE`, `INTEGRATION`.
- `FeaturePackMetadataValidator.isKnownAddonType` accepta aliases ca `resource_texture`, `resource-pack`, `texture-pack`, `data-pack`.

Problema:

Tipurile exista initial. Ramane de definit comportamentul complet pentru fiecare tip: capabilitati minime, lifecycle specializat, loading de resurse/texturi si reguli datapack.

Recomandare:

- Actualizare `AddonRegistry` ca fiecare tip sa aiba capabilitati minime validate.
- Documentare contract API pentru fiecare tip.

### C-005: Addonul medieval separat este declarat ca `FEATURE`, nu `SCENARIO`

Severitate: remediata initial
Status: remediat initial
Reguli afectate: addonurile trebuie sa declare tipul si capabilitatile corect.

Dovezi:

- `MedievalScenarioAddon` foloseste `AddonType.SCENARIO`.
- `medieval_quest.yml` declara `addon.type: "scenario"` si `primary_scenario: true`.
- Pluginul se numeste `AINPCScenarioMedieval`, iar descrierea spune ca livreaza scenariul medieval.

Problema:

Descriptorul pluginului si pack-ul sunt aliniate ca scenariu. Testele de registry/metadata si testele addonului medieval trec.

Recomandare:

- Test dedicat pentru descriptorul `MedievalScenarioAddon` exista initial.
- Verifica in smoke Paper ca primary scenario se raporteaza corect cand addonul este activ.

### C-006: `database.type` si MySQL/HikariCP

Severitate: partial remediata initial
Status: partial remediat initial
Reguli afectate: directia de productie include MySQL cu HikariCP; configurabilitate explicita.

Dovezi:

- `config.yml` are `database.type: "sqlite"`, `database.sqlite.filename` si sectiune `database.mysql` cu host, port, database, username, `password_env` si setari de pool.
- `DatabaseManager.initialize()` citeste `database.type` si alege explicit intre SQLite si MySQL.
- SQLite ramane backend-ul implicit si foloseste fisier local prin `database.sqlite.filename` sau cheia veche `database.filename`.
- MySQL incarca `com.mysql.cj.jdbc.Driver`, construieste `HikariDataSource` si foloseste setarile `database.mysql.pool.*`.
- `ainpc-core-plugin/build.gradle` include acum `sqlite-jdbc`, `HikariCP` si `mysql-connector-j`.

Problema:

Providerul de conexiune este prezent, dar portarea SQL completa nu este finalizata. In codul runtime mai exista query-uri cu sintaxa SQLite (`ON CONFLICT ... DO UPDATE`) care trebuie transformate intr-un dialect comun sau in SQL specific providerului. Din acest motiv, MySQL este suport initial/infrastructural, nu productie completa.

Recomandare:

- Extrage repository-urile la un strat de dialect (`SqlDialect`/`StorageProvider`) pentru upsert-uri, indexuri partiale si migration.
- Adauga smoke test cu MySQL real sau containerizat inainte de a marca `database.type: mysql` ca production-ready.
- Pastreaza SQLite ca default local si evita ca dependenta MySQL sa devina obligatorie operational.

### C-007: Feature flags globale pentru caracteristici majore

Severitate: medie
Status: partial remediat, runtime gating extins
Reguli afectate: orice caracteristica majora trebuie sa poata fi dezactivata.

Dovezi:

- Configul are flags pentru `simulation`, `routine`, `world_admin`, `addons`, `ai.orchestration`.
- Configul are acum `features.gui`, `features.quest`, `features.progression`, `features.story`, `features.mapping`, `features.routine`, `features.simulation`, `features.ai`, `features.generation`.
- `AINPCCommand` blocheaza comenzile publice pentru `gui`, `quest`, `progression`, `story`, `world/patch/wand/map` si `routine` cand flag-ul aferent este false.
- `AINPCCommand` cere acum `features.generation=true` pentru actiuni cu efect de generare/spawn: `/ainpc create`, `/ainpc world demo create`, `/ainpc world household spawn` si `/ainpc world settlement spawn`. Variantele read-only/plan raman disponibile sub `features.mapping`.
- `AINPCTabCompleter` ascunde actiunile `create`, `spawn` si `world demo create` cand `features.generation=false`, dar pastreaza `plan` pentru fluxuri read-only.
- `SchedulerCoordinator` nu mai porneste quest tracking, routine tick sau simulation tick cand flag-urile `features.quest`, `features.routine`, `features.simulation` sunt false.
- `QuestObjectiveListener`, `NPCInteractionListener` si `NPCChatListener` nu mai trimit evenimente in `ScenarioEngine` cand `features.quest=false`.
- `MappingWandListener` ignora wand-ul cand `features.mapping=false`.
- `GuiInventoryListener` inchide/ignora GUI-urile AINPC cand `features.gui=false`.
- `AINPCPlugin.onEnable()` initializeaza `scenarioEngine`, `progressionService`, `storyStateService`, `storyContextService`, `guiService`, `mappingWandService` neconditionat.
- `docs/feature-flags-lifecycle.md` defineste regula obligatorie pentru no-op/read-only services inainte de oprirea initializarii reale.

Problema:

Comenzile, listenerele si taskurile majore sunt acum oprite operational prin config, dar serviciile interne sunt inca initializate. Acesta este un pas util runtime, nu o dezactivare completa la nivel de lifecycle/constructie servicii.

Recomandare:

- Leaga initializarea serviciilor la `features.*` sau introdu mod disabled/read-only pentru fiecare serviciu.
- Pastreaza gating-ul din listenere/taskuri ca protectie secundara chiar daca serviciile devin conditionale.
- Testul pentru formatterul mesajului feature disabled exista initial; extinde ulterior cu harness Bukkit pentru rutele publice complete.
- Nu lasa servicii `lateinit` neinitializate cat timp exista getters Java/Kotlin care pot fi apelate; foloseste intai no-op/read-only.

### C-008: Configul default activeaza sisteme automate cu impact in lume

Severitate: remediata initial
Status: remediat initial
Reguli afectate: date validate inainte de automatizare; playability inainte de complexitate.

Dovezi:

- `demo.enabled: false`.
- `features.routine: false`, `features.simulation: false`.
- `simulation.enabled: false`, `routine.enabled: false`.
- `dialog.passive_listen_enabled: false`.
- `family.auto_generate: false`.
- `villagers.auto_repopulate.enabled: false`.
- Codul foloseste fallback conservator (`false`) pentru `family.auto_generate`, `villagers.auto_repopulate.enabled`, `dialog.passive_listen_enabled`, `routine.enabled`, `features.routine` si `features.simulation` cand cheia lipseste.
- `ConfigDefaultsTest` verifica aceste default-uri.

Problema:

Default-urile automate cu impact in lume au fost schimbate la opt-in. Ramane ca profilurile explicite `demo | production | minimal` sa fie formalizate daca proiectul vrea moduri de instalare predefinite.

Recomandare:

- Profiluri explicite: `profile: demo | production | minimal`.
- In `minimal` si `production`, sistemele automate raman off pana la configurare.
- In `demo`, ele pot fi activate explicit sub `demo.enabled` sau printr-un preset de addon demo.

### C-010: Lipseste resolverul central pentru `RuntimeFeatureState`

Severitate: medie-ridicata
Status: partial pornit, resolver minim implementat
Reguli afectate: rezolvarea centralizata a feature-urilor, configului si addonurilor; decizie determinista; conflicte nerezolvate blocate.

Dovezi:

- `docs/constitutie-proiect.md` cere ca deciziile finale despre feature-uri sa fie rezolvate central si expuse ca `RuntimeFeatureState`.
- Exista model read-only initial in `ro.ainpc.platform.features`: `RuntimeFeatureKey`, `RuntimeFeatureStatus`, `RuntimeFeatureSource` si `RuntimeFeatureState`.
- Exista `RuntimeFeatureResolver` si `RuntimeFeatureSnapshot` initiale, legate read-only in `AINPCPlatform.runtimeFeatures()`.
- Nu exista inca `ConfigResolver` complet sau `DependencyGraph` in `ainpc-core-plugin/src/main` ori `ainpc-api/src/main`.
- `AINPCCommand`, `SchedulerCoordinator`, `OpenAIService`, listenerele de quest/mapping/gui si alte servicii citesc direct chei precum `features.quest`, `features.mapping`, `features.ai`, `routine.enabled`, `simulation.enabled` din `plugin.config`.
- `AINPCPlatform.reloadFromConfig()` configureaza `AddonRegistry` cu `addons.enabled`, `addons.disabled` si `addons.load_order` si produce un snapshot read-only, dar comenzile, schedulerul si listenerele nu il consuma inca.
- `AddonDescriptor` expune `capabilities` si `dependencies`, iar `FeaturePackMetadataValidator`/`FeaturePackDependencyValidator` valideaza partial metadata, dar nu exista inca declaratii uniforme de `provides`, `requires`, `conflicts`, `fallback` sau `requested feature state`.
- `AddonRegistry.registrationErrors(...)` verifica runtime mode, capability duplicata, capability `scenarios` pentru `SCENARIO`, dependinte goale/duplicate/self/disabled, dar nu verifica dependinte lipsa intre plugin addonuri active si nu detecteaza conflicte intre addonuri.

Problema:

Constitutia cere ca runtime-ul sa consume o decizie finala unica. Codul curent este inca in model mixt: unele decizii sunt validate la load, altele sunt citite local din config, iar addonurile/pack-urile au metadata partiala. Asta este acceptabil pentru faza curenta, dar nu este suficient pentru ecosistem mare de addonuri, primary scenario multiplu, conflicte de economie/story/quest sau preseturi de server.

Riscul practic este aparitia unor configuratii care par valide individual, dar produc stare ambigua: un addon cere questuri, serverul le dezactiveaza, alt pack declara progresii dependente de story, iar runtime-ul afla problema abia cand o comanda sau un listener atinge zona respectiva.

Recomandare:

- Pastreaza modelul minim `RuntimeFeatureState` cu stari explicite: `enabled`, `disabled`, `optional`, `blocked`, `fallback`, `experimental`.
- Extinde `RuntimeFeatureResolver` din strat read-only spre rezolvare reala pentru `core defaults`, `server profile`, `features.*`, `demo.enabled`, `addons.*`, manifestele addonurilor si metadata pack-urilor.
- Nu opri direct citirile existente din config intr-o singura schimbare mare; adauga intai comanda/audit de inspectie peste resolverul read-only.
- Extinde manifestele addon/pack cu campuri declarative mici: `provides`, `requires`, `conflicts`, `optional`, `fallback`.
- Adauga audit runtime: pentru fiecare feature major, raporteaza starea finala, sursele care au influentat-o si motivul daca este blocat.
- Pastreaza politica serverului ca autoritate finala: addonurile pot cere activare, dar nu pot forta peste server profile.
- Dupa ce resolverul este stabil, muta gradual `ensureFeatureEnabled`, schedulerul, listenerele si serviciile de AI/routine/simulation pe `RuntimeFeatureState`.

### C-009: Euristici tematice ramase in servicii core

Severitate: remediata initial
Status: remediat initial in `src/main`
Reguli afectate: core neutru; core compatibil cu orice addon; demo dezactivabil.

Dovezi:

- `ScenarioEngine` nu mai contine profiluri simple hardcodate pentru `blacksmith`, `farmer`, `guard`; profilul simplu foloseste default generic si `profession_fallbacks` configurabil.
- `ScenarioEngine` nu mai foloseste ocupatii hardcodate `guard/soldier/merchant` pentru scorul de roluri interne; rolul optional din scenariul de furt este `RESPONDER`, nu `GUARD`.
- `quests.yml` nu mai livreaza `profession_fallbacks` tematice in core; exista test dedicat `QuestFallbackConfigTest`.
- `WorldAdminService.createDemoVillage(...)` nu mai seteaza tag/metadata de profesie pentru `blacksmith`, `farmer` sau `priest`; locurile demo raman tehnice (`FORGE`, `FARM`, `CUSTOM`) si neutre la nivel de profesie.
- `MappingIntentParser` pastreaza recunoasterea tehnica `forge`, dar nu mai seteaza tag/metadata `blacksmith`.
- `VillageGapAnalyzer` nu mai presupune ca tipurile tehnice `FORGE/FARM/MARKET/TAVERN` satisfac profesii tematice; cere tag/metadata explicita din config/addon.
- `DialogueEngine` nu mai produce placeholder-e profesionale tematice; foloseste valori neutre pentru fallback.
- `RoutineEngine` nu mai deduce stari specializate din nume de profesie; munca generica foloseste `NPCState.WORKING`.
- `AINPC` nu mai mapeaza ocupatii addon la profesii vizuale Minecraft; profesia vizuala trebuie ceruta explicit cu prefix `minecraft:` sau `villager:`.
- `HouseAllocationPlanner` nu mai deduce ocupatii tematice din tipuri de loc fara metadata explicita; fallback-ul este `worker`.
- `FamilyManager` nu mai genereaza automat ocupatii tematice pentru familie; fallback-ul foloseste `worker/caretaker/guide/resident`.
- `NPCManager` nu mai infereaza ocupatii tematice din blocuri sau tipuri de loc, nu mai traduce profesii Villager in ocupatii romanesti si nu mai mapeaza ocupatii tematice la personalitati.
- `CoreNeutralityStaticAuditTest` blocheaza reintroducerea termenilor de profesii tematice in `ainpc-core-plugin/src/main`.
- `DialogueEngine` nu mai spune ca serverul este medieval in promptul AI; foloseste o formulare neutra despre server Minecraft configurabil prin addonuri.
- `DemoReadinessCommand` nu mai cere explicit addonul medieval; textele cer generic addonul de scenariu.
- In `src/main` mai apar doar termeni de loc/ID tehnic ca `fierarie`, `forja`, `forge` si lista legacy `CORE_DEMO_PACK_IDS` folosita pentru a ignora pack-uri demo vechi cand demo-ul este off.
- `AINPCTabCompleter` a fost partial remediat: `/ainpc create` si `/ainpc patch ...` citesc profesii din feature packs, cu fallback neutru `worker/caretaker/guide`.

Problema:

Euristicile tematice identificate initial in serviciile core au fost neutralizate in codul runtime principal. Riscul ramas este de mentenanta: test fixtures, texte demo istorice si viitoare functionalitati pot reintroduce vocabular de scenariu in core daca nu folosesc pack-uri/addonuri/config.

Recomandare:

- Pastreaza vocabularul/profesiile/sugestiile tematice in capability-uri sau pack-uri addon.
- Pastreaza in core doar mapari tehnice Minecraft strict necesare, cu nume neutre si configurabile.
- Pastreaza regula de test/audit static pentru `src/main` care blocheaza reintroducerea termenilor de profesii tematice.
- Auditeaza separat test fixtures si texte demo istorice, fara a le trata automat ca runtime core.

## Zone Care Par Conforme sau Acceptabile

### AI

AI-ul este in mare parte conform:

- `features.ai` este false implicit.
- `ai.orchestration.enabled` este false implicit.
- `OpenAIService` nu ruleaza diagnostice, probe sau requesturi catre provider cand `features.ai=false`; raspunsurile folosesc fallback local/neutru.
- `AIOrchestrationService.enabled()` cere atat `features.ai=true`, cat si `ai.orchestration.enabled=true`.
- `AIOrchestrationPolicy` pare sa marcheze cazuri ca draft/validation.
- `OpenAIService` are fallback local si diagnostice configurabile.

Riscul ramas este lifecycle: `OpenAIService` se initializeaza inca pentru compatibilitate API, dar nu mai executa efecte externe cand `features.ai=false`.

### Generare harti si structuri

Auditul static nu a gasit executie larga necontrolata de world generation. Zonele de `patch`, `scan`, `mapping` par orientate pe analiza, draft si plan. Ramane necesar un audit separat cand se introduce generare reala de structuri.

### Addon medieval separat

Addonul medieval separat este o directie buna:

- are config propriu;
- poate fi dezactivat prin `addons.disabled` sau `addon.enabled`;
- instaleaza pack-ul in folder gestionat;
- curata pack-ul gestionat la disable.

Continutul medieval principal este acum in addonul medieval, nu in resursele runtime core.

## Plan Recomandat de Remediere

### Faza 1: Neutralizare core demo

1. Adauga `demo.enabled` si `feature_packs.install_defaults`. Status: facut initial.
2. Opreste `saveDefaultPacks()` cand demo/default packs sunt disabled si blocheaza quest fallback/world demo create cand `demo.enabled=false`. Status: facut initial.
3. Mutare `medieval.yml`, `modern.yml`, `social.yml` in addonuri sau arhiva demo. Status: `medieval` si `social` mutate in addonul medieval; `modern` arhivat in `docs/arhiva/core-demo-packs/modern.yml`.
4. Inlocuire fallback medieval din `FeaturePackDefaults` cu fallback generic neutru. Status: facut initial.
5. Extrage euristicile tematice ramase din servicii core in addonuri sau config. Status: completari comenzi, simple quest fallback, roluri interne, mapping parser, demo village, patch analyzer, dialog, routine, villager profession mapping, house allocation, family generation si `NPCManager` facute initial. Ramane de facut un audit separat pentru test fixtures si texte demo istorice.

### Faza 2: API addon constitutional

1. Extinde `AddonType` cu `STORY`, `RESOURCE`, `TEXTURE`, `DATAPACK`. Status: facut initial.
2. Actualizeaza validatorul si registry-ul pentru capabilitati minime per tip. Status: validator aliases facut; capabilitati minime ramase.
3. Schimba `MedievalScenarioAddon` la `AddonType.SCENARIO`. Status: facut initial.
4. Adauga teste pentru tipuri, aliases, dezactivare si selectie primary scenario. Status: teste aliases/metadata, fallback neutru si descriptor addon medieval facute.

### Faza 3: Feature flags globale

1. Introduce `features.*` central. Status: facut initial.
2. Leaga `quest`, `story`, `gui`, `progression`, `mapping`, `generation`, `ai` la initializare/comenzi/GUI. Status: comenzi majore facute initial; lifecycle/listenere ramase.
3. Cand o functie este disabled, comenzile trebuie sa raspunda clar, nu sa cada partial. Status: facut initial pentru comenzile majore.
4. Introdu `RuntimeFeatureState` si un resolver read-only pentru config/addon/pack metadata. Status: model si resolver minim facute initial; audit/debugdump ramas.
5. Muta gradual comenzile, schedulerul si listenerele de la citiri directe `plugin.config.getBoolean("features.*")` la starea finala rezolvata. Status: ramas.
6. Adauga raport de audit pentru feature-uri `enabled/disabled/optional/blocked/fallback/experimental`, cu motiv si surse. Status: ramas.

### Faza 4: Storage provider

1. Extrage interfata de conexiune din `DatabaseManager`. Status: partial, selectia de provider exista in manager.
2. Respecta `database.type`. Status: facut initial.
3. Adauga MySQL + HikariCP ca optiune, nu ca dependinta operationala obligatorie. Status: facut initial.
4. Pastreaza SQLite ca default pentru dev/demo. Status: facut initial.
5. Portare dialect SQL pentru `ON CONFLICT`, indexuri partiale si migration. Status: ramas.

## Verdict

Status: partial conform, cu remedieri initiale aplicate pentru C-001, C-002, C-004, C-005, C-008 si C-009, plus remediere infrastructurala initiala pentru C-006.

Codul are directia corecta pentru addonuri si configurabilitate. Pack-urile tematice au fost scoase din resursele runtime core, fallback-ul medieval hardcodat a fost neutralizat, tipurile constitutionale de addon exista initial, addonul medieval separat este declarat ca scenariu, demo-ul core are un switch functional pentru pack-uri vechi/quest fallback/world demo create, comenzile majore au feature flags initiale, default-urile automate sunt conservative, euristicile tematice runtime identificate in core au fost neutralizate initial, iar storage-ul are selectie initiala SQLite/MySQL cu HikariCP.

Cele mai importante remedieri ramase sunt lifecycle complet pentru feature flags, introducerea unui `RuntimeFeatureState` central pentru config/addon/pack resolution si portarea completa a dialectului SQL pentru MySQL.

Pana la legarea tuturor serviciilor interne de `features.*` si apoi de `RuntimeFeatureState`, `ainpc-core-plugin` trebuie tratat ca "core neutru la nivel de continut runtime, dar partial dezactivabil la nivel de lifecycle si partial centralizat la nivel de decizie feature".
