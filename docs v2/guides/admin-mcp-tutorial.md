# Admin MCP

Status: ghid operational pentru implementarea curenta, cu limite explicite.
Actualizat: 2026-07-15.

## Ce verifica ecranul

- health sincron prin `McpRuntimeClient.health()`;
- `ainpc.feature.state`;
- `ainpc.server.snapshot`;
- trei tool-uri `ainpc.build.mode.*`;
- debugdump-uri world, story, quest si mapping;
- comenzi locale pentru build mode.

## Limite curente

- endpoint-ul Spring de pe `39841` expune feature state si server snapshot, dar nu `ainpc.build.mode.*`;
- endpoint-ul Node de pe `3000` expune `ainpc.build.mode.*`, dar nu catalogul runtime Spring;
- un singur client configurat nu satisface ambele seturi;
- health-ul din render este sincron si poate astepta timeout-ul sidecar-ului;
- ecranul declara 36 sloturi, dar doua butoane sunt plasate la sloturile `38` si `39`;
- panoul nu trebuie tratat drept dovada ca bridge-ul este functional.

## Procedura sigura

1. Confirma `mcp.base_url=http://127.0.0.1:39841/mcp` pentru sidecar-ul runtime.
2. Verifica actuator health si `/ainpc debugdump mcp`.
3. Confirma profilul `local-bridge`, calea snapshot si versiunea schemei.
4. Foloseste comenzile locale `/ainpc build mode status|history|export` pentru build mode.
5. Nu activa write queue din GUI; configureaz-o si auditeaz-o separat.
6. Daca panoul arata `?`, verifica tool ownership inainte sa presupui ca datele lipsesc.

## Legaturi

- `operations/mcp-serviciu-imbunatatiri.md`
- `reference/mcp-tools-catalog.md`
- `reference/mcp-runtime-gap-checklist.md`
