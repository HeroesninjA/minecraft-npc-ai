# Storage Provider Roadmap

Status: propunere activa, compatibila si neimplementata complet.
Actualizat: 2026-07-15.

Roadmap pentru maturizarea persistentei. Starea implementata este detinuta de `reference/storage-runtime.md` si nu este redefinita aici.

## Baseline

- SQLite este backend-ul implicit si are acoperire directa in testele repository;
- dialectul MySQL, Connector/J si Hikari exista;
- traducerea SQL si auditul static acopera pattern-urile cunoscute;
- inventarul acceptat include utilizarile `INSERT OR` din relationship si economy;
- MySQL nu are inca o suita de integrare pe server real;
- nu exista versiuni de schema sau migrare automata intre backend-uri;
- pool-ul Hikari furnizeaza in prezent o singura conexiune retinuta si serializata de runtime.

## Prioritati

### P0 - siguranta datelor (IN PROGRESS)

- [x] `DatabaseManager.CURRENT_SCHEMA_VERSION = 1` defineste versiunea asteptata;
- [x] tabela `schema_version` creata la startup cu `version_key='current'`;
- [x] `runMigrations()` verifica versiunea stocata vs curenta;
- [x] startup-ul refuza versiuni viitoare (stored > current → SQLException);
- [x] migratii idempotente intre versiuni cu `applyMigration(fromVersion)`;
- [x] dialect-aware (SQLite `ON CONFLICT` vs MySQL `ON DUPLICATE KEY UPDATE`);
- [ ] migratii reale pentru versiuni > 1 (apelate in `applyMigration`);
- [ ] adauga backup si restore drill separat pentru SQLite si MySQL;
- [ ] defineste reconcilierea si validarea dupa orice import intre backend-uri.

### P1 - dovada MySQL

- ruleaza testele repository pe o instanta MySQL suportata;
- acopera create schema, upgrade, CRUD, tranzactii, restart si concurenta;
- verifica separat MariaDB inainte ca aliasul de configurare sa devina promisiune de compatibilitate;
- transforma auditul static intr-o poarta suplimentara, nu in dovada unica;
- actualizeaza inventarul SQL numai dupa confirmarea traducerii pentru MySQL.

### P1 - conexiuni si tranzactii

- decide explicit intre o singura conexiune serializata si conexiuni per operatie din pool;
- elimina presupunerea ca `maximum_pool_size` ofera paralelism in runtime-ul curent;
- inventariaza scrierile multi-step care trebuie mutate in tranzactii;
- adauga limite, timeout-uri si metrici pentru operatiile lente.

### P1 - configurare securizata

- furnizeaza un profil TLS verificat pentru MySQL;
- interzice parola inline in configuratiile de productie;
- documenteaza rotatia credentialelor si drepturile minime ale utilizatorului DB;
- evita logarea URL-urilor sau erorilor care pot expune secrete.

### P2 - migrare intre backend-uri

- defineste un format sau un pipeline export/import versionat;
- valideaza numarul de randuri, cheile si relatiile dupa import;
- pastreaza backend-ul sursa intact pana la semnarea restore drill-ului;
- documenteaza rollback-ul fara a pretinde conversie automata inexistenta.

## Criterii de acceptare

- upgrade-ul porneste de la fiecare versiune de schema suportata;
- o eroare de migrare lasa o stare detectabila si recuperabila;
- backup-ul se restaureaza intr-un mediu izolat si trece smoke-ul Paper;
- suita reala MySQL trece aceleasi contracte critice ca SQLite;
- documentatia de productie nu depinde de setarile MySQL implicite;
- schimbarea backend-ului nu este prezentata drept simpla schimbare de `database.type`.

## Legaturi

- `reference/storage-runtime.md`
- `operations/migration-si-backup.md`
- `operations/release-checklist.md`
