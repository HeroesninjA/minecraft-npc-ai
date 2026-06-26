# DeepSeek Taskuri - Batch 9 (L451-L500)

Actualizat: 2026-06-25

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Automatizare, verificare si continuitate

### L451 Verificare pentru seria activa
**Descriere tehnica:** Confirma ca seria activa porneste din ultimul batch publicat si nu se suprapune cu arhiva.
**Scop:** Evita confuziile intre continutul activ si istoricul mutat.
**Target:** Index docs, batch guide.
**Prompt AI:** Creeaza o verificare care confirma punctul de start al seriei active DeepSeek.
**Acceptare:** Seria activa este identificata fara ambiguitate.

### L452 Audit pentru lista de arhiva
**Descriere tehnica:** Verifica daca README-ul din arhiva listeaza toate batch-urile mutate.
**Scop:** Pastreaza istoricul complet.
**Target:** `./arhiva/README.md`.
**Prompt AI:** Creeaza un audit read-only care compara lista arhivei cu fisierele mutate.
**Acceptare:** Orice fisier lipsa este raportat clar.

### L453 Guard pentru linkuri ramase vechi
**Descriere tehnica:** Detecteaza referintele care mai folosesc calea veche a batch-urilor mutate.
**Scop:** Reduce linkurile moarte.
**Target:** Docs lint, reference checker.
**Prompt AI:** Adauga un guard care semnaleaza linkurile vechi ramase in documente.
**Acceptare:** Referintele invechite sunt identificate automat.

### L454 Template pentru start de batch nou
**Descriere tehnica:** Standardizeaza blocul de inceput pentru un batch DeepSeek nou.
**Scop:** Reduce variatia dintre batch-uri.
**Target:** Docs template, authoring guide.
**Prompt AI:** Creeaza un template reutilizabil pentru deschiderea unui batch nou.
**Acceptare:** Un batch nou poate porni fara format ad-hoc.

### L455 Guard pentru limita de 50 taskuri
**Descriere tehnica:** Verifica daca un batch nu depaseste limita maxima declarata.
**Scop:** Pastreaza dimensiunea controlata.
**Target:** Batch validator, docs lint.
**Prompt AI:** Adauga o regula care blocheaza batch-urile cu peste 50 de taskuri.
**Acceptare:** Depasirile sunt raportate inainte de publicare.

### L456 Guard pentru numerotare consecutiva
**Descriere tehnica:** Valideaza ca taskurile din batch cresc strict in ordine.
**Scop:** Evita gap-urile si ordinea confuza.
**Target:** Numbering audit, batch guide.
**Prompt AI:** Creeaza un guard care verifica numerotarea consecutiva a taskurilor.
**Acceptare:** Orice ruptura de secventa este semnalata.

### L457 Guard pentru dubluri de numar
**Descriere tehnica:** Detecteaza doua taskuri care folosesc acelasi numar L.
**Scop:** Previne coliziunile intre taskuri.
**Target:** Numbering validator.
**Prompt AI:** Adauga un guard care detecteaza numerele duplicate intr-un batch DeepSeek.
**Acceptare:** Dublurile sunt raportate explicit.

### L458 Guard pentru titluri duplicate
**Descriere tehnica:** Verifica daca doua taskuri au acelasi titlu semantic.
**Scop:** Reduce confuzia la review.
**Target:** Title audit, docs lint.
**Prompt AI:** Creeaza un audit care raporteaza titlurile prea apropiate sau identice.
**Acceptare:** Titlurile duplicate apar in raport.

### L459 Smoke check pentru indexul principal
**Descriere tehnica:** Verifica daca indexul principal afiseaza noul batch si arhiva corect.
**Scop:** Pastreaza navigarea stabila.
**Target:** `../README.md`.
**Prompt AI:** Creeaza un smoke check pentru linkurile relevante din indexul principal.
**Acceptare:** Indexul principal pointeaza la locatiile curente.

### L460 Smoke check pentru indexul arhivei
**Descriere tehnica:** Verifica daca indexul arhivei expune noul folder DeepSeek.
**Scop:** Pastreaza accesul la istoricul mutat.
**Target:** `../index-arhiva.md`.
**Prompt AI:** Adauga un smoke check pentru lista de arhive si linkurile ei.
**Acceptare:** Arhiva DeepSeek este vizibila din index.

### L461 Changelog pentru mutari de batch
**Descriere tehnica:** Standardizeaza o intrare de changelog pentru fiecare mutare de batch in arhiva.
**Scop:** Pastreaza istoria deciziilor.
**Target:** Changelog, maintenance log.
**Prompt AI:** Creeaza un format scurt pentru changelog-ul mutarilor de batch DeepSeek.
**Acceptare:** Orice mutare are o intrare clara.

### L462 Raport scurt de mutare
**Descriere tehnica:** Genereaza un raport minimal cu fisierele mutate si noile lor locatii.
**Scop:** Simplifica verificarea post-mutare.
**Target:** Docs ops, maintenance report.
**Prompt AI:** Scrie un raport scurt pentru o mutare de batch in arhiva.
**Acceptare:** Raportul enumera clar sursa si destinatia.

### L463 Raport pentru seria activa
**Descriere tehnica:** Produce o fotografia a documentelor care raman in zona activa dupa arhivare.
**Scop:** Ajuta la handoff si verificare.
**Target:** Status note, docs summary.
**Prompt AI:** Creeaza un raport de stare pentru seria activa DeepSeek.
**Acceptare:** Cititorul vede rapid ce este activ.

### L464 Audit pentru fisiere lipsa
**Descriere tehnica:** Compara lista din index cu fisierele reale de pe disk.
**Scop:** Detecteaza promisiuni neacoperite.
**Target:** Docs audit, index validator.
**Prompt AI:** Adauga un audit care detecteaza fisierele listate, dar inexistente.
**Acceptare:** Orice lipsa este raportata.

### L465 Audit pentru heading-uri lipsa
**Descriere tehnica:** Verifica daca fiecare batch are heading-urile obligatorii.
**Scop:** Pastreaza formatul canonic.
**Target:** Markdown lint, batch validator.
**Prompt AI:** Creeaza un audit care semnaleaza batch-urile fara headerul standard.
**Acceptare:** Lipsa structurii de baza este raportata.

### L466 Audit pentru acceptare incompleta
**Descriere tehnica:** Verifica daca fiecare task are criteriu de acceptare suficient de clar.
**Scop:** Evita taskurile greu de validat.
**Target:** Task quality audit.
**Prompt AI:** Adauga un audit care identifica taskurile cu acceptare vaga.
**Acceptare:** Taskurile ambigue sunt marcate pentru rescriere.

### L467 Audit pentru scop neclar
**Descriere tehnica:** Detecteaza taskurile care nu definesc clar rezultatul urmarit.
**Scop:** Pastreaza taskurile actionabile.
**Target:** Task quality audit.
**Prompt AI:** Creeaza un audit care semnaleaza taskurile fara scop clar.
**Acceptare:** Orice scop vag este raportat.

### L468 Audit pentru target neprecis
**Descriere tehnica:** Verifica daca fiecare task are o zona tehnica concreta.
**Scop:** Reduce taskurile prea largi.
**Target:** Task schema audit.
**Prompt AI:** Adauga un audit care semnaleaza taskurile fara target precis.
**Acceptare:** Fiecare task are o tinta tehnica clara.

### L469 Audit pentru separarea scop/acceptare
**Descriere tehnica:** Confirma ca scopul si acceptarea sunt descrise separat.
**Scop:** Pastreaza structura standard.
**Target:** Task schema audit.
**Prompt AI:** Creeaza un audit care verifica separarea dintre scop si acceptare.
**Acceptare:** Criteriile nu sunt amestecate cu intentia.

### L470 Guard pentru limbaj stabil
**Descriere tehnica:** Verifica daca formularea taskurilor ramane scurta si uniforma.
**Scop:** Evita stilul inconsistent.
**Target:** Docs style lint.
**Prompt AI:** Adauga un guard pentru stilul scurt si uniform al taskurilor DeepSeek.
**Acceptare:** Abaterile de stil sunt semnalate.

### L471 Guard pentru target duplicat
**Descriere tehnica:** Detecteaza cand mai multe taskuri din acelasi batch tintesc aceeasi zona prea strans.
**Scop:** Previne redundanta.
**Target:** Redundancy checker.
**Prompt AI:** Creeaza un guard care raporteaza targeturile prea apropiate.
**Acceptare:** Redundanta evidenta este marcata.

### L472 Guard pentru dependinte ascunse
**Descriere tehnica:** Avertizeaza cand un task presupune schimbari in mai multe documente fara sa o spuna.
**Scop:** Face impactul clar.
**Target:** Dependency audit.
**Prompt AI:** Adauga un guard pentru taskurile care ascund dependinte multiple.
**Acceptare:** Taskurile cu impact larg sunt raportate.

### L473 Regula pentru mutarea in arhiva
**Descriere tehnica:** Specifica momentul in care un batch este declarat istoric si mutat.
**Scop:** Formalizeaza tranzitia.
**Target:** Archive policy, maintenance guide.
**Prompt AI:** Scrie o regula scurta pentru declararea unui batch ca arhivat.
**Acceptare:** Momentul mutarii este clar.

### L474 Regula pentru read-only pe arhiva
**Descriere tehnica:** Stabileste ca fisierele istorice se modifica doar pentru corecturi de navigare sau clarificari.
**Scop:** Protejeaza istoricul.
**Target:** Archive policy.
**Prompt AI:** Adauga o regula read-only pentru documentele arhivate.
**Acceptare:** Schimbarile majore in arhiva sunt evitate.

### L475 Regula pentru restaurare temporara
**Descriere tehnica:** Defineste cand un fisier arhivat poate reveni temporar in zona activa.
**Scop:** Permite recuperare controlata.
**Target:** Restore policy.
**Prompt AI:** Creeaza o regula pentru restaurarea temporara din arhiva DeepSeek.
**Acceptare:** Restaurarea are criterii clare.

### L476 Regula pentru nume canonice
**Descriere tehnica:** Pastreaza numele canonice ale batch-urilor indiferent de locatie.
**Scop:** Usureaza trasabilitatea.
**Target:** Naming policy.
**Prompt AI:** Scrie o regula pentru numele canonice ale batch-urilor si folderelor.
**Acceptare:** Numele raman stabile.

### L477 Regula pentru citarea sursei
**Descriere tehnica:** Cere citarea caii corecte cand un document este mentionat din alta zona.
**Scop:** Evita referintele gresite.
**Target:** Citation policy.
**Prompt AI:** Adauga o regula pentru citarea batch-urilor DeepSeek din documente noi.
**Acceptare:** Citarea indica locatia reala.

### L478 Nota pentru curatarea referintelor
**Descriere tehnica:** Marcheaza ce referinte vechi trebuie actualizate dupa o mutare.
**Scop:** Evita linkurile moarte.
**Target:** Cleanup note, maintenance log.
**Prompt AI:** Creeaza o nota scurta pentru curatarea referintelor dupa arhivare.
**Acceptare:** Referintele vechi sunt identificate explicit.

### L479 Sumar pentru handoff
**Descriere tehnica:** Produce un rezumat scurt care explica ce a ramas activ si ce a fost arhivat.
**Scop:** Ajuta la predarea contextului.
**Target:** Handoff note, docs summary.
**Prompt AI:** Scrie un sumar de handoff pentru seria DeepSeek dupa arhivare.
**Acceptare:** Cititorul intelege starea curenta.

### L480 Placeholder pentru urmatorul ciclu
**Descriere tehnica:** Rezerva o pozitie documentara pentru urmatorul interval numeric.
**Scop:** Face extinderea previzibila.
**Target:** Batch planning, next cycle note.
**Prompt AI:** Creeaza un placeholder pentru urmatorul ciclu DeepSeek.
**Acceptare:** Trecerea la urmatorul interval este evidenta.

### L481 Template pentru batch viitor
**Descriere tehnica:** Definește un sablon care poate fi reutilizat pentru batch-ul urmator.
**Scop:** Reduce munca manuala.
**Target:** Docs template, authoring guide.
**Prompt AI:** Scrie un template pentru pornirea unui batch DeepSeek viitor.
**Acceptare:** Sablonul poate fi copiat fara ajustari mari.

### L482 Template pentru prompt stabil
**Descriere tehnica:** Pastreaza structura promptului identica pentru taskuri similare.
**Scop:** Reduce variatia generata de AI.
**Target:** Prompt template, task generator.
**Prompt AI:** Creeaza un template stabil de prompt pentru taskurile noi.
**Acceptare:** Prompturile similare folosesc acelasi cadru.

### L483 Template pentru acceptare stabila
**Descriere tehnica:** Normalizeaza formatul criteriilor de acceptare.
**Scop:** Face review-ul mai simplu.
**Target:** Acceptance schema, docs guide.
**Prompt AI:** Adauga un template standard pentru sectiunea de acceptare.
**Acceptare:** Criteriile sunt scrise in acelasi stil.

### L484 Regula pentru separarea scopului
**Descriere tehnica:** Impune separarea explicita dintre scop si verificare.
**Scop:** Evita taskurile amestecate.
**Target:** Task schema policy.
**Prompt AI:** Scrie o regula scurta pentru separarea scopului de acceptare.
**Acceptare:** Intentia si verificarea nu se suprapun.

### L485 Checklist pentru arhivare
**Descriere tehnica:** Listeaza verificarile minime inainte de a muta un batch in arhiva.
**Scop:** Reduce erorile de publicare.
**Target:** Archive checklist.
**Prompt AI:** Creeaza o checklist scurta pentru arhivarea unui batch DeepSeek.
**Acceptare:** Verificarile esentiale sunt acoperite.

### L486 Checklist pentru restaurare
**Descriere tehnica:** Listeaza verificarile minime inainte de a readuce un document arhivat.
**Scop:** Pastreaza recuperarea controlata.
**Target:** Restore checklist.
**Prompt AI:** Creeaza o checklist scurta pentru restaurarea unui document DeepSeek.
**Acceptare:** Restaurarea nu rupe navigarea.

### L487 Checklist pentru sincronizarea indexului
**Descriere tehnica:** Verifica daca indexurile reflecta corect noua structura dupa mutari.
**Scop:** Previne divergenta dintre documente si navigare.
**Target:** Index sync checklist.
**Prompt AI:** Adauga o checklist pentru sincronizarea indexului dupa arhivare.
**Acceptare:** Indexurile sunt actualizate complet.

### L488 Checklist pentru publicare
**Descriere tehnica:** Definește verificarea finala inainte de a publica un nou batch.
**Scop:** Reduce greselile de livrare.
**Target:** Publish checklist.
**Prompt AI:** Scrie o checklist finala pentru publicarea unui batch DeepSeek.
**Acceptare:** Publicarea se face doar dupa trecerea verificarii.

### L489 Checklist pentru audit
**Descriere tehnica:** Standardizeaza pasii unui audit citit-only asupra batch-urilor.
**Scop:** Pastreaza auditul repetabil.
**Target:** Audit checklist.
**Prompt AI:** Creeaza o checklist pentru auditul batch-urilor DeepSeek.
**Acceptare:** Auditul poate fi reluat identic.

### L490 Checklist pentru raportul de schimbare
**Descriere tehnica:** Stabileste ce trebuie sa apara intr-un raport scurt de schimbare.
**Scop:** Face rapoartele usor de parcurs.
**Target:** Change report checklist.
**Prompt AI:** Adauga o checklist pentru raportul de schimbare al batch-urilor.
**Acceptare:** Raportul include sursa, destinatia si motivul.

### L491 Verificare pentru continuitatea seriei
**Descriere tehnica:** Confirma ca seria poate continua imediat dupa L500.
**Scop:** Pastreaza seria extensibila.
**Target:** Continuity audit.
**Prompt AI:** Creeaza o verificare care confirma continuitatea seriei DeepSeek dupa acest batch.
**Acceptare:** Urmatorul interval poate fi pornit direct.

### L492 Regula pentru normalizarea linkurilor
**Descriere tehnica:** Standardizeaza calea relativa folosita in linkurile documentelor.
**Scop:** Reduce linkurile fragile.
**Target:** Link policy.
**Prompt AI:** Scrie o regula pentru normalizarea linkurilor dintre documentele DeepSeek.
**Acceptare:** Calele sunt consistente.

### L493 Regula pentru stabilitatea folderelor
**Descriere tehnica:** Pastreaza numele folderelor arhivate si active stabile in timp.
**Scop:** Evita mutarile inutile.
**Target:** Folder naming policy.
**Prompt AI:** Adauga o regula pentru stabilitatea numelor de foldere in documentatie.
**Acceptare:** Folderele nu se redenumesc fara motiv.

### L494 Nota despre responsabilitate
**Descriere tehnica:** Precizeaza cine actualizeaza seria activa si cine mentine arhiva.
**Scop:** Clarifica ownership-ul documentelor.
**Target:** Ownership note.
**Prompt AI:** Scrie o nota scurta despre responsabilitatea pentru seria activa si arhiva.
**Acceptare:** Rolurile sunt clare.

### L495 Nota pentru consum AI
**Descriere tehnica:** Explica cum trebuie folosite documentele de catre un model extern.
**Scop:** Reduce interpretarea gresita.
**Target:** AI usage note.
**Prompt AI:** Adauga o nota despre cum trebuie citite seria activa si arhiva de catre AI.
**Acceptare:** Citirea automata ramane stabila.

### L496 Nota pentru review uman
**Descriere tehnica:** Marcheaza ce trebuie verificat manual inainte de o mutare sau restaurare.
**Scop:** Pastreaza controlul uman asupra schimbarilor sensibile.
**Target:** Human review note.
**Prompt AI:** Creeaza o nota scurta despre punctele care cer review uman.
**Acceptare:** Cazurile sensibile sunt identificate.

### L497 Watchlist pentru regresii documentare
**Descriere tehnica:** Aduna riscurile documentare pe care trebuie sa le urmaresti dupa mutari.
**Scop:** Previne degradarea navigarii.
**Target:** Regression watchlist.
**Prompt AI:** Scrie o watchlist scurta pentru regresiile de documentatie DeepSeek.
**Acceptare:** Problemele tipice sunt enumerate.

### L498 Nota finala pentru batch-ul 9
**Descriere tehnica:** Rezuma extinderea L451-L498 si relatia cu arhiva existenta.
**Scop:** Inchide batch-ul cu un rezumat canonic.
**Target:** Release note, docs index.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L451-L498.
**Acceptare:** Documentul marcheaza clar ce a fost adaugat si de ce.

### L499 Pregatire pentru urmatorul pas
**Descriere tehnica:** Pregateste trecerea catre intervalul urmator fara a schimba formatul.
**Scop:** Face extinderea predictibila.
**Target:** Next batch note.
**Prompt AI:** Creeaza o nota scurta care pregateste urmatorul batch DeepSeek.
**Acceptare:** Cititorul vede clar continuarea.

### L500 Nota finala pentru batch-ul 9
**Descriere tehnica:** Rezuma extinderea L451-L500 si continuitatea seriei dupa arhivare.
**Scop:** Inchide seria curenta cu un rezumat canonic.
**Target:** Release note, docs index, changelog.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L451-L500.
**Acceptare:** Documentul marcheaza clar ce a fost adaugat si de ce.


