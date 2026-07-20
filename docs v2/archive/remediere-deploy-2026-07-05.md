# Remediere deploy - snapshot 2026-07-05

Status: incident inchis, pastrat istoric.

Acest document descrie o interventie punctuala din 2026-07-05. Nu stabileste configuratia, versiunile sau procedura de deploy curente.

## Rezumat istoric

- scriptul PowerShell de deploy folosit atunci a fost corectat;
- eroarea `scp permission denied` a fost evitata prin copiere in `/tmp/` si mutare cu `sudo`;
- restartul a fost mutat de la un apel Wings API la `docker restart`;
- a fost corectat un `ClassCastException` in `FeaturePackYamlSupport.kt` pentru listele `actions` din YAML.

## Impact raportat atunci

- pluginul a putut fi livrat in mediul respectiv;
- incarcarea scenariilor medievale nu mai producea eroarea de tip mentionata;
- mai ramaneau probleme de infrastructura, inclusiv imaginea Java.

## Limita

Nu reutiliza hosturi, cai, credentiale, versiuni sau concluzii de sanatate din acest snapshot. Runbook-ul curent este `../operations/server-admin-runbook.md`, iar lucrul remote ramas este in `../planning/testare-si-deploy-remote.md`.
