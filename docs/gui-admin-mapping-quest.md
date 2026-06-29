# GUI Admin Mapping si Quest

Actualizat: 2026-06-29

Punctul de intrare recomandat pentru aceasta zona este `gui-stack.md`.

## Scop

Acest document defineste o suprafata GUI de administrare pentru:

- creare, editare si stergere pentru mapping-ul de world;
- creare, editare si stergere pentru questuri;
- validare inainte de persistenta;
- confirmari explicite pentru actiuni destructive;
- audit al modificarilor.

Documentul descrie o cerinta de produs si de operare. GUI-ul este doar front-end de administrare peste servicii si comenzi existente.

## Principii

- GUI-ul admin prezinta si orchestreaza; serviciile valideaza si scriu.
- Nicio schimbare nu se persista fara validare runtime.
- Stergerea este explicita si confirmata.
- Modificarile mari se fac pe draft, apoi se salveaza.
- Orice actiune cu efect produce audit.
- Datele citite in ecran sunt snapshot-uri sau view models, nu acces direct la stare interna.

## Terminologie standard

Adminul trebuie sa pastreze aceleasi etichete scurte ca restul GUI-ului:

- `Actiuni principale` pentru lista sau navigarea initiala;
- `Admin separat` pentru actiuni cu confirmare sau permisiune;
- `Inspectie separata` pentru preview, diagnostic si comparatie;
- `Snapshot de suport` pentru metadata care ajuta la debug;
- `Panou admin separat` pentru link-uri intre zonele de editare.
- `Status: curat` / `Status: necesita save` pentru mapping-ul de lume;
- `Status: curat` / `Status: necesita atentie` pentru panoul admin quest.

## Simplificare UI/UX pentru admin

Problema principala in admin nu este lipsa de functii, ci faptul ca editarea, validarea si impactul sunt prea apropiate vizual.

Regula de simplificare:

- separa clar `view`, `edit`, `preview` si `confirm`;
- nu pune campuri editabile si actiuni destructive in acelasi prim ecran;
- afiseaza impactul inainte de confirmare;
- pastreaza un singur pas principal per ecran;
- actiunile riscante trebuie sa ceara confirmare explicita.

Aplicare practica:

- `AdminMappingGui`:
  - primul ecran arata doar lista, statusul si dependintele;
  - editarea de `Region`, `Place` si `Node` intra intr-un subecran separat;
  - stergerea nu sta langa butonul de editare.
- `AdminQuestGui`:
  - primul ecran arata doar lista de questuri, stare si warnings;
  - editarea definitionala intra in `AdminQuestEditGui`;
  - preview-ul de impact ramane separat de save.
- `ConfirmActionGui`:
  - ramane singurul loc pentru actiuni destructive;
  - trebuie sa arate actiunea exacta, tinta si efectul persistent.

Principiu de operare:

- adminul trebuie sa vada clar diferenta dintre inspectie si modificare;
- daca un ecran cere prea multe decizii simultan, trebuie spart in subecrane.

### Layout-uri simplificate recomandate

Pentru a reduce densitatea, layout-ul admin trebuie sa urmeze acelasi model peste mapping si quest:

- primul rand pentru context, cautare si warnings;
- zona centrala pentru lista sau formular;
- o zona separata pentru preview/impact;
- actiunile destructive jos, separat de edit;
- confirmarea intr-un ecran dedicat.

Aplicare directa:

- `AdminMappingGui`:
  - lista de `Region`, `Place`, `Node` in centru;
  - `preview impact` separat de `edit`;
  - `delete` clar separat si conditionat de confirmare.
- `AdminMappingEditGui`:
  - campurile de edit sunt singurele actionabile;
  - dependintele si impactul apar in lateral sau in footer.
- `AdminQuestGui`:
  - lista de questuri si warnings in centru;
  - `create`, `clone`, `edit` sus;
  - `validate`, `preview impact`, `delete` jos si separat.
- `AdminQuestEditGui`:
  - formularul principal ramane compact;
  - `save` si `discard` nu se amesteca cu actiuni destructive;
  - validation errors apar imediat, nu doar la final.

## Domenii

### Admin Mapping

Mapping-ul admin acopera:

- `Region`
- `Place`
- `Node`
- tag-uri si metadata
- relatii parinte-copil
- ancore si referinte pentru quest, spawn si world admin

Operatii suportate:

- creare regiune;
- creare loc;
- creare node;
- editare nume, tip, tag-uri, metadata, ordine si parinte;
- stergere regiune, loc sau node;
- clonare pentru variante si experimente;
- validare structura inainte de save;
- preview al impactului asupra questurilor si spawn-ului.

Reguli de stergere:

- stergerea trebuie sa fie confirmata;
- daca un obiect are dependinte, GUI-ul trebuie sa afiseze impactul;
- daca stergerea rupe ancore sau referinte, actiunea trebuie blocata sau transformata intr-un flux de remediere;
- prefera soft-delete sau arhivare daca implementarea o cere;
- nu permite stergere fara jurnal de audit.

### Admin Quest

Quest admin acopera:

- definirea questului;
- obiective si stage-uri;
- ancore;
- recompense;
- semnale narative;
- mecanica;
- vizibilitate si conditii de activare;
- dependinte intre questuri;
- stare de draft / published / archived.

Operatii suportate:

- creare quest nou din template sau gol;
- editare titlu, descriere, mecanica si etichete;
- editare obiective, stage-uri si ancore;
- editare recompense si conditii;
- stergere quest;
- clonare quest;
- preview al rezultatului in `QuestLogGui` si `QuestDetailGui`;
- validare pentru compatibilitate cu progression si story;
- export sau salvare controlata.

Reguli de stergere:

- stergerea trebuie confirmata explicit;
- daca questul este activ, actiunea trebuie blocata sau convertita intr-un flux de retragere;
- daca exista progres salvat, trebuie prezentat impactul;
- daca questul are dependinte, GUI-ul trebuie sa arate relatiile afectate;
- orice stergere trebuie inregistrata in audit.

## Flux de lucru

### Mapping

1. Adminul deschide `WorldHubGui` sau un hub dedicat de mapping.
2. Alege tipul de obiect: region, place sau node.
3. Deschide editorul de create sau edit.
4. Modifica campurile permise.
5. Ruleaza validarea.
6. Vede preview-ul impactului.
7. Salveaza cu confirmare.
8. Stergerea cere `ConfirmActionGui`.

### Quest

1. Adminul deschide un hub de quest admin sau `QuestAuthoringGui` extins cu editare.
2. Alege `create`, `edit`, `clone` sau `delete`.
3. Modifica definitia questului.
4. Ruleaza validarea pe objective, anchor, story si progression.
5. Vede preview-ul in lista si detalii.
6. Salveaza sau anuleaza.
7. Stergerea cere confirmare si afiseaza dependentele.

## Componente GUI propuse

- `AdminMappingGui` pentru lista si editor de world mapping.
- `AdminMappingEditGui` pentru detalii de region/place/node.
- `AdminQuestGui` pentru lista si editor de questuri.
- `AdminQuestEditGui` pentru draft si publicare.
- `ConfirmActionGui` pentru stergeri si actiuni cu impact mare.
- `DiffPreviewGui` pentru comparatie inainte / dupa.
- `DependencyWarningGui` pentru referinte afectate.

## Permisiuni

- `ainpc.admin` pentru acces general.
- `ainpc.gui.world` pentru mapping.
- `ainpc.gui.quest` pentru quest admin.
- `ainpc.gui.audit` pentru inspectarea impactului si a jurnalului.

Toate actiunile destructive trebuie sa pastreze un gate de confirmare chiar si cand userul are drepturi de admin.

## Validare

Validarea minima trebuie sa verifice:

- identitatea obiectului;
- formatul campurilor;
- unicitatea ID-urilor;
- consistenta tag-urilor;
- consistenta relatiilor parinte-copil;
- dependintele afectate;
- impactul asupra questurilor si spawn-ului;
- compatibilitatea cu story si progression;
- drepturile utilizatorului.

## Audit si observabilitate

Fiecare actiune trebuie sa poata fi urmarita cu:

- user;
- timestamp;
- tip actiune;
- tinta;
- rezultat;
- validator folosit;
- dependinte afectate;
- motiv de blocare, daca exista.

## Reguli pentru AI

- Daca un alt AI consuma documentul, trebuie sa trateze mapping si quest ca doua fluxuri separate, cu acelasi model de siguranta.
- Nu presupune ca stergerea este permisa doar pentru ca exista ecranul; verifica regula de confirmare si dependinte.
- Nu interpreta `draft` ca `published`.
- Nu confunda preview-ul cu persistenta.

## Creator / Authoring

`QuestAuthoringGui` trebuie tratat separat de adminul complet de editare. Scopul lui este inspectie si authoring usor, nu editare completa de productie.

Reguli de simplificare pentru creator:

- ecranul principal arata doar questul curent, mecanica si starea draft;
- actiunile principale sunt `next`, `prev`, `reset` si `dump`;
- nu amesteca in acelasi nivel inspectia curenta cu editarea directa a tuturor campurilor;
- orice functionalitate de modificare profunda trebuie sa intre intr-un flux dedicat de editare, nu in authoring read-only;
- creatorul trebuie sa poata citi rapid warnings, stage-uri si obiective fara sa navigheze prin prea multe submeniuri.

Aplicare practica:

- `QuestAuthoringGui`:
  - arata în prim plan questul selectat si mecanica selectata;
  - pastreaza butoanele de ciclu si reset vizibile;
  - dump-ul si debug-ul raman actiuni secundare, nu actiunea principala;
  - daca exista warnings, acestea trebuie sa apara imediat in rezumat.

Principiu:

- creatorul lucreaza mai des cu context si inspectie decat cu editare grea;
- reducerea numarului de actiuni vizibile face authoring-ul mai sigur si mai rapid.

## AdminMappingGui (stare curenta)

`AdminMappingGui` este ecranul principal de administrare a mapping-ului, 54 sloturi:

```text
0       wand toggle
1       bindings list
2       exterior analyze
4       header: status mapping (regiuni, places, noduri, auto-index, modificari)
6       identitate regiune curenta (tip, descriere, story, mood, threat, atmosfera)
8       reload config
9       noduri apropiate
10      locatie curenta
11      whereami
12      mapping debugdump
13      scan sat
14      demo mapping create
15      save mapping
16      toggle auto-index
17      admin quest
18      tipuri regiuni
22      patch analyze
23      patch plan
26      lumi
27      tipuri noduri
44-46   paginare regiuni
45      identitate regiune (comanda text)
47      create place (cu parametri corecti: 12 argumente)
48      create node (cu parametri corecti: 8+ argumente)
50      remove place
51      listeaza places
52      listeaza noduri
```

Imbunatatiri fata de versiunea initiala:
- card identitate regiune la slot 6 cu displayName, descriere, story, mood, threat, atmosfera
- regiunile din lista arata identity.displayName si mood si threatLevel
- butoane `Patch analyze` si `Patch plan` pentru analiza rapida a decalajelor
- comenzi corecte pentru create place (12 parametri: regionId, id, type, 6 bounds) si create node (regionId, placeId, id, type, x, y, z, radius)
- buton `Identitate regiune` pentru comanda text `/ainpc world region identity`

## Legaturi

- `docs/gui-interfete.md`
- `docs/harta-clase-gui.md`
- `docs/mapping.md`
- `docs/questuri-avansate-v2.md`
- `docs/taskuri-prioritizate.md`
