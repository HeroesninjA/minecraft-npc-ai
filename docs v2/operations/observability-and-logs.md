# Observabilitate si loguri

Status: referinta operationala canonica.
Actualizat: 2026-07-18.

Runtime-ul are loguri, audituri, sumaruri admin, exporturi de diagnostic, un registru bounded de metrici cu health `PASS/WARN/FAIL` si un buffer bounded pentru evenimentele API AINPC. Nu are tracing, persistenta de serii temporale sau endpoint Prometheus.

## Suprafete curente

| Suprafata | Locatie | Rol |
|---|---|---|
| log Paper | `logs/latest.log` si consola | startup, shutdown, warnings, stacktrace si mesaje ale serviciilor |
| audit admin | `/ainpc audit <mod>` | verificari read-only in chat/consola |
| audit machine-readable | `/ainpc audit <mod> [profil] json` si `scripts/ainpc-audit-rcon.ps1` | schema v1, constatari structurate, inventar DB, sumar spawn history paginat pentru profile complete si coduri `PASS=0`, `WARN=1`, `FAIL=2` |
| sumar debug | `/ainpc debugdump <mod>` | inspectie punctuala in chat/consola |
| export debug | `/ainpc debugdump all [player] [privacy-safe]` sau `npc [player] [privacy-safe]` | folder cu fisiere pentru diagnostic offline |
| health runtime | `/ainpc health`, `health.json`, snapshot runtime v3 si `ainpc.debug.health` | stare machine-readable si rezumat operator al bugetelor runtime |
| anomaly tracing | `/ainpc health` si `traces.json` din export | numai esecuri sau operatii care ating pragurile warn/fail, fara payload-uri de domeniu |
| evenimente API recente | `recent-api-events.txt` din export | buffer in-memory cu metadata minima pentru evenimentele Bukkit publice AINPC |
| story events recente | `recent-story-events.txt` din export | ultimele 50 de randuri citite din tabela `story_events` |

Modurile `world`, `regions`, `places`, `nodes`, `npcbound`, `mapping`, `routing`, `quest`, `questconfig`, `story`, `authoring`, `ai`, `runtime`, `mcp`, `features`, `scenario` si `progression` nu creeaza folder; ele afiseaza sumaruri.

## Exportul in fisiere

Folderul final este creat de worker sub:

```text
plugins/AINPC/debug-dumps/debug-dump-yyyyMMdd-HHmmss/
```

Fisierele comune includ `summary.txt`, `server.txt`, `config-sanitized.yml`, `audit.txt`, `health.json`, `traces.json`, `recent-server-log.txt`, `recent-api-events.txt`, `recent-story-events.txt`, `manifest.json` si `index.txt`. Scope-ul `all` adauga snapshot-uri NPC, mapping, household, spawn, quest, story, authoring si OpenAI; scope-ul `npc` pastreaza subsetul NPC.

Exportul are trei faze explicite:

1. pe main thread sunt capturate valorile Paper si starea runtime necesara, fara interogari SQL, tail de log, cleanup sau scriere de fisiere;
2. un task Paper async citeste fisierele auxiliare, construieste toate artefactele DB intr-o singura tranzactie `DatabaseManager`, ingheata setul de artefacte, apoi executa tail-ul, retentia si scrierea;
3. rezultatul sau eroarea este trimisa expeditorului printr-un callback programat inapoi pe main thread.

Comanda confirma imediat capturarea runtime si nu asteapta I/O. Un gate permite un singur export activ; o a doua cerere primeste mesajul `deja in curs`, evitand curse intre cleanup, quota si directoarele de export. Tranzactia DB ofera o vedere coerenta intre artefactele persistente, dar poate tine lock-ul DB pe durata construirii lor, deci exporturile mari raman operatii administrative deliberate.

Exporterul are doua moduri:

- `standard`, implicit, redacteaza secretele dar pastreaza datele operationale necesare investigatiei locale;
- `privacy-safe` elimina valorile structurate de prompt/raspuns si redacteaza UUID-uri, campuri de player, numele filtrului si ale playerilor online, emailuri, IP-uri si cai locale.

`SensitiveDataRedactor` este politica unica pentru config, log, snapshot AI/MCP si continut DB. `DebugDumpIO` o aplica dupa serializare, la limita finala a fiecarui fisier text sau JSON; testul `DebugDumpPrivacyContractTest` verifica artefactul scris si validitatea JSON.

Limitele curente sunt deterministe:

- `latest.log`: ultimele maximum 250 de linii dintr-o fereastra de maximum 512 KiB citita direct de la finalul fisierului;
- fiecare artefact: maximum 4 MiB dupa redaction;
- textul depasit pastreaza un prefix UTF-8 valid si un marker `AINPC export truncated`;
- JSON-ul depasit devine un envelope JSON valid cu `truncated`, `reason`, `original_bytes` si `max_bytes`;
- `index.txt` declara limitele si fisierele trunchiate.

Aceste plafoane nu elimina costul construirii snapshot-urilor in memorie.

## Health si metrici bounded

`PerformanceMonitor` retine implicit maximum 32 de serii si 100 de observatii recente per serie. Numele sunt definite de runtime; argumentele comenzilor, numele playerilor, ID-urile NPC si alte valori cu cardinalitate necontrolata nu devin label-uri.

Producatorii conectati sunt:

- `tick.routine` pentru tick-ul de rutina;
- `database.operation` pentru tranzactii, update-uri directe si executiile prepared statement;
- `command.dispatch` pentru dispatch-ul comenzilor AINPC;
- `scheduler.*` pentru task-urile recurente cu nume fixe;
- `ai.orchestration` pentru orchestration si fallback;
- `export.debug_dump` si `export.runtime_snapshot` pentru exporturi.

Bugetele implicite sunt:

```yaml
observability:
  metrics:
    max_series: 32
    samples_per_series: 100
    budgets:
      tick: { warn_ms: 20, fail_ms: 50 }
      database: { warn_ms: 50, fail_ms: 250 }
      command: { warn_ms: 100, fail_ms: 1000 }
      scheduler: { warn_ms: 50, fail_ms: 250 }
      ai: { warn_ms: 3000, fail_ms: 15000 }
      export: { warn_ms: 2000, fail_ms: 10000 }
  tracing:
    enabled: true
    max_spans: 64
```

Pragurile clasifica semnalul si nu intrerup operatia. Ultima eroare produce `FAIL`; o eroare mai veche ramasa in fereastra produce `WARN`. `droppedMeasurements` creste cand plafonul de serii respinge o serie noua. Schimbarea configuratiei prin reload reaplica bugetele si taie ferestrele la noua limita.

Tracing-ul nu captureaza fiecare apel. `PerformanceMonitor` adauga un span numai cand operatia esueaza sau durata atinge pragul `warn_ms`/`fail_ms` al domeniului. Numele metricii este deja normalizat si acceptat de plafonul `max_series`; span-ul contine numai secventa, timestamp-ul de final, domeniul, trigger-ul, durata, numarul de elemente, succesul si bugetele aplicate. Nu sunt copiate comenzi, SQL, prompturi, UUID-uri ori alte payload-uri de domeniu.

`traces.json` are schema interna `1`, ordine newest-first si maximum `max_spans` intrari globale; configuratia este limitata la `8..256`, implicit `64`. `droppedSpans` numara intrarile eliminate oldest-first. Dezactivarea prin reload goleste bufferul, iar reducerea plafonului il taie imediat.

Snapshot-ul runtime are `schemaVersion=3` si include obiectul `health` cu schema interna `1`. `ainpc.debug.health` combina health-ul sidecar-ului cu acest obiect cand snapshot-ul este disponibil. `health.json` si `traces.json` sunt capturate inainte ca exportul curent sa-si inregistreze durata; observatia exportului apare in urmatorul snapshot sau dump.

## Evenimente API si story separate

`RecentPublicEventListener` inregistreaza explicit, la prioritatea Bukkit `MONITOR`, cele 33 de clase concrete din `ainpc-api/src/main/kotlin/ro/ainpc/api/events/`. Listenerul nu modifica evenimentul si trimite in `RecentEventsBuffer` numai timestamp-ul, numele clasei, starea async si starea cancellable observata; payload-urile, UUID-urile de player, textele si obiectele de domeniu nu sunt copiate.

Catalogul este intentionat explicit. `RecentPublicEventListenerTest` compara lista runtime cu toate declaratiile concrete `*Event` din modulul API, astfel incat un eveniment public nou obliga actualizarea capturii diagnostice.

Bufferul este numai in memorie, newest-first la citire, si se goleste la restart. Capacitatea se reaplica si la reload:

```yaml
events:
  public_api_enabled: true
  debug_recent_event_buffer: 100 # clamp 10..1000
```

Exportul nu mai amesteca cele doua surse:

- `recent-api-events.txt` contine bufferul runtime al evenimentelor Bukkit API;
- `recent-story-events.txt` este construit de `DebugDumpStoryEventsText` exclusiv din tabela persistenta `story_events`, maximum 50 de randuri.

## Retentie si quota

La fiecare export, cleanup-ul ruleaza inainte si dupa scriere cu valorile:

```yaml
debug:
  dumps:
    retention_days: 14
    max_exports: 20
    max_total_mib: 512
```

Ordinea este determinista: vechime, numar maxim, apoi quota cumulata. Pentru vechime si ordonare se foloseste `lastModified`; la egalitate se foloseste numele directorului.

- sunt eligibile numai directoarele cu forma exacta `debug-dump-yyyyMMdd-HHmmss` sau varianta cu sufix numeric de coliziune;
- `quest-saves`, `quest-drafts`, fisierele izolate, directoarele cu nume manual si symlink-urile nu sunt sterse si nu intra in quota;
- directoarele sunt sterse oldest-first, fara urmarirea symlink-urilor din interior;
- radacina trebuie sa fie un director real, nu fisier sau symlink;
- dump-ul tocmai creat este protejat; daca el singur depaseste quota, este pastrat, comanda afiseaza warning si limita ramane nesatisfacuta;
- cleanup-ul ruleaza numai cand se creeaza un nou export, nu prin scheduler;
- doua exporturi din aceeasi secunda primesc directoare distincte, al doilea folosind sufixul `-01`.

`manifest.json` are `schema_version=1`, clasificarea de confidentialitate, modul de redaction, obligatia de review manual, limitele de retentie, rezultatul cleanup-ului anterior si inventarul payload-urilor cu dimensiuni/truncare. Sectiunea `snapshot` declara timestamp-urile capturii runtime si finalizarii setului, thread-urile fazelor, folosirea tranzactiei DB si faptul ca setul a fost inghetat inainte de scriere. Manifestul nu include valoarea filtrului de player sau cai absolute; inventarul sau acopera payload-urile scrise inainte de `manifest.json` si `index.txt`.

## Limite si confidentialitate

- modul `standard` nu anonimizeaza nume, UUID-uri, cai sau continut operational;
- modul `privacy-safe` este best-effort: un nume offline necunoscut, numele unui NPC sau text liber fara eticheta structurata poate ramane;
- `/ainpc debugdump ai` poate afisa preview-uri de prompt, raspuns, erori si fallback-uri;
- `/ainpc debugdump mcp` afiseaza endpoint-ul si fragmente din raspunsurile tool-urilor;
- `recent-api-events.txt` este volatil, nu contine payload-uri si nu poate reconstrui ordinea tranzactionala sau starea de domeniu dupa restart;
- `recent-story-events.txt` este un extract bounded al tabelei `story_events`, nu un bus al evenimentelor Bukkit;
- tail-ul logului este bounded la nivel de I/O, dar poate incepe dupa o linie partiala daca o singura linie depaseste fereastra de 512 KiB;
- capturarea runtime ramane lucru pe main thread si poate costa CPU pentru volume mari de NPC/mapping, dar SQL, citirea logului, retentia si scrierea nu blocheaza tick-ul;
- tranzactia snapshot serializeaza temporar accesul prin `DatabaseManager`; nu porni exporturi repetate in timpul unui incident de latenta DB.

Revizuieste manual continutul inainte de distribuire si nu incarca dump-uri neinspectate in issue tracker, chat sau servicii externe.

## Diagnostic OpenAI

Configul livrat are:

```yaml
openai:
  diagnostics:
    enabled: true
    check_on_startup: false
    log_prompt_summary: true
    log_response_preview: true
```

Pe servere cu date reale, dezactiveaza preview-urile daca nu exista un caz controlat de diagnostic. Schimbarea reduce expunerea, dar nu elimina obligatia de a inspecta logurile si dump-urile.

## Ce nu este implementat

- endpoint Prometheus, persistenta de serii temporale sau percentile p50/p95/p99;
- tracing distribuit ori OpenTelemetry;
- verificare automata a tuturor loggerelor runtime din afara exportului.

Acestea sunt idei compatibile active, nu abandonate, in `planning/diagnostic-si-observabilitate-roadmap.md`.

## Legaturi

- `operations/audit.md`
- `operations/debugging-si-testare.md`
- `operations/server-credentials.md`
- `architecture/harta-clase-debug.md`
