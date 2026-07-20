# AI si MCP: ordine de citire

Status: index derivat.
Actualizat: 2026-07-15.

## Ordine

1. `architecture/ai-orchestrare-si-mecanici.md` - scaffold-ul AI si limitele lui.
2. `reference/prompt-safety-guide.md` - gate-ul operational pentru prompt si output.
3. `architecture/spring-ai-mcp-serviciu-intern.md` - sidecar-ul runtime Spring.
4. `architecture/mcp-runtime-bridge-design.md` - snapshot, cache si command queue.
5. `reference/mcp-tools-catalog.md` - catalogul exact de tool-uri.
6. `reference/mcp-runtime-gap-checklist.md` - singurul backlog activ pentru hardening.

## Separare obligatorie

- dialogul OpenAI activ nu trece prin `AIOrchestrationService`;
- sidecar-ul Spring nu orchestreaza modele;
- serverul Node de context al proiectului nu este sidecar-ul runtime;
- acest index nu redefineste contractele sursa.
