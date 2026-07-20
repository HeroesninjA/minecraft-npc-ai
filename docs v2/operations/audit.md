# Audit runtime AINPC

Status: runbook operational canonic.
Actualizat: 2026-07-18.

Comanda `/ainpc audit` produce un raport read-only in chat, consola sau JSON compact. Necesita permisiunea `ainpc.admin`, nu repara date si nu inlocuieste testele ori un review de securitate.

## Sintaxa

```text
/ainpc audit <all|npc|world|db|spawn|quest|wand> [strict|full|offline] [json]
```

- fara mod se foloseste `all`;
- optiunile `strict`, `full` si `offline` sunt acceptate numai pentru `quest` si `all`;
- optiunea `json` este acceptata pentru orice mod si poate fi combinata cu un profil valid;
- orice alta combinatie sau orice argument suplimentar afiseaza sintaxa si nu porneste auditul.

## Profile

| Profil | Semantica |
|---|---|
| implicit | verificari live, maximum 500 quest anchors si ultimele 10 spawn batches |
| `strict` | aceleasi verificari live si maximum 500 quest anchors; pentru `all` inspecteaza ultimele 50 spawn batches; orice warning produce verdict `FAIL` |
| `full` | pastreaza verificarile live, pagineaza toate quest anchors in loturi de maximum 500 si listeaza intregul istoric `spawn_batches` pentru `all` prin pagini keyset de 200 |
| `offline` | pagineaza toate quest anchors si intregul istoric persistent de spawn, dar nu consulta NPC manager, entitati managed, lumi, mapping live sau istoricul wand din memorie |

`offline` necesita in continuare pluginul pornit si acces la DB. Numele inseamna ca raportul evita starea live, nu ca poate rula cu serverul oprit.

## Ce verifica fiecare mod

| Mod | Verificare curenta | Limita importanta |
|---|---|---|
| `npc` | ID-uri, nume si source-key duplicate, profil/ancore, lumi incarcate, entitati villager managed si indexul persistent source-key | este o verificare live; `all offline` marcheaza sectiunea `SKIPPED` |
| `world` | regions/places/nodes, bounds, referinte, ownership, suprapuneri si readiness semantic | este o verificare live; `all offline` marcheaza sectiunea `SKIPPED` |
| `db` | inventarul JDBC al tabelelor, numarul de randuri si acoperirea catalogului functional | cele 28 de tabele active sunt mapate pe noua domenii; tabelele lipsa, neclasificate sau imposibil de numarat produc warning |
| `spawn` | batch-uri problematice recente, integritatea randurilor `npc_family` si istoricul persistent detaliat cand profilul este complet | implicit 10, `all strict/full/offline` 50; `all full/offline` pagineaza toate batch-urile dupa `started_at DESC, batch_key ASC`, cate 200 |
| `quest` | campuri obligatorii, compatibilitatea tipurilor, definitia obiectivului si, cand profilul permite, tinta live | implicit/strict maximum 500; full/offline parcurg toate randurile paginat |
| `wand` | draft-uri confirmate recent si existenta tintelor region/place/node/NPC | istoricul este bounded si pastrat numai in memorie; `all offline` marcheaza sectiunea `SKIPPED` |
| `all` | ruleaza exhaustiv toate cele sase sectiuni in ordinea de mai sus | fiecare sectiune respecta profilul selectat |

Catalogul DB acopera domeniile `npc`, `dialog`, `quest`, `world`, `spawn`, `story`, `progression`, `economy` si `system`. Numele tabelelor sunt descoperite la executie prin `DatabaseMetaData`; testul de contract compara separat catalogul cu toate declaratiile active `CREATE TABLE IF NOT EXISTS` din `DatabaseManager`, astfel incat o tabela noua nu ramane tacit in afara auditului.

## Interpretarea raportului

Raportul afiseaza numarul exact de `errors`, `warnings` si `infos`, chiar daca previzualizarea din fiecare sectiune este limitata la primele 12 constatari.

| Constatari | Verdict implicit/full/offline | Verdict strict |
|---|---|---|
| cel putin un error | `FAIL` | `FAIL` |
| zero errors, cel putin un warning | `WARN` | `FAIL` |
| zero errors si zero warnings | `PASS` | `PASS` |

1. pastreaza verdictul, sumarul si momentul executiei;
2. trateaza warning-urile ca indicii care trebuie corelate cu logul si datele, chiar daca profilul `strict` le transforma in verdict `FAIL`;
3. pentru un tabel `<neaccesibil>`, verifica logul si backend-ul efectiv inainte de orice mutatie;
4. ruleaza mai intai modul ingust; foloseste `full` numai cand costul scanarii complete este acceptabil;
5. dupa un fix, ruleaza modul ingust si apoi `all`;
6. nu declara release-ul valid numai pentru ca auditul a produs `PASS`.

## JSON si exit code

Forma `/ainpc audit <mod> [profil] json` trimite un singur document JSON fara coduri de culoare. Contractul curent are `schema_version=1` si `document_type=ainpc-audit-report`.

Campurile top-level sunt:

- `generated_at`, `mode` si `profile`;
- `verdict` si `exit_code`;
- `summary` cu numerele exacte de errors, warnings, infos, constatari retinute/omise si indicatorul `truncated`;
- `execution` cu sectiunile cerute si limitele profilului;
- `database_schema`, `null` daca sectiunea DB nu a produs inventar, altfel cu sursa `jdbc_metadata`, totalurile expected/discovered/mapped, starea de acoperire, tabelele lipsa/neclasificate, row counts si acoperirea celor noua domenii;
- `spawn_history`, `null` daca nu s-a cerut scanarea completa, altfel cu starea `complete`, marimea paginii, paginile citite, totalul asteptat/scanat, numarul problematic/necunoscut si distributia bounded a statusurilor;
- `sections`, fiecare cu numar total, numar retinut/omis si constatari `{severity,message}`.

JSON-ul retine maximum 100 de constatari per sectiune, dar numerele totale raman exacte. Mesajele sunt aceleasi date operationale ca in raportul text; modul JSON nu este o forma `privacy-safe`.

| Verdict | `exit_code` |
|---|---:|
| `PASS` | `0` |
| `WARN` | `1` |
| `FAIL` | `2` |

Comanda Bukkit este considerata gestionata indiferent de verdict si nu poate schimba exit code-ul procesului Paper. Pentru automatizare prin RCON, wrapperul `scripts/ainpc-audit-rcon.ps1` propaga `exit_code` drept cod real de proces; erorile de transport, autentificare sau contract folosesc codul `3`.

Validatorul nu accepta doar orice JSON care contine schema si un exit code. El respinge un payload prefixat/sufixat sau incomplet si verifica semantic:

- `generated_at`, `mode` si `profile`, inclusiv corespondenta cu cererea facuta;
- planul `execution` exact pentru modul/profilul cerut;
- tipurile numerice/booleene, ordinea sectiunilor, severitatile si limita de 100 constatari retinute per sectiune;
- reconcilierea numerelor din `summary`, `sections` si lista de constatari;
- derivarea `PASS/WARN/FAIL` si maparea stricta la exit code `0/1/2`;
- inventarul `database_schema` si sumarul `spawn_history`, atunci cand sunt prezente.

```powershell
$env:RCON_PASSWORD = "<secret>"
powershell -ExecutionPolicy Bypass -File .\scripts\ainpc-audit-rcon.ps1 -Mode all -Profile strict
```

Pentru teste si replay fara server, `-ResponseFile` citeste un raspuns salvat, nu deschide RCON si nu necesita parola. `-Mode` si `-Profile` raman obligatorii semantic: trebuie sa descrie comanda care a produs fisierul. Continutul validat este retrimis compact la stdout, iar aceleasi coduri `0/1/2/3` se aplica.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\ainpc-audit-rcon.ps1 `
  -Mode quest -Profile full -ResponseFile .\.ai\quest-audit.json
```

## Ce nu face

- nu modifica NPC-uri, mapping, config, lume sau DB;
- nu scrie automat raportul intr-un fisier si nu schimba exit code-ul procesului Paper fara wrapperul RCON;
- nu valideaza secrete, permisiuni, dependinte vulnerabile ori configuratia hostului;
- nu demonstreaza compatibilitatea MySQL/MariaDB pe un server real;
- nu valideaza automat forma coloanelor, indexurile sau constrangerile;
- scanarea spawn history nu deschide un snapshot tranzactional intre pagini; o mutatie concurenta poate produce mismatch explicit intre totalul agregat si randurile scanate;
- nu garanteaza integritatea completa a domeniilor inspectate.

Golurile compatibile ramase sunt urmarite in `planning/diagnostic-si-observabilitate-roadmap.md`.

## Legaturi

- `operations/debugging-si-testare.md`
- `operations/observability-and-logs.md`
- `operations/release-checklist.md`
- `reference/storage-runtime.md`
