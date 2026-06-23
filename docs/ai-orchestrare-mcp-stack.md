# AI Orchestrare si MCP Stack

Actualizat: 2026-06-23

Acesta este punctul de intrare pentru stack-ul care leaga orchestrarea AI din core de serviciul Spring AI MCP sidecar.

## Cum se citeste

1. `ai-orchestrare-si-mecanici.md` - rolul, contractele si regulile AI din runtime.
2. `spring-ai-mcp-serviciu-intern.md` - sidecar-ul MCP, izolare si integrare.

## Ce acopera impreuna

- cum AI-ul propune, formuleaza si explica;
- cum runtime-ul valideaza si executa;
- cum se construieste contextul pentru dialog, quest, story, world si admin;
- cum sidecar-ul MCP expune tool-uri fara sa devina parte din runtime-ul Paper.

## Regula comuna

```text
AI-ul propune.
MCP-ul expune si structureaza context.
Core-ul valideaza si executa.
```

## Contracte separate

- `ai-orchestrare-si-mecanici.md` - politicile, intentiile si output-urile AI;
- `spring-ai-mcp-serviciu-intern.md` - transport, sidecar, izolare si extensie operationala.

## Relatii

- `dialog-si-conversatii.md` - consuma context AI pentru formulare;
- `generare-automata-questuri-ai.md` - consuma orchestrarea AI pentru drafturi;
- `story-si-context-ai.md` - foloseste contextul pentru story/quest selection;
- `relatii-documentatie.md` - harta oficiala a dependintelor.
