# Harta claselor pentru context

Status: harta derivata din cod.
Actualizat: 2026-07-15.

Codul are mai multe familii de context; ele nu formeaza un singur pipeline si nu au acelasi lifecycle.

## Context activ

- `NPCContext` - stare locala mutabila, semnale de simulare, player activ si descriere pentru prompt;
- `WorldContextSnapshot` - proiectia read-only a mapping-ului local;
- `StoryContextService` si `StoryContextSnapshot` - stare story, evenimente, ancore si semnale;
- `EnvironmentEngine` si `EnvironmentContext` - cache per lume pentru timp, vreme, sezon si evenimente speciale;
- `PromptSnapshot` - agregatul imutabil capturat pe thread-ul Paper inaintea requestului OpenAI.

## Playerul activ

`NPCContext.setInteractingPlayer(...)` pastreaza obiectul `Player` si timestampul ultimei legari. `ConversationSessionManager` pastreaza separat sesiunea player -> NPC.

- inchiderea sesiunii nu goleste imediat `NPCContext`;
- playerul din context este eliminat dupa 120000 ms sau cand devine offline;
- curatarea ruleaza numai cand `updateNearbyEntities(...)` este apelat;
- cat timp playerul ramane legat, `generateContextDescription()` poate include numele si `StoryContextService.buildForNpc(npc, player)`.

## Environment

`NPCContext` cere `EnvironmentEngine.getContextForLocation(...)`, care porneste de la cache-ul lumii si ajusteaza temperatura dupa biome. `EnvironmentEngine.tick()` ar actualiza cache-ul, dar nu are apelant de productie confirmat; timpul si vremea dintr-un context deja creat pot ramane vechi.

## Utilitare izolate

- `ContextService` construieste `ContextSnapshot` si blocuri compacte;
- `ContextRedactor` mascheaza chei OpenAI, UUID-uri, emailuri si IPv4;
- niciun apel din `src/main` nu conecteaza `ContextService` sau `ContextRedactor` la promptul activ;
- testele unitare ale utilitarelor nu confirma integrarea lor end-to-end.

## Regula

- `toPromptBlock()` inseamna serializare, nu redactare;
- starea unei sesiuni nu trebuie dedusa numai din `NPCContext.interactingPlayer`;
- contextul environment nu detine mapping-ul `Region -> Place -> Node`;
- nu descrie `ContextRedactor` drept protectie activa pana cand exista wiring si test end-to-end.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NPCContext.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/ConversationSessionManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/environment/EnvironmentEngine.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/OpenAIPromptSnapshotFactory.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/context/ContextRedactor.kt`

## Legaturi

- `architecture/environment-context-si-engine.md`
- `architecture/story-context-service.md`
- `architecture/harta-clase-ai.md`
- `reference/prompt-safety-guide.md`
