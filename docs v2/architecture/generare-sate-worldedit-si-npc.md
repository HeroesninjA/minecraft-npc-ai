# Generare de sate cu WorldEdit si NPC-uri

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Directia pentru generare de sate cu WorldEdit, apoi asociere cu NPC-uri si mapping semantic.

## Pipeline

- planificare sat;
- alocare NPC-uri;
- constructie prin WorldEdit sau fallback;
- creare region/place/node;
- legare NPC-uri la case si locuri de munca;
- audit final.

## Regula

- WorldEdit executa constructia;
- core-ul decide planul si validarea;
- mapping-ul semantic ramane in AINPC.

## Legaturi

- `architecture/generare-sate-fara-worldedit.md`
- `architecture/worldedit-integration-contract.md`
