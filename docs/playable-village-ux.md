# Playable Village UX

Actualizat: 2026-05-08

## Scop

Acest document fixeaza problemele observate in testarea de gameplay pentru primul sat jucabil. Pana aici s-au adaugat multe sisteme tehnice pentru quest, mapping, progression, GUI si rutina, dar experienta in joc ramane greu de citit daca satul, NPC-urile si interactiunile nu au reguli simple si vizibile.

Prioritatea imediata nu este inca o mecanica noua. Prioritatea este:

```text
sat lizibil
-> NPC-uri stabile
-> rutina clara
-> interactiuni clare
-> questuri vizibile si variate
```

## Probleme observate

### Interactiune NPC greoaie

Simptome:

- jucatorul nu intelege clar ce se intampla dupa click pe NPC;
- diferenta dintre dialog liber, quest status, accept si progres nu este destul de vizibila;
- GUI-ul si click-ul direct pe NPC nu explica suficient urmatorul pas;
- conversatia prin chat poate parea separata de quest/progression.

Directie:

- `NpcInteractionGui` trebuie sa devina ecran de actiuni clare, nu doar lista de NPC-uri;
- cardul NPC trebuie sa arate rol, rutina, quest disponibil/activ si actiunea recomandata;
- click dreapta pe NPC in lume ramane fluxul rapid pentru dialog/quest;
- GUI-ul trebuie sa ofere butoane clare: vorbeste, quest nearest, accept nearest, status, routine, story.

### Rutina NPC neclara

Simptome:

- NPC-ul nu pare sa aiba program;
- mutarile intre ancore pot parea arbitrare;
- adminul nu vede rapid de ce un NPC sta intr-un anumit loc;
- home/work/social exista in date, dar nu sunt suficient de vizibile in joc.

Ce exista initial:

- `RoutineEngine` are sloturi `HOME`, `WORK`, `SOCIAL`, `IDLE`;
- `RoutineService` foloseste home/work/social anchors;
- mutarile de rutina au primit cooldown si praguri mai mari;
- villagerii convertiti in AINPC primesc control de baza ca sa nu urmeze panica/pathing vanilla.

Directie:

- rutina trebuie sa aiba inspectie clara in GUI si comenzi;
- fiecare NPC permanent trebuie sa aiba home, work si social place explicite;
- rutinele trebuie sa fie predictibile pe intervale de timp, nu sa para salturi dese;
- auditul trebuie sa raporteze NPC fara home/work/social sau cu ancore prea apropiate.

### Case prea mici si sat inghesuit

Simptome:

- casele demo sunt percepute prea mici pentru spawn, familie si rutina;
- locurile sunt prea apropiate, deci NPC-urile par sa se suprapuna;
- piata, work place-urile si casele nu creeaza un traseu clar pentru jucator.

Ce exista initial:

- `/ainpc world demo create` foloseste acum un layout semantic mai spatios;
- casele demo sunt mai mari semantic decat versiunea initiala;
- piata, fieraria, ferma, taverna si altarul sunt mai distantate.

Limita importanta:

- mapping-ul demo marcheaza semantic locurile, dar nu construieste blocuri fizice;
- nu mareste case vanilla existente;
- nu niveleaza terenul.

Directie:

- generatorul fizic trebuie sa creeze case jucabile, nu doar mici volume semantice;
- fiecare casa trebuie sa aiba minim: intrare, pat, spawn interior/exterior, spatiu de conversatie;
- piata trebuie sa fie spatiu deschis, nu doar un punct;
- satul trebuie sa aiba distante intre gospodarii si drumuri evidente.

### Relief denivelat

Simptome:

- satul demo creat pe teren nepotrivit produce ancore la inaltimi incomode;
- jucatorul trebuie sa urce/coboare prea mult intre obiective;
- NPC-urile par rupte de lumea fizica daca ancorele sunt in teren denivelat.

Directie:

- comanda demo trebuie rulata intr-o zona relativ plata pana exista patch planner;
- generatorul viitor trebuie sa faca `terrain suitability check`;
- inainte de spawn, planul trebuie sa raporteze panta, obstacole, apa/lava si diferente mari de Y;
- patch planner-ul trebuie sa poata propune nivelare usoara, drumuri si platforme.

### NPC-uri care fug sau par haotice

Simptome:

- villagerii naturali convertiti pot pastra comportament vanilla daca nu sunt controlati;
- teleportul de rutina poate parea haotic daca pragurile sunt prea mici sau pathfinding-ul nu gaseste drum;
- lipsa de feedback face mutarea sa para bug, nu program.

Ce exista initial:

- NPC-urile create de plugin au AI si gravitatie active implicit, configurabil din `npc.natural_movement` si `npc.gravity`;
- villagerii existenti convertiti in AINPC primesc aceleasi setari controlate;
- startup-ul, restore-ul intarziat si sincronizarea periodica reaplica setarile controlate daca un villager ramane fara gravitatie sau fara AI dupa reload;
- rutina incearca pathfinding natural si foloseste teleport doar ca fallback/correctie.

Directie:

- pe termen scurt: stabilitate peste villageri controlati;
- pe termen mediu: miscari pe trasee simple intre ancore, nu teleport frecvent;
- pe termen lung: pathing controlat sau integrare cu navigator dedicat, dupa ce satul are drumuri si ancore curate.

## Definition of Playable Village

Un sat demo poate fi considerat jucabil doar daca:

- exista 3-5 NPC-uri cu roluri evidente;
- fiecare NPC are home/work/social clar;
- `/ainpc routine status` explica unde ar trebui sa fie NPC-ul si de ce;
- NPC-urile nu fug, nu se imping si nu sar frecvent intre ancore;
- casele si locurile de lucru sunt suficient de distantate pentru citire vizuala;
- piata are spatiu de interactiune si quest board vizibil semantic;
- terenul ales este relativ plat sau validat;
- questurile duc jucatorul intre locuri clare, nu doar la iteme;
- GUI-ul NPC arata actiunea urmatoare, nu doar informatii brute;
- dupa reload, mapping-ul, NPC bindings, rutina si quest progress raman coerente.

## Ordine de lucru recomandata

### PV1. Stabilizare experienta curenta

Status: `INCEPUT INITIAL`.

Livrabile:

- villageri AINPC controlati;
- rutina cu cooldown si praguri mai largi;
- `NpcInteractionGui` cu rutina si actiuni rapide;
- mapping demo semantic mai spatios;
- warning pentru zona plata.

### PV2. Inspectie si feedback in joc

Status: `URMATOR`.

Livrabile:

- GUI card NPC cu home/work/social si actiune recomandata;
- World GUI care arata daca regiunea este buna pentru demo;
- audit pentru ancore apropiate, case prea mici, Y variance si NPC fara bindings;
- mesaj clar cand nu exista quest disponibil sau mapping valid.

### PV3. Generator fizic minim

Status: `URMATOR / DUPA SMOKE PAPER`.

Livrabile:

- plan de sat cu distante minime;
- case fizice simple, mai mari decat vanilla minim;
- piata si drumuri simple;
- altar/fierarie/ferma/taverna cu spatiu de interactiune;
- patch planner pentru teren relativ plat.

### PV4. Rutina vizibila, nu doar teleport

Status: `DUPA PV2`.

Livrabile:

- trasee home/work/social;
- timp de stationare minim pe slot;
- stare vizuala/diagnostic pentru `HOME`, `WORK`, `SOCIAL`;
- audit pentru salturi prea dese.

### PV5. Questuri de demo legate de sat

Status: `DUPA PV2/PV3`.

Livrabile:

- primele questuri vizibile nu trebuie sa fie dominate de `collect_item`;
- Q06/T01/C02/D01/B01 trebuie sa fie accesibile rapid in sat demo;
- fiecare quest trebuie sa indice locul si motivul in GUI/status;
- story events trebuie sa fie verificabile dupa completare.

## Regula de prioritizare

Daca un task nou nu imbunatateste direct una dintre problemele de mai jos, se amana:

- claritatea interactiunii cu NPC;
- stabilitatea rutinei;
- spatierea satului;
- terenul/jucabilitatea locurilor;
- vizibilitatea progresului questului.

Mecanici noi fara aceste corectii vor creste codul, dar nu vor face demo-ul mai bun.
