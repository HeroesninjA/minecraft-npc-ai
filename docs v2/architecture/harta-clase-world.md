# Harta claselor pentru world

Status: canonical in `docs v2`.
Actualizat: 2026-06-21.

Aceasta harta urmareste subsistemul world: mapping semantic, context, bindings si audit.

## Noduri principale

- `WorldAdminService`;
- `WorldContextSnapshotBuilder`;
- `WorldContextSnapshot`;
- `NpcWorldBindingService`;
- `WorldMappingSemanticIndex`;
- `VillageGapAnalyzer`;
- `VillagePatchPlanner`;
- `ExteriorStructureAnalyzer`.

## Flux

- world admin construieste contextul;
- snapshot-ul este consumat de dialog, quest si AI;
- bindings-ul NPC se pastreaza separat;
- gap analysis si patch planning completeaza lumea.

## Reguli

- mapping-ul semantic este sursa de orientare, nu doar coordonatele;
- snapshot-urile trebuie sa fie inspectabile;
- corectiile si patch-urile trebuie sa ramana auditable.

## Legaturi

- `architecture/mapping.md`
- `architecture/story-context-service.md`
- `reference/harta-clase-index.md`
