# Rezumat audit si remedieri

Data: 2026-10-03. Actualizat: 2026-10-04. Proiect: AINPC, plugin Paper.

Acest document rezuma loturile de remedieri din sesiunea curenta si continuarile ei. Nu reprezinta o certificare a intregului repository si nu afirma ca fiecare linie din proiect a fost verificata. Modificarile sunt locale, necomise; modificarile utilizatorului din alte domenii au fost pastrate.

## Lotul curent: initializare, reconectare si inchidere DB

- `initialize` devine idempotent pentru o conexiune disponibila: nu mai deschide alta conexiune si nu repeta schema la fiecare apel. La reconectare, un pool Hikari existent si neinchis este reutilizat, nu abandonat in favoarea unui pool nou.
- Esuarea initializarii, inclusiv configurare/schema/migrare si erori non-SQL, declanseaza cleanup pentru conexiune si pool. Eroarea initiala si erorile secundare sunt pastrate; managerul nu mai expune o conexiune partial initializata si nu permite ocolirea unei versiuni de schema refuzate prin alt apel.
- `close` este terminal pentru manager: dupa cleanup, accesul gestionat si reinitializarea sunt refuzate. O eroare la inchiderea conexiunii sau la verificarea `isClosed` nu mai poate sari inchiderea pool-ului. Resursele a caror inchidere esueaza raman disponibile pentru o noua incercare de cleanup, fara redeschiderea managerului.
- Inchiderea normala lasa task-urile deja acceptate sa salveze, dar refuza depuneri noi. La timeout sau intrerupere, task-urile scoase din coada isi primesc explicit anularea in `CompletableFuture`, in loc sa ramana nerezolvate; cleanup-ul ruleaza si flagul de intrerupere este restaurat.
- Apelul `close` dintr-o operatie care detine lock-ul sau de pe executorul DB este refuzat imediat, inainte de oprirea executorului; nu mai asteapta propria operatie.

Validare: **24 de cazuri noi in `DatabaseManagerLifecycleTest`**, cu SQLite real, pool Hikari real pe SQLite, exceptii injectate, initializari concurente, salvare la inchidere, timeout simulat al executorului si intrerupere reala. Primele 16 cazuri au esuat inaintea remedierii; toate cele 24 si cele 42 de cazuri transactionale trec acum.

**Limite operationale:** initializarea esuata cere diagnosticare si un manager nou/repornirea pluginului; nu exista retry automat dupa schema refuzata. Cele 15 secunde limiteaza asteptarea executorului, nu intregul shutdown: un driver blocat, un task care ignora intreruperea sau un statement neinchis care detine lock-ul pot prelungi oprirea. Se anuleaza explicit task-urile neincepute scoase din coada, nu se declara ca un task deja pornit a fost anulat sigur. MySQL/MariaDB si Paper live nu au fost exercitate.

## Lot anterior: conexiunea tranzactiei si statement-uri intre fire

- Apelurile JDBC pe un statement gestionat sunt refuzate cu `SQLException` de pe alt fir, inainte de modificarea delegate-ului sau a starii wrapperului. Anterior, inchiderea pe alt fir inchidea resursa, marca wrapperul inchis si apoi esua la `unlock`; firul proprietar nu mai putea elibera lock-ul.
- `equals`, `hashCode` si `toString` folosesc identitatea wrapperului, fara acces la driver, inclusiv de pe alt fir. Inchiderea ramane idempotenta pentru proprietar, iar operatiile sale pe wrapperul inchis sunt respinse.
- `getConnection` si `initialize` folosesc acelasi lock ca tranzactiile si statement-urile. Initializarea ceruta dintr-o tranzactie activa este refuzata; apelurile de pe alt fir asteapta eliberarea lock-ului.
- Tranzactia retine conexiunea initiala si o verifica inclusiv inainte de commit. Conexiunea pierduta/inlocuita sau o eroare SQL la verificarea ei nu declanseaza reconectare in tranzactie. Eroarea ramane memorata chiar daca callback-ul o intercepteaza; se incearca rollback pe conexiunea originala si managerul este dezactivat.
- Refuzul nesting-ului verifica si proprietatea tranzactiei, nu doar valoarea `autoCommit`; manipularea manuala a flagului nu permite intrarea intr-un al doilea helper.

Validare: **20 de cazuri noi, 42 in total in `DatabaseManagerTransactionTest`**, cu SQLite real, injectarea unei conexiuni de inlocuire, pierderea/verificarea conexiunii, executie pe alt fir si verificarea lock-ului. Primele 14 cazuri noi au esuat inainte de remediere; toate cele 42 trec acum.

**Limita contractului:** wrapperul este limitat la firul creator, inclusiv pentru `cancel` si `isClosed`; acesta este mai restrictiv decat utilizarea generala JDBC. Referintele brute `Connection`, `ResultSet` si cele obtinute prin `unwrap` nu sunt protejate dupa iesirea din helper. Lotul de lifecycle completeaza protectiile de initializare/reconectare/inchidere, fara validare MySQL live. Nu se asteapta un task DB de pe alt fir din interiorul tranzactiei care detine lock-ul.

## Lot anterior: tranzactii JDBC, rollback si lock-uri

- `executeTransaction` face rollback si pentru `Error`/`Throwable`, nu doar pentru `Exception`; este eliminata confirmarea implicita a scrierilor ramase dupa o eroare din callback.
- Tranzactiile imbricate sau incepute manual sunt refuzate inaintea callback-ului. Nu a fost introdus suport de savepoint: serviciile economice presupun ca revenirea reusita din helper inseamna commit, deci o tranzactie interioara neconfirmata nu poate publica solduri in cache.
- Erorile secundare de rollback/inchidere sunt pastrate, fara mascarea erorii initiale. Dupa rollback esuat nu se reactiveaza auto-commit; conexiunea nesigura este dezactivata pentru operatii noi.
- Daca commit-ul a reusit deja, o eroare ulterioara de restaurare auto-commit nu transforma plata confirmata intr-un esec; soldul din cache ramane sincron cu plata salvata, iar managerul DB blocheaza urmatoarele operatii pana la repornire.
- Overload-urile `prepareStatement` impart acelasi helper si elibereaza lock-ul la erori non-SQL. Conexiunea absenta produce `SQLException`, nu NPE; finalizarea metricilor nu poate impiedica eliberarea lock-ului tranzactiei.

Validare: **22 de cazuri noi in `DatabaseManagerTransactionTest`**, cu SQLite real pe disc, observarea commit-urilor de pe o conexiune separata, exceptii JDBC injectate, lock verificat de pe alt fir, 18 tranzactii concurente serializate si integrare cu `EconomyService`. Primele 18 cazuri au reprodus 14 esecuri inaintea remedierii; dupa remediere toate cele 22 trec. Testele tintite ale economiei au trecut separat.

**Schimbare operationala:** o conexiune cu stare nesigura nu este reactivata automat de acelasi manager. Sunt necesare diagnosticarea rezultatului operatiei si repornirea pluginului. Suportul MySQL real, rezultatul unui commit intrerupt de retea si siguranta referintelor JDBC brute/conexiunilor inlocuite raman in afara dovezilor acestui lot.

## Lot anterior: restaurare sigura si date corupte

- `loadPlayerQuests` nu mai publica randuri pe masura citirii; valideaza intregul snapshot inainte de a modifica memoria. Erorile SQL, inclusiv la citirea coloanelor JSON si inchiderea cursorului, nu mai lasa progres partial.
- JSON invalid, valori `null`, chei duplicate, tipuri gresite, overflow sau progres negativ nu mai devin tacit mapari goale. UUID-urile, template-urile si statusurile sunt validate explicit.
- Restaurarea repetata nu mai suprascrie progresul deja existent in cache si nu introduce o copie arhivata peste o incercare curenta sau invers.
- Restaurarea initiala ruleaza in faza engine-urilor, inainte de comenzi/listenere/scheduler, nu dupa 40 de ticks. La esec, initializarea pluginului este oprita prin calea existenta de dezactivare.
- Parserul streaming strict elimina alocarile `TypeToken` per rand; cheia pentru diagnostic este serializata numai la eroare.

Validare: **35 de cazuri noi de integrare SQLite si 2 verificari statice pentru contractul de pornire**. Prima rulare a 32 de cazuri a reprodus 30 de esecuri; ulterior toate trec, inclusiv cazurile suplimentare de reparare/reincercare. Nu a fost rulat un server Paper.

**Schimbare operationala:** un rand invalid poate impiedica pornirea intregului plugin. Datele nu sunt resetate sau reparate automat; sunt necesare backup consistent, diagnosticarea cheii din log si corectarea cauzei. Restaurarea sincrona foloseste temporar memorie suplimentara, iar editarea directa a DB nu forteaza suprascrierea unui cache live existent.

## Lot anterior: cleanup, reconnect si pack-uri lipsa

Au fost urmarite `QuestProgressCleanupService`, lookup-ul progresului, scheduler-ul periodic, listener-ul de join si comanda `quest cache clean`.

- Cleanup-ul nu mai elimina progresul jucatorilor offline sau istoricul lor; reconnect-ul poate folosi aceleasi date deja incarcate, fara recrearea unui quest de la zero.
- Absenta temporara a unui template/pack nu mai sterge progresul din memorie sau SQL. Au fost eliminate din constructorul cleanup-ului dependentele de stergere SQL si accesul la arhiva; accesul la progresul curent este read-only.
- Sunt eliminate trackerele runtime nevalide si cooldown-urile offline, fara rescrierea preferintei persistente. Oprirea explicita a tracking-ului isi pastreaza comportamentul de salvare.
- Datele de deduplicare pentru blocuri, locuri si regiuni sunt eliberate numai pentru jucatorii fara questuri reluabile; cele aferente questurilor active, oferite sau suspendate raman disponibile inclusiv offline.
- Parsarea cheilor compuse foloseste numai prefixul UUID, evitand listele produse de `split`; comanda cache clean nu mai executa de doua ori aceeasi curatare si afiseaza corect ca progresul este pastrat.

Validare: **13 teste noi pentru serviciul real de cleanup si 2 teste noi de integrare SQLite**. Prima rulare a celor 13 teste a reprodus 9 esecuri; dupa remediere toate trec. Vechiul test care verifica doar filtrarea unei liste locale de template-uri a fost inlocuit de aceasta acoperire pe serviciul real. Cele doua teste SQLite verifica si pastrarea ancorelor, tracking-ului si istoricului la flush/reincarcare.

Limita asumata: progresul ramane in memorie; nu a fost adaugat un cache LRU sau read-through. Loaderul pack-urilor nu a fost transformat intr-un loader atomic, iar reconnect/reload nu au fost exercitate pe Paper live.

## Lotul anterior: persistenta questurilor

Au fost analizate metodele din `QuestProgressPersistenceService`, apelul de pornire a tracking-ului din `ScenarioEngine`, schema SQL relevanta si executorul DB.

| Defect / risc verificat | Remediere |
| --- | --- |
| Actualizarea progresului prin `INSERT OR REPLACE` stergea ancorele FK si reseta tracking-ul/metadatele neincluse in insert. | Un singur upsert `ON CONFLICT ... DO UPDATE` pentru salvarea individuala si snapshot; randul parinte nu mai este sters. |
| Snapshot-urile si ancorele pregateau SQL direct pe conexiune, ocolind traducerea dialectului. | Statement-uri pregatite prin `DatabaseManager`; ancorele folosesc `INSERT` standard in tranzactia de inlocuire. |
| O salvare deja programata putea recrea un quest dupa stergerea lui sincrona. | Stergerile folosesc acelasi executor ca salvarile, respectand ordinea depunerii operatiilor. |
| Selectia tracking putea rula inaintea insertului sau se putea pierde daca al doilea UPDATE esua. | Tracking-ul este programat pe executor si schimbat printr-un singur UPDATE atomic. |
| Overload-ul de tracking cu progres nu salva selectia; pornirea tracking-ului apela doar salvarea progresului. | Progresul si selectia sunt salvate in aceeasi tranzactie; apelul din `startQuestTracking` este conectat la aceasta cale. |
| O incercare arhivata putea suprascrie incercarea curenta pentru acelasi template in snapshot. | Snapshot-ul deduplica dupa template si prefera progresul curent. |
| Modificarea colectiilor apelantului dupa programarea snapshot-ului putea schimba ce se salva. | Maparea si listele sunt copiate inainte de programarea scrierii. |

Semantica distructiva a `REPLACE` este documentata oficial de [SQLite](https://www.sqlite.org/lang_conflict.html); stergerea ancorelor a fost reprodusa in testele serviciului real cu [foreign keys si cascada](https://www.sqlite.org/foreignkeys.html) active.

### Teste adaugate

`QuestProgressPersistenceIntegrationTest`: 16 teste noi, cu schema reala creata de `DatabaseManager`, SQLite in memorie si executorul real, fara server Paper simulat.

- pastrarea ancorelor, tracking-ului si metadatelor la salvare individuala si snapshot;
- ordinea save/delete, insert/tracking si delete/inceputul unei noi incercari;
- rollback la eroare de statement si eroare de commit;
- rollback complet pentru snapshot-uri si inlocuirea ancorelor;
- izolarea selectiei intre jucatori, preferinta pentru progresul curent si copierea snapshot-ului;
- reincarcarea progresului si verificarea trecerii SQL prin pregatirea dialect-aware.

Primele 13 teste au fost rulate inaintea remedierii comportamentului: 10 au esuat. Dupa remediere si adaugarea a trei cazuri suplimentare, toate cele 16 trec. Traducerea SQL este verificata; nu a fost pornit un server MySQL.

## Recapitularea loturilor anterioare

| Domeniu | Modificari realizate |
| --- | --- |
| Scheduler / reload | Pornire idempotenta; operatiile dependente de Bukkit mutate pe firul principal; watcher-ul detecteaza creare, modificare si stergere pornind de la un snapshot initial. |
| Relatii | Decay-ul pastreaza momentul ultimei interactiuni si evita scrierile fara schimbari. |
| Economia jucatorilor | Validari de sume si overflow; transferuri atomice; cache actualizat dupa commit; esecurile SQL nu sunt raportate ca plati reusite; reincarcare dupa esecul citirii initiale. |
| Integrarea JDBC / Vault | Exceptiile originale JDBC ajung la servicii; operatiile Vault si comenzile verificate respecta rezultatul real al modificarilor de sold. |
| Dobanda / investitii | Ultima dobanda este persistenta si salvata atomic cu plata; investitiile au UUID; debitul, creditul si inregistrarile aferente sunt tranzactionale; randamente zecimale si totaluri Long. |
| Economia NPC | Soldul si ultima plata salariala sunt persistate atomic; rutina si scheduler-ul impart intervalul salarial, inclusiv dupa restart; plafon si raportare pe sumele efectiv creditate; override-urile configurate sunt aplicate. |
| Magazine | Eliminata taxarea/plata dubla; schimbul este planificat pe sloturile de stocare, dupa consumul costului; respecta metadatele si limitele stack-urilor; restaurare inventar la erori de aplicare/commit. |
| GUI magazin | Butonul cumpara efectiv, reciteste oferta la click, afiseaza moneda/pretul si rezultatul; listele publice ale magazinelor sunt snapshot-uri. |

Principalele teste de integrare adaugate in loturile anterioare sunt `EconomyServiceIntegrationTest`, `BankingServiceTransactionTest`, `NpcEconomyServiceIntegrationTest` si `ShopTransactionTest`. Comportamentul persistentei este detaliat in [storage-runtime.md](storage-runtime.md).

## Validare finala a lotului curent

- Comanda: `./gradlew.bat :ainpc-core-plugin:test --console=plain`, cu JDK 25.0.2 local.
- Rezultat: **BUILD SUCCESSFUL**.
- Rapoarte JUnit XML: **1.589 teste in total, 1.588 reusite, 1 omis, 0 esecuri, 0 erori**.
- Cele 24 de cazuri lifecycle si 42 de cazuri transactionale JDBC au trecut separat inaintea suitei complete; loturile precedente au verificat separat economia, restaurarea, persistenta, cleanup-ul si traducerea de dialect.
- `git -c core.safecrlf=false diff --check`: fara erori.
- JVM raporteaza avertismentul de native access pentru SQLite JDBC; acesta nu a impiedicat executia testelor.
- Navigarea pe simboluri si referinte Serena functioneaza in aceasta continuare; documentatia HikariCP a fost verificata prin Context7. Compilarea si validarea de referinta sunt cele Gradle, care au trecut.

## Limite si urmatoarele zone de audit

1. Nu au fost rulate teste pe un server Paper sau MySQL/MariaDB real; nici intreaga suita multi-modul nu a fost rulata in acest lot.
2. Persistenta questurilor ramane asincrona, cu erori logate, fara confirmare de commit catre apelant sau retry durabil. Ordinea este garantata pentru operatiile depuse pe executor, nu pentru snapshot-uri vechi depuse ulterior.
3. Restaurarea selectiei tracking la restart/reconnect, reload-ul atomic al pack-urilor si izolarea starii temporare intre questuri distincte raman de verificat/remediat separat. Randurile invalide sunt acum respinse conservator la restaurare; repararea automata, carantina individuala si validarea tuturor domeniilor DB nu sunt implementate.
4. Recompensele de quest si finalizarea lor nu au fost auditate complet in acest lot; nu exista aici o garantie de acordare exact o data.
5. Inventarul Minecraft si SQL nu au commit comun la crash de proces. Magazinele resping explicit ofertele cu limite `maxUses` sau `requirements` neimplementate; nu a fost adaugat un loader de magazine ori un flux nou de vanzare in GUI.
6. Compatibilitatea coloanelor istorice de timestamp cu MySQL, driver-ele blocate, referintele JDBC brute si un shutdown cu termen limita global necesita audit separat. Reutilizarea si cleanup-ul Hikari au teste cu backend SQLite, nu MySQL real. Tranzactiile gestionate nu isi mai schimba conexiunea si statement-urile gestionate refuza utilizarea de pe alt fir; aceasta nu transforma toate referintele JDBC in resurse thread-safe. Nesting-ul este refuzat, fara savepoint-uri; rezultatul unui commit intrerupt de retea necesita reconciliere.

Aceste limite sunt puncte de continuare, nu rezultate prezentate ca remediate.
