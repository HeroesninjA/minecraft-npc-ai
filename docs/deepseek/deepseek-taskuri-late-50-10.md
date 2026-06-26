# DeepSeek Taskuri - Batch 12 (L601-L650)

Actualizat: 2026-06-25

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Control, audit si continuitate

### L601 Guard pentru intrare unica
**Descriere tehnica:** Confirma ca seria DeepSeek activa are o singura intrare canonica in indexul principal.
**Scop:** Evita ambiguitatea de navigare.
**Target:** `../README.md`, batch guide.
**Prompt AI:** Creeaza un guard care confirma intrarea unica pentru seria activa DeepSeek.
**Acceptare:** Exista o singura cale canonică spre seria activa.

### L602 Audit pentru seria activa
**Descriere tehnica:** Verifica daca batch-urile active nu includ documente istorice mutatе in arhiva.
**Scop:** Pastreaza separarea activa versus istoric.
**Target:** Docs audit, batch registry.
**Prompt AI:** Creeaza un audit read-only care verifica lista batch-urilor active.
**Acceptare:** Orice batch istoric ramas in zona activa este raportat.

### L603 Audit pentru arhiva DeepSeek
**Descriere tehnica:** Compara inventarul arhivei cu fisierele reale pentru a detecta lipsuri sau dubluri.
**Scop:** Pastreaza istoricul complet.
**Target:** `./arhiva/README.md`.
**Prompt AI:** Adauga un audit care valideaza inventarul arhivei DeepSeek.
**Acceptare:** Orice diferenta intre inventar si fisiere este raportata.

### L604 Audit pentru indexuri aliniate
**Descriere tehnica:** Verifica daca indexul principal si indexul arhivei descriu aceeasi stare a documentelor.
**Scop:** Evita divergenta de navigare.
**Target:** `../README.md`, `../index-arhiva.md`.
**Prompt AI:** Creeaza un audit care compara indexurile principale cu arhiva.
**Acceptare:** Diferentele dintre indexuri sunt raportate clar.

### L605 Guard pentru linkuri canonice
**Descriere tehnica:** Detecteaza linkurile care nu folosesc calea standard a seriei.
**Scop:** Reduce linkurile fragile.
**Target:** Docs lint, reference checker.
**Prompt AI:** Adauga un guard care semnaleaza linkurile non-canonice din documentatie.
**Acceptare:** Referintele iesite din standard sunt raportate.

### L606 Guard pentru linkuri moarte
**Descriere tehnica:** Verifica daca vreun link trimite la un fisier inexistent.
**Scop:** Pastreaza documentatia utila.
**Target:** Link validation.
**Prompt AI:** Creeaza un guard care gaseste linkurile moarte din documentele DeepSeek.
**Acceptare:** Linkurile invalide sunt semnalate.

### L607 Guard pentru cai relative corecte
**Descriere tehnica:** Confirma ca toate calele relative din documentele DeepSeek sunt valide.
**Scop:** Evita referintele gresite dupa mutari.
**Target:** Path validation.
**Prompt AI:** Adauga un guard pentru calele relative invalide din documentatie.
**Acceptare:** Calele gresite sunt raportate.

### L608 Guard pentru heading-uri standard
**Descriere tehnica:** Verifica daca fiecare batch mentine aceeasi structura Markdown.
**Scop:** Pastreaza formatul usor de scanat.
**Target:** Markdown lint.
**Prompt AI:** Creeaza un guard care valideaza heading-urile standard ale batch-urilor DeepSeek.
**Acceptare:** Abaterile de la structura sunt raportate.

### L609 Guard pentru note finale prezente
**Descriere tehnica:** Verifica daca fiecare batch are o nota finala explicita.
**Scop:** Clarifica granita batch-ului.
**Target:** Batch structure audit.
**Prompt AI:** Adauga un guard care verifica existenta notei finale in fiecare batch DeepSeek.
**Acceptare:** Lipsa notei finale este semnalata.

### L610 Guard pentru note de arhivare
**Descriere tehnica:** Confirma ca batch-urile mutate mentioneaza motivul arhivarii.
**Scop:** Pastreaza rationalele istorice.
**Target:** Archive README, changelog.
**Prompt AI:** Creeaza un guard care verifica prezenta motivului de arhivare.
**Acceptare:** Motivul arhivarii este vizibil.

### L611 Guard pentru continuitatea numerotarii
**Descriere tehnica:** Verifica secventa numerelor L pentru a nu exista salturi sau dubluri.
**Scop:** Pastreaza seria lizibila.
**Target:** Numbering validator.
**Prompt AI:** Adauga un guard pentru numerotarea continua a taskurilor DeepSeek.
**Acceptare:** Orice ruptura de secventa este raportata.

### L612 Guard pentru depasirea limitei de 50
**Descriere tehnica:** Blocheaza batch-urile care trec de limita declarata.
**Scop:** Pastreaza dimensiunea controlata.
**Target:** Batch validator.
**Prompt AI:** Creeaza un guard care opreste batch-urile cu peste 50 de taskuri.
**Acceptare:** Depasirea limitei este detectata.

### L613 Guard pentru titluri redundante
**Descriere tehnica:** Semnaleaza taskurile care au titluri prea similare in acelasi batch.
**Scop:** Reduce redundanta.
**Target:** Title audit.
**Prompt AI:** Adauga un guard care raporteaza titlurile repetitive.
**Acceptare:** Titlurile prea apropiate sunt marcate.

### L614 Guard pentru scopuri vagi
**Descriere tehnica:** Detecteaza taskurile care nu au un rezultat clar si observabil.
**Scop:** Pastreaza taskurile actionabile.
**Target:** Task quality audit.
**Prompt AI:** Creeaza un guard pentru scopurile care nu sunt suficient de clare.
**Acceptare:** Orice scop vag este raportat.

### L615 Guard pentru target concret
**Descriere tehnica:** Confirma ca fiecare task are o zona tehnica exacta.
**Scop:** Evita taskurile prea largi.
**Target:** Task schema audit.
**Prompt AI:** Adauga un guard care verifica targetul concret al taskurilor.
**Acceptare:** Fiecare task are o tinta clara.

### L616 Guard pentru acceptare clara
**Descriere tehnica:** Verifica daca acceptarea poate fi evaluata fara interpretari.
**Scop:** Reduce ambiguitatea la review.
**Target:** Acceptance audit.
**Prompt AI:** Creeaza un guard pentru criteriile de acceptare neclare.
**Acceptare:** Acceptarile vagi sunt marcate pentru rescriere.

### L617 Guard pentru separarea scop/acceptare
**Descriere tehnica:** Asigura ca scopul si acceptarea raman in sectiuni diferite.
**Scop:** Pastreaza structura standard.
**Target:** Task schema audit.
**Prompt AI:** Adauga un guard care verifica separarea dintre scop si acceptare.
**Acceptare:** Criteriile nu sunt amestecate.

### L618 Guard pentru prompt executabil
**Descriere tehnica:** Verifica daca taskul include instructiuni clare pentru model.
**Scop:** Evita taskurile neactionabile.
**Target:** Prompt schema audit.
**Prompt AI:** Creeaza un guard pentru prompturile care nu pot fi executate clar.
**Acceptare:** Prompturile incomplete sunt raportate.

### L619 Guard pentru dependinte nedeclarate
**Descriere tehnica:** Avertizeaza cand un task presupune mai multe modificari fara sa le declare.
**Scop:** Face impactul transparent.
**Target:** Dependency audit.
**Prompt AI:** Adauga un guard pentru taskurile cu dependinte ascunse.
**Acceptare:** Dependintele multiple neexplicate sunt raportate.

### L620 Guard pentru batch fara context
**Descriere tehnica:** Semnaleaza batch-urile care nu spun cum se leaga de seria activa si de arhiva.
**Scop:** Evita contextul pierdut.
**Target:** Context audit.
**Prompt AI:** Creeaza un guard pentru batch-urile fara context de serie.
**Acceptare:** Lipsa contextului este raportata.

### L621 Guard pentru documente active izolate
**Descriere tehnica:** Detecteaza documentele active care fac trimitere la istoricul arhivat fara motiv.
**Scop:** Pastreaza separarea clara.
**Target:** Docs organization audit.
**Prompt AI:** Adauga un guard pentru documentele active care amesteca istoricul.
**Acceptare:** Amestecul activ-istoric este raportat.

### L622 Guard pentru arhiva sincronizata
**Descriere tehnica:** Verifica daca inventarul arhivei si indexul principal sunt aliniate.
**Scop:** Evita divergenta dintre navigare si istoric.
**Target:** `./arhiva/README.md`, `../README.md`.
**Prompt AI:** Creeaza un guard care compara arhiva cu indexul principal.
**Acceptare:** Diferentele sunt raportate clar.

### L623 Guard pentru batch activ unic
**Descriere tehnica:** Confirma ca exista o singura serie activa in index.
**Scop:** Evita confuziile de stare.
**Target:** Docs index audit.
**Prompt AI:** Adauga un guard care confirma un singur batch activ DeepSeek.
**Acceptare:** Nicio alta serie activa nu apare in index.

### L624 Guard pentru index principal actualizat
**Descriere tehnica:** Verifica daca ultimul batch activ apare in indexul general.
**Scop:** Pastreaza accesul rapid.
**Target:** `../README.md`.
**Prompt AI:** Creeaza un guard pentru actualizarea indexului principal dupa fiecare batch.
**Acceptare:** Noul batch apare in index.

### L625 Guard pentru index arhiva actualizat
**Descriere tehnica:** Verifica daca arhiva DeepSeek este listata in indexul arhivei.
**Scop:** Pastreaza istoricul accesibil.
**Target:** `../index-arhiva.md`.
**Prompt AI:** Adauga un guard pentru indexul arhivei dupa mutarea batch-urilor.
**Acceptare:** Arhiva DeepSeek ramane listata.

### L626 Audit pentru motive de mutare
**Descriere tehnica:** Confirma ca fiecare fisier arhivat are un motiv explicit.
**Scop:** Pastreaza rationalele.
**Target:** Archive README, changelog.
**Prompt AI:** Creeaza un audit care verifica motivul arhivarii pentru fiecare fisier.
**Acceptare:** Fara motiv explicit, intrarea este raportata.

### L627 Audit pentru traseul de mutare
**Descriere tehnica:** Verifica daca se poate urmari drumul din index pana la fisierul arhivat.
**Scop:** Face auditul simplu.
**Target:** Traceability audit.
**Prompt AI:** Adauga un audit care verifica trasabilitatea documentelor DeepSeek.
**Acceptare:** Orice ruptura de traseu este raportata.

### L628 Audit pentru nume canonice
**Descriere tehnica:** Confirma ca numele batch-urilor raman stabile intre active si arhiva.
**Scop:** Reduce ambiguitatea.
**Target:** Naming audit.
**Prompt AI:** Creeaza un audit care verifica numele canonice ale batch-urilor DeepSeek.
**Acceptare:** Numele inconsistene sunt raportate.

### L629 Audit pentru rapoarte scurte
**Descriere tehnica:** Verifica daca rapoartele de mutare si restaurare sunt concise si complete.
**Scop:** Pastreaza mentenanta usoara.
**Target:** Change report audit.
**Prompt AI:** Adauga un audit pentru rapoartele scurte de schimbare.
**Acceptare:** Rapoartele lipsa sau vagi sunt raportate.

### L630 Audit pentru checklist-uri prezente
**Descriere tehnica:** Confirma existenta checklist-urilor pentru arhivare, restaurare si publicare.
**Scop:** Pastreaza procesul repetabil.
**Target:** Process docs audit.
**Prompt AI:** Creeaza un audit care verifica checklist-urile procesului DeepSeek.
**Acceptare:** Lipsa unui checklist este raportata.

### L631 Regula pentru arhivare controlata
**Descriere tehnica:** Defineste ca mutarea in arhiva cere index actualizat si motiv clar.
**Scop:** Pastreaza procesul auditabil.
**Target:** Archive policy.
**Prompt AI:** Scrie o regula scurta pentru arhivarea controlata a batch-urilor DeepSeek.
**Acceptare:** Arhivarea are pasi clari.

### L632 Regula pentru restaurare controlata
**Descriere tehnica:** Defineste pasii minimi pentru readucerea temporara a unui document.
**Scop:** Evita restaurarile accidentale.
**Target:** Restore policy.
**Prompt AI:** Creeaza o regula pentru restaurarea controlata din arhiva.
**Acceptare:** Restaurarea este auditabila.

### L633 Regula pentru indexare dupa mutare
**Descriere tehnica:** Cere actualizarea indexurilor dupa orice mutare de batch.
**Scop:** Pastreaza navigarea corecta.
**Target:** Index maintenance policy.
**Prompt AI:** Adauga o regula pentru actualizarea indexului dupa mutarea batch-urilor.
**Acceptare:** Indexurile reflecta mutarea.

### L634 Regula pentru citare istorica
**Descriere tehnica:** Explica cum se citeaza corect un batch arhivat.
**Scop:** Pastreaza trasabilitatea.
**Target:** Citation policy.
**Prompt AI:** Scrie o regula pentru citarea batch-urilor arhivate in documente noi.
**Acceptare:** Calea citata este corecta.

### L635 Regula pentru nume de fisiere stabile
**Descriere tehnica:** Pastreaza numele fisierelor identice intre mutare si istoric.
**Scop:** Usureaza auditul.
**Target:** Naming policy.
**Prompt AI:** Creeaza o regula pentru stabilitatea numelor de fisiere DeepSeek.
**Acceptare:** Numele nu se schimba fara motiv.

### L636 Regula pentru foldere stabile
**Descriere tehnica:** Evita redenumirea inutila a folderelor active si arhivate.
**Scop:** Reduce linkurile fragile.
**Target:** Folder policy.
**Prompt AI:** Adauga o regula pentru stabilitatea folderelor DeepSeek.
**Acceptare:** Structura folderelor ramane previzibila.

### L637 Regula pentru mentinerea arhivei read-only
**Descriere tehnica:** Stabileste ca fisierele istorice nu se rescriu fara motiv.
**Scop:** Protejeaza istoricul.
**Target:** Archive policy.
**Prompt AI:** Scrie o regula read-only pentru arhiva DeepSeek.
**Acceptare:** Schimbari majore in arhiva sunt evitate.

### L638 Regula pentru raportul de schimbare
**Descriere tehnica:** Cere ca fiecare mutare sa aiba un rezumat scurt si specific.
**Scop:** Face schimbarile usor de urmarit.
**Target:** Change report policy.
**Prompt AI:** Adauga o regula pentru rapoartele de schimbare la mutarea batch-urilor.
**Acceptare:** Rezumatul este obligatoriu.

### L639 Regula pentru handoff
**Descriere tehnica:** Spune ce trebuie mentionat cand contextul este predat unui alt mentainer.
**Scop:** Pastreaza continuitatea echipei.
**Target:** Handoff note.
**Prompt AI:** Creeaza o regula pentru handoff-ul seriei DeepSeek.
**Acceptare:** Noua persoana intelege rapid starea.

### L640 Regula pentru note finale
**Descriere tehnica:** Stabileste ca fiecare batch trebuie sa se inchida cu o nota clara.
**Scop:** Pastreaza granita batch-ului vizibila.
**Target:** Batch completion policy.
**Prompt AI:** Scrie o regula scurta despre notele finale ale batch-urilor.
**Acceptare:** Fiecare batch se inchide explicit.

### L641 Regula pentru continuitate
**Descriere tehnica:** Marcheaza cum se indica urmatorul interval numeric dupa un batch inchis.
**Scop:** Face extensia predictibila.
**Target:** Continuity policy.
**Prompt AI:** Adauga o regula despre cum se pregateste urmatorul batch DeepSeek.
**Acceptare:** Urmatorul interval este usor de pornit.

### L642 Notita pentru consum AI
**Descriere tehnica:** Explica modul corect de citire a seriei active si a arhivei de catre un model.
**Scop:** Reduce interpretarile gresite.
**Target:** AI usage note.
**Prompt AI:** Scrie o nota scurta despre cum ar trebui consumata seria DeepSeek de catre AI.
**Acceptare:** Modelul poate separa activul de istoric.

### L643 Notita pentru review uman
**Descriere tehnica:** Marcheaza cazurile in care un om trebuie sa confirme mutarea sau restaurarea.
**Scop:** Pastreaza controlul uman.
**Target:** Human review note.
**Prompt AI:** Adauga o nota despre punctele care cer review uman.
**Acceptare:** Cazurile sensibile sunt evidente.

### L644 Notita pentru publicare controlata
**Descriere tehnica:** Specifica verificarile finale inainte de publicarea unui batch sau a unei mutari.
**Scop:** Reduce greselile de livrare.
**Target:** Publish note.
**Prompt AI:** Scrie o notita scurta pentru publicarea controlata a batch-urilor DeepSeek.
**Acceptare:** Publicarea trece prin verificari esentiale.

### L645 Notita pentru curatarea referintelor
**Descriere tehnica:** Spune cum trebuie curatate linkurile vechi dupa o mutare.
**Scop:** Evita referintele moarte.
**Target:** Cleanup note.
**Prompt AI:** Creeaza o notita pentru curatarea referintelor dupa arhivare.
**Acceptare:** Referintele vechi sunt clar identificate.

### L646 Notita pentru sincronizarea indexului
**Descriere tehnica:** Marcheaza faptul ca indexurile trebuie sincronizate cu noile fisiere.
**Scop:** Pastreaza navigarea corecta.
**Target:** Index sync note.
**Prompt AI:** Adauga o notita despre sincronizarea indexului dupa mutarea batch-urilor.
**Acceptare:** Indexurile reflecta structura curenta.

### L647 Notita pentru trasabilitate
**Descriere tehnica:** Explica ce urmeaza o schimbare pentru a fi usor de urmarit ulterior.
**Scop:** Face auditul simplu.
**Target:** Traceability note.
**Prompt AI:** Scrie o notita despre trasabilitatea batch-urilor DeepSeek.
**Acceptare:** Schimbarea este usor de urmarit din index pana la fisier.

### L648 Watchlist pentru regresii
**Descriere tehnica:** Aduna riscurile cele mai frecvente dupa mutari sau extinderi.
**Scop:** Ajuta la verificari rapide.
**Target:** Regression watchlist.
**Prompt AI:** Creeaza o watchlist scurta pentru regresiile de documentatie DeepSeek.
**Acceptare:** Problemele tipice sunt enumerate.

### L649 Pregatire pentru urmatorul ciclu
**Descriere tehnica:** Rezuma ce trebuie retinut pentru a continua cu intervalul urmator.
**Scop:** Pastreaza seria extensibila.
**Target:** Next batch note.
**Prompt AI:** Scrie o nota scurta care pregateste urmatorul ciclu DeepSeek.
**Acceptare:** Trecerea la intervalul urmator este evidenta.

### L650 Nota finala pentru batch-ul 12
**Descriere tehnica:** Rezuma extinderea L601-L650 si relatia cu seria activa si arhiva.
**Scop:** Inchide batch-ul cu un rezumat canonic.
**Target:** Release note, docs index, changelog.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L601-L650.
**Acceptare:** Documentul marcheaza clar ce a fost adaugat si de ce.


