# Harta claselor pentru GUI

Actualizat: 2026-06-29

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `GUI`: serviciul central, sesiuni, ecrane, dashboard-uri de world/quest/story/debug si comenzile de confirmare.

Nu este un inventar complet al tuturor ecranelor. Este o harta de lucru pentru nodurile care afiseaza contextul intern si permit operatii asistate.

## Noduri principale

- `GuiService` -> coordoneaza deschiderea ecranelor, selectie, filtre, confirmari si `gui.skip_confirmations`
- `GuiSessionManager` -> gestioneaza sesiunile active pe player si id
- `GuiScreen` -> interfata comuna pentru toate ecranele
- `MainHubGui` -> hub de intrare
- `QuestLogGui` -> UI pentru log progresii, cu sumar progres (slot 2), context poveste (slot 6), filtre inline (slots 9-18)
- `QuestDetailGui` -> UI pentru detalii de quest, diagnostics si ancore
- `StoryGui` -> UI pentru context narativ, story state si card de mediu (slot 5)
- `WorldHubGui` -> UI pentru world, regiuni, places, nodes si ancore, cu card identitate (slot 6)
- `WorldRegionGui` -> UI pentru detalii regiune cu card identitate (slot 9)
- `WorldPlaceGui` -> UI pentru detalii place
- `AdminMappingGui` -> UI admin pentru mapping, cu identitate (slot 6), patch analyze/plan (slots 22/23), create place/node
- `AdminQuestGui` -> UI admin pentru questuri
- `StatsGui` -> UI pentru inspectie rapida de status
- `NpcInteractionGui` -> UI pentru interactiune NPC: quest, shop, rutina, story
- `NpcManagerGui` -> UI admin pentru manager NPC
- `RoutineGui` -> UI pentru inspectie rutine NPC
- `ShopGui` -> UI pentru shop NPC
- `AuditGui` -> UI pentru audit
- `DebugGui` -> UI pentru dump si inspectie tehnica
- `QuestAuthoringGui` -> UI pentru authoring quest read-only
- `ConfirmActionGui` -> UI pentru confirmari actiuni (optional cu skip_confirmations)
- `AdminHubGui`, `CreatorHubGui`, `PlayerHubGui` -> hub-uri specializate
- `QuestMapGui` -> UI pentru mapping ancore quest
- `QuestCreatorGui`, `QuestCreatorDefinitionsGui`, `QuestCreatorTestGui` -> UI pentru creator quest
- `QuestEditGui`, `QuestCreateGui`, `QuickQuestGui` -> UI pentru editare/creare rapida
- `MappingCreatorGui`, `MappingCreateRegionGui`, `MappingCreatePlaceGui`, `MappingCreateNodeGui` -> UI pentru creator mapping

## Flux principal

`GuiService` -> `GuiSessionManager` -> `GuiScreen`

`MainHubGui` deschide trasee spre world, quest, story, stats si debug.

`WorldHubGui` si `QuestDetailGui` afiseaza contextul semantic si legaturile de ancore.

`DebugGui` expune actiuni de diagnostic si dump.

## Relatii utile

- `GuiService` tine filtrele si selectiile pentru autoring si loguri.
- `GuiSessionManager` este stratul de session tracking pentru UI.
- `GuiScreen` este contractul comun; toate ecranele concrete il implementeaza.
- `WorldHubGui` si `QuestDetailGui` sunt cele mai utile pentru navigare semantica.
- `DebugGui` este pentru inspectie si nu pentru gameplay curent.

## Cum se citeste

1. Incepe cu `GuiService`.
2. Continua cu `GuiSessionManager`.
3. Treci la `MainHubGui`.
4. Foloseste `WorldHubGui`, `QuestDetailGui` si `StoryGui` pentru traseele tematice.
5. Foloseste `DebugGui` cand vrei diagnostic si dump.

## Nota

Daca vrei doar traseul intre module si pachete, foloseste harta de pachete. Daca vrei relatiile dintre clasele GUI, acesta este documentul potrivit.
