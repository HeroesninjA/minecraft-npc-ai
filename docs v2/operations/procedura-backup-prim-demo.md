# Procedura Backup pentru Primul Demo AINPC

Acesta este runbook-ul pentru backup inainte de modificari pe serverul Paper.

## Ce se salveaza

- configuratia si datele `plugins/AINPC/`;
- baza de date SQLite;
- JAR-urile pluginului;
- lumea Minecraft, daca e necesar pentru test.

## Cand se face

- inainte de primul start cu pluginul;
- inainte de `world save`;
- inainte de modificari manuale de configuratie;
- inainte de restarturi pentru persistenta.
