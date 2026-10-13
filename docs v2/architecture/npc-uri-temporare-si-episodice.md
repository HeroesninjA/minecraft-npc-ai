# NPC-uri temporare si episodice

Status: implementare partiala, cu gap-uri de lifecycle confirmate.
Actualizat: 2026-07-15.

Actorii sunt definiti in scenarii si spawnati de `ScenarioEngine`. Etichetele de lifecycle nu formeaza inca o politica completa de persistenta, simulare si cleanup.

## Contracte disponibile

- lifecycle: `PERMANENT`, `TEMPORARY`, `EPISODIC`, `SCENE_ONLY`;
- persistenta: `FULL`, `LIGHT`, `RUNTIME_ONLY`;
- simulare: `FULL`, `LIGHT`, `NONE`;
- interactiune: `FULL_AI`, `QUEST_ONLY`, `SHOP_ONLY`, `SCENE_ONLY`, `MINIMAL`;
- spawn policy: auto, stage sau manual;
- ownership, despawn rule, durata si tag-uri temporare.

## Comportament activ

- definitia YAML este aplicata pe obiectul `AINPC`;
- `DialogManager` consulta profilul de interactiune inainte de accesarea istoricului sau a providerului AI; `MINIMAL` si `SCENE_ONLY` blocheaza dialogul direct, iar `QUEST_ONLY` si `SHOP_ONLY` il permit numai cand conditia lor minima este disponibila;
- refuzurile de profil au variante rotite, aplica cooldown si nu sunt persistate ca dialog sau relatie;
- `VILLAGER/HUMANOID` folosesc Villager, `MONSTER` foloseste Zombie, iar `SPIRIT/MARKER` ArmorStand;
- `ANIMAL/NONE` folosesc adaptorul no-op si nu ofera spawn real;
- orice mod diferit de `RUNTIME_ONLY` apeleaza acelasi `saveNPC(...)`; `FULL` si `LIGHT` nu au politici distincte;
- `duration_seconds` programeaza despawn-ul;
- finalul scenariului incearca despawn pentru toti actorii spawnati.

## Gap-uri confirmate

- `NpcSimulationMode` nu este consultat de simulare sau rutina;
- `QUEST_ONLY` verifica o oferta asociata actorului curent prin UUID, ID DB sau nume; `SHOP_ONLY` cere un magazin configurat pentru ocupatie, tipul/ID-ul cladirii mapate la `workAnchor` daca magazinul are `place_types`/`place_ids`, permisiunea jucatorului si cel putin o oferta cu schimb valid structural; nu valideaza plata sau stocul;
- `despawn_rule`, ownership-ul si tag-urile nu conduc cleanup-ul;
- `TEMPORARY` nu intra in conditia de unregister folosita pentru `EPISODIC`, `SCENE_ONLY` si `RUNTIME_ONLY`;
- unregister-ul scoate NPC-ul doar din cache; nu sterge un rand deja persistat;
- lifecycle, persistence mode, simulation mode si ownership nu sunt salvate in `npc_profiles.profile_data` si revin la valorile implicite dupa reload;
- un actor `LIGHT` persistent poate ramane in DB fara politica automata de stergere.

## Regula de folosire curenta

- foloseste `RUNTIME_ONLY` pentru actori care nu trebuie sa lase rand DB;
- trateaza `LIGHT`, modurile de simulare si `despawn_rule` drept contracte incomplete;
- nu promite memorie limitata sau cost redus pana cand modurile sunt conectate in consumatori;
- verifica explicit cleanup-ul la expirare, final de scenariu si restart.

## Legaturi

- `architecture/simulation-service.md`
- `architecture/dialog-si-conversatii.md`
- `planning/rutine-npc-si-timeline.md`
