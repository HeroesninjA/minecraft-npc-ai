# World mapping si spawn

Status: index tematic derivat.
Actualizat: 2026-07-15.

Index pentru modelul semantic, planurile de settlement si executia spawn-ului.

## Documente

- `architecture/mapping.md` - owner pentru `Region -> Place -> Node`;
- `architecture/harta-clase-world.md` - clasele de mapping, scan si patch;
- `architecture/harta-clase-settlement.md` - fluxurile separate de settlement;
- `architecture/settlement-plan.md` - taxonomia planurilor si limitele lor;
- `architecture/generare-populatie-narativa.md` - preview-ul narativ partial;
- `guides/ordine-spawn-npc-cladiri-region-node.md` - procedura reala de spawn;
- `architecture/npc-world-bindings.md` - ancorele persistente NPC;
- `architecture/households-persistente.md` - rezultatul persistent al alocarii;
- `architecture/harta-clase-spawn.md` - planner, validator si orchestrator.

## Regula

- mapping-ul este intrarea spawn-ului, nu dovada unei constructii fizice;
- `SettlementPlan`, `PopulationPlan`, `HouseAllocation` si `PatchPlan` nu sunt sinonime;
- acest fisier ruteaza si nu redefineste contractele.
