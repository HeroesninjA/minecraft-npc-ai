# Server Admin Runbook

Status: runbook operational canonic.
Actualizat: 2026-07-15.

Procedura pentru instalare, upgrade, verificare si rollback pe Paper. Nu este ghid de design si nu transforma scripturile specifice unui VPS in pipeline canonic.

## Artefacte

- instaleaza `ainpc-core-plugin-<version>.jar`;
- instaleaza optional `ainpc-scenario-medieval-<version>.jar`;
- nu copia `ainpc-api-<version>.jar` in `plugins/`: nu este plugin Paper si nu contine `plugin.yml`;
- core-ul trebuie incarcat inaintea addonului medieval;
- nu lasa simultan versiuni vechi si noi ale aceluiasi JAR.

Contractul complet este in `reference/kotlin-paper-packaging-si-smoke.md`.

## Instalare curata

1. foloseste un Paper si un Java compatibile cu build-ul curent, nu cu un raport istoric;
2. construieste si inspecteaza JAR-urile conform checklistului de packaging;
3. opreste Paper;
4. copiaza core-ul si addonul optional in `plugins/`;
5. porneste Paper si verifica lipsa erorilor de classloading sau duplicate;
6. confirma crearea `plugins/AINPC/config.yml`;
7. opreste serverul pentru configurarea initiala sensibila;
8. selecteaza backend-ul si configureaza credentialele fara a le salva in repo;
9. porneste din nou si verifica initializarea DB;
10. ruleaza `/ainpc audit db` si `/ainpc debugdump runtime summary`;
11. executa smoke-urile domeniilor activate.

## Backend

- SQLite este implicit si pastreaza fisierul in `plugins/AINPC/`;
- MySQL/MariaDB necesita configurare explicita, backup separat si validare pe server real;
- schimbarea `database.type` nu migreaza datele;
- nu edita ori copia SQLite live;
- nu considera pool-ul Hikari dovada ca runtime-ul curent executa operatii SQL in paralel.

Citeste `reference/storage-runtime.md` si `operations/migration-si-backup.md` inaintea unui upgrade cu date reale.

## Upgrade controlat

1. identifica versiunea sursa, backend-ul si setul de JAR-uri;
2. creeaza backup complet si finalizeaza restore-check-ul;
3. pentru MySQL, creeaza si backup-ul DB separat;
4. opreste Paper;
5. inlocuieste JAR-urile ca un singur set si pastreaza setul anterior pentru rollback;
6. compara configuratia existenta cu noile valori implicite fara a suprascrie secretele;
7. porneste serverul si inspecteaza primul ciclu complet de startup;
8. ruleaza audit DB, smoke Paper si testele functiilor afectate;
9. reporneste si verifica persistenta;
10. semneaza release-ul numai dupa confirmarea rollback-ului.

## Diagnostic minim

- plugin absent: verifica `plugin.yml`, Java, dependintele si logul complet;
- addon absent: confirma prezenta core-ului si ordinea Paper `depend`;
- DB esuata: nu continua cu mutatii; pastreaza logul, opreste serverul si restaureaza doar dupa diagnostic;
- date lipsa: confirma backend-ul efectiv si calea/configuratia restaurata;
- SQL MySQL esuat: trateaza-l ca defect de compatibilitate, nu modifica manual datele pentru a ascunde eroarea;
- runtime suspect: colecteaza un debugdump sanitizat si auditurile read-only.

## Deploy remote

Scripturile actuale din `scripts/` si `deploy/` contin presupuneri diferite despre host, cai, restart, RCON si numele artefactului. Ele nu sunt o procedura portabila si nu trebuie rulate cu valorile urmarite in repository.

Roadmap-ul de consolidare este `planning/testare-si-deploy-remote.md`. Politica de secrete este `operations/server-credentials.md`.

## Rollback

1. opreste Paper;
2. conserva starea esuata separat;
3. restaureaza setul coerent JAR + config/date + lume;
4. restaureaza backup-ul MySQL asociat, daca este cazul;
5. porneste izolat sau cu acces restrictionat;
6. ruleaza audit si smoke;
7. redeschide serverul numai dupa validare.

## Legaturi

- `operations/release-checklist.md`
- `operations/migration-si-backup.md`
- `operations/debugging-si-testare.md`
- `operations/audit.md`
- `reference/kotlin-paper-packaging-si-smoke.md`
