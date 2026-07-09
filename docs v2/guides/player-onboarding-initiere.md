# Player Onboarding si Initiere

Status: canonical in `docs v2`.
Actualizat: 2026-05-09.

Acesta este rezumatul pentru prima intrare a jucatorului si resetarea controlata a profilului.

## Scop

- prima intrare trebuie sa fie clara, idempotenta si usor de reluat;
- starter kit-ul nu se poate revendica de doua ori accidental;
- resetul trebuie confirmat explicit;
- onboarding-ul nu trebuie sa stearga automat progresul fara optiune separata.

## Stare curenta

- exista deja baza folosita de `ProgressionService`, tutorialul `T01`, GUI-ul generic si audit/debugdump;
- lipsesc inca serviciul dedicat de profil, tabela `player_profiles`, GUI-ul de initiere si starter kit-ul idempotent;
- sistemul trebuie tratat separat de profilurile NPC.

## Flux

`join -> detectare profil lipsa -> initiere -> alegere -> starter kit -> tutorial/progression`

## Model minim

- profil player;
- audit pentru resetari;
- jurnal separat pentru starter kit claims;
- comenzi admin/player pentru inspectie si reset.

## Reguli

- GUI-ul este doar prezentare;
- serviciile persistente decid ce se scrie in DB;
- resetul trebuie sa ramana controlat si auditable;
- onboarding-ul se poate lega de tutoriale si progresie, dar nu trebuie sa dubleze quest logic.

## Legaturi

- `architecture/story-context-service.md`
- `planning/questuri-avansate-v2.md`
- `canonical/implementat-deja.md`
