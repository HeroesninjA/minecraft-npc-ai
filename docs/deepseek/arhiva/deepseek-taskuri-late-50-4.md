# DeepSeek Taskuri - Batch 6 (L301-L350)

Actualizat: 2026-06-24

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Control de calitate

### L301 Guard pentru taskuri fără `Prompt AI`
**Descriere tehnica:** Verifică dacă fiecare task conține un prompt executabil și complet.
**Scop:** Evită taskurile care nu pot fi consumate de model.
**Target:** Docs lint, task validator.
**Prompt AI:** Creează un guard care respinge taskurile fără secțiunea `Prompt AI`.
**Acceptare:** Taskurile incomplete sunt semnalate înainte de publicare.

### L302 Guard pentru taskuri fără `Target`
**Descriere tehnica:** Detectează taskurile care nu indică zona tehnică afectată.
**Scop:** Crește claritatea și scade ambiguitatea.
**Target:** Docs lint, metadata validator.
**Prompt AI:** Adaugă un checker care cere câmpul `Target` pentru fiecare task.
**Acceptare:** Intrările fără target sunt raportate explicit.

### L303 Guard pentru taskuri fără `Scop`
**Descriere tehnica:** Semnalează taskurile care nu explică rezultatul urmărit.
**Scop:** Păstrează backlog-ul orientat pe rezultate.
**Target:** Docs lint, task metadata.
**Prompt AI:** Creează o verificare care cere descrierea scopului pentru fiecare task.
**Acceptare:** Taskurile fără scop nu trec validarea.

### L304 Guard pentru acceptare neclară
**Descriere tehnica:** Detectează criteriile de acceptare care nu pot fi verificate.
**Scop:** Face taskurile măsurabile.
**Target:** Acceptance lint, docs review.
**Prompt AI:** Adaugă un detector pentru acceptare vagă sau neverificabilă.
**Acceptare:** Taskurile fără criterii clare sunt semnalate pentru rescriere.

### L305 Guard pentru taskuri prea mari
**Descriere tehnica:** Marchează taskurile care par să conțină mai mult decât o singură schimbare mică.
**Scop:** Menține granularitatea backlog-ului.
**Target:** Scope checker, planning lint.
**Prompt AI:** Creează un semnal pentru taskurile care trebuie împărțite.
**Acceptare:** Taskurile supradimensionate sunt raportate.

### L306 Guard pentru taskuri redundante
**Descriere tehnica:** Caută taskuri foarte apropiate semantic de altele existente.
**Scop:** Evită duplicarea muncii.
**Target:** Similarity audit, docs scan.
**Prompt AI:** Adaugă un audit care găsește taskurile redundante sau aproape identice.
**Acceptare:** Duplicatele sunt listate cu referințe clare.

### L307 Guard pentru numerotare secvențială
**Descriere tehnica:** Verifică dacă numerotarea taskurilor rămâne consecutivă.
**Scop:** Evită lipsuri și suprapuneri.
**Target:** Numbering audit, docs lint.
**Prompt AI:** Creează un validator care confirmă secvența numerică a taskurilor.
**Acceptare:** Orice gap sau dublură este raportată.

### L308 Guard pentru secțiuni lipsă
**Descriere tehnica:** Verifică prezența tuturor secțiunilor obligatorii în fiecare task.
**Scop:** Păstrează formatul uniform.
**Target:** Structure validator, docs tooling.
**Prompt AI:** Adaugă un checker pentru secțiunile obligatorii din taskuri.
**Acceptare:** Taskurile incomplete sunt respinse.

### L309 Guard pentru text corupt
**Descriere tehnica:** Detectează caractere sau markup care pot rupe randarea documentului.
**Scop:** Protejează lizibilitatea documentației.
**Target:** Markdown sanitizer, docs writer.
**Prompt AI:** Creează un sanitizer pentru textul generat de taskuri.
**Acceptare:** Documentele rămân randabile și curate.

### L310 Guard pentru limbaj inconsistent
**Descriere tehnica:** Semnalează folosirea unor termeni diferiți pentru același concept.
**Scop:** Menține terminologia controlată.
**Target:** Terminology audit, style checker.
**Prompt AI:** Adaugă un audit care detectează inconsistențe de terminologie.
**Acceptare:** Sinonimele necontrolate sunt raportate.

## Orchestrare și rutare

### L311 Router pentru batch-uri noi
**Descriere tehnica:** Direcționează taskurile către profilul de generare potrivit în funcție de tip.
**Scop:** Reduce erorile de procesare.
**Target:** Orchestrator, routing policy.
**Prompt AI:** Creează reguli de rutare pentru taskurile noi în funcție de categorie și risc.
**Acceptare:** Taskurile simple și riscante sunt rutate diferit.

### L312 Router pentru taskuri read-only
**Descriere tehnica:** Separă explicit taskurile de audit și verificare de cele de implementare.
**Scop:** Protejează fluxurile fără efecte secundare.
**Target:** Orchestrator, read-only profile.
**Prompt AI:** Adaugă o rută separată pentru taskurile read-only.
**Acceptare:** Taskurile de audit nu primesc tratament de patch.

### L313 Router pentru taskuri cu aprobare
**Descriere tehnica:** Marchează taskurile care trebuie aprobate înainte de execuție.
**Scop:** Menține controlul asupra schimbărilor sensibile.
**Target:** Approval gate, routing policy.
**Prompt AI:** Creează o rută care blochează taskurile riscante până la aprobare.
**Acceptare:** Taskurile sensibile nu pornesc automat.

### L314 Router pentru taskuri de documentație
**Descriere tehnica:** Trimite taskurile de documentare către un profil orientat pe text și consistență.
**Scop:** Optimizează rezultatele pentru documente, nu pentru cod.
**Target:** Prompt routing, docs profile.
**Prompt AI:** Adaugă o rută separată pentru taskurile care produc doar documentație.
**Acceptare:** Taskurile de documentare au output stabil și compact.

### L315 Router pentru taskuri de verificare
**Descriere tehnica:** Prioritizează taskurile care confirmă starea existentă.
**Scop:** Întărește observabilitatea.
**Target:** Audit routing, verification profile.
**Prompt AI:** Creează o rută pentru taskurile de verificare și audit.
**Acceptare:** Verificările nu ajung pe profilul de implementare.

### L316 Router pentru taskuri de curățare
**Descriere tehnica:** Întoarce taskurile de curățenie și organizare către un profil conservator.
**Scop:** Evită rescrierea inutilă a conținutului tehnic.
**Target:** Cleanup routing, docs maintenance.
**Prompt AI:** Adaugă o rută pentru taskurile de curățare a documentației.
**Acceptare:** Taskurile de curățenie nu modifică sensul.

### L317 Router pentru taskuri de rezumat
**Descriere tehnica:** Separă taskurile care rezumă de cele care introduc conținut nou.
**Scop:** Păstrează rezultatele concise.
**Target:** Summary routing, docs profile.
**Prompt AI:** Creează o rută pentru taskurile al căror output principal este un rezumat.
**Acceptare:** Taskurile de sumarizare rămân scurte și citibile.

### L318 Router pentru taskuri experimentale
**Descriere tehnica:** Marchează taskurile exploratorii ca izolate de fluxul stabil.
**Scop:** Protejează backlog-ul principal.
**Target:** Experimental routing, safety policy.
**Prompt AI:** Adaugă o rută separată pentru taskurile experimentale.
**Acceptare:** Taskurile experimentale nu contaminează fluxul standard.

### L319 Router pentru taskuri de fallback
**Descriere tehnica:** Leagă taskurile care definesc degradare sigură de un profil specializat.
**Scop:** Menține consistența fallback-urilor.
**Target:** Fallback routing, safety profile.
**Prompt AI:** Creează o rută pentru taskurile care specifică fallback sau no-op.
**Acceptare:** Taskurile de fallback sunt tratate ca read-only și conservator.

### L320 Router pentru taskuri finale de batch
**Descriere tehnica:** Asigură că taskul final al batch-ului are rol de închidere și sumar.
**Scop:** Face batch-urile autoexplicative.
**Target:** Batch routing, release notes.
**Prompt AI:** Adaugă o regulă pentru taskul final al unui batch.
**Acceptare:** Ultimul task rezumă și închide batch-ul.

## Documentație și menținere

### L321 Index compact pentru batch-uri
**Descriere tehnica:** Produce un index condensat al tuturor batch-urilor existente.
**Scop:** Simplifică navigarea în documentație.
**Target:** Docs index, README generator.
**Prompt AI:** Creează un index compact care listează batch-urile DeepSeek într-o singură secțiune.
**Acceptare:** Batch-urile sunt ușor de scanat și găsit.

### L322 Sumar executiv pentru seria DeepSeek
**Descriere tehnica:** Creează o descriere foarte scurtă a întregii serii de taskuri.
**Scop:** Oferă context rapid pentru un cititor nou.
**Target:** Overview doc, summary note.
**Prompt AI:** Scrie un sumar executiv al seriei DeepSeek de taskuri.
**Acceptare:** Un cititor nou înțelege tema și scopul seriei.

### L323 Reguli pentru batch nou
**Descriere tehnica:** Definește criteriile minime pentru a deschide un batch nou.
**Scop:** Menține backlog-ul controlabil.
**Target:** Authoring policy, docs standards.
**Prompt AI:** Adaugă reguli clare pentru inițierea unui batch nou DeepSeek.
**Acceptare:** Batch-ul nou respectă formatul și scopul.

### L324 Reguli pentru închiderea batch-ului
**Descriere tehnica:** Stabilește când un batch este considerat complet.
**Scop:** Clarifică tranziția către următorul batch.
**Target:** Maintenance policy, release notes.
**Prompt AI:** Creează o regulă simplă pentru închiderea unui batch de taskuri.
**Acceptare:** Închiderea batch-ului este verificabilă.

### L325 Reguli pentru actualizarea indexului
**Descriere tehnica:** Explică ce trebuie modificat când apare un fișier nou.
**Scop:** Evită linkuri uitate.
**Target:** README policy, docs maintenance.
**Prompt AI:** Adaugă o regulă pentru actualizarea indexului după fiecare batch nou.
**Acceptare:** Indexul rămâne sincronizat cu fișierele.

### L326 Reguli pentru arhivare
**Descriere tehnica:** Definește când un batch mutat din activ devine arhivă.
**Scop:** Păstrează backlog-ul principal curat.
**Target:** Archive policy, docs lifecycle.
**Prompt AI:** Creează o politică scurtă de arhivare pentru batch-urile DeepSeek.
**Acceptare:** Arhivarea are criterii clare și documentate.

### L327 Reguli pentru istoric
**Descriere tehnica:** Păstrează traseul deciziilor și al modificărilor de format.
**Scop:** Ajută la audit și comparații istorice.
**Target:** History log, docs governance.
**Prompt AI:** Adaugă o regulă pentru păstrarea istoricului batch-urilor și a schimbărilor de format.
**Acceptare:** Istoricul poate fi urmărit în ordine.

### L328 Reguli pentru rezumate
**Descriere tehnica:** Stabilește nivelul de detaliu acceptat pentru rezumatele de batch.
**Scop:** Evită rezumatele prea lungi sau prea vagi.
**Target:** Summary policy, docs style.
**Prompt AI:** Scrie reguli de lungime și claritate pentru rezumatele de batch.
**Acceptare:** Rezumatele rămân concise și utile.

### L329 Reguli pentru note de release
**Descriere tehnica:** Definește ce informații trebuie să conțină nota de release a unui batch.
**Scop:** Face schimbările ușor de înțeles.
**Target:** Release notes, changelog.
**Prompt AI:** Creează un șablon scurt pentru note de release ale batch-urilor.
**Acceptare:** Nota de release conține schimbarea și motivul.

### L330 Reguli pentru taskuri de documentare
**Descriere tehnica:** Clarifică ce se consideră task documentar în seria DeepSeek.
**Scop:** Separa documentarea de implementare.
**Target:** Task taxonomy, docs policy.
**Prompt AI:** Adaugă o regulă pentru clasificarea taskurilor de documentare.
**Acceptare:** Taskurile documentare sunt etichetate consecvent.

## Verificare și siguranță

### L331 Test pentru routerul read-only
**Descriere tehnica:** Verifică separarea dintre audit și implementare.
**Scop:** Protejează taskurile fără efecte secundare.
**Target:** Unit test, routing policy.
**Prompt AI:** Creează un test care confirmă că taskurile read-only ajung pe profilul corect.
**Acceptare:** Taskurile read-only nu sunt tratate ca patch-uri.

### L332 Test pentru routerul cu aprobare
**Descriere tehnica:** Confirmă că taskurile sensibile sunt blocate până la aprobare.
**Scop:** Menține controlul uman.
**Target:** Approval gate test.
**Prompt AI:** Adaugă un test pentru blocarea taskurilor până la aprobare manuală.
**Acceptare:** Taskurile sensibile nu pornesc fără confirmare.

### L333 Test pentru validarea secțiunilor
**Descriere tehnica:** Verifică prezența câmpurilor obligatorii în fiecare task.
**Scop:** Ține formatul stabil.
**Target:** Structure validator test.
**Prompt AI:** Creează un test pentru câmpurile obligatorii ale taskurilor.
**Acceptare:** Lipsa unei secțiuni produce eșec clar.

### L334 Test pentru sanitizarea textului
**Descriere tehnica:** Acoperă caracterele speciale și markup-ul problematic.
**Scop:** Previne output corupt.
**Target:** Sanitizer test, docs writer.
**Prompt AI:** Adaugă un test care validează sanitizarea textului generat.
**Acceptare:** Textul rămâne randabil după sanitizare.

### L335 Test pentru numerotare consecutivă
**Descriere tehnica:** Confirmă că intervalul taskurilor este continuu.
**Scop:** Evită lipsurile în serie.
**Target:** Numbering audit test.
**Prompt AI:** Creează un test care verifică secvența numerică a taskurilor DeepSeek.
**Acceptare:** Orice lipsă sau dublură este detectată.

### L336 Test pentru indexul compact
**Descriere tehnica:** Acoperă generarea indexului condensat al batch-urilor.
**Scop:** Asigură navigarea documentelor.
**Target:** Docs index test.
**Prompt AI:** Adaugă un test pentru indexul compact al batch-urilor.
**Acceptare:** Indexul include toate batch-urile relevante.

### L337 Test pentru rezumatul executiv
**Descriere tehnica:** Verifică faptul că sumarul rămâne scurt și util.
**Scop:** Protejează lizibilitatea.
**Target:** Summary test, docs QA.
**Prompt AI:** Creează un test pentru calitatea sumarului executiv al seriei.
**Acceptare:** Rezumatul nu depășește pragul stabilit.

### L338 Test pentru politica de arhivare
**Descriere tehnica:** Confirmă că batch-urile completate pot fi arhivate conform regulii.
**Scop:** Menține documentația organizată.
**Target:** Archive policy test.
**Prompt AI:** Adaugă un test pentru criteriile de arhivare ale batch-urilor.
**Acceptare:** Doar batch-urile eligibile pot fi arhivate.

### L339 Test pentru regula de release note
**Descriere tehnica:** Verifică prezența elementelor obligatorii în notele de release.
**Scop:** Păstrează istoricul clar.
**Target:** Release note test.
**Prompt AI:** Creează un test pentru șablonul de note de release.
**Acceptare:** Nota conține schimbarea, motivul și impactul.

### L340 Test pentru limba și terminologia
**Descriere tehnica:** Detectează variațiile nedorite de terminologie în taskuri.
**Scop:** Menține consistența editorială.
**Target:** Terminology test, docs lint.
**Prompt AI:** Adaugă un test care detectează termeni inconsistenți în batch.
**Acceptare:** Termenii standard rămân stabili.

## Închidere și continuitate

### L341 Notă pentru continuitatea seriei
**Descriere tehnica:** Leagă explicit batch-ul curent de următorul interval numeric.
**Scop:** Face seria ușor de continuat.
**Target:** Docs header, continuity note.
**Prompt AI:** Scrie o notă scurtă care pregătește următorul batch DeepSeek.
**Acceptare:** Cititorul poate continua seria fără căutări suplimentare.

### L342 Notă pentru conservarea regulilor
**Descriere tehnica:** Rezumă regulile care trebuie păstrate în batch-urile următoare.
**Scop:** Evită degradarea formatului.
**Target:** Governance note, docs style.
**Prompt AI:** Adaugă o notă care enumeră regulile de format ce nu trebuie încălcate.
**Acceptare:** Reguli esențiale sunt vizibile într-un singur loc.

### L343 Notă pentru schimbări viitoare
**Descriere tehnica:** Marchează zonele unde următorul batch poate adăuga noi teme.
**Scop:** Ghidează extinderea fără improvizație.
**Target:** Planning note, roadmap.
**Prompt AI:** Creează o notă scurtă despre ce teme pot fi extinse în batch-ul următor.
**Acceptare:** Direcția viitoare este explicită.

### L344 Notă pentru păstrarea compactă
**Descriere tehnica:** Confirmă regula de a menține batch-urile mici și clare.
**Scop:** Protejează ușurința de revizie.
**Target:** Batch policy, docs governance.
**Prompt AI:** Scrie o regulă finală pentru compactarea batch-urilor DeepSeek.
**Acceptare:** Batch-urile noi rămân mici și lizibile.

### L345 Notă pentru auditul complet
**Descriere tehnica:** Închide seria curentă cu un punct de control pentru audit.
**Scop:** Face seria verificabilă de la cap la cap.
**Target:** Audit note, docs summary.
**Prompt AI:** Adaugă o notă care marchează faptul că seria este pregătită pentru audit complet.
**Acceptare:** Seria are un punct final clar pentru verificare.

### L346 Notă pentru backlog stabil
**Descriere tehnica:** Separa taskurile deja clare de ideile încă nevalidate.
**Scop:** Menține stabilitatea backlog-ului.
**Target:** Backlog policy, classification.
**Prompt AI:** Creează o notă scurtă care delimitează taskurile stabile de cele speculative.
**Acceptare:** Clasificarea este clară pentru cititor.

### L347 Notă pentru taskuri viitoare
**Descriere tehnica:** Rezervă spațiu conceptual pentru extinderea seriei fără schimbări de format.
**Scop:** Păstrează consistența viitoarelor batch-uri.
**Target:** Docs continuity, future planning.
**Prompt AI:** Adaugă o notă scurtă pentru extensiile viitoare ale seriei.
**Acceptare:** Formatul existent rămâne compatibil cu batch-uri noi.

### L348 Notă pentru consum AI
**Descriere tehnica:** Definește cum trebuie citite aceste taskuri de către un model extern.
**Scop:** Reduce interpretările greșite.
**Target:** AI usage note, docs guide.
**Prompt AI:** Scrie o notă despre cum ar trebui consumată seria DeepSeek de către AI.
**Acceptare:** Citirea automată rămâne stabilă și sigură.

### L349 Notă pentru review uman
**Descriere tehnica:** Marchează ce trebuie verificat de un om înainte de folosirea seriei.
**Scop:** Protejează împotriva automatărilor greșite.
**Target:** Review note, safety policy.
**Prompt AI:** Adaugă o notă scurtă despre punctele care cer review uman.
**Acceptare:** Cititorul știe când trebuie aprobare umană.

### L350 Nota finală pentru batch-ul 6
**Descriere tehnica:** Rezumă extinderea L301-L350 și continuitatea cu batch-urile anterioare.
**Scop:** Închide seria curentă cu un rezumat canonic.
**Target:** Release note, docs index, changelog.
**Prompt AI:** Scrie o notă finală scurtă pentru batch-ul L301-L350.
**Acceptare:** Documentul marchează clar ce a fost adăugat și de ce.
