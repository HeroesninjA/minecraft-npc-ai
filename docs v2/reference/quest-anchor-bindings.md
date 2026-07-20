# Contractul quest anchor bindings

Status: referinta canonica.
Verificat in cod: 2026-07-15.

Binding-urile leaga o cheie de obiectiv de o ancora semantica rezolvata. Ele completeaza definitia din pack; nu inlocuiesc mapping-ul si nu transforma coordonatele brute in identificatori stabili.

## Model persistent

Tabela `quest_anchor_bindings` foloseste cheia unica `(player_uuid, template_id, objective_key)` si pastreaza:

- `quest_code`, `objective_type` si referinta originala;
- `anchor_type`, `anchor_id` si eticheta afisata;
- `created_at` si `updated_at`;
- statusul progresiei este citit prin join cu `player_quests`, nu stocat in binding.

Salvarea respinge un binding fara player UUID, template, objective key, objective type, anchor type sau anchor ID. Un upsert pastreaza `created_at` si actualizeaza restul campurilor pentru aceeasi cheie.

## Rezolvare

- `visit_region`, `visit_place` si `inspect_node` se rezolva prin `WorldAdminApi`;
- `talk_to_npc` si, provizoriu, `deliver_to_npc` se rezolva spre ancora `npc`;
- un binding preexistent cu acelasi `objective_key` are prioritate fata de rezolvarea din definitie;
- binding-urile jucatorului curent sunt adaugate primele;
- lipsa world admin sau a mapping-ului necesar produce issue explicit pentru ancorele world.

## Limite cunoscute

### Fallback-ul numit global (REMEDIAT)

Binding-urile cu `player_uuid = "__global__"` formeaza un namespace global persistent. 
`ProgressionAnchorBinding.GLOBAL_PLAYER_UUID` defineste constanta sentinel. 
`QuestAnchorBindingService` interogheaza binding-urile personale (UUID real) si binding-urile 
din namespace-ul global (`__global__`), aplicand override personal peste global per `objectiveKey`.

`queryAnchorBindings` cu `"__global__"` filtreaza explicit `b.player_uuid = '__global__'`. 
`saveAnchorBinding` si `deleteAnchorBinding` accepta `__global__` ca valoare valida. 
Query-ul cu player UUID gol (`""`) pastreaza comportamentul fara filtru pentru 
operatiuni administrative (toti jucatorii).

### Delivery (REMEDIAT)

`deliver_to_npc` si `talk_to_npc` suporta campul optional `npc_target` in YAML pentru 
selectia explicita a NPC-ului destinatar. `QuestEntryDefinition.npcTarget` expune campul 
din metadata. `QuestAnchorResolver` foloseste `npc_target` cu prioritate, apoi `item` 
ca fallback pentru compatibilitate cu pack-urile vechi, si in final quest giver-ul.

Materialul livrat ramane in `item`, separat de selectia NPC-ului.

## Reguli

- foloseste objective key explicit si stabil;
- foloseste ID-uri semantice calificate cand exista ambiguitate;
- un preview sau o rezolvare in memorie nu garanteaza persistenta;
- binding-urile globale folosesc `player_uuid = "__global__"` (`ProgressionAnchorBinding.GLOBAL_PLAYER_UUID`);
- nu folosi `player_uuid` gol pentru namespace global; acesta ramane filtru administrativ fara filtru de jucator;
- orice remediere trebuie sa includa migrare, audit si test de izolare intre jucatori.

## Surse tehnice

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression/ProgressionAnchorBinding.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/progression/ProgressionRepository.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestAnchorBindingService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestAnchorResolver.kt`

## Legaturi

- `architecture/mapping.md`
- `architecture/progression-service.md`
- `reference/objective-types-reference.md`
- `planning/questuri-avansate-v2.md`
