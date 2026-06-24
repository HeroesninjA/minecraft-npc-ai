# DeepSeek Taskuri - Batch 3 (L151-L200)

Actualizat: 2026-06-24

Acest document extinde backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Contract, schema si validare

### L151 Registru canonic pentru `scope`
**Descriere tehnica:** Muta valorile de scope intr-un registru central folosit de parser, validator si UI.
**Scop:** Elimina divergentele dintre listele duplicate de scope.
**Target:** Quest schema, parser YAML, helper UI.
**Prompt AI:** Creeaza un registru central pentru scope si foloseste-l in toate punctele care citesc sau afiseaza questuri.
**Acceptare:** Toate valorile de scope valide sunt rezolvate identic peste tot.

### L152 Warning pentru `scope` lipsa
**Descriere tehnica:** Marcheaza explicit questurile fara scope in validator.
**Scop:** Evita incarcarea tacita a definitiilor incomplete.
**Target:** Validator, raport de warnings, debug output.
**Prompt AI:** Adauga warning clar cand un quest nu are scope definit.
**Acceptare:** Questurile incomplete apar in raport, nu sunt ignorate silentios.

### L153 Warning pentru `target` lipsa
**Descriere tehnica:** Valideaza obiectivele care nu au target rezolvabil.
**Scop:** Blocheaza progresul fals generat de input incomplet.
**Target:** Objective parser, validator, log de incarcare.
**Prompt AI:** Daca un objective nu are target valid, afiseaza warning explicit la load.
**Acceptare:** Inputul incomplet produce warning si nu porneste progres fals.

### L154 Normalizare pentru `region_id`
**Descriere tehnica:** Uniformizeaza rezolvarea identitatii de regiune in parser si runtime.
**Scop:** Evita cazurile in care acelasi region este citit diferit de module diferite.
**Target:** Region resolver, quest loader, runtime checks.
**Prompt AI:** Extracteaza un helper comun pentru normalizarea si validarea `region_id`.
**Acceptare:** Aceeasi regiune se rezolva la fel in toate fluxurile.

### L155 Mesaje de eroare cu sursa YAML
**Descriere tehnica:** Include fisierul si linia in erorile de validare pentru questuri.
**Scop:** Reduce timpul necesar pentru debug.
**Target:** Parser, validator, raport text.
**Prompt AI:** Extinde mesajele de eroare cu path de fisier si pozitie in YAML.
**Acceptare:** Un quest invalid poate fi localizat direct din mesaj.

### L156 Audit read-only pentru obiective goale
**Descriere tehnica:** Adauga un audit care detecteaza obiectivele fara campuri utile.
**Scop:** Gaseste rapid definitiile inutile sau incomplete.
**Target:** Admin command, loader read-only, raport.
**Prompt AI:** Creeaza o comanda read-only care listeaza objective-urile goale sau aproape goale.
**Acceptare:** Adminul vede exact fisierele care trebuie curatate.

### L157 Test de regresie pentru scope alias
**Descriere tehnica:** Acopera aliasurile vechi pentru scope in parser si resolver.
**Scop:** Pastreaza compatibilitatea cu configuratii existente.
**Target:** Test suite, parser contract.
**Prompt AI:** Adauga test care verifica faptul ca aliasul vechi de scope ajunge la valoarea canonica.
**Acceptare:** Testul trece fara dependente runtime.

### L158 Test de regresie pentru target alias
**Descriere tehnica:** Valideaza normalizarea aliasurilor pentru target.
**Scop:** Previne ruptura intre YAML istoric si contractul nou.
**Target:** Parser tests, resolver tests.
**Prompt AI:** Creeaza test care confirma ca aliasul vechi de target se rezolva corect.
**Acceptare:** Aliasul si numele canonic produc acelasi rezultat.

### L159 Documentatie de schema cu exemple minime
**Descriere tehnica:** Adauga exemple scurte pentru campurile obligatorii si optionale.
**Scop:** Face contractul usor de urmat pentru AI si oameni.
**Target:** Documentatie schema, ghid de authoring.
**Prompt AI:** Scrie un exemplu minim si unul complet pentru un quest valid.
**Acceptare:** Documentatia arata clar ce este obligatoriu si ce este optional.

### L160 Changelog pentru extinderea contractului
**Descriere tehnica:** Noteaza schimbarile de schema si motivatia lor.
**Scop:** Pastreaza istoricul deciziilor usor de urmarit.
**Target:** CHANGELOG, note de release.
**Prompt AI:** Adauga o intrare scurta care explica registrul canonic si noile warnings.
**Acceptare:** Schimbarea apare in istoric cu motiv tehnic clar.

## Runtime si progres

### L161 Dispatch comun pentru progres de objective
**Descriere tehnica:** Unifica emiterea evenimentelor de progres pentru toate tipurile de objective.
**Scop:** Evita logica duplicata intre listeners diferiti.
**Target:** Runtime progress dispatcher, event bus.
**Prompt AI:** Creeaza un dispatcher comun care primeste eventul si actualizeaza objective-ul potrivit.
**Acceptare:** Toate obiectivele folosesc acelasi flux de progres.

### L162 Debounce pentru evenimente duplicate
**Descriere tehnica:** Blocheaza procesarea repetata a aceluiasi eveniment in interval scurt.
**Scop:** Previne cresterea dubla a progresului.
**Target:** Runtime, cache scurt, event listener.
**Prompt AI:** Adauga un mecanism simplu de debounce pentru evenimentele de progres duplicate.
**Acceptare:** Un singur eveniment nu avanseaza de doua ori acelasi objective.

### L163 Salvare atomica a progresului
**Descriere tehnica:** Scrie progresul intr-un mod care nu lasa stari partiale la crash.
**Scop:** Protejeaza datele active de corupere partiala.
**Target:** Persistence layer, save routine.
**Prompt AI:** Modifica salvarea progresului astfel incat sa fie atomica sau rollback-safe.
**Acceptare:** Un save intrerupt nu lasa date partial scrise.

### L164 Resume corect dupa restart
**Descriere tehnica:** Reconstruieste progresul activ din starea salvata si reporneste tracking-ul.
**Scop:** Pastreaza questurile active dupa restart de server.
**Target:** Load pipeline, runtime resume.
**Prompt AI:** Creeaza fluxul de resume pentru questurile active dupa restart.
**Acceptare:** Questul activ continua corect fara reset accidental.

### L165 Reset controlat pentru objective
**Descriere tehnica:** Adauga reset sigur pentru un objective sau pentru un stage intreg.
**Scop:** Permite adminului si sistemului sa refaca progresul fara restart complet.
**Target:** Runtime commands, state manager.
**Prompt AI:** Creeaza o cale de reset pentru progress state care pastreaza logica consistenta.
**Acceptare:** Resetul curata doar state-ul vizat.

### L166 Hook la stage complete
**Descriere tehnica:** Leaga finalizarea de stage de o actiune determinista in runtime.
**Scop:** Sincronizeaza progresul cu tranzitia de stage.
**Target:** Stage lifecycle, progress manager.
**Prompt AI:** Adauga un hook care executa actiunea necesara la finalizarea unui stage.
**Acceptare:** Trecerea de stage produce exact o tranzitie.

### L167 Telemetrie minima pentru interactiuni NPC
**Descriere tehnica:** Inregistreaza contor de interactiuni si rezultate pe NPC.
**Scop:** Ajuta la debugging fara logging excesiv.
**Target:** Runtime metrics, debug counters.
**Prompt AI:** Adauga o telemetrie minima pentru interactiunile NPC relevante.
**Acceptare:** Contorul poate fi inspectat fara a afecta gameplay-ul.

### L168 Snapshot de stare pentru quest activ
**Descriere tehnica:** Captureaza un snapshot scurt al starii active pentru debug.
**Scop:** Face reproductibil un bug raportat de utilizator.
**Target:** Debug command, state serializer.
**Prompt AI:** Creeaza un snapshot text scurt cu quest, stage, objectives si warnings.
**Acceptare:** Snapshotul poate fi folosit pentru investigatie fara alte surse.

### L169 Curatare pentru obiective orfane
**Descriere tehnica:** Detecteaza si curata obiectivele care nu mai au un owner valid.
**Scop:** Evita scurgerile de state dupa reset sau reload.
**Target:** Runtime cleanup, garbage collection logic.
**Prompt AI:** Adauga un cleanup care elimina obiectivele orfane din state-ul activ.
**Acceptare:** Nu raman obiective blocate dupa reset/reload.

### L170 Metrici simple pentru progres
**Descriere tehnica:** Expune numarul de questuri active, complete si esuate.
**Scop:** Ofera vizibilitate operationala rapida.
**Target:** Metrics endpoint, admin report.
**Prompt AI:** Adauga metrici simple pentru starea questurilor fara cost mare.
**Acceptare:** Metricile pot fi citite rapid in admin sau log.

## Admin si diagnostic

### L171 Comanda pentru sumar quest activ
**Descriere tehnica:** Afiseaza starea curenta a questului activ intr-un format compact.
**Scop:** Reduce investigatia manuala in productie.
**Target:** Admin command, console output.
**Prompt AI:** Creeaza o comanda care afiseaza questul activ, stage-ul si warnings relevante.
**Acceptare:** Rezumatul este suficient pentru triere rapida.

### L172 Comanda pentru raport de warnings
**Descriere tehnica:** Consolidarea warning-urilor intr-un raport separat.
**Scop:** Permite curatarea backlog-ului de erori de configurare.
**Target:** Audit command, warnings aggregator.
**Prompt AI:** Adauga un raport care grupeaza warnings pe fisier si tip.
**Acceptare:** Adminul vede rapid ce fisiere au probleme.

### L173 Dump debug pentru arborele de objective
**Descriere tehnica:** Exporta structura obiectivelor incarcate intr-un format stabil.
**Scop:** Ajuta la compararea dintre YAML si runtime.
**Target:** Debug dump, serializer.
**Prompt AI:** Creeaza un dump care afiseaza ierarhia obiectivelor si dependintele.
**Acceptare:** Structura poate fi citita fara acces la debugger.

### L174 Filtru debug pe player
**Descriere tehnica:** Restrange output-ul de diagnostic la un singur player.
**Scop:** Reduce zgomotul cand serverul are mai multi utilizatori.
**Target:** Admin tools, debug output.
**Prompt AI:** Adauga un filtru de debug care limiteaza raportarea la un player ales.
**Acceptare:** Output-ul este relevant doar pentru tinta selectata.

### L175 Export de diagnostic in JSON
**Descriere tehnica:** Serializeaza datele de debug intr-un format usor de procesat.
**Scop:** Permite analizarea automata a problemei.
**Target:** Export command, JSON serializer.
**Prompt AI:** Creeaza export JSON pentru quest, stage, warnings si progress.
**Acceptare:** Fisierul exportat poate fi citit de un tool extern.

### L176 Comparatie intre YAML si runtime
**Descriere tehnica:** Compara ceea ce s-a incarcat cu sursa originala.
**Scop:** Detecteaza transformari nedorite in pipeline.
**Target:** Audit tool, diff generator.
**Prompt AI:** Adauga un audit care compara questul din YAML cu reprezentarea runtime.
**Acceptare:** Diferentele apar clar in raport.

### L177 Detectie pentru cache de quest invechit
**Descriere tehnica:** Marcheaza cache-ul care nu mai corespunde fisierului sursa.
**Scop:** Evita debug pe date depasite.
**Target:** Cache manager, reload path.
**Prompt AI:** Adauga verificare pentru cache stale si raportare explicita.
**Acceptare:** Cache-ul invechit este recunoscut inainte de folosire.

### L178 Reload sigur pentru un singur quest
**Descriere tehnica:** Reincarca o definitie izolata fara sa afecteze restul runtime-ului.
**Scop:** Simplifica iteratia rapida pe configuratii.
**Target:** Hot reload, file watcher, state manager.
**Prompt AI:** Creeaza reload pentru un singur quest cu protectie la erori de load.
**Acceptare:** Reload-ul nu dubleaza listeners si nu opreste alte questuri.

### L179 Protectie la reload dublu
**Descriere tehnica:** Previne inregistrarea duplicata a aceluiasi listener la reloading.
**Scop:** Evita progresul multiplu din aceeasi cauza.
**Target:** Listener registry, reload lifecycle.
**Prompt AI:** Adauga o protectie care impiedica listener-ele duplicate dupa reload.
**Acceptare:** Reload-ul repetat pastreaza un singur listener activ.

### L180 Agregare pentru motivele de esec
**Descriere tehnica:** Grupeaza cauzele de fail pentru obiective si stadii.
**Scop:** Ajuta la trierea cauzelor frecvente.
**Target:** Failure tracker, admin report.
**Prompt AI:** Creeaza un raport scurt care grupeaza motivele de esec pe categorie.
**Acceptare:** Cauzele recurente sunt vizibile imediat.

## Persistenta si migrare

### L181 Migrator pentru nume vechi de enum
**Descriere tehnica:** Converteaste valorile istorice la numele canonice la incarcare.
**Scop:** Pastreaza compatibilitatea cu date vechi.
**Target:** Migration layer, loader.
**Prompt AI:** Adauga un migrator mic pentru enum-urile vechi folosite in questuri.
**Acceptare:** Datele vechi se incarca fara eroare si fara pierdere de sens.

### L182 Backup pentru configuratii de quest
**Descriere tehnica:** Creeaza o copie de siguranta a fisierelor de quest inainte de schimbari mari.
**Scop:** Reduce riscul de pierdere a configuratiilor.
**Target:** Backup tool, file system helper.
**Prompt AI:** Adauga o comanda care face backup la configuratiile de quest.
**Acceptare:** Backup-ul poate fi restaurat integral.

### L183 Tool de diff pentru questuri
**Descriere tehnica:** Arata diferentele dintre doua versiuni ale aceluiasi quest.
**Scop:** Simplifica review-ul de configuratii.
**Target:** CLI tool, diff output.
**Prompt AI:** Creeaza un diff text pentru doua fisiere de quest sau doua snapshot-uri.
**Acceptare:** Diferentele importante sunt usor de citit.

### L184 Deduplicare pentru blocuri comune
**Descriere tehnica:** Extrage sectiunile comune intr-un fragment reutilizabil.
**Scop:** Reduce duplicarea intre questuri similare.
**Target:** Schema reuse, loader, template helper.
**Prompt AI:** Identifica o sectiune repetata si extrage-o intr-un helper sau template.
**Acceptare:** Doua questuri similare folosesc acelasi fragment comun.

### L185 Bump pentru versiunea de schema
**Descriere tehnica:** Marcheaza explicit schimbarea contractului in metadata.
**Scop:** Face migrarea usor de urmarit.
**Target:** Schema metadata, loader compatibility.
**Prompt AI:** Adauga versiune de schema si foloseste-o in logica de incarcare.
**Acceptare:** Versiunea este vizibila si poate ghida migrarile.

### L186 Compatibilitate pentru campuri optionale
**Descriere tehnica:** Pastreaza functionalitatea cand campurile noi lipsesc.
**Scop:** Evita rupturi pentru questuri vechi.
**Target:** Parser, fallback defaults.
**Prompt AI:** Adauga fallback-uri clare pentru campurile optionale nou introduse.
**Acceptare:** Questurile vechi se incarca fara warnings false.

### L187 Fallback pentru metadata lipsa
**Descriere tehnica:** Completeaza metadata minima cand fisierul nu o defineste.
**Scop:** Pastreaza operarea in mod tolerant.
**Target:** Loader, metadata builder.
**Prompt AI:** Creeaza un fallback pentru metadata lipsa care nu mascheaza erorile reale.
**Acceptare:** Metadata minima exista, dar inputul invalid ramane vizibil.

### L188 Regenerare pentru indexul de questuri
**Descriere tehnica:** Reface indexul cand fisierele sursa s-au schimbat.
**Scop:** Evita inconsistentele intre index si continut.
**Target:** Indexer, cache invalidation.
**Prompt AI:** Adauga o comanda care regenereaza indexul questurilor din surse.
**Acceptare:** Indexul nou reflecta complet sursele curente.

### L189 Curatare pentru intrari invalide din cache
**Descriere tehnica:** Sterge inregistrarile care nu mai au corespondent pe disk.
**Scop:** Mentine cache-ul curat si predictibil.
**Target:** Cache cleanup, index maintenance.
**Prompt AI:** Creeaza o curatare pentru intrarile de cache fara fisier sursa valid.
**Acceptare:** Cache-ul nu contine referinte moarte.

### L190 Checksum per quest
**Descriere tehnica:** Asociaza fiecare quest cu un checksum al sursei.
**Scop:** Detecteaza modificari reale si elimina reload-urile inutile.
**Target:** Loader, cache validator.
**Prompt AI:** Adauga checksum pentru quest si foloseste-l la detectia schimbarilor.
**Acceptare:** Schimbarea sursei produce o diferenta masurabila si re-incarcare corecta.

## Teste, hardening si livrare

### L191 Test unit pentru validatorul de scope
**Descriere tehnica:** Acopera regulile noi pentru scope intr-un test rapid.
**Scop:** Blocheaza regresiile de schema.
**Target:** Unit test, validator rules.
**Prompt AI:** Adauga test care verifica acceptarea si respingerea valorilor de scope.
**Acceptare:** Testul esueaza pentru input invalid si trece pentru input valid.

### L192 Test unit pentru resolverul de target
**Descriere tehnica:** Valideaza logica de mapare a target-ului canonic.
**Scop:** Protejeaza normalizarea introdusa la load.
**Target:** Resolver test, parser contract.
**Prompt AI:** Creeaza test pentru rezolvarea target-ului din sirurile istorice si canonice.
**Acceptare:** Rezultatul este identic pentru formele acceptate.

### L193 Test de integrare load + progress
**Descriere tehnica:** Ruleaza un flux complet: load quest, porneste progres, confirma avansare.
**Scop:** Demonstreaza ca runtime si parserul functioneaza impreuna.
**Target:** Integration test, quest runtime.
**Prompt AI:** Adauga un test care incarca un quest si verifica progresul de la eveniment la state.
**Acceptare:** Fluxul complet trece fara interventie manuala.

### L194 Test de integrare save + resume
**Descriere tehnica:** Verifica faptul ca progresul salvat este restaurat dupa restart.
**Scop:** Protejeaza persistenta dintre sesiuni.
**Target:** Persistence integration test.
**Prompt AI:** Creeaza un test care salveaza progressul si confirma resume corect dupa reload.
**Acceptare:** State-ul final coincide cu state-ul salvat initial.

### L195 Test pentru output-ul raportului de warnings
**Descriere tehnica:** Asigura formatul stabil al raportului de diagnostic.
**Scop:** Evita regresiile de UX in consola sau admin.
**Target:** Command output test.
**Prompt AI:** Adauga test care verifica formatul si ordinea raportului de warnings.
**Acceptare:** Output-ul ramane stabil si lizibil.

### L196 Test pentru reload fara listeners duplicati
**Descriere tehnica:** Protejeaza impotriva inregistrarilor multiple la reincarcare.
**Scop:** Previne progresul dublu si leak-urile de evenimente.
**Target:** Listener lifecycle test.
**Prompt AI:** Creeaza test care confirma ca reload-ul nu dubleaza listener-ele.
**Acceptare:** Numarul de listener-e ramane constant dupa reload.

### L197 Test pentru warnings la YAML invalid
**Descriere tehnica:** Acopera cazurile in care schema primeste input gresit.
**Scop:** Pastreaza mesajele de eroare utile si predictibile.
**Target:** Parser warning test.
**Prompt AI:** Adauga test care verifica warning-urile pentru campuri invalide sau lipsa.
**Acceptare:** Warnings apar cu mesajul asteptat.

### L198 Test pentru exportul JSON de diagnostic
**Descriere tehnica:** Asigura structura exportului pentru consum extern.
**Scop:** Permite parsarea stabila de catre alte unelte.
**Target:** JSON export test.
**Prompt AI:** Creeaza test pentru structura exportului JSON si campurile obligatorii.
**Acceptare:** Exportul contine campurile asteptate si este valid JSON.

### L199 Smoke checklist pentru batch-ul nou
**Descriere tehnica:** Stabileste verificari scurte pentru un release sigur.
**Scop:** Reduce riscul de livrare cu regresii evidente.
**Target:** Release checklist, manual QA.
**Prompt AI:** Scrie o lista scurta de smoke checks pentru taskurile din acest batch.
**Acceptare:** Checklist-ul poate fi rulat rapid dupa schimbari.

### L200 Note de release pentru batch-ul nou
**Descriere tehnica:** Rezuma impactul functional si tehnic al extinderii backlog-ului.
**Scop:** Pastreaza istoricul usor de urmarit pentru urmatorul AI sau maintainer.
**Target:** Release note, changelog, docs index.
**Prompt AI:** Adauga o nota scurta care rezuma ce s-a schimbat in L151-L200.
**Acceptare:** Documentatia explica clar ce a intrat in batch si de ce.
