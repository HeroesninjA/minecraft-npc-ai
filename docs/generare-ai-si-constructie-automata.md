# Generare AI si Constructie Automata

Actualizat: 2026-04-28

## Scop

Acest document descrie directia recomandata pentru un sistem care genereaza sau construieste automat:

- sate
- cladiri avansate
- pesteri tematice
- castele
- ruine
- tabere
- puncte de interes

Scopul nu este doar sa pui blocuri in lume.
Scopul real este sa ai un pipeline controlat, modular si sigur, care poate fi folosit de:

- runtime-ul pluginului
- scenarii
- world admin
- generatie asistata de AI

Pentru designul specific de generare sate cu WorldEdit, asociere NPC, locuitori per casa, locuri de munca si decoratii, vezi `docs/generare-sate-worldedit-si-npc.md`.

## Intrebarea de baza

Cand ajungi la generare de sate cu structuri mai complexe, trebuie folosit:

- `WorldEdit API`
  sau
- cod scris de la zero pentru plasarea blocurilor

## Raspuns scurt

Recomandarea buna este:

- `WorldEdit API` pentru executia de constructii mari, complexe sau bazate pe template-uri
- cod propriu pentru planificare, selectie, validare, semantica si operatii mici sau parametrice

Pe scurt:

- nu pune AI-ul sa scrie direct mii de blocuri in lume
- nu construi de la zero un editor de volume daca `WorldEdit` rezolva deja partea grea
- nu lega tot core-ul direct de `WorldEdit`

Modelul corect este unul hibrid.

## Concluzia practica

Pentru sate, castele, pesteri si cladiri avansate:

- da, merita folosit `WorldEdit API`
- dar doar ca strat de executie sau import

Logica importanta trebuie sa ramana in sistemul tau:

- ce vrei sa construiesti
- unde vrei sa construiesti
- de ce acel loc este valid
- cum etichetezi semantic rezultatul
- ce NPC-uri sau scenarii se leaga de acea structura

## De ce nu este suficient codul de la zero

Constructia bloc cu bloc facuta integral in plugin devine repede costisitoare cand apar:

- volume mari
- rotatii si oglindiri
- clipboard-uri sau template-uri
- replace selectiv de materiale
- brush logic
- undo sau rollback
- plasare rapida a multor blocuri

Pentru o casa simpla sau o decoratie mica, codul propriu este suficient.
Pentru un castel, o pestera tematizata sau un sat complet, costul creste foarte repede.

Problemele tipice ale unei implementari 100% custom:

- multa logica de transformare spatiala
- performanta mai slaba la plasari mari
- risc mai mare de bug-uri la rotatie si offset
- cod greu de mentinut
- mult timp consumat pe infrastructura, nu pe gameplay

## De ce nu trebuie folosit doar WorldEdit

`WorldEdit` rezolva bine partea de manipulare a volumelor, dar nu este motorul tau de gameplay.

Nu vrei ca `WorldEdit` sa decida:

- unde apare satul
- ce tip de sat se potriveste topologiei
- ce casa apartine fierarului
- ce cladire devine `fierarie`
- ce noduri, places si regiuni rezulta
- cum se leaga questurile de castel sau pestera

Asta este responsabilitatea platformei tale, nu a integrarii `WorldEdit`.

## Modelul recomandat

Arhitectura buna este:

1. AI sau reguli de continut propun un plan
2. sistemul tau valideaza planul
3. sistemul tau alege strategia de constructie
4. un adapter executa constructia:
   - prin `WorldEdit API`
   - sau prin builder nativ
5. sistemul tau inregistreaza semantic rezultatul:
   - `region`
   - `place`
   - `node`
   - legaturi cu NPC-uri si scenarii

## Principiul important pentru AI

AI-ul nu ar trebui sa genereze direct o lista lunga de blocuri.

AI-ul ar trebui sa genereze:

- intentie
- layout logic
- alegere de template
- parametri
- stil
- reguli
- legaturi semantice

Exemple bune de iesiri AI:

- "genereaza un sat medieval mic langa rau"
- "alege 8-12 cladiri cu piata centrala, fierarie, taverna si 2 ferme"
- "castel mic de deal cu sala mare, turn de observatie si curte interioara"
- "pestera cu intrare ingusta, camera centrala, altar si zona de ambuscada"

Exemple proaste de iesiri AI:

- mii de coordonate bloc cu bloc
- plasare directa fara validare
- instructiuni care ocolesc contractele runtime-ului

## Recomandarea de produs

Sistemul bun este:

- `AI planner`
- `template library`
- `layout validator`
- `construction adapter`
- `semantic mapper`

Nu:

- `prompt -> blocuri direct in lume`

## Cand sa folosesti WorldEdit API

`WorldEdit API` este foarte potrivit pentru:

- paste de schematics
- structuri mari sau repetabile
- rotatii si offset-uri
- variante de cladiri pe acelasi template
- populare rapida a unui sat
- castel cu mai multe corpuri
- pesteri decorate pornind din sabloane
- drumuri, ziduri, curti si volume mari

Exemple clare:

- sat medieval generat din prefabs
- fortificatie cu turnuri si ziduri
- castel modular
- pestera de quest cu camere predefinite
- ruine tematice inserate in biomi diferiti

## Cand sa folosesti cod propriu

Codul propriu este foarte bun pentru:

- selectie locatie
- scanare teren
- validare topologie
- ajustari locale dupa constructie
- decoratii mici
- plasari conditionale
- spawn logic pentru noduri si ancore
- tag-uri semantice
- adaptari la teren in jurul structurii

Exemple bune:

- alegerea pozitiei satului pe baza de biom, panta si apa
- alegerea casei fierarului in functie de piata
- conectarea cladirilor prin drumuri simple
- marcarea locurilor ca `fierarie`, `taverna`, `casa_garzilor`
- adaugarea de lumanari, cufere, job blocks sau usi in functie de context

## Recomandare ferma

Pentru faza in care vrei sate, cladiri avansate, pesteri si castele, recomandarea este:

- `WorldEdit API` pentru executia constructiei
- cod propriu pentru orchestrare si sens gameplay

Aceasta este alegerea pragmatica.

## Cum ar trebui introdus WorldEdit in proiect

Nu recomand sa bagi dependinta direct in `ainpc-core-plugin` ca o obligatie permanenta.

Model mai bun:

- `ainpc-api` ramane curat
- `ainpc-core-plugin` defineste contractele de constructie
- integrarea efectiva cu `WorldEdit` intra intr-un addon sau modul separat

Exemple bune de nume:

- `ainpc-integration-worldedit`
- `ainpc-builder-worldedit`

Astfel:

- core-ul nu depinde tare de `WorldEdit`
- poti avea fallback fara `WorldEdit`
- poti declara capabilitatea separat
- modularitatea ramane coerenta

## Contracte recomandate

In loc sa chemi `WorldEdit` direct din toate clasele, merita introdus un strat abstract.

Interfete recomandate:

- `StructureBuildService`
- `StructurePlacementStrategy`
- `StructureTemplateRepository`
- `BuildValidationService`
- `GeneratedSettlementPlan`
- `GeneratedStructurePlan`

Exemplu conceptual:

```java
public interface StructureBuildService {
    BuildResult buildStructure(GeneratedStructurePlan plan);
}

public interface StructureTemplateRepository {
    Optional<StructureTemplate> findById(String templateId);
}

public interface BuildValidationService {
    BuildValidationReport validate(GeneratedStructurePlan plan);
}
```

Implementari posibile:

- `WorldEditStructureBuildService`
- `NativeBlockBuildService`

## Ce trebuie sa produca AI-ul

AI-ul nu trebuie sa produca `EditSession`.
AI-ul nu trebuie sa cunoasca internals din `WorldEdit`.

AI-ul ar trebui sa produca ceva de forma:

```json
{
  "settlementType": "medieval_village",
  "regionType": "settlement",
  "style": "frontier",
  "size": "small",
  "requiredPlaces": [
    "village_square",
    "blacksmith_house",
    "forge",
    "tavern",
    "farm"
  ],
  "constraints": {
    "nearWater": true,
    "maxSlope": 0.28,
    "avoidLava": true
  }
}
```

Apoi sistemul tau transforma planul in:

- template-uri selectate
- pozitii
- rotatii
- places
- nodes
- NPC owners

## Pipeline recomandat pentru sate

Pentru sate, pipeline-ul bun este:

1. alegi o regiune candidata
2. validezi terenul
3. alegi tipul de sat
4. alegi layout-ul
5. alegi template-uri de cladiri
6. plasezi structurile
7. conectezi cu drumuri si spatii publice
8. marchezi `WorldRegion`
9. marchezi `WorldPlace`
10. creezi `WorldNode`
11. asociezi NPC-uri, roluri si quest hooks

Observatie:

- `WorldEdit` ajuta la pasul 6
- sistemul tau controleaza restul

## Pipeline recomandat pentru castele si pesteri

Pentru castele:

- structura de baza merita template sau module
- turnuri, ziduri si curtea se potrivesc bine cu `WorldEdit`
- adaptarea fina la teren poate fi facuta de cod propriu

Pentru pesteri:

- daca vrei forma organica mare, poti combina generare proprie cu sabloane tematice
- daca vrei camere speciale de quest, template-urile sunt foarte utile
- `WorldEdit` este bun pentru sculptare controlata si decorare mare

Concluzia este aceeasi:

- volum si prefab = `WorldEdit`
- logica si adaptare = cod propriu

## Ce resurse ar trebui sa folosesti

Sistemul devine mult mai bun daca foloseste:

- `schematics`
- template metadata
- YAML sau JSON pentru layout
- mapping semantic `region/place/node`

Exemple de resurse:

- `templates/medieval/house_small_01.schem`
- `templates/medieval/forge_01.schem`
- `templates/castle/tower_round_01.schem`
- `templates/cave/altar_room_01.schem`

Fisier metadata recomandat:

```yml
id: "forge_01"
theme: "medieval"
category: "building"
place_type: "forge"
size:
  width: 11
  height: 8
  depth: 9
tags:
  - workplace
  - blacksmith
anchors:
  entrance:
    x: 5
    y: 0
    z: 0
  work_spot:
    x: 6
    y: 1
    z: 4
nodes:
  forge_node:
    type: "interaction"
    anchor: "work_spot"
```

## Legatura cu world admin

Tot ce este construit automat trebuie mapat in sistemul semantic al lumii.

Rezultatul unei constructii nu trebuie sa fie doar blocuri.
Trebuie sa devina:

- `WorldRegion`
- `WorldPlace`
- `WorldNode`

Exemplu:

- satul generat devine `WorldRegion`
- fieraria devine `WorldPlace`
- nicovala sau tejgheaua devine `WorldNode`

Fara aceasta mapare, AI-ul si questurile nu pot intelege ce s-a construit.

## Legatura cu NPC-uri si scenarii

Dupa constructie, sistemul trebuie sa poata:

- asocia o cladire cu un NPC owner
- seta `homeAnchor` si `workAnchor` in starea actuala
- seta ulterior `homePlaceId` si `workPlaceId`, dupa ce aceste campuri devin parte din model
- crea questuri locale
- lega dialogul de cladirea generata

Exemple:

- fierarul locuieste in `blacksmith_house` si lucreaza in `forge`
- questul de livrare tinteste `forge`
- o garda patruleaza in `castle_gate`
- un mesager apare in `tavern`

## Validare obligatorie

Inainte de executie trebuie validate:

- biome
- panta
- apa si lava
- coliziuni cu structuri existente
- coliziuni cu regiuni deja importante
- dimensiunile template-ului
- existenta tuturor resurselor
- compatibilitatea cu tema si topologia

Fara validare, generarea automata devine fragila foarte repede.

## Fallback daca WorldEdit lipseste

Este important sa existe un comportament clar cand `WorldEdit` nu este instalat.

Model bun:

- continutul care cere `WorldEdit` declara aceasta capabilitate
- core-ul sau addonul verifica prezenta integrarii
- daca lipseste, acel tip de constructie nu ruleaza
- restul platformei ramane functional

Exemple:

- sat mare procedural cu prefabs: dezactivat fara `WorldEdit`
- casa mica simpla: poate rula prin builder nativ
- decorare locala: poate rula prin cod propriu

## Capabilitati recomandate

Pentru ecosistemul modular, merita capabilitati explicite precum:

- `structure-generation`
- `structure-templates`
- `worldedit-execution`
- `native-block-build`
- `semantic-place-mapping`
- `terrain-validation`

Acestea ajuta la:

- validare de addonuri
- compatibilitate
- mesaje clare pentru admin

## Ce sa eviti

- sa pui `WorldEdit` direct in tot core-ul
- sa lasi AI-ul sa genereze blocuri brute
- sa construiesti castele mari bloc cu bloc in Java fara infrastructura buna
- sa tratezi structurile doar ca decor, fara `region/place/node`
- sa sari peste validare de teren
- sa faci generare "magica" fara template metadata

## MVP recomandat

Pentru proiectul actual, pasul sanatos este:

1. documenteaza contractele de build
2. adauga `StructureBuildService`
3. adauga `GeneratedStructurePlan`
4. adauga metadata pentru template-uri
5. adauga un modul separat pentru `WorldEdit`
6. leaga rezultatul de `WorldRegion` / `WorldPlace` / `WorldNode`
7. foloseste AI doar pentru planificare si selectie

## Verdict final

Daca obiectivul este:

- sate mai mari
- cladiri avansate
- pesteri tematice
- castele
- ruine sau puncte de interes complexe

atunci raspunsul bun este:

- foloseste `WorldEdit API` pentru executie
- foloseste cod propriu pentru plan, validare, mapping semantic si integrare cu gameplay-ul

Nu recomand o implementare 100% custom pentru toate volumele mari.
Ar consuma timp mult pe infrastructura si ar complica inutil proiectul.

Recomandarea corecta pentru acest proiect este un sistem hibrid, modular si extensibil.
