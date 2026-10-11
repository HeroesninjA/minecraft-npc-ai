# Plan de remediere: generarea satului și rutinele NPC-urilor

Actualizat: 2026-10-02.
Status: în execuție; protecția preventivă A07, propagarea eșecului de persistență A08 și separarea proprietarului de rezidenți A12 sunt implementate local. Etapele complete rămân deschise. Bifarea unui pas cere dovezi de verificare, nu doar existența claselor.

## Obiectiv

Într-o lume aleasă explicit, prima inițializare trebuie să poată construi un sat coerent: clădiri fizice, mapping exact, gospodării și proprietari stabili, NPC-uri cu locuință și ocupație, apoi rutine funcționale. Restartul sau reluarea unei operații nu trebuie să dubleze satul, să schimbe proprietarii sau să piardă asocieri.

Planul tratează atât generarea din template-uri, cât și importul satelor existente. Cele două intrări converg către același model validat, dar importul rămâne fără construcții fizice implicite.

Diagnostic: [analizele A01–A17 și blocajele anterioare](../reference/mecanici-mcp-simulare.md#analize-suplimentare-construcție-gospodării-și-rutine).

## Reguli de execuție

- Păstrează implementările existente de mapping, `HouseAllocation`, spawn și binding; repară integrarea, fără un al doilea stack paralel.
- Nu trata DTO-urile/scaffold-urile de plan drept executor funcțional și nu reactiva implicit API-uri depreciate.
- Respectă read-only, confirmările administrative și separarea dintre preview și mutație.
- Prima inițializare automată este opt-in, autorizată prin configurarea lumii și a regiunii țintă; nu construiește în toate lumile găsite.
- Orice migrare începe cu backup și raport dry-run. Înregistrările deja pierdute prin coliziuni nu pot fi reconstruite sigur fără alte dovezi sau backup.
- Baza de date și lumea de blocuri nu formează o singură tranzacție SQL. Pentru operația completă sunt necesare jurnal, rezultate persistente și compensare controlată.
- Nu activa rutinele pentru un NPC incomplet configurat doar pentru a face demonstrația să pară funcțională.

## Ordinea pașilor

| Pas | Rezultat urmărit | Dependențe |
| --- | --- | --- |
| 1 | Baseline, backup și cazuri de regresie | Niciuna |
| 2 | Gospodării și proprietari fără pierderi de date | 1 |
| 3 | Un contract comun pentru template-uri | 1; protejarea fișierelor se poate face în paralel cu 2 |
| 4 | Mapping corect pentru case, paturi și ancore | 3; partea de scanare poate începe după 1 |
| 5 | Populație și ocupații repartizate coerent | 2, 4 |
| 6 | Construcție fizică verificată și recuperabilă | 2, 3, 4 |
| 7 | Inițializare completă, reluabilă și fără dubluri | 5, 6 |
| 8 | Rutine sigure și dependente de nevoi | 5; integrarea completă după 7 |
| 9 | Profil de sat cu clădirile și rolurile dorite | 3–8 |
| 10 | Validare end-to-end și gate de activare | 1–9 |

## Pasul 1 — Protejarea stării și baseline de test

Prioritate: critică înaintea mutațiilor de date sau lume.

- [x] Inventariază modificările locale și identifică versiunea de surse/JAR testată.
- [ ] Salvează separat baza de date, lumea țintă, configurațiile, mapping-ul, template-urile personalizate și contextul MCP; verifică restaurarea într-un mediu izolat.
- [ ] Pregătește o lume de test și o bază temporară, fără a folosi datele reale pentru probe distructive.
- [ ] Transformă probele auditului în teste de regresie în suitele existente: două sate cu `house_1`, proprietar cu doi rezidenți, import repetat, scanare → planner, meserii și tranziția somn → muncă.
- [ ] Înregistrează comportamentul actual și rezultatele așteptate; nu schimba un test ca să accepte defectul.

Acceptare: backup restaurabil, fixture reproductibil și defecte reproduse pe build-ul obținut din sursele curente. Probele anterioare pe JAR-ul existent nu înlocuiesc acest baseline.

## Pasul 2 — Identitate, proprietate și persistență

Acoperă: A07, A08, A12.

- [ ] Definește identități stabile și unice în proiect pentru regiune, casă, gospodărie și rezident. Include scope-ul regiunii/lumii unde este necesar; nu deriva familia doar din `house_1` și nu te baza pe normalizări care pot produce aceeași cheie.
- [ ] Separă identitatea familiei de identitatea gospodăriei sau validează explicit relația dintre ele. O coliziune cu altă casă trebuie respinsă, nu rezolvată prin suprascriere.
- [ ] Pregătește migrarea printr-un tabel explicit de corespondențe pentru household-uri, residents, world bindings și istoricul operațiilor. Oprește cazurile ambigue pentru verificare manuală.
- [x] Separă adăugarea unui rezident de atribuirea proprietarului. Rezolvă `primaryOwnerNpcKey` la NPC-ul creat/reutilizat și atribuie proprietatea într-o etapă distinctă, după binding-ul rezidenților (A12, teste locale; conflictele vechi cer recuperare explicită).
- [ ] Fă tranzacționale modificările corelate din baza de date, în limitele provider-ului utilizat.
- [x] Propagă eșecul SQL al salvării gospodăriei sau numărul incomplet de rezidenți ca rezultat eșuat, nu succes complet cu warning; împiedică rollback-ul global automat pentru această categorie de eșec (A08, teste locale).
- [ ] Extinde și verifică politica de recuperare pentru toate etapele. La eșecul persistenței gospodăriei, NPC-urile sunt deja păstrate; comportamentul celorlalte eșecuri de spawn/binding nu este schimbat de lotul A08.

Zone principale: `HouseAllocationPlanner`, `HouseAllocation`, `HouseholdPersistenceServiceState`, `NpcSpawnOrchestrator`, `WorldAdminService` și binding-ul post-spawn din `AINPCCommand`.

Acceptare: două sate cu aceeași denumire locală a casei păstrează patru rezidenți în două gospodării; al doilea rezident nu schimbă proprietarul; o eroare SQL injectată nu raportează succes complet. Migrarea repetată nu produce alte modificări.

## Pasul 3 — Contract unic pentru clădiri și template-uri

Acoperă: A02, A03, A04, A05.

- [ ] Protejează imediat fișierele personalizate: template-urile implicite se instalează doar dacă lipsesc; actualizările sunt versionate și necesită o politică explicită de migrare.
- [ ] Definește o singură sursă autoritativă pentru ID, versiune, amprentă, înălțime, rotații și ancore. Dacă geometria rămâne în JSON și semantica în YAML, leagă-le prin același manifest validat.
- [ ] Unifică `house_small`/`small_house` printr-un ID canonic și alias de compatibilitate; înregistrează coerent și taverna.
- [x] Încarcă `building_templates.yml` la startup/reload și validează schema înainte de înlocuirea catalogului. Primul lot semantic transmite tag-uri/metadata/ancore către mapping și planner; configurația fizică nu este încă unificată. Contract și limite: [template-config-runtime](../reference/template-config-runtime.md).
- [ ] Derivă limitele și ancorele din aceeași definiție folosită la construcție. Validează transformarea lor pentru fiecare rotație permisă.

Zone principale: `BuildingTemplateRegistry`, `BuildingTemplateExecutor`, `WorldGenService`, `building_templates.yml` și manifestele/template-urile fizice.

Acceptare: restartul nu schimbă hash-ul unui template custom; toate ID-urile active se rezolvă; patul, intrarea și workstation-ul mapate corespund blocurilor și orientării construite. O definiție invalidă produce eroare explicită înainte de mutații.

## Pasul 4 — Mapping exact și import validat

Acoperă: A13, A14, A15 și blocajul anterior `npc_spawn` absent.

- [ ] Pentru clădirile generate, creează mapping-ul din planul/template-ul validat, nu prin reidentificarea aproximativă a propriilor construcții.
- [ ] Pentru satul importat, identifică paturile complete și deduplică jumătățile; capacitatea se bazează pe locuri reale de dormit, nu pe numărul brut de blocuri sau pe dublura `bed`/`home`.
- [ ] Delimitează clădirile folosind dovezi spațiale suficiente. Nu impune regula simplistă „un etaj = o casă”; separă locuințele independente și cere confirmare pentru cazuri ambigue.
- [ ] Generează sau rezolvă puncte `npc_spawn` sigure, distincte conceptual de paturi și intrări. Definește dacă un punct de spawn poate fi reutilizat; un singur punct sigur nu trebuie confundat cu o singură capacitate de locuire.
- [ ] Permite roluri multiple pentru o clădire: locuință cu atelier, cârciumă cu camere etc. Expune nodurile de lucru eligibile fără a pierde proprietatea locuinței.
- [ ] Leagă explicit punctele sociale de destinațiile acceptate de planner; simpla existență a unui clopot regional nu garantează `socialAnchor`.

Zone principale: `VanillaVillageScanner`, `SemanticVillageMapper`, `SpawnSemanticRules`, `HouseAllocationPlanner` și validatorii de ancore.

Acceptare: un pat are capacitate 1; două locuințe vecine confirmate rămân distincte; o casă cu etaje poate rămâne o singură casă; atelierul interior este eligibil; scanare → planificare produce alocări valide și raportează ambiguitățile fără a inventa geometrie.

## Pasul 5 — Populație, meserii și profile compatibile

Acoperă: A09, A15, A16.

- [ ] Definește ID-uri canonice de ocupație și profil, separate de etichetele afișate: de exemplu `farmer` și „Fermier”. Adaugă aliasuri/migrare pentru valorile deja salvate.
- [ ] Transformă workstation-urile recunoscute și metadata clădirilor în meserii eligibile; o fierărie detectată nu trebuie să devină automat ocupația generică `Locuitor`.
- [ ] Repartizează posturile la nivelul întregului sat, cu capacitate și rezervări explicite. Nu reseta distribuția la prima fierărie pentru fiecare casă.
- [ ] Reutilizează rezidenții și rezervările existente la reluare; evită mutarea nejustificată a locului de muncă la fiecare replanificare.
- [ ] Validează profilul rezolvat și ancorele cerute de rol. Pentru copii sau alte roluri fără muncă salariată, folosește activități potrivite, nu obligația artificială de a avea un job.
- [ ] Afișează în preview: casă, pat, proprietar, ocupație, workplace, punct social, profil și avertismente.

Acceptare: un fixture cu posturi distincte ocupă atelierele fără a depăși capacitatea; meseriile românești existente ajung la profilurile canonice corecte; fiecare pat și post rezervat are un ocupant coerent. Reluarea păstrează alocările.

## Pasul 6 — Construcție fizică și rezultat verificabil

Acoperă: A01, A02, A03 și blocajele anterioare de teren și rezultat fizic ignorat.

- [ ] Introdu un flux explicit de construire sau un mod separat; păstrează semantica existentă a comenzilor de import/auto-place și confirmările lor.
- [ ] Validează lumea, terenul, panta, înălțimea, coliziunile, intrările, căile și bugetul de blocuri/chunk-uri. Nu folosi automat `region.minY + 1` ca nivel al solului.
- [ ] Execută un plan determinist, cu identitate de operație și preview. Executorul nativ trebuie să fie suficient pentru MVP; WorldEdit rămâne opțional și declarat disponibil doar dacă este verificat.
- [ ] Verifică rezultatul fizic înainte de confirmarea mapping-ului final. Un draft semantic poate exista înainte, dar nu trebuie prezentat ca structură construită.
- [ ] Agregă rezultatele tuturor clădirilor; o eroare fizică nu poate fi mascată de succesul patch-ului semantic.
- [ ] Salvează date suficiente pentru compensarea modificărilor proprii și detectarea conflictelor cu modificări ulterioare ale jucătorilor. Nu implementa undo prin ștergerea necondiționată a zonei.

Acceptare: casa, fierăria și cârciuma apar fizic la coordonatele mapate; un template lipsă sau un executor care eșuează blochează confirmarea; compensarea nu distruge construcții preexistente. Testele cu blocuri reale se rulează pe Paper de test.

## Pasul 7 — Bootstrap și reluarea operației complete

Acoperă: A10 și lipsa fluxului complet la prima inițializare.

- [ ] Salvează pentru lumea/regiunea autorizată: ID-ul operației, seed-ul, versiunea planului, etapele finalizate și artefactele create.
- [ ] Modelează etape persistente explicite, de exemplu `PLANNED → BUILT → MAPPED → ALLOCATED → SPAWNED → BOUND → READY`, plus eroarea și checkpoint-ul de reluare. Aceste nume sunt propuneri, nu statusuri deja implementate.
- [ ] Conectează planificarea, build-ul, mapping-ul, alocarea, spawn-ul și binding-ul într-un rezultat comun. `READY` cere persistența completă și verificarea ancorelor obligatorii.
- [ ] Reconciliază artefactele existente după identitate și conținut. Repetarea nu trebuie să recreeze orbește `bell_1` sau `house_1`; un conflict real cere diagnostic, nu suprascriere.
- [ ] Păstrează importul satului existent ca intrare alternativă, fără build implicit. Refolosește mecanismele actuale de compensare pentru artefactele operației.
- [ ] Dă sens explicit flag-urilor: conectează activarea la acest flux și documentează precondițiile; nu presupune că `worldgen.enabled=true` îl execută deja.

Acceptare: prima pornire autorizată finalizează o singură operație; a doua nu dublează nimic; oprirea după fiecare etapă permite reluarea; un rezultat incomplet rămâne inspectabil și nu pornește prematur rutinele.

## Pasul 8 — Rutine dependente de nevoi și destinații valide

Acoperă: A06, A11, A16, A17 și condițiile runtime documentate anterior.

- [ ] Definește prioritatea dintre pericol, nevoi critice, activități forțate și orar; evaluează aceste condiții înaintea acceptării slotului normal.
- [ ] Implementează o tranziție controlată de trezire și tratează rezultatul `changeState`. Nu elimina global protecțiile pentru combat/panică doar ca să funcționeze somn → muncă.
- [ ] Rezolvă lipsa sau invaliditatea destinației înainte de confirmarea activității. Distinge intenția/deplasarea de executarea efectivă a muncii sau somnului.
- [ ] Adaugă intervale/condiții de socializare care pot fi atinse în profilurile livrate și fallback-uri explicite pentru lipsa workplace-ului.
- [ ] Încarcă profilurile o dată și invalidează cache-ul la reload/modificare validată; păstrează ultima versiune validă dacă un reload eșuează.
- [ ] Stabilește politica pentru NPC nespawnat, chunk neîncărcat și traseu inaccesibil, cu bugete și motive raportate. Nu încărca nelimitat lumea pentru a forța o rutină.

Acceptare: trezire → deplasare → muncă → socializare/acasă → somn pe un ciclu complet; energie/siguranță critică întrerup orarul; lipsa ancorei produce fallback sau blocaj explicit, nu muncă fără țintă. Numărul citirilor YAML nu crește cu fiecare NPC evaluat.

## Pasul 9 — Clădirile și identitatea satului dorit

Scop: extinderea fixture-ului tehnic la satul cerut de utilizator.

- [ ] Definește un profil de sat cu inventar explicit: case, cârciumă, atelier/fermă, punct social, școală și castel dacă profilul îl cere. Tipurile obligatorii trebuie declarate, nu presupuse existente în generator.
- [ ] Pentru fiecare tip, livrează template fizic, limite, intrări, noduri, capacități, roluri și comportamente. Școala are nevoie de profesori/elevi și program; castelul de roluri și acces potrivite.
- [ ] Derivă eticheta „Casa lui Gigel” din proprietarul stabil, păstrând ID-ul casei independent de numele afișat.
- [ ] Leagă circulația dintre locuințe și punctele de activitate și verifică utilizarea efectivă a clădirilor.
- [ ] Păstrează un profil MVP mic pentru depanare și un profil complet pentru scenariul dorit. Orașele mari, politica complexă și economia avansată rămân ulterioare acestui flux stabil.

Acceptare: profilul complet construiește toate tipurile declarate obligatorii, fiecare clădire are identitate și utilizatori, iar „Casa lui Gigel” rămâne aceeași după restart. Un profil cu școală/castel obligatoriu nu este declarat complet dacă acestea lipsesc.

## Pasul 10 — Verificare end-to-end și activare controlată

- [ ] Rulează testele unitare și de integrare relevante pentru mapping, alocări, persistență, binding și rutine pe build-ul curent.
- [ ] Pe Paper de test: lume nouă → sat fizic → mapping → gospodării → NPC-uri → ciclu de rutină → restart → restaurare.
- [ ] Verifică două sate cu aceleași nume locale de case, mai mulți rezidenți, import repetat și template-uri personalizate păstrate.
- [ ] Injectează eșecuri la build, mapping, salvare SQL, spawn și binding; verifică raportarea, compensarea și reluarea fiecărui caz.
- [ ] Compară blocurile, mapping-ul și înregistrările persistente, nu doar mesajele de succes sau existența entităților.
- [ ] Măsoară costul scanării, construcției și rutinei la populația țintă; stabilește pragurile acceptate înaintea testului, fără a inventa limite nevalidate.
- [ ] Actualizează instrucțiunile de configurare, migrare și recuperare, apoi activează gradual numai în lumea autorizată.

Acceptare finală: toate etapele obligatorii sunt complete, nu există coliziuni de identitate, proprietari schimbați accidental, capacități depășite sau rezultate fals pozitive. Restartul și recuperarea sunt demonstrate, iar NPC-urile folosesc clădirile reale conform rolurilor.

## Progres — 2026-10-02

### Lot preventiv A07: refuzul coliziunilor la salvare

- Implementat în `HouseholdPersistenceServiceState.saveHouseholdRow`: dacă ID-ul există deja cu o casă nevidă diferită, salvarea aruncă `SQLException` înainte de `removeConflictingHousehold`, upsert și modificarea rezidenților. Este refuzată și golirea casei deja atribuite.
- Compatibilitatea este păstrată: actualizarea aceleiași gospodării la aceeași casă rămâne permisă, iar o înregistrare legacy fără casă poate primi prima asociere. Nu sunt schimbate ID-urile sau source keys existente.
- Adăugate cinci teste în `HouseholdPersistenceServiceTest`: coliziune între sate, coliziune cu o gospodărie deja prezentă la destinație, încercarea de golire a casei, actualizare normală și asociere legacy. Cazurile refuzate compară toate coloanele din `households` și `household_residents` înainte/după apel.
- Baseline: cele 12 teste existente din `HouseholdPersistenceServiceTest` și `HouseAllocationPlannerTest` au trecut. Înaintea fixului, trei dintre cele cinci teste noi au eșuat; după fix, toate cele 11 teste de persistență au trecut pe build-ul local.
- Validare extinsă: 141 de teste din 31 de suite, fără eșecuri sau teste sărite, pentru `ro.ainpc.spawn.*`, `ro.ainpc.world.*`, `SpawnHistoryAuditTest` și `PostSpawnBindingResultTest`. Comanda: `gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.spawn.*" --tests "ro.ainpc.world.*" --tests ro.ainpc.commands.SpawnHistoryAuditTest --tests ro.ainpc.commands.PostSpawnBindingResultTest --offline --console=plain`. `git diff --check` a trecut pentru codul schimbat.
- Backup de fișiere înainte de editare, verificat prin hash: `.ai/backups/household-safety-20261001-202312/`. Backup MCP: `context-backup-20261001-202158.zip`; `ctx:test-restore` a verificat restaurarea izolată, integritatea și autenticitatea arhivei.
- Baza de cod verificată: HEAD `14f8856` plus modificările locale preexistente, păstrate. Testele folosesc SQLite în memorie. Nu s-au făcut deploy, migrare sau modificări în baza jocului/lumea reală; backup-ul acestora rămâne obligatoriu înaintea intervențiilor respective.

**Limite:** acesta este un mecanism preventiv, nu închiderea A07 sau a pasului 2. Planner-ul poate genera în continuare ID-uri în conflict, dar apelul secvențial verificat nu mai suprascrie gospodăria existentă. Nu este introdusă o tranzacție completă sau o garanție nouă pentru scrieri concurente. La finalul acestui lot, orchestratorul încă transforma excepția în warning; lotul A08 de mai jos schimbă raportarea. Identitățile și proprietarul nu sunt reparate de această protecție; pentru binding-ul proprietarului vezi lotul A12. Datele deja pierdute nu sunt recuperate de această verificare.

### Lot A08: persistența eșuată nu înseamnă spawn complet

- `NpcSpawnOrchestrator` finalizează salvarea prin `HouseholdSpawnPersistence.complete`. O `SQLException` sau un număr salvat diferit de numărul planificat produce `success=false`, `persistenceFailed=true`, `rolledBack=false`, cu eroarea explicită și rezultatele NPC-urilor create/reutilizate păstrate. Un `HouseAllocation` absent nu mai poate deveni succes la această limită de finalizare.
- `HouseholdSpawnResult` distinge structural eșecul persistenței; nu se bazează pe textul erorii. Constructorul JVM anterior cu opt argumente rămâne disponibil. Planurile, binding-ul familiei și avertismentele anterioare sunt păstrate pentru diagnostic.
- `SettlementSpawnFailureHandler` oprește lotul la eșecul persistenței fără rollback global și fără curățarea compensatorie a gospodăriilor. Aceasta evită inclusiv ștergerea gospodăriei originale după refuzul unei coliziuni A07. Nu mai sunt procesate alocările rămase; cele deja procesate rămân inspectabile.
- Tracker-ul primește rezultat eșuat, fără rollback: statusul destinat pasului și lotului este `FAILED`, nu `SUCCEEDED`/`ROLLED_BACK`. Dacă baza este indisponibilă și scrierea istoricului eșuează, durabilitatea acelui status nu este garantată; rezultatul returnat și mesajele rămân eșuate. Comenzile existente nu mai intră în ramura de binding/completare rezervată unui spawn reușit.
- Adăugate 13 teste: șase pentru finalizarea persistenței și compatibilitatea rezultatului, cinci pentru politica de eșec settlement și două în fixture-ul SQLite existent. Probele SQLite verifică păstrarea exactă a gospodăriilor la coliziune și un trigger care eșuează la al doilea rezident după prima scriere. În al doilea caz, starea parțială rămâne inspectabilă, iar cele trei înregistrări NPC sunt păstrate.
- Validare: 24 de teste țintite din trei suite și apoi 154 de teste din 33 de suite, fără eșecuri, erori sau teste sărite. Comanda extinsă rămâne cea documentată în lotul A07. `git diff --check` a trecut; avertismentele existente de compilare și SQLite/native-access nu au blocat testele.
- Backup de surse și documentație înainte de editare, verificat SHA-256: `.ai/backups/household-persistence-20261002-090103/`. Modificările locale preexistente sunt păstrate; nu s-au făcut deploy, migrare, acces la baza jocului sau modificări ale lumii reale.

**Recuperare și limite:** operația este incompletă chiar dacă NPC-urile există. Verifică eroarea, gospodăriile/rezidenții, source keys, familiile și mapping-ul; rezolvă cauza și păstrează un backup înaintea unei reluări controlate. Nu șterge automat NPC-uri reutilizate și nu presupune că simpla rerulare repară orice stare parțială. Salvarea nu devine tranzacțională în acest lot și poate avea deja modificări SQL parțiale înaintea excepției. Politica veche pentru eșecurile de spawn/binding rămâne neschimbată și necesită audit separat al compensării. Nu sunt validate aici un ciclu Paper complet, restartul sau dezactivarea rutinei NPC-urilor incomplete. A07 (identități/migrare) și tranzacțiile rămân deschise; proprietarul este tratat în lotul A12 de mai jos.

### Lot A12: proprietar explicit, independent de ordinea rezidenților

- `WorldAdminService.addNpcResidentToHomePlace` adaugă doar rezidența. `assignHomeOwner` atribuie separat proprietarul, cere un ID nevid deja prezent între rezidenți și refuză înlocuirea unui proprietar diferit înainte de modificarea metadata. Actualizarea aceluiași proprietar este permisă.
- `bindNpcToHomePlace` păstrează semnătura existentă și atribuirea inițială pentru o casă fără proprietar, dar nu mai transferă proprietatea la fiecare rezident. Apelurile de binding/repair care trec prin el nu mai suprascriu proprietarul existent cu ultimul NPC procesat. Numele și statusul proprietarului nu sunt preluate de la alt rezident.
- Ambele operații distincte invalidează cache-ul de liste din WorldAdmin. Fără această corecție, `places` citit înaintea binding-ului păstra snapshot-ul cu vechiul proprietar, deși `getPlace` vedea deja modificarea.
- Comenzile `household spawn` și `settlement spawn` transmit alocarea originală. `HouseholdOwnerBinding.resolve` leagă `primaryOwnerNpcKey` de rezultatul creat/reutilizat și de ID-ul persistent `npc_<databaseId>`, nu de poziția în listă sau de numele afișat. Lipsa cheii, ambiguitatea, planurile pentru altă casă și NPC-ul proprietar invalid produc rezultat de binding eșuat înaintea scrierilor mapping.
- Binding-ul adaugă rezidenții fără atribuire implicită. Proprietarul este aplicat o singură dată în etapa de finalizare, numai după ce binding-urile mapping și persistente ale rezidenților au reușit. Un conflict de proprietar rămâne `PARTIAL`, cu eroare și progresul deja efectuat păstrat, nu succes complet. Nu există rollback automat pentru această etapă.
- Regresie înaintea fixului: 16 teste WorldAdmin executate, 13 existente trecute și trei teste noi eșuate (al doilea rezident înlocuiește proprietarul, aceeași eroare după reîncărcare YAML, cache de places neactualizat). După fix: 39 de teste țintite din trei suite, toate trecute.
- Adăugate 21 de teste: șapte WorldAdmin și 14 pentru selectarea/finalizarea proprietarului. A fost extinsă și verificarea statică existentă a integrării comenzilor. Probele includ ordine inversată, NPC reutilizat, rerulare, round-trip YAML, chei normalizate/ambigue, conflict de proprietar și persistență incompletă care nu permite atribuirea proprietății.
- Validare extinsă: **250 de teste din 36 de suite**, fără eșecuri, erori sau teste sărite. Comanda: `gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.spawn.*" --tests "ro.ainpc.world.*" --tests ro.ainpc.commands.SpawnHistoryAuditTest --tests ro.ainpc.commands.PostSpawnBindingResultTest --tests ro.ainpc.commands.HouseholdOwnerBindingTest --tests ro.ainpc.managers.NPCManagerTextTest --tests ro.ainpc.addons.AddonRegistryTest --offline --console=plain`.
- Backup verificat SHA-256: `.ai/backups/household-owner-20261002-091749/`. Inițializarea economiei deja modificată în `AINPCCommand` și restul worktree-ului au fost păstrate. `git diff --check` a trecut pentru fișierele urmărite modificate în acest lot.

**Limite și continuare:** nu sunt migrate sau reparate automat proprietățile greșite din datele vechi. Un proprietar diferit, inclusiv un identificator legacy care nu corespunde textual ID-ului canonic, necesită verificare și recuperare autorizată; nu se presupune că noul plan poate transfera casa. Round-trip-ul YAML nu echivalează cu un restart Paper complet sau cu verificarea autosave-ului în joc. Nu s-au făcut deploy, modificări în lumea reală sau în baza jocului. Pasul 2 rămâne deschis pentru identități/migrare A07, tranzacții și recuperarea datelor vechi; după acestea urmează contractul template-urilor și fluxul complet de generare.

## Primul lot recomandat

### Progres: baza undo pentru construire fizică

Executorul nativ capturează starea inițială o singură dată per coordonată și o restaurează prin `BlockState.update(true, false)`. Preview-ul nu mai poate fi anulat distructiv. Scrierile și undo cer thread-ul principal; eșecurile parțiale sunt raportate ca eșec și păstrează ID-ul sesiunii pentru recuperare. Snapshot-urile nerestaurate rămân disponibile pentru reîncercare. Testele folosesc dubluri Bukkit, nu un server Paper real.

Pasul 6 rămâne deschis: snapshot-urile nu sunt persistente, nu detectează editările ulterioare și nu garantează restaurarea efectelor externe ale blocurilor. Nu este activată o comandă nouă de construire; integrarea geometriei cu mapping-ul, preview-ul terenului și confirmarea rămân următoarele lucrări.

### Progres 2026-10-05: capacitatea locurilor de muncă

`HouseAllocationPlanner.planSettlement` rezervă acum posturile între casele din același plan, în ordine deterministă. Metadata `max_workers` este validată în template-uri și la planificare: implicit `1`, `0` dezactivează postul, valori invalide produc eroare. Capacitatea epuizată păstrează locuința rezidentului, dar nu atribuie workplace și raportează un avertisment. Planificarea nu modifică mapping-ul și repetarea produce aceeași repartizare. Sunt acoperite capacitatea implicită, capacitatea explicită, epuizarea, valori invalide, limita de case și capacitatea foarte mare fără alocări proporționale în memorie.

Acesta este un lot parțial din pasul 5: nu rezervă posturi în DB, nu consultă angajările existente și nu coordonează spawn-uri concurente sau comenzi independente. Nu schimbă identitățile familiilor și nu migrează date. Asocierea socială rămâne independentă de capacitatea workplace-ului.

Execută pasul 1 și apoi pasul 2: testul cu două sate, remedierea cheilor de gospodărie, separarea proprietarului de rezidenți și propagarea erorilor de persistență. Protejarea fișierelor template din pasul 3 poate intra în același lot de siguranță, cu test separat de restart. Nu începe prin activarea globală a generării sau rutinei.

## Documente conexe

- [Mecanicile simulării și diagnosticul](../reference/mecanici-mcp-simulare.md)
- [Consolidările satului semantic](ce-mai-trebuie-pentru-sat-semantic.md)
- [Generare de sate cu WorldEdit și NPC-uri](generare-sate-worldedit-si-npc.md)
- [Gospodării persistente](../architecture/households-persistente.md)
- [Binding-uri NPC–lume](../architecture/npc-world-bindings.md)

Roadmap-urile anterioare păstrează istoricul propriilor livrări. În particular, suportul pentru reutilizarea unei regiuni și raportarea binding-ului post-spawn nu dovedesc idempotenta importului sau succesul persistenței gospodăriei, verificate separat în acest audit.
