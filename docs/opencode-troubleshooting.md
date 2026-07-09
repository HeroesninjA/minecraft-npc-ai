# OpenCode Troubleshooting

Actualizat: 2026-07-09

Acest ghid scurt acopera verificarea setup-ului OpenCode pentru proiectul curent.

## Ce trebuie sa existe

- `opencode.json` in root-ul proiectului.
- `opencode.jsonc` ca varianta comentata/editabila.
- MCP local activ la `http://127.0.0.1:3000/mcp`.
- Serena activa la `http://127.0.0.1:9121/mcp`.
- `context7` configurat ca server remote pentru documentatie.

## Verificari rapide

1. Deschide proiectul din root-ul repo-ului.
2. Confirma ca OpenCode incarca `opencode.json`, nu alt config global.
3. Verifica daca `opencode.json` include `ainpc-project-memory`, `serena` si `context7`.
4. Ruleaza `npm run ctx:status -- "C:\Users\HeroesninjA\IdeaProjects\test"` din serverul MCP local.
5. Verifica `ctx:status` pentru:
   - `mcp.health`
   - `serena.ok`
   - `codexConfig`
   - `sessionSyncWatcher`
6. Ruleaza `.\scripts\check-opencode-config-drift.ps1 -ProjectRoot "."` si verifica raportul din `build/opencode-config-drift/`.
7. Ruleaza `.\scripts\check-opencode-config-sync.ps1 -ProjectRoot "."` si verifica raportul din `build/opencode-config-sync/`.
8. Pentru o verificare completa, ruleaza `.\scripts\check-opencode-config.ps1 -ProjectRoot "."`.

## Daca OpenCode nu vede tool-urile MCP

- Repornește sesiunea OpenCode dupa modificarea config-ului.
- Verifica sa nu existe un `opencode.json` global care suprascrie config-ul din repo.
- Verifica daca URL-urile locale raspund in browser sau prin dashboard-ul MCP.
- Daca `context7` nu apare, confirma ca reteaua nu blocheaza accesul la `https://mcp.context7.com/mcp`.
- Daca scriptul de drift raporteaza mismatch, aliniaza `opencode.json` cu `.codex/config.toml` pentru serverele comune.
- Daca scriptul de sync raporteaza mismatch, actualizeaza `opencode.jsonc` si regenereaza `opencode.json`.
- Daca verificarea completa esueaza, rezolva mai intai drift-ul si sync-ul, apoi ruleaza din nou wrapper-ul.

## Legaturi utile

- `README.md`
- `docs/README.md`
- `docs/mcp-docker-server-mvp-si-faze.md`
- `docs/ai-orchestrare-mcp-stack.md`
