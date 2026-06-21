# GUI Admin Mapping si Quest

Actualizat: 2026-06-21

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

## Legaturi

- `docs/gui-interfete.md`
- `docs/harta-clase-gui.md`
- `docs/mapping.md`
- `docs/questuri-avansate-v2.md`
- `docs/taskuri-prioritizate.md`
