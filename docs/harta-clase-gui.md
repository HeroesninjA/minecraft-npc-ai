# Harta claselor pentru GUI

Actualizat: 2026-06-21

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `GUI`: serviciul central, sesiuni, ecrane, dashboard-uri de world/quest/story/debug si comenzile de confirmare.

Nu este un inventar complet al tuturor ecranelor. Este o harta de lucru pentru nodurile care afiseaza contextul intern si permit operatii asistate.

## Noduri principale

- `GuiService` -> coordoneaza deschiderea ecranelor, selectie, filtre si actiuni
- `GuiSessionManager` -> gestioneaza sesiunile active pe player si id
- `GuiScreen` -> interfata comuna pentru toate ecranele
- `MainHubGui` -> hub de intrare
- `WorldHubGui` -> UI pentru world, regiuni, places, nodes si ancore
- `QuestDetailGui` -> UI pentru detalii de quest, diagnostics si ancore
- `StoryGui` -> UI pentru contextul narativ si starea story
- `StatsGui` -> UI pentru inspectie rapida de status
- `DebugGui` -> UI pentru dump si inspectie tehnica

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
