# Harta claselor pentru debug

Status: harta arhitecturala derivata.
Actualizat: 2026-07-18.

Aceasta pagina explica ownership-ul de cod. Procedurile operatorului sunt in `operations/audit.md`, `operations/debugging-si-testare.md` si `operations/observability-and-logs.md`.

## Rutare

- `AINPCCommand.handleAudit` construieste `AuditReport` si ruleaza sectiunile read-only;
- `AINPCCommand.handleDebugDump` ruteaza modurile de sumar catre handler-ele domeniilor;
- `AINPCCommand.handleDebugDumpExport` parseaza `DebugDumpExportOptions` si ruteaza `all` si `npc` catre exporterul de fisiere;
- `AINPCTabCompleter` publica optiunile principale, dar help-ul runtime ramane sursa operationala mai directa.

## Export

- `DebugDumpService` creeaza un folder timestamped unic, selecteaza fisierele dupa scope si aplica cleanup inainte si dupa export;
- `DebugDumpRetention` limiteaza numai directoarele gestionate dupa varsta, numar si bytes, fara a urma symlink-uri si fara a sterge exportul curent;
- `DebugDumpManifest` construieste manifestul versionat cu clasificarea de confidentialitate, inventarul artefactelor si rezultatul cleanup-ului initial;
- clasele `DebugDump*Json` si `DebugDump*Text` construiesc snapshot-uri pe domenii;
- `SensitiveDataRedactor` detine politica comuna pentru secrete si modul `privacy-safe`;
- `DebugDumpSecrets` este adaptorul compatibil pentru apelantii care cer numai redaction de secrete;
- `DebugDumpIO` citeste numai coada bounded a logului si aplica politica activa plus plafonul de bytes la limita finala de scriere pentru text si JSON;
- `DebugDumpLogTail` raporteaza bytes cititi, limitele si tipul trunchierii, iar `DebugDumpWriteResult` raporteaza marimea fiecarui artefact;
- `DebugDumpService.DebugDumpResult` returneaza folderul, scope-ul normalizat, modul de confidentialitate si rezultatul final al retentiei.

Exporterul este apelat sincron din comanda admin. Scope-urile de fisier active prin comanda sunt `all` si `npc`; ambele accepta un filtru optional de player si optiunea `privacy-safe`. Celelalte cuvinte `debugdump` reprezinta sumaruri in chat/consola.

## Metrici si health

- `PerformanceMonitor` este registrul bounded comun si construieste `RuntimeHealthSnapshot` cu stari `PASS/WARN/FAIL`;
- acelasi monitor retine newest-first numai span-urile anormale intr-un buffer global bounded si construieste `RuntimeTraceSnapshot` schema v1 pentru `traces.json`;
- `RuntimeMetricNames` detine seriile fixe pentru tick, DB, comenzi, AI si export; `SchedulerCoordinator` adauga numai nume de task definite in cod;
- `DatabaseManager` masoara executiile SQL prin proxy-ul prepared statement si caile directe de tranzactie/update;
- `RuntimeSnapshotProducer` publica health-ul in schema runtime v3, iar `DebugDumpService` scrie aceeasi proiectie in `health.json`;
- sidecar-ul Java oglindeste schema si `ainpc.debug.health` include obiectul runtime cand fisierul este valid.

## Evenimente si AI

- `RecentPublicEventListener` inregistreaza explicit la `MONITOR` catalogul celor 33 de evenimente concrete din API si nu modifica payload-ul;
- `RecentEventsBuffer` retine bounded numai timestamp, nume de clasa, async si starea cancellable observata; capacitatea configurabila este limitata la `10..1000` si se reaplica la reload;
- `DebugDumpStoryEventsText` citeste separat maximum 50 de randuri din `story_events`;
- `DebugDumpService` scrie `recent-api-events.txt` si `recent-story-events.txt` ca artefacte distincte semantic;
- `OpenAIDebugSnapshot` pastreaza stare recenta pentru diagnostic;
- sumarurile `ai` si `mcp` pot expune continut operational si necesita review manual.

## Clase adiacente

- `WorldMappingSemanticIndex` ajuta sumarurile de mapping/routing;
- `MappingWandService` apartine mapping-ului, chiar daca apare in suprafata de audit;
- `AuditReport` retine constatari structurate `INFO/WARN/ERROR` pe sectiuni si pastreaza compatibil rendererul text colorat;
- `DatabaseSchemaCatalog` mapeaza cele 28 de tabele active pe noua domenii, iar `DatabaseSchemaInventory` descopera schema reala prin metadata JDBC si agrega acoperirea plus row counts;
- `DatabaseSchemaAudit` ordoneaza driftul inaintea detaliilor bounded si ataseaza inventarul structurat la raport;
- `SpawnBatchHistoryPager` citeste keyset istoricul persistent in ordinea stabila `started_at DESC, batch_key ASC`, fara `OFFSET` si cu maximum 500 de randuri per pagina;
- `SpawnHistoryAudit` agrega statusurile, listeaza fiecare batch cu campuri text bounded si ataseaza sumarul complet/partial la raport dupa ce verificarile `npc_family` au avut prioritate;
- `AuditReportJson` construieste documentul compact schema v1, cu numere exacte, truncare explicita, planul de executie, inventarul DB, sumarul spawn history optional si `exit_code` derivat din verdict;
- `AuditVerdict` detine codurile stabile `PASS=0`, `WARN=1`, `FAIL=2`, iar `scripts/ainpc-audit-rcon.ps1` le propaga drept cod de proces dupa validarea raspunsului.

## Limite

- cleanup-ul de retentie ruleaza numai cand incepe un export nou, nu printr-un scheduler periodic;
- registrul nu persista istoric si nu expune Prometheus sau tracing;
- bufferul evenimentelor API este volatil si nu pastreaza payload-urile de domeniu;
- JSON-ul audit este bounded la 100 de constatari retinute per sectiune si nu este scris automat in fisier;
- inventarul DB verifica prezenta si numarul de randuri, nu forma coloanelor, indexurile sau constrangerile;
- paginile spawn history nu sunt incadrate intr-un snapshot tranzactional comun; sumarul marcheaza scanarea incompleta daca totalul agregat difera de numarul parcurs;
- redaction-ul comun este best-effort si nu garanteaza inferarea tuturor numelor sau textelor libere sensibile;
- snapshot-urile de domeniu si serializarea raman sincrone chiar daca tail-ul logului este bounded;
- prezenta unei clase de debug nu demonstreaza ca are apelant runtime.

Lucrul compatibil ramas este in `planning/diagnostic-si-observabilitate-roadmap.md`.

## Legaturi

- `reference/harta-clase-cod.md`
- `operations/audit.md`
- `operations/observability-and-logs.md`
- `planning/diagnostic-si-observabilitate-roadmap.md`
