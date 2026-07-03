# Strategie pe termen lung: cod mai putin hardcoded, mai mult datadriven si generativ

Actualizat: 2026-07-01

Scopul pe termen lung este sa reducem hardcode-ul din logica de gameplay si sa mutam cat mai mult in:

- date declarative
- reguli configurabile
- generare controlata
- validare explicita
- audit si inspectie

## Principiul de baza

Nu eliminam complet hardcode-ul. Il pastram doar pentru lucrurile care trebuie sa fie stabile si sigure.

Hardcode ramas in core:

- siguranta
- validare
- prioritati
- persistenta critica
- pathfinding de baza
- protectie impotriva starii invalide

Tot ce tine de continut si comportament trebuie sa fie cat mai mult:

- configurabil
- extins prin date
- generat din reguli
- usor de schimbat fara editari in multe clase

## Ce trebuie scos treptat din hardcode

- ore fixe identice pentru toti NPC-ii
- questuri scrise manual in prea multe locuri
- rutine individuale codate per NPC
- evenimente narative adaugate one-by-one
- reguli duplicate in mai multe servicii
- trasee si alegeri statice fara context

## Ce trebuie pus in loc

### 1. Model semantic al lumii

Lumea trebuie descrisa prin obiecte clare:

- locuri
- noduri
- cladiri
- drumuri
- roluri
- zone sociale
- zone de munca

### 2. Bindings persistente

NPC-urile trebuie sa aiba legaturi stabile:

- casa
- loc de munca
- loc social
- familie
- household
- noduri de interactiune

### 3. Engine pe reguli si scoruri

In loc de multe `if`-uri fixe, folosim:

- conditii
- scoruri
- prioritati
- praguri
- cooldown-uri
- selectie bazata pe context

### 4. Generatoare de continut

Trebuie sa existe generare pentru:

- questuri
- evenimente
- dialoguri
- rutine
- activitati sociale
- spawn si alocari

### 5. Validatoare

Orice generare trebuie verificata inainte de aplicare:

- locul exista
- node-ul exista
- NPC-ul are voie
- contextul este valid
- nu se rupe o regula de siguranta

### 6. Override system

Evenimentele narative si questurile trebuie sa poata intrerupe rutina, dar controlat:

- cu expirare
- cu audit
- cu prioritate clara
- fara sa distruga starea de baza

## Arhitectura recomandata

```text
core rigid
-> content datadriven
-> rule engine
-> generator controlat
-> validation
-> audit
```

### Core rigid

Aici sta codul mic, stabil si greu de stricat:

- safety
- persistenta
- validare
- orchestration
- API intern

### Content datadriven

Aici stau datele schimbabile:

- profile de rutina
- tipuri de cladiri
- roluri
- weighted tables
- templates
- chestii locale pe sat sau regiune

### Rule engine

Aici se calculeaza ce trebuie sa faca sistemul:

- ce NPC merge la munca
- cand merge acasa
- unde ia pauza
- ce eveniment se poate genera
- ce quest are sens acum

### Generator controlat

Aici se produce continut nou din starea lumii:

- evenimente
- questuri
- propuneri de routine
- propuneri de spawn

### Validation

Aici se decide daca generarea e buna sau nu.

### Audit

Aici se poate explica de ce s-a ales o actiune.

## Strategia de migrare

### Faza 1: muta constantele in date

- ore
- praguri
- ponderi
- liste de roluri
- liste de locuri

### Faza 2: introdu profile declarative

- rutina pe rol
- stil de comportament
- preferinte de locatie
- toleranta la intarziere

### Faza 3: treci la reguli si scoruri

- nu mai alegi prin hardcode direct
- alegi prin evaluare de context

### Faza 4: adaugi generare

- questurile si evenimentele apar din semnale
- nu sunt scrise manual una cate una

### Faza 5: adaugi fallback-uri standard

- daca lipseste ceva, sistemul cade intr-o stare sigura
- nu blocheaza NPC-ul

### Faza 6: elimini duplicarea

- aceeasi regula nu trebuie sa existe in 3 servicii diferite
- logica comuna se extrage in helperi sau servicii dedicate

## Reguli practice

### AI-ul propune, runtime-ul valideaza

AI-ul nu trebuie sa scrie direct in lumea vie fara control.

### Datele descriu, codul executa

Nu programezi manual fiecare detaliu al satului.

In loc de asta:

- definesti date
- definesti reguli
- definesti generatoare
- lasi runtime-ul sa execute

### Foloseste extensie, nu multiplicare

Daca apare un nou tip de sat sau o noua profesie, nu copia acelasi cod.
Extinzi datele si regulile.

## Ce inseamna succesul

Sistemul este bun daca:

- satul poate fi schimbat prin date, nu prin refactor mare
- NPC-urile se comporta diferit fara sa aiba cod separat
- questurile si evenimentele apar din contextul lumii
- se poate inspecta fiecare decizie
- se pot adauga noi tipuri de continut fara hardcode extensiv

## Concluzie

Pe termen lung, directia corecta este:

- core rigid pentru siguranta si consistenta
- date declarative pentru continut
- reguli si scoruri pentru decizie
- generare controlata pentru dinamica
- audit pentru explicabilitate

Asta reduce hardcode-ul, creste flexibilitatea si face lumea mai naturala fara sa programezi manual fiecare detaliu.
