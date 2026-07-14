# API Events, Listeners si Triggers

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Contractul public pentru evenimente, listener-e si trigger-e din AINPC.

## Acopera

- quest si progresie;
- story state si story events;
- dialog si intentii;
- NPC lifecycle, interactiune, emotii si memorie;
- context read-only pentru world, story, NPC si player.

## Principii

- evenimentele publice sunt contract de integrare;
- serviciile deterministe raman sursa de adevar;
- contextul expus este read-only;
- evenimentele cancellable se limiteaza la starea nealterata.

## Directie

- defineste pachete clare in `ainpc-api`;
- foloseste payload-uri stabile si prietenoase cu Java;
- mentine hook-uri clare pentru addonuri.

## Legaturi

- `reference/addon-developer-guide.md`
- `reference/documentatie-api.md`
- `architecture/spring-ai-mcp-serviciu-intern.md`
