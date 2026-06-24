# Objective Types — Referință Canonică

| Tip canonic | Aliases | Hook runtime | Categorie |
|---|---|---|---|
| `talk_to_npc` | talk, speak, conversation, talk_npc, talk_nlc, speak_to_npc | NPC interaction | Social |
| `deliver_to_npc` | deliver, deliveritem, deliver_item, turnin, turn_in | NPC interaction | Social |
| `visit_region` | visit, travel, go_to, enter_region | PlayerMoveEvent | Explorare |
| `visit_place` | visitplace, enterplace, enter_place, go_to_place | PlayerMoveEvent | Explorare |
| `inspect_node` | inspect, inspectnode, interact_node, interact_nkde | PlayerMoveEvent / Node interaction | Explorare |
| `collect_item` | item, collect, collectitem, fetch, gather | EntityPickupItemEvent / Inventory check | Colectare |
| `craft_item` | craft, craftitem, make, create_item, fabricate | CraftItemEvent | Crafting |
| `use_item` | use, consume, drink, eat, activate, utilize | PlayerInteractEvent | Crafting |
| `equip_item` | equip, wear, don, put_on | InventoryClickEvent (armor slots) | Crafting |
| `place_block` | placeblock, build, construct | BlockPlaceEvent | Construcție |
| `break_block` | break, breakblock, mine, dig, excavate | BlockBreakEvent | Construcție |
| `kill_mob` | kill, slay, defeat | EntityDeathEvent | Combat |

## Metadata obligatorie

| Câmp | Tip | Obligatoriu | Descriere |
|---|---|---|---|
| `type` | string | da | Tipul canonic sau alias |
| `item` | string | da* | Referința țintei (entitate, material, profesie, locație) |
| `amount` | int | da | Cantitatea necesară |
| `description` | string | nu | Text afișat jucătorului |

*Exceptie: `kill_mob` poate funcționa fără `item` specific.

## Reguli de normalizare

- Intrare: `trim()` + `lowercase()` + înlocuire caractere non-alfanumerice cu `_` + eliminare prefix `minecraft:`
- Tipul gol → `collect_item`
- Tip nerecunoscut → trece prin nemodificat (poate fi alias custom)
- Aliasurile tolerate produc același rezultat ca tipul canonic
