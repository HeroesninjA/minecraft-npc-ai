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
