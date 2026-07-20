# Comportament natural NPC si rutine

Status: contract verificat in cod.
Actualizat: 2026-07-15.

Rutina transforma ora lumii, profilul de comportament si ancorele NPC intr-o atribuire zilnica si, optional, intr-o deplasare.

## Intrari

- NPC spawnat si neocupat;
- ora vanilla a lumii curente;
- `BehaviorProfile` selectat dupa ocupatie sau profilul `default`;
- home/work/social anchors hidratate din profil si `npc_world_bindings`;
- bias extern temporar pentru sincronizarea grupurilor sociale.

## Flux activ

1. `RoutineTimeResolver` aplica offsetul NPC-ului;
2. `RoutineEngine` alege schedule entry, slot, activitate, goal si stare;
3. `RoutineService` actualizeaza starea si publica schimbarea de activitate;
4. pentru o ancora valida si un chunk incarcat, incearca `Pathfinder.moveTo(...)`;
5. teleportul ramane fallback configurabil pentru distante mari, lumi diferite sau pathfinding nereusit.

## Protectii

- starile de dialog, trading, combat, flee, panic, hide, quest giving si following sunt sarite;
- raza de sosire, distantele si cooldown-ul sunt configurabile;
- batch processing exista prin `routine.batch_size`; valoarea absenta sau zero proceseaza tot setul;
- rutina este dezactivata implicit prin `routine.enabled=false`.

## Limite confirmate

- pathfinding-ul merge direct spre ancora; nu foloseste un graf de drumuri sau carari;
- lipsa unei ancore sau un chunk neincarcat opreste miscarea pentru tick-ul curent;
- household-ul persistent nu este citit de `RoutineEngine`;
- API-urile de pause si custom override din `RoutineCoordinator` nu influenteaza tick-ul;
- schimbarea activitatii emite eveniment public, dar core-ul nu are consumator quest/story pentru el.

## Legaturi

- `architecture/npc-world-bindings.md`
- `architecture/evita-comportamentul-robotic-prin-rutine-staggered.md`
- `architecture/harta-clase-routine.md`
- `planning/rutine-npc-si-timeline.md`
