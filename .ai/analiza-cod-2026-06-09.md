# Analiză Cod AINPC — 2026-06-09

## Metadate analiză

- **Model**: `opencode/big-pickle` (asa numit „big-pickle")
- **Data**: 2026-06-09
- **Proiect**: AINPC (AI Non-Player Characters) — plugin PaperMC 1.21
- **Limbaje**: Kotlin 2.3.21 + Java 21
- **Tooling**: Claude Codex + Serena (IDE LSP) + Chroma MCP (ainpc-project-memory)
- **Context**: 7116 vectori Chroma indexați, 9 reguli, 314 changelog, 231 memorii

## Instrucțiuni pentru următorul AI care citește acest document

Acest document NU trebuie tratat ca adevăr absolut. Este o analiză exploratorie
generată de un AI (big-pickle) pe baza citirii codului și a indexării vectoriale.
Scopul este:

1. **Oferi perspective** — unele sugestii pot fi corecte, altele pot ignora context
   pe care autorii codului îl cunosc mai bine.
2. **Stimula discuția** — fiecare punct trebuie verificat de un developer uman
   înainte de a fi implementat.
3. **Documenta observații** — nu prescrie soluții finale.

**Cum să folosești acest document**:
- Verifică fiecare sugestie în cod înainte să acționezi.
- Unele probleme pot fi deja rezolvate în versiuni mai noi ale codului.
- Prioritizează sugestiile cu impact mare și efort mic.
- Ignoră sugestiile care nu se aplică contextului real al proiectului.
- Acest document reflectă starea codului la data de mai sus.

---

## Categoria 1: Arhitectură & God Classes

### 1.1 `ScenarioEngine.java` — 6.512 linii
**Fișier**: `ainpc-core-plugin/src/main/java/ro/ainpc/engine/ScenarioEngine.java`

Un singur fișier Java amestecă: lifecycle quest, persistare SQL, template-uri,
player tracking, reward-uri, snapshot-uri GUI, validare, progresie.
Orice modificare riscă efecte de cascade. Testabilitatea e aproape nulă.

**Sugestie**: Sparge în `QuestLifecycleManager`, `QuestPersistence`,
`QuestTemplateRepository`, `QuestRewardCalculator`, `QuestTracker`.

### 1.2 `NPCManager.java` — 2.566 linii
**Fișier**: `ainpc-core-plugin/src/main/java/ro/ainpc/managers/NPCManager.java`

Același pattern: lifecycle NPC, descoperire villageri, repair duplicate,
persistență, profile management, world binding, village rebalance.

**Sugestie**: Split în `NpcRepository`, `NpcLifecycleService`,
`NpcRepairService`, `NpcProfileService`, `NpcDiscoveryService`.

### 1.3 `WorldAdminService.kt` — ~1.084 linii
**Fișier**: `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/WorldAdminService.kt`

`createDemoSettlement()` are ~170 linii. Clasa amestecă date, validare,
serializare și query logic.

**Sugestie**: Extrage `WorldTopologySerializer`, `WorldTopologyValidator`,
`DemoSettlementGenerator`, `WorldQueryService`.

### 1.4 `AINPCCommand.java` — 6.267 linii
**Fișier**: `ainpc-core-plugin/src/main/java/ro/ainpc/commands/AINPCCommand.java`

Gestionează toate comenzile: quest, world, spawn, story, patch, audit,
debugdump, demo, batch. Mai mare decât ScenarioEngine.

**Sugestie**: Sparge în `QuestCommands.kt`, `WorldCommands.kt`,
`SpawnCommands.kt`, `StoryCommands.kt`, `AdminCommands.kt`.

### 1.5 `DemoReadinessCommand.kt` — 2.344 linii
**Fișier**: `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/DemoReadinessCommand.kt`

Template-uri de răspuns încorporate direct în cod.

**Sugestie**: Extrage template-urile în resurse externe YAML.

### 1.6 `AINPCCommandText.kt` — 3.145 linii
**Fișier**: `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandText.kt`

Formatter masiv de text, top-level functions, apelat static din Java.

**Sugestie**: Fișiere separate pe domenii.

---

## Categoria 2: Dependențe & Cuplaj

### 2.1 Toate serviciile primesc `AINPCPlugin` ca parametru
**Fisiere**: Majoritatea claselor din `ainpc-core-plugin/src/main/kotlin/`

Toate serviciile accesează `plugin.xxxService` — cuplaj circular implicit.
Imposibil de testat unitar fără a mock-ui întreg pluginul.

**Sugestie**: Constructor-based dependency injection cu interfețe explicite.
Nu mai depinde de monolitul `AINPCPlugin`.

### 2.2 `AINPCPlugin.instance` — companion object static
**Fișier**: `ainpc-core-plugin/src/main/kotlin/ro/ainpc/AINPCPlugin.kt:269`

Referință statică împiedică garbage collection. La reload, instanța veche
rămâne în memorie.

**Sugestie**: Elimină `instance`. Folosește `JavaPlugin.getPlugin()`.

### 2.3 `25 lateinit var`-uri în `AINPCPlugin`
**Fișier**: `ainpc-core-plugin/src/main/kotlin/ro/ainpc/AINPCPlugin.kt:41-97`

Stare masivă mutabilă. `reload()` verifică doar unele câmpuri cu
`::xxx.isInitialized`.

**Sugestie**: Înlocuiește cu un `DependencyGraph` sau `ServiceRegistry`
care garantează ordinea de inițializare.

---

## Categoria 3: Duplicare Cod

### 3.1 Helperi identici în spawn classes
**Fisiere**:
- `spawn/HouseAllocationPlanner.kt` (linii 376-430)
- `spawn/HouseAllocationValidator.kt` (linii 434-491)
- `spawn/NpcSpawnOrchestrator.kt` (linii 521-659)

Metode copy-paste: `normalizeToken`, `idMatches`, `matchesAnyToken`,
`nodeMatchesAny`, `isHousePlace`, `isWorkplace`, `requiresWorkAnchor`,
`firstNonBlank`, `distanceSquared`, `findBestNodeForPlace`.

**Sugestie**: Extrage în `SpawnTokenUtils.kt` sau `WorldAdminQueries.kt`.

### 3.2 `normalize()` duplicat în quest engine
**Fisiere**: `engine/QuestDirector.kt`, `engine/QuestTemplateSelector.kt`

Același algoritm: diacritice → lowercase → înlocuiește non-alphanumeric.

### 3.3 `normalizeToken()` în 5+ clase
**Fisiere**: `AddonRegistry.kt`, `NpcSpawnOrchestrator.kt`,
`HouseAllocationPlanner.kt`, `HouseAllocationValidator.kt`,
`ProgressionService.kt`

`.trim().lowercase().replace(' ', '_').replace('-', '_')`

**Sugestie**: O singură extension function `fun String.normalizeToken(): String`
în pachetul `utils`.

---

## Categoria 4: Thread Safety & Concurrency

### 4.1 Lock-ul din `DatabaseManager.prepareStatement()`
**Fișier**: `database/DatabaseManager.kt` (718-783)

`prepareStatement()` face lock cu `ReentrantLock` dar delegă unlock-ul la
`close()` printr-un `Proxy`. Dacă `close()` nu e apelat (excepție între
prepareStatement și `use {}`), lock-ul rămâne luat → deadlock.

**Sugestie**: Folosește `use {}` strict. Elimină proxy-ul.

### 4.2 `getConnection()` re-execută schema la reconnect
**Fișier**: `database/DatabaseManager.kt` (680-689)

La pierderea conexiunii MySQL, `initialize()` rulează din nou toate
`CREATE TABLE IF NOT EXISTS` și migrările.

**Sugestie**: Separă connection management de schema management.

### 4.3 Dynamic Proxy pentru fiecare PreparedStatement
**Fișier**: `database/DatabaseManager.kt` (755-762)

Reflection overhead la fiecare query.

### 4.4 `getConnection()` non-atomic la inițializare
**Fișier**: `database/DatabaseManager.kt` (681-684)

Dacă două thread-uri cheamă `getConnection()` simultan înainte de primul
init, se pot crea două conexiuni.

### 4.5 Fără health-check pentru conexiunea DB
**Fișier**: `database/DatabaseManager.kt` (680-689)

Verifică doar `isClosed`, nu execută un `SELECT 1`.

### 4.6 `AddonRegistry` — `@Synchronized` pe fiecare metodă
**Fișier**: `addons/AddonRegistry.kt`

Toate metodele sunt sincronizate individual. Operațiile de registru sunt
serializate complet, posibil bloc sub sarcină.

**Sugestie**: Folosește `ConcurrentHashMap` și optimisti locking acolo
unde e posibil.

### 4.7 `CompletableFuture` anti-pattern în `DialogManager`
**Fișier**: `ai/DialogManager.kt` (38-112)

Lanț de 4 `CompletableFuture`-uri care amestecă DB thread cu Bukkit API
(necesită main thread). Poate cauza `IllegalStateException`.

**Sugestie**: Adaugă `runTask`/`runTaskAsynchronously` explicite.
Documentează contractul de threading per stage.

---

## Categoria 5: Performance

### 5.1 Scorurile emoțiilor nu sunt normalizate
**Fișier**: `engine/DecisionEngine.kt` (68-78)

`personality.extraversion - 0.5` produce range [-15, +15].
`emotions.happiness * 20` poate fi 0–2000 dacă e pe scară 0–100.
Un singur factor domină decizia.

**Sugestie**: Normalizează toate modifier-ele la aceeași scară.

### 5.2 `DECISION_COOLDOWN = 1000ms` — același pentru toate NPC-urile
**Fișier**: `engine/DecisionEngine.kt:400`

**Sugestie**: Fă-l per-NPC, bazat pe trăsături de personalitate.

### 5.3 Cache-ul de scoruri nu expiră
**Fișier**: `engine/DecisionEngine.kt:22`

`scoreCache` crește nelimitat cu câte NPC-uri sunt încărcate.

**Sugestie**: Caffeine/Guava cache cu expirare temporală.

### 5.4 `config.getBoolean()` citit la fiecare tick
**Fisiere**: `SchedulerCoordinator.kt`, `NPCInteractionListener.kt`

Feature flags citiți din config la fiecare execuție, nu la startup.

**Sugestie**: Cache la startup, reîmprospătat doar la reload.

### 5.5 `allowPublicKeyRetrieval=true` default
**Fișier**: `database/DatabaseManager.kt:90`

Risc de securitate MySQL.

**Sugestie**: Default `false`, activat doar explicit.

---

## Categoria 6: Error Handling

### 6.1 Catch-uri prea largi
**Fisiere**: `DialogueEngine.kt:379`, `NpcSpawnOrchestrator.kt:422`,
`SchedulerCoordinator.kt:339`

Exception generic prins peste tot. Pierzi cauza reală.

**Sugestie**: Prinde doar ce poți trata; lasă restul să se propage.

### 6.2 Loguri cu nivel greșit
**Fisiere**: Multiple

Erori sunt `debug`, diagnostic sunt `info`. Un admin normal nu vede
erorile reale.

**Sugestie**: Erori → `warning`/`severe`, diagnostic → `debug`.

### 6.3 `AddonRegistry.onLoad()` înainte de `registerDescriptorInternal()`
**Fișier**: `AddonRegistry.kt:69-72`

Dacă `addon.onLoad()` aruncă excepție, addonul e jumătate încărcat.

**Sugestie**: Try-catch cu rollback la `onLoad()` eșuat.

### 6.4 `ProgressionService` — fail-lazy la `databaseManager` null
**Fișier**: `ProgressionService.kt:15`

`Repository` se construiește cu `databaseManager ?: throw SQLException`,
dar orice metodă aruncă abia la prima utilizare.

**Sugestie**: Fail-fast la inițializare.

---

## Categoria 7: Config & Operare

### 7.1 Valori magic hardcodate în `DecisionEngine`
**Fișier**: `engine/DecisionEngine.kt` (20-402)

`((personality.extraversion - 0.5) * 30)`, `modifier += 50`, `40`, `1000L`.

**Sugestie**: Externalizează în `config.yml` sub `simulation.scoring`.

### 7.2 Model default `gpt-5.4-nano` în config
**Fișier**: `config.yml:15`

Modelul nu există public (încă). Eroare 404 la prima încercare.

**Sugestie**: Schimbă cu `gpt-4o-mini` sau adaugă fallback auto.

### 7.3 Comentarii în română în config.yml
**Fișier**: `config.yml`

Un admin internațional nu înțelege comentariile.

**Sugestie**: Config-ul în engleză.

### 7.4 11 comenzi separate în plugin.yml
**Fișier**: `plugin.yml`

`/quest`, `/progression`, `/contract`, `/duty`, `/bounty`, `/event`,
`/tutorial`, `/ritual`, `/npcquest`, `/ainpc` — fiecare e o comandă
Paper separată.

**Sugestie**: Unifică sub `/ainpc quest ...`, păstrează doar `/ainpc` + `/quest`.

### 7.5 Aliasuri românești în plugin.yml
**Fișier**: `plugin.yml:33` (`sarcina`, `sarcini`)

Jucătorii non-români nu le folosesc.

**Sugestie**: Elimină sau adaugă variante internaționale.

---

## Categoria 8: Securitate

### 8.1 API key OpenAI logată indirect
**Fișier**: `ai/OpenAIService.kt` (68-73)

`baseUrl` e logată în `logConfigurationDiagnostics()`. Dacă cineva
pune cheia în URL, apare în plain text.

**Sugestie**: Mask-uiește URL-urile la logare.

### 8.2 API key ca `String` în memorie
**Fișier**: `ai/OpenAIService.kt:22`

Poate fi leaked prin debug dump, heap dump, serializare.

**Sugestie**: Folosește `char[]` sau `SecretKey` și șterge după utilizare.

### 8.3 Validare insuficientă input jucător
**Fișier**: `engine/DialogueEngine.kt:220-228`

Mesajul jucătorului ajunge în prompt-ul AI fără sanitizare sau
limitare de lungime.

**Sugestie**: Sanitizează și limitează lungimea înainte de a trimite la API.

### 8.4 `sanitizePathSegment()` nu blochează null bytes
**Fișier**: `platform/AINPCPlatform.kt:104-122`

Path traversal posibil cu caractere de control codate.

---

## Categoria 9: Baze de Date

### 9.1 SQL inline în business logic
**Fisiere**: `ai/DialogManager.kt`, `engine/ScenarioEngine.java`, multiple

SQL brut împrăștiat în toate clasele. Schema changes = căutare manuală.

**Sugestie**: Repository layer cu interfețe.

### 9.2 Fără sistem de migrare a schemei
**Fișier**: `database/DatabaseManager.kt`

Doar `CREATE TABLE IF NOT EXISTS`. Schimbări de schemă necesită
scripturi manuale sau pierdere de date.

**Sugestie**: Flyway sau Liquibase.

### 9.3 MySQL connection string prin interpolare
**Fișier**: `database/DatabaseManager.kt:91-96`

Valori din config inserate direct în string, fără validare.

### 9.4 `SpawnBatchTracker` folosește `databaseManager!!`
**Fișier**: `spawn/SpawnBatchTracker.kt`

Null-assert la fiecare metodă, deși `databaseManager` e nullable.

---

## Categoria 10: Internaționalizare

### 10.1 Template-uri dialog hardcodate în română
**Fișier**: `engine/DialogueEngine.kt:32-180`

Toate string-urile de dialog, fallback-uri, placeholders.

**Sugestie**: Externalizează în fișiere de limbă (YAML/properties).
Suport i18n din prima zi.

### 10.2 Loguri și comentarii în română
**Fisiere**: Majoritatea fișierelor

Mix de cod în engleză cu loguri în română.

**Sugestie**: Standardizează totul în engleză.

---

## Categoria 11: Testare

### 11.1 Zero teste pentru clase critice
`DecisionEngine`, `OpenAIService`, `DialogManager`, `RoutineEngine`,
`WorldAdminService`, `DatabaseManager`, `AddonRegistry`,
`SchedulerCoordinator`.

### 11.2 Testele existente sunt periferice
Teste pentru metadata quest, nu pentru execuția reală cap-coadă
(ofertă → accept → progres → completare → reward).

### 11.3 Nu există teste pentru fallback AI
Când OpenAI e down, `DecisionEngine` și `DialogueEngine` au fallback
determinist. Zero teste care simulează `isAvailable == false`.

### 11.4 Nu există teste pentru rollback spawn
`spawnHousehold()` rollback la eșec. Zero teste care verifică
că NPC-urile create anterior sunt șterse.

### 11.5 `NPCManagerText` — denumire românească
Redenumește în `NPCManagerTest`.

### 11.6 Testele folosesc SQLite real, nu in-memory
Lente și fragile.

---

## Categoria 12: Build & DevOps

### 12.1 `duplicatesStrategy = EXCLUDE`
**Fișier**: `ainpc-core-plugin/build.gradle:16`

Ascunde conflicte reale între dependențe.

**Sugestie**: EXCLUDE doar pentru META-INF cunoscut.

### 12.2 Scripturi PowerShell fără teste
17 script-uri de smoke testing, release, backup — niciun test automat.

### 12.3 Fără `.editorconfig`
Indentare inconsistentă: 4 spații vs tab-uri.

### 12.4 Gradle 9.5.1 — pre-release
**Fișier**: `gradle/wrapper/gradle-wrapper.properties`

Versiune instabilă; risc de compatibilitate.

**Sugestie**: Folosește Gradle 8.x stabil.

### 12.5 `gradle-wrapper.properties` — retries=0
Zero retry la download distribution, deși există `networkTimeout`.

### 12.6 `ainpc-scenario-medieval` — fără dependență Gradle pe core
**Fișier**: `ainpc-scenario-medieval/build.gradle`

Doar `depend: [AINPCPlugin]` în plugin.yml, fără `implementation project()`.

**Sugestie**: Adaugă `implementation project(':ainpc-core-plugin')`.

### 12.7 `ProcessResources` se aplică și la `ainpc-api`
**Fișier**: `build.gradle:59-66`

API-ul n-are plugin.yml, dar task-ul rulează oricum.

### 12.8 `build/` și `target/` coexistă
Ambele directoare de build (Gradle și Maven). Sugerează migrare
incompletă.

### 12.9 `.gitignore` minimal
**Fișier**: `.gitignore`

Lipsesc: `build/`, `target/`, `.gradle/`, `*.log`, `.kotlin/errors/`,
`.idea/`, fișiere OS.

### 12.10 Fișiere generate trackate în git
`.kotlin/errors/`, `.serena/cache/*.pkl`, `.ai/` (64 fișiere generate),
`data/tmp/` (IDE state).

**Sugestie**: Adaugă în `.gitignore`.

---

## Categoria 13: Memory Leaks & Resurse

### 13.1 `reload()` creează `OpenAIService` nou fără a-l închide pe cel vechi
**Fișier**: `AINPCPlugin.kt:227-228`

HTTP client și thread-uri OpenAI se scurg la fiecare `/ainpc reload`.

### 13.2 `reloadContent()` discardă `StoryStateService` fără cleanup
**Fișier**: `AINPCPlugin.kt:240-241`

Conexiuni DB și statement providers se scurg.

### 13.3 Listeners neînregistrați în `onDisable()`
**Fișier**: `AINPCPlugin.kt:214`

`unregisterAll()` unregister doar serviciile, nu și event listeners.

### 13.4 `SchedulerCoordinator` fără graceful shutdown
La `onDisable()`, task-urile `runTaskTimer` rămân active sau sunt
anulate abrupt fără salvare stare.

---

## Categoria 14: Progression Service

### 14.1 `ProgressionService` — wrapper anemic
**Fișier**: `progression/ProgressionService.kt` (397 linii)

Aproape fiecare metodă e `return scenarioEngine().getXxx(...)`.
Nu adaugă abstractizare reală.

**Sugestie**: Fuzionează cu `ScenarioEngine` sau transformă în fațadă
cu comportament propriu.

### 14.2 Stringly-typed selectors
Selectorii de progresie sunt string-uri parseate manual. Fără type safety.

**Sugestie**: Î îmbunătățește `ProgressionSelector.kt` cu sealed class.

---

## Categoria 15: Diverse

### 15.1 Model default inexistent `gpt-5.4-nano`
**Fișier**: `config.yml:15`

### 15.2 Prea multe comenzi Paper (~11)
**Fișier**: `plugin.yml`

### 15.3 Fără Docker health checks
MCP și Chroma n-au `healthcheck` în docker-compose.

### 15.4 `quests.yml` fără validare materiale
Hardcodări ca `OAK_PLANKS`, `EMERALD` fără verificare de versiune.

### 15.5 `data/` conține stare IDE temporară
`data/tmp/jetbrains-state-search/`.

### 15.6 Fără Dockerfile pentru plugin
Doar MCP server e containerizat.

### 15.7 `conversion/README-CONVERSION.md` — documentație învechită
Proces de transformare Java→Kotlin parțial. 15.345 linii Java rămase.

### 15.8 Bus factor: autor unic
`plugin.yml` — `authors: [v0]`. Comentarii și loguri în română.
Proiectul pare dezvoltat de o singură persoană.

**Sugestie**: Documentație externă, code review, onboarding.

---

## Categoria 16: Kotlin Null Safety & API

### 16.1 `WorldAdminApi` — parametri `String?`
Fiecare metodă acceptă null, dar comportamentul e nedefinit per metodă.

### 16.2 `AINPCPlatformApi.getAddonConfigDirectory()` default vs override
Implementarea default diferă de override: una sanitizează cu `ifNullOrBlank`,
cealaltă cu path segment validation. Încălca Liskov substitution.

### 16.3 `DatabaseManager` constructor — `AINPCPlugin?`
Jumătate din clasă folosește `plugin!!`, cealaltă `plugin?.`.

### 16.4 `MemoryManager` — `plugin.databaseManager` non-null assertat
NPE în teste dacă `databaseManager` e null.

### 16.5 `AbstractPluginListener.callSync()` — timeout 2 secunde
Prea scurt pentru servere încărcate. Aruncă `IllegalStateException`
înșelător: "could not execute on main thread" când de fapt e timeout.

---

## Sumar statistic

| Metric | Valoare |
|---|---|
| Fișiere Java > 2.000 linii | 3 (ScenarioEngine 6512, AINPCCommand 6267, NPCManager 2566) |
| Fișiere Kotlin > 1.000 linii | 3 (AINPCCommandText 3145, DemoReadinessCommand 2344, WorldAdminService 1084) |
| Duplicare cod confirmată | ~200 linii helperi spawn + normalize() în 5+ clase |
| Fără teste unitare (clase critice) | ~8 clase |
| SQL inline | Zeci de locații |
| Loguri în română | Majoritatea fișierelor |
| Teste existente | ~34 suite, dar periferice |
| Scripturi PowerShell | 17, fără teste automate |
| Total sugestii | ~60 |

---

*Document generat la 2026-06-09 de modelul `opencode/big-pickle`.*
*Nu trebuie tratat ca adevăr absolut. Verifică fiecare punct în cod înainte de acțiune.*
