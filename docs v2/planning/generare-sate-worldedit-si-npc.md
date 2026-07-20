# Propunere: generare de sate cu WorldEdit si NPC-uri

Status: propunere compatibila, neimplementata.
Actualizat: 2026-07-15.

Aceasta directie ramane valida pentru o faza viitoare de constructie fizica. Lipsa adaptorului actual nu inseamna ca ideea este abandonata.

## Baseline curent

- mapping-ul, planificarea `HouseAllocation` si spawn-ul exista;
- `SettlementGenerationPlan` si modurile `WORLDEDIT_TEMPLATE` exista doar ca scaffold sau capability placeholders;
- AINPC nu are adaptor WorldEdit, loader de schematic, paste, rotire sau undo;
- fluxurile curente de auto-place si patch creeaza numai mapping semantic.

## Pipeline propus

1. selecteaza o intentie de settlement si un plan spatial versionat;
2. valideaza capabilitatea WorldEdit, lumea, chunk-urile, coliziunile si limitele;
3. produce preview/dry-run inspectabil;
4. cere confirmare administrativa explicita;
5. executa constructia fizica printr-un adaptor optional;
6. verifica rezultatul si persista un record de executie plus undo;
7. creeaza sau confirma mapping-ul semantic;
8. genereaza `HouseAllocation`, ruleaza dry-run-ul de spawn si apoi spawn-ul;
9. salveaza binding-urile si auditul final.

## Reguli de compatibilitate

- core-ul decide semantica si validarea; adaptorul executa numai operatii de lume;
- WorldEdit ramane optional, fara tipuri WorldEdit in API-ul public generic;
- mapping-ul nu se confirma inaintea succesului fizic decat daca exista compensare;
- un esec de spawn nu trebuie sa stearga automat constructia fara politica explicita;
- AI poate propune un draft, dar nu executa blocuri sau spawn direct;
- fallback-ul fara WorldEdit trebuie definit ca executor real separat, nu presupus.

## Gate-uri pentru implementare

- DTO spatial ales si versionat;
- adaptor optional cu detectie de capabilitate;
- preview, aprobare, rezultat persistent si undo testat;
- strategie tranzactionala intre build, mapping si spawn;
- teste cu WorldEdit absent, prezent si esuat partial.

## Legaturi

- `architecture/worldedit-integration-contract.md`
- `planning/schema-authoring-structuri-world.md`
- `planning/generare-ai-si-constructie-automata.md`
- `guides/ordine-spawn-npc-cladiri-region-node.md`

