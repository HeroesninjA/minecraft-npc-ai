# Migration si backup

Status: canonical in `docs v2`.
Actualizat: 2026-07-11.

Runbook operational pentru backup, restore-check si migration.

## Regula

- nu rulezi migration sau cleanup fara backup verificat;
- restore-check-ul se face izolat;
- raportul trebuie sa includa manifest si hash-uri.

## Acopera

- date persistente;
- backup extins pentru lumi;
- script de backup + restore-check;
- smoke test si raport.

## Legaturi

- `operations/server-admin-runbook.md`
- `operations/release-checklist.md`
