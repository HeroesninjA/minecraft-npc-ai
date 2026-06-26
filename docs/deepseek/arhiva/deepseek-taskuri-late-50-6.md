# DeepSeek Taskuri - Batch 8 (L401-L450)

Actualizat: 2026-06-25

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Arhivare, continuitate si igiena istorica

### L401 Arhiva dedicata pentru batch-urile DeepSeek
**Descriere tehnica:** Creeaza un folder de arhiva clar pentru batch-urile deja realizate, fara sa amestece istoricul cu backlog-ul activ.
**Scop:** Separa documentele istorice de seria curenta.
**Target:** `./arhiva/`, index arhiva.
**Prompt AI:** Creeaza structura de arhiva dedicata pentru batch-urile DeepSeek deja realizate.
**Acceptare:** Exista un loc unic unde sunt mutate batch-urile istorice.

### L402 README pentru arhiva DeepSeek
**Descriere tehnica:** Defineste un README scurt pentru arhiva DeepSeek cu lista documentelor istorice si motivul mutarii.
**Scop:** Face istoricul navigabil fara a deschide fiecare fisier.
**Target:** `./arhiva/README.md`.
**Prompt AI:** Scrie un README de arhiva care explica ce batch-uri au fost mutate si de ce.
**Acceptare:** Un cititor intelege rapid ce este arhivat si ce ramane activ.

### L403 Link din documentatia activa catre arhiva
**Descriere tehnica:** Actualizeaza indexurile active sa arate clar unde se afla documentele istorice.
**Scop:** Evita linkuri moarte si cautari manuale.
**Target:** `../README.md`, `../index-arhiva.md`.
**Prompt AI:** Adauga linkuri clare catre arhiva DeepSeek din indexurile principale.
**Acceptare:** Documentele arhivate pot fi deschise direct din index.

### L404 Link invers din arhiva spre seria activa
**Descriere tehnica:** Pune in arhiva o trimitere explicita catre batch-ul curent, ca sa fie clara continuarea.
**Scop:** Pastreaza contextul dintre istoricul mutat si seria activa.
**Target:** README-ul din arhiva DeepSeek.
**Prompt AI:** Adauga o trimitere spre batch-ul activ care continua seria.
**Acceptare:** Un cititor din arhiva stie imediat unde continua seria.

### L405 Regula de mutare pentru batch-uri finalizate
**Descriere tehnica:** Formalizeaza cand un batch trece din zona activa in arhiva.
**Scop:** Evita ambiguitatea intre activ si istoric.
**Target:** Docs policy, archive workflow.
**Prompt AI:** Scrie o regula simpla pentru mutarea batch-urilor finalizate in arhiva.
**Acceptare:** Exista un criteriu clar pentru arhivare.

### L406 Nume stabil pentru batch-urile arhivate
**Descriere tehnica:** Pastreaza aceleasi nume de fisiere in arhiva ca in seria activa, pentru trasabilitate.
**Scop:** Reduce confuziile la audit si referinte.
**Target:** Archive naming convention.
**Prompt AI:** Documenteaza o conventie stabila de nume pentru batch-urile arhivate.
**Acceptare:** Fisierele istorice sunt usor de recunoscut.

### L407 Inventar al batch-urilor mutate
**Descriere tehnica:** Creeaza o lista scurta cu batch-urile deja arhivate si intervalele lor numerice.
**Scop:** Face auditul istoric mai rapid.
**Target:** Archive README, index history.
**Prompt AI:** Genereaza un inventar al batch-urilor DeepSeek mutate in arhiva.
**Acceptare:** Toate batch-urile istorice apar in inventar.

### L408 Audit read-only pentru fisiere duplicate
**Descriere tehnica:** Verifica daca acelasi batch nu apare simultan in zona activa si in arhiva.
**Scop:** Previne dublurile si inconsistenta.
**Target:** Docs audit, archive validator.
**Prompt AI:** Creeaza un audit read-only care detecteaza duplicatele dintre active si arhiva.
**Acceptare:** Orice dublura este raportata explicit.

### L409 Warning pentru referinte ramase in urma mutarii
**Descriere tehnica:** Detecteaza linkurile care mai trimit la numele vechi al fisierelor mutate.
**Scop:** Evita referinte moarte dupa arhivare.
**Target:** Docs lint, reference checker.
**Prompt AI:** Adauga un warning pentru linkurile care nu mai corespund locului actual al fisierului.
**Acceptare:** Referintele invechite sunt identificate automat.

### L410 Actualizare automata a indexului de documente
**Descriere tehnica:** Sincronizeaza indexul principal dupa mutarea documentelor istorice.
**Scop:** Pastreaza navigarea corecta.
**Target:** `../README.md`, generator index.
**Prompt AI:** Creeaza o regula sau un script care actualizeaza indexul dupa arhivare.
**Acceptare:** Indexul reflecta structura curenta fara editare manuala riscanta.

### L411 Regula pentru continuitatea numerotarii
**Descriere tehnica:** Verifica faptul ca numerele L raman consecutive peste batch-uri si nu sar nejustificat.
**Scop:** Pastreaza seria usor de urmarit.
**Target:** Numbering audit, batch guide.
**Prompt AI:** Adauga o regula care valideaza continuitatea numerotarii pe toata seria DeepSeek.
**Acceptare:** Lipsurile sau suprapunerile sunt raportate clar.

### L412 Sumar al seriei active
**Descriere tehnica:** Produce un rezumat foarte scurt al batch-ului curent si al pozitionarii lui fata de istoricul arhivat.
**Scop:** Ajuta la handoff intre mentenanti.
**Target:** Release note, docs summary.
**Prompt AI:** Scrie un sumar de o pagina pentru seria DeepSeek activa.
**Acceptare:** Un cititor nou intelege ce este activ si ce este istoric.

### L413 Nota despre boundary-ul dintre activ si istoric
**Descriere tehnica:** Marcheaza explicit limita dintre seria activa si batch-urile arhivate.
**Scop:** Evita interpretari gresite despre starea backlog-ului.
**Target:** README, archive note.
**Prompt AI:** Adauga o nota care explica unde se termina seria activa si unde incepe arhiva.
**Acceptare:** Limita de stare este clara si usor de gasit.

### L414 Check pentru linkuri catre batch-uri istorice
**Descriere tehnica:** Verifica toate linkurile care mai trimit la batch-uri vechi.
**Scop:** Reduce linkurile invalide dupa mutare.
**Target:** Docs checker, navigation audit.
**Prompt AI:** Creeaza un check care listeaza toate referintele la batch-uri istorice.
**Acceptare:** Lista completa poate fi auditata rapid.

### L415 Politica pentru mutarea progresiva a batch-urilor
**Descriere tehnica:** Permite mutarea in arhiva pe loturi, nu doar toate odata, cand seria devine mai mare.
**Scop:** Face mentenanta scalabila.
**Target:** Archive policy, maintenance notes.
**Prompt AI:** Scrie o regula pentru arhivarea progresiva a batch-urilor deja finalizate.
**Acceptare:** Arhiva poate creste fara schimbare de proces.

### L416 Template pentru mesajul de arhivare
**Descriere tehnica:** Standardizeaza textul scurt folosit cand un batch este mutat.
**Scop:** Pastreaza consistenta intre documente.
**Target:** Changelog template, archive note.
**Prompt AI:** Creeaza un template scurt pentru mesajele de arhivare.
**Acceptare:** Mesajele de arhivare au aceeasi forma de baza.

### L417 Template pentru inventarul arhivei
**Descriere tehnica:** Standardizeaza modul in care sunt listate documentele istorice in README-ul arhivei.
**Scop:** Face arhiva usor de extins.
**Target:** Archive README, docs template.
**Prompt AI:** Creeaza un template de tabela pentru documentele arhivate.
**Acceptare:** O noua intrare se adauga fara schimbare de structura.

### L418 Regula pentru descrieri de motiv la arhivare
**Descriere tehnica:** Cere un motiv clar pentru fiecare document mutat.
**Scop:** Pastreaza traseul decizional.
**Target:** Archive policy, changelog.
**Prompt AI:** Adauga o regula prin care fiecare document arhivat are un motiv explicit.
**Acceptare:** Fiecare intrare din arhiva explica de ce a fost mutata.

### L419 Scurt raport de mutare
**Descriere tehnica:** Produce un raport minimal care spune ce fisiere au fost mutate si unde.
**Scop:** Simplifica verificarea post-mutare.
**Target:** Maintenance report, docs ops.
**Prompt AI:** Scrie un raport scurt despre mutarea batch-urilor DeepSeek in arhiva.
**Acceptare:** Raportul enumera clar sursa si destinatia.

### L420 Audit de trasabilitate
**Descriere tehnica:** Verifica daca numele si intervalele batch-urilor pot fi urmarite din index pana la fisier.
**Scop:** Face auditul documentatiei mai robust.
**Target:** Traceability audit, archive docs.
**Prompt AI:** Creeaza un audit read-only pentru trasabilitatea batch-urilor DeepSeek.
**Acceptare:** Orice salt sau lipsa de trasabilitate este raportata.

### L421 Guard pentru ordinea cronologica
**Descriere tehnica:** Verifica daca documentele active si arhivate sunt afisate in ordine logica.
**Scop:** Evita navigarea confuza.
**Target:** Docs index, archive index.
**Prompt AI:** Adauga un guard care valideaza ordinea cronologica a batch-urilor in index.
**Acceptare:** Ordinea gresita este detectata.

### L422 Guard pentru suprapuneri de intervale
**Descriere tehnica:** Detecteaza daca doua batch-uri pretind acelasi interval L.
**Scop:** Previne coliziunile de numerotare.
**Target:** Batch guide, numbering validator.
**Prompt AI:** Creeaza un guard care detecteaza suprapuneri de intervale intre batch-uri.
**Acceptare:** Orice suprapunere este semnalata automat.

### L423 Guard pentru gap-uri numerice
**Descriere tehnica:** Depisteaza intervalele lipsa dintre batch-uri.
**Scop:** Face seria completa si verificabila.
**Target:** Batch guide, numbering audit.
**Prompt AI:** Creeaza un guard care raporteaza gap-urile din secventa DeepSeek.
**Acceptare:** Lipsa unui interval este vizibila imediat.

### L424 Nota pentru batch-uri partial arhivate
**Descriere tehnica:** Definește cum sunt tratate batch-urile care sunt mutate doar partial.
**Scop:** Acopera situatiile de tranzitie.
**Target:** Archive policy, migration notes.
**Prompt AI:** Scrie o regula pentru batch-urile arhivate partial sau pe etape.
**Acceptare:** Statiile intermediare sunt descrise fara ambiguitate.

### L425 Nota pentru batch-uri reluate
**Descriere tehnica:** Explica cum se marcheaza un batch care revine temporar din arhiva in zona activa.
**Scop:** Evita confuzia la revenire.
**Target:** Archive policy, restore note.
**Prompt AI:** Adauga o regula pentru batch-urile care sunt readuse temporar din arhiva.
**Acceptare:** Revenirea din arhiva este explicit marcata.

### L426 Politica pentru read-only pe arhiva
**Descriere tehnica:** Stabileste ca documentele istorice se citesc, nu se rescriu fara motiv.
**Scop:** Protejeaza istoricul.
**Target:** Archive README, maintenance policy.
**Prompt AI:** Scrie o regula prin care arhiva este tratata ca read-only by default.
**Acceptare:** Modificarile in arhiva cer motiv clar.

### L427 Politica pentru actualizari minime in arhiva
**Descriere tehnica:** Limiteaza editarea documentelor istorice la corecturi de navigare sau metadate.
**Scop:** Pastreaza integritatea istorica.
**Target:** Archive editing policy.
**Prompt AI:** Adauga o regula pentru actualizari minime si justificate in arhiva.
**Acceptare:** Nu apar rescrieri masive in fisierele istorice.

### L428 Regula pentru denumiri canonice ale batch-urilor
**Descriere tehnica:** Pastreaza o denumire canonica pentru fiecare fisier, inclusiv dupa mutare.
**Scop:** Reduce ambiguitatea in referinte.
**Target:** Naming policy, docs index.
**Prompt AI:** Scrie o regula pentru denumirile canonice ale batch-urilor DeepSeek.
**Acceptare:** Fiecare batch are un nume stabil si unic.

### L429 Scurt changelog de arhivare
**Descriere tehnica:** Creeaza o intrare de changelog care sumarizeaza mutarea batch-urilor istorice.
**Scop:** Pastreaza istoricul decizional in acelasi loc.
**Target:** Changelog, release notes.
**Prompt AI:** Adauga o intrare de changelog pentru arhivarea batch-urilor DeepSeek.
**Acceptare:** Changelog-ul explica mutarea la nivel inalt.

### L430 Nota de inchidere pentru arhiva
**Descriere tehnica:** Rezuma ce contine arhiva DeepSeek si cum se foloseste.
**Scop:** Ofera un capat clar pentru istoricul mutat.
**Target:** `./arhiva/README.md`.
**Prompt AI:** Scrie o nota de inchidere pentru arhiva DeepSeek cu utilizare si scop.
**Acceptare:** Arhiva are un rezumat final si clar.

### L431 Raport de stare pentru seria curenta
**Descriere tehnica:** Produce o fotografie scurta a starii curente dupa arhivarea batch-urilor vechi.
**Scop:** Ajuta la verificare dupa mutare.
**Target:** Status note, docs ops.
**Prompt AI:** Scrie un raport de stare care arata ce ramane activ dupa arhivare.
**Acceptare:** Starea curenta poate fi recitita rapid.

### L432 Notita pentru referinte externe
**Descriere tehnica:** Marcheaza unde trebuie actualizate referintele externe la batch-urile mutate.
**Scop:** Evita linkurile uitate in alte documente.
**Target:** Cross-reference note.
**Prompt AI:** Creeaza o notita care indica ce referinte externe trebuie verificate dupa arhivare.
**Acceptare:** Lista de referinte de verificat este explicita.

### L433 Regula pentru citarea batch-urilor istorice
**Descriere tehnica:** Stabileste cum se citeaza corect un batch arhivat in documente noi.
**Scop:** Pastreaza trasabilitatea istorica.
**Target:** Citation policy, docs guide.
**Prompt AI:** Adauga o regula pentru citarea batch-urilor care au fost mutate in arhiva.
**Acceptare:** Citarea indica mereu locul corect al fisierului.

### L434 Regula pentru documente care depind de batch-uri vechi
**Descriere tehnica:** Marcheaza documentele care raman dependente de istoricul arhivat.
**Scop:** Evita ruperea contextului.
**Target:** Dependency notes, docs map.
**Prompt AI:** Scrie o regula pentru documentele care inca se bazeaza pe batch-uri arhivate.
**Acceptare:** Dependintele istorice sunt vizibile.

### L435 Scurt audit de consistenta a titlurilor
**Descriere tehnica:** Verifica daca titlurile batch-urilor si ale arhivei folosesc acelasi model.
**Scop:** Pastreaza o prezentare consistenta.
**Target:** Title audit, docs lint.
**Prompt AI:** Creeaza un audit care compara titlurile batch-urilor cu cele ale arhivei.
**Acceptare:** Inconsistentele de titlu sunt raportate.

### L436 Scurt audit de consistenta a datelor
**Descriere tehnica:** Verifica daca datele de actualizare coincid cu mutarea documentelor.
**Scop:** Evita cronologii false.
**Target:** Metadata audit.
**Prompt AI:** Adauga un audit read-only pentru datele de actualizare ale batch-urilor si ale arhivei.
**Acceptare:** Datele vechi sunt identificate clar.

### L437 Regula pentru istoricul de revizii
**Descriere tehnica:** Pastreaza un traseu scurt al reviziilor importante pentru batch-urile mutate.
**Scop:** Ajuta la investigatii ulterioare.
**Target:** Revision notes, archive metadata.
**Prompt AI:** Scrie o regula pentru istoricul de revizii al documentelor arhivate.
**Acceptare:** Reviziile majore sunt usor de urmarit.

### L438 Regula pentru note de restaurare
**Descriere tehnica:** Defineste ce trebuie scris cand un document arhivat este restaurat temporar.
**Scop:** Face restaurarea controlata.
**Target:** Restore policy, archive workflow.
**Prompt AI:** Creeaza o regula pentru notele de restaurare din arhiva DeepSeek.
**Acceptare:** Restaurarea lasa o urma clara in documentatie.

### L439 Template pentru raportul de restaurare
**Descriere tehnica:** Standardizeaza raportul scurt folosit dupa restaurarea unui document.
**Scop:** Pastreaza raportarea uniforma.
**Target:** Restore report template.
**Prompt AI:** Scrie un template de raport pentru restaurarea unui fisier din arhiva.
**Acceptare:** Orice restaurare poate fi raportata in acelasi format.

### L440 Notita pentru curatarea referintelor vechi
**Descriere tehnica:** Marcheaza explicit cand o referinta veche a fost inlocuita cu una noua.
**Scop:** Reduce dublura si confuzia.
**Target:** Docs cleanup note.
**Prompt AI:** Adauga o notita care spune cand o referinta veche a fost inlocuita complet.
**Acceptare:** Cititorul vede ce a ramas activ si ce a fost inlocuit.

### L441 Smoke check pentru linkuri in index
**Descriere tehnica:** Ruleaza o verificare simpla asupra linkurilor din indexurile de documente.
**Scop:** Detecteaza linkuri moarte imediat.
**Target:** Docs smoke test, index validation.
**Prompt AI:** Creeaza un smoke check care verifica linkurile din indexurile principale.
**Acceptare:** Linkurile rupte sunt raportate inainte de publicare.

### L442 Smoke check pentru arhiva DeepSeek
**Descriere tehnica:** Verifica daca README-ul arhivei si linkurile sale principale sunt valide.
**Scop:** Protejeaza navigarea istorica.
**Target:** Archive smoke test.
**Prompt AI:** Adauga un smoke check pentru documentatia arhivei DeepSeek.
**Acceptare:** Arhiva poate fi deschisa fara linkuri invalide.

### L443 Smoke check pentru batch-ul activ
**Descriere tehnica:** Verifica daca batch-ul curent este prezent in indexul principal si legat corect din ghid.
**Scop:** Pastreaza seria activa usor de gasit.
**Target:** Active batch smoke test.
**Prompt AI:** Creeaza un smoke check pentru batch-ul DeepSeek activ.
**Acceptare:** Seria activa are o cale clara din index.

### L444 Audit pentru consistenta dintre ghid si fisiere
**Descriere tehnica:** Compara tabelul din ghid cu fisierele existente pe disk.
**Scop:** Evita indexurile care promit fisiere absente.
**Target:** Batch guide audit.
**Prompt AI:** Creeaza un audit care compara ghidul DeepSeek cu fisierele reale.
**Acceptare:** Orice discrepanta intre ghid si fisiere este raportata.

### L445 Audit pentru consistenta dintre arhiva si index
**Descriere tehnica:** Verifica daca tot ce este listat in arhiva chiar exista si este mentionat in index.
**Scop:** Pastreaza arhiva completa.
**Target:** Archive/index consistency audit.
**Prompt AI:** Adauga un audit care compara lista arhivei cu indexul principal.
**Acceptare:** Orice lipsa dintre lista si fisierele reale apare in raport.

### L446 Notita pentru publicare controlata
**Descriere tehnica:** Definește ce trebuie verificat inainte de a publica o mutare de batch in arhiva.
**Scop:** Reduce riscul de publicare gresita.
**Target:** Publish checklist, archive workflow.
**Prompt AI:** Scrie o nota scurta de verificare inainte de publicarea unei arhivari.
**Acceptare:** Publicarea trece doar dupa verificarea esentiala.

### L447 Notita pentru sincronizarea automata a indexului
**Descriere tehnica:** Marcheaza faptul ca indexul se regenereaza automat sau semiautomat dupa mutari.
**Scop:** Evita update-urile manuale uitate.
**Target:** Index sync note.
**Prompt AI:** Adauga o notita despre sincronizarea automata a indexului dupa arhivare.
**Acceptare:** Cititorul intelege cand indexul este actualizat.

### L448 Regula pentru conservarea contextului istoric
**Descriere tehnica:** Pastreaza descrierile istorice chiar daca documentele sunt mutate.
**Scop:** Protejeaza rationalele vechi utile.
**Target:** Archive preservation policy.
**Prompt AI:** Scrie o regula pentru conservarea contextului istoric in arhiva DeepSeek.
**Acceptare:** Arhiva nu pierde explicatiile utile.

### L449 Sumar executiv pentru urmatorul ciclu
**Descriere tehnica:** Pregateste trecerea spre urmatorul interval numeric dupa L450.
**Scop:** Face continuarea previzibila.
**Target:** Continuity note, next batch.
**Prompt AI:** Scrie un sumar scurt care pregateste urmatorul ciclu DeepSeek.
**Acceptare:** Urmatorul interval poate fi pornit fara schimbare de format.

### L450 Nota finala pentru batch-ul 8
**Descriere tehnica:** Rezuma extinderea L401-L450 si relatia cu batch-urile arhivate.
**Scop:** Inchide seria curenta cu un rezumat canonic.
**Target:** Release note, docs index, changelog.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L401-L450.
**Acceptare:** Documentul marcheaza clar ce a fost adaugat si de ce.


