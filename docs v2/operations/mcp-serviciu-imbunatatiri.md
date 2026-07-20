# Operare si diagnostic MCP runtime

Status: runbook operational; backlog-ul este in checklist-ul dedicat.
Actualizat: 2026-07-18.

## Identifica serviciul

| Endpoint implicit | Proces | Scop |
|---|---|---|
| `127.0.0.1:39841` | `ainpc-mcp-service` Spring | bridge runtime Paper |
| `127.0.0.1:3000` | server Node local | context proiect, Chroma, memory |
| `127.0.0.1:9121` | Serena | navigare simbolica |

Nu schimba endpoint-ul pluginului la `3000` doar pentru ca acel server raspunde MCP: clientul Paper asteapta health la `/actuator/health` si tool-urile Spring runtime.

## Ordine de diagnostic

1. Verifica procesul Spring la `http://127.0.0.1:39841/actuator/health`.
2. Ruleaza `/ainpc debugdump mcp`.
3. Confirma profilul Spring activ; `static` nu activeaza snapshot-ul.
4. Confirma ca producerul si readerul folosesc aceeasi cale absoluta.
5. Verifica `schemaVersion`; producerul curent emite `3`, iar readerul accepta `1..3`.
6. Apeleaza `ainpc.debug.health`, apoi un tool bazat pe snapshot.
7. Verifica separat command queue numai daca write tools sunt intentionat activate.

## Validare locala

- `.\gradlew.bat :ainpc-mcp-service:test`;
- `powershell -ExecutionPolicy Bypass -File .\scripts\smoke-mcp-service.ps1`;
- inspecteaza `data/mcp-runtime-snapshot.json` fara a copia continut sensibil in raport;
- verifica fisierele de audit si command queue in directoarele configurate, nu in directoare presupuse.

Contractele sidecar pentru reader si `ainpc.debug.health` includ fixture v3 cu health bounded. Smoke-ul end-to-end cu fisier produs de Paper ramane separat in `reference/mcp-runtime-gap-checklist.md`.

## Interpretarea health-ului

- actuator `UP` confirma procesul Spring, nu compatibilitatea snapshot-ului;
- `fresh`, `cached`, `stale`, `missing`, `invalid` si `offline` sunt stari diferite;
- un raspuns MCP reusit poate contine `available=false`;
- `snapshot.runtimeHealth` exista numai cand snapshot-ul disponibil contine schema health; lipsa lui pe v1/v2 nu inseamna ca sidecar-ul este oprit;
- un fisier de comanda creat inseamna `queued`, nu `executed`.

## Regula de siguranta

- pastreaza bind-ul local;
- nu activa consumul write queue fara allowlist si control de acces;
- nu expune secrete sau payload-uri complete in audit;
- nu declara bridge-ul functional pana nu trece un smoke end-to-end cu snapshot produs de plugin.

## Legaturi

- `architecture/mcp-runtime-bridge-design.md`
- `reference/mcp-runtime-gap-checklist.md`
- `reference/mcp-tools-catalog.md`
