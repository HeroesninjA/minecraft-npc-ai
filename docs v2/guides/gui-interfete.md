# GUI Interfete

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este ghidul scurt pentru suprafetele GUI ale AINPC.

## Principiu

GUI-ul este strat de prezentare si control. Serviciile validate raman sursa de adevar.

## Suprafete active

- `QuestLogGui` pentru progres, tracking si detalii;
- `QuestDetailGui` pentru stage-uri, obiective si actiuni validate;
- `NpcInteractionGui` pentru interactiune NPC si rutina;
- `WorldHubGui` pentru context de mapping si ancore;
- `StoryGui` pentru state narativ read-only;
- `RoutineGui` pentru programul NPC;
- `DebugGui` si `AuditGui` pentru inspectie;
- `ConfirmActionGui` pentru actiuni riscante.

## Reguli

- click-urile valideaza din nou starea si permisiunile;
- actiunile destructive cer confirmare;
- listele mari folosesc paginare;
- inventarele AINPC sunt identificate prin holder, nu prin titlu;
- GUI-ul nu muta logica de business in inventare;
- comenzile text raman fallback.

## Ce trebuie aratat

- statusul curent;
- actiunea principala;
- warnings si erori relevante;
- contextul care explica de ce exista starea curenta.

## Legaturi

- `canonical/implementat-deja.md`
- `architecture/mapping.md`
- `architecture/ai-orchestrare-si-mecanici.md`
- `planning/questuri-avansate-v2.md`
