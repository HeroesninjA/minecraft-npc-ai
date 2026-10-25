# Debugging si testare

Status: runbook operational canonic.
Actualizat: 2026-10-25.

Acest document detine fluxul de triage. Starea implementata apartine codului si testelor, iar imbunatatirile neimplementate raman in `planning/diagnostic-si-observabilitate-roadmap.md`.

## Flux de triage

1. reproduce problema cu input minim si inregistreaza versiunea commitului, Java, Paper, backend-ul si feature flags relevante;
2. pastreaza logul de startup si fereastra exacta in care apare defectul;
3. ruleaza auditul cel mai ingust: de exemplu `/ainpc audit db`, `npc`, `world`, `spawn` sau `quest`;
4. foloseste un sumar `debugdump` in chat pentru domeniul afectat;
5. genereaza un export in fisiere numai cand sumarul nu este suficient;
6. ruleaza testul sau categoria cea mai apropiata de defect;
7. aplica fixul la cauza, adauga regresia si repeta pasii in aceeasi ordine;
8. pentru persistenta sau lifecycle, executa un restart controlat si verifica din nou.

Nu modifica date manual pentru a ascunde simptomul si nu combina simultan upgrade, migrare si fix functional.

## Diagnostic pe server

Necesita `ainpc.admin`:

```text
/ainpc audit <mod>
/ainpc audit <mod> [strict|full|offline] json
/ainpc debugdump
/ainpc debugdump runtime
/ainpc debugdump features
/ainpc debugdump scenario
/ainpc debugdump progression
```

Pentru CI sau smoke prin RCON, foloseste `scripts/ainpc-audit-rcon.ps1`; acesta valideaza semantic `ainpc-audit-report` schema v1, inclusiv cererea mode/profile, planul de executie, totalurile, severitatile, verdictul si payload-urile DB/spawn, apoi intoarce procesului codul `0`, `1` sau `2`, respectiv `3` pentru transport/contract invalid. Parametrul `-ResponseFile` ruleaza aceeasi validare pe un fixture salvat fara server si fara credentiale.

Pentru drift de persistenta, `/ainpc audit db json` expune `database_schema`: tabele descoperite/mapate, lipsuri, tabele neclasificate, row counts si acoperirea domeniilor. Un warning de schema cere verificarea backend-ului si a migrarii; inventarul nu valideaza forma coloanelor sau constrangerile.

Pentru istoric spawn complet, ruleaza `/ainpc audit all full json` sau `offline`. Obiectul `spawn_history` confirma `complete`, totalul scanat si numarul paginilor; detaliile individuale raman bounded in `sections`, iar un mismatch de total indica inclusiv posibilitatea unei mutatii concurente intre pagini.

`world`, `regions`, `places`, `nodes`, `npcbound`, `mapping`, `routing`, `quest`, `questconfig`, `story`, `authoring`, `ai`, `runtime`, `mcp`, `features`, `scenario` si `progression` afiseaza sumaruri in chat/consola.

Numai comenzile urmatoare creeaza folder de export:

```text
/ainpc debugdump all [uuid-fragment] [privacy-safe]
/ainpc debugdump npc [privacy-safe]
```

Comanda captureaza mai intai starea runtime pe main thread, confirma ca exportul continua asincron si livreaza rezultatul ulterior tot pe main thread. SQL-ul ruleaza intr-o singura tranzactie pe worker, iar tail-ul, cleanup-ul si scrierea nu blocheaza tick-ul. Numai un export poate fi activ; mesajul `deja in curs` cere asteptarea rezultatului cererii precedente.

Folderul rezultat este `plugins/AINPC/debug-dumps/debug-dump-<timestamp>/`; coliziunile din aceeasi secunda primesc sufix numeric. Filtrul optional din `all` cauta un fragment literal din UUID-ul stocat (nu rezolva nume de jucatori) in `player-progressions.json`, `player-quest-progress.json`, `player-progression.json` si `quest-anchor-bindings.json`; randurile sunt filtrate direct in SQL, iar sumarurile acestor fisiere reflecta doar randurile filtrate. Fiecare JSON include `player_filter_present`, iar `quest.txt` indica filtrul cerut; niciunul nu repeta valoarea fragmentului ca metadate. Scope-ul `npc` nu accepta filtru, fiindca nu contine aceste exporturi. Auditul quest, evenimentele story si alte artefacte raman globale, deci filtrul nu izoleaza toate datele unui singur jucator. Optiunea `privacy-safe` poate fi pusa inainte sau dupa filtru si este recomandata pentru orice artefact care paraseste serverul. `index.txt` indica doar aplicarea filtrului, fara valoarea introdusa, si declara fazele snapshot, limitele active, cleanup-ul si artefactele trunchiate; `manifest.json` declara schema de confidentialitate, metadata snapshot si inventarul payload-urilor. Retentia automata elimina oldest-first numai directoarele managed si protejeaza exportul curent; inspecteaza ambele fisiere inainte sa distribui dump-ul sau sa interpretezi absenta unor randuri drept absenta starii runtime.

Citeste `operations/observability-and-logs.md` inainte sa distribui un dump.

## Pregatire izolata fara pornire

Cand tinta de test nu este inca confirmata, poti pregati fisierele fara a porni Paper sau a accepta EULA:

```powershell
./scripts/smoke-paper-addon-lifecycle.ps1 -PaperJar "C:\cale\paper.jar" -SkipBuild -PrepareOnly
```

Runnerul pastreaza un director nou in `.ai/tmp/`, cu core-ul copiat, `eula=false`, bind `127.0.0.1`, port automat si RCON oprit. Nu foloseste lumile sau DB-urile unui server existent. `-SkipBuild` presupune artefacte deja construite; fara el poate rula build-ul Gradle, dar nu lanseaza serverul in modul de pregatire.

Raportul `paper-addon-lifecycle-preparation.json` este separat de cel runtime: exit code 0 confirma numai pregatirea, cu `status=prepared_not_run`, `preparation_ok=true`, `ok=false`, `smoke_attempted=false` si zero faze. Compatibilitatea dintre JAR-ul Paper, Java si plugin nu este certificata prin copiere sau hash. Pornirea reala prin runner necesita ulterior `-AcceptEula`, numai dupa acceptarea de catre operator, si creeaza un alt director izolat; pregatirea nu accepta EULA nici daca ambele switch-uri sunt date. [Contractul complet](../reference/kotlin-paper-packaging-si-smoke.md#smoke-paper) separa aceste doua actiuni.

Checkpoint local 2026-10-25: pregatirea a fost executata cu `-SkipBuild`, fara pornire, cu `eula=false`, zero faze, fara lume sau loguri generate si cu hash-ul core copiat identic artefactului selectat. Candidatul Paper local declara in `version.json` versiunea 26.1.2 si Java 25; compatibilitatea cu pluginul compilat pe API 1.21.11 ramane neverificata. Cele sase teste noi verifica scriptul cu runtime neexecutabil in fixture, pastrarea dovezilor anterioare si refuzul pornirii fara acceptare explicita. Scriptul este acum declarat input Gradle pentru a nu reutiliza teste invechite dupa editarea lui. Verificarea completa core/API/addon are 3.173 de teste, fara esecuri sau erori si cu doua omise; ABI-ul si freeze-ul static trec. Raportul `.ai/release-reports/paper-sandbox-preflight-2026-10-25T13-36-56Z.json` separa acest rezultat de un smoke runtime si pastreaza calea sandbox-ului.

## Smoke test cognitiv

Observarea nu necesita activarea continuitatii. Foloseste un NPC existent si `ainpc.admin`:

```text
/ainpc decision <numeNpc>
/ainpc decision nearest
```

Consola trebuie sa indice numele; `nearest` necesita un jucator si cauta in raza de 10 blocuri. Nu porni o conversatie pentru a inspecta ingrijirea in curs: conversatia poate intrerupe legitim obiectivul. Comanda de diagnostic nu face aceasta tranzitie.

1. Verifica separat `Atentie`, `Nevoi (0-100)`, `Intentie curenta`, `Ultima decizie aplicata` si `Progres ingrijire`. Absenta unei inregistrari recente nu este o eroare si nu trebuie inlocuita printr-o reusita inventata. Distributia candidatilor este o previzualizare, nu dovada executiei.
2. Repeta comanda fara o actualizare reala a simularii intre citiri. Nevoile, progresul si numarul deciziilor reale nu trebuie sa avanseze din cauza comenzii; vechimea observatiei poate creste normal. Un tick periodic intre citiri poate schimba legitim datele.
3. Verifica `Continuitate ingrijire`. Pentru un NPC `FULL`, configuratia livrata trebuie raportata ca feature oprit. Raportul explica primul impediment in ordinea verificarilor, nu toate cauzele simultan. O stare deja modificata sau un obiectiv intrerupt poate fi impedimentul raportat in locul unei alte conditii. Satietatea si energia mari inseamna nevoi mai bine acoperite, nu foame sau oboseala mai mari.

Pentru scenariul activ, foloseste numai un server de test pregatit, un NPC `FULL` cu nevoie sub 35 si o casa valida in lumea curenta. Pastreaza configuratia initiala si activeaza explicit:

```yaml
features:
  simulation: true
  routine: true
simulation:
  enabled: true
  decision:
    goal_continuity_enabled: true
routine:
  enabled: true
```

Acesta este un fragment de configuratie, nu un fisier complet; pastreaza celelalte setari si foloseste fluxul de reload/restart al mediului de test. Nu modifica DB-ul sau terenul de productie pentru a forta scenariul.

4. Lasa rutina sa accepte deplasarea spre casa. Diagnosticul poate arata `RETURNING_HOME` si continuitatea eligibila, dar nu trebuie sa afirme sosirea. O alegere aplicata de continuare trebuie sa mentioneze pastrarea revenirii; selectia singura nu muta entitatea.
5. Dupa sosirea confirmata, urmareste `RECOVERING`, apoi imbunatatirea nevoii la actualizari reale si `SATISFIED` la pragul 60. Sosirea singura nu incheie obiectivul. Un rezultat produs printr-o schimbare externa nu trebuie atribuit automat planului.
6. Daca se cumuleaza trei minute de observatii valide fara sosire ori refacere, asteapta eliberarea prioritatii si mentionarea cauzei necunoscute, nu declararea unei rute imposibile. Reaplicarea aceleiasi intentii nu trebuie sa reseteze contorul. Intervalele invalide sau prea lungi pot invalida observatia, conform contractului de simulare.
7. Verifica separat o conversatie, un pericol, o pauza de rutina si un override: continuitatea nu trebuie sa le suprascrie. Oprirea feature-ului trebuie sa elimine imediat eligibilitatea si reutilizarea alegerii de continuitate din cache; citirea diagnosticului nu sterge cache-ul. La final, restaureaza configuratia initiala a serverului de test.

Inregistreaza versiunea, flagurile, intervalele taskurilor si diagnosticul dinainte/dupa fiecare eveniment. Scenariul ramane de verificat pe server; testele unitare si de integrare nu reprezinta un smoke test Minecraft executat.

### Livrari si pauze observabile

Aceeasi comanda `decision` arata acum `Ajutor alimentar confirmat`, ultima confirmare pastrata si `Pauza cereri hrana`. Este tot un diagnostic administrativ, fara executarea simularii sau a unui transfer. Istoricul alimentar este distinct de decizia aplicata si de progresul ingrijirii autonome.

- Inainte si dupa `Shift + click dreapta`, compara inventarul real, ID-ul confirmarii, `recordedAt` in milisecunde epoch, UUID-ul celui care a ajutat si efectul consemnat asupra satietatii. Valorile din confirmare sunt istorice, nu inlocuiesc nevoia curenta afisata separat. Un timestamp in viitor este semnalat explicit, nu prezentat ca livrare noua.
- Numarul reprezinta numai confirmarile pastrate, cel mult 32; la capacitate, o livrare noua poate schimba ultima confirmare fara sa creasca numarul. Numele jucatorului nu este inclus in acest sumar. UUID-urile raman identificatori sensibili pentru distribuirea logurilor.
- Un administrator care executa comanda ca jucator vede numai pauza propriei identitati, prin [UUID-ul persistent Paper](https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/Entity.html#getUniqueId()). `PROMISE` inseamna timp acordat livrarii, nu aliment primit; `DECLINE` inseamna refuz respectat. Alt UUID, chiar cu acelasi nume, nu mosteneste pauza. Consola declara pauza neinspectata, nu absenta ei.
- Absenta unei pauze nu certifica eligibilitatea unei cereri noi: limitele de timp, nevoia, vizibilitatea si celelalte conditii raman separate. Citirea nu reinnoieste si nu sterge pauzele expirate. Dupa livrarea promisa reusita, numai o captura noua reflecta confirmarea si incheierea asteptarii; captura anterioara ramane neschimbata.

`AINPCDecisionCommandTest` verifica formatul limitat, separarea datelor istorice si cazul timestamp-ului viitor. `AINPCDecisionDiagnosticCommandTest` exercita comanda reala cu expeditori controlati, identitati distincte, livrarea prin `NpcFoodAid`, expirarea si snapshot-urile inghetate. Inventarul acestui test este controlat in fixture; nu reprezinta dovada unui click, a fizicii sau a unui server Paper pornit.

Checkpoint local 2026-10-25 pentru acest diagnostic: sapte teste noi; verificarile complete core/API/addon medieval trec 3.163 de teste, cu zero esecuri sau erori si doua omise. Cele trei JAR-uri sunt construite; ABI-ul API 1.1.0 ramane neschimbat, iar freeze-ul static trece fara avertismente. Raportul `.ai/release-reports/cognitive-food-diagnostic-preflight-2026-10-25T12-55-44Z.json` pastreaza rezultatele si hash-urile artefactelor, cu smoke-ul Minecraft explicit neexecutat. Nu sunt modificate implicit flagurile de simulare si nu este instalat niciun plugin.

### Memoria alimentara dupa rehidratare

`NpcFoodRequestServiceTest` leaga acum o livrare prin `NpcFoodInteraction`, serializarea profilului cu `buildProfileData`, rehidratarea folosita de `NPCManager` si o cerere autonoma ulterioara. Confirmarea initiala nu este inserata manual. Inventarul, entitatile si expeditorii raman controlati in fixture.

```powershell
./gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.engine.NpcFoodRequestServiceTest" --console=plain
```

Cele patru scenarii suplimentare verifica:

- confirmarea si satietatea 32 rezultate dintr-un singur mar pornind de la 20 sunt pastrate, iar cererea merge la UUID-ul ajutorului, nu la un omonim mai apropiat;
- trei cicluri de reconstruire/rehidratare nu consuma alte obiecte, nu cresc din nou satietatea si nu transforma promisiunea nelivrata intr-o confirmare; pauza vechii instante nu este indeplinita de citirea profilului;
- lipsa dovezii, schema nesuportata, o cantitate invalida, expirarea si timestamp-ul viitor nu activeaza o cerere dupa rehidratare;
- raspunsul unei cereri apartinand vechii instante este respins; in acelasi proces, serviciul existent pastreaza cooldown-ul de cinci minute si accepta raspunsul numai dupa o cerere noua valida.

Preferintele, cererile in asteptare si throttling-ul raman numai runtime, conform contractului existent; aceste teste nu adauga persistenta lor. Rehidratarea este invocata prin metoda de productie pe un manager controlat, nu prin inlocuirea manuala a memoriei. Nu se executa SQL, restart Paper, navigatie sau click Minecraft real. Pentru validarea dupa restart, pastreaza separat dovezile din server si confirma politica starii runtime; nu extinde garantia acestor teste la intregul restart.

Checkpoint local 2026-10-25: cele 29 de teste din `NpcFoodRequestServiceTest` trec; verificarea completa core/API/addon medieval are 3.167 de teste, fara esecuri sau erori si cu doua omise. Construirea JAR-urilor si freeze-ul static trec, cu ABI 1.1.0 neschimbat si fara actualizarea baseline-ului. Raportul `.ai/release-reports/cognitive-food-hydration-preflight-2026-10-25T13-12-19Z.json` pastreaza rezultatele, hash-urile si limitarile, fara sa declare un smoke Minecraft executat.

### Nevoi invalide in profil

Regresiile pentru rehidratare au reprodus trei probleme ale citirii generice anterioare: nivelul negativ era acceptat, un boolean putea opri restaurarea celorlalte campuri, iar numarul `4294967316` devenea sațietate 20 prin conversie cu overflow. Un NPC cu valoare anterioara 82 putea astfel parea infometat fara o observatie reala.

```powershell
./gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.managers.NpcSimulationNeedsHydrationTest" --tests "ro.ainpc.engine.NpcFoodRequestServiceTest" --console=plain
```

Cele cinci teste suplimentare verifica toate cele cinci nevoi, intervalul inclusiv 0-100, fractiile, numerele prea mari, tipurile gresite, campurile absente si compatibilitatea numerelor exact integrale in forma zecimala sau exponentiala. Valoarea invalida pastreaza numai nivelul anterior al acelui camp, fara clamp la extreme si fara pierderea celorlalte date valide. Un test porneste de la ajutor alimentar efectiv consumat in inventarul controlat, trece prin JSON si metoda de productie de rehidratare, apoi verifica lipsa unei cereri false si pastrarea confirmarii. Controlul cu sațietate numeric valida 20 produce in continuare cererea catre UUID-ul corect.

Testele nu modifica schema profilului, nu rescriu DB-uri existente si nu certifica un restart sau o interactiune Minecraft. Pentru un profil editat manual, foloseste numere JSON valide, nu stringuri numerice; nu forta extremele nevoilor pentru a ascunde date invalide. [Contractul restaurarii](../architecture/simulation-service.md#restaurarea-nevoilor-din-profil) descrie politica exacta de fallback.

Checkpoint local 2026-10-25: cele trei regresii initiale sunt reproduse inainte de corectie; dupa corectie trec cele 76 de teste focalizate si verificarea completa core/API/addon, cu 3.178 de teste, fara esecuri sau erori si doua omise. JAR-urile sunt construite; ABI-ul 1.1.0 ramane neschimbat, iar freeze-ul static trece fara avertismente. Raportul `.ai/release-reports/cognitive-needs-validation-preflight-2026-10-25T14-35-42Z.json` pastreaza dovezile red/green si hash-ul noului core. Serverul nu este pornit, iar sandbox-ul pregatit anterior ramane neschimbat, cu artefactul sau istoric; noul build nu este instalat automat acolo.

### Timp si intentii invalide in profil

Sase regresii suplimentare verifica timestamp-ul si textele din `simulation` prin rehidratarea reala a managerului. Inainte de corectie, patru teste esueaza: `last_simulation_tick_at=-1` inlocuieste un timp anterior valid, un boolean intrerupe restaurarea memoriei si ancorelor, `current_goal=true` devine intentia textuala `true`, iar `18446744073709551617` se ingusteaza la 1. Ultimul caz produce la prima actualizare reala a nevoilor scaderea satietatii 31 -> 25, desi profilul nu furnizeaza un timp valid.

```powershell
./gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.managers.NpcSimulationNeedsHydrationTest" --tests "ro.ainpc.managers.NPCManagerTextTest" --tests "ro.ainpc.engine.DecisionExperienceIntegrationTest" --tests "ro.ainpc.engine.CognitiveLifeSimulationTickTest" --tests "ro.ainpc.engine.NpcFoodRequestServiceTest" --console=plain
```

Dupa corectie trec toate cele 101 teste focalizate. Datele invalide pastreaza fiecare valoare anterioara, iar memoria alimentara si ancora valida supravietuiesc. Un control pozitiv cu timestamp numeric 1 continua sa actualizeze nevoile prin acelasi motor; numerelor intregi mari, timestamp-urilor viitoare reprezentabile si textelor valide nu li se schimba semantica. [Contractul restaurarii](../architecture/simulation-service.md#restaurarea-timpului-si-intentiilor) explica de ce validarea reprezentarii nu confirma timpul real sau executia unei intentii. Testele nu pornesc Minecraft, nu rescriu baze de date si nu valideaza un restart complet.

Checkpoint local 2026-10-25: verificarea completa core/API/addon si construirea JAR-urilor reusesc, cu 3.184 de teste, zero esecuri sau erori si doua omise. Core-ul ruleaza din nou; task-urile test API/addon neschimbate isi reutilizeaza rezultatele valide up-to-date. Freeze-ul static trece fara avertismente, iar ABI-ul 1.1.0 ramane neschimbat. Raportul `.ai/release-reports/cognitive-profile-runtime-preflight-2026-10-25T15-16-14Z.json` pastreaza cele patru dovezi red, rezultatele green si hash-urile artefactelor. Nu se porneste serverul si nu se actualizeaza sandbox-ul pregatit anterior.

### Ceas egal sau regresiv in simulare

Opt teste noi acopera timpul neinitializat, egal sau regresiv, un timestamp viitor foarte mare, reluarea dupa rebazare si pastrarea factorilor pentru intervale pozitive. Sase esueaza inainte de garda de interval: trei actualizari fara timp scurs reduc satietatea 41 -> 38; o masa in aceeasi milisecunda o creste 41 -> 45; un profil valid cu timp `Long.MAX_VALUE` o reduce 31 -> 30 la primul update. Intrarea publica a managerului reproduce scaderea 20 -> 19 pentru `FULL`; dupa corectie acelasi test verifica si `LIGHT`.

```powershell
./gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.engine.DecisionSimulationClockTest" --tests "ro.ainpc.engine.CognitiveLifeSimulationTickTest" --tests "ro.ainpc.managers.NpcSimulationNeedsHydrationTest" --tests "ro.ainpc.engine.DecisionExperienceIntegrationTest" --tests "ro.ainpc.engine.SelfCareGoalIntegrationTest" --tests "ro.ainpc.engine.SelfCareContinuityIntegrationTest" --tests "ro.ainpc.engine.NpcFoodRequestServiceTest" --console=plain
```

Ceasul controlat afecteaza numai actualizarea nevoilor; constructorii existenti pastreaza ceasul sistemului. Regresiile verifica refuzul atribuirii de experienta pe interval invalid, pastrarea istoricului, lipsa reactivarii atribuirii vechi la revenirea timpului si invatarea dupa o noua aplicare valida. Controlul de profil foloseste scrierea JSON si rehidratarea reala, iar controlul managerului foloseste perceptia si entitatea controlate din fixture. [Politica intervalelor](../architecture/simulation-service.md#intervale-de-simulare-fara-timp-scurs) nu reprezinta validarea unui restart sau a fizicii Minecraft.

Checkpoint local 2026-10-25: trec cele 84 de teste focalizate si verificarea completa core/API/addon, cu 3.192 de teste, zero esecuri sau erori si doua omise. Core-ul ruleaza din nou; API/addon neschimbate reutilizeaza rezultatele valide up-to-date. JAR-urile sunt construite, constructorii existenti sunt prezenti in bytecode, iar freeze-ul static trece fara avertismente si fara schimbarea ABI-ului 1.1.0. Raportul `.ai/release-reports/cognitive-clock-rebase-preflight-2026-10-25T15-36-01Z.json` pastreaza cele sase esecuri initiale, controalele pozitive, hash-urile si limitele. Serverul nu este pornit, iar sandbox-ul pregatit anterior nu este modificat.

### Progres memorat versus situatie curenta

Cinci regresii noi reproduc formularea nejustificata la prezent a unui snapshot activ. Dupa un tick public care aplica masa acasa, fixture-ul observa pozitia atelierului fara o noua actualizare a obiectivului, dar raspunsul local anterior continua sa spuna `Sunt acasa`. Dupa atribuirea unei case valide, o observatie anterioara `BLOCKED` continua sa spuna `imi lipseste o casa confirmata`. Alte controale verifica modelul comun, diagnosticul si raspunsul local cu o intentie curenta diferita.

```powershell
./gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.npc.NpcSelfCareGoalTest" --tests "ro.ainpc.ai.NpcCognitiveResponseTest" --tests "ro.ainpc.engine.CognitiveLifeSimulationTickTest" --tests "ro.ainpc.engine.SelfCareGoalIntegrationTest" --tests "ro.ainpc.commands.AINPCDecisionCommandTest" --tests "ro.ainpc.commands.AINPCDecisionDiagnosticCommandTest" --tests "ro.ainpc.ai.OpenAIPromptSnapshotIsolationTest" --console=plain
```

Corectia modifica numai formularea explicatiilor din `NpcSelfCareGoal.Snapshot.description`: etapele active si contorul fara progres se refera la ultima observatie, nu la o perceptie noua. Starea, timestamp-ul, vechimea, pragurile, deciziile si invatarea nu se schimba. Testul prin manager verifica si lipsa scanarilor, miscarilor, actualizarilor de nevoi, selectiilor sau memoriilor suplimentare la intrebare. Testele existente pastreaza distinctia dintre intentie, sosire, refacere si succes, precum si izolarea promptului dupa stergerea obiectivului live. [Contractul monitorului](../architecture/simulation-service.md#progresul-obiectivelor-de-ingrijire) ramane o inregistrare limitata, nu un senzor de locatie in timp real.

Pozitia este modificata in fixture pentru a reprezenta o schimbare externa intre observatii; nu se invoca `AINPC.teleport`, care sterge deja monitorul live. Contextul furnizat catre AI este verificat, nu raspunsul unui provider real. Nu se porneste Minecraft si nu se confirma navigatie sau perceptie fizica reala.

Checkpoint local 2026-10-25: toate cele cinci regresii esueaza inainte de corectia formularii, apoi trec cele 83 de teste focalizate. Verificarea completa core/API/addon si construirea JAR-urilor reusesc cu 3.197 de teste, zero esecuri sau erori si doua omise. Core-ul ruleaza din nou; task-urile API/addon neschimbate reutilizeaza rezultatele valide up-to-date. Freeze-ul static trece fara avertismente si pastreaza ABI-ul 1.1.0. Raportul `.ai/release-reports/cognitive-progress-history-preflight-2026-10-25T16-02-07Z.json` pastreaza dovezile red/green, hash-urile si limitele. Nu se porneste serverul, nu se solicita un provider AI real si nu se modifica sandbox-ul pregatit anterior.

### Integrarea pe tick-ul public

Regresia locala pentru bucla cognitiva ruleaza fara server:

```powershell
./gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.engine.CognitiveLifeSimulationTickTest"
```

Testul foloseste intrarea publica a managerului, actualizarea reala a nevoilor, alegerea, aplicarea, memoria si raspunsul local. Perceptia, entitatea si pozitia sunt controlate in fixture; nu se instaleaza o implementare globala de registre Paper si nu se afirma executia fizicii ori a navigatiei reale. Invocarile sunt accelerate, cu un interval pozitiv intre ele si cu limitarile de timp existente ale simularii.

Doua regresii protejeaza explicit pierderea entitatii: flagul `spawned` ramas vechi nu permite continuarea pe coordonatele stocate, iar invalidarea in timpul actualizarii contextului opreste tick-ul inainte de nevoi si decizii. Aceeasi protectie este verificata pentru `LIGHT` si pentru apelurile directe ale motoarelor. O disparitie nu trebuie sa produca succes, experienta sau decizie aplicata noua.

Patru scenarii acopera si callback-ul aplicarii: schimbarea modului cu revenire la `FULL` nu rearmeaza invatarea anulata, conversatia isi pastreaza intentia, revenirea la aceeasi stare dupa o intrerupere nu certifica alegerea veche, iar un tick nou executat reentrant isi pastreaza explicatia, obiectivul si perioada proprie de invatare. Regresiile urmaresc tick-urile urmatoare: experienta apare numai dupa o noua aplicare valida si observarea ulterioara, nu din simpla refacere a nevoii dupa intrerupere.

Alte patru scenarii verifica proiectia contextului dupa aplicare: teleport urmat de tick reentrant, conversatie cu schimbarea pozitiei fara invalidarea ciclului de viata, pierderea entitatii dupa stergerea indiciilor de locatie si aplicarea normala cu sau fara tranzitie de stare. Textul contextului trebuie sa descrie atelierul observat, nu casa dinaintea interactiunii, si sa pastreze intentia curenta. Numarul scanarilor confirma ca repararea proiectiei nu executa inca o perceptie a lumii. Teleportul si pozitia sunt controlate in fixture; nu reprezinta validare fizica in Minecraft.

### Comparatie cauzala a alegerii

Doua scenarii din `CognitiveLifeSimulationTickTest` verifica adaptarea prin managerul public. Mai intai, masa este aleasa si aplicata, apoi o actualizare reala observa cresterea satietatii si produce memoria. Pentru alegerea ulterioara, ambele ramuri pornesc de la aceleasi nevoi, pozitie, stare, personalitate, ora si configuratie: satietate 33, energie 20, acasa, stare `IDLE`. Configuratia ponderarii ramane cea implicita; fixture-ul activeaza alegerea probabilistica.

Cache-ul si istoricul selectiilor sunt golite in ambele ramuri. Timestamp-ul ultimei actualizari este zero pentru a nu modifica nevoile inghetate la primul tick al comparatiei. Testul cauta un seed pentru care memoria schimba rezultatul, apoi foloseste acelasi UUID, seed si numar de selectie in ambele executii publice: fara observatie se aplica `SLEEP`, cu observatia se aplica `EAT`. Aceasta izoleaza influenta memoriei; nu afirma ca toate seed-urile sau toate situatiile produc alegeri diferite.

Al doilea scenariu repeta comparatia dupa scrierea si recitirea textului JSON cu `NpcActionExperienceCodec`; nu simuleaza un restart complet sau accesul bazei de date. Ambele scenarii verifica raspunsul local `Ce ai invatat?` fara mutatii, lipsa unei observatii noi la simpla reaplicare a intentiei, noua observatie abia la actualizarea urmatoare, oprirea influentei prin `experience_enabled=false` si neutilizarea experientei vechii case la o casa noua. Istoricul ramane pastrat in aceste controale. Perceptia si entitatea sunt controlate; validarea fizica in Minecraft ramane separata.

### Promisiunea indeplinita prin ajutor

`NpcFoodRequestServiceTest` verifica si circuitul fara confirmari inserate manual: inventar controlat → aliment consumat prin `NpcFoodInteraction` → confirmare exacta de ajutor → cerere privata → `Iti aduc mancare` → o noua livrare reusita. Promisiunea singura nu schimba inventarul, nevoile sau dovezile. Livrarea incheie numai asteptarea promisiunii destinatarului exact; omonimul si rollback-ul nu o incheie, iar refuzul explicit ramane respectat. Testele de preferinte si ajutor acopera suplimentar expirarea, ceasul anterior promisiunii, replay-ul si eliberarea capacitatii limitate.

Pentru verificarea in joc, foloseste serverul de test si conditiile existente pentru cereri: NPC `FULL`, ajutor anterior confirmat, nevoie dominanta, apropiere, vizibilitate si disponibilitate. Raspunde unei cereri reale cu promisiunea, apoi ofera un aliment prin `Shift + click dreapta`. Verifica transferul unei singure unitati si noua confirmare, fara sa presupui ca satietatea a ajuns la pragul de recuperare. La o foame ulterioara, cererea autonoma poate reveni numai dupa limitele normale de cinci minute per NPC si un minut per jucator; livrarea nu le reseteaza. Un refuz urmat de ajutor voluntar nu scurteaza pauza de 15 minute.

Fixture-ul automat controleaza slotul si cantitatea, dar nu executa inventarul unui server Minecraft, fizica sau intregul listener Bukkit. Aceasta verificare nu inlocuieste smoke test-ul in joc.

### Explicarea asteptarii in dialog

Dupa un refuz sau o promisiune acceptata, intreaba NPC-ul `De ce ai nevoie?` cat timp nevoia alimentara este activa si contextul sigur. Raspunsul trebuie sa pastreze nevoia reala, fara alta cerere de hrana, si sa explice numai motivul corespunzator jucatorului care intreaba: respectarea refuzului sau timpul acordat livrarii promise. Alt jucator nu primeste motivul acestei pauze. Declaratia nu trebuie prezentata ca aliment deja primit.

Dupa livrare ori expirare, o intrebare noua foloseste starea curenta; citirile nu prelungesc pauza si nu executa simularea. `DialogueRequestContextTest`, `NpcCognitiveResponseTest` si `NpcFoodRequestPreferencesTest` verifica acest contract, limitarile pentru pericol sau nevoie deja satisfacuta si compatibilitatea metodei publice Java. `OpenAIPromptSnapshotIsolationTest` verifica motivul si indicatorul pentru destinatarul exact, pastrarea snapshot-ului vechi dupa livrare sau expirare si capturarea corecta a unui refuz ulterior. Aceste verificari inspecteaza datele si promptul construit; nu afirma ca un provider AI a generat corect un raspuns in joc.

## Verificare integrala si artefact

Inaintea smoke-ului in joc, separa testele complete de poarta ABI si de construirea artefactului:

```powershell
./gradlew.bat :ainpc-core-plugin:test :ainpc-api:test :ainpc-core-plugin:jar --console=plain
./gradlew.bat :ainpc-api:verifyApiAbi --console=plain
```

`JAVA_HOME` trebuie sa indice un JDK complet, inclusiv `bin/javap.exe` pentru verificarea ABI. Un runtime JetBrains minimal poate executa testele, dar sa nu ofere acest utilitar. Nu modifica baseline-ul pentru a ascunde lipsa uneltei sau diferentele API. `:ainpc-api:check` depinde si de poarta ABI; un esec acolo nu trebuie confundat cu esecul testelor si poate opri alte taskuri inaintea executiei lor.

Checkpoint local 2026-10-25: core-ul are 3.060 de teste in 448 de suite, fara esecuri si cu doua omise; API-ul are 72 de teste in 26 de suite, fara esecuri sau omisiuni. API-ul a fost si recompilat/retestat fortat. JAR-ul core a fost construit separat, cu descriptorul expandat, API si runtime necesar, clase proprii verificate pentru Java 21, fara clase Paper/Spring incluse accidental, clase duplicate ori semnaturi invalide. `simulation.enabled` si `goal_continuity_enabled` raman `false` in configuratia impachetata.

La acest checkpoint initial, verificarea ABI era blocata de diferentele deja comise pentru `PermissionNode` si `WorldRegionInfo`: constanta `PROFILE`, respectiv descrierile story si constructorul suplimentar, cu constructorul anterior pastrat. Baseline-ul si `apiVersion=1.0.0` necesitau revizuire/versionare deliberata; checkpoint-ul nu le modifica si nu declara release-ul aprobat. Raportul local `.ai/release-reports/cognitive-preflight-2026-10-25T12-06-59Z.json` pastreaza hash-urile, rezultatele complete si limitarile. Nu a fost pornit un server, copiat pluginul sau modificata o lume. JAR-ul Paper local etichetat `26.1.2` nu a fost validat ca tinta pentru dependinta de compilare `1.21.11`.

### Revizuire ABI si verificare addon - 2026-10-25

Checkpoint-ul ulterior clasifica explicit cele doua extensii ca API minor 1.1.0, pastreaza constructorul anterior si actualizeaza deliberat baseline-ul prin task-ul dedicat. [Decizia de versionare](../reference/api-versioning-and-abi.md#revizuire-api-110---2026-10-25) descrie contractele pastrate si limitele compatibilitatii enumurilor. `projectVersion` ramane 1.0.0; nu sunt schimbate actiunile NPC-urilor sau configuratia simulatoarelor.

```powershell
./gradlew.bat :ainpc-api:check :ainpc-core-plugin:check :ainpc-scenario-medieval:check :ainpc-api:jar :ainpc-core-plugin:jar :ainpc-scenario-medieval:jar --console=plain
./scripts/release-api-addon-freeze.ps1 -ProjectRoot . -ReleaseId "<release-id>" -FailOnWarnings
```

Rezultat: 3.156 de teste in 481 de suite, fara esecuri sau erori si cu aceleasi doua teste core omise. API-ul include acum sapte teste suplimentare de consum Java/Kotlin; addonul medieval contribuie cu 17 teste. Toate cele trei task-uri `check`, construirea celor trei JAR-uri si freeze-ul static trec. Verificarea ABI ulterioara nu actualizeaza baseline-ul si raporteaza zero diferente.

Raportul local `.ai/release-reports/cognitive-api-1.1.0-2026-10-25T12-20-32Z-api-addon-freeze.json` are zero avertismente si pastreaza hash-urile API/addon si semnatura ABI; raportul preflight separat pastreaza si artefactul core si rezultatele testelor. Acest freeze inspecteaza surse si JAR-uri, nu porneste Paper si nu valideaza lifecycle-ul efectiv. Smoke-ul Minecraft, alegerea tintei compatibile si aprobarea release-ului raman separate; raportul initial nereusit nu este suprascris.

## Teste locale

Foloseste un JDK compatibil configurat prin `JAVA_HOME` sau `PATH`; scripturile nu trebuie sa presupuna o cale locala fixa.

```powershell
./gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.debug.DebugDumpOutputContractTest"
./gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.commands.AINPCCommandReachabilityTest"
./scripts/run-tests.ps1 -Module debug
./scripts/run-tests.ps1 -Module command
./scripts/run-tests.ps1 -Test "pachet.ClasaTest"
./scripts/run-tests.ps1 -List
```

Categoriile curente ale wrapperului sunt `all`, `gui`, `quest`, `progression`, `story`, `mapping`, `npc`, `command`, `debug`, `spawn`, `listener`, `economy`, `ai`, `routine`, `topology`, `utils` si `database`.

`AINPCCommandReachabilityTest` citeste toate radacinile si aliasurile declarate in `plugin.yml`, apoi extrage fiecare forma `/ainpc <comanda>` din cele 221 de documente active. Catalogul `AINPCCommandCatalog` este folosit direct de dispatcher si tab completer, iar `when`-ul exhaustiv din dispatcher obliga fiecare ruta catalogata sa aiba handler la compilare. Help-ul principal are separat un test care cere completion pentru fiecare comanda afisata, cu exceptia actiunilor ascunse intentionat cand feature flag-ul lor este oprit.

Porneste cu testul focalizat, apoi categoria afectata si abia dupa aceea cu suita mai larga.

## Smoke si dovezi

- `scripts/smoke-paper-mapping.ps1` si `scripts/smoke-paper-quests.ps1` sunt smoke-uri specializate; pastreaza si inspecteaza rapoartele lor;
- `scripts/smoke-demo-complet.ps1` este un executor legacy de pregatire/RCON si nu valideaza semantic raspunsurile;
- `scripts/test-demo.ps1` genereaza un checklist si nu dovedeste PASS-urile serverului;
- `scripts/validate-demo-paper-evidence.ps1` valideaza structura unui fisier de dovezi, nu serverul;
- `scripts/release-report.ps1` agrega artefacte si stari furnizate; nu transforma o valoare declarata in dovada reala.

Checklistul runtime consolidat este `operations/demo-server-verification.md`.

## Regula de confidentialitate

Modul implicit `standard` redacteaza secretele, dar pastreaza identificatorii necesari diagnosticului local. Modul `privacy-safe` aplica aceeasi politica la fiecare fisier text sau JSON, elimina valorile structurate de prompt/raspuns si redacteaza UUID-uri, campuri de player, nume cunoscute, emailuri, IP-uri si cai locale. Politica este best-effort: revizuieste manual fiecare artefact inainte de ticket, chat, CI sau release.

## Legaturi

- `operations/audit.md`
- `operations/observability-and-logs.md`
- `operations/demo-server-verification.md`
- `operations/server-admin-runbook.md`
- `architecture/harta-clase-debug.md`
