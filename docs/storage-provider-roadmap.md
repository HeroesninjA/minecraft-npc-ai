# Storage Provider Roadmap

Importanta: ridicata.

Acest document detaliaza directia constitutionala pentru persistenta AINPC. SQLite ramane backend-ul implicit pentru dezvoltare, servere mici si demo local. MySQL cu HikariCP este directia de productie, dar trebuie tratat ca suport initial pana cand toate repository-urile folosesc un dialect SQL portabil.

## Configuratie

Backend-ul este ales prin:

```yaml
database:
  type: "sqlite" # sqlite | mysql
```

SQLite:

```yaml
database:
  sqlite:
    filename: "ainpc_data.db"
```

MySQL:

```yaml
database:
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "ainpc"
    username: "ainpc"
    password: ""
    password_env: "AINPC_MYSQL_PASSWORD"
    pool:
      maximum_pool_size: 10
      minimum_idle: 1
      connection_timeout_ms: 30000
```

Regula de securitate: parola trebuie preferabil citita din environment prin `password_env`, nu scrisa direct in config.

## Stare Curenta

- `DatabaseManager` respecta `database.type`.
- SQLite deschide fisier local si ramane default.
- MySQL foloseste `mysql-connector-j` si `HikariDataSource`.
- Configul expune setari de pool pentru productie.
- Exista un helper initial `DatabaseDialectSql` pentru chei primare auto-increment, tipuri text scurte/lungi (`TEXT` vs `VARCHAR/LONGTEXT`), traduceri `INSERT OR ...`, traduceri simple `ON CONFLICT (...) DO UPDATE SET ...` catre MySQL, traduceri `datetime('now'...)` folosite de memories, `LIMIT MAX(0, (...))`, `MIN/MAX` scalar cu doua argumente si traduceri DDL simple.
- O prima parte din schema foloseste tipuri text scurte pentru coloane de identitate/index (`uuid`, `source_key`, `trait_id`, `player_uuid`, `template_id`, quest/progression anchor keys), reducand riscul MySQL de index pe `TEXT`.
- Suportul MySQL este initial/infrastructural.

## Limitari Cunoscute

Codul runtime inca foloseste in unele repository-uri sintaxa SQLite:

- `ON CONFLICT (...) DO UPDATE SET ...`
- indexuri partiale cu `WHERE ...`
- unele conventii de schema orientate SQLite.

Aceste zone trebuie mutate intr-un strat `SqlDialect` sau in metode repository specifice providerului inainte ca `database.type: mysql` sa fie marcat production-ready.

Inventar static curent, protejat de `StorageDialectStaticAuditTest`:

| Pattern | Fisiere runtime |
|---|---|
| `ON CONFLICT` | `ScenarioEngine.java`, `NPCManager.java`, `DialogManager.kt`, `ProgressionRepository.kt`, `HouseholdPersistenceServiceState.kt`, `SpawnBatchTracker.kt`, `StoryStateService.kt`, `NpcWorldBindingService.kt` |
| `INSERT OR` | `NPCManager.java`, `DatabaseManager.kt` |
| `datetime(...)` SQLite | `MemoryManager.kt` |
| `LIMIT MAX(0, (...))` | `MemoryManager.kt` |
| `MIN/MAX` scalar SQL | `DialogManager.kt`, `HouseholdPersistenceServiceState.kt` |
| `PRAGMA` | `DatabaseManager.kt` |

Orice aparitie noua trebuie fie portata prin helper de dialect, fie adaugata deliberat in inventar si roadmap.
`MemoryManager.kt` ramane in inventar deoarece literalele SQLite exista in sursa, dar `DatabaseManager.prepareStatement(...)` le traduce central catre `UTC_TIMESTAMP()` / `DATE_ADD(...)` cand dialectul este MySQL.
Auditul static valideaza si blocurile SQL DML inventariate: dupa `DatabaseDialectSql.translateDml(..., MYSQL)`, nu trebuie sa ramana constructe SQLite-only cunoscute in acel bloc.

## Reguli de Dezvoltare

- Orice tabel nou trebuie definit printr-un helper compatibil SQLite/MySQL.
- Orice upsert nou trebuie sa treaca printr-un helper de dialect, nu sa scrie direct `ON CONFLICT`.
- Orice migration trebuie testat pe SQLite si MySQL.
- SQLite nu trebuie degradat sau eliminat; ramane backend suportat pentru instalari mici.
- MySQL trebuie validat cu smoke test real inainte de release production-ready.

## Pasii Urmatori

1. Extinde `DatabaseDialectSql` pentru DDL, upsert si indexuri. Status: inceput pentru auto-increment, tipuri text scurte/lungi, `INSERT OR ...`, `ON CONFLICT (...) DO UPDATE SET ...` simplu, `datetime('now'...)`, `LIMIT MAX(0, (...))`, `MIN/MAX` scalar si `CREATE INDEX IF NOT EXISTS`.
2. Portare repository-uri: quest, progression, story state, world bindings, spawn tracking, NPC profile.
3. Adauga teste de unit pentru SQL generat per dialect.
4. Adauga smoke test MySQL containerizat sau server local controlat.
5. Marcheaza `database.type: mysql` ca production-ready doar dupa migration si backup validate.
