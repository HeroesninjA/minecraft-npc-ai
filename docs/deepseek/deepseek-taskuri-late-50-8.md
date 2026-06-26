# DeepSeek Taskuri - Batch 10 (L501-L550)

Actualizat: 2026-06-25

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Curatare, audit si stabilizare

### L501 Guard pentru batch activ unic
**Descriere tehnica:** Verifica daca exista un singur batch DeepSeek activ in indexul principal.
**Scop:** Evita concurenta intre mai multe serii curente.
**Target:** Index docs, batch guide.
**Prompt AI:** Creeaza un guard care confirma ca exista un singur batch activ DeepSeek.
**Acceptare:** Nicio alta serie activa nu apare in index.

### L502 Audit pentru batch-uri istorice
**Descriere tehnica:** Compara lista documentelor arhivate cu inventarul asteptat.
**Scop:** Pastreaza arhiva completa si curata.
**Target:** `./arhiva/README.md`.
**Prompt AI:** Creeaza un audit read-only care verifica batch-urile istorice listate in arhiva.
**Acceptare:** Orice batch lipsa sau in plus este raportat.

### L503 Guard pentru linkuri canonice
**Descriere tehnica:** Verifica daca indexurile folosesc numai cai canonice catre batch-urile active si arhivate.
**Scop:** Reduce linkurile fragile.
**Target:** `../README.md`, `../index-arhiva.md`.
**Prompt AI:** Adauga un guard care semnaleaza linkurile non-canonice din indexuri.
**Acceptare:** Referintele iesite din standard sunt raportate.

### L504 Template pentru task nou
**Descriere tehnica:** Definește un sablon exact pentru un task DeepSeek nou, cu toate campurile cerute.
**Scop:** Pastreaza consistenta structurii.
**Target:** Docs template, authoring guide.
**Prompt AI:** Creeaza un template reutilizabil pentru taskurile noi din seria DeepSeek.
**Acceptare:** Un task nou poate fi scris fara improvizatie.

### L505 Template pentru batch nou
**Descriere tehnica:** Standardizeaza blocul complet pentru un fisier batch nou.
**Scop:** Reduce variatia dintre fisierele de serie.
**Target:** Batch template, docs guide.
**Prompt AI:** Creeaza un template pentru pornirea unui batch nou DeepSeek.
**Acceptare:** Structura minima apare identic de la un batch la altul.

### L506 Audit pentru structura batch-ului
**Descriere tehnica:** Verifica daca un batch are header, reguli, categorii si taskuri numerotate.
**Scop:** Pastreaza formatul canonic.
**Target:** Markdown lint, batch validator.
**Prompt AI:** Adauga un audit care verifica structura standard a unui batch DeepSeek.
**Acceptare:** Lipsa unei sectiuni este raportata.

### L507 Audit pentru acceptare clara
**Descriere tehnica:** Detecteaza acceptarile care nu pot fi verificate usor.
**Scop:** Reduce taskurile vagi.
**Target:** Task quality audit.
**Prompt AI:** Creeaza un audit care semnaleaza criteriile de acceptare neclare.
**Acceptare:** Acceptarile ambigue sunt marcate pentru rescriere.

### L508 Audit pentru scop verificabil
**Descriere tehnica:** Verifica daca scopul taskului are un rezultat concret si observabil.
**Scop:** Evita obiectivele difuze.
**Target:** Task schema audit.
**Prompt AI:** Adauga un audit pentru scopurile care nu sunt verificabile.
**Acceptare:** Orice scop vag este raportat.

### L509 Audit pentru target concret
**Descriere tehnica:** Confirma daca fiecare task are o zona tehnica precisa.
**Scop:** Evita taskurile prea largi.
**Target:** Task schema audit.
**Prompt AI:** Creeaza un audit care verifica targeturile concrete pentru taskuri.
**Acceptare:** Fiecare task are o tinta clara.

### L510 Audit pentru prompt executabil
**Descriere tehnica:** Verifica daca un task include instructiuni suficient de precise pentru model.
**Scop:** Evita taskurile neactionabile.
**Target:** Prompt schema audit.
**Prompt AI:** Adauga un audit pentru prompturile care nu pot fi executate clar.
**Acceptare:** Prompturile incomplete sunt raportate.

### L511 Audit pentru separare intre scop si acceptare
**Descriere tehnica:** Verifica faptul ca scopul si acceptarea sunt scrise in sectiuni diferite.
**Scop:** Pastreaza claritatea taskului.
**Target:** Task schema audit.
**Prompt AI:** Creeaza un audit care verifica separarea dintre scop si acceptare.
**Acceptare:** Criteriile nu sunt amestecate.

### L512 Guard pentru numerotare continua
**Descriere tehnica:** Verifica secventa numerelor L din batch-ul activ.
**Scop:** Previne gaps si dubluri.
**Target:** Numbering validator.
**Prompt AI:** Adauga un guard pentru numerotarea continua a taskurilor.
**Acceptare:** Orice ruptura de ordine este raportata.

### L513 Guard pentru numere duplicate
**Descriere tehnica:** Detecteaza doua taskuri care revendica acelasi numar.
**Scop:** Pastreaza unicitatea taskurilor.
**Target:** Numbering validator.
**Prompt AI:** Creeaza un guard care raporteaza numerele duplicate intr-un batch.
**Acceptare:** Dublurile apar explicit in raport.

### L514 Guard pentru titluri repetitive
**Descriere tehnica:** Semnaleaza taskurile care au titluri prea apropiate.
**Scop:** Reduce redundanta.
**Target:** Title audit.
**Prompt AI:** Adauga un guard pentru titlurile repetitive din seria DeepSeek.
**Acceptare:** Titlurile similare sunt raportate.

### L515 Guard pentru depasirea limitei
**Descriere tehnica:** Blocheaza batch-urile care depasesc 50 de taskuri.
**Scop:** Pastreaza dimensiunea controlata.
**Target:** Batch validator.
**Prompt AI:** Creeaza un guard care opreste batch-urile prea mari.
**Acceptare:** Depasirea limitei este detectata.

### L516 Guard pentru continuitatea seriilor
**Descriere tehnica:** Verifica daca noul batch continua logic ultimul interval publicat.
**Scop:** Pastreaza ordinea documentatiei.
**Target:** Batch guide, docs index.
**Prompt AI:** Adauga un guard pentru continuitatea dintre batch-uri.
**Acceptare:** Continuitatea este confirmata sau raportata.

### L517 Guard pentru arhiva sincronizata
**Descriere tehnica:** Verifica daca arhiva DeepSeek si indexul principal spun acelasi lucru.
**Scop:** Previne divergenta intre navigare si istoric.
**Target:** `./arhiva/README.md`, `../index-arhiva.md`.
**Prompt AI:** Creeaza un guard care compara arhiva cu indexul principal.
**Acceptare:** Diferentele sunt raportate clar.

### L518 Guard pentru linkuri moarte
**Descriere tehnica:** Semnaleaza linkurile care trimit la fisiere inexistente.
**Scop:** Păstreaza documentatia utila.
**Target:** Docs lint, link checker.
**Prompt AI:** Adauga un guard care gaseste linkurile moarte din documente.
**Acceptare:** Orice link invalid este raportat.

### L519 Guard pentru cai relative invalide
**Descriere tehnica:** Verifica daca folderele si fisierele folosesc cai relative corecte.
**Scop:** Evita referintele fragile.
**Target:** Link validation.
**Prompt AI:** Creeaza un guard pentru cailor relative gresite in documentatie.
**Acceptare:** Calele invalide apar in raport.

### L520 Guard pentru heading-uri standard
**Descriere tehnica:** Verifica daca toate batch-urile folosesc aceeasi structura de heading-uri.
**Scop:** Pastreaza consistenta.
**Target:** Markdown lint.
**Prompt AI:** Adauga un guard care valideaza heading-urile standard ale batch-urilor.
**Acceptare:** Abaterile de la structura sunt raportate.

### L521 Guard pentru note finale prezente
**Descriere tehnica:** Confirma ca fiecare batch are o nota finala pentru inchidere.
**Scop:** Pastreaza seria bine delimitata.
**Target:** Batch structure audit.
**Prompt AI:** Creeaza un guard care verifica existenta notei finale in fiecare batch.
**Acceptare:** Lipsa notei finale este semnalata.

### L522 Guard pentru note de arhivare prezente
**Descriere tehnica:** Verifica daca batch-urile mutate mentioneaza clar motivul arhivarii.
**Scop:** Pastreaza rationalele vizibile.
**Target:** Archive README, changelog.
**Prompt AI:** Adauga un guard pentru motivele de arhivare in documentele istorice.
**Acceptare:** Motivul arhivarii este vizibil.

### L523 Guard pentru index actualizat
**Descriere tehnica:** Verifica daca indexul principal include ultimul batch activ.
**Scop:** Evita ascunderea seriei curente.
**Target:** `../README.md`.
**Prompt AI:** Creeaza un guard pentru actualizarea indexului principal dupa fiecare batch nou.
**Acceptare:** Noul batch apare in index.

### L524 Guard pentru index arhiva actualizat
**Descriere tehnica:** Verifica daca indexul arhivei expune corect folderul DeepSeek.
**Scop:** Pastreaza istoria accesibila.
**Target:** `../index-arhiva.md`.
**Prompt AI:** Adauga un guard pentru indexul arhivei dupa mutari.
**Acceptare:** Arhiva DeepSeek ramane listata.

### L525 Guard pentru documentatie activa separata
**Descriere tehnica:** Verifica daca documentele active nu amesteca istoricul cu seria curenta.
**Scop:** Pastreaza separarea clara intre activ si istoric.
**Target:** Docs organization audit.
**Prompt AI:** Creeaza un guard pentru separarea documentelor active de cele arhivate.
**Acceptare:** Mixturile active-istoric sunt raportate.

### L526 Guard pentru descrieri scurte
**Descriere tehnica:** Avertizeaza cand o descriere tehnica devine prea lunga sau prea vaga.
**Scop:** Pastreaza lizibilitatea taskurilor.
**Target:** Style audit.
**Prompt AI:** Adauga un guard pentru descrierile tehnice prea lungi sau prea vagi.
**Acceptare:** Abaterile de stil sunt semnalate.

### L527 Guard pentru target duplicat in batch
**Descriere tehnica:** Detecteaza mai multe taskuri care tintesc exact acelasi subiect.
**Scop:** Reduce redundanta.
**Target:** Redundancy checker.
**Prompt AI:** Creeaza un guard care semnaleaza targeturile duplicate din acelasi batch.
**Acceptare:** Redundanta evidenta este raportata.

### L528 Guard pentru dependinte explicite
**Descriere tehnica:** Verifica daca taskul mentioneaza dependintele atunci cand atinge mai multe docuri.
**Scop:** Face impactul transparent.
**Target:** Dependency audit.
**Prompt AI:** Adauga un guard pentru taskurile care au dependinte multiple nedeclarate.
**Acceptare:** Dependintele ascunse sunt raportate.

### L529 Guard pentru batch-uri fara scop istoric
**Descriere tehnica:** Avertizeaza cand un batch nu spune clar ce rezolva fata de cel anterior.
**Scop:** Pastreaza continuitatea narativa.
**Target:** Batch continuity audit.
**Prompt AI:** Creeaza un guard pentru batch-urile care nu explica diferenta fata de cele vechi.
**Acceptare:** Continuitatea lipsa este raportata.

### L530 Guard pentru batch-uri fara context
**Descriere tehnica:** Semnaleaza batch-urile care nu ofera context despre seria activa si arhiva.
**Scop:** Reduce ambiguitatea.
**Target:** Context audit.
**Prompt AI:** Adauga un guard pentru batch-urile fara context de serie sau arhiva.
**Acceptare:** Contextul lipsa este raportat.

### L531 Regula pentru arhivare controlata
**Descriere tehnica:** Defineste ca mutarea in arhiva necesita un motiv si un index actualizat.
**Scop:** Pastreaza procesul auditabil.
**Target:** Archive policy.
**Prompt AI:** Scrie o regula scurta pentru arhivarea controlata a batch-urilor DeepSeek.
**Acceptare:** Arhivarea are pasi clari.

### L532 Regula pentru restaurare controlata
**Descriere tehnica:** Defineste pasii minimi pentru aducerea temporara a unui document arhivat inapoi.
**Scop:** Evita restaurarile accidentale.
**Target:** Restore policy.
**Prompt AI:** Creeaza o regula pentru restaurarea controlata din arhiva.
**Acceptare:** Restaurarea este auditabila.

### L533 Regula pentru indexare dupa mutare
**Descriere tehnica:** Cere actualizarea indexurilor dupa orice mutare a unui batch.
**Scop:** Păstreaza navigarea corecta.
**Target:** Index maintenance policy.
**Prompt AI:** Adauga o regula pentru actualizarea indexului dupa mutarea batch-urilor.
**Acceptare:** Indexurile reflecta mutarea.

### L534 Regula pentru citare istorica
**Descriere tehnica:** Spune cum se citeaza un batch mutat in arhiva in documente noi.
**Scop:** Pastreaza trasabilitatea.
**Target:** Citation policy.
**Prompt AI:** Scrie o regula pentru citarea corecta a batch-urilor arhivate.
**Acceptare:** Calea citata este corecta.

### L535 Regula pentru nume de fisiere stabile
**Descriere tehnica:** Pastreaza numele fisierelor identice intre mutare si istoric.
**Scop:** Usureaza auditul.
**Target:** Naming policy.
**Prompt AI:** Creeaza o regula pentru stabilitatea numelor de fisiere DeepSeek.
**Acceptare:** Numele nu se schimba fara motiv.

### L536 Regula pentru foldere stabile
**Descriere tehnica:** Evita redenumirea inutila a folderelor active si arhivate.
**Scop:** Reduce linkurile fragile.
**Target:** Folder policy.
**Prompt AI:** Adauga o regula pentru stabilitatea folderelor DeepSeek.
**Acceptare:** Structura folderelor ramane previzibila.

### L537 Regula pentru mentinerea arhivei read-only
**Descriere tehnica:** Stabileste ca fisierele istorice nu se rescriu fara motiv.
**Scop:** Protejeaza istoricul.
**Target:** Archive policy.
**Prompt AI:** Scrie o regula read-only pentru arhiva DeepSeek.
**Acceptare:** Schimbari majore in arhiva sunt evitate.

### L538 Regula pentru raportul de schimbare
**Descriere tehnica:** Cere ca fiecare mutare sa aiba un rezumat scurt si specific.
**Scop:** Face schimbarile usor de urmarit.
**Target:** Change report policy.
**Prompt AI:** Adauga o regula pentru rapoartele de schimbare la mutarea batch-urilor.
**Acceptare:** Rezumatul este obligatoriu.

### L539 Regula pentru handoff
**Descriere tehnica:** Spune ce trebuie mentionat cand contextul este predat unui alt mentainer.
**Scop:** Pastreaza continuitatea echipei.
**Target:** Handoff note.
**Prompt AI:** Creeaza o regula pentru handoff-ul seriei DeepSeek.
**Acceptare:** Noua persoana intelege rapid starea.

### L540 Regula pentru note finale
**Descriere tehnica:** Stabileste ca fiecare batch trebuie sa se inchida cu o nota clara.
**Scop:** Pastreaza granita batch-ului vizibila.
**Target:** Batch completion policy.
**Prompt AI:** Scrie o regula scurta despre notele finale ale batch-urilor.
**Acceptare:** Fiecare batch se inchide explicit.

### L541 Regula pentru continuitate
**Descriere tehnica:** Marcheaza cum se indica urmatorul interval numeric dupa un batch inchis.
**Scop:** Face extensia predictibila.
**Target:** Continuity policy.
**Prompt AI:** Adauga o regula despre cum se pregateste urmatorul batch DeepSeek.
**Acceptare:** Urmatorul interval este usor de pornit.

### L542 Notita pentru consum AI
**Descriere tehnica:** Explica modul corect de citire a seriei active si a arhivei de catre un model.
**Scop:** Reduce interpretarile gresite.
**Target:** AI usage note.
**Prompt AI:** Scrie o nota scurta despre cum ar trebui consumata seria DeepSeek de catre AI.
**Acceptare:** Modelul poate separa activul de istoric.

### L543 Notita pentru review uman
**Descriere tehnica:** Marcheaza cazurile in care un om trebuie sa confirme mutarea sau restaurarea.
**Scop:** Pastreaza controlul uman.
**Target:** Human review note.
**Prompt AI:** Adauga o nota despre punctele care cer review uman.
**Acceptare:** Cazurile sensibile sunt evidente.

### L544 Notita pentru publicare controlata
**Descriere tehnica:** Specifica verificarile finale inainte de publicarea unui batch sau a unei mutari.
**Scop:** Reduce greselile de livrare.
**Target:** Publish note.
**Prompt AI:** Scrie o notita scurta pentru publicarea controlata a batch-urilor DeepSeek.
**Acceptare:** Publicarea trece prin verificari esentiale.

### L545 Notita pentru curatarea referintelor
**Descriere tehnica:** Spune cum trebuie curatate linkurile vechi dupa o mutare.
**Scop:** Evita referintele moarte.
**Target:** Cleanup note.
**Prompt AI:** Creeaza o notita pentru curatarea referintelor dupa arhivare.
**Acceptare:** Referintele vechi sunt clar identificate.

### L546 Notita pentru sincronizarea indexului
**Descriere tehnica:** Marcheaza faptul ca indexurile trebuie sincronizate cu noile fisiere.
**Scop:** Pastreaza navigarea corecta.
**Target:** Index sync note.
**Prompt AI:** Adauga o notita despre sincronizarea indexului dupa mutarea batch-urilor.
**Acceptare:** Indexurile reflecta structura curenta.

### L547 Notita pentru trasabilitate
**Descriere tehnica:** Explica ce urmeaza o schimbare pentru a fi usor de urmarit ulterior.
**Scop:** Face auditul simplu.
**Target:** Traceability note.
**Prompt AI:** Scrie o notita despre trasabilitatea batch-urilor DeepSeek.
**Acceptare:** Schimbarea este usor de urmarit din index pana la fisier.

### L548 Watchlist pentru regresii
**Descriere tehnica:** Aduna riscurile cele mai frecvente dupa mutari sau extinderi.
**Scop:** Ajuta la verificari rapide.
**Target:** Regression watchlist.
**Prompt AI:** Creeaza o watchlist scurta pentru regresiile de documentatie DeepSeek.
**Acceptare:** Problemele tipice sunt enumerate.

### L549 Pregatire pentru urmatorul ciclu
**Descriere tehnica:** Rezuma ce trebuie retinut pentru a continua cu intervalul urmator.
**Scop:** Pastreaza seria extensibila.
**Target:** Next batch note.
**Prompt AI:** Scrie o nota scurta care pregateste urmatorul ciclu DeepSeek.
**Acceptare:** Trecerea la intervalul urmator este evidenta.

### L550 Nota finala pentru batch-ul 10
**Descriere tehnica:** Rezuma extinderea L501-L550 si relatia cu seria activa si arhiva.
**Scop:** Inchide batch-ul cu un rezumat canonic.
**Target:** Release note, docs index, changelog.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L501-L550.
**Acceptare:** Documentul marcheaza clar ce a fost adaugat si de ce.


