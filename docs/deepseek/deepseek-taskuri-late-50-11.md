# DeepSeek Taskuri - Batch 13 (L651-L700)

Actualizat: 2026-06-25

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Tooling, rapoarte si executie controlata

### L651 Registry JSON pentru batch-uri DeepSeek
**Descriere tehnica:** Introduce un registru JSON generabil din ghidul DeepSeek, cu batch, interval, fisier si stare.
**Scop:** Face batch-urile usor de verificat automat.
**Target:** Docs tooling, batch registry.
**Prompt AI:** Creeaza un registru JSON pentru batch-urile DeepSeek pe baza indexului existent.
**Acceptare:** Registrul contine batch, interval, fisier, stare si tema.

### L652 Validator pentru registrul JSON
**Descriere tehnica:** Verifica daca registrul JSON respecta schema minima si nu are campuri lipsa.
**Scop:** Previne indexuri automate invalide.
**Target:** Docs validator, JSON schema.
**Prompt AI:** Creeaza un validator pentru registrul JSON al batch-urilor DeepSeek.
**Acceptare:** Campurile lipsa sunt raportate explicit.

### L653 Generator pentru tabelul din ghid
**Descriere tehnica:** Genereaza tabelul de batch-uri din registrul JSON, nu manual.
**Scop:** Reduce erorile de sincronizare.
**Target:** Docs generator, `deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un generator care produce tabelul de batch-uri din registrul JSON.
**Acceptare:** Tabelul generat coincide cu registrul.

### L654 Smoke check pentru tabelul generat
**Descriere tehnica:** Compara tabelul generat cu tabelul curent din ghid.
**Scop:** Detecteaza drift intre sursa si document.
**Target:** Docs smoke test.
**Prompt AI:** Adauga un smoke check care compara tabelul generat cu ghidul DeepSeek.
**Acceptare:** Drift-ul este raportat clar.

### L655 Export CSV pentru batch-uri
**Descriere tehnica:** Produce un CSV cu batch, interval, fisier, tema si stare.
**Scop:** Permite analiza rapida in spreadsheet.
**Target:** Docs export, reporting.
**Prompt AI:** Creeaza un export CSV pentru registrul batch-urilor DeepSeek.
**Acceptare:** CSV-ul include toate batch-urile din registru.

### L656 Export Markdown pentru status
**Descriere tehnica:** Produce un rezumat Markdown scurt despre batch-urile active si arhivate.
**Scop:** Simplifica rapoartele de mentenanta.
**Target:** Status report, docs tooling.
**Prompt AI:** Creeaza un export Markdown pentru starea seriei DeepSeek.
**Acceptare:** Raportul separa activul de arhiva.

### L657 Audit pentru batch-uri fara fisier
**Descriere tehnica:** Detecteaza intrarile din registru care trimit la fisiere inexistente.
**Scop:** Previne indexurile false.
**Target:** Registry audit.
**Prompt AI:** Adauga un audit care verifica existenta fisierelor listate in registru.
**Acceptare:** Fisierele lipsa sunt raportate cu calea asteptata.

### L658 Audit pentru fisiere fara registru
**Descriere tehnica:** Detecteaza fisierele DeepSeek existente care nu apar in registru.
**Scop:** Previne documente orfane.
**Target:** Registry audit.
**Prompt AI:** Creeaza un audit care raporteaza fisierele DeepSeek nelistate in registru.
**Acceptare:** Orice fisier orfan este raportat.

### L659 Audit pentru intervale suprapuse
**Descriere tehnica:** Verifica daca doua batch-uri revendica acelasi numar L.
**Scop:** Pastreaza numerotarea coerenta.
**Target:** Registry validator.
**Prompt AI:** Adauga un audit pentru suprapuneri de intervale in registrul DeepSeek.
**Acceptare:** Suprapunerile sunt listate cu ambele batch-uri implicate.

### L660 Audit pentru gap-uri de interval
**Descriere tehnica:** Verifica daca exista intervale lipsa intre batch-urile publicate.
**Scop:** Face seria usor de urmarit.
**Target:** Registry validator.
**Prompt AI:** Creeaza un audit care raporteaza gap-urile numerice dintre batch-uri.
**Acceptare:** Orice gap este raportat cu limita inferioara si superioara.

### L661 Guard pentru stare activa unica
**Descriere tehnica:** Verifica daca registrul marcheaza corect batch-urile active fata de arhivate.
**Scop:** Evita mai multe surse de lucru curent.
**Target:** Registry status audit.
**Prompt AI:** Adauga un guard pentru starea activa unica in registrul DeepSeek.
**Acceptare:** Starea multipla activa este raportata.

### L662 Guard pentru stare arhivata
**Descriere tehnica:** Confirma ca batch-urile din `./arhiva/` sunt marcate ca arhivate.
**Scop:** Pastreaza starea corecta dupa mutare.
**Target:** Archive status audit.
**Prompt AI:** Creeaza un guard care verifica starea batch-urilor din arhiva.
**Acceptare:** Fisierele din arhiva fara stare arhivata sunt raportate.

### L663 Guard pentru tema lipsa
**Descriere tehnica:** Verifica daca fiecare batch are o tema scurta si utila.
**Scop:** Face indexul scanabil.
**Target:** Registry quality audit.
**Prompt AI:** Adauga un guard pentru batch-urile fara tema declarata.
**Acceptare:** Tema lipsa este raportata.

### L664 Guard pentru tema prea vaga
**Descriere tehnica:** Detecteaza teme generice care nu ajuta la navigare.
**Scop:** Creste calitatea indexului.
**Target:** Registry quality audit.
**Prompt AI:** Creeaza un guard care semnaleaza temele prea vagi din registru.
**Acceptare:** Temele generice sunt marcate pentru rescriere.

### L665 Guard pentru nume de fisier necanonic
**Descriere tehnica:** Verifica daca fisierele active respecta conventia `deepseek-taskuri-late-50-N.md`.
**Scop:** Reduce variatia numelor.
**Target:** Naming audit.
**Prompt AI:** Adauga un guard pentru numele necanonice ale batch-urilor active.
**Acceptare:** Numele iesite din conventie sunt raportate.

### L666 Guard pentru arhiva fara README
**Descriere tehnica:** Verifica daca folderul de arhiva are README cu inventar si regula de folosire.
**Scop:** Pastreaza istoricul navigabil.
**Target:** Archive audit.
**Prompt AI:** Creeaza un guard care verifica README-ul arhivei DeepSeek.
**Acceptare:** Lipsa README-ului sau a inventarului este raportata.

### L667 Guard pentru README fara link activ
**Descriere tehnica:** Confirma ca README-ul arhivei trimite catre seria activa curenta.
**Scop:** Leaga istoricul de continuare.
**Target:** Archive README.
**Prompt AI:** Adauga un guard care verifica linkul din arhiva catre seria activa.
**Acceptare:** Lipsa linkului activ este raportata.

### L668 Guard pentru README fara ghid
**Descriere tehnica:** Verifica daca README-ul arhivei trimite catre ghidul DeepSeek.
**Scop:** Pastreaza regulile usor de gasit.
**Target:** Archive README.
**Prompt AI:** Creeaza un guard care verifica trimiterea catre `deepseek-batch-guide.md`.
**Acceptare:** Lipsa linkului spre ghid este raportata.

### L669 Raport de drift intre ghid si index
**Descriere tehnica:** Compara ghidul DeepSeek cu indexul principal pentru batch-urile active.
**Scop:** Detecteaza divergente de navigare.
**Target:** Drift report.
**Prompt AI:** Creeaza un raport care compara ghidul cu indexul principal.
**Acceptare:** Diferentele sunt grupate pe fisier si interval.

### L670 Raport de drift intre arhiva si ghid
**Descriere tehnica:** Compara batch-urile arhivate din ghid cu inventarul arhivei.
**Scop:** Pastreaza istoricul sincronizat.
**Target:** Drift report.
**Prompt AI:** Adauga un raport care compara arhiva DeepSeek cu ghidul.
**Acceptare:** Drift-ul este raportat cu intrarile lipsa sau in plus.

### L671 Raport pentru batch-ul curent
**Descriere tehnica:** Produce un status scurt al ultimului batch activ, cu interval si tema.
**Scop:** Ajuta la handoff rapid.
**Target:** Status report.
**Prompt AI:** Creeaza un raport scurt pentru ultimul batch activ DeepSeek.
**Acceptare:** Raportul include fisier, interval, tema si data.

### L672 Raport pentru urmatorul batch
**Descriere tehnica:** Pregateste o nota despre urmatorul interval numeric asteptat.
**Scop:** Face continuarea predictibila.
**Target:** Planning report.
**Prompt AI:** Creeaza un raport care indica urmatorul interval DeepSeek disponibil.
**Acceptare:** Urmatorul start si final sunt calculate corect.

### L673 Checker pentru data de actualizare
**Descriere tehnica:** Verifica daca noile batch-uri au `Actualizat` la data curenta a editarii.
**Scop:** Reduce metadatele invechite.
**Target:** Metadata audit.
**Prompt AI:** Adauga un checker pentru campul `Actualizat` din batch-uri.
**Acceptare:** Datele lipsa sau vechi sunt raportate.

### L674 Checker pentru titlul batch-ului
**Descriere tehnica:** Verifica daca titlul fisierului contine batch number si intervalul corect.
**Scop:** Pastreaza titlurile clare.
**Target:** Markdown metadata audit.
**Prompt AI:** Creeaza un checker pentru titlul batch-urilor DeepSeek.
**Acceptare:** Titlurile incorecte sunt raportate.

### L675 Checker pentru sectiunea reguli
**Descriere tehnica:** Confirma ca fiecare batch nou include regulile standard.
**Scop:** Pastreaza instructiunile locale aproape de taskuri.
**Target:** Markdown structure audit.
**Prompt AI:** Adauga un checker pentru sectiunea `Reguli` din batch-uri.
**Acceptare:** Lipsa regulilor este raportata.

### L676 Checker pentru campul `Descriere tehnica`
**Descriere tehnica:** Verifica daca fiecare task contine campul obligatoriu.
**Scop:** Pastreaza contextul tehnic minim.
**Target:** Task schema audit.
**Prompt AI:** Creeaza un checker pentru campul `Descriere tehnica`.
**Acceptare:** Taskurile fara descriere tehnica sunt raportate.

### L677 Checker pentru campul `Scop`
**Descriere tehnica:** Verifica daca fiecare task declara rezultatul urmarit.
**Scop:** Pastreaza intentia clara.
**Target:** Task schema audit.
**Prompt AI:** Adauga un checker pentru campul `Scop`.
**Acceptare:** Taskurile fara scop sunt raportate.

### L678 Checker pentru campul `Target`
**Descriere tehnica:** Verifica daca fiecare task indica zona tehnica afectata.
**Scop:** Reduce taskurile imposibil de directionat.
**Target:** Task schema audit.
**Prompt AI:** Creeaza un checker pentru campul `Target`.
**Acceptare:** Taskurile fara target sunt raportate.

### L679 Checker pentru campul `Prompt AI`
**Descriere tehnica:** Verifica daca fiecare task are instructiunea executabila pentru model.
**Scop:** Pastreaza taskurile consumabile automat.
**Target:** Prompt schema audit.
**Prompt AI:** Adauga un checker pentru campul `Prompt AI`.
**Acceptare:** Taskurile fara prompt sunt raportate.

### L680 Checker pentru campul `Acceptare`
**Descriere tehnica:** Verifica daca fiecare task are criteriu de acceptare.
**Scop:** Face validarea posibila.
**Target:** Acceptance schema audit.
**Prompt AI:** Creeaza un checker pentru campul `Acceptare`.
**Acceptare:** Taskurile fara acceptare sunt raportate.

### L681 Checker pentru ordine campuri
**Descriere tehnica:** Verifica daca taskurile folosesc ordinea standard a campurilor.
**Scop:** Pastreaza citirea uniforma.
**Target:** Task style audit.
**Prompt AI:** Adauga un checker pentru ordinea campurilor in taskurile DeepSeek.
**Acceptare:** Ordinea diferita este raportata ca warning.

### L682 Checker pentru taskuri prea late
**Descriere tehnica:** Detecteaza taskurile care ating mai multe zone tehnice majore.
**Scop:** Pastreaza taskurile mici.
**Target:** Task scope audit.
**Prompt AI:** Creeaza un checker care semnaleaza taskurile prea largi.
**Acceptare:** Taskurile cu impact prea mare sunt marcate pentru impartire.

### L683 Checker pentru taskuri fara test
**Descriere tehnica:** Avertizeaza cand un task de runtime nu mentioneaza test sau audit read-only.
**Scop:** Pastreaza schimbarea verificabila.
**Target:** Test policy audit.
**Prompt AI:** Adauga un checker pentru taskurile de runtime fara verificare.
**Acceptare:** Lipsa testului sau auditului este raportata.

### L684 Checker pentru taskuri de parser fara warning
**Descriere tehnica:** Avertizeaza cand un task de parser nu cere warning clar pentru input invalid.
**Scop:** Pastreaza contractul de erori.
**Target:** Parser policy audit.
**Prompt AI:** Creeaza un checker pentru taskurile de parser fara warning.
**Acceptare:** Lipsa warning-ului este raportata.

### L685 Checker pentru mecanici mari
**Descriere tehnica:** Detecteaza taskurile care propun mecanici mari fara contract si regresie.
**Scop:** Protejeaza stabilitatea proiectului.
**Target:** Scope guard.
**Prompt AI:** Adauga un checker pentru mecanicile mari introduse fara contract.
**Acceptare:** Taskurile riscante sunt marcate pentru rescriere.

### L686 Checker pentru taskuri duplicate semantic
**Descriere tehnica:** Detecteaza taskurile cu formulare diferita, dar intentie identica.
**Scop:** Reduce redundanta in backlog.
**Target:** Duplicate audit.
**Prompt AI:** Creeaza un checker pentru duplicate semantice in batch-uri.
**Acceptare:** Duplicatele probabile sunt raportate cu ambele numere.

### L687 Checker pentru taskuri dependente de arhiva
**Descriere tehnica:** Avertizeaza cand un task activ depinde de un batch arhivat fara referinta explicita.
**Scop:** Pastreaza contextul vizibil.
**Target:** Dependency audit.
**Prompt AI:** Adauga un checker pentru dependinte nedeclarate catre arhiva.
**Acceptare:** Dependintele ascunse sunt raportate.

### L688 Checker pentru taskuri fara categorie
**Descriere tehnica:** Verifica daca fiecare task sta sub o categorie de nivel doi.
**Scop:** Pastreaza organizarea lizibila.
**Target:** Markdown structure audit.
**Prompt AI:** Creeaza un checker pentru taskurile fara categorie.
**Acceptare:** Taskurile orfane sunt raportate.

### L689 Checker pentru categorii goale
**Descriere tehnica:** Detecteaza categoriile fara taskuri sub ele.
**Scop:** Curata structura documentului.
**Target:** Markdown structure audit.
**Prompt AI:** Adauga un checker pentru categoriile goale din batch-uri.
**Acceptare:** Categoriile goale sunt raportate.

### L690 Checker pentru categorii prea mari
**Descriere tehnica:** Avertizeaza cand o categorie aduna prea multe taskuri fara subimpartire.
**Scop:** Pastreaza scanarea usoara.
**Target:** Markdown structure audit.
**Prompt AI:** Creeaza un checker pentru categoriile prea mari.
**Acceptare:** Categoriile care depasesc pragul sunt raportate.

### L691 Summary automat pentru batch
**Descriere tehnica:** Genereaza un rezumat de 3-5 randuri pentru fiecare batch activ.
**Scop:** Ajuta la review rapid.
**Target:** Summary generator.
**Prompt AI:** Creeaza un generator de rezumat scurt pentru batch-urile DeepSeek.
**Acceptare:** Rezumatul contine tema, interval si riscuri principale.

### L692 Summary automat pentru arhiva
**Descriere tehnica:** Genereaza un rezumat scurt al batch-urilor mutate in arhiva.
**Scop:** Face istoricul mai usor de parcurs.
**Target:** Archive summary.
**Prompt AI:** Creeaza un generator de rezumat pentru arhiva DeepSeek.
**Acceptare:** Rezumatul grupeaza batch-urile arhivate pe tema.

### L693 Raport de calitate pentru taskuri
**Descriere tehnica:** Agrega warning-urile de schema, stil si redundanta intr-un raport unic.
**Scop:** Ofera o imagine clara asupra calitatii backlog-ului.
**Target:** Quality report.
**Prompt AI:** Creeaza un raport de calitate pentru taskurile DeepSeek.
**Acceptare:** Raportul include numar total de warning-uri pe categorie.

### L694 Raport de risc pentru taskuri
**Descriere tehnica:** Marcheaza taskurile care ating runtime, parser, persistenta sau comenzi admin.
**Scop:** Ajuta la prioritizarea review-ului.
**Target:** Risk report.
**Prompt AI:** Creeaza un raport de risc pentru taskurile DeepSeek.
**Acceptare:** Taskurile riscante sunt grupate pe tip de impact.

### L695 Raport de efort estimat
**Descriere tehnica:** Eticheteaza taskurile ca mic, mediu sau mare pe baza targetului si acceptarii.
**Scop:** Ajuta la planificarea batch-urilor de implementare.
**Target:** Planning report.
**Prompt AI:** Creeaza o estimare simpla de efort pentru taskurile DeepSeek.
**Acceptare:** Fiecare task primeste o eticheta de efort.

### L696 Raport de dependinte
**Descriere tehnica:** Listeaza dependintele intre taskuri cand acestea pot fi deduse din target sau descriere.
**Scop:** Evita executia in ordine gresita.
**Target:** Dependency report.
**Prompt AI:** Creeaza un raport de dependinte pentru taskurile DeepSeek.
**Acceptare:** Dependintele evidente sunt listate cu task sursa si task tinta.

### L697 Raport de prioritizare
**Descriere tehnica:** Sorteaza taskurile dupa risc, efort si valoare operationala.
**Scop:** Ajuta la alegerea urmatorului task implementabil.
**Target:** Prioritization report.
**Prompt AI:** Creeaza un raport de prioritizare pentru taskurile DeepSeek.
**Acceptare:** Primele taskuri recomandate au motivatie scurta.

### L698 Raport de handoff
**Descriere tehnica:** Produce o nota scurta pentru alt agent sau mentainer despre starea seriei.
**Scop:** Pastreaza continuitatea lucrului.
**Target:** Handoff report.
**Prompt AI:** Scrie un raport de handoff pentru seria DeepSeek activa.
**Acceptare:** Raportul include activ, arhiva, urmatorul interval si riscuri.

### L699 Pregatire pentru urmatorul ciclu
**Descriere tehnica:** Calculeaza si anunta urmatorul interval numeric dupa L700.
**Scop:** Face continuarea previzibila.
**Target:** Next batch note.
**Prompt AI:** Scrie o nota scurta care pregateste urmatorul ciclu DeepSeek.
**Acceptare:** Urmatorul interval este indicat clar.

### L700 Nota finala pentru batch-ul 13
**Descriere tehnica:** Rezuma extinderea L651-L700 si rolul ei in automatizarea seriei DeepSeek.
**Scop:** Inchide batch-ul cu un rezumat canonic.
**Target:** Release note, docs index, changelog.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L651-L700.
**Acceptare:** Documentul marcheaza clar ce a fost adaugat si de ce.

