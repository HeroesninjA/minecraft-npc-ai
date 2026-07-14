# Harta claselor pentru GUI

Status: canonical in `docs v2`.
Actualizat: 2026-06-29.

Aceasta harta urmareste subsistemul GUI si ecranele sale principale.

## Noduri principale

- `GuiService`;
- `GuiSessionManager`;
- `GuiScreen`;
- `MainHubGui`;
- `QuestLogGui`;
- `QuestDetailGui`;
- `StoryGui`;
- `WorldHubGui`;
- `NpcInteractionGui`;
- `NpcManagerGui`;
- `RoutineGui`;
- `ShopGui`;
- `AuditGui`;
- `DebugGui`;
- `QuestAuthoringGui`;
- `ConfirmActionGui`.

## Flux

- serviciul central deschide si valideaza ecranele;
- sesiunile pastreaza starea de navigare;
- hub-urile deschid zonele specializate;
- confirmarea protejeaza actiunile riscante.

## Reguli

- GUI-ul este strat de prezentare;
- serviciile validate sunt sursa de adevar;
- click-urile destructive cer confirmare;
- comenzile text raman fallback.

## Legaturi

- `reference/harta-clase-cod.md`
- `architecture/harta-clase-world.md`
- `architecture/harta-clase-quest.md`
