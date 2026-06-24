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

## Prioritatea aliasurilor

Când același concept are mai multe aliasuri, ordinea de prioritate este:

1. **Tip canonic** — întotdeauna recomandat (ex: `talk_to_npc`)
2. **Alias tolerat** — acceptat, parsează la fel ca tipul canonic (ex: `talk_npc`, `speak_to_npc`)
3. **Alias deprecated** — acceptat dar emite warning de migrare (ex: `talk_nlc`, `interact_nkde`, `turnin`, `gather`, `slay`, `construct`, `fabricate`)

Regulă: aliasurile deprecated produc același rezultat ca tipul canonic, dar apar în `/ainpc quest deprecated` cu sugestie de înlocuire.

## Migrare de la aliasuri vechi la contract nou

Pentru a migra questurile existente:

1. Rulează `/ainpc quest deprecated` — vezi ce aliasuri deprecated sunt folosite
2. Înlocuiește fiecare alias cu tipul canonic conform tabelului de mai sus
3. Rulează `/ainpc audit quest` — confirmă 0 erori
4. Exemple de înlocuire:

| Alias vechi (deprecated) | Înlocuire canonică |
|---|---|
| `talk_nlc` | `talk_to_npc` |
| `interact_nkde` | `inspect_node` |
| `turnin` | `deliver_to_npc` |
| `gather` | `collect_item` |
| `slay` | `kill_mob` |
| `construct` | `place_block` |
| `fabricate` | `craft_item` |

Toate aliasurile tolerate (non-deprecated) rămân acceptabile, dar tipul canonic este întotdeauna recomandat pentru claritate.
