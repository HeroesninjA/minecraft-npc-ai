# Build Mode pentru `Region / Place / Node`

Actualizat: 2026-07-01

Punctul de intrare recomandat pentru aceasta zona ramane `mapping-stack.md`.

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

Ghidul pas cu pas este in `docs/build-mode-tutorial.md`.
Pentru `Quest / Progression`, foloseste `docs/quest-progression-tutorial.md`.
Pentru administrare si inspectie runtime, foloseste `docs/admin-mcp-tutorial.md`.
Pentru fluxul vizual din GUI, foloseste `docs/admin-world-hub-tutorial.md`.
Pentru `Quest Authoring / Quick Quest`, foloseste `docs/quest-authoring-tutorial.md`.

## Scop

`Build Mode` este modul de authoring pentru mapping semantic in care adminul defineste `Region`, `Place` si `Node` direct in lume, fara sa scrie manual fiecare camp in configuratie.

Scopul lui este sa permita:

- selectie rapida si clara a zonelor
- suport pentru forme dreptunghiulare si forme neregulate
- preview vizual persistent pe durata sesiunii
- creare consistenta de `Region`, `Place` si `Node`
- compatibilitate cu `maintenance mode`

## Principii

- `Build Mode` descrie o intentie de mapping, nu o reconstructie completa a lumii.
- Se lucreaza peste lumea existenta, nu impotriva ei.
- Selectia trebuie sa ramana vizibila pana la confirmare sau anulare.
- Orice selectie trebuie sa produca un rezultat semantic verificabil.
- Nu se pierde contextul daca adminul trece intre moduri de selectie in aceeasi sesiune.

## Moduri de selectie

### 1. `Sign Mode`

`Sign Mode` este varianta rapida pentru cazurile simple, mai ales cand zona are forma clara si vrei sa o marchezi direct in lume.

Flux recomandat:

1. adminul pune un semn la `pos1`
2. adminul pune un semn la `pos2`
3. pe semn se scrie tipul semantic si numele propus
4. sistemul interpreteaza selectia ca interval sau ca ancorare de zona

Exemplu de intentie:

- `place`
- `curte castel`
- `pos1`
- `pos2`

Exemplu de rezultat asteptat:

- `Place`
- nume: `curte_castel`
- tip: `castle_courtyard` sau un tip apropiat existent
- bounds: definit de `pos1` si `pos2`
- metadata: descriere, tags, eventual owner sau access

`Sign Mode` este util cand:

- vrei sa marchezi repede o constructie simpla
- vrei sa etichetezi clar o zona fara selectie complexa
- vrei un flux usor de invatat de catre admin

### 2. `Multi-Selection Wand`

`Multi-Selection Wand` este modul pentru forme neregulate si zone care nu pot fi descrise corect doar printr-un paralelipiped.

Acest mod trebuie sa permita:

- adaugare de puncte in selectie
- scoatere de puncte din selectie
- selectie compusa din mai multe segmente
- contururi neregulate pentru curti, ziduri, curti interioare, teren fragmentat sau zone partial acoperite

Folosit pentru:

- curti de castel
- regiuni cu relief neregulat
- locuri de munca cu mai multe anexe
- zone de tranzitie intre `Region` si `Place`

Regula de lucru:

- wand-ul selecteaza forma
- promptul sau semnul da semantica
- preview-ul confirma rezultatul inainte de scriere

### 3. `Point / Anchor Mode`

`Point / Anchor Mode` este pentru `Node`-uri si ancore exacte.

Se foloseste pentru:

- intrari
- puncte de dialog
- workstations
- bed nodes
- quest trigger points
- puncte sociale

In acest mod, selectia nu descrie o suprafata mare, ci un punct sau un radius mic in jurul lui.

## Creare asistata de AI

`Build Mode` poate expune un flux asistat de AI pentru creare de `Region`, `Place`, `Node`, `Quest` si `Progression`.

Scopul este ca adminul sa poata porni de la o comanda scurta, iar asistentul sa completeze pasii lipsa prin intrebari ghidate.

Exemple de pornire:

- `/ainpc world create ai`
- `/ainpc world create ai preview region`
- `/ainpc world create ai preview place`
- `/ainpc world create ai preview node`
- `/ainpc quest create ai`
- `/ainpc progression create ai`

Fluxul asistat trebuie sa faca urmatoarele:

- cere pe rand numele
- cere ID-ul
- cere tipul semantic
- cere selectia `pos1` / `pos2` sau punctul activ
- cere descrierea scurta a rolului
- cere confirmarea finala inainte de scriere

Exemplu de dialog pentru `Region`:

1. `Cum se va numi?`
2. `Ce ID folosim?`
3. `Unde sunt pos1 si pos2?`
4. `Ce tip are regiunea?`
5. `Confirmi creatia?`

Exemplu de dialog pentru `Quest`:

1. `Cum se numeste questul?`
2. `Care este quest ID?`
3. `Ce anchor folosim?`
4. `Ce obiectiv are?`
5. `Ce recompensa se aplica?`
6. `Confirmi creatia?`

Exemplu de dialog pentru `Progression`:

1. `Ce tip de progresie creem?`
2. `Care este progression ID?`
3. `Ce quest sau ce sistem consuma progresia?`
4. `Ce praguri, etape sau reguli aplicam?`
5. `Ce activatoare sau recompense sunt legate de ea?`
6. `Confirmi creatia?`

## Asistenta pentru `Quest` si `Progression`

Asistenta AI pentru questuri si progresie trebuie sa ramana orientata pe configurare ghidata, nu pe scriere libera fara validare.

Pentru `Quest`, asistenta trebuie sa poata completa:

- `questId`
- nume afisat
- tipul questului
- obiectivul principal
- ancorele folosite
- recompensa
- dependintele fata de progresie sau story

Pentru `Progression`, asistenta trebuie sa poata completa:

- `progressionId`
- nume
- domeniu de aplicare
- reguli de avansare
- praguri de XP sau stage
- conditii de blocare
- efecte la schimbarea starii

## Legaturi cu `Region / Place / Node`

`Quest` si `Progression` trebuie sa poata consuma direct mapping-ul semantic existent.

Legaturi valide:

- `Quest` poate referi un `Region` ca zona tinta
- `Quest` poate referi un `Place` ca locatie principala
- `Quest` poate referi unul sau mai multe `Node` ca trigger, obiectiv sau punct de finalizare
- `Progression` poate depinde de o regiune, un place sau un node pentru unlock, stage sau gating

Regula de validare:

- daca referinta semantica nu exista, preview-ul trebuie sa arate conflictul
- daca referinta exista, preview-ul trebuie sa arate ID-ul complet calificat
- daca un quest sau o progresie are mai multe ancore, acestea trebuie afisate separat

## Subtipuri propuse pentru `Quest`

Fluxul AI trebuie sa poata propune cel putin urmatoarele familii:

- `story`
- `tutorial`
- `contract`
- `duty`
- `bounty`
- `event`
- `ritual`
- `exploration`
- `custom`

Regula:

- daca tipul nu este cunoscut, sistemul propune un tip apropiat si cere confirmare
- daca un `Quest` necesita `Progression`, preview-ul trebuie sa arate dependinta explicit

## Subtipuri propuse pentru `Progression`

Fluxul AI trebuie sa poata propune cel putin urmatoarele familii:

- `quest`
- `story`
- `area`
- `faction`
- `npc_relationship`
- `resource`
- `custom`

Regula:

- daca progresia este legata de un singur quest, sistemul marcheaza scope-ul ca `quest`
- daca progresia controleaza o zona sau un sistem regional, scope-ul trebuie marcat explicit ca `area`
- daca progresia gestioneaza stadii narative, preview-ul trebuie sa explice momentul de avansare

Reguli:

- daca lipsește `questId` sau `progressionId`, sistemul cere completare explicită
- daca tipul este ambiguu, sistemul propune variante compatibile
- daca obiectivele sau pragurile nu sunt coerente, sistemul cere clarificare
- daca progresia depinde de un quest inexistent, creația se blochează pana la corectare
- daca ID-ul generat automat exista deja, sistemul propune variante numerotate
- daca numele si tipul nu se potrivesc, asistentul cere confirmare inainte de scriere

Fluxul asistat pentru `Quest` si `Progression` trebuie sa reuseasca aceeasi regula ca la mapping:

- intreaba pe rand
- arata preview
- permite corectie rapida
- confirma doar la final

## Auto-suggest pentru ID-uri si tipuri

Asistentul trebuie sa poata genera sugestii initiale, dar nu sa le scrie fara confirmare.

Reguli pentru `ID`:

- transforma spatiile in underscore
- elimina caracterele nesigure
- foloseste litere mici
- adauga sufix numeric daca ID-ul exista deja

Exemple:

- `curte castel` -> `curte_castel`
- `Curtea Castelului` -> `curtea_castelului`
- `Apararea castelului` -> `apararea_castelului`

Reguli pentru tip:

- daca utilizatorul cere `quest`, sistemul propune tipul exact sau o familie apropiata
- daca utilizatorul cere `progression`, sistemul propune un scope compatibil
- daca intentia este vaga, preview-ul trebuie sa includa 2-3 variante

Reguli pentru corectie:

- utilizatorul poate selecta o sugestie prin sageata sau buton
- orice corectie trebuie sa actualizeze preview-ul imediat
- sugestia aleasa devine noul draft curent pana la confirmare

## Validare automata a conflictelor

Inainte de confirmare, sistemul trebuie sa verifice daca noul draft intra in conflict cu datele existente.

Tipuri de conflicte:

- `ID already exists`
- `bounds overlap`
- `semantic mismatch`
- `duplicate anchor`
- `missing selection`
- `invalid type`
- `missing dependency`

Reguli:

- daca exista conflict, confirmarea este blocata
- preview-ul trebuie sa arate clar cauza conflictului
- sistemul poate propune o corectie automata, dar nu o aplica fara acord
- daca conflictul este rezolvabil, utilizatorul primeste varianta corectata ca sugestie

## Highlight vizual pentru optiunea corecta

Cand sistemul ofera mai multe variante, UI-ul trebuie sa faca optiunea recomandata foarte clara.

Mecanisme acceptate:

- sageata de selectie pe randul recomandat
- highlight cu culoare distincta
- badge `recommended`
- accent pe prima varianta valida

Regula:

- optiunea recomandata trebuie sa fie cea mai vizibila
- daca exista o singura varianta valida, aceasta trebuie marcata automat
- daca exista mai multe variante valide, ordinea trebuie sa urmeze scorul de potrivire

## Fallback pentru comenzi gresite

Daca utilizatorul scrie o comanda gresita, incompleta sau ambigua, sistemul nu trebuie sa opreasca fluxul fara ajutor.

Comportament asteptat:

- afiseaza comanda corecta apropiata
- arata ce argument lipseste
- propune 2-3 variante valide
- deschide wizard-ul corect daca intentia e clara

Exemple:

- `/ainpc quest create` -> propune `/ainpc quest create ai`
- `/ainpc world create` -> propune `/ainpc world create ai`
- `/ainpc progression` -> propune `/ainpc progression create ai`
- `/npc world create ai` -> daca aliasul este activ, trimite fluxul catre comanda echivalenta

Regula finala:

- eroarea de sintaxa trebuie transformata intr-o sugestie utila, nu intr-un refuz opac

## Ordine de implementare

Pentru a evita un design prea mare dintr-un singur pas, implementarea trebuie facuta incremental.

### Faza 1: baza de comenzi

- adauga `/ainpc world create ai`
- adauga `/ainpc quest create ai`
- adauga `/ainpc progression create ai`
- adauga aliasul operational `/npc` doar daca runtime-ul il permite
- valideaza comenzi gresite cu fallback util

### Faza 2: wizard UI

- implementeaza `Back / Next / Confirm / Cancel`
- implementeaza `Suggest / Edit`
- pastreaza preview-ul actualizat dupa orice corectie
- afiseaza highlight pe varianta recomandata

### Faza 3: mapping build

- leaga `Sign Mode` de `pos1 / pos2`
- leaga `Multi-Selection Wand` de selectii neregulate
- leaga `Point / Anchor Mode` de `Node`
- pastreaza selectia vizibila pe durata sesiunii

### Faza 4: quest si progression

- adauga auto-suggest pentru `questId` si `progressionId`
- valideaza dependintele dintre `Quest`, `Progression` si mapping
- blocheaza confirmarea cand exista conflict
- expune preview separat pentru `Quest` si `Progression`

### Faza 5: hardening

- adauga verificare de conflicte
- adauga mesaj clar pentru tip invalid sau selectie lipsa
- valideaza compatibilitatea cu `maintenance mode`
- adauga log/audit pentru corectii si confirmari

## Backlog tehnic

### 1. Comenzi si parser

- parseaza `/ainpc world create ai`
- parseaza `/ainpc quest create ai`
- parseaza `/ainpc progression create ai`
- parseaza aliasul `/npc` daca este activ
- detecteaza comenzi incomplete si propune varianta corecta

### 2. Wizard state

- creeaza state pentru pasii wizard-ului
- retine campurile introduse intre `Back` si `Next`
- separa state-ul de `Quest`, `Progression` si `Build`
- reseteaza sigur la `Cancel`

### 3. Preview engine

- genereaza preview pentru `Quest`
- genereaza preview pentru `Progression`
- genereaza preview pentru `Region / Place / Node`
- actualizeaza preview-ul la orice sugestie sau editare

### 4. Validation layer

- valideaza ID-uri duplicate
- valideaza selectii lipsa
- valideaza tipuri incompatibile
- valideaza dependinte lipsa intre `Quest` si `Progression`
- valideaza conflictul de bounds si ancore

### 5. Visual feedback

- marcheaza optiunea recomandata cu highlight
- afiseaza sageata pe varianta corecta
- pastreaza selectia vizibila in build mode
- arata conflictele in preview si in GUI

### 6. Save flow

- confirma doar dupa preview valid
- scrie mapping-ul sau draft-ul de quest/progression
- salveaza doar schimbarile aprobate
- inregistreaza actiunea in audit

### 7. Compatibility

- respecta `maintenance mode`
- respecta lock-urile de scriere
- nu rupe fluxul existent de `mapping-harti-manuale.md`
- pastreaza compatibilitatea cu `WorldAdminService` si continutul existent

## Taskuri numerotate

### B01 Parser comenzi AI

Descriere:

- parseaza comenzi pentru `/ainpc world create ai`, `/ainpc quest create ai`, `/ainpc progression create ai`
- recunoaste aliasul `/npc` daca este activ

Acceptare:

- comanda valida deschide wizard-ul corect
- comanda invalida primeste sugestie apropiata

### B02 Wizard state persistent

Descriere:

- retine pasii completati intre `Back` si `Next`
- separa starea pentru `Build`, `Quest` si `Progression`

Acceptare:

- revenirea cu `Back` nu pierde datele introduse
- `Cancel` sterge draft-ul activ si revine curat

### B03 Preview engine

Descriere:

- genereaza preview pentru `Build`, `Quest` si `Progression`
- actualizeaza preview-ul dupa sugestii sau editari

Acceptare:

- preview-ul arata ID, nume, tip, dependinte si conflicte
- orice corectie modifica instant preview-ul

### B04 Auto-suggest ID si tip

Descriere:

- sugereaza ID-uri sanitizate
- sugereaza tipuri apropiate cand inputul este vag

Acceptare:

- spatiile devin underscore
- duplicatele primesc sufix numeric
- varianta recomandata este evidentiata

### B05 Validare conflicte

Descriere:

- blocheaza confirmarea daca exista ID duplicat, selectie lipsa sau overlap
- valideaza dependintele dintre quest si progression

Acceptare:

- confirmarea nu trece cu conflicte active
- preview-ul arata motivul blocajului

### B06 Visual feedback

Descriere:

- afiseaza highlight, sageata si badge pentru varianta recomandata
- mentine selectia vizibila pe durata build mode

Acceptare:

- optiunea recomandata este usor de observat
- selectia nu dispare pana la confirmare sau anulare

### B07 Save flow

Descriere:

- confirma doar dupa preview valid
- scrie mapping-ul sau draft-ul aprobat
- inregistreaza actiunea in audit

Acceptare:

- nimic nu se scrie fara confirmare
- auditul reflecta schimbarea finala

### B08 Maintenance compatibility

Descriere:

- respecta blocajele de mentenanta
- permite preview si inspectie chiar cand scrierea este limitata

Acceptare:

- build mode nu forteaza scrierea in maintenance
- preview-ul ramane functional

## Ordine stricta de implementare

Ordinea recomandata este:

1. `B01` Parser comenzi AI
2. `B02` Wizard state persistent
3. `B03` Preview engine
4. `B04` Auto-suggest ID si tip
5. `B05` Validare conflicte
6. `B06` Visual feedback
7. `B07` Save flow
8. `B08` Maintenance compatibility

Regula:

- fiecare pas trebuie sa lase un rezultat testabil inainte de pasul urmator
- nu se implementeaza save flow complet inainte de preview si validare
- nu se activeaza confirmarea finala fara conflict checks

## Mapare pe module

### `ainpc-core-plugin`

Responsabil pentru:

- parser comenzi
- wizard state
- preview generation
- validation
- save flow
- visual hooks

### `ainpc-api`

Responsabil pentru:

- contracte publice pentru `Quest`, `Progression`, `Region`, `Place`, `Node`
- tipuri de preview si payload-uri read-only
- extensii publice pentru wizard si sugestii

### `docs`

Responsabil pentru:

- descrierea fluxului
- ordinea de lucru
- criteriile de acceptare
- relatia cu `mapping-harti-manuale.md` si `mapping-stack.md`

## Criteria de gata pentru fluxul asistat

Fluxul asistat este gata cand:

- comanda deschide wizard-ul potrivit
- preview-ul arata tot ce urmeaza sa fie scris
- sugestiile corecteaza inputul gresit
- conflictele blocheaza confirmarea
- build mode pastreaza selectia vizibila
- compatibilitatea cu `maintenance mode` este pastrata

## Urmatorul pas tehnic

Daca implementezi efectiv, incepe cu:

1. parser comenzi
2. state wizard
3. preview engine
4. validare conflicte

Abia apoi adaugi feedback vizual, save flow si compatibilitate de mentenanta.

## Comenzi propuse

Fluxul trebuie sa poata fi pornit atat cu `/ainpc`, cat si cu aliasul operational `/npc`, daca aliasul este activ in runtime.

### Quest

```text
/ainpc quest create ai
/ainpc quest create ai <questType>
/ainpc quest create ai <questType> <preset>
/ainpc quest create ai from selection
```

### Progression

```text
/ainpc progression create ai
/ainpc progression create ai <progressionType>
/ainpc progression create ai <progressionType> <preset>
```

### Mapping si build

```text
/ainpc build mode help
/ainpc world create ai
/ainpc world create ai help
/ainpc world create ai preview region
/ainpc world create ai preview place
/ainpc world create ai preview node
/ainpc world create ai dryrun region
/ainpc world create ai inspect place
/ainpc map edit
/ainpc map open
/ainpc map gui
/ainpc build mode status
/ainpc build mode on [region|place|node]
/ainpc build mode off
/ainpc build mode sign [region|place|node]
/ainpc build mode wand [region|place|node]
/ainpc build mode point [region|place|node]
```

Compatibilitate: forma scurta `/ainpc world create ai region|place|node ...` ramane suportata, dar fluxul recomandat este `preview|dryrun|inspect` urmat de `map edit` si `map confirm`.

Semnul poate contine si hinturi AI pe linii, de exemplu:

```text
region
curte_castel
type=settlement
size=48
```

Pentru `place` sau `node`:

```text
place
fieraria_george
type=forge
region=regatul_nordic
```

```text
node
avizier
type=quest_trigger
radius=2.5
```

Pentru inspectie fara deschiderea formularului, foloseste `preview`:

```text
/ainpc world create ai preview region name=curte_castel type=castle size=48
/ainpc map edit
/ainpc map confirm
```

`/ainpc map edit`, `/ainpc map open` si `/ainpc map gui` deschid acelasi editor pentru draftul curent dupa `preview`/`dryrun`/`inspect`.

Daca draftul AI are hinturi structurate (`id=`, `name=`, `label=`, `type=`, `region=`, `place=`, `size=`, `radius=`, `height=`), dar nu exista selectie activa,
build mode foloseste pozitia curenta a builderului ca centru:

- `region` fara wand primeste implicit o zona patrata in jurul builderului
- `place` fara wand primeste o zona mai mica in jurul builderului
- `node` fara punct selectat primeste coordonata curenta a builderului
- `size=` seteaza latimea/adancimea, `radius=` seteaza distanta fata de centru, `height=` seteaza inaltimea
- daca `id=` lipseste, `name=` devine fallback pentru ID local; `label=` ramane nume afisat separat

## Flow UI propus

Wizard-ul trebuie sa fie liniar si usor de corectat.

Layout recomandat:

- `Back`
- `Next`
- `Confirm`
- `Cancel`
- `Suggest`
- `Edit`

Reguli:

- `Back` revine la campul anterior fara sa piarda datele deja introduse
- `Next` valideaza campul curent si avanseaza
- `Suggest` propune valori corecte cand inputul este incomplet sau gresit
- `Edit` permite suprascrierea campului curent
- `Confirm` scrie doar dupa preview valid
- `Cancel` sterge draft-ul activ
- `Auto rename` genereaza un id unic daca exista coliziune de nume
- `Auto fit` reduce marimea/radius-ul pana cand nu mai exista conflict de spatiu
- `Auto move` muta centrul/punctul activ intr-o pozitie libera cand exista o alternativa valida
- starea activa este vizibila si in snapshot-ul MCP intern pentru admin/debug
- exista si tool-ul MCP `ainpc.build.mode.status` pentru inspectie dedicata
- exista si tool-ul MCP `ainpc.build.mode.history [player] [limit]` pentru ultimele schimbari
- exista si tool-ul MCP `ainpc.build.mode.export [player] [limit]` pentru export compact
- comanda ` /ainpc build mode history` arata ultimele schimbari in chat
- comanda ` /ainpc build mode export` arata un sumar compact in chat
- comanda ` /ainpc build mode clear-history` curata istoricul local al playerului
- snapshot-ul MCP include si lista playerilor activi cu `style/target`
- snapshot-ul MCP include si istoricul recent al schimbarilor de build mode
- `point` captureaza pozitia blocului vizat si deschide formularul potrivit cu coordonatele precompletate
- `sign` foloseste textul semnului ca prompt si deschide formularul potrivit cu contextul dedus

### Exemple rapide

- `/ainpc build mode on region` — activeaza build mode pe regiuni
- `/ainpc build mode wand place` — deschide modul de selectie cu wand pentru place
- `/ainpc build mode point node` — captureaza un punct si deschide editorul pentru node
- `/ainpc build mode history` — arata ultimele modificari locale
- `/ainpc build mode export` — afiseaza rezumatul curent si ultimele actiuni
- `/ainpc build mode clear-history` — sterge istoricul local al playerului curent
- `/ainpc build mode help` — afiseaza sintaxa completa si exemplele disponibile

## State machine

Fluxul trebuie tratat ca o secventa de stari explicite, nu ca un dialog ad-hoc.

Stari:

- `idle`
- `build_mode_enabled`
- `draft_selected`
- `draft_prefilled`
- `draft_previewed`
- `validation_failed`
- `validation_adjusted`
- `ready_to_confirm`
- `confirmed`
- `cancelled`

Tranziții:

- `idle -> build_mode_enabled` la activarea modului
- `build_mode_enabled -> draft_selected` la selectie prin sign/wand/point
- `draft_selected -> draft_prefilled` dupa deducerea AI
- `draft_prefilled -> draft_previewed` dupa afisarea vizuala
- `draft_previewed -> validation_failed` daca lipsesc date sau exista conflict
- `validation_failed -> validation_adjusted` la `Suggest`, `Auto rename`, `Auto fit` sau `Auto move`
- `validation_adjusted -> draft_previewed` dupa reaplicarea sugestiei
- `draft_previewed -> ready_to_confirm` cand toate regulile sunt satisfacute
- `ready_to_confirm -> confirmed` la confirmare explicita
- orice stare activa -> `cancelled` la anulare sau iesire din sesiune

Reguli:

- `Confirm` nu are voie sa sara peste `validation_failed`
- `Auto rename` modifica doar id-ul
- `Auto fit` modifica doar marimea sau raza
- `Auto move` modifica doar centrul sau punctul activ
- `draft_previewed` trebuie sa ramana vizibil pana la `confirmed` sau `cancelled`

## Format de preview

Preview-ul trebuie sa afiseze clar ce urmeaza sa fie scris.

### Preview `Quest`

```text
Quest draft
- id: quest_castel_01
- name: Apararea castelului
- type: story
- objective: protejeaza poarta
- anchors: region/place/node
- progression: progression_castel_01
- reward: xp, reputation, item
- status: draft
- issues: none
```

### Preview `Progression`

```text
Progression draft
- id: progression_castel_01
- name: Apararea castelului
- scope: quest/story/area
- stages: 3
- requirements: quest_started
- rewards: unlock_dialogue
- blockers: none
- status: draft
- issues: none
```

### Preview `Build`

```text
Build draft
- mode: sign / wand / point
- target: region / place / node
- name: curte_castel
- id: curte_castel_01
- pos1: world, 100, 64, 100
- pos2: world, 120, 78, 130
- semantic type: castle_courtyard
- tags: castle, public, courtyard
- metadata: owner=castle, access=guarded
- issues: none
```

## Corectie asistata

Daca utilizatorul trimite o valoare gresita sau incompleta, sistemul trebuie sa ofere corectii rapide, nu sa esueze opac.

Corectia asistata poate folosi:

- sugestii de tip `inapoi / inainte`
- optiuni vizuale in GUI
- marcaje cu sageata pentru alegerea corecta
- listare de variante cand textul este ambiguu

Reguli:

- daca ID-ul este invalid, sistemul propune un ID sanitizat
- daca tipul nu este recunoscut, sistemul propune 2-3 tipuri apropiate
- daca `pos1` sau `pos2` lipsesc, sistemul cere selectia activa sau o comanda de setare
- daca un camp este contradictoriu, asistentul cere clarificare inainte de confirmare
- daca exista o alternativa mai buna, asistentul poate propune `Auto rename`, `Auto fit` sau `Auto move` in loc de esec direct

Aceasta asistenta trebuie sa reduca erorile la creare, fara sa elimine preview-ul sau confirmarea explicita.

## Vizualizare grafica

Selectia trebuie sa fie vizibila pe durata intregii sesiuni de build.

Forme acceptate de vizualizare:

- particule
- blocuri temporare de contur
- outline pe marginea selectiei
- marker pentru punctul activ

Cerinte:

- zonele selectate raman vizibile atat timp cat `Build Mode` este activ
- refresh-ul vizual trebuie sa fie stabil, nu intermitent
- vizualizarea nu trebuie sa distruga lumea
- vizualizarea trebuie sa diferentieze clar `Region`, `Place` si `Node`

Recomandare:

- `Region` = contur mare, vizibil de la distanta
- `Place` = contur mediu, distinct de regiune
- `Node` = marker punctual clar, cu accent vizual puternic
- `Region`/`Place`/`Node` pot afisa si sugestia AI curenta pentru a face corectia imediata

## Checklist final

Ordinea recomandata de implementare pentru cod:

1. build mode state storage
2. sign mode / wand mode / point mode
3. AI prefill pentru `Region`
4. AI prefill pentru `Place`
5. AI prefill pentru `Node`
6. auto validation pentru campuri lipsa
7. conflict checks reale
8. `Auto rename`
9. `Auto fit`
10. `Auto move`
11. preview persistent
12. confirm/cancel explicit
13. integration cu `maintenance mode`
14. documentare si help text pentru comenzile `/ainpc ... create ai`

Reguli de final:

- niciun flux AI nu confirma fara preview
- niciun conflict real nu trece fara corectie sau anulare
- corectiile automate nu modifica mai mult decat este necesar
- toate modificarile trebuie sa ramana vizibile pana la confirmare sau cancel

## Backlog pe module

### `ainpc-core-plugin`

- storage pentru starea `Build Mode`
- integrare cu `GuiService` pentru `Region`, `Place`, `Node`
- validare campuri + validare conflicte
- `Auto rename`, `Auto fit`, `Auto move`
- persistenta preview-ului si a selectiei vizibile

### `ainpc-api`

- contract stabil pentru tipurile de preview si pentru starea de selectie
- acces de citire pentru obiecte create asistat
- clarificare intre `draft`, `preview` si `confirmed`

### `docs`

- sincronizare intre fluxul real si documentatie
- help text pentru `/ainpc world create ai`
- help text pentru `/ainpc quest create ai`
- help text pentru `/ainpc progression create ai`

### Criterii de acceptare

- un draft AI poate ajunge la confirmare fara interventie manuala doar daca trece validarea
- conflictele de nume se rezolva prin `Auto rename`
- conflictele de marime se rezolva prin `Auto fit`
- conflictele de pozitie se rezolva prin `Auto move`
- daca nu exista solutie automata, UI trebuie sa ramana in starea de preview cu mesaj clar

## Persistenta sesiunii

In `Build Mode`, selectia activa trebuie sa ramana vizibila si dupa schimbari de submod, pana la:

- confirmare
- anulare
- expirare explicita a sesiunii
- inchidere sesiune admin

Daca exista mai multe zone selectate in acelasi flow, toate trebuie sa ramana vizibile pana la finalizarea operatiei curente.

## Compatibilitate cu `maintenance mode`

`Build Mode` trebuie sa fie compatibil cu `maintenance mode`.

Asta inseamna:

- poate functiona in timpul unei ferestre de mentenanta
- nu trebuie sa forteze restart
- nu trebuie sa blocheze citirea sau inspectia mapping-ului
- nu trebuie sa intre in conflict cu lock-urile de operare
- trebuie sa respecte restrictiile de scriere impuse de mentenanta

Regula practica:

- daca `maintenance mode` limiteaza scrierile, `Build Mode` poate ramane in preview
- daca `maintenance mode` permite operare controlata, `Build Mode` poate confirma doar schimbarile validate

## Tipuri de obiecte si campuri minime

### `Region`

Lista detaliata minima:

- `id`
- `displayName`
- `type`
- `world`
- `bounds`
- `tags`
- `metadata`
- `status`

Ce trebuie sa fie clar:

- ce zona mare acopera
- ce rol are in lume
- ce limite fizice are
- daca este activa, draft sau blocata de validare

### `Place`

Lista detaliata minima:

- `id`
- `regionId`
- `displayName`
- `type`
- `bounds`
- `ownerNpcId`
- `publicAccess`
- `tags`
- `metadata`

Ce trebuie sa fie clar:

- ce constructie sau zona functionala reprezinta
- cine o foloseste
- daca este privata sau publica
- cum se leaga de regiunea parinte

### `Node`

Lista detaliata minima:

- `id`
- `regionId`
- `placeId`
- `type`
- `position`
- `radius`
- `tags`
- `metadata`
- `enabled`

Ce trebuie sa fie clar:

- unde se intampla interactiunea exacta
- ce rol semantic are punctul
- ce loc parinte il contine
- daca node-ul este activ pentru runtime

## Flux recomandat

### Pentru un `Place`

1. selectezi zona cu `Sign Mode` sau `Multi-Selection Wand`
2. descrii intentia: de exemplu `place curte castel`
3. preview-ul arata numele, tipul si bounds
4. confirmi doar daca tipul si limita sunt corecte
5. sistemul scrie mapping-ul si pastreaza istoricul

### Pentru un `Node`

1. selectezi punctul sau radius-ul
2. descrii rolul: `entrance`, `workstation`, `quest_trigger`, `social`
3. preview-ul arata markerul si metadata
4. confirmi doar dupa ce markerul este vizibil si corect

### Pentru o `Region`

1. selectezi perimetrul mare
2. marchezi numele si rolul semantic
3. vizualizarea trebuie sa fie mai evidenta decat la `Place`
4. confirmi doar dupa ce limitele si relațiile sunt corecte

## Reguli de siguranta

- nu scrie direct in world mapping fara preview
- nu ascunde selectia activa cat timp sesiunea este deschisa
- nu confunda `Region` cu `Place`
- nu transforma un `Node` in `Place` doar fiindca are radius mare
- nu permite selectie fara mod explicit sau fara context clar

## Rezultat asteptat

`Build Mode` trebuie sa faca authoring-ul semantic mai rapid, mai vizibil si mai sigur decat editarea manuala a configuratiei.

In varianta corecta, adminul poate:

- marca rapid o curte de castel ca `Place`
- defini o forma neregulata cu wand multi-select
- vedea selectia mereu vizibila
- lucra fara sa piarda contextul
- ramane compatibil cu `maintenance mode`
