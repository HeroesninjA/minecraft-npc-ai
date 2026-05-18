# Quest Anchor Bindings

Actualizat: 2026-05-11

Status: implementat initial in cod, cu audit strict si comanda admin read-only.

## Scop

`quest_anchor_bindings` leaga obiectivele semantice de quest de ancore reale din mapping.

Fara acest strat, un obiectiv precum `visit_place: blacksmith` ramane doar text sau o referinta fragila. Cu binding-uri, questul salveaza explicit ce `region`, `place`, `node` sau `npc` a fost ales la momentul oferirii sau acceptarii.

## De ce exista

Questurile si story-ul nu trebuie sa depinda de coordonate brute sau de text generat de AI.

Fluxul corect este:

```text
quest template -> QuestAnchorResolver -> quest_anchor_bindings -> progres runtime
```

Astfel, AI-ul si engine-ul pot lucra cu ID-uri reale:

- `regionId`
- `placeId`
- `nodeId`
- `npcId`
- `objectiveKey`

## Tabela DB

Tabela este creata automat de `DatabaseManager`.

```sql
CREATE TABLE IF NOT EXISTS quest_anchor_bindings (
    player_uuid TEXT NOT NULL,
    template_id TEXT NOT NULL,
    objective_key TEXT NOT NULL,
    quest_code TEXT NOT NULL DEFAULT '',
    objective_type TEXT NOT NULL,
    reference TEXT NOT NULL DEFAULT '',
    anchor_type TEXT NOT NULL,
    anchor_id TEXT NOT NULL,
    anchor_label TEXT NOT NULL DEFAULT '',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (player_uuid, template_id, objective_key),
    FOREIGN KEY (player_uuid, template_id)
        REFERENCES player_quests(player_uuid, template_id)
        ON DELETE CASCADE
);
```

Index:

```sql
CREATE INDEX IF NOT EXISTS idx_quest_anchor_bindings_anchor
ON quest_anchor_bindings(anchor_type, anchor_id);
```

## Campuri

| Camp | Rol |
|---|---|
| `player_uuid` | Jucatorul pentru care a fost rezolvata ancora |
| `template_id` | Template-ul de quest |
| `objective_key` | Cheia obiectivului generata de runtime |
| `quest_code` | Codul de quest, daca exista |
| `objective_type` | Tipul obiectivului, de exemplu `visit_place` |
| `reference` | Referinta ceruta de template, de exemplu `tag:blacksmith` |
| `anchor_type` | Tipul ancorei rezolvate: `region`, `place`, `node`, `npc` |
| `anchor_id` | ID-ul semantic real din mapping sau NPC |
| `anchor_label` | Eticheta de afisare/debug |
| `created_at` | Timestamp la creare |
| `updated_at` | Timestamp la ultima rescriere |

## Flux runtime

La oferire, acceptare sau pornire manuala:

1. `ScenarioEngine` cere `QuestAnchorResolver` pentru template-ul de quest.
2. Resolverul verifica obiectivele semantice.
3. Daca lipsesc ancore obligatorii, questul este refuzat cu mesaj administrativ.
4. Daca ancorele sunt valide, progresul este salvat in `player_quests`.
5. Binding-urile sunt salvate in `quest_anchor_bindings`.
6. Pentru compatibilitate runtime, aceleasi ancore sunt reflectate si in `questVariables`.

La reload:

1. progresul se citeste din `player_quests`;
2. binding-urile se citesc din `quest_anchor_bindings`;
3. `questVariables` este hidratat din DB pentru obiectivele legate de mapping;
4. tracking-ul `visit_place` si `inspect_node` foloseste ancora persistata.

La reset/refuz/stergere progres:

- randurile din `quest_anchor_bindings` sunt sterse explicit;
- foreign key-ul cu `ON DELETE CASCADE` ramane protectie suplimentara.

## Tipuri suportate initial

Binding-urile sunt folosite initial pentru:

- `visit_region` -> `region`
- `visit_place` -> `place`
- `inspect_node` -> `node`
- `talk_to_npc` -> `npc`

Obiectivele non-semantice, precum `collect_item` sau `kill_mob`, nu au nevoie de rand in aceasta tabela.

## Reguli importante

- Questul nu trebuie oferit daca un obiectiv semantic obligatoriu nu poate fi rezolvat.
- Progresul trebuie comparat cu `anchor_id`, nu doar cu textul original din YAML.
- `questVariables` ramane compatibilitate si cache runtime, nu sursa finala de audit.
- `quest_anchor_bindings` nu inlocuieste `player_quests`; il completeaza.
- Statusul questului se citeste din `player_quests`, nu din tabela de binding-uri.

## Comenzi admin

Inspectie read-only:

```text
/ainpc quest anchors [jucator|uuid|all] [templateId]
```

Exemple:

```text
/ainpc quest anchors
/ainpc quest anchors all medieval:iron_shortage
/ainpc quest anchors Steve
/ainpc quest anchors 00000000-0000-0000-0000-000000000000 medieval:iron_shortage
```

Comanda listeaza cele mai recente binding-uri, cu:

- `player_uuid`
- `template_id`
- `objective_key`
- `objective_type`
- `anchor_type`
- `anchor_id`
- statusul questului din `player_quests`

Audit read-only:

```text
/ainpc audit quest
/ainpc audit quest strict
```

Auditul verifica initial quest templates si quest anchor bindings. Varianta normala pastreaza un plafon de audit pentru bindings, ca sa ramana folosibila in joc. Varianta `strict` verifica toate randurile din `quest_anchor_bindings`; `full` si `offline` sunt aliasuri pentru acelasi mod complet.

Pentru template-uri verifica:

- coduri duplicate
- profesii giver inexistente
- prerequisite-uri catre questuri inexistente
- obiective/recompense cu materiale sau entity invalide
- dialoguri, faze si roluri critice lipsa

Pentru binding-uri verifica:

- binding-uri fara progres parinte in `player_quests`
- `player_uuid` invalid
- campuri obligatorii lipsa
- `objective_key` inexistent in definitia progresiei incarcate
- incompatibilitate intre `objective_type` si `anchor_type`
- `region`, `place` sau `node` care nu mai exista in mapping
- `npc` anchor catre NPC care nu este incarcat in runtime

Debug dump complet:

```text
/ainpc debugdump quest
/ainpc debugdump all
```

Dump-ul include `loaded-quest-definitions.json`, `player-quest-progress.json` si `quest-anchor-bindings.json`. Pentru ancore, `quest-anchor-bindings.json` include:

- toate randurile din `quest_anchor_bindings`, fara limita de preview;
- statusul si faza questului parinte din `player_quests`, daca exista;
- agregari `by_template` si `by_anchor_type`;
- cheile stabile de obiectiv, de exemplu `visit_forge` sau `patrol_region`.

## Limitari curente

- `objective_key` foloseste cheia YAML/`entry_id` pentru binding-uri noi, cu fallback legacy pentru progres vechi.
- Comanda `/ainpc quest anchors` afiseaza un preview limitat; exportul complet este in `quest-anchor-bindings.json`, progresul parinte complet este in `player-quest-progress.json`, iar template-urile incarcate sunt in `loaded-quest-definitions.json`.
- Auditul normal verifica un set limitat de randuri pentru tinerea rezultatului scurt in joc; `/ainpc audit quest strict` scaneaza toate binding-urile.
- Daca mapping-ul este schimbat dupa acceptarea questului, binding-ul ramane catre ID-ul vechi pana la reset/reoffer.

## Avertizari

- Nu schimba ID-uri de `place` sau `node` pe un server live fara migration/backfill.
- Nu lasa AI-ul sa creeze direct randuri in aceasta tabela.
- Nu folosi `anchor_label` pentru logica; este doar pentru afisare si debug.
- Nu presupune ca un binding valid la creare ramane valid dupa editari manuale in mapping.

## Urmatoarele faze

Prioritate recomandata:

1. adauga repair/backfill pentru binding-uri dupa rename de mapping;
2. extinde `StoryContextService` dupa ce exista story state persistent;
3. adauga `quest_story_links` dupa ce story state-ul este persistent.
