# Harta claselor pentru NPC

Actualizat: 2026-06-21

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `NPC`: entitatea persistenta, managerul, contextul, personalitatea, emotiile, starea si relatiile.

Nu este un inventar complet al tuturor claselor NPC. Este o harta de lucru pentru nodurile care definesc identitatea si comportamentul NPC-ului.

## Noduri principale

- `AINPC` -> modelul principal de NPC persistent si bridge catre entity
- `NPCManager` -> registrul si coordonarea NPC-urilor active
- `NPCContext` -> context operational: lume, interactiuni, stare si semnale
- `NPCPersonality` -> trasaturi stabile si afinitati
- `NPCEmotions` -> stare emotionala, decays si descrieri
- `NPCState` -> starea operationala curenta
- `NPCAction` -> actiunile pe care NPC-ul le poate face
- `NPCRelationship` -> relatia dintre NPC si alti actori
- `NpcVillageSnapshot` -> snapshot de sat / populatie pentru inspectie
- `NpcRepairCounters` -> contori pentru operatii de repair / audit
- `NpcFactResolver` -> extrage fapte si intentii utile pentru AI si dialog

## Flux principal

`NPCManager` gestioneaza ciclul de viata al NPC-urilor.

`AINPC` tine identitatea persistenta si sincronizarea cu entity.

`NPCContext` aduna semnale de lume si interactiune.

`NPCPersonality`, `NPCEmotions` si `NPCState` descriu comportamentul curent.

`NPCRelationship` si `NpcFactResolver` conecteaza NPC-ul la dialog si AI.

## Relatii utile

- `AINPC` este nodul central pentru identitate, locatie si stare persistenta.
- `NPCManager` se ocupa de incarcarea, salvarea, repararea si auditul NPC-urilor.
- `NPCContext` este stratul de semnale pentru logicile care trebuie sa inteleaga lumea.
- `NPCPersonality` si `NPCEmotions` influenteaza dialogul si reactiile.
- `NPCState` si `NPCAction` definesc ce poate face NPC-ul in momentul curent.
- `NpcVillageSnapshot` si `NpcRepairCounters` sunt utile pentru inspectie si hardening.

## Cum se citeste

1. Incepe cu `AINPC`.
2. Continua cu `NPCManager`.
3. Treci la `NPCContext`.
4. Urmareste `NPCPersonality`, `NPCEmotions` si `NPCState`.
5. Foloseste `NpcFactResolver` si `NPCRelationship` cand vrei legatura cu AI sau dialog.

## Nota

Daca vrei doar traseul intre module si pachete, foloseste harta de pachete. Daca vrei relatiile dintre clasele NPC, acesta este documentul potrivit.
