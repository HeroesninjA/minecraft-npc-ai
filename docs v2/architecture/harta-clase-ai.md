# Harta claselor pentru AI

Status: canonical in `docs v2`.
Actualizat: 2026-06-21.

Aceasta harta urmareste subsistemul AI: prompturi, orchestrare si dialog.

## Noduri principale

- `OpenAIService`;
- `OpenAIPromptSnapshotFactory`;
- `DialogManager`;
- `AIOrchestrationService`;
- `StoryContextService`;
- `ProgressionService`;
- `PromptSnapshot`;
- `DialogHistory`;
- `NPCRelationship`;
- `NPCEmotions`.

## Flux

- contextul narativ si de progres intra in snapshot;
- dialogul trimite cererea prin serviciul AI;
- orchestration decide calea;
- rezultatul revine in dialog sau in serviciul specializat.

## Reguli

- AI-ul propune, runtime-ul executa;
- prompturile trebuie sa fie compacte si auditable;
- story si progression pot alimenta AI, dar nu sunt controlate de el;
- fallback-ul ramane obligatoriu.

## Legaturi

- `architecture/story-context-service.md`
- `architecture/harta-clase-npc.md`
- `reference/harta-clase-cod.md`
