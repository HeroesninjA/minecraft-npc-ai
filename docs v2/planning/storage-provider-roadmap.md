# Storage Provider Roadmap

Acesta este planul pentru persistenta AINPC pe SQLite si MySQL.

## Directie

- SQLite ramane backend-ul implicit pentru dezvoltare si demo local;
- MySQL cu HikariCP este directia de productie;
- compatibilitatea SQL trebuie mutata intr-un strat de dialect sau in repository-uri dedicate.

## Stare

- `DatabaseManager` respecta `database.type`;
- SQLite ramane default;
- MySQL este functional initial, dar inca are zone cu sintaxa SQLite;
- auditul static protejeaza inventarul de pattern-uri SQL.
