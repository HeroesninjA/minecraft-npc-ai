# MCP Docker Server MVP si Faze

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Planul de baza pentru serverul MCP in Docker si fazele ulterioare.

## Ce acopera

- server MCP real pe `http://127.0.0.1:3000/mcp`;
- sidecar Serena pe `http://127.0.0.1:9121/mcp`;
- persistenta in `data/`;
- indexare si Chroma in parity;
- config Codex, JetBrains si fallback reparabil;
- backup, restore, health, autostart si watcher.

## Criteriul minim

- serviciile ruleaza fara blocaje;
- `/health` si `/mcp` raspund corect;
- snapshot si audit exista;
- contextul de proiect poate fi citit din MCP;
- repair-flow-ul este documentat si repetabil.

## Ordine recomandata

- inchide base stack-ul Docker;
- valideaza config-urile clientilor;
- adauga observabilitate si backup;
- confirma watcher-ul si restore-ul;
- stabilizeaza repair flow-ul.

## Legaturi

- `architecture/spring-ai-mcp-serviciu-intern.md`
- `operations/server-admin-runbook.md`
- `operations/mcp-serviciu-imbunatatiri.md`
