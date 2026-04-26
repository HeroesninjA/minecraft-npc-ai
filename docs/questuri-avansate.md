# Questuri Avansate - Documentatie de implementare

Actualizat: 2026-04-26

## Scop

Acest document descrie cum poate evolua sistemul actual de questuri din proiect intr-un sistem avansat, modular si extensibil.

Documentul are 2 obiective:
- sa descrie clar ce exista deja in cod
- sa arate cum se extinde corect spre questuri complexe, cu etape, progres persistent, obiective diferite si integrare cu lumea

## Ce exista deja in proiect

Fisiere relevante:
- `src/src/main/java/ro/ainpc/engine/ScenarioEngine.java`
- `src/src/main/java/ro/ainpc/engine/FeaturePackLoader.java`
- `src/src/main/java/ro/ainpc/listeners/QuestObjectiveListener.java`
- `src/src/main/resources/quests.yml`
- `ainpc-scenario-medieval/src/main/resources/packs/medieval_quest.yml`

Capabilitati actuale:
- questurile sunt definite prin `scenarios` cu `base_type: "QUEST"`
- questul poate avea `quest.code`, `giver_profession`, `objectives`, `rewards`
- exista progres persistent in tabela `player_quests`
- exista stari de baza:
  - `NOT_STARTED`
  - `OFFERED`
  - `ACTIVE`
  - `COMPLETED`
  - `FAILED`
- exista comenzi:
  - `/ainpc quest`
  - `/ainpc quest accept`
  - `/ainpc quest decline`
  - `/ainpc quest abandon`
  - `/ainpc quest status`
  - `/ainpc quest reset`
  - `/ainpc quest complete`

Tipuri de obiective suportate in acest moment:
- `collect_item`
- `deliver_to_npc`
- `talk_to_npc`
- `visit_region`
- `kill_mob`

Limitari actuale:
- modelul de obiectiv este foarte simplu: `type`, `item`, `amount`, `description`
- recompensele sunt in practica doar iteme
- questul este inca tratat ca subtip de `Scenario`
- progresul curent este gandit in principal pentru un singur quest activ per jucator
- nu exista inca etape reale separate, branching sau obiective conditionale

## Fluxul actual

Fluxul actual este bun pentru un MVP:

1. NPC-ul expune un quest prin `ScenarioTemplate`
2. Jucatorul il vede in starea `OFFERED`
3. Jucatorul il accepta si intra in `ACTIVE`
4. Evenimentele actualizeaza progresul:
   - conversatie cu NPC
   - miscare intre regiuni
   - ucidere de mob
   - verificare inventar pentru iteme
5. Jucatorul revine la NPC
6. sistemul verifica obiectivele
7. se dau recompensele si questul intra in `COMPLETED`

Acest flux trebuie pastrat ca baza, dar separat de logica avansata.

## Directia corecta pentru un sistem avansat

Questul avansat nu trebuie tratat doar ca o lista de iteme de adus.

Un quest avansat are:
- identitate proprie
- etape reale
- obiective pe etapa
- conditii de activare
- progres persistent
- variabile interne
- roluri pentru NPC-uri
- integrare cu regiuni, locuri si noduri
- posibilitatea de ramificare

Modelul bun este:
- `QuestDefinition` = definitia statica
- `QuestStageDefinition` = o etapa din quest
- `QuestObjectiveDefinition` = un obiectiv al etapei
- `QuestRewardDefinition` = recompensa
- `QuestProgress` = progresul runtime al jucatorului
- `QuestVariableBag` = valori dinamice folosite in runtime

## Arhitectura recomandata

### Varianta buna pe termen lung

Pe termen lung, questurile ar trebui extrase din `ScenarioEngine` intr-un sistem separat:

- `QuestEngine`
- `QuestDefinition`
- `QuestStageDefinition`
- `QuestObjectiveDefinition`
- `QuestRewardDefinition`
- `QuestProgress`
- `QuestTriggerService`

`ScenarioEngine` poate ramane pentru evenimente emergente intre NPC-uri.
`QuestEngine` trebuie sa devina responsabil pentru:
- acceptare
- progres
- validare
- finalizare
- abandon
- esec
- persistenta

### Varianta sigura pe termen scurt

Daca vrei sa evoluezi fara refactor mare imediat:
- pastrezi `ScenarioEngine`
- extinzi `ScenarioTemplate` pentru questuri complexe
- introduci clase noi doar pentru questuri
- muti treptat logica de quest in metode si fisiere dedicate

Aceasta este calea cu cel mai mic risc.

## Modelul de date recomandat

### QuestDefinition

Campuri recomandate:
- `id`
- `code`
- `displayName`
- `description`
- `category`
- `repeatable`
- `cooldownSeconds`
- `giverNpcSelector`
- `giverRole`
- `requiredPlayerLevel`
- `requiredQuestIds`
- `blockedByQuestIds`
- `requiredWorldTags`
- `requiredPlaceTags`
- `stages`
- `rewards`
- `failureRules`
- `metadata`

### QuestStageDefinition

Campuri recomandate:
- `id`
- `displayName`
- `description`
- `autoStart`
- `completionMode`
- `objectives`
- `onStartActions`
- `onCompleteActions`
- `nextStageId`
- `branchRules`

`completionMode` poate fi:
- `ALL_OBJECTIVES`
- `ANY_OBJECTIVE`
- `MANUAL_TURN_IN`

### QuestObjectiveDefinition

Campuri recomandate:
- `id`
- `type`
- `targetRef`
- `amount`
- `description`
- `optional`
- `hiddenUntilActive`
- `consumeOnComplete`
- `conditions`
- `trackingMode`

### QuestRewardDefinition

Campuri recomandate:
- `id`
- `type`
- `targetRef`
- `amount`
- `description`
- `conditions`

Tipuri utile:
- `item`
- `xp`
- `money`
- `reputation`
- `unlock_quest`
- `set_story_state`
- `set_place_state`
- `run_action`

### QuestProgress

Campuri recomandate:
- `playerId`
- `questId`
- `status`
- `startedAt`
- `completedAt`
- `updatedAt`
- `currentStageId`
- `objectiveProgress`
- `questVariables`
- `assignedNpcIds`
- `history`

Observatie:
- in codul actual ai deja ceva foarte apropiat prin `PlayerQuestProgress`
- aceasta structura este o baza buna pentru evolutie

## Tipuri de obiective recomandate

### Tipuri deja potrivite

- `collect_item`
- `deliver_to_npc`
- `talk_to_npc`
- `visit_region`
- `kill_mob`

### Tipuri noi recomandate

- `visit_place`
- `enter_place`
- `talk_to_role`
- `talk_to_npc_at_place`
- `kill_mob_in_region`
- `kill_mob_in_place`
- `bring_item_to_place`
- `inspect_node`
- `escort_npc`
- `wait_until_time`
- `survive_duration`
- `craft_item`
- `smelt_item`
- `interact_block`
- `choose_dialogue_branch`
- `trigger_story_flag`

### Exemplu de semantica

- `visit_region`
  - tinta: `satul_central`
- `visit_place`
  - tinta: `fierarie`
- `talk_to_npc`
  - tinta: `npc_ion`
- `talk_to_role`
  - tinta: `blacksmith`
- `kill_mob`
  - tinta: `ZOMBIE`
- `inspect_node`
  - tinta: `forge_spot`

## Suport pentru etape reale

Sistemul avansat trebuie sa ruleze pe etape.

Exemplu:

1. `INTRODUCTION`
2. `ACCEPTANCE`
3. `GATHERING`
4. `RETURN`
5. `COMPLETION`

In codul actual, `phases` exista deja, dar sunt in mare parte informative.

Pentru implementare reala:
- fiecare etapa trebuie sa aiba obiective proprii
- progresul trebuie verificat pe etapa curenta
- trecerea la etapa urmatoare trebuie sa fie controlata explicit

Regula buna:
- nu verifica toate obiectivele din tot questul in acelasi timp
- verifica doar etapa activa

## Ramificare

Questurile avansate trebuie sa suporte ramificare.

Exemple:
- jucatorul duce itemul la fierar sau la negustor
- jucatorul minte sau spune adevarul
- jucatorul ajuta satul sau banda de hoti

Pentru asta ai nevoie de:
- `branchRules`
- `questVariables`
- `nextStageId` dinamic

Exemplu de variabila:
- `helped_blacksmith=true`
- `chose_merchant_path=true`

## Integrare cu NPC-uri

Questurile bune trebuie sa se lege de roluri, nu doar de nume fixe.

Exemple:
- `QUEST_GIVER`
- `HELPER`
- `TARGET`
- `ANTAGONIST`
- `WITNESS`

Avantaj:
- acelasi quest poate fi reutilizat in lumi diferite
- rolurile pot fi rezolvate din profesii, trasaturi sau locatii

Exemplu:
- `QUEST_GIVER` = primul NPC cu profesia `blacksmith`
- `HELPER` = un NPC `farmer` sau `merchant`

## Integrare cu mapping-ul lumii

Pentru questuri avansate, integrarea cu sistemul de mapping este foarte importanta.

Questurile nu ar trebui sa lucreze doar cu coordonate brute, ci cu:
- `region`
- `place`
- `node`
- `tags`

Exemple:
- viziteaza `fierarie`
- du coletul in `magazin`
- vorbeste cu gardianul la `poarta_castelului`
- inspecteaza `node=forge_spot`

De aceea, `visit_place` si `inspect_node` sunt extensii naturale peste sistemul din `mapping.md`.

## YAML recomandat pentru questuri avansate

Format propus:

```yml
scenarios:
  Q02:
    name: "Provizii pentru sat"
    description: "Quest multi-etapa cu ramificare."
    base_type: "QUEST"
    requires_player: true
    replace_base_type: false
    hint: "Fierarul pare ingrijorat de lipsa materialelor."

    quest:
      code: "Q02"
      giver_profession: "blacksmith"
      repeatable: false
      required_quests:
        - "Q01"

      stages:
        introduction:
          display_name: "Discutia initiala"
          completion_mode: "MANUAL_TURN_IN"
          objectives:
            talk_blacksmith:
              type: "talk_to_npc"
              item: "npc_blacksmith"
              amount: 1
              description: "Vorbeste cu fierarul."
          next_stage: "gathering"

        gathering:
          display_name: "Strange materialele"
          completion_mode: "ALL_OBJECTIVES"
          objectives:
            iron_ingots:
              type: "collect_item"
              item: "IRON_INGOT"
              amount: 3
              description: "Strange 3 lingouri de fier."
            visit_forge:
              type: "visit_place"
              item: "fierarie"
              amount: 1
              description: "Mergi la fierarie."
          next_stage: "turn_in"

        turn_in:
          display_name: "Preda materialele"
          completion_mode: "MANUAL_TURN_IN"
          objectives:
            deliver:
              type: "deliver_to_npc"
              item: "npc_blacksmith"
              amount: 1
              description: "Preda materialele fierarului."
          next_stage: "completion"

      rewards:
        iron_sword:
          type: "item"
          item: "IRON_SWORD"
          amount: 1
          description: "Primesti o sabie de fier."
```

Observatie importanta:
- schema actuala din `FeaturePackLoader` nu poate incarca inca `stages`
- pentru acest model trebuie extins parserul

## Persistenta recomandata

In prezent exista deja persistenta in `player_quests`, ceea ce este foarte bine.

Pentru nivel avansat, progresul trebuie sa stocheze:
- `quest_id`
- `stage_id`
- `status`
- `objective_progress_json`
- `quest_variables_json`
- `assigned_npc_ids_json`
- `started_at`
- `updated_at`
- `completed_at`

Important:
- `objective_progress` trebuie indexat logic dupa `objectiveId`, nu doar dupa combinatie generata din tip si item
- `questVariables` trebuie folosit pentru decizii, ramificari si legaturi runtime

## Evenimente si tracking

Pentru questuri avansate, listener-ele trebuie sa acopere mai mult decat miscarea si uciderea de mobi.

Listener-e recomandate:
- `PlayerMoveEvent`
- `EntityDeathEvent`
- `InventoryClickEvent`
- `CraftItemEvent`
- `FurnaceSmeltEvent`
- `PlayerInteractEvent`
- `AsyncPlayerChatEvent` sau fluxul intern de dialog
- evenimente custom pentru:
  - intrare in `place`
  - activare de `node`
  - schimbare de `story state`

Recomandare:
- nu pune toata logica direct in listener
- listener-ul doar publica evenimentul catre `QuestEngine`

## Recompense avansate

Acum sistemul acorda in principal iteme.

Pentru un sistem avansat, recompensele trebuie abstractizate:
- iteme
- bani
- experienta
- reputatie
- deblocare de quest
- schimbare de stare de poveste
- schimbare de relatie cu NPC-ul
- deschidere de loc nou in lume

Asta inseamna ca recompensa trebuie procesata de un `RewardResolver`, nu direct doar prin `Material`.

## Reguli importante de design

- defineste questurile in date, nu hardcodate in Java
- foloseste `id` stabil pentru quest, etapa si obiectiv
- nu lega questurile de nume de NPC daca poti folosi roluri sau tag-uri
- nu lega questurile de coordonate brute daca poti folosi `region`, `place` si `node`
- separa validarea obiectivelor de logica de UI
- separa parserul YAML de runtime

## Ce trebuie schimbat in codul actual

### 1. Extinderea parserului

Fisier:
- `src/src/main/java/ro/ainpc/engine/FeaturePackLoader.java`

De facut:
- extinde `QuestEntryDefinition` sau inlocuieste-l cu un model mai bogat
- adauga suport pentru `stages`
- adauga suport pentru campuri noi:
  - `id`
  - `optional`
  - `consume_on_complete`
  - `conditions`
  - `target_ref`
  - `next_stage`

### 2. Separarea runtime-ului de definitie

Fisier:
- `src/src/main/java/ro/ainpc/engine/ScenarioEngine.java`

De facut:
- separa definitia questului de progresul runtime
- muta logica de obiective in metode dedicate pe tip de obiectiv
- inlocuieste verificarea globala a obiectivelor cu verificare per etapa

### 3. Suport pentru mai multe questuri active

Acesta este unul dintre cele mai importante upgrade-uri.

Modelul actual foloseste in esenta un singur `activePlayerQuests` per jucator.

Pentru avansat, recomandarea este:
- `Map<UUID, Map<String, PlayerQuestProgress>>`

sau un repository dedicat:
- `QuestProgressRepository`

Asta permite:
- quest principal
- quest secundar
- quest repetabil
- quest de reputatie

## Ordine buna de implementare

1. Pastreaza sistemul actual functional
2. Adauga `objectiveId` explicit in definitii
3. Extinde parserul pentru obiective mai bogate
4. Introdu `stages`
5. Muta verificarea progresului pe etapa curenta
6. Adauga `visit_place` si `inspect_node`
7. Extinde recompensa dincolo de iteme
8. Introdu suport pentru mai multe questuri active
9. Extrage treptat un `QuestEngine` separat

## MVP avansat recomandat

Daca vrei o versiune buna, fara sa fie excesiv de mare, MVP-ul avansat ar trebui sa includa:
- quest cu mai multe etape
- `collect_item`
- `talk_to_npc`
- `visit_region`
- `visit_place`
- `deliver_to_npc`
- recompense cu iteme si reputatie
- progres persistent
- abandon si reset
- un singur branch simplu

## Ce sa eviti

- questuri hardcodate direct in comenzi
- obiective care depind de nume fragile
- logica de progres imprastiata prin multe clase fara un punct central
- recompense implementate doar prin `Material`
- lipsa de `objectiveId` stabil
- lipsa de etapa curenta persistata

## Concluzie

Sistemul actual este o baza buna pentru questuri simple si medii.

Pentru questuri avansate, pasii corecti sunt:
- model de date mai bogat
- etape reale
- obiective extensibile
- integrare cu locatii semantice
- persistenta mai structurata
- separarea treptata a questurilor de scenariile generale

Cel mai bun drum nu este rescrierea completa imediata.
Cel mai bun drum este extinderea controlata a sistemului actual pana cand `QuestEngine` poate fi extras curat.
