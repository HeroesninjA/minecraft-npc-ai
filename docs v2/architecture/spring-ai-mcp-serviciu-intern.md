# Spring AI MCP ca serviciu intern

Status: contract canonic pentru limita sidecar-ului MCP.
Actualizat: 2026-07-14.

Sidecar-ul local expune context si tool-uri validate fara sa preia autoritatea runtime-ului Paper.

## Responsabilitate

- ruleaza separat de procesul Paper;
- expune tool-uri MCP si context redactat;
- orchestreaza integrarea cu providerii AI;
- aplica validari de transport, schema si acces;
- returneaza rezultate inspectabile catre consumatori.

## Limite

- nu contine logica de gameplay;
- nu modifica direct lumea sau baza de date a pluginului;
- nu decide progresul, reward-urile sau persistenta;
- foloseste bridge-ul runtime pentru starea expusa de Paper.

## Legaturi

- `architecture/ai-orchestrare-si-mecanici.md`
- `architecture/mcp-runtime-bridge-design.md`
- `reference/mcp-tools-catalog.md`
