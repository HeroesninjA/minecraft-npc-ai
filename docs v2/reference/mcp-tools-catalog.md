# MCP Tools Catalog

Status: canonical in `docs v2`.
Actualizat: 2026-06-28.

Acesta este catalogul actual si propus pentru tool-urile MCP.

## Actual

- `ainpc.ping`;
- `ainpc.feature.state`;
- `ainpc.debug.health`;
- tool-uri semantic context pentru world, quest, mapping, story si routing.

## Propus

- `ainpc.server.snapshot`;
- `ainpc.npc.list`;
- `ainpc.npc.context`;
- `ainpc.quest.summary`;
- `ainpc.world.mapping.summary`;
- `ainpc.dialog.context`;
- `ainpc.story.summary`;
- `ainpc.reputation.summary`;
- `ainpc.debug.bridge`.

## Regula

- tool-urile importante trebuie sa consume snapshot read-only;
- aliasurile vechi raman doar pentru compatibilitate.

## Legaturi

- `operations/mcp-serviciu-imbunatatiri.md`
- `architecture/spring-ai-mcp-serviciu-intern.md`
