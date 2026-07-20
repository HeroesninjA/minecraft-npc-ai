# Storage runtime

Status: referinta canonica pentru persistenta implementata.
Actualizat: 2026-07-18.

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
- compatibilitatea existenta adauga explicit numai coloanele cunoscute din `player_quests` si `spawn_batch_steps.household_id`;
- `DatabaseDialectSql` traduce pattern-urile SQLite inventariate pentru dialectul MySQL;
- `/ainpc audit db` descopera tabelele reale prin metadata JDBC si compara cele 28 de tabele active cu `DatabaseSchemaCatalog`, mapat pe domeniile `npc`, `dialog`, `quest`, `world`, `spawn`, `story`, `progression`, `economy` si `system`;
- nu exista un registru de versiuni de schema, Flyway/Liquibase, downgrade sau migrare automata SQLite -> MySQL;
- `/ainpc migration households` este un backfill de date de business pentru households, nu o migrare de schema sau de backend.

## Tranzactii si executie

- `executeTransaction` ofera commit si rollback sub acelasi lock;
- publicarea unui draft story muta randul din `story_pending_events` in `story_events` in aceeasi tranzactie;
- existenta helper-ului nu inseamna ca fiecare scriere sau backfill este tranzactional;
- executorul DB este single-threaded pentru helper-ele async;
- unele apeluri pot ramane sincrone, deci prezenta executorului nu garanteaza automat non-blocking pentru toate call site-urile.

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
