# Feature flags si lifecycle

Status: politica canonica cu abateri runtime documentate.
Actualizat: 2026-07-16.

Un flag exprima o intentie de configurare. Prezenta lui nu dovedeste automat ca listenerul, schedulerul, serviciul si fallback-ul folosesc aceeasi stare rezolvata.

## Regula

Pentru fiecare feature major trebuie verificate separat:

1. valoarea implicita din configuratie;
2. inregistrarea listenerului sau schedulerului;
3. gate-ul de la intrarea publica;
4. gate-ul din serviciu;
5. fallback-ul si comportamentul la reload sau shutdown.

Documentatia nu va declara un feature complet dezactivat daca doar o ramura interna este oprita.

Un feature flag nu acorda permisiune. Pentru orice intrare publica trebuie verificate separat flagul, nodul Bukkit si gate-ul final al mutatiei.

## Exemple confirmate

- `features.ai` este verificat in `OpenAIService`; dialogul poate continua cu fapte, template sau fallback local;
- `ai.orchestration.enabled` controleaza doar scaffold-ul orchestration, nu calea activa `DialogManager -> DialogueEngine`;
- `dialog.passive_listen_enabled` opreste rezolvarea pasiva a tintei, nu inregistrarea `NPCChatListener`;
- `features.quest` este verificat in listener-ele player-NPC inaintea ramurii quest;
- `features.gui` este verificat de comanda `/ainpc gui` si de `GuiInventoryListener`, dar nu in interiorul `GuiService.open`;
- `GuiService.canOpen` si `handleClick` verifica permisiunea cheii; comenzile sau serviciile finale raman responsabile de autorizarea mutatiei;
- `story.npc_reactions_enabled` opreste `StoryReactionService.reactToEvent(...)`;
- simularea cere `features.simulation` si `simulation.enabled`, iar rutina cere `features.routine` si `routine.enabled`;
- configuratia livrata are flagurile globale active, dar `simulation.enabled=false` si `routine.enabled=false`;
- `villagers.auto_repopulate.enabled` este independent de simulare si rutina; schedulerul apeleaza periodic metoda, iar metoda revine imediat cand flagul este `false`.

## Lifecycle actor scenariu

- `RUNTIME_ONLY` evita salvarea in DB;
- `FULL` si `LIGHT` urmeaza in prezent aceeasi cale `saveNPC(...)`;
- `duration_seconds` programeaza despawn-ul;
- `EPISODIC`, `SCENE_ONLY` si `RUNTIME_ONLY` declanseaza unregister la despawn;
- `TEMPORARY` nu este inclus in acea conditie;
- modurile de simulare si interactiune sunt metadata neconsumata de schedulere si dialog.

## Abateri cunoscute

- `NPCChatListener` ramane activ si anuleaza mesajul chiar cand ascultarea pasiva este oprita si nu exista sesiune;
- un apel direct `GuiService.open` poate crea inventarul cand `features.gui=false`; listenerul il inchide la primul click sau drag;
- `EnvironmentEngine.tick()` exista, dar nu are scheduler sau apelant de productie confirmat;
- metadata lifecycle/persistence/simulation a actorului nu este salvata si rehidratata din profil;
- pause/custom override din `RoutineCoordinator` nu este consultat de tick-ul rutinei;
- nu toate serviciile expun o stare comuna `enabled/disabled/no-op` inspectabila.

## Surse in cod

- `ainpc-core-plugin/src/main/resources/config.yml`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/listeners/NPCChatListener.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/GuiService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/listeners/GuiInventoryListener.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/OpenAIService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap/SchedulerCoordinator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NpcDefinitions.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineCoordinator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/story/StoryReactionService.kt`

## Legaturi

- `canonical/constitutie-proiect.md`
- `architecture/interactiuni.md`
- `architecture/ai-orchestrare-si-mecanici.md`
- `architecture/environment-context-si-engine.md`
- `architecture/simulation-service.md`
- `architecture/npc-uri-temporare-si-episodice.md`
- `reference/gui-stack.md`
- `reference/sistem-permisiuni-compatibilitate-pluginuri.md`
