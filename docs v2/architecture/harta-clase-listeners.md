# Harta claselor pentru listeners

Status: harta derivata din cod.
Actualizat: 2026-07-15.

`ListenerRegistry.registerAll()` inregistreaza listener-ele Paper fara un feature gate global pentru dialog.

## Listener-e player-NPC

- `NPCInteractionListener` - click dreapta, asociere villager-NPC, range gate, quest-first si pornire sesiune;
- `NPCChatListener` - input GUI, alegere tinta, intentii quest, cooldown si `DialogManager`;
- `AbstractPluginListener` - acces la sesiuni, pornire conversatie, memorie de prima intalnire si revenire pe thread-ul Paper;
- `ConversationSessionManager` - stare de sesiune in memorie, folosita de ambele listener-e.

## Alte intrari

- `QuestObjectiveListener` - evenimente de gameplay pentru progres;
- `VillagerLifecycleListener` - ciclul de viata al entitatilor;
- `MappingWandListener` - input pentru authoring de mapping;
- `PlayerJoinListener` - onboarding si semnale de intrare;
- `WorldListener` - curatarea cache-urilor la unload.

## Limite confirmate

- `NPCChatListener` anuleaza fiecare `AsyncChatEvent` dupa ramura de input GUI, chiar daca `resolveTarget(...)` returneaza `null`;
- testele existente confirma prezenta rutarii si a evenimentelor publice, nu comportamentul complet de chat, timeout, distanta sau fallback;
- listener-ele de dialog pot fi anulate prin unele evenimente publice, dar acest lucru nu transforma payload-ul in autoritate de persistenta.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners/ListenerRegistry.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners/NPCInteractionListener.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners/NPCChatListener.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/ConversationSessionManager.kt`

## Legaturi

- `architecture/interactiuni.md`
- `architecture/interactiune-dialog-reactie-stack.md`
- `reference/api-events-listeners-triggers.md`
