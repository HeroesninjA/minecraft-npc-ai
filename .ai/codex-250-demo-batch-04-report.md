# Codex 250 Demo Batch 04 Report

Data: 2026-05-26
Zona: settlement / household planner
Tip: experimental local, fara Paper live

## Task-uri acoperite

| ID | Status | Acoperire |
| --- | --- | --- |
| T051 | PASS | `HouseAllocationPlannerTest.plansSettlementFromAllDemoHouses` valideaza plan complet pentru `demo_sat`. |
| T053 | PASS | Planul local produce 4 NPC/resident plans inspectabile pentru casele demo. |
| T054 | PASS | Fiecare resident generat este legat de un place demo prin `homePlaceId` implicit al alocarii. |
| T055 | PASS | Fiecare resident are `homeNodeId` si `bedNodeId` neblank in casa proprie. |
| T056 | PASS | Fiecare resident are `workPlaceId=demo_sat:fierarie` si `workNodeId=demo_sat:fierarie:work_1`. |
| T057 | PASS | Fiecare resident are `socialPlaceId=demo_sat:piata` si `socialNodeId=demo_sat:piata:meeting_point_1`. |
| T058 | PASS | `planSettlement(..., maxHouses=2)` intoarce primele 2 case si warning clar despre limita. |
| T067 | PASS | Teste locale dedicate verifica planner-ul cu mapping-ul demo generat de `WorldAdminService`. |
| T069 | PASS | `primaryOwnerNpcKey`, `maxResidents` si capacitatea pe node-uri sunt validate pentru coerenta household. |
| T070 | PASS | Rolurile generate sunt verificate ca `resident` pentru casele demo cu un singur slot valid. |

## Ce a fost schimbat

- Extins `HouseAllocationPlannerTest` cu verificari pentru capacitate, subset de case, ancore home/work/social si selectori invalizi.
- Validat ca cererea de 4 rezidenti pe `demo_sat:house_1` este taiata la capacitatea reala: `maxResidents=2`, dar doar 1 pereche spawn/home disponibila.
- Validat ca planner-ul nu produce alocari pentru regiuni/case inexistente si intoarce mesaje clare.

## Test executat

```text
.\gradlew.bat :ainpc-core-plugin:test --tests ro.ainpc.world.HouseAllocationPlannerTest
BUILD SUCCESSFUL
```

## Limite ramase

- T052, T059, T060, T061, T062, T063, T064 si T065 raman partiale sau neacoperite deoarece cer Paper live, spawn real, restart sau repair commands.
- Acest batch intareste partea determinista locala, nu executia live pe server.
