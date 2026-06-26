# DeepSeek Taskuri - Batch 5 (L251-L300)

Actualizat: 2026-06-24

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Infrastructura de taskuri

### L251 Registry pentru batch-uri DeepSeek
**Descriere tehnica:** Centralizeaza lista batch-urilor si metadatele lor intr-un registry simplu.
**Scop:** Face navigarea si auditul mai usor de automatizat.
**Target:** Docs registry, index, navigare.
**Prompt AI:** Creeaza un registry care listeaza toate batch-urile DeepSeek si intervalele lor.
**Acceptare:** Batch-urile pot fi gasite fara a cauta manual in README.

### L252 Normalizare a numerotarii
**Descriere tehnica:** Valideaza ca fiecare batch foloseste intervale numerice consecutive si fara suprapuneri.
**Scop:** Elimina confuziile la extinderea backlog-ului.
**Target:** Docs lint, numbering audit.
**Prompt AI:** Adauga o regula de validare pentru intervalele numerice ale taskurilor DeepSeek.
**Acceptare:** Gaps si dubluri sunt raportate explicit.

### L253 Template pentru batch nou
**Descriere tehnica:** Definește un sablon standard pentru un nou fisier batch DeepSeek.
**Scop:** Reduce variatia dintre batch-uri.
**Target:** Docs template, authoring guide.
**Prompt AI:** Creeaza un template reutilizabil pentru batch-urile urmatoare.
**Acceptare:** Un batch nou poate fi pornit fara structura ad-hoc.

### L254 Index automat pentru batch-uri
**Descriere tehnica:** Adauga generare automata a indexului de batch-uri.
**Scop:** Evita actualizari manuale uitate.
**Target:** Docs tooling, README generator.
**Prompt AI:** Creeaza un mecanism care regenereaza indexul cu batch-urile DeepSeek.
**Acceptare:** Indexul se actualizeaza consistent dupa adaugarea unui fisier nou.

### L255 Link spre batch precedent
**Descriere tehnica:** Adauga referinta explicita la batch-ul anterior in header.
**Scop:** Usureaza citirea secventiala.
**Target:** Docs navigation, header metadata.
**Prompt AI:** Introdu un link sau o nota care pointeaza spre batch-ul precedent.
**Acceptare:** Cititorul poate merge direct la batch-ul anterior.

### L256 Link spre batch urmator
**Descriere tehnica:** Rezerva o referinta clara pentru batch-ul urmator.
**Scop:** Face continuitatea istorica vizibila.
**Target:** Docs navigation, header metadata.
**Prompt AI:** Introdu o nota pentru batch-ul urmator inainte ca acesta sa existe.
**Acceptare:** Structura documentului arata clar cum continua seria.

### L257 Sumar tematic pe batch
**Descriere tehnica:** Scrie un rezumat scurt al temei batch-ului in antet.
**Scop:** Ghideaza citirea rapida.
**Target:** Docs summary, header.
**Prompt AI:** Adauga un sumar tematic de o propozitie pentru batch-ul curent.
**Acceptare:** Cititorul intelege imediat ce acopera batch-ul.

### L258 Etichete pentru audit
**Descriere tehnica:** Adauga taguri consistente pentru taskurile de infrastructura.
**Scop:** Face filtrarea automata mai simpla.
**Target:** Docs metadata, audit filters.
**Prompt AI:** Creeaza un set de etichete standard pentru taskurile DeepSeek.
**Acceptare:** Taskurile pot fi filtrate dupa tema.

### L259 Politica pentru archiving
**Descriere tehnica:** Defineste cand un batch poate fi mutat in arhiva.
**Scop:** Pastreaza backlog-ul activ curat.
**Target:** Docs maintenance, archive policy.
**Prompt AI:** Adauga o regula de arhivare pentru batch-urile complet rezolvate.
**Acceptare:** Batch-urile vechi au un criteriu clar de arhivare.

### L260 Check de compatibilitate intre batch-uri
**Descriere tehnica:** Detecteaza sectiunile sau regulile care se contrazic intre batch-uri.
**Scop:** Evita inconsistentele in backlog.
**Target:** Docs audit, conflict detection.
**Prompt AI:** Creeaza un audit care gaseste reguli sau formulări care se bat cap in cap intre batch-uri.
**Acceptare:** Contradictiile sunt raportate cu referinta la ambele locuri.

## Prompting si control

### L261 Prompt builder pentru batch-uri
**Descriere tehnica:** Generaza promptul de task dintr-un set de campuri structurate.
**Scop:** Reduce eroarea umana la scrierea prompturilor.
**Target:** Prompt builder, task generator.
**Prompt AI:** Creeaza un builder care combina titlu, scop, target si acceptare intr-un prompt stabil.
**Acceptare:** Prompturile rezultate au format uniform.

### L262 Sanitizare pentru input de prompt
**Descriere tehnica:** Curata caracterele problematice inainte de compunerea promptului.
**Scop:** Evita injectii de formatare si output corupt.
**Target:** Prompt builder, sanitizer.
**Prompt AI:** Adauga sanitizare pentru textul folosit la construirea prompturilor.
**Acceptare:** Inputul ciudat nu strica formatul promptului.

### L263 Separator pentru context si instructiuni
**Descriere tehnica:** Delimiteaza clar contextul de instructiunile executabile.
**Scop:** Reduce confuzia modelului intre referinte si actiuni.
**Target:** Prompt template, section delimiters.
**Prompt AI:** Creeaza o separare explicita intre context, reguli si cerinta.
**Acceptare:** Promptul are sectiuni distincte si usor de citit.

### L264 Guard pentru prompturi prea lungi
**Descriere tehnica:** Marcheaza prompturile care depasesc un prag sigur de lungime.
**Scop:** Pastreaza controlul asupra costului si claritatii.
**Target:** Prompt validator, length guard.
**Prompt AI:** Adauga un guard care semnaleaza prompturile prea lungi.
**Acceptare:** Prompturile mari sunt oprite sau simplificate.

### L265 Guard pentru prompturi ambigue
**Descriere tehnica:** Detecteaza formulele care lasa scopul sau targetul neclar.
**Scop:** Creste predictibilitatea taskurilor.
**Target:** Prompt lint, ambiguity detector.
**Prompt AI:** Creeaza un detector pentru ambiguitatea din prompturile DeepSeek.
**Acceptare:** Prompturile neclare sunt raportate pentru rescriere.

### L266 Fișă pentru intentie de task
**Descriere tehnica:** Inregistreaza intentia reala a unui task separat de redactarea lui.
**Scop:** Ajuta la review si prioritizare.
**Target:** Task metadata, planning notes.
**Prompt AI:** Adauga un camp separat pentru intentia de business sau tehnica a taskului.
**Acceptare:** Intentia poate fi citita fara a interpreta promptul.

### L267 Politica pentru taskuri destructive
**Descriere tehnica:** Marcheaza orice task care poate sterge sau rescrie date.
**Scop:** Protejeaza datele si documentatia.
**Target:** Safety policy, approval flow.
**Prompt AI:** Creeaza o regula care eticheteaza taskurile destructive si cere aprobare.
**Acceptare:** Taskurile destructive sunt vizibile si controlate.

### L268 Politica pentru taskuri cu efecte externe
**Descriere tehnica:** Identifica taskurile care ating retele, servicii externe sau integrari.
**Scop:** Pastreaza separarea dintre local-first si extern.
**Target:** Safety policy, integration guard.
**Prompt AI:** Adauga o regula pentru taskurile care ar putea face IO extern.
**Acceptare:** Taskurile externe cer semnal explicit.

### L269 Guard pentru continut nesanitizat
**Descriere tehnica:** Verifica daca output-ul contine fragmente brute care ar trebui redactate.
**Scop:** Evita scurgerile de date sau markup corupt.
**Target:** Output sanitizer, audit.
**Prompt AI:** Creeaza un guard care detecteaza continut nesanitizat in output.
**Acceptare:** Datele brute sensibile sunt semnalate inainte de salvare.

### L270 Audit pentru instructiuni contradictorii
**Descriere tehnica:** Compara regulile din batch si scoate in evidenta instructiunile care se contrazic.
**Scop:** Previne conflictele in generarea viitoare.
**Target:** Prompt audit, docs lint.
**Prompt AI:** Adauga un audit care gaseste instructiuni contradictorii intre batch-uri.
**Acceptare:** Contradictiile sunt listate explicit.

## Calitate si verificare

### L271 Test pentru registry-ul de batch-uri
**Descriere tehnica:** Acopera scenariul in care un batch nou este adaugat in registry.
**Scop:** Blocheaza regresiile in indexare.
**Target:** Unit test, registry.
**Prompt AI:** Adauga un test care confirma ca registry-ul include batch-ul nou.
**Acceptare:** Testul cade daca un batch lipseste din registry.

### L272 Test pentru normalizarea numerelor
**Descriere tehnica:** Valideaza parse-ul numerelor de task si a intervalelor.
**Scop:** Protejeaza numerotarea consecutiva.
**Target:** Test suite, numbering logic.
**Prompt AI:** Creeaza test pentru detectarea numerelor duplicate sau lipsa.
**Acceptare:** Secventa invalida este respinsa.

### L273 Test pentru template-ul de batch
**Descriere tehnica:** Verifica faptul ca template-ul produce o structura completa.
**Scop:** Pastreaza standardizarea documentelor.
**Target:** Docs template test.
**Prompt AI:** Adauga un test care confirma prezenta sectiunilor obligatorii in template.
**Acceptare:** Template-ul nu permite lipsa sectiunilor esentiale.

### L274 Test pentru sanitizerul de prompt
**Descriere tehnica:** Acopera caracterele speciale si inputul brut.
**Scop:** Previne iesiri corupte.
**Target:** Sanitizer test.
**Prompt AI:** Creeaza test pentru sanitizarea prompturilor si a textului de context.
**Acceptare:** Textul periculos este normalizat fara pierdere de structura.

### L275 Test pentru separarea contextului
**Descriere tehnica:** Asigura delimitarea clara intre sectiuni.
**Scop:** Evita amestecarea instructiunilor cu referintele.
**Target:** Prompt template test.
**Prompt AI:** Adauga test care verifica delimitatoarele dintre context si instructiuni.
**Acceptare:** Sectiunile sunt distincte in output.

### L276 Test pentru prag de lungime
**Descriere tehnica:** Verifica respingerea prompturilor prea mari.
**Scop:** Pastreaza costul controlat.
**Target:** Length guard test.
**Prompt AI:** Creeaza test care confirma blocarea prompturilor peste prag.
**Acceptare:** Inputul prea mare e marcat clar.

### L277 Test pentru taskuri destructive
**Descriere tehnica:** Acopera etichetarea si aprobarea taskurilor cu impact mare.
**Scop:** Protejeaza modificarile riscante.
**Target:** Safety policy test.
**Prompt AI:** Adauga test pentru detectarea taskurilor destructive.
**Acceptare:** Taskurile riscante primesc marcajul corect.

### L278 Test pentru efecte externe
**Descriere tehnica:** Valideaza detectia operatiilor care ating integrari externe.
**Scop:** Pastreaza local-first-ul controlat.
**Target:** Integration guard test.
**Prompt AI:** Creeaza test pentru taskurile cu efecte externe.
**Acceptare:** Operatiile externe sunt semnalate in mod predictibil.

### L279 Test pentru warning-uri de contradiction
**Descriere tehnica:** Verifica raportarea instructiunilor contradictorii.
**Scop:** Pastreaza calitatea prompturilor.
**Target:** Audit test, contradiction detector.
**Prompt AI:** Adauga test care confirma raportarea conflictelor intre reguli.
**Acceptare:** Conflictul produce warning explicit.

### L280 Smoke check pentru documentul batch
**Descriere tehnica:** Ruleaza o verificare rapida pe structura si numerotarea batch-ului.
**Scop:** Asigura publicarea fara erori evidente.
**Target:** Docs smoke, lint step.
**Prompt AI:** Creeaza un smoke check care valideaza formatul batch-ului inainte de salvare.
**Acceptare:** Batch-ul trece doar daca structura este completa.

## Continuitate si mentenanta

### L281 Nota de legatura cu batch-ul anterior
**Descriere tehnica:** Rezuma ce tema a fost preluata din batch-ul anterior.
**Scop:** Pastreaza continuitatea narativa a backlog-ului.
**Target:** Release note, docs summary.
**Prompt AI:** Scrie o nota scurta care leaga batch-ul curent de batch-ul anterior.
**Acceptare:** Cititorul intelege de unde continua seria.

### L282 Nota de pregatire pentru batch-ul urmator
**Descriere tehnica:** Indica directia pentru extinderea viitoare.
**Scop:** Ajuta la planificarea urmatorului set.
**Target:** Planning note, roadmap.
**Prompt AI:** Adauga o nota scurta care pregateste tema batch-ului urmator.
**Acceptare:** Seria ramane usor de continuat.

### L283 Sumar de decizii
**Descriere tehnica:** Colecteaza deciziile cheie luate pentru formatul batch-urilor.
**Scop:** Ajuta la consistenta pe termen lung.
**Target:** Decision log, docs governance.
**Prompt AI:** Creeaza un sumar scurt al deciziilor de format si control adoptate pana acum.
**Acceptare:** Deciziile de format sunt usor de gasit.

### L284 Lista de intrebari deschise
**Descriere tehnica:** Marcheaza zonele unde mai sunt necunoscute in backlog.
**Scop:** Evita presupunerile in batch-uri viitoare.
**Target:** Planning backlog, open questions.
**Prompt AI:** Adauga o lista de intrebari deschise pentru urmatorul batch DeepSeek.
**Acceptare:** Intrebarile sunt explicite si actionabile.

### L285 Politica pentru curatarea batch-urilor vechi
**Descriere tehnica:** Stabileste cand si cum se comprima sau arhiveaza batch-urile vechi.
**Scop:** Pastreaza documentatia gestionabila.
**Target:** Archive policy, docs maintenance.
**Prompt AI:** Creeaza o politica simpla pentru curatarea batch-urilor vechi si a notelor duplicate.
**Acceptare:** Batch-urile vechi nu raman redundante fara motiv.

### L286 Politica pentru extindere incrementala
**Descriere tehnica:** Defineste regula de a adauga doar un numar limitat de taskuri pe batch.
**Scop:** Păstrează controlul asupra complexității.
**Target:** Planning policy, batch sizing.
**Prompt AI:** Adaugă o regulă privind dimensiunea maximă a unui batch DeepSeek.
**Acceptare:** Batch-urile rămân mici și verificabile.

### L287 Politica pentru teme noi
**Descriere tehnica:** Desparte temele noi de cele deja stabilizate.
**Scop:** Reduce riscul de amestecare a priorităților.
**Target:** Roadmap policy, backlog taxonomy.
**Prompt AI:** Creează o regulă care marchează o temă nouă ca experimentală până la validare.
**Acceptare:** Temele noi nu intră direct în fluxul stabil.

### L288 Politica pentru refactor de documentație
**Descriere tehnica:** Permite doar refactorări care nu schimbă sensul.
**Scop:** Protejează sursa canonică.
**Target:** Docs maintenance, review policy.
**Prompt AI:** Adaugă o regulă pentru refactorul documentației fără schimbarea conținutului tehnic.
**Acceptare:** Refactorul nu alterează contractele scrise.

### L289 Politica pentru versiuni istorice
**Descriere tehnica:** Menține accesul la versiunile anterioare ale batch-urilor.
**Scop:** Ajută la audit și comparație.
**Target:** Archive docs, history index.
**Prompt AI:** Creează o regulă pentru păstrarea versiunilor istorice ale batch-urilor DeepSeek.
**Acceptare:** Versiunile vechi pot fi găsite și comparate.

### L290 Changelog pentru batch-ul curent
**Descriere tehnica:** Notează ce a intrat în acest batch și ce problemă rezolvă.
**Scop:** Păstrează istoria schimbărilor.
**Target:** Changelog, release notes.
**Prompt AI:** Scrie o intrare scurtă de changelog pentru batch-ul L251-L300.
**Acceptare:** Schimbările sunt rezumate clar și scurt.

### L291 Regula pentru taskuri de curățenie
**Descriere tehnica:** Clasifică taskurile care doar îmbunătățesc lizibilitatea sau ordinea.
**Scop:** Ajută la separarea lor de taskurile funcționale.
**Target:** Task taxonomy, docs policy.
**Prompt AI:** Adaugă o regulă clară pentru taskurile de curățenie și lizibilitate.
**Acceptare:** Taskurile de curățenie sunt etichetate consistent.

### L292 Regula pentru taskuri de risc redus
**Descriere tehnica:** Marchează taskurile care pot fi executate fără impact semnificativ.
**Scop:** Permite tratarea lor ca backlog sigur.
**Target:** Task taxonomy, priority labels.
**Prompt AI:** Creează o regulă pentru identificarea taskurilor cu risc redus.
**Acceptare:** Taskurile simple sunt ușor de recunoscut și prioritizat.

### L293 Regula pentru taskuri de verificare
**Descriere tehnica:** Separă taskurile care verifică existentul de cele care adaugă nou.
**Scop:** Păstrează claritatea în planificare.
**Target:** Task taxonomy, audit grouping.
**Prompt AI:** Adaugă o regulă care separă taskurile de verificare de cele de implementare.
**Acceptare:** Taskurile de verificare sunt marcate distinct.

### L294 Regula pentru taskuri de documentare
**Descriere tehnica:** Marchează taskurile al căror rezultat principal este documentația.
**Scop:** Evită confuzia cu taskurile de cod.
**Target:** Task taxonomy, docs classification.
**Prompt AI:** Creează o regulă pentru taskurile al căror output principal este documentația.
**Acceptare:** Taskurile documentare sunt separate de cele de runtime.

### L295 Regula pentru taskuri de orchestrare
**Descriere tehnica:** Identifică taskurile care modifică fluxul de decizie și nu conținutul.
**Scop:** Ghidează review-ul pentru componente sensibile.
**Target:** Orchestrator taxonomy, review policy.
**Prompt AI:** Adaugă o regulă pentru taskurile de orchestrare și control.
**Acceptare:** Taskurile de orchestrare sunt ușor de audit.

### L296 Regula pentru taskuri de audit
**Descriere tehnica:** Clarifică modul în care sunt scrise și tratate taskurile de audit read-only.
**Scop:** Păstrează auditul sigur și fără efecte secundare.
**Target:** Audit policy, read-only tasks.
**Prompt AI:** Scrie o regulă scurtă pentru taskurile de audit care nu modifică nimic.
**Acceptare:** Taskurile de audit sunt explicit read-only.

### L297 Regula pentru taskuri cu fallback
**Descriere tehnica:** Definește când un task trebuie să includă fallback sau degradare sigură.
**Scop:** Protejează runtime-ul și documentația.
**Target:** Safety policy, fallback requirements.
**Prompt AI:** Adaugă o regulă pentru taskurile care trebuie să specifice fallback-ul.
**Acceptare:** Taskurile sensibile menționează ce se întâmplă la eșec.

### L298 Regula pentru taskuri cu autorizare
**Descriere tehnica:** Marchează taskurile care cer aprobare umană înainte de aplicare.
**Scop:** Menține controlul asupra schimbărilor sensibile.
**Target:** Approval policy, execution gate.
**Prompt AI:** Creează o regulă pentru taskurile care trebuie aprobate înainte de execuție.
**Acceptare:** Taskurile critice nu pornesc fără aprobare.

### L299 Regula pentru taskuri finale de batch
**Descriere tehnica:** Definește ce conține ultimul task dintr-un batch.
**Scop:** Păstrează încheierea batch-ului clară și utilă.
**Target:** Batch structure, release note.
**Prompt AI:** Adaugă o regulă pentru ultimul task din batch astfel încât să rezume batch-ul.
**Acceptare:** Ultimul task încheie batch-ul cu un rezumat util.

### L300 Nota finală pentru batch-ul 5
**Descriere tehnica:** Rezumă extensia adusă de L251-L300 și ce urmează.
**Scop:** Închide seria curentă cu un rezumat canonic.
**Target:** Release note, docs index, changelog.
**Prompt AI:** Scrie o notă finală scurtă pentru batch-ul L251-L300.
**Acceptare:** Documentul marchează clar ce a fost adăugat și de ce.
