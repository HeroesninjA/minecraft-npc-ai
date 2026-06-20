# Exemple JSON/YAML pentru quest, mapping si world_admin

Acest document completeaza `docs/json-yaml-contract.md` cu exemple concrete, gata de folosit ca referinta pentru autori, migrari si verificari rapide.

## Quest

### `quests.yml`

```yaml
type: quest
version: 1
meta:
  id: quest_intro_market
  title: "Misiunea din piata"
spec:
  objectives:
    - id: reach_market
      trigger: quest.started
      hook: resolve_place_target
      target:
        kind: place
        ref: tag:market
  rewards:
    - type: item
      item: emerald
      amount: 3
```

### `quests.json`

```json
{
  "type": "quest",
  "version": 1,
  "meta": {
    "id": "quest_intro_market",
    "title": "Misiunea din piata"
  },
  "spec": {
    "objectives": [
      {
        "id": "reach_market",
        "trigger": "quest.started",
        "hook": "resolve_place_target",
        "target": {
          "kind": "place",
          "ref": "tag:market"
        }
      }
    ],
    "rewards": [
      {
        "type": "item",
        "item": "emerald",
        "amount": 3
      }
    ]
  }
}
```

## Mapping

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
      min: { x: 100, y: 60, z: 100 }
      max: { x: 220, y: 90, z: 220 }
      tags: [village, public]
      places:
        piata:
          name: "Piata"
          type: market
          min: { x: 140, y: 64, z: 140 }
          max: { x: 155, y: 74, z: 155 }
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
    "auto_index": {
      "enabled": true
    },
    "regions": {
      "satul_central": {
        "name": "Satul Central",
        "world": "world",
        "type": "settlement",
        "min": { "x": 100, "y": 60, "z": 100 },
        "max": { "x": 220, "y": 90, "z": 220 },
        "tags": ["village", "public"],
        "places": {
          "piata": {
            "name": "Piata",
            "type": "market",
            "min": { "x": 140, "y": 64, "z": 140 },
            "max": { "x": 155, "y": 74, "z": 155 },
            "tags": ["market", "public"],
            "nodes": {
              "quest_board": {
                "type": "quest_trigger",
                "x": 148,
                "y": 65,
                "z": 148,
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

## Legaturi utile

- `docs/json-yaml-contract.md`
- `docs/progression-service.md`
- `docs/mapping.md`
- `docs/questuri-avansate-v2.md`
- Fixture-uri de test: `ainpc-core-plugin/src/test/resources/json-yaml-contract/quests.yml`, `ainpc-core-plugin/src/test/resources/json-yaml-contract/quests.json`, `ainpc-core-plugin/src/test/resources/json-yaml-contract/world-admin.yml`, `ainpc-core-plugin/src/test/resources/json-yaml-contract/world-admin.json`
