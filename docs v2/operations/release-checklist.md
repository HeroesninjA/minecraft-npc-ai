# Release Checklist

Status: checklist operational canonic.
Actualizat: 2026-07-17.

Release-ul necesita dovezi pentru artefact, startup, date si rollback. Un build verde sau un ZIP cu hash-uri nu este suficient separat.

## 1. Scope si artefacte

- [ ] versiunea si commit-ul sursa sunt identificate;
- [ ] `projectVersion` si `apiVersion` sunt identificate separat;
- [ ] schimbarile intentionate sunt separate de fisiere locale sau secrete;
- [ ] testele relevante trec;
- [ ] `:ainpc-api:verifyApiAbi` trece fata de baseline-ul comis;
- [ ] orice diferenta ABI este clasificata, are bump SemVer justificat si baseline revizuit;
- [ ] `release-api-addon-freeze.ps1` trece cu `-FailOnWarnings`;
- [ ] core JAR contine `plugin.yml` si runtime-ul necesar;
- [ ] addon JAR contine descriptorul, config template si pack-urile gestionate;
- [ ] API JAR nu este livrat ca plugin Paper;
- [ ] nu exista JAR-uri duplicate sau artefacte vechi prezentate drept build curent.

## 2. Secrete si configurare

- [ ] secret scanning-ul nu raporteaza credentiale literale urmarite in repository;
- [ ] orice credential expus anterior a fost rotit sau revocat, nu doar sters;
- [ ] debugdump-urile si rapoartele sunt sanitizate;
- [ ] parolele DB, RCON, tokenurile si cheile private vin din surse controlate;
- [ ] backup-ul este restrictionat deoarece poate contine `config.yml`.

Scriptul `scripts/deploy-ainpc-vps.sh` este blocat pentru release pana cand credentialul literal identificat este rotit/revocat si scriptul este parametrizat. Nu copia valoarea in rapoarte.

## 3. Date si backup

- [ ] backend-ul efectiv este inregistrat;
- [ ] Paper este oprit sau datele sunt quiesced printr-o metoda aprobata;
- [ ] backup-ul de fisiere are manifest si hash-uri;
- [ ] backup-ul MySQL exista separat cand backend-ul nu este SQLite;
- [ ] restore-check-ul de fisiere trece;
- [ ] restore drill-ul DB si startup-ul izolat trec;
- [ ] setul de rollback leaga aceleasi JAR-uri, config, date si lumi.

## 4. Smoke Paper

- [ ] `scripts/smoke-paper-addon-lifecycle.ps1` trece pe JAR-ul Paper tinta;
- [ ] startup-ul nu are erori critice sau retry-uri SQL repetate;
- [ ] core-ul se inregistreaza si addonul se incarca dupa el;
- [ ] oprirea controlata elimina pack-urile gestionate, iar restartul fara JAR-ul addonului nu le recreeaza;
- [ ] `/ainpc audit db` trece;
- [ ] `/ainpc debugdump runtime summary` poate fi produs si sanitizat;
- [ ] smoke-urile mapping, NPC, household, quest si AI ruleaza numai pentru functiile activate;
- [ ] restartul confirma persistenta si nu dubleaza inregistrari sau pack-uri.

## 5. Porti suplimentare MySQL

- [ ] release-ul a fost testat pe versiunea MySQL/MariaDB tinta, nu numai prin traducere statica;
- [ ] TLS, utilizatorul si drepturile DB au fost revizuite;
- [ ] backup-ul a fost importat intr-o baza izolata;
- [ ] startup, CRUD critic, tranzactii si restart au trecut pe baza restaurata.

## 6. Deploy si rollback

- [ ] destinatia, calea pluginurilor si mecanismul de restart sunt furnizate extern;
- [ ] scriptul de deploy nu depinde de host, token, parola sau versiune hardcodate;
- [ ] oprirea si restartul au timeout si stare verificabila;
- [ ] setul anterior ramane disponibil;
- [ ] operatorul poate executa rollback-ul din `operations/server-admin-runbook.md`.

## Opreste release-ul daca

- exista un secret literal sau o valoare implicita reala in scripturi/documente urmarite;
- testele relevante esueaza fara o cauza acceptata si documentata;
- JAR-ul nu corespunde rolului sau versiunii declarate;
- ABI-ul API nu corespunde baseline-ului ori baseline-ul a fost actualizat fara bump/versionare justificata;
- pluginul, addonul ori DB nu se initializeaza;
- backup-ul nu corespunde backend-ului;
- restore drill-ul nu a fost executat;
- MySQL este declarat production-ready numai pe baza auditului static;
- un raport de incident datat este folosit drept dovada a starii serverului curent;
- auditul sau smoke-ul identifica pierderi, conflicte ori mutatii neasteptate.

## Dovezi de pastrat

- commit, versiune Java/Paper, `projectVersion`, `apiVersion` si hash-urile JAR;
- rezultatele testelor si inspectiei JAR;
- raportul freeze, raportul ABI si hash-ul snapshot-ului public;
- manifestul backup-ului si identificatorul backup-ului DB;
- raportul si logurile `paper-addon-lifecycle-smoke` pentru cele trei faze;
- logul de startup sanitizat;
- rezultatele audit/smoke si verificarea dupa restart;
- decizia de release si procedura de rollback.

## Legaturi

- `operations/server-admin-runbook.md`
- `operations/migration-si-backup.md`
- `operations/server-credentials.md`
- `operations/debugging-si-testare.md`
- `operations/audit.md`
- `reference/kotlin-paper-packaging-si-smoke.md`
- `reference/api-versioning-and-abi.md`
