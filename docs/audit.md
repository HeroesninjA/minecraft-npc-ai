# AINPC Audit

Actualizat: 2026-04-29
Status: revizuit fata de codul curent

## Verdict

Documentul vechi era partial valid ca lista de riscuri de securitate, dar nu era valid ca documentatie pentru sistemul `/ainpc audit`.

In codul curent exista doua zone diferite:

- audit runtime read-only, prin `/ainpc audit`
- security review backlog, adica riscuri care trebuie verificate si remediate separat

Concluzie:

- valid partial ca backlog de securitate
- invalid/incomplet ca documentatie de audit operational
- actualizat mai jos pentru implementarea curenta

## Audit Runtime Implementat

Comenzi:

```text
/ainpc audit
/ainpc audit all
/ainpc audit npc
/ainpc audit world
/ainpc audit db
/ainpc audit spawn
```

Reguli:

- auditul este read-only
- nu modifica NPC-uri, DB, config sau lumea
- foloseste starea curenta incarcata in runtime
- raporteaza erori si warning-uri separat
- este gandit pentru diagnostic rapid in joc, nu pentru testare de securitate completa

## `/ainpc audit npc`

Verifica:

- NPC-uri incarcate in manager
- ID DB valid
- UUID prezent
- nume prezent
- lume setata si incarcata
- ocupatie prezenta
- profil persistent
- `profile_data` JSON valid
- duplicate dupa nume normalizat sau ID DB
- ancore lipsa sau lumi invalide pentru `homeAnchor`, `workAnchor`, `socialAnchor`

## `/ainpc audit world`

Verifica:

- WorldAdmin activ
- regiuni, places si nodes existente
- lumi neincarcate
- bounds invalide
- places in afara regiunii
- nodes in afara containerului
- places suprapuse
- case fara `owner_npc_id`
- locuri de munca fara nodes

## `/ainpc audit db`

Verifica:

- numar randuri `npcs`
- numar randuri `npc_profiles`
- profile DB lipsa
- profile DB orfane
- campuri DB obligatorii lipsa
- UUID-uri duplicate
- `profile_source` lipsa
- `profile_data` JSON invalid

## `/ainpc audit spawn`

Verifica ordinea casa/node/NPC/familie:

- case cu `max_residents` sau `capacity`
- `metadata.residents` nu depaseste capacitatea casei
- casa cu rezidenti are node `bed`, `home` sau `npc_spawn`
- rezidentii din metadata se pot rezolva ca NPC-uri incarcate
- rezidentul are `homeAnchor`
- `homeAnchor` este in interiorul casei
- NPC cu ocupatie reala are `workAnchor`
- `homeAnchor` nu indica un place non-house
- `workAnchor` este in workplace-ul mapat cand exista mapping
- relatiile din `npc_family` sunt reciproce
- randurile duplicate exacte din `npc_family` sunt raportate

## Debug Dump `audit.txt`

Comenzi:

```text
/ainpc debugdump all
/ainpc debugdump npc
/ainpc debugdump world
```

`debugdump` genereaza:

```text
plugins/AINPC/debug-dumps/debug-dump-YYYYMMDD-HHMMSS/audit.txt
```

`audit.txt` include:

- NPC-uri fara UUID
- NPC-uri fara home/work anchor
- NPC-uri fara profil persistent
- NPC-uri fara ocupatie
- WorldAdmin dezactivat sau gol
- case fara owner
- locuri de munca fara nodes
- nodes cu raza invalida
- verificari de baza pentru case, rezidenti si `homeAnchor`

Note:

- `debugdump audit.txt` se suprapune cu `/ainpc audit`, dar nu este identic.
- `debugdump` este pentru investigatie offline.
- `/ainpc audit` este pentru feedback rapid in joc.

## Ce Nu Face Auditul Runtime

- nu face penetration testing
- nu verifica secrete reale din fisiere externe
- nu demonstreaza conformitate GDPR
- nu verifica toate riscurile OpenAI
- nu repara date corupte
- nu face rollback
- nu scaneaza toate fisierele de log pentru secrete

## Security Review Backlog

Documentul vechi enumera 10 riscuri. Ele trebuie pastrate ca backlog de securitate, dar nu toate sunt confirmate ca vulnerabilitati exploatabile in forma actuala.

| # | Tema | Verdict curent | Prioritate |
|---|------|----------------|------------|
| 1 | Cheie OpenAI/logging | Partial valid; verifica logurile reale si foloseste env var. | P0 |
| 2 | SQL identifier injection in migrari DB | Valid ca hardening; exploatare externa neconfirmata daca valorile raman interne. | P1 |
| 3 | Prompt injection | Valid ca risc AI/input-control. | P1 |
| 4 | Validare input comenzi | Valid ca hardening. | P1 |
| 5 | Rate limiting OpenAI | Valid ca risc operational/cost. | P0 |
| 6 | Date jucatori in SQLite plaintext | Valid ca risc privacy/design. | P2 |
| 7 | HTTPS/base_url OpenAI | Valid ca hardening config. | P0 |
| 8 | Cache DecisionEngine fara cleanup dedicat | Valid ca hardening memorie. | P2 |
| 9 | Singleton `AINPCPlugin.instance` non-volatile | Low-priority concurrency hardening. | P3 |
| 10 | Debug info in productie | Valid daca `debug: true` ramane activ pe server public. | P2 |

## Prioritate Recomandata

P0:

- foloseste `OPENAI_API_KEY` din env si lasa `openai.api_key` gol in config
- respinge sau avertizeaza pentru `openai.base_url` fara HTTPS
- adauga rate limiter global pentru request-uri OpenAI
- verifica faptul ca debug dump-ul redacteaza secretele

P1:

- limiteaza lungimea mesajelor trimise catre OpenAI
- separa inputul jucatorului in prompt cu delimitatori clari
- adauga validari de lungime/caractere pentru `/ainpc create`
- valideaza identificatorii DB folositi in migrari

P2:

- politica de retentie pentru `dialog_history` si `npc_memories`
- cleanup cache in `DecisionEngine` cand NPC-ul este sters
- reguli clare pentru `debug: true` doar in test/dev

## Confirmare Finala

`docs/audit.md` este valid acum ca documentatie pentru:

- comenzile runtime `/ainpc audit`
- auditul generat de `debugdump`
- backlog-ul de securitate ramas de verificat

Nu trebuie interpretat ca dovada ca pluginul este sigur complet. Este un document de audit operational si backlog de hardening.
