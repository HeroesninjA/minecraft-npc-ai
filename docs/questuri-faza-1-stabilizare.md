# Questuri - Faza Q1 Stabilizare

Actualizat: 2026-05-03

## Scop

Faza Q1 stabilizeaza questurile medievale existente inainte de questuri mai complexe pe mapping.

Obiectivul este ca Q01-Q05 sa fie sigure pentru test pe server:

- questurile se incarca din pack
- giver profession exista
- prerequisite-urile indica questuri reale
- obiectivele folosesc tipuri suportate
- recompensele folosesc iteme valide sau actiuni story valide
- `/ainpc audit quest` raporteaza problemele de template si binding

## Implementat in aceasta faza

- `/ainpc audit quest` verifica acum si quest templates, nu doar `quest_anchor_bindings`.
- Auditul raporteaza numarul de quest templates incarcate.
- Auditul verifica:
  - `quest.code` duplicat
  - `requires_player`
  - `quest.giver_profession`
  - profesie inexistenta
  - prerequisite necunoscut
  - `repeatable` fara cooldown
  - faze lipsa
  - dialoguri lipsa
  - rol `QUEST_GIVER` lipsa sau nealiniat cu `giver_profession`
  - materiale Minecraft invalide pentru obiective/recompense
  - entity invalid pentru `kill_mob`
  - actiuni story incomplete
- Testul `MedievalQuestPackTest` verifica Q01-Q05 mai strict pentru contract runtime.
- Scriptul `scripts/smoke-paper-quests.ps1` pregateste JAR-urile si comenzile de test pentru server.

## Comenzi de test pe server

Dupa restart cu JAR-ul nou:

```text
ainpc audit quest
ainpc quest log <player>
ainpc quest nearest <player>
ainpc quest accept nearest <player>
ainpc quest status nearest <player>
ainpc quest track start <player>
```

Pentru Q01, stai langa fierar si testeaza finalizarea rapida:

```text
give <player> iron_ingot 3
give <player> oak_planks 1
ainpc quest status nearest <player>
ainpc quest nearest <player>
ainpc quest log <player>
ainpc quest anchors <player>
ainpc audit quest
```

## Ce ramane pentru fazele urmatoare

- smoke test manual Q01-Q05 cap-coada pe server live
- debugdump complet pentru `player_quests` si `quest_anchor_bindings`
- 1-2 questuri legate direct de `visit_place` si `inspect_node`
- `objectiveId` explicit in YAML
- etape reale persistente
