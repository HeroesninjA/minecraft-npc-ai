# Harta claselor pentru NPC

Status: canonical in `docs v2`.
Actualizat: 2026-06-21.

Aceasta harta urmareste subsistemul NPC: identitate persistenta, stare si relatii.

## Noduri principale

- `AINPC`;
- `NPCManager`;
- `NPCContext`;
- `NPCPersonality`;
- `NPCEmotions`;
- `NPCState`;
- `NPCAction`;
- `NPCRelationship`;
- `NpcVillageSnapshot`;
- `NpcRepairCounters`;
- `NpcFactResolver`.

## Flux

- `AINPC` tine identitatea;
- `NPCManager` coordoneaza ciclul de viata;
- `NPCContext` colecteaza semnale;
- personalitatea, emotiile si starea definesc comportamentul;
- relatiile si fact resolver-ul leaga NPC-ul de dialog si AI.

## Reguli

- identitatea NPC-ului trebuie sa ramana persistenta;
- contextul si emotiile trebuie sa fie inspectabile;
- repair si audit trebuie sa fie separate de gameplay;
- AI si dialog citesc starea, nu o inventeaza.

## Legaturi

- `architecture/harta-clase-world.md`
- `architecture/harta-clase-ai.md`
- `reference/harta-clase-cod.md`
