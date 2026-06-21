# Harta claselor pentru world

Actualizat: 2026-06-21

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste numai subsistemul `world`: mapping semantic, snapshot de context, bindings NPC, audit de gap-uri, planificare de patch si validare de structuri exterioare.

Nu este un inventar complet al tuturor claselor din world. Este o harta de lucru pentru zonele care decid comportamentul semantic al lumii.

## Noduri principale

- `WorldAdminService` -> admin semantic pentru regiuni, places, nodes si persistenta mappingului
- `WorldContextSnapshotBuilder` -> construieste contextul de lume consumat de dialog, quest si AI
- `WorldContextSnapshot` -> rezumatul semantic al lumii curente si al vecinatatilor relevante
- `NpcWorldBindingService` -> salveaza si citeste legaturile NPC -> home/work/social
- `WorldMappingSemanticIndex` -> index de inspectie si debug pentru mapping-ul semantic
- `VillageGapAnalyzer` -> identifica lipsuri si nepotriviri in sat / regiune
- `VillagePatchPlanner` -> transforma gap-urile in planuri si candidati de patch
- `ExteriorStructureAnalyzer` -> valideaza structurile exterioare si contextul semantic al lor

## Flux principal

`WorldAdminService` -> `WorldContextSnapshotBuilder` -> `WorldContextSnapshot`

`WorldAdminService` -> `NpcWorldBindingService` -> bindings persistente

`VillageGapAnalyzer` -> `VillagePatchPlanner` -> `PatchCandidate` / `PatchPlan`

`ExteriorStructureAnalyzer` completeaza traseul de validare pentru structurile care nu sunt doar case simple.

## Relatii utile

- `WorldContextSnapshotBuilder` colecteaza locuri, node-uri, bindings si NPC-uri din contextul curent.
- `WorldContextSnapshot` este consumat de dialog, AI si quest anchoring.
- `NpcWorldBindingService` este stratul persistent pentru legaturi NPC -> world.
- `WorldMappingSemanticIndex` este stratul de inspectie pentru ceea ce s-a indexat deja.
- `VillageGapAnalyzer` si `VillagePatchPlanner` sunt perechea de audit si propunere pentru completarea lumii.

## Cum se citeste

1. Incepe cu `WorldAdminService`.
2. Continua cu `WorldContextSnapshotBuilder` si `WorldContextSnapshot`.
3. Urmareste `NpcWorldBindingService` pentru persistenta.
4. Treci la `VillageGapAnalyzer` si `VillagePatchPlanner` pentru corectii.
5. Foloseste `ExteriorStructureAnalyzer` cand te intereseaza validarea structurilor exterioare.

## Nota

Daca vrei doar traseul intre module si pachete, foloseste harta de pachete. Daca vrei traseul intre clasele de world, acesta este documentul potrivit.
