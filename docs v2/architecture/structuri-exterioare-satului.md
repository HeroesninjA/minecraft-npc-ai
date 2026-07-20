# Catalog si validare pentru structuri exterioare

Status: contract verificat in cod.
Actualizat: 2026-07-15.

Subsistemul exterior descrie si valideaza mapping semantic. Nu genereaza structuri fizice, loot, mobi, NPC-uri sau quest progress.

## Catalog activ

`ExteriorStructureBlueprintCatalog` contine blueprint-uri pentru:

- `castle`, `forest`, `fountain`, `isolated_house`;
- `hamlet`, `faction_settlement`, `dungeon`, `camp`;
- `ruins`, `cave_or_mine`, `shrine`, `watchtower`.

Fiecare blueprint declara sugestii de tip de regiune, tags, places, nodes si reguli. `CUSTOM` exista in enum, dar nu are blueprint in catalog si nu poate fi planificat prin comanda.

## Comenzi

- `/ainpc world outside types` listeaza tipurile;
- `/ainpc world outside blueprint <type>` afiseaza contractul hardcodat;
- `/ainpc world outside plan <type> <baseId>` produce IDs propuse, read-only;
- `/ainpc world outside report <region>` inspecteaza mapping-ul existent;
- `/ainpc world outside validate <region>` afiseaza raportul cu accent pe validare.

## Clase

- `ExteriorStructurePlanner` normalizeaza ID-ul si produce numai un plan textual;
- `ExteriorStructureAnalyzer` detecteaza tipul din regiune, places si nodes;
- validarile sunt specifice tipului si raporteaza warning-uri sau erori;
- niciun flux nu creeaza `Region`, `Place`, `Node` sau blocuri.

## Regula

- blueprint-ul este o conventie semantica, nu schema YAML si nu template de constructie;
- mapping-ul existent ramane sursa analizata;
- NPC-urile, household-urile, loot-ul si questurile cer fluxuri separate;
- `SettlementPlan` nu este folosit de acest subsistem.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/exterior/ExteriorStructureType.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/exterior/ExteriorStructureBlueprintCatalog.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/exterior/ExteriorStructurePlanner.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/world/exterior/ExteriorStructureAnalyzer.kt`

## Legaturi

- `architecture/mapping.md`
- `operations/mediu-test-controlat-sat-si-structuri-exterioare.md`
- `reference/template-cladiri-si-marker-nodes.md`
