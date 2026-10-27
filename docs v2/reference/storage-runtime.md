# Storage runtime

Status: referinta canonica pentru persistenta implementata.
Actualizat: 2026-10-27.

Acest document descrie comportamentul verificat in cod. Lucrul neimplementat ramane in `planning/storage-provider-roadmap.md`.

## Selectarea backend-ului

- `database.type` este citit la initializarea `DatabaseManager`;
- valoarea implicita este `sqlite`;
- `mysql` si aliasul `mariadb` selecteaza dialectul MySQL;
- o valoare necunoscuta opreste initializarea bazei de date si, implicit, initializarea pluginului;
- schimbarea tipului nu migreaza automat datele intre backend-uri.

## SQLite

- fisierul este `plugins/AINPC/<database.sqlite.filename>`;
- cheia veche `database.filename` ramane fallback, iar numele implicit este `ainpc_data.db`;
- conexiunea activeaza foreign keys;
- startup-ul aplica `busy_timeout=5000`, `journal_mode=WAL` si `synchronous=NORMAL`;
- backup-ul prin copiere de fisiere necesita oprirea Paper sau un mecanism SQLite online compatibil; scriptul proiectului nu coordoneaza o copie live.

SQLite este backend-ul implicit si backend-ul exercitat direct de testele repository. Aceasta nu elimina obligatia unui restore drill pentru date reale.

## MySQL si MariaDB

- implementarea foloseste MySQL Connector/J si construieste un `HikariDataSource`;
- hostul, portul, baza, utilizatorul si pool-ul vin din `database.mysql`;
- parola este citita mai intai din variabila indicata de `database.mysql.password_env`, implicit `AINPC_MYSQL_PASSWORD`, apoi din `database.mysql.password`;
- setarile implicite `use_ssl: false` si `allow_public_key_retrieval: true` sunt valori de dezvoltare, nu un profil de productie securizat;
- codul retine in prezent o singura conexiune obtinuta din Hikari si serializeaza operatiile printr-un lock global;
- dimensiunea pool-ului nu inseamna, in implementarea actuala, executie SQL paralela per operatie;
- logul de startup marcheaza suportul MySQL/Hikari drept initial si cere validare pe un server real.

Testele de dialect reduc riscul sintaxei incompatibile, dar nu inlocuiesc un test de integrare cu MySQL sau MariaDB real.

## Schema si migrari

- startup-ul executa DDL `CREATE TABLE IF NOT EXISTS` pentru schema curenta;
- `story_pending_events` pastreaza drafturile story separat de istoricul publicat `story_events` si are index scoped dupa `scope_type`, `scope_id`, `queued_at`;
- compatibilitatea existenta adauga explicit coloanele cunoscute din `player_quests`, `spawn_batch_steps.household_id` si `npc_economy.last_salary_at`; ultima este `BIGINT NOT NULL DEFAULT 0` si pastreaza conturile existente;
- `DatabaseDialectSql` traduce pattern-urile SQLite inventariate pentru dialectul MySQL;
- `/ainpc audit db` descopera tabelele reale prin metadata JDBC si compara cele 29 de tabele active cu `DatabaseSchemaCatalog`, mapat pe domeniile `npc`, `dialog`, `quest`, `world`, `spawn`, `story`, `progression`, `economy` si `system`; `bank_interest_payments` pastreaza ultima plata de dobanda dupa restart;
- registrul `schema_version` este la versiunea `2`; migrarea `1 -> 2` adauga `npc_profiles.write_revision`, iar bazele fara registru sunt compatibilizate inaintea marcarii versiunii curente; nu exista Flyway/Liquibase, downgrade automat sau migrare SQLite -> MySQL;
- `/ainpc migration households` este un backfill de date de business pentru households, nu o migrare de schema sau de backend.

## Tranzactii si executie

- `executeTransaction` ofera commit si rollback sub acelasi lock;
- dobanda salveaza soldul si `bank_interest_payments.last_applied` in aceeasi tranzactie; cache-ul se actualizeaza numai dupa commit;
- investirea salveaza debitul si investitia atomic; colectarea salveaza creditul si sterge toate investitiile platite in aceeasi tranzactie, cu verificarea fiecarui rand sters;
- investitiile au ID-uri UUID si folosesc `INSERT`, fara inlocuirea unui rand existent; portofoliul se actualizeaza numai dupa commit si este reincarcat dupa un esec initial de citire;
- randamentele folosesc calcul zecimal, iar totalurile portofoliului folosesc `Long`; investitiile noi cu randament peste limita soldului `Int` sunt respinse, iar colectarile care nu incap in sold pastreaza investitiile neplatite;
- depunerile, retragerile si transferurile raporteaza esecul SQL fara sa modifice soldurile din memorie; transferurile salveaza ambele solduri atomic;
- o eroare la incarcarea initiala a soldurilor blocheaza modificarile pana la recitirea reusita, pentru a evita suprascrierea soldurilor existente cu valori calculate de la zero;
- wrapperul JDBC transmite exceptia originala a driverului, inclusiv `SQLException`, pentru ca serviciile sa poata trata erorile SQL;
- economia NPC actualizeaza cache-ul numai dupa commit; soldul si ultima plata salariala se salveaza atomic, iar erorile initiale de citire blocheaza modificarile pana la o reincarcare reusita;
- platile salariale din scheduler si din rutina de lucru impart intervalul `economy.salary_interval_seconds` (minimum 120 secunde), inclusiv dupa restart; `npc_salary_overrides` este aplicat pe ocupatii;
- plata pentru lucru cere un NPC spawnat aflat intr-o stare de munca; raportul platii periodice numara numai NPC-urile si monedele creditate efectiv, inclusiv la plafonul de 10.000 monede;
- depunerile NPC pastreaza plafonarea existenta si returneaza `false` daca nu se crediteaza nimic; campurile publice `Int` pentru totaluri sunt saturate la `Int.MAX_VALUE` in loc sa devina negative;
- publicarea unui draft story muta randul din `story_pending_events` in `story_events` in aceeasi tranzactie;
- existenta helper-ului nu inseamna ca fiecare scriere sau backfill este tranzactional;
- executorul DB este single-threaded pentru helper-ele async;
- unele apeluri pot ramane sincrone, deci prezenta executorului nu garanteaza automat non-blocking pentru toate call site-urile.

### Siguranta helper-ului JDBC

- `executeTransaction` detine intreaga tranzactie: refuza cu `SQLException` intrarea cand `autoCommit` este deja dezactivat, inainte de a rula callback-ul. Tranzactiile imbricate si cele incepute manual nu sunt suportate; nu se folosesc savepoint-uri. Astfel, un serviciu nu poate publica in cache o plata interioara inca neconfirmata de tranzactia exterioara;
- exceptiile si erorile `Throwable` din callback sau commit declanseaza rollback; eroarea originala este retransmisa, iar eventualele erori de rollback/inchidere sunt pastrate ca `suppressed`;
- daca rollback-ul esueaza, helper-ul nu reactiveaza auto-commit. Trecerea la auto-commit poate confirma tranzactia in curs conform [contractului JDBC](https://docs.oracle.com/en/java/javase/25/docs/api/java.sql/java/sql/Connection.html);
- esecul inceperii tranzactiei, al rollback-ului ori al restaurarii auto-commit marcheaza managerul ca indisponibil si incearca inchiderea conexiunii. Operatiile gestionate noi raporteaza `SQLException`, `getConnection()` returneaza `null`, iar `initialize()` nu reactiveaza acelasi manager. Sunt necesare verificarea datelor si repornirea pluginului;
- daca commit-ul a reusit, dar restaurarea auto-commit esueaza ulterior, operatia deja confirmata ramane raportata drept reusita; conexiunea este dezactivata si eroarea este logata. Aceasta evita cache-uri de sold ramase in urma DB sau reincercarea unei plati deja confirmate;
- cele doua overload-uri `prepareStatement` folosesc aceeasi cale de pregatire si elibereaza lock-ul inclusiv la erori non-SQL. Lipsa conexiunii este raportata ca `SQLException`, nu `NullPointerException`; esecul construirii wrapperului inchide statement-ul creat;
- lock-ul tranzactiei si al `executeUpdate` este eliberat chiar daca finalizarea metricii arunca o eroare. Statement-urile pregatite raman resurse care trebuie inchise pe firul care le-a creat; callback-urile nu trebuie sa modifice direct auto-commit sau sa faca commit/rollback manual.

### Conexiunea activa si proprietarul statement-ului

- `getConnection` si `initialize` participa la acelasi lock. Un apel de pe alt fir asteapta tranzactia/statement-ul activ; `initialize` din interiorul tranzactiei returneaza `false`, fara inlocuirea conexiunii;
- helper-ul pastreaza identitatea conexiunii tranzactiei, verifica disponibilitatea ei la acces si inainte de commit, fara reconectare in interiorul tranzactiei. Pierderea/inlocuirea sau o eroare SQL la verificare ramane un esec chiar daca este interceptata de callback. Se incearca rollback pe conexiunea originala, iar managerul este dezactivat pentru operatii noi;
- nesting-ul este refuzat si prin starea explicita a tranzactiei active, nu doar prin `autoCommit`;
- wrapperul `PreparedStatement` refuza cu `SQLException` orice apel JDBC venit de pe alt fir, inclusiv `close`, `cancel` si `isClosed`, inainte de accesul la driver. Aceasta previne inchiderea resursei urmata de imposibilitatea eliberarii lock-ului de catre proprietar: [ReentrantLock permite unlock numai firului proprietar](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html);
- `equals`, `hashCode` si `toString` folosesc identitatea proxy-ului si pot fi apelate de pe orice fir, fara delegate JDBC. Proprietarul poate repeta `close`; `isClosed` ramane `true` dupa inchiderea wrapperului, iar alte operatii sunt respinse.

Contractul este intentionat mai restrictiv decat JDBC general: nu se transfera statement-uri gestionate intre fire si nu se asteapta task-uri DB de pe alte fire din callback-ul care detine lock-ul. Accesul direct prin `Connection`, `ResultSet` sau `unwrap` nu este incapsulat de aceasta protectie. Callback-urile nu trebuie sa faca manual commit/rollback ori schimbari `autoCommit`; verificarea identitatii nu poate anula un commit facut direct de apelant.

`DatabaseManagerTransactionTest` are 43 de cazuri pe SQLite in fisiere temporare, cu verificarea datelor dintr-o a doua conexiune, fault injection JDBC, conexiuni inlocuite/pierdute, verificarea lock-ului si refuzul operatiilor pe statement de pe alt fir, inspectia gestionata a autocommit-ului, operatii concurente si integrare cu `EconomyService`. Nu valideaza MySQL real. Un esec de retea in timpul commit-ului poate avea rezultat ambiguu; inchiderea unei conexiuni cu tranzactie activa dupa rollback esuat este dependenta de driver. Nu se afirma recuperare automata sau garantie exactly-once in aceste situatii.

### Initializare, pool si shutdown

- Initializarea repetata pastreaza conexiunea disponibila si nu reexecuta schema. La reconectarea unei conexiuni inchise, se imprumuta alta conexiune din pool-ul Hikari existent si neinchis, daca exista;
- o initializare esuata inchide conexiunea partial configurata si pool-ul independent, pastrand eroarea primara si erorile de cleanup ca `suppressed`. Managerul devine indisponibil pana la recreare/repornire; nu se sare peste eroarea de schema prin `getConnection` sau o noua initializare;
- `close` opreste depunerile async, asteapta pana la 15 secunde task-urile deja acceptate, apoi curata resursele sub lock. Inchiderea este terminala: operatiile gestionate nu redeschid DB. Inchiderea pool-ului este necesara separat de returnarea conexiunii, conform [documentatiei HikariCP](https://github.com/brettwooldridge/HikariCP/wiki/FAQ);
- cleanup-ul apeleaza direct `Connection.close`, fara sa depinda de un `isClosed` care poate esua; pool-ul este inchis chiar daca inchiderea conexiunii arunca o eroare. Referintele cu cleanup esuat sunt pastrate pentru o noua incercare `close`, dar managerul ramane inchis;
- la timeout/intrerupere se foloseste `shutdownNow`; task-urile returnate din coada sunt anulate explicit prin future-urile lor. Task-urile deja pornite primesc doar intreruperea best-effort prevazuta de [ExecutorService](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ExecutorService.html), nu o garantie ca efectele lor au fost anulate;
- o intrerupere a firului care inchide DB nu sare cleanup-ul si flagul este restaurat la iesire. `close` apelat de pe executorul DB sau dintr-o operatie care detine lock-ul arunca `IllegalStateException` inainte de oprirea executorului, evitand asteptarea propriei operatii.

`DatabaseManagerLifecycleTest` are 27 de cazuri cu SQLite real si HikariCP 5.1.0 pe SQLite, inclusiv fault injection, initializare concurenta, reconectare, schema refuzata, migrare v0/v1 cu profil pastrat, DDL v2 proaspat, drain cu scrieri persistate, timeout simulat si intrerupere reala. Aceste teste nu valideaza driverul MySQL. Nu exista termen limita global garantat pentru shutdown: achizitia lock-ului si apelurile driverului pot ramane blocate daca o operatie ignora intreruperea sau un statement nu este inchis. Intreruperea nu inlocuieste o reconciliere a commit-urilor ambigue.

## Confirmarea profilului pentru rezervarea progresului

`NPCManager.saveProfile` reutilizeaza [`NpcProfileWriter`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NpcProfileWriter.kt) pentru salvarea conditionata de revizie in `npc_profiles`, nu un UPSERT neconditionat. Payload-ul include o singura captura a identitatii, nevoilor, timpului, checkpoint-ului si absentei; rezervarea ramane exclusiv runtime. Schema v2 si protocolul CAS sunt descrise in sectiunea urmatoare; nu exista task nou la unload.

Toate scrierile de profil folosesc `executeTransaction` pentru revizia CAS. Cu flagurile `simulation.progress.enabled` si `simulation.handoff.enabled` active si o rezervare valida, salvarea poate suplimentar confirma checkpoint-ul numai dupa commit si verificarea sursei/tokenului. `isAutoCommitEnabled` inspecteaza conexiunea sub lock-ul DB. O tranzactie exterioara deja activa este refuzata **fara scriere**, pentru a nu publica o baza locala neconfirmata; nu se creeaza tranzactii imbricate.

Fiecare cerere admisa invalideaza confirmarea precedenta inainte de SQL si primeste un token runtime. Cererile deja vechi in propriul obiect sunt refuzate inainte de SQL. Confirmarile/cache-urile finalizate tarziu nu pot trece peste un token nou sau invalidarea actorului. Erorile SQL/commit/cleanup si rezultatele ambigue raman neconfirmate. Nu se tine monitorul NPC in timpul I/O. `false` semnaleaza si refuzul unei revizii, identitati ori tranzactii exterioare; `true` nu confera autoritate abstracta.

Aceasta confirma salvarea profilului, nu toate tabelele `npcs`, transferul de inventar/bani sau proprietatea abstracta. CAS-ul persistent de mai jos protejeaza acum continutul fata de writerii participanti cu baza veche, inclusiv alte obiecte/conexiuni. Confirmarile si tokenurile runtime nu se restaureaza din JSON. Reconcilierea lumii/resurselor si transferul autoritatii raman necesare inaintea executiei abstracte.

SQLite ramane WAL cu `synchronous=NORMAL`, fara garantie power-loss, conform [documentatiei SQLite](https://www.sqlite.org/pragma.html#pragma_synchronous). Nu se modifica PRAGMA-urile sau backup-urile. Testele folosesc fisiere temporare, conexiuni independente si fault injection; nu valideaza MySQL real sau restart Paper. [Contractul simularii](../architecture/simulation-service.md#confirmarea-sql-a-profilului-rezervat---continuare-ag3) delimiteaza dovada runtime.

## Revizii persistente ale profilului

Schema `2` adauga `npc_profiles.write_revision BIGINT NOT NULL DEFAULT 0`, independent de `profile_version` si progresul nevoilor. DDL-ul proaspat, migrarea v1 si compatibilizarea bazelor fara registru pastreaza JSON-ul existent si baza initiala `0`. Tabela de versiune compatibilizeaza si `updated_at` daca lipseste. Migrarea este idempotenta; nu exista tabel nou sau migrare de backend.

Loaderul citeste profilul si revizia in aceeasi interogare. Un ticket capturat cu baza `r` poate modifica randul numai prin `UPDATE ... WHERE npc_id = ? AND write_revision = r`, atribuind `r + 1` calculat sigur in Kotlin. La `r = 0`, numai un rand absent poate fi inserat. Primul commit valid castiga; nu se face rebase/retry orb al unei capturi respinse. Un profil sters cu baza pozitiva nu este recreat automat. `Long.MAX_VALUE` opreste scrierile, iar reviziile DB invalide nu sunt convertite la zero.

`NpcProfileRevision` blocheaza cererile concurente in timpul publicarii unui commit si respinge ticket-urile vechi. Conflictele fata de alt writer devin `CONFLICT`, fara adoptarea automata a reviziei castigatoare. Dupa erori, o inspectie care confirma baza neschimbata permite o captura noua; altfel starea ramane `UNCERTAIN`. Reconcilierea trebuie sa verifice datele si revizia impreuna. Nu rezolvati conflictele prin resetarea manuala a contorului sau importul unui camp din JSON. Diagnosticul `/ainpc decision` este read-only.

Protectia se aplica salvarilor de profil chiar cu AI oprit, ca un writer legacy fara flaguri sa nu poata ocoli fence-ul. Nu include SQL direct care ignora revizia, alte tabele, o garantie de lease distribuit sau ordonare dupa timpul ultimei capturi. Operatiile unei incercari sunt limitate, fara coada noua sau bucla de retry. Testele includ doi writeri simultani cu conexiuni SQLite independente; nu sunt teste multi-JVM sau MySQL de productie.

### Reconciliere limitata a profilului

Administratorul poate rula `/ainpc profile-reconcile <numeNpc>` pentru preview, apoi explicit `/ainpc profile-reconcile <numeNpc> apply`. Actorul trebuie sa fie canonic, persistat, detasat si blocat `CONFLICT/UNCERTAIN`. Apply participa la politica MCP read-only, verificata si dupa inspectia asincrona. Comanda nu detaseaza entitati pentru a obtine eligibilitate si nu ofera `force` sau un parametru numeric de revizie.

O singura citire SQL verifica UUID-ul din `npcs`, identitatea JSON, profilul complet, metadatele si revizia; nu se executa scrieri sau migrari. Comparatia ignora numai timestamp-ul generat `spawn_state.updated_at` si diferentele de reprezentare JSON echivalente. Date divergente sunt refuzate de `apply`; randul lipsa, revizia invalida/mai veche/epuizata sau actorul schimbat sunt refuzate in toate modurile. `apply` adopta baza verificata numai dupa revalidare pe threadul principal si revoca lucrul runtime vechi. Preview nu deblocheaza profilul si nu consuma progres temporal.

Pentru diferente **numai la nevoi/checkpoint**, preview poate indica eligibilitatea lui `/ainpc profile-reconcile <numeNpc> restore-needs --confirm`. Este o operatie distincta, confirmata, care inlocuieste impreuna cele cinci nevoi, cursorul si checkpoint-ul local cu datele SQL validate si adopta revizia strict mai noua. Necesita unload observat in runtime, checkpoint local si SQL suspendate/coerente, nevoi valide, fara regres de timp/secventa si fara timp SQL ulterior unload-ului. Datele restaurate din JSON nu devin dovezi noi de unload. Niciun alt camp sau metadata SQL nu poate diferi; nu se face import tolerant ori clamp.

Aplicarea revoca lucrul vechi si restaureaza progresul cu generatie noua, fara catch-up, autoritate abstracta sau modificarea lumii/SQL. Nu activeaza flaguri si nu reconciliaza inventare/bani/efecte in curs. O schimbare SQL dupa inspectie ramane posibila si este detectata prin CAS la salvarea urmatoare. Nu folositi comanda ca dovada de lease, transfer durabil sau recuperare generala a unui castigator divergent. Detalii: [profil identic](../architecture/simulation-service.md#reconciliere-explicita-a-profilurilor-identice), [contractul recuperarii nevoilor](../architecture/simulation-service.md#recuperare-explicita-a-nevoilor-divergente).

### Backup si aplicare

Inainte de prima pornire cu schema v2: opriti Paper, inchideti conexiunile si efectuati un backup verificat al datelor/configuratiei/pluginului. Pentru SQLite se poate folosi procedura existenta, cu calea serverului inlocuita explicit:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-backup-restore-check.ps1 -ServerDir "<director-server-oprit>" -IncludeWorlds
```

Scriptul nu coordoneaza un backup SQLite live. Pentru MySQL/MariaDB este necesar separat backup-ul backend-ului si un restore drill. Testati intai pornirea si migrarea pe o copie, apoi verificati `schema_version = 2`, coloana noua si pastrarea profilurilor. Nu exista downgrade automat: revenirea presupune oprire si restaurarea backup-ului coerent impreuna cu versiunea veche a pluginului, nu stergerea coloanei sau resetarea versiunii pe date live. In aceasta implementare nu s-a pornit Paper, nu s-a migrat o baza reala si nu s-a executat backup/deploy pe serverul utilizatorului.

## Tranzactii de magazin

Tranzactiile `ShopService` folosesc exclusiv sloturile de stocare ale inventarului. Schimbul este planificat pe copii, dupa consumarea costului, cu verificarea metadatelor si limitelor stack-urilor. `ITEM` foloseste obiecte; `VAULT_OPTIONAL` foloseste monedele interne cand `EconomyService` este disponibil si revine la obiecte altfel, fara taxare sau plata dubla.

La platile in monede, aplicarea inventarului ruleaza in tranzactia SQL; erorile de aplicare sau commit restaureaza copia initiala a inventarului. Persistenta Minecraft si SQL nu formeaza insa o tranzactie comuna la crash de proces. Nu se livreaza surplusuri prin drop in lume. Permisiunea magazinului este verificata; ofertele cu `maxUses` limitat sau `requirements` sunt respinse explicit pana la implementarea urmaririi limitarilor. `ShopGui` afiseaza moneda si pretul efectiv, reciteste oferta curenta la click si executa cumpararea, apoi afiseaza rezultatul si reincarca meniul.

## Persistenta progresului questurilor

- salvarile individuale si snapshot-urile folosesc acelasi upsert `ON CONFLICT ... DO UPDATE`, pregatit prin `DatabaseManager` pentru traducerea dialectului;
- actualizarea unui quest nu mai sterge randul parinte: ancorele FK, `tracked` si `current_stage_id` deja stocate raman intacte;
- snapshot-ul prefera progresul curent in fata unei incercari arhivate cu acelasi template si copiaza colectiile inainte de programarea scrierii;
- salvarea, stergerea si schimbarea tracking-ului sunt programate pe acelasi executor DB, in ordinea depunerii; o salvare deja programata nu mai recreeaza un quest sters ulterior;
- selectia tracking-ului se schimba printr-un singur `UPDATE`; overload-ul cu progres salveaza progresul si selectia intr-o tranzactie, inclusiv din `startQuestTracking`;
- inlocuirea ancorelor foloseste stergere si `INSERT` standard in aceeasi tranzactie, fara SQL SQLite trimis direct conexiunii brute;
- erorile sunt logate; operatiile async nu ofera apelantului confirmare de commit sau retry durabil. Restaurarea selectiei tracking la restart necesita un audit separat.

`QuestProgressPersistenceIntegrationTest` exercita serviciul real, schema reala si executorul real pe SQLite cu foreign keys active, inclusiv rollback la eroare SQL si commit esuat. Verificarea traducerii SQL nu reprezinta un test pe server MySQL.

### Restaurare la pornire si date invalide

- `ServiceRegistry.phase7Engines` restaureaza questurile imediat dupa crearea engine-ului, inaintea comenzilor, listener-elor si scheduler-ului; restaurarea amanata cu 40 de ticks a fost eliminata;
- `loadPlayerQuests` citeste si valideaza toate randurile intr-un snapshot temporar; o eroare SQL, inclusiv la citirea JSON sau inchiderea cursorului, ori un rand invalid returneaza `false` fara publicarea progresului citit partial;
- `ScenarioEngine` propaga esecul restaurarii; initializarea esueaza si pluginul este dezactivat prin calea existenta, in loc sa accepte interactiuni cu progres incomplet;
- validarea cere UUID canonic, template nevid si status cunoscut; JSON trebuie sa contina obiecte cu chei unice, progres numeric intreg intre `0` si `Int.MAX_VALUE`, respectiv variabile text. `null`, valori incompatibile si JSON malformat nu sunt convertite in mapari goale;
- parserul streaming Gson foloseste `Strictness.STRICT`; nu mai construieste cate doua `TypeToken` pentru fiecare rand. Identitatea randului este serializata pentru diagnostic numai pe calea de eroare;
- la repetarea restaurarii, intrarile deja existente in oricare dintre cache-urile curent/arhivat raman autoritative; citirea SQL completeaza numai cheile absente si nu anuleaza progresul live;
- nu exista reparare automata a datelor. La esec: opriti serverul, realizati un backup consistent, verificati cheia player/template raportata in log, corectati cauza si reporniti. Nu inlocuiti progresul cu `{}` doar pentru a ascunde eroarea;
- snapshot-ul temporar consuma memorie suplimentara la restaurare; aceasta cale ramane sincrona. Nu a fost introdus un reload concurent sau un sistem de carantina per quest.

`QuestProgressLoadingIntegrationTest` verifica 35 de cazuri pe SQLite real: JSON invalid, chei/statusuri invalide, erori JDBC injectate, lipsa publicarii partiale, pastrarea memoriei curente si reusita dupa remedierea cauzei. `QuestProgressStartupContractTest` adauga doua verificari statice ale ordinii de pornire si propagarii esecului; acestea nu inlocuiesc un smoke Paper real.

### Cleanup, reconnect si pack-uri indisponibile

- cleanup-ul periodic si comanda `quest cache clean` nu mai sterg progresul curent, istoricul sau randurile SQL; absenta jucatorului ori a template-ului nu reprezinta o cerere de reset;
- serviciul de cleanup primeste progresul printr-o interfata read-only si nu mai primeste callback de stergere SQL sau acces la arhiva;
- progresul incarcat ramane disponibil pentru lookup dupa reconnect, inclusiv cand un pack dispare temporar si revine; nu este implementata o evacuare LRU ori o incarcare la cerere, deci memoria ramane proportionala cu progresul retinut;
- starea de deduplicare pentru blocuri, locuri si intrari in regiuni este pastrata cat timp exista questuri active, oferite sau suspendate, inclusiv offline; intrarile fara questuri reluabile sunt eliberate;
- cooldown-urile si marcarile runtime pentru jucatori offline sunt curatate; trackerele fara selectie valida, template sau progres curent sunt oprite fara modificarea preferintei persistente;
- oprirea explicita a tracking-ului continua sa salveze deselectarea; cleanup-ul nu reporneste automat tracking-ul la reconnect;
- loaderul de template-uri nu a devenit atomic prin aceasta remediere: este eliminata stergerea progresului din cleanup, nu indisponibilitatea temporara a unui pack invalid.

`QuestProgressCleanupServiceTest` verifica serviciul real in memorie. Doua cazuri suplimentare din `QuestProgressPersistenceIntegrationTest` verifica pastrarea progresului, arhivei, ancorelor si tracking-ului SQL dupa cleanup, flush si reincarcare. Testarea pe un server Paper cu reconnect/reload ramane necesara.

## Perceptie tranzitorie si provenienta

Cadenta optionala `simulation.lod.perception.enabled` nu modifica schema `2`, JSON-ul profilului sau politica SQL de reconciliere. Istoricul per actor retine numai in runtime sursa observatiei, configurarea, pozitia si tick-ul serverului; referintele/tokenurile sunt marcate tranzitorii. Nu se restaureaza dupa restart sau dintr-un profil, iar schimbarile de ciclu de viata le invalideaza.

Snapshot-ul comun poate expune metadata imuabila `NpcPerceptionObservation`, fara referinte Bukkit sau tokenuri: momentul observatiei originale si varsta la ultima evaluare. Ea nu este un checkpoint de timp/resurse, o confirmare noua de observatie sau un lease. Dialogul si diagnosticul nu transforma reutilizarea in cunostinte proaspete. Nu exista scrieri DB, worker ori task periodic nou pentru cache. Detalii si limite: [cadenta perceptiei](../architecture/simulation-service.md#cadenta-perceptiei-pentru-npc-uri-indepartate).

## Verificari existente

- `DatabaseDialectSqlTest` verifica transformarile SQL cunoscute;
- `StorageDialectStaticAuditTest` inventariaza pattern-urile SQLite si verifica traducerea lor;
- `ConfigDefaultsTest` verifica backend-ul si variabila de parola implicite;
- `DatabaseSchemaInventoryTest` compara catalogul cu DDL-ul activ, verifica descoperirea metadata si agregarea pe domenii;
- inventarul explicit include utilizarile `INSERT OR` din relationship si economy, iar testul de traducere nu lasa pattern-urile DML SQLite cunoscute in SQL-ul MySQL;
- nu exista inca o suita de integrare pe un server MySQL real.

Auditul static verde reduce deriva, dar nu dovedeste executia pe un server MySQL real.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/database/DatabaseManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/database/DatabaseDialect.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/database/DatabaseSchemaInventory.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap/ServiceRegistry.kt`
- `ainpc-core-plugin/src/main/resources/config.yml`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/database/DatabaseDialectSqlTest.kt`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/database/DatabaseSchemaInventoryTest.kt`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/StorageDialectStaticAuditTest.kt`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/ConfigDefaultsTest.kt`

## Legaturi

- `planning/storage-provider-roadmap.md`
- `operations/migration-si-backup.md`
- `operations/server-admin-runbook.md`
