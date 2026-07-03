# Cum evitam comportamentul robotic in rutinele NPC

Actualizat: 2026-07-01

Problema: daca toti NPC-urile merg la munca si pleaca in acelasi moment, satul pare artificial si sincronizat prea perfect.

Solutia nu este random mare. Solutia este variatie controlata.

## Principiul de baza

Satul trebuie sa aiba ritm, nu sincronizare.

Asta inseamna:

- NPC-urile nu au aceeasi ora fixa
- fiecare NPC are un offset stabil
- exista o variatie mica zilnica
- rolurile diferite au program diferit
- unele NPC-uri intarzie sau pleaca mai devreme in mod natural

## Ce trebuie sa eviti

- toti la munca la aceeasi ora
- toti ies de la munca in acelasi minut
- toti merg direct acasa in acelasi pattern
- toti folosesc acelasi interval de pauza
- toate gospodariile urmeaza acelasi ceas

Daca faci asta, satul arata ca o fabrica, nu ca o comunitate.

## Ce trebuie sa faci

### 1. Interval, nu moment fix

Foloseseste ferestre de timp, nu un timestamp exact.

Exemple:

- fermier: 06:00-07:30 start
- fierar: 08:00-09:30 start
- comerciant: 09:00-11:00 start
- garda: schimburi diferite

### 2. Offset stabil per NPC

Fiecare NPC primeste un decalaj persistent:

```text
final_time = template_time + stable_offset + daily_jitter
```

Offset-ul trebuie sa fie acelasi pe termen lung, ca NPC-ul sa para consecvent.

### 3. Jitter mic zilnic

Adauga doar o abatere mica:

- ±5 minute pentru NPC-uri foarte disciplinate
- ±10-15 minute pentru NPC-uri normale
- mai putin pentru roluri rigide, mai mult pentru roluri flexibile

### 4. Diferenta pe rol

Nu toti locuitorii au acelasi program.

Exemple:

- fermierii incep mai devreme
- fierarii si artizanii au program de atelier
- comerciantii au program de deschidere si inchidere
- locuitorii simpli au mai multa libertate

### 5. Plecari si intoarceri in valuri

Nu inchide si nu deschide satul in bloc.

Trebuie sa existe:

- unii NPC-uri care pleaca mai devreme
- unii care stau peste program
- unii care fac o oprire la magazin
- unii care merg la parc sau la social
- unii care se intorc direct acasa

### 6. Foloseste contextul real

Intarzierile si decalajele trebuie sa vina din situatii credibile:

- client prezent
- stoc de marfa
- casa aglomerata
- vreme
- drum mai lung
- eveniment local
- pauza sociala

Nu folosi intarzieri arbitrare fara semnificatie.

## Model recomandat

```text
routine_profile = role_schedule(role)
stable_offset = npc_personality_offset(npcId)
daily_jitter = small_variation(daySeed)
final_action_time = routine_profile.base + stable_offset + daily_jitter
```

## Regula buna pentru sat

- dimineata: ies pe rand
- la pranz: se misca doar o parte
- seara: se intorc in valuri
- noaptea: majoritatea raman acasa

## Exemplu de comportament natural

```text
fermieri -> pleaca devreme, se intorc treptat
fierari -> au pauze si clienti
comercianti -> deschid tarziu si inchid mai tarziu
locuitori -> merg pe la magazin, parc sau acasa
```

## Criteriu de calitate

Comportamentul este bun daca:

- satul are viata, dar nu haos
- NPC-urile nu sunt sincronizate perfect
- orele sunt previzibile, dar nu identice
- traseele si activitatile par firesti
- satul nu seamana cu un script repetat

## Concluzie

Ca sa nu para robotic, satul are nevoie de ritm variabil, nu de aleatoriu pur. Programul trebuie sa fie semnat de rol, offset si context, astfel incat fiecare NPC sa para ca traieste in lume, nu ca executa aceeasi comanda la un ceas comun.
