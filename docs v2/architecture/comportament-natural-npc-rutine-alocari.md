# Comportament natural NPC si rutine

Status: contract verificat in cod.
Actualizat: 2026-10-12.

Rutina transforma ora lumii, profilul de comportament si ancorele NPC intr-o atribuire zilnica si, optional, intr-o deplasare.

## Intrari

- NPC spawnat si neocupat;
- ora vanilla a lumii curente;
- `BehaviorProfile` selectat dupa ocupatie sau profilul `default`;
- home/work/social anchors hidratate din profil si `npc_world_bindings`;
- bias extern temporar pentru sincronizarea grupurilor sociale.
- mediul local actualizat la fiecare evaluare de rutină pentru NPC-urile evaluate.
- extraversia NPC-ului, ancora sociala si probabilitatea `socialize_chance` din profil.
- starea story efectiva a regiunii si place-ului curent, cu prioritate pentru starea persistenta.

## Flux activ

1. `RoutineTimeResolver` aplica offsetul NPC-ului;
2. o regula `world_state_rules` configurata poate alege slotul, activitatea, goal-ul si starea NPC-ului pe baza cheii story efective din regiune/place si/sau a modului efectiv al regiunii; aceasta regula are prioritate fata de schedule;
3. in lipsa unei reguli story potrivite, `RoutineEngine` alege schedule entry, slot, activitate, goal si stare;
   daca profilul permite reactiile meteo, vremea extrema trimite NPC-ul acasa in timpul zilei, cu conditia sa aiba ancora `home`; slotul de noapte si o atribuire deja orientata spre casa raman prioritare;
   in intervalul `phase_ticks.social_start`–`social_end`, sloturile libere `HOME`/`IDLE` pot deveni `SOCIAL`; sansa de baza din profil este ajustata de extraversie si trasa determinist per NPC si zi;
4. `RoutineService` actualizeaza starea si publica schimbarea de activitate;
5. pentru o ancora valida si un chunk incarcat, incearca `Pathfinder.moveTo(...)`;
6. teleportul ramane fallback configurabil pentru distante mari, lumi diferite sau pathfinding nereusit.

## Protectii

- starile de dialog, trading, combat, flee, panic, hide, quest giving si following sunt sarite;
- raza de sosire, distantele si cooldown-ul sunt configurabile;
- batch processing exista prin `routine.batch_size`; valoarea absenta sau zero proceseaza tot setul;
- rutina este dezactivata implicit prin `routine.enabled=false`.
- `weather_reactions` controleaza adapostirea pentru furtuna, inghet si caldura extrema; textul activitatii poate fi suprascris prin `routine_texts.weather_shelter` in profil.
- `socialize_chance` controleaza iesirile in timpul liber, iar `phase_ticks.social_start`/`social_end` stabilesc intervalul; extraversia scaleaza sansa cu factorul `0.5 + extraversion`, iar textul se poate configura prin `routine_texts.personality_social`.
- `routine.story_state_cache_seconds` limiteaza vechimea citirilor persistente; implicit starea este recitita cel mult la fiecare 15 secunde pentru fiecare regiune/place.
- GUI-ul de rutina si debug dump-ul folosesc `RoutineService.previewDay(...)`, care recalculeaza snapshot-ul si starea story inaintea previzualizarii; pentru NPC fara locatie, contextul efectiv este golit.
- fiecare regula `world_state_rules` contine `scope` (`REGION` sau `PLACE`), `slot`, `activity_key` si `story_state`, `story_mode` sau ambele; cand ambele sunt completate trebuie sa se potriveasca simultan. `story_mode` accepta `static`, `evolutive` sau `rotative` si se aplica numai regiunilor; `state` este optional.

Exemplu de profil: cheia persistenta sau cea din mapping `festival` trimite NPC-ul la ancora sociala:

```yaml
routine_texts:
  world_festival: "participa la sarbatoarea din regiune"
world_state_rules:
  - scope: "REGION"
    story_state: "festival"
    slot: "SOCIAL"
    activity_key: "world_festival"
    state: "SOCIALIZING"
```

Pentru a folosi modul regiunii in locul cheii story, se poate configura de exemplu `story_mode: "ROTATIVE"` fara `story_state`; pentru a combina filtrele, se completeaza ambele.

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
