# Migrare, backup si restore

Status: runbook operational canonic.
Actualizat: 2026-07-15.

Acest runbook separa backup-ul de fisiere, backup-ul bazei de date, restore drill-ul si backfill-ul households. Niciuna dintre aceste operatii nu o inlocuieste automat pe cealalta.

## 1. Identifica backend-ul

Verifica `database.type` si pastreaza o copie a configuratiei efective fara a publica secrete:

- `sqlite` foloseste fisierul din `plugins/AINPC/`;
- `mysql` sau `mariadb` foloseste o baza externa;
- schimbarea valorii nu copiaza datele intre backend-uri;
- `/ainpc migration households` nu schimba schema si nu muta date intre backend-uri.

Detaliile implementate sunt in `reference/storage-runtime.md`.

## 2. Pregateste fereastra de backup

1. stabileste un ID de release si calea de backup pe un volum separat, cu acces restrictionat;
2. ruleaza `save-all flush` daca sunt incluse lumile;
3. opreste complet Paper inaintea copierii SQLite, a folderului pluginului sau a lumilor;
4. confirma ca procesul Java nu mai scrie in directorul serverului;
5. nu folosi folderul implicit din server drept unica copie de siguranta.

Scriptul proiectului nu opreste Paper, nu coordoneaza SQLite WAL si nu pune serverul in maintenance.

## 3. Ruleaza backup-ul de fisiere

Exemplu PowerShell:

~~~powershell
.\scripts\release-backup-restore-check.ps1 -ServerDir "D:\paper" -BackupRoot "E:\backups\ainpc" -ReleaseId "pre-release" -IncludeWorlds
~~~

Scriptul selecteaza:

- `plugins/AINPC/`, inclusiv configuratia si baza SQLite aflata acolo;
- JAR-urile AINPC din `plugins/`;
- `server.properties`;
- lumile cerute prin `-IncludeWorlds` si `-WorldNames`.

El produce arhiva ZIP, raport JSON cu manifest, dimensiuni si SHA-256, plus rezultatul restore-check-ului.

## 4. Intelege limita restore-check-ului

Restore-check-ul scriptului:

- extrage arhiva intr-un director temporar;
- verifica prezenta fisierelor din manifest;
- compara dimensiunile si hash-urile.

Nu:

- porneste Paper;
- executa un restore peste un server;
- ruleaza `PRAGMA integrity_check`;
- valideaza schema sau semantica datelor;
- captureaza ori restaureaza o baza MySQL externa;
- confirma compatibilitatea JAR-ului cu datele restaurate.

`-SkipRestoreCheck` reduce dovada si nu trebuie folosit pentru o copie declarata release-ready.

## 5. Verifica SQLite

Pentru backup-ul prin copiere:

1. opreste Paper;
2. arhiveaza intregul folder `plugins/AINPC/`, inclusiv orice fisiere WAL/SHM prezente;
3. extrage copia intr-un director izolat;
4. ruleaza un integrity check cu un instrument SQLite compatibil;
5. porneste o copie izolata a serverului pe datele restaurate;
6. ruleaza `/ainpc audit db` si smoke-urile relevante;
7. pastreaza raportul si hash-ul arhivei.

Daca downtime-ul nu este acceptabil, foloseste un instrument bazat pe SQLite Online Backup API. Proiectul nu furnizeaza in prezent acest mecanism.

## 6. Verifica MySQL

Scriptul PowerShell salveaza fisierele pluginului, nu baza MySQL. Pentru un release MySQL:

1. creeaza separat un backup logic sau fizic aprobat pentru versiunea serverului;
2. pentru tabele InnoDB, un dump logic poate folosi o tranzactie consistenta, conform documentatiei MySQL;
3. nu pune parola in argumente care ajung in history sau process list;
4. importa backup-ul intr-o baza izolata;
5. conecteaza un server Paper de staging la baza restaurata;
6. ruleaza startup, `/ainpc audit db` si smoke-urile;
7. pastreaza impreuna manifestul fisierelor si identificatorul backup-ului DB.

Un ZIP reusit fara backup-ul MySQL corespunzator nu este backup complet.

## 7. Backfill households

Comenzile implementate sunt:

~~~text
/ainpc migration households dryrun [limit]
/ainpc migration households apply [limit]
~~~

Reguli:

- comanda este administrativa;
- `dryrun` este read-only si trebuie rulat primul;
- limita trebuie sa fie pozitiva si este plafonata la `1000`;
- `apply` scrie pe household si rezident, fara o tranzactie globala pentru intregul lot;
- conflictele pot fi sarite cu warning, deci raportul trebuie revizuit;
- dupa apply, ruleaza din nou dry-run si auditurile households/DB;
- comanda este backfill de business din bindings si metadata, nu schema migration.

## 8. Restore si rollback

1. opreste serverul tinta;
2. pastreaza separat starea esuata pana la incheierea diagnosticului;
3. restaureaza JAR-urile, `plugins/AINPC/` si lumile din acelasi set de backup;
4. pentru MySQL, restaureaza separat baza asociata acelui set;
5. porneste serverul si verifica logurile de initializare;
6. ruleaza audit DB, smoke runtime si verificarea dupa restart;
7. redeschide accesul numai dupa validarea datelor.

Nu exersa prima restaurare direct pe singura copie de productie.

## Securitatea arhivei

`plugins/AINPC/` poate contine cheia OpenAI, parola DB inline sau alte date sensibile. Scriptul nu cripteaza arhiva. Foloseste stocare restrictionata, retentie definita si transport securizat.

## Referinte externe

- [SQLite Online Backup API](https://www.sqlite.org/backup.html)
- [MySQL - Using mysqldump for Backups](https://dev.mysql.com/doc/refman/8.4/en/using-mysqldump.html)
- [MySQL - Database Backup Methods](https://dev.mysql.com/doc/refman/8.4/en/backup-methods.html)

## Legaturi

- `reference/storage-runtime.md`
- `operations/server-admin-runbook.md`
- `operations/release-checklist.md`
- `operations/server-credentials.md`
