# Addon Developer Guide

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este ghidul scurt pentru autorii de addonuri.

## Ce trebuie sa stie un addon

- depinde de `ainpc-api`, nu de internals;
- declara capabilitati si dependinte;
- livreaza config propriu;
- poate fi dezactivat fara coruperea datelor;
- foloseste contracte stabile si tipuri read-only.

## Ce nu trebuie sa faca

- sa scrie direct in core;
- sa trateze internals ca API public;
- sa adauge dependinte obligatorii pentru core;
- sa ascunda conflictul de namespace sau de data.

## Legaturi

- `reference/documentatie-api.md`
- `canonical/constitutie-proiect.md`
- `reference/scenario-pack-schema.md`
