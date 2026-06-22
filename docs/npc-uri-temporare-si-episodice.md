# NPC-uri Temporare, Cu Impact Redus si Episodice

Actualizat: 2026-04-26

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## Scop

Acest document descrie cum ar trebui extins sistemul actual pentru a suporta:

- NPC-uri temporare
- NPC-uri cu impact redus asupra sistemului principal
- NPC-uri episodice, folosite doar in anumite questuri sau scene
- NPC-uri care nu sunt bazate pe `Villager`

Exemple:

- un NPC care apare doar cand incepe un quest
- un martor care exista doar intr-o etapa de poveste
- un ghid care apare, vorbeste si dispare
- un gardian temporar
- un negustor de eveniment
- un spirit, soldat, bandit sau mesager care nu trebuie sa fie `Villager`

## De ce ai nevoie de aceasta categorie separata

Nu toti NPC-ii trebuie sa fie NPC permanenti, cu:

- memorie completa
- familie
- rutina zilnica
- persistenta completa in DB
- participare la simularea de viata

Daca bagi toate aceste tipuri in acelasi sistem full, apar probleme:

- cost mai mare de runtime
- mai multa persistenta inutila
- mai multa complexitate in `NPCManager`
- questurile episodice devin greu de gestionat
- NPC-ii narativi scurti ajung sa fie tratati ca localnici permanenti

Regula buna:
- NPC permanent = parte din lume
- NPC episodic = parte din eveniment
- NPC low-impact = parte din prezentare sau gameplay local, fara toata infrastructura sociala

## Ce exista deja in cod

### Ce ajuta

In codul actual exista deja cateva lucruri utile:

- `AINPC` foloseste `Entity` generic in campul `bukkitEntity`
- pozitia, numele, profilul si starea sunt deja separate partial de entitatea concreta
- sistemul de dialog, emotii si context poate fi refolosit si pentru NPC-uri non-persistente
- tabela `npcs` este destul de generica la nivel de nume, locatie si profil

### Ce blocheaza acum

In acest moment, sistemul este inca legat tare de `Villager`:

- `AINPC.spawn()` creeaza direct `Villager`
- `AINPC.attachToVillager(...)` este metoda principala de atasare
- `NPCManager` este centrat pe `ensureVillagerIsNPC(...)`
- listener-ele principale lucreaza cu `instanceof Villager`
- auto-profile, auto-repopulate si multe reguli de ocupatie pornesc din profesiile de villager

Asta inseamna:
- NPC-ul logic exista
- dar stratul de manifestare in lume este inca specializat pe `Villager`

## Tipurile recomandate de NPC

### 1. NPC permanent

Acesta este modelul actual principal.

Caracteristici:
- persistent in DB
- legat de simulare
- are memorie, relatie, familie, rutina
- poate trai in sat si poate fi redescoperit in chunk-uri

Exemple:
- fierar
- hangiu
- fermier
- preot

### 2. NPC temporar persistent usor

NPC care exista o perioada limitata, dar trebuie sa poata supravietui la restart pe durata unui quest sau eveniment.

Caracteristici:
- persistenta minima
- fara familie
- fara simulare completa
- memorie limitata sau deloc
- legat de `questId`, `scenarioId` sau `eventId`

Exemple:
- un soldat trimis intr-o misiune
- un curier care sta in sat pana predai mesajul

### 3. NPC episodic runtime-only

NPC care apare si dispare in aceeasi sesiune, fara persistenta reala.

Caracteristici:
- nu intra in DB sau intra doar intr-un registru temporar
- nu participa la autosave complet
- nu are familie sau istoric lung
- poate avea doar dialog, reactie si context minimal

Exemple:
- martor de scena
- spirit de quest
- ghid temporar

### 4. NPC de impact redus

NPC folosit pentru prezentare sau interactivitate limitata.

Caracteristici:
- nu are simulare de viata
- nu are nevoi complexe
- nu ia decizii extinse
- reactioneaza doar la un set mic de interactiuni

Exemple:
- santinela care spune 2 replici
- comerciant de eveniment
- paznic de poarta pentru un singur obiectiv

### 5. NPC non-villager

NPC logic care foloseste alt suport vizual sau gameplay.

Exemple:
- om soldat
- bandit
- spirit
- fantoma
- animal companion
- boss helper

## Modelul de clasificare recomandat

Nu este suficient un singur boolean `temporary`.

Modelul bun este pe mai multe axe:

- `lifecycleType`
- `persistenceMode`
- `simulationMode`
- `entityKind`
- `interactionProfile`

### Enum-uri recomandate

#### `NpcLifecycleType`

- `PERMANENT`
- `TEMPORARY`
- `EPISODIC`
- `SCENE_ONLY`

#### `NpcPersistenceMode`

- `FULL`
- `LIGHT`
- `RUNTIME_ONLY`

#### `NpcSimulationMode`

- `FULL`
- `LIGHT`
- `NONE`

#### `NpcInteractionProfile`

- `FULL_AI`
- `QUEST_ONLY`
- `SHOP_ONLY`
- `SCENE_ONLY`
- `MINIMAL`

#### `NpcEntityKind`

- `VILLAGER`
- `HUMANOID`
- `MONSTER`
- `ANIMAL`
- `SPIRIT`
- `MARKER`
- `NONE`

Observatie:
- `entityKind` nu trebuie sa fie acelasi lucru cu `EntityType`
- el descrie intentia de design, nu doar clasa Bukkit

## Campuri noi recomandate in model

`AINPC` sau un wrapper de definire ar trebui sa suporte:

- `lifecycleType`
- `persistenceMode`
- `simulationMode`
- `interactionProfile`
- `entityKind`
- `entityArchetype`
- `ownerQuestId`
- `ownerScenarioId`
- `spawnSource`
- `spawnedAt`
- `expiresAt`
- `despawnRule`
- `temporaryTags`
- `shouldTrackMemories`
- `shouldTrackRelationship`
- `shouldUseEmotionDecay`
- `shouldUseFamilySystem`
- `shouldParticipateInVillageBalance`

Acestea separa clar:
- un localnic real
de
- un actor temporar de quest

## Separarea dintre NPC logic si corpul din lume

Aceasta este cheia pentru suportul de NPC non-villager.

Trebuie sa separi:

- `NPC logic`
de
- `carrier entity`

### Recomandare

Introdu un strat nou:

- `NpcEntityAdapter`

Interfata minima:

- `spawn(AINPC npc, Location location)`
- `attach(AINPC npc, Entity entity)`
- `despawn(AINPC npc)`
- `lookAt(AINPC npc, Player player)`
- `teleport(AINPC npc, Location location)`
- `updateDisplayName(AINPC npc)`
- `getLocation(AINPC npc)`
- `isValid(AINPC npc)`
- `getEntity(AINPC npc)`

### Implementari recomandate

- `VillagerNpcEntityAdapter`
- `HumanoidNpcEntityAdapter`
- `MobNpcEntityAdapter`
- `MarkerNpcEntityAdapter`
- `InvisibleLogicNpcAdapter`

Astfel:
- `AINPC` nu mai stie direct cum se creeaza corpul
- doar cere adapter-ului sa il materializeze

## De ce este important acest adapter

Fara acest strat, ajungi sa dublezi logica:

- `spawnVillagerNpc`
- `spawnSpiritNpc`
- `spawnGuardNpc`
- `spawnQuestHumanNpc`

Cu adapter:
- sistemul de memorie, reactie si quest poate folosi acelasi `AINPC`
- doar modul in care apare in lume se schimba

## NPC-uri temporare fara impact mare

Pentru low-impact sau episodice, nu vrei sa ruleze tot pipeline-ul complet.

### Ce poti dezactiva

- familie
- nevoi complexe
- autosave complet
- repopulare de sat
- decay emotional complet
- participare la gossip sistemic
- integrare adanca cu economie

### Ce poti pastra

- nume
- localizare
- cateva replici
- reactie la jucator
- legare la quest
- eventual emotii simple

Regula buna:
- low-impact NPC = interactioneaza clar, dar nu consuma infrastructura de localnic permanent

## Moduri concrete de implementare

### Varianta 1: runtime-only registry

Adaugi un registru separat:

- `TemporaryNpcRegistry`

Acesta tine:
- NPC-uri create din questuri
- NPC-uri de scena
- NPC-uri de eveniment

Caracteristici:
- nu intra in `loadAllNPCs()`
- nu intra in salvarea permanenta standard
- sunt curatate pe:
  - quest complete
  - quest fail
  - server stop
  - timeout

Avantaj:
- simplu de introdus
- risc mic pentru nucleul actual

### Varianta 2: persistenta light

NPC-ul intra in DB, dar cu metadate speciale:

- `persistence_mode = light`
- `owner_quest_id = Q12`
- `expires_at = ...`

Caracteristici:
- poate supravietui la restart
- dar nu participa la toate subsistemele

Avantaj:
- bun pentru questuri lungi
- bun pentru escorte sau NPC aparuti temporar in lume

### Varianta 3: scene-only actor

NPC fara existenta sociala reala, doar pentru scena.

Caracteristici:
- apare la o etapa
- are 1-2 replici
- poate merge sau privi jucatorul
- dispare la final

Exemple:
- spirit care arata drumul
- soldat care deschide scena
- prizonier care cere ajutor

Acest tip poate avea:
- `simulationMode = NONE`
- `persistenceMode = RUNTIME_ONLY`
- `interactionProfile = SCENE_ONLY`

## NPC-uri non-villager

### Ce inseamna in practica

NPC-ul poate fi:

- alt `LivingEntity`
- o combinatie de marker + interact
- o integrare cu un plugin extern
- sau chiar un NPC logic fara corp complet

### Tipuri utile

#### `HUMANOID`

Pentru:
- gardieni
- banditi
- soldati
- magi

Acest tip cere un carrier diferit de villager.

#### `MONSTER`

Pentru:
- dusmani inteligenti
- sefi episodici
- creaturi de quest

#### `SPIRIT`

Pentru:
- fantome
- ghizi mistici
- mesageri temporari

#### `MARKER`

Pentru:
- punct de interactiune narativa
- silueta de scena
- obiect vorbitor

## Integrare cu questuri

Acesta este cel mai important caz de folosire.

Un quest ar trebui sa poata:

- spawna un NPC temporar la inceputul unei etape
- muta acel NPC in alta locatie
- schimba dialogul pe etapa
- despawna NPC-ul la final

### Hook-uri utile pe etapa

- `onStageStart`
- `onStageComplete`
- `onQuestFail`
- `onQuestReset`

Actiuni utile:

- `spawn_npc`
- `despawn_npc`
- `teleport_npc`
- `change_npc_dialogue_profile`
- `bind_npc_to_player`
- `set_npc_hostile`
- `spawn_actors`
- `despawn_actors`
- `on_stage_enter`
- `on_stage_exit`
- `on_stage_complete`
- `quest_actor_triggers`

Aliasuri acceptate:

- `on_stage_start` -> `on_stage_enter`
- `on_stage_finish` / `on_stage_end` -> `on_stage_complete`

Cheile necunoscute din `quest_actor_triggers` sunt ignorate si emit warning la incarcare.
`/ainpc scenario list` afiseaza si numarul de warning-uri pentru fiecare scenariu activ.
`/ainpc scenario list warnings-only` filtreaza doar scenariile care au warning-uri.
`/ainpc scenario list warnings-first` sorteaza scenariile cu warning-uri primele.
`warnings-only` si `warnings-first` nu pot fi folosite impreuna; un mod necunoscut este respins cu mesaj clar.
`/ainpc scenario warnings` listeaza direct doar scenariile cu warning-uri.

### Exemplu conceptual

```yml
quest:
  code: "Q_SPIRIT_01"
  stages:
    summon_spirit:
      on_start_actions:
        - type: "spawn_npc"
          npc_id: "spirit_guide_01"
          lifecycle: "episodic"
          persistence: "runtime_only"
          entity_kind: "spirit"
          interaction_profile: "quest_only"
          place: "ruina_altar"
```

## Impact redus asupra simularii

NPC-urile temporare nu ar trebui sa intre automat in:

- `runLifeSimulationTick()` complet
- rebalance de sate
- calcul de ancore home/work/social implicite
- regulile de familie si reproducere

### Recomandare

In `NPCManager`, filtrarea de simulare ar trebui sa tina cont de:

- `simulationMode`
- `lifecycleType`

Exemplu:
- `FULL` -> toate tick-urile
- `LIGHT` -> doar context + reactie + quest
- `NONE` -> doar interactiuni explicite

## Persistenta recomandata

### Ce poti refolosi

Tabela `npcs` actuala este destul de buna pentru:

- nume
- locatie
- profil
- skin
- ocupatie

### Ce trebuie adaugat

Coloane recomandate:

- `npc_lifecycle_type`
- `npc_persistence_mode`
- `npc_simulation_mode`
- `npc_interaction_profile`
- `npc_entity_kind`
- `owner_quest_id`
- `owner_scenario_id`
- `expires_at`
- `spawn_source`
- `spawn_phase`
- `spawn_policy`
- `is_low_impact`

Alternativ:
- poti incepe cu aceste campuri in `profile_data`
- apoi le promovezi in coloane cand modelul se stabilizeaza

## Reguli de cleanup

NPC-urile temporare trebuie sa aiba reguli clare de disparitie.

Exemple de reguli:

- la final de etapa
- la final de quest
- dupa timeout
- cand jucatorul paraseste regiunea
- la restart

`despawnRule` poate fi:

- `ON_STAGE_COMPLETE`
- `ON_QUEST_COMPLETE`
- `ON_QUEST_FAIL`
- `ON_TIMEOUT`
- `ON_RESTART`
- `MANUAL`

Fara reguli clare, apar:

- NPC-uri uitate in lume
- entitati orfane
- questuri duplicate
- inconsistente intre DB si runtime

## NPC-uri temporare si reactie

NPC-urile temporare nu trebuie sa foloseasca automat acelasi profil de reactie ca localnicii permanenti.

Exemple:

- un spirit de quest
  - reactie simpla, calma, orientata pe obiectiv
- un soldat episodic
  - reactie orientata pe avertizare, ordine, escorta
- un martor de scena
  - reactie minimalista, cu replici strict controlate

De aceea, `interactionProfile` este important.

Exemple:

- `FULL_AI`
- `QUEST_ONLY`
- `SCENE_ONLY`
- `LOW_IMPACT_REACTIVE`

## NPC fara corp persistent

Uneori vrei doar un "actor logic" care:

- are nume
- are replici
- poate fi tinta de quest
- dar nu trebuie sa aiba entitate permanenta tot timpul

Acest model este util pentru:

- voci narative
- entitati invocate scurt
- actori de scena

In acest caz:
- `entityKind = NONE`
- `spawn()` devine optional
- questul sau scena poate materializa doar o reprezentare scurta

## Ordine buna de implementare

### Faza 1

Suport pentru NPC-uri temporare bazate tot pe `Villager`

De facut:
- `lifecycleType`
- `persistenceMode`
- `simulationMode`
- registru temporar
- reguli de despawn

Avantaj:
- impact mic
- folosesti aproape toata infrastructura existenta

### Faza 2

Decuplare spawn logic -> entity adapter

De facut:
- `NpcEntityAdapter`
- `VillagerNpcEntityAdapter`
- refactor in `AINPC.spawn()`

Avantaj:
- pregatesti suportul pentru NPC non-villager

### Faza 3

Suport real pentru NPC-uri non-villager

De facut:
- `entityKind`
- adaptere noi
- hook-uri de quest pentru spawn/despawn
- profile de interactiune diferite

### Faza 4

Persistenta light si cleanup automat

De facut:
- expirare
- owner quest/scenario
- curatare pe reload/restart/finalizare

## MVP recomandat

Cel mai bun MVP pentru tine ar fi:

1. NPC temporar de quest bazat pe `Villager`
2. `runtime_only` si `light`
3. fara familie
4. fara simulare de viata completa
5. spawn la inceput de etapa
6. despawn la final de etapa

Abia dupa ce asta e stabil, merita introdus suportul pentru NPC non-villager.

## Ce sa eviti

- sa bagi NPC-urile temporare direct in acelasi flux ca localnicii permanenti
- sa salvezi complet in DB orice actor de scena
- sa implementezi non-villager direct in `AINPC.spawn()` cu `if`-uri multe
- sa lasi questurile sa gestioneze manual entitatile fara un serviciu comun
- sa amesteci profilele de reactie ale unui spirit, gardian si localnic obisnuit

## Directia recomandata pentru implementare

Ca sa reusezi acelasi mecanism in mai multe scenarii, sistemul trebuie sa plece de la o definitie de actor, nu de la o lista fixa de tipuri.

### Principiul de baza

- scenariul defineste actorul
- core-ul executa ciclul de viata si atasarea in lume
- adapterul decide cum apare actorul in lume
- profilul de interactiune spune ce poate face actorul

### Modelul de date recomandat

Un actor scenariu-definit ar trebui sa aiba:

- `lifecycleType`
- `persistenceMode`
- `simulationMode`
- `interactionProfile`
- `entityKind`
- `entityArchetype`
- `ownerScenarioId`
- `ownerQuestId`
- `spawnSource`
- `spawnPhase`
- `spawnPolicy`
- `questActorTriggers`
- `despawnRule`
- `durationSeconds`
- `temporaryTags`

`spawnPolicy` controleaza daca actorul se spawneaza automat, la o faza, doar prin reguli de stage sau manual din admin.

### Ce rezolva acest model

- un paznic temporar pentru un quest
- un martor care apare doar intr-o scena
- un ghid de dungeon
- un mesager sau curier
- un actor de tip monstru, spirit sau om simplificat

### Ce ramane in core

- persistenta si cleanup
- reguli de spawn/despawn
- legarea la quest sau scenariu
- adaptarea la tipul de entitate Bukkit

### Ce ramane in scenariu

- numele si rolul actorului
- durata de viata
- profilul minim de interactiune
- ancorele de quest sau story
- tag-urile de scena

### Exemplu de configurare

```yml
scenarios:
  Q08:
    actors:
      dungeon_spirit:
        name: "Spiritul din Cripta"
        lifecycle_type: "episodic"
        persistence_mode: "runtime_only"
        simulation_mode: "none"
        interaction_profile: "scene_only"
        entity_kind: "spirit"
        entity_archetype: "wraith"
        owner_scenario_id: "medieval_quest"
        owner_quest_id: "Q08"
        spawn_source: "border_patrol_scene"
        spawn_phase: "investigation"
        spawn_policy: "phase"
        despawn_rule: "on_stage_complete"
        duration_seconds: 180
        temporary_tags: ["undead", "dungeon", "scene"]
      border_messenger:
        name: "Mesagerul Hotarului"
        lifecycle_type: "temporary"
        persistence_mode: "runtime_only"
        simulation_mode: "light"
        interaction_profile: "scene_only"
        entity_kind: "villager"
        spawn_source: "quest_accept_scene"
        spawn_policy: "manual"
        despawn_rule: "on_quest_complete"
        duration_seconds: 240
    quest_actor_triggers:
      on_accept:
        - "border_messenger"
      on_fail:
        - "border_messenger"
      on_reset:
        - "border_messenger"
    stages:
      PATROL:
        description: "Jucatorul verifica regiunea si elimina amenintarile."
        completion_mode: "all_objectives"
        next_stage: "RETURN"
        spawn_actors: "dungeon_spirit"
        on_stage_enter: "dungeon_spirit"
        on_stage_complete: "border_messenger"
        objectives:
          - "patrol_region"
          - "clear_zombies"
      RETURN:
        description: "Jucatorul revine la garda cu raportul."
        completion_mode: "manual_turn_in"
        despawn_actors: "dungeon_spirit"
        on_stage_exit: "dungeon_spirit"
        objectives:
          - "report_to_guard"
```

## Stadiul implementarii

Am inceput printr-o baza minima care separa:

- clasificarea actorului
- definitia scenariu
- adapterul de entitate

In aceasta etapa, implementarea porneste in continuare cu suportul existent pe `Villager`, dar fara sa mai blocheze designul in jurul lui.

### Ce este deja legat in runtime

- scenariile incarcate pot contine `actors`
- scenariile active pastreaza actorii definiti in template
- actorii runtime-only pot fi inregistrati temporar in manager
- exista helper pentru spawn/despawn pe scenariu activ
- debug dump-ul include numarul de actori si detaliile lor
- exista comanda admin `/ainpc scenario <list|info|spawn|despawn>`

## Concluzie

Sistemul actual este foarte bun pentru NPC permanenti bazati pe `Villager`, dar pentru NPC-uri temporare sau episodice ai nevoie de 3 separari clare:

- separare intre permanent si temporar
- separare intre full simulation si low-impact
- separare intre NPC logic si tipul de entitate vizibila in lume

Drumul bun este:

1. temporar pe `Villager`
2. lifecycle + persistence + cleanup
3. `NpcEntityAdapter`
4. suport pentru non-villager si scene episodice

