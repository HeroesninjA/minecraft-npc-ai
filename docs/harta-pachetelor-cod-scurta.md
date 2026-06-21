# Harta Scurta a Pachetelor de Cod

Actualizat: 2026-06-21

Aceasta este versiunea scurta a hartii de cod, folosita pentru orientare rapida.
Nu schimba runtime-ul si nu inlocuieste [harta completa](./harta-pachetelor-cod.md).

## Citire rapida

- `ainpc-api` - contracte publice, modele shared, evenimente si API addon.
- `ainpc-core-plugin` - runtime principal, servicii, manageri, engine, UI si persistenta.
- `ainpc-scenario-medieval` - addon concret pentru scenariul medieval.

## Flux minim

1. `AINPCPlugin` bootstrapeaza platforma, baza de date, engine-urile si listener-ele.
2. `AINPCPlatform` compune `AddonRegistry`, `WorldAdminService` si feature snapshot-ul.
3. `FeaturePackLoader` incarca continutul semantic care hraneste `DecisionEngine`, `DialogueEngine` si `ScenarioEngine`.
4. `WorldContextSnapshotBuilder` si `StoryContextService` construiesc contextul pentru `ProgressionService`, `DialogManager` si AI.
5. `NpcSpawnOrchestrator`, `RoutineService` si `WorldAdminService` mentin lumea si NPC-urile sincronizate.
6. `DebugDumpService`, `AuditReport`, `VillageGapAnalyzer` si `VillagePatchPlanner` acopera inspectia si corectiile operationale.

## Pachete cheie

- `ro.ainpc.api` - contracte publice.
- `ro.ainpc.platform` - profil runtime si addon registry.
- `ro.ainpc.engine` - scenarii, dialog, decizie si feature packs.
- `ro.ainpc.world` - world admin, context si mapping.
- `ro.ainpc.world.scan` - scan si import semantic pentru sate vanilla.
- `ro.ainpc.world.patch` - gap analysis si planificare de patch.
- `ro.ainpc.world.exterior` - planificare pentru structuri exterioare.
- `ro.ainpc.world.fixture` - fixtures controlate pentru testare.
- `ro.ainpc.spawn` - spawn, household si populator.
- `ro.ainpc.routine` - rutina NPC.
- `ro.ainpc.progression` - progresii, ancore si snapshot-uri.
- `ro.ainpc.story` - stare narativa si context.
- `ro.ainpc.gui` - fluxuri UI si ecrane.
- `ro.ainpc.ai` - prompturi, relatie NPC si orchestration AI.
- `ro.ainpc.debug` - dump-uri si buffer de evenimente.

## Clase nod

- `AINPCPlugin` - boot si wiring.
- `AINPCPlatform` - contractul de platforma interna.
- `FeaturePackLoader` - datele de continut.
- `ScenarioEngine` - questuri si scenarii.
- `DialogueEngine` - raspunsuri NPC.
- `DecisionEngine` - simulare si actiuni.
- `WorldAdminService` - regiuni, locuri, noduri.
- `StoryContextService` - context pentru poveste si quest.
- `ProgressionService` - progresie si ancore.
- `DialogManager` - relatie, istoric si AI request.
- `NpcSpawnOrchestrator` - spawn si household.
- `RoutineService` - rutina NPC.
- `DebugDumpService` - inspectie read-only.

## Cand o folosesti

- Cand ai nevoie de o privire de ansamblu inainte de a intra in harta completa.
- Cand vrei sa vezi rapid ce modul depinde de ce alt modul.
- Cand faci vibe coding si vrei traseul minim pentru pachetul potrivit.
