# Story si context AI

Status: document derivat pentru integrarea dintre contextul narativ si AI.
Actualizat: 2026-07-14.

Aceasta pagina descrie consumul contextului validat, nu stocarea sau modificarea story state-ului.

## Flux

1. `StoryContextService` construieste snapshot-ul read-only.
2. orchestration-ul AI selecteaza capabilitatea si formuleaza cererea.
3. raspunsul este validat si ramane sugestie, briefing sau draft.
4. orice efect executabil trece prin serviciul determinist responsabil.

## Consumatori

- dialog si briefing;
- GUI si inspectie admin;
- quest authoring si generare asistata;
- rezumate narative.

## Regula

- contextul AI nu este autoritate asupra starii;
- acest document nu redefineste contractele story state sau story context.

## Legaturi

- `architecture/story-state-service.md`
- `architecture/story-context-service.md`
- `architecture/ai-orchestrare-si-mecanici.md`
