# OpenCode Troubleshooting

Status: canonical in `docs v2`.
Actualizat: 2026-07-09.

Acesta este ghidul scurt pentru verificarea setup-ului OpenCode.

## Ce trebuie sa existe

- `opencode.json`;
- `opencode.jsonc`;
- MCP local;
- Serena;
- `context7`.

## Verificari rapide

- configurația incarca fisierul din repo;
- serverele MCP sunt prezente;
- drift-ul si sync-ul de config sunt curate;
- `ctx:status` raporteaza stare coerenta.

## Cand nu vede tool-urile

- reporneste sesiunea;
- verifica suprascrieri globale;
- verifica URL-urile locale;
- rezolva drift-ul si sync-ul inainte de retest.

## Legaturi

- `reference/mcp-runtime-gap-checklist.md`
- `reference/mcp-runtime-gap-checklist.md`
