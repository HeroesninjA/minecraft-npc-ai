# Harta claselor pentru debug

Status: canonical in `docs v2`.
Actualizat: 2026-06-21.

Aceasta harta urmareste subsistemul debug, audit si dump-uri.

## Noduri principale

- `DebugDumpService`;
- `RecentEventsBuffer`;
- `WorldMappingSemanticIndex`;
- `MappingWandService`;
- `AuditReport`;
- `OpenAIDebugSnapshot`.

## Flux

- serviciile aduna stare reala;
- buffer-ul pastreaza evenimente recente;
- indexul semantic ajuta la inspectie;
- auditul si dump-urile devin artefacte verificabile.

## Reguli

- debug-ul nu trebuie sa schimbe runtime-ul;
- dump-urile trebuie sa fie utile si stabile;
- mapping wand trebuie sa ramana legat de audit;
- AI debug snapshot trebuie sa explice, nu sa execute.

## Legaturi

- `reference/harta-clase-cod.md`
- `architecture/harta-clase-world.md`
- `architecture/harta-clase-ai.md`
