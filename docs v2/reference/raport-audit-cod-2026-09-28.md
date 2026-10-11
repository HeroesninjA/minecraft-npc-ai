# Audit tehnic al codului AINPC

Data auditului: 2026-09-28  
Tip: analiză statică, navigare semantică, verificare Git, rulare teste Gradle  
Scop: defecte funcționale, pierderi de stare, integrare incompletă și zone implementate fragil

Auditul nu a modificat codul aplicației. Raportul este separat de rapoartele istorice pentru a păstra contextul fiecărei verificări.

## Notă de interpretare pentru această revizie

Proiectul țintește Paper și este încă în dezvoltare, fără versiune lansată.
Feature-pack-urile sunt administrate de persoane de încredere; observațiile
despre comenzi de consolă sunt, în configurația actuală, riscuri de hardening
pentru etapa de lansare, nu vulnerabilități publice confirmate.

Etichetele P1/P2 din această versiune sunt provizorii și nu reprezintă o politică
fixă de produs. În revizia următoare constatările vor fi separate în documente
pentru defecte confirmate, riscuri statice, securitate și arhitectură. Fiecare
constatare va include explicit tipul dovezii, impactul, condițiile de reproducere,
remedierea și statusul verificării. Deciziile editoriale sunt centralizate în
`docs v2/reference/decizii-clarificare-audit-2026-09-28.md`.

## Rezumat executiv

Cele mai urgente probleme sunt ruta de economie neinițializată, investițiile cu durată nulă/negativă și încărcarea progresului questurilor doar la startup. Acestea pot produce crash la comandă, bani creați instant și progres aparent pierdut după reconectare. Configurația `ai_provider: ollama` nu controlează fluxul real de dialog, iar testele principale nu verifică această integrare.

## Constatări prioritare

### P1 — Comenzile economy balance/pay/set/top folosesc un plugin neinițializat

Dovezi:

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandEconomy.kt:8` declară `lateinit var ainpcCommandEconomyPlugin`.
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandEconomy.kt:10` definește inițializatorul, însă constructorul principal din `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt:126` nu îl apelează.
- Ruta de comandă ajunge direct la aceste funcții în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt:829`.

La prima comandă `/ainpc economy balance`, `/pay`, `/set` sau `/top`, accesul la variabila `lateinit` poate produce `UninitializedPropertyAccessException`. Funcțiile locale pentru `invest`, `collect` și `portfolio` folosesc instanța `plugin`, ceea ce explică de ce doar o parte din subcomenzi este afectată.

Remediere: eliminarea globalului și injectarea pluginului în handler; adăugarea unui test de dispatch pentru fiecare subcomandă.

### P1 — Investițiile cu durată nulă sau negativă se maturează instant

Dovezi:

- Parserul acceptă orice integer în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt:856`.
- `BankingService.invest` verifică doar suma minimă și soldul, nu durata.
- `Investment.matured` compară timpul cu `durationHours * 3600000L`; pentru `0` sau valori negative condiția este imediat adevărată.

Scenariu: un jucător cu cel puțin 100 de monede execută `/ainpc economy invest 100 0`, apoi `collect`; investiția poate returna 102 monede fără așteptare. Aceeași problemă există pentru o durată negativă.

Remediere: respingere explicită pentru `durationHours <= 0`, limite maxime configurabile și test pentru zero/negativ.

### P1 — Progresul questurilor poate fi șters din memorie înainte ca jucătorul să revină

Dovezi:

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestProgressCleanupService.kt:46` elimină progresul activ și arhivat pentru orice jucător offline.
- Curățarea este programată periodic de `ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap/SchedulerCoordinator.kt:73`.
- `ScenarioEngine` încarcă progresul din baza de date la startup în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt:2667`; lookup-ul ulterior citește doar cache-ul în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt:111`.

Dacă un jucător se deconectează mai mult decât intervalul de curățare, progresul este eliminat din cache și nu există încărcare la reconnect înainte de acceptarea/verificarea questului. Reconnect-ul poate afișa progres lipsă sau poate permite recrearea unei intrări care încă există în DB.

Remediere: curățarea trebuie să elimine doar cache-ul auxiliar, nu progresul questului; încărcarea per jucător trebuie făcută la join sau înainte de primul lookup.

### P1 — Reload-ul poate șterge progres pentru un feature pack lipsă sau invalid

Dovezi:

- `FeaturePackLoader.loadAllPacksInternal` golește toate mapările înainte de încărcare în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/FeaturePackLoader.kt:45`.
- Configurația implicită permite continuarea după un pack invalid în `ainpc-core-plugin/src/main/resources/config.yml:163`.
- Reload-ul execută încărcarea pack-urilor și reconstruirea template-urilor în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap/ServiceRegistry.kt:443`.
- `QuestProgressCleanupService` șterge din cache și DB orice template care nu există momentan în lista încărcată la `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestProgressCleanupService.kt:62`.

Scenariu: un YAML este temporar incomplet în timpul editării sau un addon nu este disponibil la reload. Pack-ul este ignorat, template-ul dispare din lista cunoscută, iar următoarea curățare poate șterge progresul jucătorilor pentru acel template. Revenirea fișierului nu restaurează progresul șters.

Remediere: încărcare atomică într-un snapshot nou, păstrarea ultimului conținut valid la eroare și interzicerea ștergerii progresului doar pentru că un template nu este disponibil într-un reload.

### P1 — Providerul Ollama este declarat configurabil, dar dialogul folosește OpenAI

Dovezi:

- Configurația declară providerul în `ainpc-core-plugin/src/main/resources/config.yml:6`.
- Doar `AIOrchestrationService` citește această valoare în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AIOrchestrationService.kt:37`.
- Fluxul real apelează `OpenAIService` în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/DialogueEngine.kt:265` și fallback-ul direct din `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/DialogManager.kt:93`.
- Orchestratorul este doar inițializat/reîncărcat în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap/ServiceRegistry.kt:246`.

Schimbarea `ai_provider` la `ollama` nu mută dialogurile NPC pe Ollama. În plus, rezultatul orchestratorului raportează permanent providerul OpenAI la `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/orchestration/AIOrchestrationService.kt:126`.

Remediere: un singur `AiProvider` injectat în `DialogueEngine`, test de integrare pentru ambele valori și provider raportat dinamic.

### P2 — Întregul lanț de validare AI este neconectat

`AIResponseValidator` este folosit în testele proprii, dar nu are apeluri în codul de producție. Orchestratorul construiește rezultatul după generare fără să aplice validatorul, deși există reguli pentru lungime, format și pattern-uri interzise. Testele verifică obiecte și constante izolate, nu un request real care trece prin provider, validare și fallback.

Remediere: validarea trebuie să fie obligatorie în `AIOrchestrationService`, iar testul trebuie să injecteze un provider fake care returnează răspuns valid, gol și invalid.

### P2 — Istoricul OpenAI poate asocia răspunsul altui jucător

`OpenAIService` păstrează numele curent în câmpuri mutable la `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/OpenAIService.kt:73` și `:106`, apoi le citește în callback-uri asincrone la `:159`. Două cereri simultane pot suprascrie valorile înainte ca prima cerere să termine. Istoricul de debug poate atribui promptul și răspunsul NPC-ului sau jucătorului greșit.

Remediere: capturarea numelor în variabile locale imutabile ale requestului și propagarea unui obiect de context prin `CompletableFuture`.

### P2 — Persistența questurilor folosește SQL SQLite în calea generică

`DatabaseManager.prepareStatement` traduce SQL după dialect în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/database/DatabaseManager.kt:868`. Snapshot-ul de progres ocolește această metodă și folosește direct `conn.prepareStatement` în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestProgressPersistenceService.kt:42`, cu `INSERT OR REPLACE` la `:38`; aceeași formă este folosită pentru ancore la `:175`.

Pe MySQL, această sintaxă nu este echivalentul portabil folosit de translatorul aplicației. Flush-ul periodic sau la shutdown poate eșua, iar excepția este doar logată la `QuestProgressPersistenceService.kt:53`, fără semnal către runtime.

Remediere: folosirea API-ului DB tradus sau a unui upsert per dialect, plus test de integrare pentru SQLite și MySQL/MariaDB.

### P1 — Shop-ul nu are date de producție și butoanele nu cumpără

Dovezi:

- `ShopService` este creat în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap/ServiceRegistry.kt:301`, dar căutarea în sursa de producție nu găsește apeluri `registerShop`.
- GUI-ul doar citește indexul de roluri în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens/ShopGui.kt:81`.
- Ofertele sunt afișate ca butoane în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens/ShopGui.kt:166`, însă `ShopGui` nu are handler de click și nu apelează `executePurchase` sau `executeSell`.

Rezultatul este un flux vizual care poate arăta un shop, dar fără oferte încărcate în runtime și fără tranzacție la click. Testele existente verifică prezența unor fragmente de cod și metodele `ShopService` izolat, nu deschid GUI-ul și nu execută o cumpărare.

Remediere: încărcarea ofertelor dintr-o sursă clară la startup/reload, handler GUI care validează sesiunea și oferta, apoi test de integrare pentru click, inventar și sold.

### P2 — `ShopService.clear()` lasă indexul de roluri contaminat

`ainpc-core-plugin/src/main/kotlin/ro/ainpc/economy/ShopService.kt:47` golește doar mapa `shops`, nu și `roleIndex`. După un reload, `findShopsForRole` poate returna definiții vechi sau duplicate, chiar dacă `getShop` nu le mai găsește. Fixul trebuie să golească ambele structuri sau să derive indexul din mapa principală.

### P2 — Deploy-ul smoke poate raporta succes după un transfer sau restart eșuat

În `scripts/smoke-vps.ps1:95` transferul este urmat necondiționat de `Ok` la `:97`; restartul SSH de la `:103` este urmat de `Ok` la `:106`. Nu se verifică exit code-ul operației înainte de raportare. O versiune veche poate rămâne pornită, iar smoke-ul poate valida serverul vechi.

Remediere: verificarea explicită a `$LASTEXITCODE`, oprire imediată la eroare și verificarea versiunii/build-id după restart.

### P2 — Lint-ul CI nu poate bloca jobul

`.github/workflows/build.yml:94` rulează `./gradlew kotlinLint --no-daemon || true`. Orice task lipsă sau eroare este transformată în succes, deci pasul numit „Check Kotlin code style” nu oferă feedback obligatoriu.

Remediere: task de lint declarat explicit și eliminarea `|| true`; dacă lint-ul este informativ, pasul trebuie denumit ca atare.

### P2 — Parserul RCON acceptă pachete incomplete

În `scripts/rcon-client.ps1:81` EOF-ul oprește citirea, iar `:85` verifică lungimea bufferului alocat, nu numărul efectiv de octeți citiți. `:93` poate decoda un pachet truncat ca răspuns valid sau gol.

Remediere: acumularea citirilor până la lungimea declarată și respingerea explicită când `$read -ne $len`.

### P2 — Logul smoke nu păstrează suficiente dovezi pentru un FAIL

`scripts/smoke-vps.ps1:60` persistă comanda, dar statusul și outputul rămân în consolă prin `Ok`/`Fail`. Rezumatul de la `:142` păstrează doar totalurile. După pierderea consolei nu se poate identifica rapid comanda sau răspunsul care a eșuat.

Remediere: JSONL per comandă cu status, exit code și output limitat/sanitizat.

### P1/P2 — Conținutul din feature-pack poate executa comenzi de consolă fără allowlist

`FeaturePackYamlSupport` încarcă direct `runtime_actions` și parametrii lor (`:728-746`), iar `ExecuteCommandAction` acceptă parametrul `as_console` și apelează `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved)` în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/runtime/actions/ExecuteCommandAction.kt:14-25`. Separat, recompensa de tip `command` din `ScenarioObjectiveProgress.kt:560-565` trimite `itemId` direct către consola serverului. Validatorul permite explicit tipul `command` în `QuestDraftValidator.kt:229`.

Nu apare o allowlist de comenzi, o semnătură/verificare de integritate a pack-ului sau o separare între conținut de încredere și conținut generat/importat. Dacă un jucător, un proces AI sau un operator cu acces limitat poate scrie/modifica pack-uri, acesta devine un salt direct la privilegii de consolă; chiar și într-un mediu administrativ, un pack corupt poate executa comenzi distructive la încărcarea scenariului/obținerea recompensei.

Remediere: eliminarea execuției arbitrare din conținutul generic sau allowlist strictă pe comenzi și argumente, interzicerea implicită a `as_console`, validare înainte de activarea pack-ului și audit cu autorul/sursa pack-ului. Testele trebuie să verifice că o comandă necunoscută este respinsă și că substituțiile nu permit injectarea de separatori de comenzi.

## Probleme de arhitectură și implementare grăbită

- `AINPCCommand.kt` are 5.175 linii non-goale și aproximativ 220 funcții; `AINPCCommandText.kt` are 2.886/211, `ScenarioEngine.kt` 2.974/134, iar `NPCManager.kt` 1.815/85. Aceste clase combină routing, validare, acces DB, mesaje și efecte Bukkit, ceea ce face regresiile greu de izolat.
- `WorldGenService.kt:47` calculează un executor preferat, apoi păstrează executorul injectat în constructor; comentariul spune că executorul nu poate fi schimbat. Parametrul `executorPreference` din `executePatch` nu controlează efectiv execuția fizică, ci poate face API-ul să pară configurabil fără să fie.
- `WorldEditBlockExecutor` ține sesiuni în memorie și `undo` șterge sesiunea înainte de a termina operația la `WorldEditBlockExecutor.kt:115`. Un restart sau o eroare după ștergere lasă construcția fără posibilitate de undo; aceasta este o limitare de lifecycle, nu o persistență reală a istoricului.
- `WorldEditBlockExecutor.undo` modifică blocurile, dar nu închide sau golește `EditSession` după operație în `ainpc-core-plugin/src/main/kotlin/ro/ainpc/worldgen/WorldEditBlockExecutor.kt:115`. Implementarea alternativă din `WorldEditAdapter` are explicit flush/close, deci cele două căi nu au același lifecycle și pot produce undo incomplet.

## Verificare și limitări

Rezultatele testelor rulate cu JDK 25 local, pentru proiect configurat cu Java 21:

- `:ainpc-core-plugin:test`: 1.351 teste, 0 fail, 1 skipped.
- `:ainpc-api:test`: 72 teste, 0 fail.
- `:ainpc-scenario-medieval:test`: 17 teste, 0 fail.
- `:ainpc-mcp-service:test`: 53 teste, 1 fail; `AinpcDebugHealthToolsTest` așteaptă `FRESH`, dar primește `STALE`.

Copia Java a fixture-ului MCP păstrează mtime-ul sursei: `runtime-snapshot-valid.json` are data 2026-07-18, iar `SnapshotReader` compară mtime-ul cu pragul de 3600 secunde. Testul trebuie să seteze explicit mtime-ul sau să injecteze un `Clock`.

Nu au fost executate serverul Paper, conexiuni OpenAI/Ollama, deploy VPS sau comenzi RCON. Constatările lor sunt derivate din cod și din contractele testate local.

## Ordine recomandată de remediere

1. Inițializarea rutelor economy și validarea duratei investiției.
2. Încărcarea progresului la reconnect și eliminarea SQL-ului SQLite din calea generică.
3. Unificarea providerului AI și conectarea validatorului în fluxul real.
4. Fixarea testului MCP dependent de mtime și transformarea lint/smoke în verificări care eșuează corect.
5. Separarea `AINPCCommand` și `ScenarioEngine` în servicii cu responsabilități clare.
