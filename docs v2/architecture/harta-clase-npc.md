# Harta claselor pentru NPC

Status: harta derivata din cod.
Actualizat: 2026-07-15.

Aceasta harta separa identitatea NPC, contextul temporar si cele doua domenii de relatie.

## Identitate si lifecycle

- `AINPC` - identitate, profil, stare curenta, emotii si legatura cu entitatea;
- `NPCManager` - incarcare, spawn, reparare, deduplicare si persistenta operationala;
- `NPCPersonality`, `NPCEmotions`, `NPCState` si `NPCAction` - modelul local de comportament;
- `NpcVillageSnapshot` si `NpcRepairCounters` - inspectie si reparare, nu autoritate de gameplay.

## Context si dialog

- `NPCContext` - semnale din lume, simulare, environment, playerul activ si text pentru prompt;
- `ConversationSessionManager` - mapare temporara player -> NPC si timestampul sesiunii;
- `NpcFactResolver` - raspunsuri deterministe despre NPC;
- `DialogManager` - istoric, memorii si relatia player-NPC persistata;
- `DialogueEngine` - selectie fapt, intentie, template si AI optional.

## Relatii distincte

- `DialogManager` citeste si scrie `npc_relationships`, indexat prin NPC si UUID-ul playerului;
- `RelationshipService` citeste si scrie `npc_npc_relationships`, indexat prin doua UUID-uri de NPC;
- ambele folosesc modelul `NPCRelationship`, dar nu impart acelasi lifecycle.

## Reactii

- `EmotionManager` aplica si persista efectele dialogului;
- `StoryReactionService` modifica direct stare, activitate si emotii pentru evenimente story;
- `ScenarioEngine` poate produce raspunsuri de quest fara `DialogManager`.

## Reguli

- AI si dialog citesc starea; nu devin autoritate asupra questului sau story state-ului;
- sesiunea de conversatie si playerul din `NPCContext` sunt stari temporare diferite;
- nu combina statisticile player-NPC cu relatiile NPC-NPC;
- auditul si repararea raman separate de gameplay.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/AINPC.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NPCContext.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/ConversationSessionManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/DialogManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/RelationshipService.kt`

## Legaturi

- `architecture/interactiune-dialog-reactie-stack.md`
- `architecture/harta-clase-ai.md`
- `architecture/harta-clase-context.md`
- `reference/harta-clase-cod.md`
