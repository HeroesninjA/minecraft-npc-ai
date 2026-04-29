# Mapping pentru Implementari Ulterioare

Actualizat: 2026-04-27

## Scop

Acest document explica:

- cum trebuie folosit sistemul de mapping in implementarile viitoare
- ce rol are in questuri, NPC-uri, generare si AI
- ce reguli de design trebuie respectate
- ce imbunatatiri merita adaugate in sistem

Nu este un document despre configurarea de baza a `regions / places / nodes`.
Pentru fundatie, vezi si `docs/mapping.md`.

## Ce inseamna mapping in proiect

In proiect, mapping-ul este stratul care traduce lumea din:

- coordonate brute

in:

- regiuni cu sens
- locuri semantice
- puncte exacte de interactiune

Modelul actual este:

- `Region` = zona mare
- `Place` = loc semantic
- `Node` = punct precis

Exemple:

- `Region`: `satul_central`
- `Place`: `satul_central:fierarie`
- `Node`: `satul_central:fierarie:forge_spot`

## Ideea centrala

Orice sistem care are nevoie de "sensul" unui loc nu ar trebui sa lucreze direct cu coordonate.

Ar trebui sa lucreze cu:

- `regionId`
- `placeId`
- `nodeId`
- `placeType`
- `tags`
- metadata semantica

Coordonatele raman importante doar pentru:

- detectie
- validare
- plasare fizica
- navigatie

Semantica trebuie sa conduca gameplay-ul.

## Cum trebuie folosit mapping-ul

Fluxul bun este:

1. un sistem pleaca de la locatie, NPC, eveniment sau plan de generatie
2. mapping-ul rezolva `region`, `place` si `node`
3. contextul semantic este injectat in runtime
4. logica de gameplay reactioneaza pe baza de `id`, `type`, `tags`, `metadata`
5. rezultatul este persistat sau transmis mai departe

Pe scurt:

- coordonatele spun unde este ceva
- mapping-ul spune ce este acel loc
- gameplay-ul decide ce inseamna acel loc

## Cum se va folosi in implementari ulterioare

### 1. NPC-uri si rutine

Mapping-ul trebuie sa devina sursa principala pentru rutina sociala si profesionala a NPC-urilor.

Exemple:

- un NPC merge la `homePlaceId` seara
- un NPC lucreaza la `workPlaceId` ziua
- un NPC socializeaza intr-un `socialPlaceId`
- un NPC foloseste `nodes` diferite in acelasi `place`

Exemple concrete:

- `homePlaceId = satul_central:casa_fierarului`
- `workPlaceId = satul_central:fierarie`
- `socialPlaceId = satul_central:taverna`

Ce trebuie sa faca implementarea:

- `AINPC` sa tina ID-urile semantice
- `NPCContext` sa stie locul curent
- planner-ul de rutina sa aleaga destinatii prin `placeId`, nu prin coordonate hardcodate

### 2. Dialog si raspuns AI

Dialogul trebuie sa consume context semantic.

In loc de:

- "NPC-ul este la x=144 y=65 z=149"

AI-ul si sistemul de dialog ar trebui sa stie:

- este in `fierarie`
- locul are tip `forge`
- tag-urile sunt `workplace`, `blacksmith`, `shop`
- owner-ul este un anumit NPC
- accesul este public sau privat

Efecte:

- replici contextuale
- ton diferit in loc public vs privat
- raspunsuri diferite in functie de program si tipul locului
- motivatie mai buna pentru scenarii si evenimente

### 3. Questuri

Aici mapping-ul este esential.

Questurile actuale folosesc deja semantica limitata prin `visit_region`.
Extensia buna este:

- `visit_place`
- `enter_place`
- `inspect_node`
- `talk_to_npc_at_place`
- `bring_item_to_place`
- `kill_mob_in_place`
- `escort_npc_to_place`

Exemple:

- "mergi la fierarie"
- "vorbeste cu fierarul la tejghea"
- "inspecteaza intrarea in pestera"
- "du minereul in camera de topire"

Regula importanta:

- questul nu trebuie sa aiba doar `target coordinates`
- questul trebuie sa tina `semantic target`

### 4. Generare de sate, castele, pesteri si structuri

Mapping-ul este puntea dintre constructie si gameplay.

Fara mapping, o structura generata este doar blocuri.
Cu mapping, structura devine continut utilizabil.

Pipeline recomandat dupa generare:

1. se construieste structura
2. se creeaza `region`
3. se creeaza `places`
4. se creeaza `nodes`
5. se asociaza NPC-uri si roluri
6. se adauga quest hooks si story hooks

Exemplu pentru un sat generat:

- `region = satul_nou`
- `place = satul_nou:piata`
- `place = satul_nou:fierarie`
- `place = satul_nou:taverna`
- `node = satul_nou:fierarie:forge_spot`
- `node = satul_nou:piata:announcement_board`

Abia dupa acest pas generarea devine utila pentru AI si questuri.

### 5. Scenarii narative

Scenariile viitoare nu trebuie sa porneasca doar din evenimente abstracte.

Ele pot porni din:

- un loc
- o combinatie de tag-uri
- un `node`
- o stare a unui `place`

Exemple:

- furt in `market`
- incendiu in `forge`
- conflict in `tavern`
- aparitie episodica la `castle_gate`

Ce inseamna asta:

- mapping-ul devine trigger source
- mapping-ul devine selectie de context
- mapping-ul devine filtru de validare pentru scenarii

### 6. Admin si debugging

Mapping-ul trebuie sa fie baza pentru operare si debug, nu doar pentru runtime.

Implementari utile:

- inspectie rapida a locului curent
- listare de `places` si `nodes`
- verificare de suprapuneri
- verificare de ownership
- validare de `tags`

Comenzile existente sunt deja un inceput bun:

- `/ainpc world whereami`
- `/ainpc world places`
- `/ainpc world region info ...`
- `/ainpc world place info ...`
- `/ainpc world region create ...`
- `/ainpc world place create ...`
- `/ainpc world node create ...`
- `/ainpc world save`

### 7. Indexare automata dezactivabila

Mapping-ul are acum un index intern pe chunk-uri pentru `regions`, `places` si `nodes`.

Rol:

- cautari mai rapide pentru `findRegion(...)`
- cautari mai rapide pentru `findPlace(...)`
- fundatie pentru lookup-uri viitoare pe nodes

Configurare:

```yml
world_admin:
  auto_index:
    enabled: true
```

Daca este dezactivat:

```yml
world_admin:
  auto_index:
    enabled: false
```

sistemul revine la cautarea liniara. Asta este util pentru debugging, pentru harti foarte mici sau daca apare o problema de consistenta in index.

## Reguli bune de folosire

### 1. `Region` nu trebuie sa fie folosit pe post de `Place`

Regiunea descrie aria mare.
Cladirea sau camera trebuie modelata ca `place`.

Exemplu rau:

- tot satul este folosit ca destinatie pentru o interactiune precisa

Exemplu bun:

- satul este `region`
- fieraria este `place`
- nicovala este `node`

### 2. `Node` nu trebuie sa inlocuiasca `Place`

Node-ul este punct exact, nu spatiu semantic.

Daca un sistem vrea sa stie:

- unde locuieste un NPC
- unde munceste
- unde se afla o scena

atunci tinta buna este `place`, nu doar `node`.

### 3. ID-urile trebuie sa fie stabile

`id`-urile trebuie tratate ca identificatori de gameplay.

Asta inseamna:

- nu le redenumesti des
- nu folosesti texte afisate drept identificator intern
- generarea trebuie sa produca namespace-uri coerente

### 4. Tag-urile trebuie sa fie semantice, nu decorative

Tag-uri bune:

- `home`
- `workplace`
- `shop`
- `public`
- `restricted`
- `danger`
- `ritual`

Tag-uri slabe:

- `cool`
- `important`
- `misc`

### 5. Mapping-ul trebuie folosit prin API sau service, nu citit direct din YAML

Codul de gameplay nu trebuie sa parseze singur `config.yml`.

El trebuie sa consume:

- `WorldAdminApi`
- `WorldAdminService`
- modele publice `WorldRegionInfo`, `WorldPlaceInfo`, `WorldNodeInfo`

## Exemple de fluxuri viitoare

### Exemplu 1: NPC merge la munca

1. planner-ul verifica ora si rutina
2. gaseste `workPlaceId`
3. cere `place` din world admin
4. cauta un `node` cu rol de lucru
5. muta NPC-ul spre acel punct
6. dialogul si comportamentul se schimba deoarece `currentPlaceType = forge`

### Exemplu 2: Quest de inspectie

1. questul cere `inspect_node`
2. tinta este `satul_central:fierarie:forge_spot`
3. listener-ul detecteaza apropierea jucatorului
4. progresul pe obiectiv este actualizat
5. jurnalul si dialogul se schimba in functie de noul progres

### Exemplu 3: Generare de sat

1. sistemul alege zona
2. construieste cladirile
3. inregistreaza `region`
4. inregistreaza `places`
5. adauga `nodes`
6. salveaza mapping-ul
7. NPC-urile si questurile incep sa foloseasca automat noua structura

## Imbunatatiri recomandate pentru sistemul de mapping

### 1. API public de scriere mai complet

Acum exista deja creare in runtime prin comenzi si service, dar pe termen lung merita un contract public clar pentru mutatii controlate.

Exemple:

- `createRegion`
- `createPlace`
- `createNode`
- `removeRegion`
- `removePlace`
- `removeNode`
- `updatePlaceMetadata`

### 2. Persistenta mai buna decat `config.yml`

Pentru continut generat dinamic, `config.yml` este bun ca start, dar nu este suficient pe termen lung.

Imbunatatiri:

- persistenta in baza de date
- versiuni de mapping
- snapshot / rollback
- export / import

### 3. Stare dinamica pentru `places`

In prezent, `place` este in principal o definitie statica.
Pe viitor, un `place` ar trebui sa poata avea si stare.

Exemple:

- `open`
- `closed`
- `under_attack`
- `burned`
- `contested`
- `quest_locked`

Asta ar ajuta mult:

- dialogul
- questurile
- scenariile
- rutinele

### 4. Tipuri si roluri mai bogate pentru `nodes`

Acum `node` este util, dar poate evolua.

Merita adaugate roluri semantice precum:

- `entry`
- `exit`
- `sleep`
- `work`
- `guard`
- `shop_counter`
- `ritual_center`
- `loot_spawn`
- `conversation_anchor`

Aceste roluri pot sta fie in `type`, fie in metadata standardizata.

### 5. Relatii intre `places`

Sistemul actual descrie containment, dar nu si relatii intre locuri.

Pe viitor ar ajuta:

- `connected_to`
- `adjacent_to`
- `owned_by`
- `serves`
- `depends_on`

Exemple:

- taverna este conectata la piata
- fieraria serveste satul central
- camera tronului apartine castelului

### 6. Ierarhii si sub-places

Pentru castele, pesteri si orase mari, un singur nivel de `place` poate deveni prea simplu.

Extensii utile:

- `parentPlaceId`
- `subplaces`
- camere interioare
- zone functionale din cladiri mari

Exemple:

- `castel:sala_mare`
- `castel:turn_nord`
- `castel:turn_nord:camera_garda`

### 7. Evenimente Bukkit pentru mapping

Foarte util pentru extensii.

Exemple:

- `AINPCRegionEnteredEvent`
- `AINPCPlaceEnteredEvent`
- `AINPCPlaceExitedEvent`
- `AINPCNodeReachedEvent`
- `AINPCMappingReloadedEvent`

Acestea ar permite:

- quest hooks
- scene triggers
- analytics
- addonuri externe

### 8. Validare mai stricta

Sistemul actual valideaza deja o parte din cazuri, dar merita extins.

Verificari utile:

- `place` complet in interiorul regiunii
- `node` in interiorul containerului
- suprapuneri per tip
- owner NPC valid
- tag-uri necunoscute
- metadata obligatorie pentru anumite tipuri

### 9. Editor si unelte admin mai bune

Pe termen lung, configurarea manuala devine lenta.

Imbunatatiri utile:

- selectie din world cu doua colturi
- `setpos1 / setpos2` intern
- creare `place` din selectie
- creare `node` la pozitia curenta
- highlight vizual pentru region / place / node
- export rapid din selectie

### 10. Integrare cu navigatie si pathing

Mapping-ul va deveni si mai util cand se leaga de miscare.

Exemple:

- `preferred entry node`
- `sleep node`
- `work node`
- drumuri sau muchii intre `places`
- costuri de traversare

Astfel:

- NPC-urile merg mai coerent
- escort quests sunt mai stabile
- scenele cu miscare sunt mai usor de orchestrat

### 11. Template-uri si generare semantica

Pentru constructie automata, mapping-ul ar trebui sa poata fi generat din template metadata.

Exemple:

- o schematica de fierarie vine cu `placeType=forge`
- anumite markere din template definesc `nodes`
- template-ul declara tag-uri, owner roles si hooks

Asta reduce mult munca manuala dupa generare.

### 12. Standardizare pentru tags si metadata

Pe termen lung, trebuie evitat haosul semantic.

Merita definite:

- tag-uri oficiale
- chei oficiale de metadata
- conventii de naming

Exemple:

- `role=work`
- `access=public`
- `business=blacksmith`
- `danger=high`

## Ordine buna de evolutie

1. leaga NPC-urile si `NPCContext` de `placeId`
2. adauga obiective `visit_place` si `inspect_node`
3. adauga evenimente de mapping
4. standardizeaza `tags` si metadata
5. introdu stare dinamica pentru `places`
6. adauga relatii intre `places`
7. adauga editor si unelte vizuale
8. leaga mapping-ul de generare automata si template-uri

## Concluzie

Mapping-ul nu trebuie vazut ca un tabel de coordonate.
El trebuie vazut ca stratul semantic comun dintre:

- lume
- NPC
- quest
- scena
- AI
- generare

Daca este folosit corect, mapping-ul devine infrastructura centrala a gameplay-ului.
Daca este folosit doar pentru afisare sau debug, valoarea lui ramane foarte mica.
