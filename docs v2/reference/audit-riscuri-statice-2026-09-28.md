# Riscuri statice și condiționale

## RISK-001 — Progres quest eliminat din cache fără încărcare garantată la reconnect

- **Dovadă:** cleanup-ul șterge progresul offline; încărcarea principală este la startup, iar lookup-ul folosește cache-ul.
- **Condiție:** jucătorul rămâne offline peste intervalul de cleanup.
- **Impact:** progresul poate părea pierdut sau se poate recrea o intrare existentă în DB.
- **Remediere:** păstrează progresul persistent și reîncarcă-l din DB la reconnect/primul lookup.
- **Status:** static; necesită test Paper cu reconnect.
- **Actualizare 2026-10-03:** defect reprodus si remediat in `QuestProgressCleanupService`; progresul si arhiva nu mai sunt evacuate la offline. Lookup-ul la revenire foloseste starea pastrata. Testele serviciului real si integrarea SQLite trec; nu a fost rulat reconnect pe Paper live.

## RISK-002 — Reload invalid poate elimina progresul unui quest

- **Dovadă:** loaderul golește mapările înainte de încărcare, iar cleanup-ul elimină template-urile absente din snapshotul curent.
- **Condiție:** YAML incomplet, pack lipsă sau addon indisponibil la reload.
- **Impact:** progresul DB poate fi șters ireversibil înainte ca pack-ul să redevină valid.
- **Remediere:** reload atomic, păstrarea ultimului snapshot valid și interzicerea ștergerii progresului pentru simpla absență temporară.
- **Status:** static; necesită scenariu Paper cu reload eșuat.
- **Actualizare 2026-10-03:** calea distructiva de cleanup este eliminata, inclusiv din `quest cache clean`; un template temporar absent nu mai sterge progresul din memorie sau DB. Testele acopera disparitia/revenirea template-ului si reincarcarea SQL. Reload-ul atomic al registrului si validarea pe Paper raman separate.

## RISK-003 — SQL de quest nu este portabil între dialecte

- **Dovadă:** `QuestProgressPersistenceService` folosește direct `INSERT OR REPLACE`, în timp ce `DatabaseManager` are traducere după dialect.
- **Condiție:** activarea MySQL/MariaDB fără adaptarea căii de persistence.
- **Impact:** flush-ul progresului poate eșua și eroarea este doar logată.
- **Remediere:** API DB comun sau upsert explicit per dialect și test de integrare.
- **Status:** static; SQLite este mediul curent de dezvoltare.
- **Actualizare 2026-10-03:** calea `QuestProgressPersistenceService` pregateste acum toate statement-urile prin `DatabaseManager`, iar progresul foloseste upsert non-destructiv. Regresiile SQLite si traducerea SQL sunt testate; integrarea MySQL/MariaDB reala ramane nevalidata. Detalii in [rezumatul remedierilor](rezumat-audit-remedieri-2026-10-03.md).

## RISK-004 — Smoke deploy poate valida o versiune veche

- **Dovadă:** scriptul marchează transferul și restartul drept succes fără verificarea exit code-ului.
- **Impact:** smoke-testul poate trece după un deploy eșuat.
- **Remediere:** verifică `$LASTEXITCODE` și build-id-ul după restart.
- **Status:** static; necesită VPS.

## RISK-005 — Lint-ul CI este permis să eșueze

- **Dovadă:** workflow-ul folosește `|| true` pentru pasul de lint.
- **Impact:** o eroare de lint nu blochează build-ul și pasul raportează fals succes.
- **Remediere:** elimină mascarea sau redenumește pasul ca informativ.
- **Status:** confirmat prin citirea workflow-ului; efectul asupra branch protection depinde de configurarea CI.

## RISK-006 — Parserul RCON poate accepta răspunsuri incomplete

- **Dovadă:** verificarea folosește lungimea bufferului alocat, nu numărul efectiv de octeți citiți.
- **Impact:** pachetele truncat pot fi interpretate ca răspuns valid.
- **Remediere:** acumulează până la lungimea declarată și respinge `$read -ne $len`.
- **Status:** static; necesită server RCON.
