# NPC, rutine si simulare

Status: index tematic.
Actualizat: 2026-07-15.

Index pentru runtime-ul NPC, persistenta de lume, rutina si lifecycle. Nu confirma singur implementarea.

## Runtime si rutina

- `reference/simulation-stack.md`
- `architecture/simulation-service.md`
- `architecture/harta-clase-routine.md`
- `architecture/comportament-natural-npc-rutine-alocari.md`
- `architecture/evita-comportamentul-robotic-prin-rutine-staggered.md`

## Persistenta si lifecycle

- `planning/npc-population-world-stack.md`
- `architecture/npc-world-bindings.md`
- `architecture/households-persistente.md`
- `architecture/npc-uri-temporare-si-episodice.md`
- `architecture/simulare-sat-si-lume.md`

## Interactiune

- `architecture/interactiune-dialog-reactie-stack.md`
- `architecture/interactiuni.md`
- `architecture/dialog-si-conversatii.md`
- `architecture/reactie-npc-jucator.md`

## Roadmap

- `planning/rutine-npc-si-timeline.md`

## Regula

- simularea nevoilor si rutina cu miscare sunt fluxuri distincte;
- relatia player-NPC nu trebuie confundata cu relatiile NPC-NPC;
- `NpcPopulationService` inseamna statistici, nu spawn sau rebalance;
- household-ul persistent nu este autoritate de rutina;
- modurile actorilor temporari raman partiale pana cand sunt consumate de schedulere si dialog.
