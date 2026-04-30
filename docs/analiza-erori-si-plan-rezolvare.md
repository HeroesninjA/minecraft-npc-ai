# Analiza Erori si Plan de Rezolvare

Actualizat: 2026-04-29

## Scop

Acest document centralizeaza erorile si riscurile confirmate in proiect si descrie cum trebuie rezolvate.

Important:

- documentul nu presupune modificari de cod facute acum
- analiza separa erorile reale de problemele structurale
- rezultatele sunt bazate pe rulare locala si pe logurile existente in repo

## Ce a fost verificat

Au fost rulate urmatoarele verificari:

- `mvn test`
- `mvn clean test`
- inspectarea logurilor din `debug-logs/`
- inspectarea scriptului `scripts/debug-openai.ps1`
- inspectarea codului din `OpenAIService` si `FeaturePackLoader`
- validarea review-ului extern Kiro AI din 2026-04-29

Data verificarii:

- 2026-04-28
- 2026-04-29 pentru review-ul extern Kiro AI

## Rezumat executiv

La momentul analizei, proiectul nu are erori de compilare sau test care sa blocheze build-ul.

Rezultatul real este:

- build Maven: `SUCCESS`
- compilare curata de la zero: `SUCCESS`
- teste automate existente: `SUCCESS`

Erorile confirmate in repo sunt in acest moment de doua tipuri:

1. erori istorice in logurile de diagnostic pentru Ollama
2. probleme structurale sau de observabilitate care nu rup build-ul acum, dar pot produce defecte mai greu de depistat

## Rezultatele build-ului

### Status actual

`mvn clean test` a trecut complet pentru:

- `ainpc-api`
- `ainpc-core-plugin`
- `ainpc-scenario-medieval`

Nu au fost reproduse:

- erori de compilare
- erori de test
- erori de packaging in faza test

### Concluzie

Nu exista in acest moment o eroare Java confirmata care sa blocheze build-ul proiectului.

Prin urmare, orice documentatie de remediere trebuie sa porneasca de la ideea corecta:

- problema actuala nu este una de compilare
- problemele reale sunt de diagnostic runtime, logging si structura de build

## Erori confirmate

### Eroarea 1: diagnostic HTTP Ollama cu acces invalid la proprietatea `Response`

Sursa:

- `debug-logs/ollama-debug-20260423-195901.log`

Mesaj observat:

- `The property 'Response' cannot be found on this object. Verify that the property exists.`

Context:

- eroarea apare la probele HTTP pentru `/api/version` si `/api/tags`
- logul indica faptul ca procesul `ollama` era pornit si asculta pe portul `11434`
- comanda `ollama list` functiona

Interpretare:

- problema nu pare sa fie ca Ollama nu ruleaza
- problema pare sa fie in scriptul de diagnostic, mai exact in tratarea rezultatului unei exceptii HTTP sau a unui obiect returnat diferit fata de ce asteapta scriptul

Impact:

- diagnosticarea locala devine nesigura
- logul poate sugera fals ca serverul e problema, desi defectul este in script

Plan de rezolvare:

1. scriptul de diagnostic trebuie sa foloseasca un wrapper uniform pentru raspunsurile HTTP
2. accesarea directa a unor proprietati conditionale precum `Response` trebuie eliminata
3. orice eroare HTTP trebuie serializata intr-o structura fixa:
   - `Success`
   - `StatusCode`
   - `Body`
   - `Error`
4. logurile trebuie sa afiseze separat:
   - eroarea de transport
   - codul HTTP, daca exista
   - preview-ul body-ului, daca exista

Observatie:

- scriptul actual `scripts/debug-openai.ps1` este deja mult mai aproape de acest model, ceea ce sugereaza ca logurile din `debug-logs/ollama-debug-*.log` provin dintr-o varianta mai veche sau dintr-un script separat care nu mai este in repo

### Eroarea 2: `Object reference not set to an instance of an object` in diagnosticul Ollama

Sursa:

- `debug-logs/ollama-debug-20260423-195921.log`

Mesaj observat:

- `Request esuat: Object reference not set to an instance of an object.`

Context:

- eroarea apare din nou la probele HTTP
- procesul si portul Ollama erau active
- `ollama list` functiona

Interpretare:

- aceasta este tot o eroare de scripting sau de tratare a raspunsului
- cel mai probabil exista o referinta nula in ramura de diagnostic HTTP
- nu exista date care sa indice o exceptie Java in plugin

Impact:

- investigarea incidentelor AI locale este ingreunata
- se pierde claritatea intre:
  - server indisponibil
  - endpoint gresit
  - raspuns neasteptat
  - bug in script

Plan de rezolvare:

1. orice acces la obiecte returnate din HTTP trebuie protejat prin verificari explicite de null
2. codul de diagnostic trebuie sa trateze distinct:
   - esec de conexiune
   - timeout
   - HTTP non-2xx
   - raspuns cu body invalid
3. trebuie adaugat un mesaj final de sumar care spune explicit una dintre variante:
   - serverul nu raspunde
   - serverul raspunde dar endpoint-ul e incompatibil
   - scriptul a esuat intern
   - modelul nu este disponibil

## Probleme structurale confirmate

Acestea nu sunt erori de build acum, dar sunt surse reale de defecte si confuzie.

### Problema 1: `ainpc-core-plugin` compila din folderul legacy `src/src`

Confirmare:

- inainte de refactorizarea din 2026-04-28, `ainpc-core-plugin/pom.xml` folosea:
  - `${project.basedir}/../src/src/main/java`
  - `${project.basedir}/../src/src/main/resources`

Status:

- rezolvat pentru build-ul Maven
- sursele si resursele core sunt acum in `ainpc-core-plugin/src/main`
- folderul legacy `src/src` a fost eliminat dupa migrare
- `mvn clean test` trece dupa schimbare

Impact:

- inainte de remediere, codul sursa real al modulului nu era localizat in modul
- IDE-ul, build-ul si refactorizarile puteau deveni greu de urmarit
- era usor sa apara divergente intre structura declarata si structura reala

Plan de rezolvare:

1. finalizat: mutarea fizica a surselor in `ainpc-core-plugin/src/main/java`
2. finalizat: mutarea resurselor in `ainpc-core-plugin/src/main/resources`
3. finalizat: revenirea la layout Maven standard in `pom.xml`
4. finalizat: rularea `mvn clean test` dupa migrare
5. finalizat: stergerea folderului legacy dupa validarea build-ului
6. ramas: curatarea mentiunilor istorice din documentatie doar unde incurca navigarea curenta

Prioritate:

- rezolvata pentru build; curatarea completa ramane prioritate medie

### Problema 2: logging slab la incarcarea feature pack-urilor

Confirmare:

- `FeaturePackLoader.loadPack()` logheaza doar:
  - `Eroare la incarcarea feature pack: <fisier>`
- apoi apeleaza `e.printStackTrace()`

Impact:

- mesajele nu sunt structurate
- lipsesc date utile precum:
  - pack id
  - sectiunea YAML problematica
  - cauza functionala
- in productie, stack trace-ul brut este mai greu de filtrat si corelat

Plan de rezolvare:

1. inlocuirea `printStackTrace()` cu logging prin `plugin.getLogger()`
2. includerea contextului minim:
   - fisier
   - pack id
   - sectiune
   - cauza exceptionala
3. introducerea unui raport de validare pentru pack-uri
4. diferentierea intre:
   - eroare de parse
   - eroare de schema
   - eroare de dependinte
   - override invalid

Prioritate:

- medie spre mare

### Problema 3: acoperire de teste foarte mica pentru zonele critice

Confirmare:

- in `ainpc-core-plugin` exista doar cateva teste de baza:
  - `RoutineEngineTest`
  - `SemanticVillageMapperTest`
  - `WorldAdminServiceTest`
- nu exista teste pentru:
  - `FeaturePackLoader`
  - `OpenAIService`
  - `AddonRegistry`
  - `ScenarioEngine`
  - `AINPCPlugin.reloadContent()`

Impact:

- defectele de runtime trec usor de build
- erorile de configurare si integrare nu sunt prinse devreme

Plan de rezolvare:

1. adaugarea de teste pentru incarcarea pack-urilor valide si invalide
2. adaugarea de teste pentru fallback-ul din `OpenAIService`
3. adaugarea de teste pentru `AddonRegistry`
4. adaugarea de smoke tests pentru lifecycle:
   - startup
   - reload
   - load addon

Prioritate:

- mare

## Probleme care nu sunt erori confirmate acum

Acestea trebuie tratate corect ca riscuri, nu ca defecte demonstrate:

- `ScenarioEngine` este foarte mare si greu de intretinut
- `NPCManager` este foarte mare si probabil prea cuplat
- `AINPCPlugin` concentreaza prea mult wiring

Acestea nu au generat o eroare de build in analiza curenta, dar cresc probabilitatea de:

- regresii la refactorizare
- bug-uri greu de izolat
- timp mare de depanare

## Validare Review Extern Kiro AI - 2026-04-29

### Verdict general

Review-ul extern este util ca lista de riscuri, dar nu este complet valid ca raport final de bug-uri critice.

Concluzia validata:

- multe probleme indicate exista in cod
- severitatea este exagerata in mai multe puncte
- unele exemple de cod propuse de Kiro sunt pseudo-cod si contin greseli de compilare
- metricile de tip `OutOfMemoryError dupa 6-8 ore`, CPU 40-60% sau TPS 15-18 nu sunt demonstrate in repo
- raportul trebuie folosit ca backlog de hardening, nu ca dovada ca toate problemele sunt bug-uri critice active

Nu trebuie copiate direct solutiile propuse de Kiro. Ele trebuie transformate in patch-uri mici, testabile, adaptate la arhitectura pluginului.

### Probleme confirmate sau partial confirmate

| # | Observatie Kiro | Verdict validat | Severitate realista | Actiune recomandata |
|---|-----------------|-----------------|---------------------|---------------------|
| 1 | `DecisionEngine` are cache-uri nelimitate | Confirmat. `scoreCache` si `lastDecisionTime` sunt `ConcurrentHashMap` fara TTL, limita sau cleanup explicit la stergerea NPC-ului. | Inalta | Adauga TTL/max size si metoda explicita de cleanup pe UUID. Apeleaza cleanup din fluxul de stergere/despawn unde este sigur. |
| 2 | `DialogueEngine.recentResponses` nu are cleanup | Confirmat partial. Lista per NPC este limitata la 5 raspunsuri, dar map-ul poate pastra UUID-uri pentru NPC-uri sterse. | Medie spre inalta | Adauga cleanup pe UUID si optional TTL. Nu este la fel de grav ca un cache care creste per mesaj, dar este real. |
| 3 | Race condition in `ConversationSessionManager.touchConversation()` | Partial respins. `computeIfPresent` pe `ConcurrentHashMap` este atomic pentru cheia respectiva. Raportul exagereaza riscul. | Scazuta spre medie | Nu adauga lock global fara nevoie. Daca apar operatii compuse mai mari, foloseste `compute` sau limiteaza managerul la main thread. |
| 4 | `AbstractPluginListener.callSync()` blocheaza thread async | Confirmat. Metoda asteapta `future.get(2, TimeUnit.SECONDS)` si este folosita din `NPCChatListener`. | Inalta | Inlocuieste treptat cu flux asincron bazat pe `CompletableFuture` sau adauga logging/timeout mai clar. Nu bloca inutil worker threads. |
| 5 | `NPCContext.updateNearbyEntities()` scaneaza entitati des | Confirmat. Fiecare update foloseste `World#getNearbyEntities` pe raza 20. | Inalta pentru multe NPC-uri | Introdu cache/spatial index pe tick sau pe interval scurt. Masoara inainte/dupa cu profiling. |
| 6 | `AINPC.getLocation()` sincronizeaza prea des | Partial confirmat. `getLocation()` apeleaza `syncLocationFromEntity()` la fiecare citire cand NPC-ul este spawned. | Medie | Separa citirea locatiei live de sincronizarea pentru persistenta. Evita dirty tracking care poate produce coordonate stale. |
| 7 | `EmotionManager.runEmotionUpdate()` verifica thread-ul prea des | Respins ca problema importanta. `Bukkit.isPrimaryThread()` este ieftin comparativ cu operatiile Bukkit si particulele. | Scazuta | Nu optimiza acum. Batch update merita doar daca profiling-ul arata cost real. |
| 8 | `NPCManager` este prea mare | Confirmat. Fisierul are peste 2500 linii si responsabilitati multiple: CRUD, DB, spawn, villager discovery, populare sat, profil, ancore. | Inalta ca mentenabilitate | Refactorizare incrementala in servicii, nu rescriere mare dintr-o data. |
| 9 | Verificari `null` duplicate | Partial valid. Exista multe verificari defensive, dar nu toate sunt code smell. | Scazuta | Nu crea `ValidationUtils` generic peste tot. Extrage doar validari repetate cu semantica clara. |
| 10 | Magic numbers | Confirmat partial. Exista praguri si distante hardcodate in mai multe clase. | Medie | Muta valorile de gameplay in config sau constante de domeniu, nu toate numerele mecanic. |
| 11 | Long methods / Strategy Pattern | Partial valid. `calculateActionScore()` nu are 80+ linii in codul curent, dar clasa si managerii mari au complexitate reala. | Medie | Refactorizeaza doar unde reduce risc. Strategy Pattern complet poate fi overengineering acum. |
| 12 | Exception swallowing | Partial confirmat. Unele catch-uri sunt fallback intentionat, dar altele logheaza prea slab sau doar in debug. | Medie spre inalta | Standardizeaza logging-ul si separa fallback normal de defecte reale. Nu arunca exceptii in toate cazurile, pentru ca unele fluxuri trebuie sa fie reziliente. |

### Probleme neconfirmate din raport

Urmatoarele afirmatii nu sunt demonstrate de cod sau de datele existente:

- `OutOfMemoryError dupa 6-8 ore`
- CPU 40-60% cu 50 NPC-uri
- TPS 15-18 cauzat direct de codul indicat
- 8 bug-uri critice confirmate
- race condition critica in `ConversationSessionManager`
- optimizarea `EmotionManager.runEmotionUpdate()` ca prioritate reala

Acestea pot deveni ipoteze de testare, dar nu trebuie tratate ca defecte confirmate fara profiling sau reproducere.

### Probleme in pseudo-codul propus de Kiro

Unele fragmente de cod din review-ul extern sunt doar schite si nu trebuie copiate direct:

- exemplul `EntityCacheManager` contine identificatori gresiti precum `woldCach.gt`, `isStae`, `Lst`, `ew ArrayList`
- exemplul pentru `AbstractPluginListener` mentioneaza in text `TimeUnt.SECONDS`, dar codul real foloseste `TimeUnit.SECONDS`
- refactorizarea completa a `NPCManager` in multe servicii este o directie buna, dar ca patch mare ar avea risc mare de regresii
- `ValidationUtils` generic poate ascunde contextul si poate face codul mai greu de citit daca este aplicat mecanic

### Prioritate dupa validare

P0:

- adauga cleanup explicit pentru cache-urile `DecisionEngine` si `DialogueEngine`
- conecteaza cleanup-ul la stergerea NPC-ului
- adauga teste/unit smoke pentru cleanup-ul cache-urilor

P1:

- reproiecteaza folosirea `callSync()` din `NPCChatListener`, preferabil cu flux asincron
- optimizeaza `NPCContext.updateNearbyEntities()` cu un cache scurt pe tick/interval
- imbunatateste exception handling pentru zonele unde se pierde contextul erorii

P2:

- separa treptat responsabilitatile din `NPCManager`
- muta distante/praguri importante in config sau constante de domeniu
- creste acoperirea de teste pentru manageri, AI fallback si incarcare feature packs

P3:

- curata duplicatele minore de validare
- adauga JavaDoc doar la API-uri publice sau zone greu de inteles
- optimizeaza `EmotionManager` numai daca exista masuratori care arata cost real

### Plan de lucru recomandat pentru remedierea review-ului

1. Stabilizare cache-uri.
   - `DecisionEngine`: TTL, limita, cleanup pe UUID.
   - `DialogueEngine`: cleanup pe UUID si optional TTL.
   - test minim pentru stergere NPC si curatarea intrarilor.

2. Eliminare blocking sensibil.
   - identifica exact fluxurile care folosesc `callSync()`.
   - transforma rezolvarea targetului din chat intr-un flow care nu blocheaza worker thread-ul mai mult decat este necesar.
   - pastreaza timeout si logging daca metoda ramane temporar.

3. Optimizare context NPC.
   - introdu cache de entitati pe lume sau index spatial pe interval scurt.
   - evita scanari identice pentru fiecare NPC in acelasi tick.
   - masoara impactul inainte si dupa.

4. Refactorizare controlata.
   - extrage mai intai servicii cu risc mic din `NPCManager`: persistenta, discovery, population.
   - ruleaza teste dupa fiecare extragere.
   - evita schimbarea simultana a spawn-ului, DB-ului si profilarii.

5. Testare si observabilitate.
   - adauga teste pentru cache cleanup, session manager, nearby entity logic si fallback AI.
   - imbunatateste logging-ul pentru exceptii fara sa opresti fluxurile care trebuie sa ramana reziliente.

## Ordinea recomandata de rezolvare

1. Clarificarea zonei de diagnostic AI.
   - Se decide daca scriptul vechi pentru Ollama mai este suportat.
   - Daca da, se aduce la acelasi standard cu `scripts/debug-openai.ps1`.

2. Standardizarea logurilor de eroare pentru feature packs si AI probes.
   - Fara `printStackTrace()`.
   - Cu context functional clar.

3. Curatarea mentiunilor istorice ramase catre `src/src`, doar unde incurca navigarea curenta.
   - Build-ul foloseste deja `ainpc-core-plugin/src/main`, iar folderul legacy a fost eliminat.

4. Cresterea acoperirii de teste in zonele de integrare.
   - Prioritar pentru pack loading, addon lifecycle si AI fallback.

5. Abia dupa aceea refactorizarea claselor mari.
   - `ScenarioEngine`
   - `NPCManager`
   - `AINPCPlugin`

## Definitia de rezolvare

Analiza de fata poate fi considerata inchisa doar cand sunt indeplinite toate conditiile de mai jos:

- scriptul de diagnostic nu mai produce erori interne de tip `Response` lipsa sau `Object reference not set`
- logurile disting clar intre eroare de script, eroare de retea si eroare de API
- `FeaturePackLoader` nu mai foloseste `printStackTrace()`
- `ainpc-core-plugin` compileaza din propriul `src/main/java`
- exista teste automate pentru cel putin:
  - pack loading
  - addon registry
  - AI fallback
  - reload content

## Concluzie

Starea actuala a proiectului este mai buna decat sugereaza logurile istorice:

- build-ul este verde
- testul existent trece
- nu exista o eroare Java de compilare confirmata acum

Problemele reale identificate sunt:

- diagnostic runtime fragil pentru zona AI/Ollama
- logging insuficient pentru erori de configurare
- mentiuni istorice ramase catre folderul legacy `src/src`
- acoperire de teste insuficienta pentru zonele cele mai riscante

Acestea sunt punctele care trebuie rezolvate primele.
