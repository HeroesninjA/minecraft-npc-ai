# NPC World Bindings

Actualizat: 2026-05-07

## Scop

`npc_world_bindings` este tabela dedicata pentru legatura dintre NPC-uri si mapping-ul semantic.

Ea completeaza fallback-ul vechi din `npc_profiles.profile_data.owned_locations`:

- `profile_data` pastreaza coordonate si ancore runtime
- `npc_world_bindings` pastreaza ID-uri stabile de `place` si `node`

## Schema initiala

Un rand reprezinta binding-ul curent pentru un NPC:

- `npc_id`
- `npc_uuid`
- `npc_name`
- `home_place_id`
- `work_place_id`
- `social_place_id`
- `home_node_id`
- `work_node_id`
- `social_node_id`
- `family_id`
- `source`
- `created_at`
- `updated_at`

## Ce scrie tabela acum

Tabela este actualizata initial din:

- `/ainpc world bind npc ...`
- `/ainpc world household spawn ...`
- `/ainpc world settlement spawn ...`
- backfill initial din ancorele existente, daca ancorele cad intr-un mapped place

Surse folosite:

- `manual_bind`
- `spawn_plan`
- `profile_backfill`

## Ce ramane fallback

Inca se scrie si metadata pe `WorldPlace`:

- `owner_npc_id`
- `resident_npc_ids`
- `worker_npc_ids`
- `social_npc_ids`

Inca se scriu si ancorele runtime in `npc_profiles.profile_data.owned_locations`.

Aceasta compatibilitate ramane intentionata pana cand auditul si rutina folosesc complet tabela dedicata.

## Audit

`/ainpc audit db` raporteaza acum numarul de randuri din `npc_world_bindings` si valideaza:

- binding-uri orfane fara NPC
- binding-uri fara niciun place
- place-uri inexistente
- node-uri inexistente
- node-uri care nu apartin place-ului asteptat
- `home_place_id` care nu este casa/home
- `work_place_id` care nu este workplace
- `social_place_id` care nu este loc social clar

## Inspectie read-only

Exista comanda admin read-only:

```text
/ainpc world bindings [limit]
/ainpc world bindings list [limit]
/ainpc world bindings npc <numeNpc|nearest|npcId|uuid>
/ainpc world bindings place <placeId> [limit]
```

Comanda listeaza randurile persistente, afiseaza sursa, timestamp-urile, `home/work/social place_id`, `home/work/social node_id` si, cand mapping-ul este incarcat, rezolva place/node info pentru verificare rapida.

## Debugdump

`/ainpc debugdump world` si `/ainpc debugdump all` exporta `npc-world-bindings.json`.

Exportul contine:

- toate randurile din `npc_world_bindings`;
- agregari pe `source`, `home_place_id`, `work_place_id` si `social_place_id`;
- `loaded_npc` pentru NPC-urile incarcate in runtime;
- rezolvare place/node cand WorldAdmin este activ;
- contoare pentru referinte place/node lipsa.

## Urmatorii pasi

- integrare cu modelul `households` / `household_residents`; vezi `households-persistente.md`
- migration/backfill mai explicit pentru servere vechi
- audit dedicat `worldbindings`
- integrare completa in rutina si spawn-order, astfel incat coordonatele din `profile_data` sa ramana doar cache runtime
