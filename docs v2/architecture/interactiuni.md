# Interactiuni player-NPC

Status: contract canonic verificat in cod pentru intrarea playerului in dialog.
Actualizat: 2026-07-15.

Acest document detine capturarea mesajului si alegerea tintei NPC. Evenimentele story si reactiile de lume au alt flux.

## Click dreapta

`NPCInteractionListener` proceseaza doar entitati `Villager`:

1. cauta NPC-ul asociat sau incearca `NPCManager.ensureVillagerIsNPC(...)`;
2. anuleaza interactiunea vanilla;
3. verifica distanta prin `AINPC.isInRange(player)`;
4. actualizeaza `NPCContext` si seteaza playerul activ;
5. ofera prioritate interactiunii de quest cand `features.quest` este activ;
6. daca questul nu consuma interactiunea, deschide o sesiune de conversatie.

Sesiunea este pastrata in memorie de `ConversationSessionManager`. Pornirea ei creeaza asincron memoria de prima intalnire daca NPC-ul nu are deja memorii pentru acel player. Salutul initial este local si nu necesita provider AI.

## Chat

`NPCChatListener` este inregistrat permanent si asculta `AsyncChatEvent` la prioritatea `LOWEST`:

- un input text cerut de GUI are prioritate fata de dialog;
- o sesiune activa tinteste NPC-ul deja asociat playerului;
- sesiunea expira dupa 300000 ms la urmatorul mesaj procesat;
- ascultarea pasiva este opt-in prin `dialog.passive_listen_enabled`, implicit `false`;
- in modul pasiv, tinta poate fi un NPC mentionat, singurul NPC apropiat sau cel mai apropiat NPC din raza de auto-engage;
- intentiile de quest sunt evaluate inaintea `DialogManager`.

## Limite confirmate

- listenerul seteaza `event.isCancelled = true` inainte sa stie daca exista o tinta; in forma curenta, un mesaj fara GUI, sesiune sau NPC pasiv eligibil nu mai ajunge in chatul public;
- o sesiune activa nu revalideaza distanta fata de NPC la fiecare mesaj;
- `clearConversation(...)` elimina doar sesiunea playerului, nu si `NPCContext.interactingPlayer`;
- `NPCContext` curata playerul vechi dupa 120000 ms sau cand acesta este offline, dar numai in timpul unei actualizari a entitatilor apropiate;
- raspunsurile de quest consumate de `ScenarioEngine` pot ocoli complet `DialogManager` si post-procesarea lui.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners/NPCInteractionListener.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners/NPCChatListener.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners/AbstractPluginListener.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/ConversationSessionManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NPCContext.kt`

## Legaturi

- `architecture/interactiune-dialog-reactie-stack.md`
- `architecture/dialog-si-conversatii.md`
- `architecture/reactie-npc-jucator.md`
