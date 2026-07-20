# Limita integrarii WorldEdit

Status: contract de roadmap; integrarea nu este implementata.
Actualizat: 2026-07-15.

WorldEdit poate exista pe un server de test, dar AINPC nu declara un adaptor sau o dependenta de productie care sa execute constructii.

## Ce exista

- `BuildMode.WORLDEDIT_TEMPLATE` in DTO-urile API;
- `PatchBuildMode.WORLDEDIT_TEMPLATE` in planner-ul de patch;
- un warning explicit in `VillagePatchApplier` ca aplicarea WorldEdit nu este suportata;
- fixture-ul controlat raporteaza `usesWorldEdit=false`.

Aceste simboluri sunt capability placeholders, nu integrare functionala.

## Ce nu exista

- modul sau adaptor WorldEdit;
- loader de schematic/template fizic;
- paste, rotire sau transformare de blocuri;
- preflight pentru coliziuni si chunk-uri;
- undo/rollback WorldEdit;
- jurnal persistent al executiei;
- legatura dintre DTO-ul `SettlementGenerationPlan` si un executor.

## Conditii pentru activare viitoare

Un adaptor poate deveni activ numai dupa ce are:

1. dependenta optionala si detectie explicita de capabilitate;
2. preview determinist si validare a zonei tinta;
3. executor separat de logica de gameplay;
4. undo real si rezultat persistent;
5. mapping semantic creat numai dupa succesul constructiei sau compensat atomic;
6. teste fara WorldEdit si cu WorldEdit prezent.

Pana atunci, documentatia si comenzile nu trebuie sa promita constructie, fallback sau rollback WorldEdit.

## Legaturi

- `architecture/generare-sate-fara-worldedit.md`
- `planning/generare-sate-worldedit-si-npc.md`
- `planning/schema-authoring-structuri-world.md`
- `planning/ce-mai-trebuie-pentru-sat-semantic.md`
