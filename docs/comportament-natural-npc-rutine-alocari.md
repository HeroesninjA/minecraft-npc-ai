# Comportament natural pentru NPC

Actualizat: 2026-07-01

Acest document descrie cum facem NPC-urile sa se comporte natural in sat: casa clara, loc de munca clar, locuri de cumparat, drumuri logice, rutina zilnica si mai putin mers aleatoriu fara sens.

## Scop

Vrem ca NPC-ul sa nu mai fie doar o entitate care se misca random. Vrem un comportament care seamana cu un sat real:

- NPC-ul are o casa
- NPC-ul are sau nu are un loc de munca
- NPC-ul stie unde merge dimineata, la pranz si seara
- NPC-ul foloseste drumuri si carari, nu taie haotic prin lume
- magazinele pot fi si loc de munca, si loc de cumparat
- parcurile si locurile publice pot fi folosite pentru socializare si pauze

## Problema pe care o rezolvam

Fara alocari clare, NPC-ul face lucruri care par artificiale:

- merge random intre puncte fara relatie intre ele
- nu are un ciclu zilnic
- nu are legatura cu casa sau munca
- nu foloseste drumuri sau trasee naturale
- nu are motiv sa intre in magazin, ferma, fierarie sau parc

Rezultatul este un sat care pare gol sau scriptat prost.

## Modelul de baza

Fiecare NPC ar trebui sa aiba un set minim de ancore:

```text
home_place_id
work_place_id
social_place_id
home_node_id
work_node_id
social_node_id
routine_profile
```

Optional, pentru comportament mai bun:

```text
family_id
household_id
current_goal
last_task_type
routine_override_until
```

## Tipuri de locuri

### Casa

Casa este locul principal de repaus.

Roluri:

- somn
- intoarcere seara
- punct de start dimineata
- loc pentru NPC fara activitate urgenta

Reguli:

- fiecare NPC trebuie sa aiba o casa sau un fallback clar
- casa trebuie sa fie intr-o zona locuibila, nu in mijlocul activitatii industriale
- mai multi NPC pot sta in aceeasi casa daca modelul de sat permite asta

### Loc de munca

Locul de munca este locul unde NPC-ul petrece cea mai mare parte din zi.

Exemple:

- fierarie
- ferma
- magazin
- atelier
- han
- moara

Reguli:

- locul de munca trebuie ales dupa profesie
- NPC-ul trebuie sa mearga acolo in intervalul de lucru
- locul de munca trebuie sa aiba un node clar de interactiune, nu doar un `place`

### Magazin

Magazinul este special: poate fi si loc de munca, si loc de cumparat.

Exemple:

- comerciantul lucreaza in magazin
- clientii merg acolo pentru cumparaturi
- in afara programului, magazinul poate functiona ca loc social sau punct de trecere

Reguli:

- magazinul trebuie sa aiba un `work_node` pentru comerciant
- magazinul trebuie sa aiba un `customer_node` sau `interaction_node`
- NPC-urile nu trebuie sa intre in magazin fara un motiv: cumparaturi, munca, social sau traversare logica

### Fierarie

Fieraria este un loc de productie.

Comportament natural:

- fierarul merge dimineata la atelier
- are pauze scurte sau activitati secundare
- daca exista clienti, poate opri rutina scurta pentru interactiune
- se intoarce acasa seara

### Ferma

Ferma are ritm diferit fata de oras.

Comportament natural:

- NPC-ul pleaca spre camp dimineata
- poate face pauze la pranz
- seara revine la casa sau la gospodarie
- daca ferma este mare, poate avea mai multe node-uri de lucru

### Parc si locuri publice

Parcurile si spatiile publice sunt bune pentru viata sociala.

Roluri:

- plimbare
- pauza
- socializare
- asteptare
- tranzitie intre zona de casa si zona de munca

Reguli:

- NPC-ul poate merge in parc cand nu are task urgent
- parcul nu trebuie sa devina destinatie random fara sens
- parcul trebuie preferat ca loc de odihna sociala, nu ca inlocuitor universal al casei

## Drumuri si carari

Drumurile si cararile sunt esentiale pentru senzatia de sat viu.

Trebuie sa existe alocari clare pentru:

- drum intre casa si locul de munca
- drum intre casa si magazin
- drum intre locul de munca si puncte publice
- carari mici intre cladiri apropiate

Reguli:

- NPC-ul trebuie sa prefere drumurile marcate
- daca exista o carare directa si naturala, NPC-ul o foloseste
- daca nu exista drum clar, se creeaza un fallback de path logic
- NPC-ul nu trebuie sa taie prin gradini, pereti sau zone neimportante doar pentru ca ruta este mai scurta in linie dreapta

## Reguli de alocare a cladirilor

### Casa

Casa se aloca dupa:

- proximitate fata de locul de munca
- apartenenta la un household
- zona rezidentiala
- capacitate de rezidenti

### Loc de munca

Locul de munca se aloca dupa:

- profesie
- rol social
- disponibilitate de node-uri
- distanta rezonabila fata de casa

### Magazin

Magazinul poate avea doua roluri:

- workplace pentru comerciant
- shopping place pentru NPC-uri si jucatori

### Parc

Parcul se aloca ca:

- loc de tranzitie
- loc social
- loc de relaxare

### Cladiri publice

Cladirile publice pot fi:

- primarie
- han
- piata
- biserica
- sala comuna

Acestea pot deveni puncte de rutina, dar nu trebuie sa rupa logica casei si a muncii.

## Rutina zilnica recomandata

O rutina simpla si naturala este mai buna decat una foarte complexa.

### Dimineata

- NPC-ul pleaca de acasa
- merge spre locul de munca sau spre punctul de activitate
- foloseste drumuri naturale

### Pranz

- NPC-ul ia o pauza
- poate merge la magazin, parc sau acasa, in functie de distanta
- poate interactiona scurt cu alti NPC

### Dupa-amiaza

- revine la munca sau la activitatile secundare
- poate face comisioane daca are motivatie

### Seara

- se intoarce acasa
- reduce activitatea
- trece in mod social sau de repaus

### Noaptea

- NPC-ul ramane acasa sau in zona de odihna
- nu mai pleaca fara motiv clar
- doar evenimente speciale pot rupe rutina

## Cum evitam mersul aleatoriu

NPC-ul nu trebuie sa se plimbe fara obiectiv.

Reguli anti-randombwalk:

- fiecare NPC are un `current_goal`
- daca nu are task, alege cea mai apropiata ancora utila
- daca nu are munca, merge la casa, parc sau zona sociala
- schimba destinatia doar daca apare un motiv valid
- foloseste cooldown-uri pentru schimbari dese

Daca NPC-ul nu stie ce sa faca, fallback-ul trebuie sa fie logic:

1. casa
2. loc social apropiat
3. magazin sau piata
4. parc
5. idle controlat

## Prioritatea deciziilor

Cand mai multe sisteme vor sa controleze NPC-ul, ordinea trebuie sa fie clara:

```text
1. Safety / stare invalida
2. Override de poveste sau quest
3. Routine zilnica
4. Activitate sociala
5. Idle controlat
```

Asta previne conflictele intre AI, rutina si evenimente.

## Integrare cu sistemele existente

Documentul se leaga direct de:

- `docs/npc-world-bindings.md`
- `docs/rutine-npc-si-timeline.md`
- `docs/ordine-spawn-npc-cladiri-region-node.md`
- `docs/template-cladiri-si-marker-nodes.md`

Ce trebuie folosit in cod:

- `home_place_id` si `work_place_id`
- `home_node_id` si `work_node_id`
- `social_place_id` pentru socializare sau pauze
- `RoutineService` pentru decizia zilnica
- `WorldPlace` si node-urile semantice pentru destinatii

## Reguli practice pentru implementare

### 1. Nu porni de la random

NPC-ul trebuie sa primeasca mai intai context:

- unde locuieste
- unde lucreaza
- ce profesie are
- ce hora de rutina are

### 2. Nu folosi doar centrul cladirii

Trebuie folosite node-uri specifice:

- usa
- pat
- workstation
- counter
- poarta
- punct social

### 3. Nu trata toate cladirile la fel

O ferma, o fierarie si un magazin au comportamente diferite.

### 4. Nu rupe rutina pentru orice interactiune

Doar evenimente importante trebuie sa intrerupa programul.

### 5. Nu lasa NPC-ul sa repete aceeasi destinatie la infinit

Ai nevoie de:

- cooldown
- memorare a ultimei activitati
- preferinte de rutine

## Exemplu de flow natural

```text
Casa
-> drum principal
-> fierarie
-> pauza scurta
-> magazin sau parc
-> intoarcere acasa
```

Sau pentru comerciant:

```text
Casa
-> magazin
-> interactiune cu clienti
-> pauza la pranz
-> magazin
-> casa
```

Sau pentru fermier:

```text
Casa
-> carare spre ferma
-> lucru pe camp
-> pauza
-> lucru
-> casa
```

## Criterii de calitate

Comportamentul este bun daca:

- NPC-ul are scop clar in fiecare faza a zilei
- rutele par naturale
- locul de munca este legat de profesie
- magazinele sunt vii si utile
- parcurile si spatiile publice au rol real
- NPC-ul nu mai pare ca se plimba haotic

## Checklist de implementare

- [ ] fiecare NPC are casa sau fallback
- [ ] fiecare NPC are loc de munca sau o regula de idle
- [ ] magazinele au rol dublu: munca + cumparaturi
- [ ] drumurile si cararile sunt mapate
- [ ] exista `current_goal` si schimbari controlate
- [ ] exista rutina pe ore sau faze ale zilei
- [ ] exista override pentru evenimente si questuri
- [ ] exista inspectie pentru casa, munca si traseu

## Concluzie

Comportamentul natural nu vine din mai multa aleatoritate. Vine din alocari clare: casa, munca, magazin, parc, drumuri si rutina zilnica. Daca NPC-ul are un loc unde traieste, un loc unde lucreaza si un motiv clar pentru fiecare deplasare, el va parea parte din lume, nu un actor care se plimba fara sens.
