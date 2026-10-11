# NPC World Bindings

Status: contract persistent verificat in cod.
Actualizat: 2026-10-02.

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

## Rezidență și proprietate

- `WorldAdminService.addNpcResidentToHomePlace` actualizează rezidenții fără să atribuie sau să schimbe proprietarul.
- `WorldAdminService.assignHomeOwner` cere ca NPC-ul să fie deja rezident și ca locuința să nu aibă alt proprietar. Un conflict este refuzat fără schimbarea câmpurilor proprietarului; aceeași identitate poate fi reaplicată.
- API-ul existent `bindNpcToHomePlace` păstrează compatibilitatea atribuirii inițiale într-o casă fără proprietar. Dacă există deja un proprietar, alt rezident nu îl înlocuiește. Acest apel nu mai poate fi folosit implicit pentru transferul proprietății.
- Pentru spawn de gospodărie/sat, `HouseholdOwnerBinding` rezolvă `primaryOwnerNpcKey` din alocarea originală la NPC-ul creat/reutilizat și ID-ul `npc_<databaseId>`. Cheia absentă/ambiguă ori rezultatul invalid blochează binding-ul; nu există alegere implicită a primului sau ultimului rezident.
- Rezidenții sunt legați separat; proprietarul este atribuit numai după reușita binding-urilor mapping și persistente ale tuturor rezidenților. Eșecul atribuirii produce `PARTIAL`, păstrează progresul și eroarea și nu declanșează rollback.
- Operațiile home invalidează listele cache din WorldAdmin și marchează mapping-ul ca modificat. Persistența configurației rămâne responsabilitatea mecanismului existent de salvare.
- Proprietarii istorici greșiți sau selectorii legacy neechivalenți textual cu ID-ul canonic cer verificare, backup și recuperare autorizată; acest lot nu introduce transfer automat sau migrare.

Validare locală: teste de ordine a rezidenților, idempotentă, NPC reutilizat, conflicte, rezultate parțiale și round-trip YAML. Nu reprezintă încă dovadă de restart/autosave pe Paper sau tranzacție atomică între SQL și mapping.

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
