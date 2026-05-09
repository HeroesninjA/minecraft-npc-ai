# Generare si Worldgen

Actualizat: 2026-05-04

Aceasta categorie acopera generarea de sate, completarea lumii si constructia asistata.

## Documente

| Document | Rol |
|---|---|
| `../../playable-village-ux.md` | Cerinte de playability pentru generator: teren plat, case mai mari, distante, drumuri si locuri de interactiune |
| `../../settlement-plan.md` | Contract pentru plan complet de sat/regiune, validabil inainte de constructie sau spawn |
| `../../patch-planner.md` | Contract pentru gap analyzer si patch-uri planificate fara executie directa |
| `../../template-cladiri-si-marker-nodes.md` | Contract pentru template-uri, ancore si marker nodes care devin mapping |
| `../../worldedit-integration-contract.md` | Contract pentru integrarea optionala WorldEdit ca adapter de executie |
| `../../generare-sate-fara-worldedit.md` | Generare si completare sate vanilla fara WorldEdit obligatoriu |
| `../../generare-sate-worldedit-si-npc.md` | Integrare optionala WorldEdit |
| `../../generare-ai-si-constructie-automata.md` | Planuri AI validate pentru constructie |
| `../../ai-orchestrare-si-mecanici.md` | AI orchestration pentru drafturi de plan, nu executie directa |
| `../../ordine-spawn-npc-cladiri-region-node.md` | `SettlementPlan`, `HouseAllocation`, spawn si rollback |
| `../../mapping.md` | Mapping ca iesire obligatorie dupa generare |

## Regula

Generarea trebuie sa produca mai intai planuri validate, apoi mapping, apoi spawn. AI-ul nu executa direct modificari in lume. Cerintele din `playable-village-ux.md` au prioritate fata de generare decorativa: satul trebuie sa fie lizibil si jucabil inainte sa fie complex.

## Urmatoarele documente utile

- Contract pentru builder nativ si executie fizica fara WorldEdit.
