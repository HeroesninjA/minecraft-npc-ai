# Generarea structurii satului si orasului

Actualizat: 2026-07-01

Acest document descrie cum generam o structura coerenta pentru sat sau oras, astfel incat lumea sa contina:

- case si locuinte
- cladiri de profesie
- cladiri de utilitate
- cladiri de timp liber
- structuri publice
- structuri in afara satului
- structuri pentru questuri
- structuri exterioare

Scopul este ca fiecare constructie sa aiba sens semantic si rol de gameplay, nu doar forma geometrica.

## 1. Principiul de baza

O lume buna nu se construieste ca o colectie de cladiri separate. Se construieste ca un sistem de locuri conectate.

Fiecare obiect trebuie sa aiba:

- tip semantic
- rol functional
- conectivitate prin drumuri sau carari
- noduri de interactiune
- relatie cu NPC-urile
- relatie cu questurile sau story-ul

## 2. Zonele principale

### 2.1 Zona rezidentiala

Aici intra locuintele si casele NPC-urilor.

Exemple:

- case simple
- gospodarii
- apartamente de oras
- case de familie
- locuinte comune
- han cu camere

Reguli:

- locuinta trebuie sa fie aproape de rutina zilnica a NPC-ului
- zonele rezidentiale trebuie sa fie separate de zona grea de productie, daca satul sau orasul este mare
- locuintele trebuie sa aiba node-uri clare: usa, pat, depozit, curte, iesire

### 2.2 Zona de profesie

Aici intra cladirile in care NPC-urile lucreaza.

Exemple:

- fierarie
- ferma
- magazin
- atelier
- brutarie
- moara
- tamplarie
- han
- primarie
- templu
- atelier de croitorie

Reguli:

- fiecare cladire de profesie trebuie sa aiba un rol clar
- profesia trebuie sa se lege de comportamentul NPC-ului
- o cladire poate avea mai multe node-uri de lucru, dar trebuie sa existe un work node principal
- distanta fata de locuinte trebuie sa fie rezonabila

### 2.3 Zona de utilitate

Aici intra structurile care fac satul functionabil.

Exemple:

- fantana
- depozit
- hambar
- poarta
- pod
- trotuar / carare
- statie de caruta
- grajd
- punct de colectare
- iluminat public

Reguli:

- aceste structuri nu sunt doar decor
- ele trebuie sa participe la fluxul zilnic
- unele trebuie sa poata fi folosite de questuri sau evenimente

### 2.4 Zona de timp liber

Aici intra locurile unde NPC-urile se relaxeaza sau socializeaza.

Exemple:

- parc
- piata
- terasa
- han
- banci
- gradina publica
- loc de foc comun
- loc de spectacol

Reguli:

- zona de timp liber trebuie sa fie accesibila pe jos
- nu trebuie sa intre in conflict cu zona de munca
- poate fi folosita pentru pauze, socializare si mici evenimente

### 2.5 Structuri publice

Structurile publice sunt pentru administratie, ritual, ordine sau viata comuna.

Exemple:

- primarie
- tribunal
- biserica
- sala comuna
- centru civic
- biblioteca
- turn de paza
- camp de antrenament

Reguli:

- ele trebuie sa aiba rol social sau structural
- nu trebuie tratate ca simple decoruri
- pot deveni puncte de quest sau story

## 3. Structuri in afara satului

Structurile exterioare trebuie sa fie separate semantic de satul central, dar conectate la el.

Exemple:

- padure
- ruine
- pesteri
- tabere
- forturi
- drumuri comerciale
- poduri
- puncte de observatie
- ferme izolate
- santinele
- altar
- fantani uitate

## 4. Structuri pentru questuri

Acestea sunt structuri care exista pentru a sustine gameplay-ul si progresia.

Exemple:

- notice board
- altar stravechi
- criptă
- turn abandonat
- casa izolata
- tabara de banditi
- depozit ascuns
- pivnita secreta
- punct de livrare
- poarta inchisa
- ruina investigabila

Reguli:

- trebuie sa aiba scop clar
- trebuie sa aiba entry nodes si interaction nodes
- trebuie sa poata fi folosite de quest/story
- nu trebuie sa fie doar "locuri interesante" fara functionalitate

## 5. Structuri exterioare

Structurile exterioare sunt locuri in afara nucleului satului sau orasului, dar mapate semantic.

Tipuri frecvente:

- wilderness
- camp
- ruin
- dungeon
- outpost
- shrine
- cave
- border watch
- caravan stop
- hidden camp

Acestea trebuie tratate ca entitati de gameplay, nu ca decor gol.

Pentru o schema dedicata structurilor exterioare vezi:

- `docs/structuri-exterioare-satului.md`
- `docs/mediu-test-controlat-sat-si-structuri-exterioare.md`
- `docs/schema-scenariu-predefinit-testare.md`

## 6. Cum generam o structura buna

### Pasul 1: alegem tipul de asezare

Satul si orasul nu se genereaza la fel.

- satul are densitate mai mica, trasee mai simple, case mai multe si activitate agricola
- orasul are densitate mai mare, cartiere, utilitati, cladiri publice si profesii specializate

### Pasul 2: definim districtele

Un layout bun are zone clare:

- rezidential
- comercial
- productie
- administrativ
- social
- periferic
- exterior

### Pasul 3: plasam cladirile importante

Ordinea recomandata:

1. casa sau locuinte
2. cladirile de profesie
3. utilitatile
4. spatiile publice
5. zonele de timp liber
6. structurile externe
7. structurile de quest

### Pasul 4: conectam prin drumuri si carari

Fiecare loc important trebuie sa aiba acces semantic:

- casa -> munca
- casa -> magazin
- casa -> parc
- centru -> periferie
- sat -> structuri exterioare
- oras -> zone comerciale si administrative

### Pasul 5: adaugam noduri

Fiecare structura trebuie sa aiba node-uri utile:

- door
- bed
- workbench
- counter
- storage
- meeting point
- entrance
- exit
- quest trigger
- inspection point

### Pasul 6: validam

Nu acceptam o structura doar pentru ca arata bine.

Trebuie sa verificam:

- exista sens semantic
- exista accesibilitate
- exista relatie cu NPC sau quest
- nu blocheaza drumul
- nu suprascrie alte zone importante

## 7. Model de date recomandat

Pentru fiecare structura importanta folosim un set minim de campuri.

```text
id
type
displayName
category
parentRegion
tags
entryNodes
interactionNodes
questAnchors
dangerLevel
faction
spawnPolicy
requiresValidation
```

### Categorii utile

```text
residential
profession
utility
leisure
public
outside
quest
exterior
```

### Exemple de `type`

```text
house
farm
smithy
shop
inn
market
park
well
bridge
ruin
dungeon
outpost
shrine
watchtower
quest_board
```

## 8. Diferenta intre sat si oras

### Sat

- mai putine cladiri mari
- mai multe case si ferme
- spatii deschise mai multe
- drumuri mai simple
- utilitati mai rare
- public spaces mai mici

### Oras

- mai multa densitate
- mai multe cladiri de profesie
- district comercial clar
- administratie vizibila
- spatii publice mai multe
- trafic mai complex

## 9. Reguli pentru NPC si rutina

Structura trebuie sa permita comportament natural.

Asta inseamna:

- casa aproape de rutina zilnica
- munca legata de tipul cladirii
- spatii publice pentru pauze
- drumuri care par naturale
- locuri de cumparat separate de locuri de munca, dar pot coincide unde are sens

## 10. Reguli pentru questuri

Structurile pentru questuri trebuie sa rezolve nevoi reale ale lumii.

Exemple:

- casa izolata poate ascunde un NPC important
- fantana uitata poate fi loc de investigatie
- ruina poate contine un indiciu
- turnul de paza poate declansa o misiune de alerta
- magazinul poate genera o lipsa de marfa
- ferma poate genera un quest de recoltare sau livrare

Questurile bune ies din context, nu din decor arbitrar.

## 11. Reguli pentru structuri exterioare

Structurile exterioare trebuie:

- sa fie conectate semantic la sat sau oras
- sa aiba un motiv sa existe
- sa fie accesibile prin drumuri, poteci sau trasee
- sa aiba entry nodes si interaction nodes
- sa poata deveni target pentru quest/story

## 12. Ordinea buna de implementare

1. definim taxonomia de structuri
2. definim template-urile pentru case si cladiri
3. definim utilitatile si spatiile publice
4. definim structurile exterioare
5. definim quest structures
6. conectam toate prin drumuri si node-uri
7. validam si auditam rezultatul

## 13. Criterii de calitate

Generarea este buna daca:

- satul sau orasul pare locuit
- fiecare cladire are rol clar
- NPC-urile au motiv sa se miste intre zone
- questurile au suport spatial
- structurile exterioare adauga gameplay, nu doar peisaj
- lumea poate fi extinsa fara sa rescriem manual totul

## Concluzie

O structura buna nu inseamna doar "sa existe cladiri". Inseamna sa existe o retea de locuri cu sens: locuinte, profesii, utilitati, timp liber, public, exterior si quest. Cand aceste categorii sunt generate semantic, satul sau orasul devine utilizabil pentru rutina NPC, evenimente si poveste.
