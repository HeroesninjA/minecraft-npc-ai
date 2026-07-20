# Exemple operationale de obiective YAML

Status: referinta operationala.
Verificat in cod: 2026-07-15.
Depinde de: `reference/objective-types-reference.md`.

Exemplele folosesc numai nume canonice. Catalogul si aliasurile raman in documentul de care depinde aceasta pagina.

## Inventar si actiuni

```yaml
objectives:
  gather_wood:
    type: "collect_item"
    item: "OAK_LOG"
    amount: 10
    description: "Colecteaza 10 busteni de stejar."
  craft_sword:
    type: "craft_item"
    item: "IRON_SWORD"
    amount: 1
    description: "Craft-uieste o sabie de fier."
  equip_helmet:
    type: "equip_item"
    item: "IRON_HELMET"
    amount: 1
    description: "Echipeaza o casca de fier."
```

Pentru `place_block`, `break_block` si `use_item`, pastreaza aceeasi forma si schimba tipul plus materialul urmarit.

## Combat si interactiune NPC

```yaml
objectives:
  clear_zombies:
    type: "kill_mob"
    item: "ZOMBIE"
    amount: 3
    description: "Ucide 3 zombie."
  report_to_guard:
    type: "talk_to_npc"
    item: "profession:guard"
    amount: 1
    description: "Raporteaza gardianului."
```

## Mapping semantic

```yaml
objectives:
  enter_village:
    type: "visit_region"
    item: "demo_sat"
    amount: 1
    description: "Intra in regiunea satului."
  enter_house:
    type: "visit_place"
    item: "demo_sat:house_1"
    amount: 1
    description: "Intra in casa."
  inspect_bed:
    type: "inspect_node"
    item: "demo_sat:house_1:bed_1"
    amount: 1
    description: "Inspecteaza patul."
```

ID-urile trebuie sa existe in mapping sau sa poata fi rezolvate neambiguu prin tip, tag ori metadata. Un selector rezolvat in preview nu inlocuieste un binding persistent valid.

## Delivery

```yaml
objectives:
  deliver_stone:
    type: "deliver_to_npc"
    item: "COBBLESTONE"
    amount: 2
    description: "Preda 2 pietre la turn-in."
```

Acesta este formatul folosit de pack-ul demonstrativ pentru inventar si consum. Schema curenta nu are un camp separat, canonic, pentru NPC-ul destinatar, iar resolver-ul de ancore reutilizeaza `item`. Testeaza explicit materialul, NPC-ul si consumul inainte de folosire in productie.

## Sursa completa

Exemplul care contine toate cele 12 tipuri este `ainpc-scenario-medieval/src/main/resources/packs/sample_objectives_quest.yml`.

## Legaturi

- `reference/objective-types-reference.md`
- `reference/json-yaml-contract.md`
- `reference/quest-anchor-bindings.md`
