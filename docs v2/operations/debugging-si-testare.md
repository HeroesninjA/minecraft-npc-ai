# Debugging si testare

Status: runbook operational canonic.
Actualizat: 2026-07-18.

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
/ainpc debugdump all [player] [privacy-safe]
/ainpc debugdump npc [player] [privacy-safe]
```

Comanda captureaza mai intai starea runtime pe main thread, confirma ca exportul continua asincron si livreaza rezultatul ulterior tot pe main thread. SQL-ul ruleaza intr-o singura tranzactie pe worker, iar tail-ul, cleanup-ul si scrierea nu blocheaza tick-ul. Numai un export poate fi activ; mesajul `deja in curs` cere asteptarea rezultatului cererii precedente.

Folderul rezultat este `plugins/AINPC/debug-dumps/debug-dump-<timestamp>/`; coliziunile din aceeasi secunda primesc sufix numeric. Filtrul optional de player restrange numai exporturile care il folosesc; nu trebuie interpretat ca izolarea tuturor datelor unui singur jucator. Optiunea `privacy-safe` poate fi pusa inainte sau dupa filtru si este recomandata pentru orice artefact care paraseste serverul. `index.txt` declara fazele snapshot, limitele active, cleanup-ul si artefactele trunchiate, iar `manifest.json` declara schema de confidentialitate, metadata snapshot si inventarul payload-urilor. Retentia automata elimina oldest-first numai directoarele managed si protejeaza exportul curent; inspecteaza ambele fisiere inainte sa distribui dump-ul sau sa interpretezi absenta unor randuri drept absenta starii runtime.

Citeste `operations/observability-and-logs.md` inainte sa distribui un dump.

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
