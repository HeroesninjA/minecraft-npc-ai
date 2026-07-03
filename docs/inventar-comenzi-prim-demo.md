# Inventar Comenzi pentru Primul Demo AINPC

Actualizat: 2026-07-01

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## Scop

Inventarul complet al comenzilor `/ainpc` necesare pentru fiecare faza D0-D9.

## D1: Build & Config

| Comanda | Scop |
|---------|------|
| /ainpc | Verifica pluginul raspunde |
| /plugins | Verifica AINPC si addonul medieval sunt enabled |
| /ainpc audit db | Verifica integritatea bazei de date |

## D2: Mapping demo_sat

| Comanda | Scop |
|---------|------|
| /ainpc world demo create demo_sat | Genereaza harta satului demo |
| /ainpc world whereami | Arata locatia curenta |
| /ainpc world places demo_sat | Listeaza locurile din regiune |
| /ainpc world region info demo_sat | Detalii regiune |
| /ainpc world save | Salveaza harta |
| /ainpc audit world | Audit complet al lumii |
| /ainpc wand | Activeaza wand-ul pentru mapping manual |
| /ainpc map <descriere> | Creeaza draft mapping din selectia curenta |
| /ainpc map preview | Arata sumarul si preview-ul vizual al draftului |
| /ainpc map edit | Deschide GUI-ul pentru draftul curent |
| /ainpc world create ai preview region name=demo_extensie type=settlement size=48 | Genereaza draft AI/intent fara sa deschida formularul |
| /ainpc map confirm | Confirma draftul in runtime dupa verificare |

## D3: NPC Population & Bindings

| Comanda | Scop |
|---------|------|
| /ainpc world settlement plan demo_sat 5 | Planifica casele |
| /ainpc world settlement spawn demo_sat 5 | Populeaza satul |
| /ainpc list | Lista NPC-uri |
| /ainpc world bindings <npc> | Verifica legaturile NPC |
| /ainpc audit npc | Audit NPC |
| /ainpc audit spawn | Audit spawn |

## D4: Routine & UX

| Comanda | Scop |
|---------|------|
| /ainpc routine status nearest | Status rutina NPC curent |
| /ainpc routine tick | Tick manual de rutina |
| /ainpc info nearest | Informatii NPC curent |
| /ainpc quest nearest | Quest-ul NPC-ului curent |
| /ainpc gui | Deschide hub-ul GUI |
| /ainpc gui quest all | GUI quest-uri |
| /ainpc gui story | GUI story |
| /ainpc gui world | GUI world |
| /ainpc gui routine | GUI rutina |

## D5: Quest + Progression

| Comanda | Scop |
|---------|------|
| /ainpc quest log | Quest log |
| /ainpc quest accept nearest | Accepta quest-ul curent |
| /ainpc quest status nearest | Status quest curent |
| /ainpc quest track start | Incepe tracking |
| /ainpc quest anchors | Arata ancorele quest-ului |
| /ainpc progression definitions | Lista mecanici de progres |
| /ainpc progression stored <player> | Progresia jucatorului |
| /ainpc contract definitions | Contracte disponibile |
| /ainpc bounty definitions | Bounty-uri disponibile |
| /ainpc duty definitions | Sarcini disponibile |
| /ainpc event definitions | Evenimente disponibile |
| /ainpc tutorial definitions | Tutoriale disponibile |
| /ainpc ritual definitions | Ritualuri disponibile |
| /ainpc audit quest | Audit quest-uri |

## D6: Story Context

| Comanda | Scop |
|---------|------|
| /ainpc story context | Context narativ curent |
| /ainpc story context <player> nearest | Context narativ player |
| /ainpc story region demo_sat | Starea povestii pe regiune |
| /ainpc story place <placeId> | Starea povestii pe loc |
| /ainpc story events | Evenimente de poveste |
| /ainpc debugdump story | Export story |

## D7: Dialog & AI Fallback

| Comanda | Scop |
|---------|------|
| Click dreapta pe NPC | Deschide dialogul |
| /ainpc info nearest | Verifica NPC-ul inainte de dialog |

## D8: Restart & Persistenta

| Comanda | Scop |
|---------|------|
| /ainpc audit all | Audit complet |
| /ainpc debugdump all | Export complet |
| /ainpc demo definition | Definitia demo-ului |
| /ainpc demo status demo_sat | Status demo |
| /ainpc demo next demo_sat | Urmatorii pasi |
| /ainpc demo phases demo_sat <player> | Fazele demo |
| /ainpc demo script demo_sat <player> | Script demo |
| /ainpc demo evidence demo_sat <player> | Dovezi milestone |
| /ainpc demo runbook demo_sat <player> | Ghid operare |
| /ainpc demo smoke demo_sat <player> | Verificare rapida |
| /ainpc demo summary demo_sat <player> | Rezumat |
| /ainpc demo commands demo_sat <player> | Lista comenzi |
| /ainpc demo restart demo_sat | Gate restart |

## D9: Script Demo Final

| Comanda | Scop |
|---------|------|
| Toate comenzile de mai sus | Script complet |
| /ainpc demo status demo_sat | Status final |
