# Quest: Blestemul Castelului Parasit

## Poveste

Fierarul din sat a fost contactat de Dagon, fostul stăpân al castelului.
Dagon spune că o vrăjitoare și-a făcut cuib în castel și a pus mâna pe o cheie antică,
necesară pentru a deschide tezaurul din curtea castelului.

Fierarul te roagă să cureți castelul de zombi, să învingi vrăjitoarea,
să iei cheia și să deschizi cufărul cu comoară.

## Structură

- **NPC giver:** `profession:blacksmith` (Fierarul)
- **Kind:** `hunt` / `exploration`
- **Category:** `side`
- **Acceptance mode:** `explicit`
- **Completion mode:** `return_to_giver`
- **Prerequisites:** Q01 (sabia de fier)
- **Tags:** `combat`, `exploration`, `castle`, `boss`, `story`

## Obiective (11 buc)

| # | Key | Tip | Target | Amount | Stage |
|---|-----|-----|--------|--------|-------|
| 1 | talk_blacksmith | `talk_to_npc` | `profession:blacksmith` | 1 | ACCEPTANCE |
| 2 | enter_castle | `visit_region` | `castel` | 1 | ENTER_CASTLE |
| 3 | pass_gate | `visit_place` | `castel:poarta_castel` | 1 | ENTER_CASTLE |
| 4 | zone1_zombies | `kill_mob` | `ZOMBIE` | 2 | ZONE_1 |
| 5 | zone2_zombies | `kill_mob` | `ZOMBIE` | 2 | ZONE_2 |
| 6 | zone3_zombies | `kill_mob` | `ZOMBIE` | 2 | ZONE_3 |
| 7 | zone4_zombies | `kill_mob` | `ZOMBIE` | 1 | ZONE_4 |
| 8 | kill_witch | `kill_mob` | `WITCH` | 1 | BOSS |
| 9 | find_key | `inspect_node` | `castel:sala_boss:cheie_vrajitoare` | 1 | BOSS |
| 10 | open_chest | `inspect_node` | `castel:curte_castel:cufar` | 1 | TREASURE |
| 11 | return_blacksmith | `talk_to_npc` | `profession:blacksmith` | 1 | RETURN |

## Recompense

| Key | Tip | Item/Val | Amount |
|-----|-----|----------|--------|
| enchanted_sword | `item` | `DIAMOND_SWORD` | 1 |
| emeralds | `item` | `EMERALD` | 3 |
| xp_reward | `experience` | `XP` | 100 |
| castle_cleansed | `record_story_event` | `castle_cleansed` | 1 |

## Mapping necesar

### Place-uri de adăugat

| ID | Tip | Descriere |
|----|-----|-----------|
| `castel:zona_1` | `castle_courtyard` | Prima zonă de luptă |
| `castel:zona_2` | `castle_courtyard` | A doua zonă de luptă |
| `castel:zona_3` | `castle_courtyard` | A treia zonă de luptă |
| `castel:zona_4` | `castle_courtyard` | A patra zonă de luptă |
| `castel:sala_boss` | `throne_room` | Sala tronului, boss fight |

### Node-uri de adăugat

| ID | Tip | Descriere |
|----|-----|-----------|
| `castel:sala_boss:cheie_vrajitoare` | `loot` | Cheia antică lângă vrăjitoare |

### Aliases de adăugat

În `medieval_quest.yml` sub `guard`:
```yaml
aliases:
  - "guard"
  - "soldat"
  - "garda"
```

## YAML complet

```yaml
castel_curatat:
  name: "Blestemul Castelului Parasit"
  description: "Fierarul te trimite la castelul parasit al lui Dagon sa cureti creaturile, sa invingi vrajitoarea si sa aduci comoara."
  base_type: "QUEST"
  mechanic: "side_quests"
  trigger_probability: 0.08
  min_npcs: 1
  requires_player: true
  hint: "Fierarul priveste spre castel si spune ca vrajitoarea trebuie oprita."
  preferred_topologies:
    - "village_center"
    - "castle"
  narrative_hints:
    - "este o misiune de combat si explorare la castel"
    - "jucatorul vorbeste cu fierarul, merge la castel, omoara 7+1 zombi si o vrajitoare"
    - "finalizarea scrie un story event regional"
  phases:
    ACCEPTANCE: "Fierarul explica ce trebuie facut la castel."
    ENTER_CASTLE: "Jucatorul ajunge la castel si intra pe poarta."
    ZONE_1: "Prima zona de lupta."
    ZONE_2: "A doua zona de lupta."
    ZONE_3: "A treia zona de lupta."
    ZONE_4: "A patra zona de lupta."
    BOSS: "Lupta cu vrajitoarea."
    TREASURE: "Cauta cheia si deschide cufarul."
    RETURN: "Intoarce-te la fierar cu comoara."
    COMPLETION: "Fierarul multumeste si ofera rasplata."
  quest:
    code: "CST02"
    giver_profession: "blacksmith"
    kind: "hunt"
    category: "side"
    acceptance_mode: "explicit"
    completion_mode: "return_to_giver"
    tracking_mode: "next_objective"
    tags: ["combat", "exploration", "castle", "boss", "story"]
    prerequisites:
      - "Q01"
    dialogues:
      offer:
        - "Dagon a venit la mine azi-noapte. Are un castel plin de morti-vii si o vrajitoare care a pus mana pe o cheie antica. Vrei sa cureti zona?"
      unavailable:
        - "Nu te trimit la castel fara o sabie buna. Ajuta-ma intai cu materialele."
      offered:
        - "E periculos acolo. Daca accepti, pregateste-te de lupta."
      accepted:
        - "Bun. Du-te la castel, intra pe poarta si cureaza fiecare zona. La capat o gasesti pe vrajitoare. Ia cheia si deschide cufarul din curte."
      active:
        - "Curata tot castelul. Nu lasa niciun zombi in viata."
      ready:
        - "Te-ai intors? Ai cheia si comoara?"
      completed:
        - "Excelent! Castelul e curatat, iar comoara merita. Dagon va fi mandru."
    stages:
      ACCEPTANCE:
        description: "Vorbeste cu fierarul si accepta misiunea."
        completion_mode: "manual_turn_in"
        next_stage: "ENTER_CASTLE"
        objectives:
          - "talk_blacksmith"
      ENTER_CASTLE:
        description: "Ajungi la castel si intri pe poarta."
        completion_mode: "all_objectives"
        next_stage: "ZONE_1"
        objectives:
          - "enter_castle"
          - "pass_gate"
      ZONE_1:
        description: "Prima zona: elimina 2 zombi."
        completion_mode: "all_objectives"
        next_stage: "ZONE_2"
        objectives:
          - "zone1_zombies"
      ZONE_2:
        description: "A doua zona: elimina 2 zombi."
        completion_mode: "all_objectives"
        next_stage: "ZONE_3"
        objectives:
          - "zone2_zombies"
      ZONE_3:
        description: "A treia zona: elimina 2 zombi."
        completion_mode: "all_objectives"
        next_stage: "ZONE_4"
        objectives:
          - "zone3_zombies"
      ZONE_4:
        description: "A patra zona: elimina 1 zombi."
        completion_mode: "all_objectives"
        next_stage: "BOSS"
        objectives:
          - "zone4_zombies"
      BOSS:
        description: "Sala tronului: invinge vrajitoarea si ia cheia."
        completion_mode: "all_objectives"
        next_stage: "TREASURE"
        objectives:
          - "kill_witch"
          - "find_key"
      TREASURE:
        description: "Deschide cufarul cu cheia antica."
        completion_mode: "all_objectives"
        next_stage: "RETURN"
        objectives:
          - "open_chest"
      RETURN:
        description: "Intoarce-te la fierar cu comoara."
        completion_mode: "manual_turn_in"
        objectives:
          - "return_blacksmith"
    objectives:
      talk_blacksmith:
        type: "talk_to_npc"
        item: "profession:blacksmith"
        amount: 1
        phase: "ACCEPTANCE"
        description: "Vorbeste cu fierarul despre castel."
      enter_castle:
        type: "visit_region"
        item: "castel"
        amount: 1
        phase: "ENTER_CASTLE"
        description: "Intra in regiunea castelului."
      pass_gate:
        type: "visit_place"
        item: "castel:poarta_castel"
        amount: 1
        phase: "ENTER_CASTLE"
        description: "Treci pe la poarta castelului."
      zone1_zombies:
        type: "kill_mob"
        item: "ZOMBIE"
        amount: 2
        phase: "ZONE_1"
        description: "Elimina 2 zombi in prima zona."
      zone2_zombies:
        type: "kill_mob"
        item: "ZOMBIE"
        amount: 2
        phase: "ZONE_2"
        description: "Elimina 2 zombi in a doua zona."
      zone3_zombies:
        type: "kill_mob"
        item: "ZOMBIE"
        amount: 2
        phase: "ZONE_3"
        description: "Elimina 2 zombi in a treia zona."
      zone4_zombies:
        type: "kill_mob"
        item: "ZOMBIE"
        amount: 1
        phase: "ZONE_4"
        description: "Elimina 1 zombi in a patra zona."
      kill_witch:
        type: "kill_mob"
        item: "WITCH"
        amount: 1
        phase: "BOSS"
        description: "Invinge vrajitoarea din sala tronului."
      find_key:
        type: "inspect_node"
        item: "castel:sala_boss:cheie_vrajitoare"
        amount: 1
        phase: "BOSS"
        description: "Ia cheia antica de langa vrajitoare."
      open_chest:
        type: "inspect_node"
        item: "castel:curte_castel:cufar"
        amount: 1
        phase: "TREASURE"
        description: "Deschide cufarul cu cheia antica."
      return_blacksmith:
        type: "talk_to_npc"
        item: "profession:blacksmith"
        amount: 1
        phase: "RETURN"
        description: "Intoarce-te la fierar cu comoara."
    rewards:
      enchanted_sword:
        type: "item"
        item: "DIAMOND_SWORD"
        amount: 1
        description: "Primesti o sabie de diamant."
      emeralds:
        type: "item"
        item: "EMERALD"
        amount: 3
        description: "Primesti 3 smaralde."
      xp_reward:
        type: "experience"
        amount: 100
        description: "Primesti 100 XP."
      castle_cleansed:
        type: "record_story_event"
        item: "castle_cleansed"
        amount: 1
        description: "Regiunea retine ca castelul a fost curatat."
        scope: "region"
        target: "current_region"
        event_type: "quest_completed"
        event_key: "cst02_castle_cleansed"
        title: "Castelul a fost curatat"
        payload:
          quest: "CST02"
          outcome: "castle_cleansed"
  roles:
    QUEST_GIVER:
      description: "Fierarul care trimite jucatorul la castel."
      required_professions:
        - "blacksmith"
      preferred_professions:
        - "blacksmith"
    HERO:
      description: "Jucatorul care cureata castelul."
      player_role: true
```
