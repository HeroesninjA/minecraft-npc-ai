# Patch Planner

Acesta este contractul pentru analiza, planificarea si aplicarea de patch-uri semantice pe sate existente.

## Scop

- transforma un sat existent sau un mapping partial intr-un set de completari mici si inspectabile;
- produce `GapReport`, `PatchCandidate`, `PatchPlan` si rezultatul aplicarii;
- scrie doar completari validate, nu reconstructie brutala.

## Flux

- `VanillaVillageScanner`;
- `SemanticVillageMapper`;
- `VillageGapAnalyzer`;
- `VillagePatchPlanner`;
- validare;
- `VillagePatchApplier`;
- `WorldAdminService`.

## Principiu

- completeaza, nu inlocui;
- adauga case, node-uri, workstations si puncte sociale proportionale;
- nu sterge si nu reconstrui intreaga asezare fara o faza separata.
