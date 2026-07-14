# MCP Service Improvement Plan

Status: canonical in `docs v2`.
Actualizat: 2026-06-28.

Acesta este planul de imbunatatire pentru MCP sidecar.

## Starea actuala

- tool-urile sunt in mare parte statice sau schema-only;
- pluginul are client si fallback, dar nu citeste inca date reale din runtime Paper;
- bridge-ul runtime trebuie inchis prin snapshot read-only.

## Pasii principali

- snapshot file periodic;
- tool-uri reale read-only;
- redactare si audit;
- health indicator real;
- consolidare tool-uri;
- profiles si smoke;
- teste;
- documentatie si smoke script.

## Regula

- nu adauga serviciu nou pana nu sunt inchise gap-urile;
- pastreaza backward compat prin aliasuri.

## Legaturi

- `reference/mcp-runtime-gap-checklist.md`
- `reference/mcp-tools-catalog.md`
- `architecture/spring-ai-mcp-serviciu-intern.md`
