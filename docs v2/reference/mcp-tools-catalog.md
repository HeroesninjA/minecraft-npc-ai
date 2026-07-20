# Catalogul tool-urilor MCP AINPC

Status: referinta canonica extrasa din adnotarile `@McpTool`.
Actualizat: 2026-07-18.

Catalogul de mai jos apartine modulului Spring `ainpc-mcp-service`, nu serverului Node de context al proiectului.

## Tool-uri statice

- `ainpc.ping`;
- `ainpc.feature.state`;
- `ainpc.debug.health`, care ramane disponibil fara snapshot si adauga `snapshot.runtimeHealth` cand bridge-ul v3 este valid.

## Tool-uri bazate pe snapshot

- `ainpc.server.snapshot`;
- `ainpc.npc.list`;
- `ainpc.npc.context`;
- `ainpc.quest.summary`;
- `ainpc.dialog.context`;
- `ainpc.world.mapping.summary`.

Aceste tool-uri sunt inregistrate, iar readerul accepta schemele runtime `1..3`. Datele raman indisponibile in profilul `static`, `offline`, cand fisierul lipseste sau cand producerul si sidecar-ul nu folosesc aceeasi cale.

## Tool-uri semantice statice

- `ainpc.semantic.context`;
- `ainpc.semantic.context.summary`;
- `ainpc.quest.semantic.context`;
- `ainpc.quest.semantic.context.summary`;
- `ainpc.quest.authoring.context.summary`;
- `ainpc.mapping.semantic.context`;
- `ainpc.mapping.semantic.context.summary`;
- `ainpc.story.semantic.context`;
- `ainpc.story.semantic.context.summary`;
- `ainpc.routing.semantic.context`;
- `ainpc.routing.semantic.context.summary`;
- `ainpc.semantic.routing.summary`.

Acestea descriu contracte si routing semantic. Doua metode folosesc numele `ainpc.semantic.context`, deci registrarea trebuie consolidata inainte de a promite o semnatura unica.

## Tool-uri care scriu in command queue

- `ainpc.npc.say`;
- `ainpc.npc.setState`;
- `ainpc.broadcast`;
- `ainpc.executeCommand`;
- `ainpc.quest.progress`;
- `ainpc.quest.complete`;
- `ainpc.quest.list`.

Ele creeaza fisiere de comanda; raspunsul `queued` nu confirma procesarea sau efectul. Tool-urile sunt publicate de sidecar chiar daca pluginul nu consuma coada.

## Tool-uri ale altui serviciu

Serverul Node de context al proiectului de pe portul `3000` expune separat:

- `ainpc.build.mode.status`;
- `ainpc.build.mode.history`;
- `ainpc.build.mode.export`.

Aceste nume nu exista in modulul Spring. Nu combina cataloagele ca si cum un singur endpoint le-ar expune pe toate.

## Surse in cod

- `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/tools/`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/bridge/McpDialogContextProvider.kt`

## Legaturi

- `architecture/spring-ai-mcp-serviciu-intern.md`
- `architecture/mcp-runtime-bridge-design.md`
- `reference/mcp-runtime-gap-checklist.md`
