# Generare si Worldgen

Actualizat: 2026-04-30

Aceasta categorie acopera generarea de sate, completarea lumii si constructia asistata.

## Documente

| Document | Rol |
|---|---|
| `../../generare-sate-fara-worldedit.md` | Generare si completare sate vanilla fara WorldEdit obligatoriu |
| `../../generare-sate-worldedit-si-npc.md` | Integrare optionala WorldEdit |
| `../../generare-ai-si-constructie-automata.md` | Planuri AI validate pentru constructie |
| `../../ordine-spawn-npc-cladiri-region-node.md` | `SettlementPlan`, `HouseAllocation`, spawn si rollback |
| `../../mapping.md` | Mapping ca iesire obligatorie dupa generare |

## Regula

Generarea trebuie sa produca mai intai planuri validate, apoi mapping, apoi spawn. AI-ul nu executa direct modificari in lume.

## Urmatoarele documente utile

- Specificatie `SettlementPlan`.
- Specificatie `PatchPlanner`.
- Contract pentru template-uri de cladiri si marker nodes.
