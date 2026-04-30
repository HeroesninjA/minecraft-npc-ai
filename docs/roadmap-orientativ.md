# Roadmap Orientativ

Actualizat: 2026-04-30

## Scop

Acest document propune un roadmap realist pentru evolutia proiectului, pornind de la starea actuala a codului si de la directia dorita:

- plugin modular
- scenarii extensibile
- addonuri separate
- pe termen lung, scenarii asistate de AI

Nu este un plan rigid.
Este o ordine recomandata de constructie, astfel incat fiecare faza sa lase in urma un produs utilizabil, nu doar infrastructura.

## Principiul de baza

Ordinea buna este:

1. produs mic, dar jucabil
2. modularitate reala
3. runtime extensibil
4. ecosistem de addonuri
5. authoring asistat de AI

Ce trebuie evitat:

- sa construiesti prea devreme faza AI
- sa amani primul release pana cand totul este "perfect"
- sa extinzi `ScenarioEngine` in toate directiile inainte sa separi contractele

## Starea actuala

Proiectul are deja:

- nucleu functional de plugin
- persistenta SQLite
- NPC-uri AI bazate pe `Villager`
- dialog contextual cu OpenAI
- memorie, emotii, relatii
- questuri de baza functionale
- feature packs YAML
- API public initial
- addon medieval separat
- world admin la nivel de regiuni, places si noduri
- audit read-only pentru NPC, world mapping si DB
- debug dump avansat pentru colectarea contextului de investigatie
- ancore automate pentru casa si locul de munca al NPC-urilor
- scanner vanilla initial pentru sate
- mapper semantic initial catre `WorldRegion`, `WorldPlace` si `WorldNode`
- rutina zilnica initiala pentru NPC-uri pe `home/work/social anchors`
- audit de spawn order pentru case, rezidenti, ancore si relatii familiale reciproce

Asta inseamna ca proiectul este deja peste stadiul de prototip simplu.

## Rezumat pe faze

### Faza 1

`First playable release`

Scop:
- un pachet mic, stabil si jucabil cap-coada

### Faza 2

`Stabilizare modulara`

Scop:
- separarea curata intre core, API si addonuri

### Faza 3

`Runtime de scenarii extensibil`

Scop:
- scenarii configurabile prin actiuni, conditii si tranzitii

### Faza 4

`Addon ecosystem`

Scop:
- scenarii si integrari livrate din jar-uri separate

### Faza 5

`Prompt-assisted scenario authoring`

Scop:
- AI-ul ajuta la generarea scenariilor, dar pluginul ramane executorul si validatorul

## Faza 1: First Playable Release

Aceasta este faza care trebuie sa duca la primul release public sau semi-public.

Obiectiv:
- un server admin sa poata instala pluginul
- sa poata crea sau incarca un sat demo
- jucatorul sa poata interactiona cu NPC-uri
- sa existe cateva questuri clare si completabile

## Must have

- instalare clara si configurare minima
- un scenariu medieval mic, dar complet
- `5-10 NPC-uri` cu roluri diferite
- `3-5 questuri` cap-coada
- persistenta stabila pentru NPC-uri si questuri
- fallback acceptabil cand AI-ul raspunde slab sau deloc
- comenzi admin suficiente pentru create, info, tp, reload, world, audit si debugdump
- documentatie de instalare si testare

## Should have

- extinderea rutinei zilnice initiale cu pathfinding/pasi intermediari si evenimente sociale probabilistice
- mapping demo populat cu regiune, case, locuri de munca, piata si nodes
- world admin stabilizat pe regiuni, places si noduri
- mai multe mesaje de debug pentru probleme de config
- reward-uri mai clare si mai consistente

## Can wait

- economie completa
- reputatie globala
- NPC-uri non-villager
- branching avansat
- scenarii generate prin prompt

## Criteriul de iesire din Faza 1

Poti spune ca ai un `v0.1` sau `alpha` cand:

- pluginul se instaleaza fara pasi obscuri
- un demo medieval se joaca de la inceput la sfarsit
- questurile nu se rup usor la reload
- NPC-urile interactioneaza consistent
- ai documentatie minima pentru admin

## Faza 2: Stabilizare Modulare

Aceasta faza nu este despre continut nou mare, ci despre curatarea arhitecturii.

Obiectiv:
- addonurile sa poata depinde de `ainpc-api`, nu de internals

## De facut

- clarificarea responsabilitatilor `ainpc-api` vs `ainpc-core-plugin`
- stabilizarea `AINPCPlatformApi`
- extinderea documentatiei API
- conventii pentru `capabilities` si `dependencies`
- addon demo minim documentat
- reload de continut mai sigur
- mai putin coupling direct cu clase din core

## Criteriul de iesire din Faza 2

Poti spune ca ai `v0.2` sau `beta modular` cand:

- un addon separat se poate conecta curat prin API
- addonul poate instala continut fara hack-uri
- nu trebuie sa importe clase interne instabile
- documentatia API este suficienta pentru un developer extern

## Faza 3: Runtime de Scenarii Extensibil

Aceasta este faza in care treci de la scenarii finite si relativ hardcodate la scenarii compuse din piese extensibile.

Obiectiv:
- pluginul sa poata executa scenarii mai flexibile fara sa modifici core-ul pentru fiecare caz nou

## De facut

- `ScenarioActionRegistry`
- `ScenarioConditionRegistry`
- `ScenarioTriggerRegistry`
- `ScenarioExecutionContext`
- parser separat pentru scenarii
- validator separat
- suport YAML pentru:
  - `actions`
  - `conditions`
  - `transitions`
  - `cleanup`

## Puternic recomandat in aceasta faza

- consum complet al `places` semantice in NPCContext, questuri si scenarii
- questuri pe etape reale
- NPC-uri temporare de quest
- reactie NPC-jucator mai structurata

## Criteriul de iesire din Faza 3

Poti spune ca ai `v0.3` sau `advanced scenario runtime` cand:

- scenariile nu mai depind exclusiv de if-uri in `ScenarioEngine`
- poti adauga un tip nou de actiune fara sa rupi tot motorul
- continutul poate fi validat inainte de executie

## Faza 4: Addon Ecosystem

Aceasta este faza in care scenariile si capabilitatile noi se muta natural in jar-uri separate.

Obiectiv:
- core-ul devine platforma
- universurile si integratiile devin addonuri

## De facut

- addon template oficial
- exemple pentru `ainpc-scenario-*`
- feature packs suplimentare:
  - fantasy
  - social
  - modern
- reguli de compatibilitate si versionare
- mesaje mai clare pentru dependinte lipsa
- validare de capabilitati la load

## Rezultatul dorit

- poti avea mai multe pachete tematice
- poti porni servere cu continut diferit fara fork de proiect
- poti combina addonuri fara configuratie fragila

## Criteriul de iesire din Faza 4

Poti spune ca ai `v0.4` sau `platform mode` cand:

- exista cel putin `2-3` addonuri reale separate
- core-ul ruleaza si fara sa fie lipit de un singur scenariu
- ecosistemul are reguli clare de compatibilitate

## Faza 5: Scenarii Prin Prompturi

Aceasta faza trebuie sa vina tarziu, nu devreme.

Obiectiv:
- AI-ul ajuta la authoring de continut
- pluginul valideaza si executa

## Modelul corect

Nu:

- `prompt -> executie directa`

Da:

1. `prompt -> research`
2. `research -> alegere template`
3. `template -> draft de scenariu`
4. `draft -> validare`
5. `valid -> import in plugin`

## Ce trebuie sa existe inainte

- template-uri de scenarii
- validator de schema
- validator de capabilitati
- registri maturi de actiuni si conditii
- documentatie API buna
- context semantic despre lume, NPC-uri si questuri
- `WorldContextSnapshot` si story state validabil peste mapping semantic

## De facut

- script sau tool extern de generare
- biblioteca de template-uri
- raport de validare
- import controlat in `packs/`
- eventual mod `deep research` pentru cereri complexe

## Criteriul de iesire din Faza 5

Poti spune ca ai `v0.5` sau `AI-assisted authoring` cand:

- un prompt poate produce un draft coerent
- draftul trece prin validator
- validatorul poate spune clar daca resursele actuale sunt suficiente
- scenariul rezultat nu ocoleste contractele platformei

## V1.0 realist

Un `v1.0` realist pentru proiectul tau nu inseamna:

- toate sistemele imaginabile
- generare infinita
- toate tipurile de NPC

Un `v1.0` realist inseamna:

- first release deja stabilizat in productie mica
- API public clar
- addonuri separate functionale
- runtime de scenarii suficient de extensibil
- continut modular real

## Propunere simpla de versiuni

### `v0.1-alpha`

Focus:
- demo medieval jucabil
- stabilitate
- instalare si documentatie minima

### `v0.2-beta`

Focus:
- API mai clar
- modularizare mai curata
- addon model mai solid

### `v0.3`

Focus:
- runtime de scenarii extensibil
- questuri mai avansate
- obiective `visit_place` si `inspect_node`
- NPC-uri temporare

### `v0.4`

Focus:
- ecosistem de addonuri
- mai multe scenarii tematice
- compatibilitate si validare

### `v0.5`

Focus:
- authoring asistat de AI
- template-uri
- validator de capabilitati

### `v1.0`

Focus:
- platforma modulara stabila
- documentatie suficienta
- scenarii livrabile si extensibile

## Prioritatea recomandata chiar acum

Daca alegi doar urmatoarele directii pentru perioada imediata, ele sunt cele mai valoroase:

1. `first playable release`
2. stabilitate si debug
3. generator real care produce automat `HouseAllocation` din regiuni, cladiri si node-uri
4. popularea unui mapping demo real cu scannerul vanilla si import semantic
5. `WorldContextSnapshot` pentru NPCContext, questuri si story
6. comenzi admin pentru validare/dry-run planuri serializate
7. evenimente sociale probabilistice peste rutina
8. API si modularizare curate
9. runtime extensibil pentru scenarii

Nu recomand sa intri acum direct in:

- prompt generation
- scripting avansat
- economie mare
- sisteme foarte largi de reputatie sau RPG

## Concluzie

Roadmap-ul sanatos pentru proiect este:

1. scoate un release mic, dar complet
2. stabilizeaza modularitatea
3. deschide runtime-ul pentru extensii
4. construieste ecosistemul de addonuri
5. adauga AI-ul ca tool de authoring, nu ca substitut pentru arhitectura

Aceasta ordine iti permite sa livrezi ceva jucabil devreme, fara sa blochezi proiectul in infrastructura nesfarsita.
