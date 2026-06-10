# Codex 250 Demo Batch 03

Data: 2026-05-26
Status: PASS

## Task-uri Selectate

Batch-ul 03 acopera local task-uri din zona `world`:

- T031 | world | verify | `/ainpc world places demo_sat`
- T032 | world | verify | `/ainpc world demo create demo_sat`
- T033 | world | verify | minim 5 places
- T034 | world | verify | minim 10 nodes
- T035 | world | verify | minim 3 case
- T036 | world | verify | minim 1 workplace
- T037 | world | verify | minim 1 social hub
- T038 | world | inspect | tags pentru case demo
- T039 | world | inspect | tags pentru forge
- T040 | world | inspect | tags pentru farm
- T041 | world | inspect | tags pentru market
- T042 | world | inspect | tags pentru tavern
- T043 | world | inspect | tags pentru altar
- T045 | world | docs | demo mapping nu construieste blocuri
- T048 | world | restart | places persist dupa reload config
- T049 | world | restart | nodes persist dupa reload config

## Implementare

Extins `WorldAdminServiceTest` pentru mapping-ul demo:

- verifica metadata `source=demo_mapping`;
- verifica roluri si profesii pentru market, forge, farm, tavern si altar;
- verifica tag-uri rituale/sacre pentru altar;
- verifica metadata semantic pentru `quest_board`, `ritual_circle`, `work_1` si `bed_1`;
- verifica alocarea automata `demo_sat_2` cand `demo_sat` exista deja;
- confirma persistenta prin save/reload config deja acoperita in testul demo.

## Comenzi Rulate

```powershell
.\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.world.WorldAdminServiceTest
```

Rezultat: PASS

## Paper

Nu s-a rulat server Paper real in acest batch. Validarea este unitara pe `WorldAdminService`.

## Urmatorul Batch Recomandat

Urmatorul batch poate acoperi `settlement` T051-T070, folosind `HouseAllocationPlannerTest` si testele de spawn/bindings existente.
