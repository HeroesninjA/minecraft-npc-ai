# Rutine staggered

Status: comportament verificat in cod.
Actualizat: 2026-07-15.

Variatia curenta este determinista. Documentatia nu presupune jitter zilnic sau coordonare pe household.

## Implementat

- fiecare NPC primeste un offset stabil calculat din `UUID|name`, modulo 1800 tick-uri;
- profilele de comportament pot avea ferestre diferite pe rol sau ocupatie;
- `RoutineCoordinator` grupeaza NPC-urile dupa chunk-ul ancorei sociale;
- grupurile cu minimum doi membri primesc temporar acelasi bias, modulo 300 tick-uri;
- offsetul se recalculeaza determinist si nu necesita persistenta.

## Neimplementat

- jitter aleator nou in fiecare zi;
- plecari coordonate dupa household sau familie;
- valuri calculate la nivel de sat;
- calendar, anotimp sau sarbatoare ca sursa pentru offsetul de baza.

## Efect real

Offsetul stabil reduce tranzitiile simultane, iar biasul de grup apropie ferestrele sociale. Nu garanteaza distributie uniforma si nu este un scheduler global.

## Legaturi

- `architecture/comportament-natural-npc-rutine-alocari.md`
- `architecture/simulation-service.md`
- `planning/rutine-npc-si-timeline.md`
