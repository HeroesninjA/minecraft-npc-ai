# Addon Config Template

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Referinta pentru configuratia core si template-ul de addon.

## Ce acopera

- config core universal in `plugins/AINPC/config.yml`;
- config addon per addon in `plugins/AINPC/addons/<addon-id>/config.yml`;
- template livrat in JAR la `src/main/resources/config-template.yml`;
- validare stricta pentru metadata, dependinte si runtime modes.

## Reguli

- core-ul ramane generic;
- balansarea si continutul specific stau in addon;
- calea de config se obtine prin API, nu prin hardcode;
- pack-urile invalide pot fi sarite sau pot opri pornirea, in functie de setare.

## Legaturi

- `reference/addon-developer-guide.md`
- `reference/scenario-pack-schema.md`
