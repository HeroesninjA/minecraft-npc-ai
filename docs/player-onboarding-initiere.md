# Player Onboarding si Initiere

Actualizat: 2026-05-09

## Scop

Acest document descrie sistemul viitor pentru ce se intampla prima data cand un jucator intra pe server si pentru resetarea controlata a profilului sau.

Numele tehnic recomandat:

```text
PlayerProfileService
PlayerOnboardingService
StarterKitService
OnboardingGui
```

Acest sistem nu trebuie confundat cu profilurile NPC (`npc_profiles`). Profilul de player este despre alegerile jucatorului, progresul de initiere si itemele de start.

## Principiu

Prima intrare trebuie sa fie:

```text
join -> detectare profil lipsa -> initiere -> alegere caracteristici -> starter kit -> tutorial/progression
```

Reguli:

- initierea trebuie sa fie idempotenta;
- starter kit-ul nu se poate revendica de doua ori accidental;
- resetul trebuie sa fie confirmat explicit;
- resetul de profil nu trebuie sa stearga automat questurile, story state-ul sau progresul fara optiune separata;
- GUI-ul este doar prezentare; serviciile persistente decid ce se scrie in DB.

## Ce exista deja relevant

Exista deja mecanici care pot fi folosite ca baza:

- `ProgressionService` si filtrele pentru `tutorial`;
- mecanica `onboarding` prin tutorialul `T01`;
- GUI generic pentru quest/progression;
- listener-e pentru player join/quit in zona de interactiuni;
- audit/debugdump extinse pentru date persistente;
- comenzi cu confirmare pentru actiuni riscante.

Ce lipseste:

- tabela dedicata `player_profiles`;
- serviciu pentru creare/reset profil player;
- GUI de initiere;
- starter kit idempotent;
- comenzi admin/player pentru reset profil;
- audit pentru profiluri incomplete.

## Model DB propus

### `player_profiles`

Sursa de adevar pentru profilul jucatorului.

Campuri recomandate:

| Camp | Rol |
|---|---|
| `player_uuid` | cheia principala |
| `player_name` | ultimul nume cunoscut |
| `first_join_at` | timestamp prima intrare observata |
| `last_seen_at` | timestamp ultima intrare |
| `profile_created` | 0/1, daca initierea a fost finalizata |
| `profile_version` | versiune schema/alegeri |
| `origin_id` | origine aleasa: sat, drumet, ucenic etc. |
| `archetype_id` | rol/arhetip: luptator, negustor, cercetas etc. |
| `trait_ids` | JSON array cu trasaturi alese |
| `starter_kit_id` | kit ales sau acordat |
| `starter_kit_claimed` | 0/1, previne duplicarea itemelor |
| `onboarding_status` | `NEW`, `IN_PROGRESS`, `COMPLETE`, `RESET_REQUIRED` |
| `onboarding_step` | pasul curent din GUI/flow |
| `created_at` | timestamp creare |
| `updated_at` | timestamp update |

### `player_profile_resets`

Audit pentru resetari.

Campuri recomandate:

| Camp | Rol |
|---|---|
| `id` | autoincrement |
| `player_uuid` | player resetat |
| `reset_type` | `PROFILE_ONLY`, `ONBOARDING`, `QUEST_PROGRESS`, `FULL_PLAYER` |
| `requested_by` | admin/player/system |
| `reason` | motiv text scurt |
| `snapshot_before` | JSON compact cu valorile vechi |
| `created_at` | timestamp |

### `starter_kit_claims`

Jurnal separat pentru iteme de start.

Campuri recomandate:

| Camp | Rol |
|---|---|
| `player_uuid` | player |
| `kit_id` | kit acordat |
| `claimed_at` | timestamp |
| `source` | onboarding/admin/reset |
| `payload` | JSON cu itemele acordate |

Cheie recomandata:

```text
PRIMARY KEY(player_uuid, kit_id)
```

Astfel kit-ul nu poate fi dat de doua ori fara reset explicit.

## Flow prima intrare

Flux propus:

```text
PlayerJoinEvent
-> PlayerProfileService.getOrCreate(player)
-> daca profile_created=false, onboarding_status=NEW/IN_PROGRESS
-> deschide OnboardingGui dupa delay scurt
-> jucatorul alege origine/arhetip/trasaturi
-> PlayerOnboardingService.finalizeProfile(...)
-> StarterKitService.claimStarterKit(...)
-> porneste sau ofera tutorialul T01 prin ProgressionService/ScenarioEngine
```

Detalii:

- delay-ul scurt evita deschiderea GUI-ului prea devreme in tick-ul de join;
- daca playerul inchide GUI-ul, statusul ramane `IN_PROGRESS`;
- la urmatorul join se reia pasul neterminat;
- adminul poate forta finalizarea sau resetarea.

## GUI de initiere

`OnboardingGui` trebuie sa fie simplu:

1. alegere origine;
2. alegere arhetip;
3. alegere 1-3 trasaturi;
4. preview kit;
5. confirmare finala.

Exemple de optiuni:

| Categorie | Exemple |
|---|---|
| Origine | localnic, calator, ucenic, supravietuitor, negustor |
| Arhetip | lucrator, cercetas, aparator, mestesugar, diplomat |
| Trasaturi | harnic, curios, prudent, indraznet, empatic |
| Kit | basic, builder, explorer, trader, defender |

Reguli GUI:

- nu acorda iteme la simplul click pe preview;
- confirmarea finala este singurul pas care scrie profilul complet;
- daca playerul nu are permisiune sau profilul s-a schimbat intre timp, click-ul trebuie respins;
- toate alegerile trebuie salvate prin serviciu, nu direct in GUI.

## Starter kit

`StarterKitService` trebuie sa fie idempotent.

Reguli:

- verifica `starter_kit_claims` inainte sa dea iteme;
- daca inventarul este plin, fie pune itemele in overflow controlat, fie amana claim-ul;
- nu marca `starter_kit_claimed=true` pana cand itemele au fost acordate cu succes;
- logheaza kit-ul acordat in audit/debugdump.

Exemple de kit-uri:

| Kit | Iteme posibile |
|---|---|
| `basic` | paine, lemn, unealta simpla |
| `builder` | blocuri de baza, unealta, mancare |
| `explorer` | busola, mancare, torte |
| `trader` | emerald mic, mancare, item de schimb |
| `defender` | scut simplu, mancare, unealta |

## Reset profil

Resetul trebuie separat pe tipuri.

Tipuri recomandate:

| Reset | Ce sterge | Ce pastreaza |
|---|---|---|
| `PROFILE_ONLY` | origine, arhetip, trasaturi, status onboarding | quest progress, story, relatii |
| `ONBOARDING` | profil + starter kit claim optional | quest progress daca adminul nu cere altfel |
| `QUEST_PROGRESS` | progresii player | profil, starter kit |
| `FULL_PLAYER` | profil, progresii, starter kit claim, story flags player | doar date globale/world |

Comenzi propuse:

```text
/ainpc player profile <player>
/ainpc player reset-profile <player> confirm
/ainpc player reset-onboarding <player> confirm
/ainpc player reset-quests <player> confirm
/ainpc player reset-full <player> confirm
```

Pentru player:

```text
/profile
/profile reset request
```

Reset self-service trebuie sa fie configurabil si probabil dezactivat implicit pe servere publice.

## Integrare cu quest/progression

Onboarding-ul de profil si tutorialul `T01` sunt doua lucruri diferite:

| Sistem | Rol |
|---|---|
| Player onboarding | creeaza profilul si acorda kit-ul |
| Tutorial `T01` | invata jucatorul cum foloseste quest log, NPC si mapping |
| ProgressionService | tine progresul tutorialului |
| ScenarioEngine | executa accept/progress/complete |

Dupa finalizarea profilului:

1. se ofera sau activeaza `T01`;
2. daca exista NPC/tutorial board relevant, se seteaza tracking;
3. se poate trimite playerul catre primul NPC sau loc semantic;
4. story event optional: `PLAYER_ONBOARDED`.

## Audit si debugdump

Audit propus:

- player fara `player_profiles`;
- profil `IN_PROGRESS` de prea mult timp;
- `starter_kit_claimed=true` fara rand in `starter_kit_claims`;
- rand in `starter_kit_claims` pentru player fara profil;
- profil `COMPLETE` fara tutorial T01 oferit/completat;
- reseturi multe pentru acelasi player;
- JSON invalid in `trait_ids` sau payload kit.

Debugdump propus:

```text
player-profiles.json
starter-kit-claims.json
player-profile-resets.json
```

Continut minim:

- total profiluri;
- status counts;
- kit counts;
- profiluri incomplete;
- ultimele reseturi;
- progres tutorial T01 legat de fiecare player.

## Config propus

```yaml
player_onboarding:
  enabled: true
  open_gui_on_first_join: true
  require_profile_before_quest: false
  allow_self_reset_request: false
  default_starter_kit: basic
  restart_tutorial_after_profile_reset: true
  starter_kits:
    basic:
      items:
        - "BREAD:8"
        - "WOODEN_AXE:1"
        - "TORCH:16"
```

Reguli:

- config-ul decide daca onboarding-ul este obligatoriu;
- starter kit-ul trebuie validat la startup;
- itemele invalide sunt warnings in audit, nu crash.

## Ordine de implementare

### PO1 - Document si schema

Status: `DESIGN`.

Livrabile:

- tabele `player_profiles`, `starter_kit_claims`, `player_profile_resets`;
- config default pentru onboarding;
- audit DB pentru tabele lipsa si JSON invalid.

### PO2 - PlayerProfileService

Status: `URMATOR`.

Livrabile:

- `getOrCreateProfile(Player)`;
- `saveProfile(...)`;
- `markOnboardingStep(...)`;
- `completeProfile(...)`;
- `resetProfile(...)`;
- teste in-memory pentru upsert si reset.

### PO3 - StarterKitService

Status: `URMATOR`.

Livrabile:

- validare kit din config;
- claim idempotent;
- inventar plin tratat explicit;
- audit pentru claims.

### PO4 - OnboardingGui

Status: `URMATOR`.

Livrabile:

- ecran origine;
- ecran arhetip;
- ecran trasaturi;
- preview kit;
- confirmare finala.

### PO5 - Join listener si reluare flow

Status: `URMATOR`.

Livrabile:

- detectare first join;
- deschidere GUI cu delay;
- reluare profil incomplet;
- mesaj fallback daca GUI-ul este dezactivat.

### PO6 - Comenzi admin/player

Status: `URMATOR`.

Livrabile:

- inspectare profil;
- reset profil;
- reset onboarding;
- reset quest progress separat;
- confirmare pentru reset full.

### PO7 - Integrare cu T01

Status: `DUPA BAZA`.

Livrabile:

- dupa finalizare profil, se ofera tutorialul `T01`;
- tracking optional;
- story event `PLAYER_ONBOARDED`;
- smoke Paper pentru first join -> profil -> kit -> tutorial.

### PO8 - Debugdump si release gate

Status: `DUPA BAZA`.

Livrabile:

- `player-profiles.json`;
- `starter-kit-claims.json`;
- `player-profile-resets.json`;
- release gate pentru profiluri incomplete daca onboarding obligatoriu.

## Criterii de acceptare

Sistemul este gata pentru demo cand:

- un player nou primeste GUI de initiere o singura data;
- daca inchide GUI-ul, poate relua fara date corupte;
- starter kit-ul nu se dubleaza la relog/restart;
- resetul profilului nu sterge progresul de quest fara reset separat;
- auditul explica profilurile incomplete;
- debugdump-ul contine profiluri si starter kit claims;
- tutorialul `T01` se poate lega dupa finalizarea profilului.

## Avertizari

- Nu lega itemele de start direct de `PlayerJoinEvent`; foloseste `StarterKitService`.
- Nu salva alegerile doar in scoreboard sau metadata runtime; trebuie DB.
- Nu amesteca reset profil cu reset complet player.
- Nu porni AI pentru alegeri de profil pana cand schema si validatoarele sunt stabile.
- Nu bloca spawn-ul pe server daca onboarding-ul are eroare; degradeaza cu mesaj admin si fallback.
