# Roadmap diagnostic si observabilitate

Status: roadmap inchis.
Actualizat: 2026-07-18.

Runtime-ul are audit, sumaruri, exporturi si tracing bounded functionale. Toate imbunatatirile urmarite aici sunt implementate; directiile noi trebuie deschise separat, cu criterii verificabile.

## Audit

- [x] inventar JDBC automat al celor 28 de tabele active, catalog verificat fata de DDL si acoperire explicita pe noua domenii in raportul text/JSON;
- [x] raport JSON schema v1 cu constatari structurate, exit code `0/1/2` si wrapper RCON care propaga rezultatul;
- [x] severitati si criterii `PASS/WARN/FAIL` documentate;
- [x] modurile `strict`, `full` si `offline` cu semantica real diferita;
- [x] validare efectiva pentru wand, entitati managed si source-key;
- [x] paginare completa pentru quest anchors in profilele `full` si `offline`;
- [x] paginare keyset pentru lista detaliata a intregului spawn history, cate 200 de randuri, cu sumar structurat si detectie de scanare incompleta.

## Export si loguri

- [x] tail bounded pentru `latest.log`, maximum 250 linii dintr-o fereastra de 512 KiB;
- [x] snapshot runtime capturat pe main thread, snapshot DB intr-o tranzactie unica si log/retentie/scriere executate asincron fara blocarea tick-ului;
- [x] retentie, quota, cleanup oldest-first si manifest de confidentialitate versionat;
- [x] redaction policy unica pentru config, log, AI, MCP si DB la limita artefactului exportat;
- [x] teste adversariale pentru secrete si date personale pe fisierele finale text/JSON;
- [x] optiune `privacy-safe` fara valori structurate prompt/response si cu identificatorii de player redactati.

## Evenimente si metrici

- [x] `RecentEventsBuffer` conectat la catalogul explicit al celor 33 de evenimente API concrete, cu test de acoperire si capacitate bounded;
- [x] extractul `story_events` redenumit si separat de bufferul evenimentelor API in artefacte distincte;
- [x] health snapshot machine-readable in snapshot-ul runtime v3, `health.json` si `ainpc.debug.health`;
- [x] metrici bounded pentru tick, DB, comenzi, schedulere, AI si export, cu label-uri fixe si bugete declarate;
- [x] anomaly tracing numai pentru esecuri sau depasiri de buget, cu nume normalizate din seriile acceptate, buffer global bounded si export `traces.json` fara payload-uri de domeniu.

## Testare

- [x] teste de reachability pentru toate modurile si profilele documentate ale `/ainpc audit`;
- [x] reachability pentru toate radacinile si aliasurile din `plugin.yml`, toate comenzile top-level `/ainpc` din documentatia activa si fiecare comanda afisata in help/tab completion, prin catalogul folosit efectiv de dispatcher;
- [x] verificari semantice care resping raspunsuri RCON invalide, inclusiv drift mode/profile, plan, totaluri, severitati, verdict si payload-uri DB/spawn;
- [x] fixture-uri mici/medii/mari pentru audit la limita de retentie per sectiune si pentru export text/JSON sub, exact la si peste plafonul per artefact;
- [x] teste de retentie, quota, cleanup si protectie a exportului curent;
- [x] test de confidentialitate pe artefactul final.

## Prioritate propusa

1. [x] redaction comuna si mod `privacy-safe`;
2. [x] tail bounded si limita de 4 MiB per artefact, cu marker text/envelope JSON si raportare in index;
3. [x] reachability plus semantica reala pentru audit;
4. [x] retentie si cleanup;
5. [x] health/metrici cu bugete explicite si cardinalitate controlata.
6. [x] evenimente API recente bounded si extract story separat semantic.
7. [x] raport audit JSON versionat si exit code automatizabil prin RCON.
8. [x] inventar automat al schemei si acoperire functionala explicita pe domenii.
9. [x] lista detaliata paginata a intregului spawn history pentru `all full/offline`.
10. [x] anomaly tracing bounded pentru esecuri si depasiri de buget, fara payload-uri de domeniu.

## Legaturi

- `operations/audit.md`
- `operations/observability-and-logs.md`
- `architecture/harta-clase-debug.md`
- `planning/performance-hardening.md`
