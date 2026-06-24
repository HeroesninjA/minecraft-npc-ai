# Exemple de Obiective pentru Quest YAML

## 1. Social — talk_to_npc
```yaml
talk_to_guard:
  type: "talk_to_npc"
  item: "profession:garda"
  amount: 1
  description: "Vorbeste cu gardianul."
```
Aliasuri: `talk`, `speak`, `talk_npc`, `talk_nlc`, `conversation`

## 2. Colectare — collect_item
```yaml
gather_wood:
  type: "collect_item"
  item: "OAK_LOG"
  amount: 10
  description: "Colecteaza 10 busteni."
```
Aliasuri: `collect`, `fetch`, `gather`, `item`

## 3. Explorare — visit_place
```yaml
reach_market:
  type: "visit_place"
  item: "demo_sat:piata"
  amount: 1
  description: "Ajungi in piata satului."
```
Aliasuri: `visitplace`, `go_to_place`, `enter_place`

## 4. Inspectare — inspect_node
```yaml
check_chest:
  type: "inspect_node"
  item: "demo_sat:house_1:bed_1"
  amount: 1
  description: "Inspecteaza patul din casa."
```
Aliasuri: `inspect`, `inspectnode`, `interact_nkde`

## 5. Combat — kill_mob
```yaml
kill_zombies:
  type: "kill_mob"
  item: "ZOMBIE"
  amount: 3
  description: "Ucide 3 zombies."
```
Aliasuri: `kill`, `slay`, `defeat`

## 6. Constructie — place_block
```yaml
build_fence:
  type: "place_block"
  item: "OAK_FENCE"
  amount: 5
  description: "Plaseaza 5 garduri."
```
Aliasuri: `placeblock`, `build`, `construct`

## 7. Spargere — break_block
```yaml
mine_stone:
  type: "break_block"
  item: "COBBLESTONE"
  amount: 10
  description: "Sparge 10 pietre."
```
Aliasuri: `break`, `mine`, `dig`, `excavate`

## 8. Crafting — craft_item
```yaml
craft_sword:
  type: "craft_item"
  item: "IRON_SWORD"
  amount: 1
  description: "Forjeaza o sabie."
```
Aliasuri: `craft`, `make`, `fabricate`, `create_item`

## 9. Utilizare — use_item
```yaml
drink_potion:
  type: "use_item"
  item: "POTION"
  amount: 1
  description: "Bea o poțiune."
```
Aliasuri: `use`, `consume`, `drink`, `eat`, `activate`

## 10. Echipare — equip_item
```yaml
wear_armor:
  type: "equip_item"
  item: "IRON_CHESTPLATE"
  amount: 1
  description: "Echipeaza o platoșă."
```
Aliasuri: `equip`, `wear`, `don`, `put_on`

## 11. Livrare — deliver_to_npc
```yaml
deliver_food:
  type: "deliver_to_npc"
  item: "profession:fermier"
  amount: 1
  description: "Preda mancarea fermierului."
```
Aliasuri: `deliver`, `turn_in`, `deliver_item`

## 12. Vizitare regiune — visit_region
```yaml
enter_castle:
  type: "visit_region"
  item: "castel"
  amount: 1
  description: "Intra in regiunea castelului."
```
Aliasuri: `visit`, `travel`, `go_to`, `enter_region`
