# Documentatie Lipsa si Idei Recomandate

Actualizat: 2026-04-30

Acest document listeaza documentatiile care lipsesc sau merita separate din documentele mari existente.

## Prioritate mare

| Document propus | De ce lipseste | Categorie |
|---|---|---|
| `npc-world-bindings.md` | NPC-urile au ancore runtime, dar nu model persistent clar `homePlaceId/workPlaceId/socialPlaceId` | World mapping si NPC |
| `server-admin-runbook.md` | Lipseste un ghid scurt pentru instalare, config minim, comenzi de verificare si troubleshooting | Operare |
| `release-checklist.md` | Lipseste o lista verificabila inainte de build/release/test pe Paper real | Operare |
| `migration-si-backup.md` | Urmeaza modele persistente noi; trebuie strategie de backup, migration si rollback | Operare |

## Prioritate medie

| Document propus | De ce lipseste | Categorie |
|---|---|---|
| `story-state-service.md` | Story-ul are design, dar nu contract pentru flags, events si integrare cu questuri | Quest, story si AI |
| `settlement-plan.md` | Spawn order v2 descrie ideea, dar merita contract separat pentru generator | Generare |
| `patch-planner.md` | Scannerul vanilla exista initial, dar lipseste specificatia pentru completarea lipsurilor | Generare |
| `addon-developer-guide.md` | API-ul exista, dar lipseste ghidul pentru cine scrie addonuri | API si addonuri |
| `scenario-pack-schema.md` | Feature packs exista, dar schema YAML nu este documentata ca referinta compacta | API si questuri |

## Prioritate mica

| Document propus | De ce lipseste | Categorie |
|---|---|---|
| `prompt-safety-guide.md` | Prompturile AI trebuie sa aiba reguli clare de context, limite si fallback | Quest, story si AI |
| `performance-notes.md` | Indexarea si task-urile periodice pot avea impact pe servere mari | Operare |
| `observability-and-logs.md` | Lipsesc conventii pentru loguri, debugdump si date sensibile | Operare |
| `worldedit-integration-contract.md` | Exista design, dar nu contract API clar pentru integrarea optionala | Generare |
| `test-fixtures-and-demo-world.md` | Lipseste descrierea unui demo world minimal pentru teste manuale repetabile | Operare |

## Ordine recomandata

1. `npc-world-bindings.md`
2. `server-admin-runbook.md`
3. `release-checklist.md`
4. `migration-si-backup.md`
5. `story-state-service.md`

Aceste cinci documente reduc cel mai mult riscul pentru urmatoarele faze, pentru ca proiectul tocmai a inceput sa lege mapping-ul de questuri si AI context.

## Acoperite recent

| Document | Status |
|---|---|
| `quest-anchor-bindings.md` | Creat dupa implementarea initiala a tabelei `quest_anchor_bindings`; auditul si comanda admin read-only exista initial, dar lipseste export/debugdump complet |
| `story-context-service.md` | Creat dupa implementarea initiala a `StoryContextService`; ramane necesara documentatia separata pentru story state persistent si events |
