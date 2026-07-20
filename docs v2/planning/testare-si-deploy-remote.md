# Testare si deploy remote

Status: propunere activa, compatibila si neimplementata complet.
Actualizat: 2026-07-15.

Plan pentru un flux remote repetabil. A fost mutat din `operations/` deoarece proiectul nu are inca un pipeline unic, portabil si aprobat.

## Baseline verificat

- `deploy/deploy-ainpc.ps1`, `scripts/deploy-vps.ps1` si `scripts/deploy-ainpc-vps.sh` folosesc mecanisme diferite;
- scripturile presupun hosturi, cai, restarturi sau nume de artefact specifice unui mediu;
- unele valori RCON au fallback-uri nepotrivite pentru productie;
- scriptul shell include un credential bearer literal care trebuie rotit/revocat;
- scriptul de backup nu opreste serverul si nu captureaza MySQL;
- nu exista o singura dovada automata stop -> backup -> deploy -> startup -> smoke -> rollback.

Aceste goluri sunt backlog activ, nu o idee abandonata.

## Tinta

Un singur entry point de deploy trebuie sa:

1. primeasca targetul, caile, artefactele si metoda de restart din configuratie externa;
2. refuze credentiale lipsa si sa nu aiba fallback secret;
3. descopere artefactele dupa metadata, nu dupa o versiune hardcodata;
4. verifice hash-ul si continutul JAR inainte de transfer;
5. execute preflight, maintenance, backup si restore-check;
6. transfere intr-o locatie temporara si sa faca swap controlat;
7. astepte starea Paper cu timeout;
8. ruleze smoke read-only si teste RCON aprobate;
9. produca raport sanitizat;
10. restaureze setul anterior la esec.

## Separarea responsabilitatilor

- transport: SSH/SCP sau API de panel, ales prin adaptor;
- lifecycle: stop, status, start si timeout;
- backup: fisiere plus backup DB specific backend-ului;
- deploy: staging si swap atomic unde platforma permite;
- validare: log, audit, debugdump si RCON;
- rollback: artefacte si date din acelasi release set.

Niciun adaptor nu trebuie sa inglobeze credentiale reale.

## Matrice de testare remote

### Smoke

- serverul porneste;
- core-ul si addonul au versiunea asteptata;
- `/ainpc audit db` este disponibil;
- debugdump-ul runtime poate fi colectat si sanitizat;
- restartul pastreaza datele.

### NPC si mapping

- creare/inspectie/binding pe fixture controlat;
- dialog si fallback fara blocarea thread-ului principal;
- household status si audit;
- cleanup-ul foloseste numai date de test identificabile.

### Quest

- acceptare, progres, tracking si completare;
- restart intre doua etape;
- verificare read-only a persistentei;
- PASS/FAIL plus dovezi, fara date sensibile.

## Criterii de acceptare

- niciun secret sau target real nu este urmarit in repository;
- acelasi entry point functioneaza pe cel putin un mediu local si unul remote prin configuratie;
- dry-run-ul arata actiunile fara transfer sau restart;
- backup-ul corespunde backend-ului;
- orice esec lasa serverul fie pe versiunea veche functionala, fie oprit cu cauza clara;
- raportul include hash-uri, versiuni, timpi si smoke-uri, nu valori secrete;
- runbook-ul operational poate promova pipeline-ul numai dupa aceste dovezi.

## Legaturi

- `operations/server-admin-runbook.md`
- `operations/release-checklist.md`
- `operations/server-credentials.md`
- `operations/migration-si-backup.md`
