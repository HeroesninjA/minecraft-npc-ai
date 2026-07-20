# Reactii NPC: dialog, story si relatii

Status: contract canonic verificat in cod pentru mutatiile produse dupa interactiuni si evenimente.
Actualizat: 2026-07-15.

Runtime-ul nu are un singur motor de reactie reutilizat de dialog, quest, story si simulare. Exista trei cai distincte care nu trebuie confundate.

## Reactia dupa dialogul playerului

`DialogManager` foloseste clasificarea locala a sentimentului pentru:

- relatia player-NPC din `npc_relationships`;
- memoria playerului din `npc_memories`, numai pentru interactiuni suficient de importante;
- emotiile globale ale NPC-ului prin `EmotionManager`.

Aceasta cale ruleaza numai dupa un raspuns nevid al `DialogManager`. Nu reprezinta validarea unei actiuni de quest sau combat.

## Reactia la un eveniment story

`StoryAuthoringService.recordEvent(...)` apeleaza `StoryReactionService.reactToEvent(...)` dupa persistarea evenimentului. Serviciul:

- recunoaste doar tipurile prezente in mapa sa fixa de reactii;
- poate fi oprit prin `story.npc_reactions_enabled`;
- rezolva regiunea din `regionId`, cu fallback la `scopeId`;
- selecteaza NPC-uri spawnate in raza fata de centrul regiunii;
- respecta prioritatea starii curente;
- schimba `NPCState`, `plannedRoutineActivity` si valorile din `NPCEmotions`.

Modificarile emotionale sunt aplicate direct pe obiect, nu prin `EmotionManager`. Calea nu persista explicit starea, rutina sau emotiile, nu actualizeaza display name si nu emite `EmotionChangedEvent`.

## Relatiile NPC-NPC

`RelationshipService` apartine simularii sociale dintre doua NPC-uri:

- foloseste tabela `npc_npc_relationships`;
- este alimentat de `RoutineCoordinator` si poate aplica decay periodic;
- alimenteaza GUI-ul de relatii, contextul NPC-NPC si snapshot-ul MCP;
- nu este serviciul folosit de `DialogManager` pentru relatia dintre NPC si player.

Clasa de date `NPCRelationship` este reutilizata in ambele domenii, dar tabelele, cheile si apelantii sunt diferiti.

## Ce nu este unificat

- mesajele de quest consumate de `ScenarioEngine` pot ocoli mutatiile de dialog;
- reactiile story nu creeaza memorii despre player si nu ating `npc_relationships`;
- `RelationshipService` nu primeste automat sentimentul conversatiei playerului;
- dialogul poate exprima contextul curent, dar nu dovedeste ca toate efectele au fost persistate.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/DialogManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/RelationshipService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryReactionService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryAuthoringService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineCoordinator.kt`

## Legaturi

- `architecture/interactiuni.md`
- `architecture/dialog-si-conversatii.md`
- `architecture/story-state-service.md`
- `architecture/interactiune-dialog-reactie-stack.md`
