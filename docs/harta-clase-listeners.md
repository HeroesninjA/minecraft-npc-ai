# Harta claselor pentru listeners

Actualizat: 2026-06-25

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `listeners`: inregistrarea centralizata a listener-elor Bukkit si gestionarea evenimentelor de joc.

## Noduri principale

- `ListenerRegistry` -> punctul central de inregistrare; instantiaza si inregistreaza toti cei 7 listeneri
- `AbstractPluginListener` -> clasa de baza pentru toti listenerii (referinta plugin, mesaje, conversatii, task-uri sync/async)
- `VillagerLifecycleListener` -> sincronizeaza villagerii vanilla cu sistemul NPC (spawn, chunk load, career change, death)
- `QuestObjectiveListener` -> monitorizeaza evenimentele player (move, craft, break, place, combat, inventory) si le trimite catre `ScenarioEngine` pentru progres
- `NPCInteractionListener` -> click dreapta pe NPC: inregistrare, sesiune conversatie, delegare catre quest engine
- `NPCChatListener` -> dialog prin chat: rezolvare tinta, intentii quest (accept/decline/abandon), integrare `DialogManager`
- `MappingWandListener` -> interactiuni cu unealta wand: puncte mapping, selectie regiune
- `PlayerJoinListener` -> la join: verifica memoria NPC, aplica recunoastere si reactii emotionale

## Flux principal

`ListenerRegistry.registerAll(plugin)` -> instantiaza fiecare listener -> `pluginManager.registerEvents(listener, plugin)`

`QuestObjectiveListener` -> eveniment Paper -> `ScenarioEngine` -> progres obiective

`NPCInteractionListener` + `NPCChatListener` -> interactiune player -> `DialogManager` / `ScenarioEngine`

## Relatii utile

- `ListenerRegistry` este apelat o singura data la startup din `AINPCPlugin`
- `AbstractPluginListener` ofera metode comune (trimitere mesaje, task-uri sync/async)
- `QuestObjectiveListener` este cel mai complex; alimenteaza direct progresul de quest
- `NPCInteractionListener` si `NPCChatListener` formeaza stratul de interactiune player-NPC
- `MappingWandListener` este izolat de restul; depinde doar de `MappingWandService`

## Cum se citeste

1. Incepe cu `ListenerRegistry` (inregistrarea)
2. Continua cu `QuestObjectiveListener` (progres quest)
3. Treci la `NPCInteractionListener` si `NPCChatListener` (interactiune)
4. Consulta `VillagerLifecycleListener` pentru sincronizare vanilla
5. Restul sunt listeneri specializati
