# MCP Runtime Hardening Checklist

Status: singurul backlog activ pentru bridge-ul MCP runtime.
Actualizat: 2026-07-15.

## P0 - blocaje

- [x] Aliniaza producerul si readerul la aceeasi `schemaVersion`.
- [x] Actualizeaza modelul Java si fixture-urile pentru toate campurile snapshot pastrate.
- [ ] Ruleaza un test end-to-end cu snapshot generat de `RuntimeSnapshotProducer`.
- [x] Configureaza cai absolute comune pentru snapshot si command queue.
- [x] Alege un owner pentru `ainpc.build.mode.*`; GUI-ul nu trebuie sa presupuna un catalog combinat Spring + Node.
- [ ] Publica write tools numai dupa gate explicit, autentificare si allowlist.
- [ ] Separa health-ul procesului de health-ul bridge-ului in smoke si UI.

## P1 - corectitudine

- [x] Repara baseline-ul de teste: `SnapshotReaderTest.reportsExpiredCacheWhenTtlIsZero` si `AinpcDebugHealthToolsTest.debugHealthReturnsReadOnlySidecarSnapshot` esueaza in rularea curenta.
- [x] Pastreaza starea `STALE` dupa redactare, fara conversie silentioasa la `CACHED`.
- [x] Fa `ainpc.feature.state` sa raporteze modul, bridge-ul si write tools reale.
- [x] Elimina numele duplicat `ainpc.semantic.context` sau defineste o singura semnatura.
- [x] Verifica faptul ca redactorul pastreaza campurile non-sensibile v2 si mascheaza sursele sensibile.
- [ ] Corecteaza provenance-ul providerului si conecteaza validarea in fluxurile AI care folosesc MCP.
- [x] Muta health/tool calls lente din render-ul GUI in executie asincrona cu cache.
- [x] Verifica dimensiunea si sloturile `AdminMcpGui`; ecranul de 36 sloturi foloseste in cod sloturile `38` si `39`.

## Gate de inchidere

- [ ] Testele unitare trec pentru plugin si sidecar.
- [ ] Smoke-ul porneste ambele procese din workdir-uri diferite si foloseste cai explicite.
- [ ] Snapshot fresh, stale, invalid, missing si offline au rezultate distincte.
- [ ] Tool-urile read-only nu produc fisiere de comanda.
- [ ] Tool-urile de scriere neautorizate nu sunt publicate sau executate.
- [ ] `/ainpc debugdump mcp` si GUI-ul raporteaza acelasi endpoint si aceeasi stare.
- [ ] Documentele canonice sunt actualizate dupa cod, nu inainte.

## Regula

Prezenta claselor, fixture-urilor sau a unui health endpoint nu marcheaza o faza ca `done`. Gate-ul se inchide numai cu integrare end-to-end verificata.

## Baseline verificat

La 2026-07-16, toate cele 3 module (`ainpc-core-plugin`, `ainpc-mcp-service`, `ainpc-scenario-medieval`) compileaza si ruleaza testele fara erori (1170+ teste). MCP service: 52/52 passing. Plugin core: toate testele passing.

## Legaturi

- `architecture/mcp-runtime-bridge-design.md`
- `architecture/spring-ai-mcp-serviciu-intern.md`
- `operations/mcp-serviciu-imbunatatiri.md`
