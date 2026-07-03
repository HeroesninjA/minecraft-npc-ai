# Ce mai trebuie pentru un sat suficient de semantic

Actualizat: 2026-07-01

Acest document rezuma ce mai lipseste ca satul sa fie suficient de semantic incat:

- actiunile NPC-urilor sa para naturale
- rutina zilnica sa aiba sens
- evenimentele sa poata aparea din starea lumii
- questurile sa poata fi generate din context, nu din random

Documente legate:

- `docs/comportament-natural-npc-rutine-alocari.md`
- `docs/npc-world-bindings.md`
- `docs/rutine-npc-si-timeline.md`
- `docs/ordine-spawn-npc-cladiri-region-node.md`
- `docs/template-cladiri-si-marker-nodes.md`

## 1. Ce inseamna "sat semantic"

Un sat este semantic cand fiecare element important are rol clar si poate fi inteles de runtime:

- casa
- loc de munca
- magazin
- ferma
- fierarie
- parc
- drum
- carare
- loc social
- cladire publica
- nod de intrare / iesire / interactiune

Nu ajunge sa existe blocuri in lume. Trebuie sa existe semnificatie functionala.

## 2. Ce mai trebuie facut

### 2.1 Taxonomie completa de locuri

Fiecare `place` trebuie etichetat clar:

```text
home
work
shop
social
park
road
public
farm
smithy
inn
market
```

Fara asta, AI-ul si rutina nu pot face alegeri coerente.

### 2.2 Bindings persistente pentru NPC

Fiecare NPC trebuie sa aiba legaturi stabile:

```text
home_place_id
work_place_id
social_place_id
home_node_id
work_node_id
social_node_id
household_id
family_id
```

Asta evita navigarea random si permite revenirea la un program coerent.

### 2.3 Rutina zilnica pe faze

NPC-ul trebuie sa aiba faze de comportament:

- dimineata: plecare de acasa
- zi: munca sau activitate principala
- pranz: pauza, magazin, parc sau social
- seara: intoarcere acasa
- noapte: repaus

Ritmul trebuie sa fie stabil, nu schimbat la fiecare tick.

### 2.4 Reguli de alegere a actiunii

NPC-ul trebuie sa aleaga ce face pe baza de:

- profesie
- distanta
- ora din zi
- nevoie curenta
- rol social
- evenimente active
- override de quest sau poveste

Nu trebuie sa aleaga random dintr-o lista fara sens.

### 2.5 Drumuri si carari semantice

Trebuie sa existe trasee clare intre:

- casa si munca
- casa si magazin
- munca si locuri publice
- zone rezidentiale si zone comerciale

NPC-ul trebuie sa prefere drumurile marcate, nu sa traverseze haotic satul.

### 2.6 Cladiri cu rol dublu unde are sens

Unele cladiri trebuie sa aiba doua roluri:

- magazin = loc de munca + loc de cumparat
- han = loc social + loc de tranzit
- piata = loc comercial + loc social
- parcul = loc de relaxare + loc de asteptare

Asta creeaza viata in sat fara sa dubleze artificial sistemele.

### 2.7 Evenimente generate din starea lumii

Evenimentele trebuie sa fie produse din semnale reale:

- stoc mic la magazin
- recolta gata
- fierarie aglomerata
- NPC fara job
- familie separata
- drum blocat
- sarbatoare locala
- conflict intre roluri sau zone

Daca nu exista semnal semantic, evenimentul pare arbitrar.

### 2.8 Questuri generate din lipsuri si oportunitati

Questurile bune apar din context:

- lipsa marfii
- nevoie de livrare
- reparatii la cladirii
- protectia fermei
- cerere din partea unui comerciant
- ajutor pentru un NPC ocupat

Questul trebuie sa aiba legatura cu locul, rolul si starea satului.

### 2.9 Fallback-uri controlate

Daca lipseste ceva, NPC-ul nu trebuie sa se blocheze.

Fallback recomandat:

1. casa
2. loc social apropiat
3. magazin / piata
4. parc
5. idle controlat

Fallback-ul trebuie sa fie inspectabil.

### 2.10 Audit si inspectie

Trebuie sa poata fi verificat rapid:

- unde locuieste NPC-ul
- unde lucreaza
- ce ruta foloseste
- ce routine profile are
- ce override are activ
- de ce a fost generat un quest sau eveniment

Fara audit, sistemul semantic nu poate fi validat.

## 3. Ordinea corecta de lucru

### Faza 1: semantic map

Rezultat:

- locurile sunt etichetate corect
- node-urile sunt reale si utile
- drumurile sunt mapate

### Faza 2: NPC bindings

Rezultat:

- casa, munca si locurile sociale sunt persistente
- NPC-ul are ancore stabile

### Faza 3: routine engine

Rezultat:

- NPC-ul urmeaza o zi normala
- comportamentul nu mai este random

### Faza 4: event generator

Rezultat:

- lumea produce semnale naturale
- evenimentele apar din stare, nu din script mut

### Faza 5: quest generator

Rezultat:

- questurile apar din lipsuri, roluri si conflicte reale

### Faza 6: audit si tuning

Rezultat:

- sistemul poate fi inspectat, reparat si ajustat fara ghicit

## 4. Criterii de finalizare

Satul este suficient de semantic cand:

- fiecare NPC are casa si, daca are sens, loc de munca
- magazinele functioneaza ca loc de cumparat si loc de lucru
- parcurile si locurile publice au rol real
- rutele sunt naturale si repetabile
- NPC-urile nu mai umbla fara motivatie
- evenimentele si questurile pot fi generate din starea satului
- fiecare decizie poate fi explicata prin context

## 5. Concluzie

Daca vrei comportament natural, nu trebuie sa adaugi mai multa aleatoritate. Trebuie sa adaugi mai multa semnificatie.

Ordinea corecta este:

```text
semantic map
-> persistent bindings
-> daily routine
-> event generation
-> quest generation
-> audit
```

Cand aceste straturi exista, satul incepe sa para viu si coerent, iar NPC-urile nu mai merg fara sens.
