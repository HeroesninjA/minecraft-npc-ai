# MCP runtime bridge design

Status: contract canonic pentru transportul read-only dintre Paper si MCP.
Actualizat: 2026-07-14.

Bridge-ul publica starea runtime printr-un snapshot JSON redactat si inspectabil.

## Contract

- pluginul Paper produce snapshot-ul;
- MCP service il citeste la cerere;
- comunicarea ramane read-only si fara socket suplimentar;
- cache-ul MCP foloseste un TTL scurt;
- scrierea si citirea nu blocheaza main thread-ul Paper.

## Validare

- datele sensibile sunt redactate;
- un snapshot expirat nu este raportat healthy;
- erorile de schema si citire sunt vizibile in health;
- tool-urile nu transforma snapshot-ul in stare executabila.

## Limita

- acest document defineste transportul si prospetimea datelor;
- politica AI apartine orchestration-ului;
- lista tool-urilor apartine catalogului MCP.

## Legaturi

- `architecture/spring-ai-mcp-serviciu-intern.md`
- `reference/mcp-runtime-gap-checklist.md`
- `operations/mcp-serviciu-imbunatatiri.md`
