# NPC World Bindings

Status: contract persistent verificat in cod.
Actualizat: 2026-07-18.

`npc_world_bindings` leaga un rand NPC de ID-uri din mapping. Cheia este `npc_id`, cu foreign key catre `npcs` si stergere in cascada.

## Campuri

- identitate derivata: `npc_uuid`, `npc_name`;
- `home/work/social_place_id`;
- `home/work/social_node_id`;
- `family_id`, `source`, `created_at`, `updated_at`.

## Citire si precedenta

1. profilul NPC hidrateaza ancorele vechi din `profile_data`;
2. daca mapping-ul este activ si exista binding, ID-urile rezolvabile suprascriu ancora rolului;
3. un ID lipsa sau nerezolvabil nu sterge ancora deja hidratata;
4. backfill-ul automat din ancore ruleaza numai daca randul dedicat lipseste si exista cel putin un place mapabil.

Tabela este autoritatea pentru ID-urile salvate, dar nu este o autoritate stricta pentru golirea ancorelor runtime.

## Binding dupa spawn

- `NpcSpawnOrchestrator` incheie spawn-ul si persistenta household inaintea binding-ului world;
- comenzile `household spawn` si `settlement spawn` aplica apoi metadata home/work/social si incearca `NpcWorldBindingService.saveBinding()`;
- `PostSpawnBindingResult` numara progresul mapping si persistenta, pastreaza erorile si clasifica rezultatul `COMPLETE`, `PARTIAL`, `FAILED` sau `NOT_APPLICABLE`;
- un esec dupa una sau mai multe scrieri mapping poate lasa progres partial; nu exista snapshot/restore pentru metadata place-ului;
- mesajul final spune explicit ca NPC-urile raman spawnate si ca nu s-a executat rollback.

## Validare reala

- `NpcWorldBindingService.saveBinding()` cere doar binding nenul si `npcId > 0`;
- existenta NPC-ului este protejata de foreign key;
- serviciul nu verifica existenta place/node, apartenenta node-ului la place sau validitatea familiei;
- comenzile de inspectie, audit si repair fac verificarile semantice suplimentare;
- `repair npc-bindings` poate reconstrui diferentele din ancorele profilului, cu dry-run/apply separat.

## Limite

- `family_id` este metadata; nu creeaza relatii in `npc_family`;
- binding-ul nu creeaza household;
- rutina consuma ancorele rezolvate, nu citeste tabela direct la fiecare tick.

## Legaturi

- `architecture/mapping.md`
- `architecture/households-persistente.md`
- `architecture/comportament-natural-npc-rutine-alocari.md`
- `planning/npc-population-world-stack.md`
