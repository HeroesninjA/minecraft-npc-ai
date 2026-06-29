# Exemple JSON/YAML pentru feature packs, quest si world_admin

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

Acest document completeaza `docs/json-yaml-contract.md` cu exemple concrete in formatul real folosit de AINPC.

## Feature Pack cu quest

### `quests.yml` (YAML)

```yaml
id: contract_example
name: "Contract Example"
description: "Exemplu de feature pack."
addon:
  type: "feature"
  version: "1.0.0"
  capabilities: ["quest", "traits", "dialogues"]

scenarios:
  quest_intro_market:
    name: "Misiunea din piata"
    description: "Viziteaza piata satului si vorbeste cu negustorul."
    base_type: "QUEST"
    mechanic: "side_quests"
    quest:
      code: "QM01"
      giver: "merchant"
      kind: "side"
      category: "tutorial"
      dialogues:
        available:
          - "Bine ai venit in piata!"
          - "Am nevoie de ajutorul tau."
        active:
          - "Primarul te asteapta in piata."
        completed:
          - "Multumesc! Afacerea e incheiata."
      objectives:
        visit_market:
          type: "visit_place"
          item: "tag:market"
          amount: 1
          description: "Viziteaza piata satului"
        talk_to_merchant:
          type: "talk_to_npc"
          item: "npc:merchant"
          amount: 1
          description: "Vorbeste cu negustorul"
      rewards:
        reward_coin:
          type: "economy:money"
          item: "gold_nugget"
          amount: 5
          description: "5 bucati de aur"

story_defaults:
  settlement:
    default_state: "peaceful"
    pool:
      - "peaceful"
      - "trade"

dialogues:
  greeting:
    - "Bine ai venit!"
  farewell:
    - "La revedere!"
```

### `quests.json` (JSON)

```json
{
  "id": "contract_example",
  "name": "Contract Example",
  "description": "Exemplu de feature pack.",
  "addon": {
    "type": "feature",
    "version": "1.0.0",
    "capabilities": ["quest", "traits", "dialogues"]
  },
  "scenarios": {
    "quest_intro_market": {
      "name": "Misiunea din piata",
      "description": "Viziteaza piata satului si vorbeste cu negustorul.",
      "base_type": "QUEST",
      "mechanic": "side_quests",
      "quest": {
        "code": "QM01",
        "giver": "merchant",
        "kind": "side",
        "category": "tutorial",
        "dialogues": {
          "available": ["Bine ai venit in piata!", "Am nevoie de ajutorul tau."],
          "active": ["Primarul te asteapta in piata."],
          "completed": ["Multumesc! Afacerea e incheiata."]
        },
        "objectives": {
          "visit_market": { "type": "visit_place", "item": "tag:market", "amount": 1, "description": "Viziteaza piata" },
          "talk_to_merchant": { "type": "talk_to_npc", "item": "npc:merchant", "amount": 1, "description": "Vorbeste cu negustorul" }
        },
        "rewards": {
          "reward_coin": { "type": "economy:money", "item": "gold_nugget", "amount": 5, "description": "5 bucati de aur" }
        }
      }
    }
  }
}
```

## World Admin mapping

### `world-admin.yml`

```yaml
world_admin:
  enabled: true
  auto_index:
    enabled: true
  regions:
    satul_central:
      name: "Satul Central"
      world: "world"
      type: settlement
      min_x: 100
      min_y: 60
      min_z: 100
      max_x: 220
      max_y: 90
      max_z: 220
      tags: [village, public]
      story_state_key: "default"
      story_mode: "evolutive"
      places:
        piata:
          type: market
          min_x: 140
          min_y: 64
          min_z: 140
          max_x: 155
          max_y: 74
          max_z: 155
          tags: [market, public]
          nodes:
            quest_board:
              type: quest_trigger
              x: 148
              y: 65
              z: 148
              radius: 2.0
```

### `world-admin.json`

```json
{
  "world_admin": {
    "enabled": true,
    "auto_index": { "enabled": true },
    "regions": {
      "satul_central": {
        "name": "Satul Central",
        "world": "world",
        "type": "settlement",
        "min_x": 100, "min_y": 60, "min_z": 100,
        "max_x": 220, "max_y": 90, "max_z": 220,
        "tags": ["village", "public"],
        "story_state_key": "default",
        "story_mode": "evolutive",
        "places": {
          "piata": {
            "type": "market",
            "min_x": 140, "min_y": 64, "min_z": 140,
            "max_x": 155, "max_y": 74, "max_z": 155,
            "tags": ["market", "public"],
            "nodes": {
              "quest_board": {
                "type": "quest_trigger",
                "x": 148, "y": 65, "z": 148,
                "radius": 2.0
              }
            }
          }
        }
      }
    }
  }
}
```

## Tipuri suportate

### Tipuri de quest (`base_type`)
| Tip | Descriere |
|-----|-----------|
| `QUEST` | Quest standard cu obiective si recompense |
| `BOUNTY` | Bounty local, repeatabil cu cooldown |
| `WORLD_EVENT` | Eveniment local temporar |
| `TUTORIAL` | Tutorial/onboarding pentru jucatori |
| `RITUAL` | Ritual local cu participare periodica |
| `TRADE_DEAL` | Contract/investigatie non-quest |
| `DUTY` | Sarcina NPC |

### Tipuri de obiective
| Tip | Parametri |
|-----|-----------|
| `collect_item` | `item: MATERIAL`, `amount: N` |
| `deliver_to_npc` | `item: MATERIAL`, `amount: N` |
| `talk_to_npc` | `item: npc:NAME` |
| `visit_region` | `item: tag:TAG` sau `region:ID` |
| `visit_place` | `item: tag:TAG` sau `place:ID` |
| `inspect_node` | `item: tag:TAG` sau `node:ID` |
| `kill_mob` | `item: ENTITY_TYPE`, `amount: N` |
| `place_block` | `item: MATERIAL`, `amount: N` |
| `break_block` | `item: MATERIAL`, `amount: N` |
| `craft_item` | `item: MATERIAL`, `amount: N` |
| `use_item` | `item: MATERIAL` |
| `equip_item` | `item: MATERIAL` |

### Tipuri de recompense
| Tip | Efect |
|-----|-------|
| `item` | Da un item din inventar |
| `experience` | XP vanilla |
| `economy:money` | Monede in economy |
| `progression:xp` | XP in sistemul de progresie |
| `progression:level` | Seteaza nivel jucator |
| `progression:skill` | XP la un skill |
| `progression:skill_level` | Seteaza nivel skill |
| `reputation:region` | Reputatie in regiune |
| `reputation:faction` | Reputatie in factiune |
| `command` | Executa comanda consola |
| `set_story_state` | Seteaza o stare narativa |
| `record_story_event` | Inregistreaza un eveniment story |

## Legaturi utile

- `docs/json-yaml-contract.md`
- `docs/progression-service.md`
- `docs/mapping.md`
- `docs/questuri-avansate-v2.md`
- `docs/gui-interfete.md`
- Test fixtures: `ainpc-core-plugin/src/test/resources/json-yaml-contract/`
- Pack-uri reale: `ainpc-scenario-medieval/src/main/resources/packs/`
