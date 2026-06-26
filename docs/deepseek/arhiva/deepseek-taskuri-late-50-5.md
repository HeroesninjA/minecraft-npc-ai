# DeepSeek Taskuri - Batch 7 (L351-L400)

Actualizat: 2026-06-24

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Audit și guvernare

### L351 Registry pentru reguli canonice
**Descriere tehnica:** Centralizează regulile canonice într-un registry ușor de audit și referențiat.
**Scop:** Evită duplicarea regulilor în mai multe documente.
**Target:** Docs registry, governance index.
**Prompt AI:** Creează un registry central pentru regulile canonice ale seriei DeepSeek.
**Acceptare:** Regulele pot fi găsite dintr-un singur loc.

### L352 Guard pentru reguli duplicate
**Descriere tehnica:** Detectează reguli identice sau aproape identice în mai multe batch-uri.
**Scop:** Reduce redundanța.
**Target:** Docs audit, duplicate detector.
**Prompt AI:** Adaugă un checker care identifică regulile duplicate între batch-uri.
**Acceptare:** Duplicatele sunt raportate cu referință la ambele locații.

### L353 Guard pentru reguli contradictorii
**Descriere tehnica:** Semnalează instrucțiunile care se contrazic între batch-uri.
**Scop:** Menține coerența backlog-ului.
**Target:** Conflict audit, docs lint.
**Prompt AI:** Creează un audit pentru regulile care se bat cap în cap.
**Acceptare:** Orice contradicție este listată explicit.

### L354 Guard pentru reguli prea vagi
**Descriere tehnica:** Marchează regulile care nu au suficientă formă operațională.
**Scop:** Păstrează instrucțiunile acționabile.
**Target:** Rule lint, quality audit.
**Prompt AI:** Adaugă un checker pentru regulile formulate prea vag.
**Acceptare:** Regulile vagi sunt semnalate pentru rescriere.

### L355 Guard pentru reguli prea lungi
**Descriere tehnica:** Identifică regulile care depășesc un prag de lungime util.
**Scop:** Evită supraîncărcarea documentului.
**Target:** Docs lint, length guard.
**Prompt AI:** Creează un guard pentru regulile prea lungi.
**Acceptare:** Intrările excesive sunt raportate.

### L356 Guard pentru lipsă de scop
**Descriere tehnica:** Verifică dacă regula explică efectul sau motivul ei.
**Scop:** Asigură contextul minim.
**Target:** Rule metadata, validation.
**Prompt AI:** Adaugă un checker pentru regulile care nu explică scopul lor.
**Acceptare:** Regulile fără motiv clar sunt marcate.

### L357 Guard pentru lipsă de target
**Descriere tehnica:** Confirmă că fiecare regulă menționează ce zonă afectează.
**Scop:** Ghidează review-ul.
**Target:** Rule metadata, target lint.
**Prompt AI:** Creează un audit pentru regulile fără target tehnic.
**Acceptare:** Lipsa targetului produce warning.

### L358 Guard pentru exemple nesigure
**Descriere tehnica:** Detectează exemplele care includ comportament riscant sau neacoperit.
**Scop:** Protejează documentația de sugestii periculoase.
**Target:** Safety audit, example checker.
**Prompt AI:** Adaugă o verificare pentru exemplele considerate nesigure.
**Acceptare:** Exemplele riscante sunt marcate explicit.

### L359 Guard pentru termeni interziși
**Descriere tehnica:** Semnalează folosirea termenilor care sugerează efecte automate necontrolate.
**Scop:** Păstrează limbajul conform cu politica de control.
**Target:** Terminology guard, safety lint.
**Prompt AI:** Creează un detector pentru termeni care implică execuție necontrolată.
**Acceptare:** Termenii interziși sunt raportați.

### L360 Guard pentru format corupt
**Descriere tehnica:** Verifică dacă markup-ul sau delimitatorii documentului au fost rupți.
**Scop:** Protejează randarea și parsarea.
**Target:** Markdown integrity, docs writer.
**Prompt AI:** Adaugă un guard care detectează formatul corupt.
**Acceptare:** Documentul invalid este semnalat înainte de publicare.

## Prompting și consum AI

### L361 Prompt minimal pentru audit
**Descriere tehnica:** Definește un prompt scurt și strict pentru taskuri read-only.
**Scop:** Reduce riscul de ieșiri în afara scopului.
**Target:** Prompt template, audit profile.
**Prompt AI:** Creează un prompt minimal pentru taskuri de audit și verificare.
**Acceptare:** Promptul cere doar inspectare și raportare.

### L362 Prompt minimal pentru documentare
**Descriere tehnica:** Definește un prompt scurt pentru taskuri care produc doar text explicativ.
**Scop:** Menține rezultatele compacte.
**Target:** Prompt template, docs profile.
**Prompt AI:** Creează un prompt minimal pentru taskurile de documentare.
**Acceptare:** Promptul produce output concentrat pe documentație.

### L363 Prompt minimal pentru validare
**Descriere tehnica:** Separă taskurile de verificare de taskurile de generare.
**Scop:** Clarifică autoritatea modelului.
**Target:** Prompt template, validation profile.
**Prompt AI:** Creează un prompt minimal pentru taskuri de validare și control.
**Acceptare:** Promptul nu solicită patch-uri sau schimbări de stare.

### L364 Prompt minimal pentru rezumat
**Descriere tehnica:** Creează o formă scurtă pentru rezumatele batch-urilor.
**Scop:** Evită rezumatele lungi și puțin utile.
**Target:** Summary template, docs helper.
**Prompt AI:** Creează un prompt minimal pentru generarea de rezumate scurte.
**Acceptare:** Rezultatul rămâne foarte concis.

### L365 Prompt minimal pentru cleanup
**Descriere tehnica:** Standardizează promptul folosit pentru taskuri de curățare și ordonare.
**Scop:** Reduce riscul de rescriere accidentală.
**Target:** Cleanup template, docs maintenance.
**Prompt AI:** Creează un prompt minimal pentru cleanup editorial.
**Acceptare:** Promptul cere doar reorganizare și claritate, nu conținut nou.

### L366 Prompt minimal pentru fallback
**Descriere tehnica:** Definește cerința de degradare sigură în promptul pentru fallback-uri.
**Scop:** Protejează runtime-ul și documentația.
**Target:** Safety prompt, fallback profile.
**Prompt AI:** Creează un prompt minimal pentru taskurile care specifică fallback.
**Acceptare:** Promptul solicită explicit degradare sigură.

### L367 Prompt minimal pentru approval
**Descriere tehnica:** Include cerința de aprobare manuală în taskurile sensibile.
**Scop:** Menține controlul uman.
**Target:** Approval prompt, safety profile.
**Prompt AI:** Creează un prompt minimal pentru taskuri care cer aprobare manuală.
**Acceptare:** Promptul nu lasă taskul să ruleze automat.

### L368 Prompt minimal pentru arhivare
**Descriere tehnica:** Definește cum se cere arhivarea unui batch în mod clar.
**Scop:** Ajută la întreținerea backlog-ului.
**Target:** Archive prompt, docs maintenance.
**Prompt AI:** Creează un prompt minimal pentru arhivarea batch-urilor finalizate.
**Acceptare:** Promptul solicită doar mutarea și rezumatul necesar.

### L369 Prompt minimal pentru index
**Descriere tehnica:** Standardizează generația indexului de batch-uri.
**Scop:** Face navigarea stabilă.
**Target:** Index prompt, docs generator.
**Prompt AI:** Creează un prompt minimal pentru actualizarea indexului de batch-uri.
**Acceptare:** Promptul cere doar actualizarea listei și a linkurilor.

### L370 Prompt minimal pentru review uman
**Descriere tehnica:** Specifică că outputul trebuie pregătit pentru citire de către om înainte de aplicare.
**Scop:** Evită aplicarea directă a sugestiilor sensibile.
**Target:** Review prompt, safety profile.
**Prompt AI:** Creează un prompt minimal pentru taskurile care cer review uman.
**Acceptare:** Outputul este clar, complet și ușor de inspectat.

## Verificare și siguranță

### L371 Test pentru registry-ul regulilor
**Descriere tehnica:** Verifică prezența tuturor regulilor canonice în registry.
**Scop:** Protejează guvernanța documentației.
**Target:** Registry test, governance.
**Prompt AI:** Adaugă un test care confirmă că registry-ul include toate regulile.
**Acceptare:** Testul eșuează dacă lipsește o regulă.

### L372 Test pentru duplicarea regulilor
**Descriere tehnica:** Acoperă identificarea regulilor duplicate.
**Scop:** Reduce redundanța.
**Target:** Duplicate audit test.
**Prompt AI:** Creează un test pentru regulile duplicate în batch-uri diferite.
**Acceptare:** Duplicatul produce warning clar.

### L373 Test pentru contradicții
**Descriere tehnica:** Confirmă că regulile contradictorii sunt detectate.
**Scop:** Menține coerența.
**Target:** Conflict detector test.
**Prompt AI:** Adaugă un test pentru detectarea regulilor care se contrazic.
**Acceptare:** Conflictul este raportat.

### L374 Test pentru lungime excesivă
**Descriere tehnica:** Verifică pragul de lungime pentru reguli și prompturi.
**Scop:** Protejează lizibilitatea.
**Target:** Length guard test.
**Prompt AI:** Creează un test pentru regulile prea lungi.
**Acceptare:** Elementele excesive sunt marcate.

### L375 Test pentru lipsa scopului
**Descriere tehnica:** Confirmă că lipsa scopului produce warning.
**Scop:** Păstrează taskurile acționabile.
**Target:** Scope lint test.
**Prompt AI:** Adaugă un test pentru regulile fără scop clar.
**Acceptare:** Warning-ul apare predictibil.

### L376 Test pentru lipsa targetului
**Descriere tehnica:** Validează că lipsa targetului nu trece neobservată.
**Scop:** Protejează navigarea tehnică.
**Target:** Target lint test.
**Prompt AI:** Creează un test pentru regulile fără target.
**Acceptare:** Lipsa targetului produce eșec sau warning clar.

### L377 Test pentru format corupt
**Descriere tehnica:** Acoperă markup-ul invalid și delimitatorii rupți.
**Scop:** Protejează randarea documentului.
**Target:** Markdown integrity test.
**Prompt AI:** Adaugă un test pentru detectarea formatului corupt.
**Acceptare:** Documentul invalid este marcat.

### L378 Test pentru prompt minim audit
**Descriere tehnica:** Verifică faptul că promptul de audit rămâne read-only.
**Scop:** Previne devierea de la scop.
**Target:** Prompt template test.
**Prompt AI:** Creează un test pentru promptul minim folosit la audit.
**Acceptare:** Promptul nu cere acțiuni destructive.

### L379 Test pentru prompt minim fallback
**Descriere tehnica:** Confirmă că promptul de fallback menționează degradarea sigură.
**Scop:** Menține consistența semantică.
**Target:** Safety prompt test.
**Prompt AI:** Adaugă un test pentru promptul de fallback.
**Acceptare:** Fallback-ul este menționat explicit.

### L380 Test pentru prompt minim approval
**Descriere tehnica:** Verifică faptul că promptul pentru taskuri sensibile cere aprobare manuală.
**Scop:** Protejează execuția.
**Target:** Approval prompt test.
**Prompt AI:** Creează un test pentru promptul de aprobare manuală.
**Acceptare:** Taskul nu pare automatizabil fără confirmare.

## Continuitate

### L381 Notă pentru batch-ul anterior
**Descriere tehnica:** Leagă explicit acest batch de intervalul numeric anterior.
**Scop:** Păstrează continuitatea seriei.
**Target:** Docs continuity, header note.
**Prompt AI:** Scrie o notă scurtă care referă batch-ul anterior.
**Acceptare:** Cititorul poate naviga înapoi fără efort.

### L382 Notă pentru batch-ul următor
**Descriere tehnica:** Pregătește legătura spre următorul batch numeric.
**Scop:** Face seria ușor de extins.
**Target:** Docs continuity, forward link.
**Prompt AI:** Adaugă o notă care pregătește următorul batch DeepSeek.
**Acceptare:** Seria rămâne continuabilă.

### L383 Notă pentru mixul de teme
**Descriere tehnica:** Explică faptul că batch-ul conține teme de guvernanță, prompturi și verificare.
**Scop:** Ghidează citirea rapidă.
**Target:** Batch summary, docs overview.
**Prompt AI:** Creează un rezumat al temelor din batch-ul curent.
**Acceptare:** Cititorul înțelege ce acoperă batch-ul.

### L384 Notă pentru stabilitate
**Descriere tehnica:** Reafirmă că formatul trebuie păstrat stabil pentru batch-urile următoare.
**Scop:** Evită variațiile inutile.
**Target:** Docs policy, maintenance note.
**Prompt AI:** Adaugă o notă scurtă despre stabilitatea formatului.
**Acceptare:** Formatul este prezentat ca invariant.

### L385 Notă pentru separarea rolurilor
**Descriere tehnica:** Clarifică distincția între reguli, prompturi, teste și note.
**Scop:** Simplifică review-ul.
**Target:** Governance note, docs style.
**Prompt AI:** Scrie o notă despre separarea rolurilor în documentul batch.
**Acceptare:** Fiecare tip de conținut este distinct.

### L386 Notă pentru taskurile viitoare
**Descriere tehnica:** Definește că taskurile următoare trebuie să continue modelul de granularitate mică.
**Scop:** Protejează calitatea backlog-ului.
**Target:** Planning note, future work.
**Prompt AI:** Adaugă o notă care cere taskuri scurte și verificabile în continuare.
**Acceptare:** Următorul batch poate urma aceeași regulă.

### L387 Notă pentru output safe
**Descriere tehnica:** Reamintește că output-ul generat nu trebuie să includă secrete sau payload brut sensibil.
**Scop:** Menține alinierea la politica de siguranță.
**Target:** Safety note, prompt guide.
**Prompt AI:** Scrie o notă scurtă despre output safe și redactare.
**Acceptare:** Notele de lucru menționează clar fără secrete.

### L388 Notă pentru audit read-only
**Descriere tehnica:** Marchează taskurile de audit ca fiind fără efecte secundare.
**Scop:** Evită confundarea auditului cu patch-ul.
**Target:** Audit note, task taxonomy.
**Prompt AI:** Adaugă o notă care explică natura read-only a taskurilor de audit.
**Acceptare:** Auditul este prezentat ca verificare, nu modificare.

### L389 Notă pentru approval gate
**Descriere tehnica:** Clarifică faptul că taskurile sensibile trec prin aprobare umană.
**Scop:** Protejează schimbările cu impact mare.
**Target:** Approval note, safety policy.
**Prompt AI:** Scrie o notă despre gate-ul de aprobare pentru taskurile sensibile.
**Acceptare:** Cerința de aprobare este explicită.

### L390 Notă pentru arhivare
**Descriere tehnica:** Precizează cum trebuie păstrată arhiva batch-urilor.
**Scop:** Asigură traseul istoric.
**Target:** Archive note, docs lifecycle.
**Prompt AI:** Adaugă o notă scurtă pentru arhivarea batch-urilor finalizate.
**Acceptare:** Arhiva este menționată ca sursă de istoric.

### L391 Notă pentru indexarea automată
**Descriere tehnica:** Reamintește că indexul trebuie menținut la zi automat sau prin proces clar.
**Scop:** Evită linkuri lipsă.
**Target:** Index note, docs tooling.
**Prompt AI:** Scrie o notă despre actualizarea indexului după batch nou.
**Acceptare:** Indexarea este prezentată ca pas obligatoriu.

### L392 Notă pentru verificări finale
**Descriere tehnica:** Indică faptul că fiecare batch trebuie verificat la număr și structură.
**Scop:** Reduce erorile de publicare.
**Target:** Final check note, docs QA.
**Prompt AI:** Adaugă o notă pentru verificările finale înainte de publicare.
**Acceptare:** Binele de publicare include structură și numerotare.

### L393 Notă pentru compactare
**Descriere tehnica:** Reafirmă regula de a păstra batch-ul compact și clar.
**Scop:** Susține citirea eficientă de către AI și om.
**Target:** Batch policy, docs governance.
**Prompt AI:** Scrie o notă scurtă despre compactarea batch-urilor.
**Acceptare:** Batch-urile rămân în limite rezonabile.

### L394 Notă pentru disciplină editorială
**Descriere tehnica:** Definește așteptarea de consistență în ton și structură.
**Scop:** Păstrează seria ușor de întreținut.
**Target:** Editorial policy, docs style.
**Prompt AI:** Adaugă o notă pentru disciplina editorială a seriei DeepSeek.
**Acceptare:** Tonul și structura rămân consistente.

### L395 Notă pentru continuitatea batch-urilor
**Descriere tehnica:** Leagă în mod explicit seriea curentă de seria care urmează.
**Scop:** Evită ruperea contextului.
**Target:** Continuity note, roadmap.
**Prompt AI:** Creează o notă care explică legătura dintre batch-uri.
**Acceptare:** Cititorul vede continuitatea fără ambiguitate.

### L396 Notă pentru prioritate
**Descriere tehnica:** Clarifică faptul că batch-ul este ghidat de siguranță și verificabilitate.
**Scop:** Protejează ordinea de lucru.
**Target:** Priority note, planning.
**Prompt AI:** Adaugă o notă care explică prioritatea siguranței și a verificării.
**Acceptare:** Prioritatea este explicită și stabilă.

### L397 Notă pentru referințe canonice
**Descriere tehnica:** Menționează că documentul trebuie citit împreună cu indexul și constituția.
**Scop:** Întărește ghidarea către surse canonice.
**Target:** Canonical docs note, README.
**Prompt AI:** Scrie o notă despre citirea batch-ului alături de documentele canonice.
**Acceptare:** Legătura cu documentele canonice este explicită.

### L398 Notă pentru intervenție umană
**Descriere tehnica:** Precizează ce tipuri de taskuri cer intervenție umană înainte de aplicare.
**Scop:** Menține controlul asupra schimbărilor sensibile.
**Target:** Human review note, safety policy.
**Prompt AI:** Adaugă o notă despre taskurile care trebuie revizuite de un om.
**Acceptare:** Cazurile sensibile sunt clar identificate.

### L399 Notă pentru pregătirea următorului ciclu
**Descriere tehnica:** Marchează trecerea spre următorul ciclu numeric al seriei.
**Scop:** Face extinderea ulterioară simplă.
**Target:** Continuity note, next batch.
**Prompt AI:** Scrie o notă finală care pregătește următorul ciclu DeepSeek.
**Acceptare:** Seria poate continua fără schimbare de format.

### L400 Nota finală pentru batch-ul 7
**Descriere tehnica:** Rezumă extinderea L351-L400 și relația cu batch-urile anterioare.
**Scop:** Închide seria curentă cu un rezumat canonic.
**Target:** Release note, docs index, changelog.
**Prompt AI:** Scrie o notă finală scurtă pentru batch-ul L351-L400.
**Acceptare:** Documentul marchează clar ce a fost adăugat și de ce.
