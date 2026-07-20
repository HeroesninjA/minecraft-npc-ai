# Player onboarding si initiere

Status: propunere activa, neimplementata cap-coada.
Verificat fata de runtime: 2026-07-16.

Acest plan descrie initierea profilului de player. Nu trebuie confundat cu scenariile quest `TUTORIAL` sau cu aliasul de comanda `onboarding` deja existent.

## Baseline implementat

- `/tutorial`, `/tutorials` si `/onboarding` ruteaza catre progresie/quest si folosesc aliasul `onboarding:T01`;
- `TUTORIAL` exista ca tip de scenariu si filtru in quest log;
- `ProgressionService` poate urmari progresul definitiilor incarcate;
- `/ainpc gui` alege hub-ul din permisiuni prin `GuiRole`;
- auditul si debugdump-ul pot inspecta starea existenta.

Aceste elemente nu formeaza un pipeline automat de prima intrare.

## Lipsuri verificate

- nu exista model sau serviciu dedicat `PlayerProfile` pentru initiere;
- nu exista tabela `player_profiles`;
- nu exista jurnal idempotent pentru revendicarea starter kit-ului;
- nu exista GUI dedicat alegerilor initiale si resetarii profilului;
- nu exista flux `PlayerJoinEvent -> detectare profil -> initiere -> starter kit`;
- aliasul quest `onboarding` nu creeaza si nu reseteaza un profil de player.

## Model propus

- profil player separat de profilurile si memoriile NPC;
- stare de initiere versionata;
- alegeri initiale cu schema explicita;
- ledger pentru starter kit claims;
- audit pentru create/reset/replay;
- legatura optionala spre un scenariu tutorial, fara duplicarea motorului quest.

## Flux propus

`join -> detectare profil -> initiere GUI/command -> validare alegere -> commit profil -> claim starter kit -> pornire tutorial`

Fiecare pas trebuie sa fie reluabil dupa disconnect si idempotent dupa retry.

## Reset

- resetul profilului, progresiei si starter kit-ului sunt operatii separate;
- orice reset distructiv cere confirmare si audit;
- resetul nu trebuie sa stearga implicit progresia quest fara optiune explicita;
- starter kit-ul nu poate fi revendicat din nou doar prin redeschiderea GUI-ului;
- rollback-ul trebuie definit pentru commit partial.

## Criteriu de implementare

- schema si migrarile exista pentru backend-urile suportate;
- join-ul nu blocheaza thread-ul Paper cu I/O lung;
- doua join-uri ori click-uri concurente nu dubleaza claim-ul;
- playerul poate relua initierea intrerupta;
- adminul poate inspecta si reseta separat fiecare componenta;
- scenariul tutorial porneste numai dupa commit sau are compensare documentata;
- testele acopera first join, rejoin, retry, reset si backend failure.

## Legaturi

- `architecture/progression-service.md`
- `reference/progression-events-onboarding-stack.md`
- `guides/playable-village-ux.md`
- `reference/gui-stack.md`
- `planning/questuri-avansate-v2.md`
