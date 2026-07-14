# Remediere Deploy 2026-07-05

Acesta este raportul de remediere pentru incidentul de deploy din 2026-07-05.

## Rezumat

- a fost corectat scriptul PowerShell de deploy;
- a fost rezolvat `scp` permission denied prin copiere in `/tmp/` si mutare cu `sudo`;
- restartul a fost mutat de la apelul Wings API la `docker restart`;
- a fost corectat un `ClassCastException` in `FeaturePackYamlSupport.kt` pentru listele `actions` din YAML.

## Impact

- pluginul a putut fi livrat mai curat;
- incarcarea scenariilor medievale nu mai produce eroarea de tip mentionata;
- ramaneau doar probleme de infrastructura, cum ar fi imaginea Java folosita de server.
