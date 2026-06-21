# Harta claselor pentru quest

Actualizat: 2026-06-21

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste numai subsistemul `quest` si traseul lui de lucru: director, ancore, authoring, progresie, context narativ si stare persistenta.

Nu este un inventar complet al tuturor claselor din quest. Este o harta de lucru pentru nodurile care decid ce questuri sunt selectate, cum se rezolva ancorele si cum se tine evidenta progresului.

## Noduri principale

- `QuestDirector` -> decide ce are prioritate si ce tip de cerere trebuie generat
- `QuestAnchorResolver` -> rezolva ancorele de quest in raport cu world-ul si NPC-urile
- `QuestAuthoringService` -> analizeaza si rezuma definitiile de quest
- `ProgressionService` -> runtime-ul generic de progres, ancore, obiective si tracking
- `StoryContextService` -> context narativ si semnale pentru selectia sau explicarea questului
- `StoryStateService` -> stare persistenta si evenimente de story asociate questurilor
- `QuestDirectorRequest` -> intrarea pentru decizia directorului
- `QuestDirectorDecision` -> rezultatul deciziei

## Flux principal

`QuestDirectorRequest` -> `QuestDirector` -> `QuestDirectorDecision`

`QuestAnchorResolver` foloseste world-ul si NPC-urile pentru a lega obiectivele la ancore concrete.

`QuestAuthoringService` analizeaza definirea si produce rezumatul de authoring.

`ProgressionService` gestioneaza obiectivele, ancorele si progresul real.

`StoryContextService` si `StoryStateService` adauga context si istoric pentru questurile care trebuie explicate sau persistate.

## Relatii utile

- `QuestDirector` decide daca o cerere trebuie condusa de progres, de story sau de contextul lumii.
- `QuestAnchorResolver` este podul dintre obiectivele textuale si lumea semantica.
- `ProgressionService` este stratul care pastreaza progresul, nu doar intentia.
- `StoryContextService` face questul inteligibil pentru AI si debugging.
- `StoryStateService` tine evenimentele si starea persistenta care sustin questurile avansate.

## Cum se citeste

1. Incepe cu `QuestDirector`.
2. Continua cu `QuestAnchorResolver` pentru ancore.
3. Foloseste `ProgressionService` pentru progres si tracking.
4. Treci la `StoryContextService` si `StoryStateService` pentru context si persistenta.
5. Foloseste `QuestAuthoringService` daca te intereseaza inspectia si rezumatul definitiilor.

## Nota

Daca vrei doar traseul intre module si pachete, foloseste harta de pachete. Daca vrei relatiile dintre clasele quest, acesta este documentul potrivit.
