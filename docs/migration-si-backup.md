# Migration si Backup

Actualizat: 2026-05-24

Status: runbook operational pentru backup, restore-check si migration pe server Paper cu AINPC. Nu inlocuieste backup-ul complet al providerului de hosting.

## Scop

Acest document stabileste regula minima pentru orice operatie care poate modifica date persistente:

```text
server oprit
-> backup cu manifest si hash-uri
-> restore-check izolat
-> migration/dry-run
-> smoke test
-> raport
```

Nu rula migration sau cleanup pe date reale fara backup verificat.

## Ce Se Considera Date Persistente

Backup minim AINPC:

```text
plugins/AINPC/config.yml
plugins/AINPC/quests.yml
plugins/AINPC/ainpc_data.db
plugins/AINPC/debug-dumps/
plugins/*ainpc*.jar
server.properties
```

Backup extins, cand operatia schimba mapping, NPC-uri sau entitati in lume:

```text
world/
world_nether/
world_the_end/
```

Pentru servere mari, backup-ul lumilor trebuie facut prin mecanismul hostingului sau storage snapshot. Scriptul local este potrivit pentru servere de test, demo si snapshot-uri controlate.

## Script Backup Cu Restore-Check

Script:

```text
scripts/release-backup-restore-check.ps1
```

Backup minim:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-backup-restore-check.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -ReleaseId "lts-rc1"
```

Backup cu lumi:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-backup-restore-check.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -ReleaseId "lts-rc1" `
  -IncludeWorlds `
  -WorldNames world,world_nether,world_the_end
```

Backup intr-un folder separat:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-backup-restore-check.ps1 `
  -ServerDir "C:\Minecraft\paper-test" `
  -BackupRoot "D:\AINPCBackups" `
  -ReleaseId "lts-rc1"
```

Output:

```text
ainpc-release-backup-<releaseId>-<timestamp>.zip
ainpc-release-backup-<releaseId>-<timestamp>-report.json
ainpc-release-backup-<releaseId>-<timestamp>-restore-check.json
```

Raportul contine path-uri, dimensiuni si SHA256. Nu contine continutul fisierelor si nu salveaza parole in clar in JSON, dar arhiva poate contine config si DB reale; trateaz-o ca artefact sensibil.

## Restore-Check

Implicit, scriptul face restore intr-un director temporar si verifica:

- `backup-manifest.json` exista in arhiva;
- fiecare fisier din manifest exista dupa extract;
- dimensiunea fisierului restaurat este identica;
- SHA256 este identic.

Daca restore-check esueaza, backup-ul nu este acceptabil pentru release.

Foloseste `-SkipRestoreCheck` doar cand ai alt mecanism de verificare. Pentru LTS, nu bifa backup-ul ca valid fara restore-check.

## Reguli Pentru Migration

Inainte de migration:

1. Opreste serverul.
2. Ruleaza backup cu restore-check.
3. Noteaza `backup_zip` si `backup_zip_sha256` din raport.
4. Porneste serverul doar daca migration-ul este executat prin comenzi AINPC sau procedura documentata.
5. Ruleaza mai intai dry-run daca exista.

Comenzi care cer atentie:

```text
/ainpc migration households dryrun
/ainpc migration households apply
/ainpc repair batch <batchKey> dryrun
/ainpc repair batch <batchKey> apply
/ainpc repair batch <batchKey> mark-failed
```

Nu edita `plugins/AINPC/ainpc_data.db` cu serverul pornit.

## Rollback

Rollback rapid dupa un release esuat:

1. Opreste serverul.
2. Pastreaza o copie a starii esuate pentru investigatie, daca spatiul permite.
3. Extrage backup-ul validat intr-un director temporar.
4. Inlocuieste `plugins/AINPC/` si JAR-urile AINPC din `plugins/`.
5. Restaureaza lumile numai daca operatia a modificat entitati, structuri sau mapping in lume si backup-ul include lumile.
6. Porneste serverul.
7. Ruleaza `/ainpc audit all`.
8. Ruleaza smoke-ul relevant din `release-checklist.md`.
9. Noteaza cauza rollback-ului si arhiva folosita.

Nu face rollback partial de DB peste o lume care pastreaza entitati noi create dupa backup, decat daca stii exact ce entitati trebuie curatate.

## Criteriu LTS

Pentru prima versiune LTS, un release nu este gata daca:

- nu exista backup zip cu SHA256;
- nu exista restore-check trecut;
- raportul de release nu contine `Backup path`;
- rollback-ul nu poate fi explicat fara editare DB live;
- migration-ul necesar nu are dry-run sau procedura de verificare.

## Legaturi

- `release-checklist.md`
- `server-admin-runbook.md`
- `prevenire-duplicare-npc.md`
- `debugging-si-testare.md`
