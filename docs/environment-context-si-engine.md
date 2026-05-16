# Environment Context si EnvironmentEngine

Actualizat: 2026-05-04

## Scop

Acest document separa clar ce este necesar acum pentru contextul de mediu si ce ramane feature viitor pentru un `EnvironmentEngine` complet.

Pentru questuri avansate de baza nu este nevoie de un motor de environment separat. Este nevoie de un serviciu read-only care raspunde la intrebari despre lume:

- unde este jucatorul;
- in ce regiune, place sau node se afla;
- ce timp, vreme sau world event este activ;
- ce story state este relevant pentru locul curent;
- ce semnale de simulare pot deveni utile mai tarziu.

## Decizie principala

Implementarea recomandata este in doua trepte:

| Treapta | Cand | Rol |
|---|---|---|
| `EnvironmentContextService` | acum | snapshot read-only despre lume, mapping si stare curenta |
| `EnvironmentEngine` | viitor | observa lumea, produce semnale, alimenteaza `QuestDirector` |

Regula: daca doar verifici unde este jucatorul sau ce stare are locul curent, folosesti context. Daca generezi sau activezi questuri pe baza starii dinamice a lumii, ai nevoie de engine.

## Ce exista in jur

Componentele relevante:

| Componenta | Rol |
|---|---|
| `QuestEngine` | ruleaza questuri, obiective, progres si finalizare |
| `StoryStateService` | scrie si citeste stari narative persistente |
| `StoryContextService` | ofera context narativ read-only pentru AI/debug |
| `WorldAdminService` / mapping | regiuni, places, nodes si ancore semantice |
| `QuestAnchorResolver` | rezolva referinte de quest catre ancore reale |
| `QuestDirector` | viitor: recomanda questuri pe baza contextului |
| `EnvironmentContextService` | acum: snapshot read-only al mediului |
| `EnvironmentEngine` | viitor: semnale sistemice din starea lumii |

## Ce trebuie implementat acum

### `EnvironmentContextService`

Contract minim:

```text
EnvironmentContextService
- snapshotForPlayer(player)
- snapshotForLocation(location)
- findCurrentRegion(location)
- findCurrentPlace(location)
- findNearbyNode(location, radius)
- currentTime(world)
- currentWeather(world)
- activeWorldEvents(location)
- storySnapshot(location)
```

Snapshot minim:

```text
EnvironmentContextSnapshot
- world_id
- region_id
- region_label
- place_id
- place_label
- nearby_node_id
- nearby_node_label
- minecraft_time
- time_bucket
- weather
- active_world_events
- story_state_summary
```

Exemplu JSON:

```json
{
  "world_id": "overworld",
  "region_id": "satul_central",
  "region_label": "Satul Central",
  "place_id": "satul_central:fierarie",
  "place_label": "Fierarie",
  "nearby_node_id": "satul_central:fierarie:anvil",
  "nearby_node_label": "Nicovala",
  "minecraft_time": 13200,
  "time_bucket": "night",
  "weather": "clear",
  "active_world_events": [],
  "story_state_summary": {
    "forge_state": "supplied"
  }
}
```

Reguli:

- serviciul este read-only;
- nu modifica progres de quest;
- nu aplica reward-uri;
- nu genereaza questuri;
- nu scrie story state;
- poate fi cache-uit pe termen scurt;
- poate fi folosit de quest, story context, AI prompt si debug.

## De ce nu implementam EnvironmentEngine acum

Un `EnvironmentEngine` complet adauga complexitate mare:

- trebuie sa observe lumea;
- trebuie sa produca semnale;
- trebuie sa aiba cooldown;
- trebuie sa nu scaneze excesiv;
- trebuie sa supravietuiasca restartului;
- trebuie sa interactioneze cu `QuestDirector`;
- trebuie sa fie auditat.

Pentru questuri precum:

```text
Adu 8 OAK_PLANKS si mergi la forja.
```

nu este nevoie de asa ceva. Este suficient:

```text
QuestEngine
Objective handlers
QuestAnchorResolver
EnvironmentContextService
WorldAdminApi / mapping
```

## Cand devine necesar EnvironmentEngine

`EnvironmentEngine` devine util cand questurile depind de starea dinamica a lumii, nu doar de progresul jucatorului.

Exemple:

| Stare de lume | Rezultat posibil |
|---|---|
| satul are hrana sub prag | fermierul cere provizii |
| drumul de nord are multe atacuri | garda ofera quest de curatare |
| este noapte | apar questuri de aparare |
| este toamna | se activeaza festivalul recoltei |
| ploua mult timp | fermierul cere verificarea campului |
| NPC important lipseste | apare quest de cautare |
| reputatia este mica | apar questuri de incredere |
| story state indica pericol | apare quest de investigatie |

Flux viitor:

```text
World state / simulation / weather / resources
-> EnvironmentEngine
-> EnvironmentSignal
-> QuestDirector
-> QuestRecommendation sau QuestDraft
-> audit / validation
-> QuestEngine
```

## EnvironmentSignal

`EnvironmentEngine` nu trebuie sa creeze questuri active direct. El produce semnale.

Model:

```text
EnvironmentSignal
- signal_id
- signal_type
- world_id
- region_id
- settlement_id
- place_id
- severity
- key
- value
- threshold
- created_at
- expires_at
- cooldown_key
- payload
```

Exemplu:

```json
{
  "signal_type": "settlement_resource_low",
  "world_id": "overworld",
  "settlement_id": "satul_central",
  "severity": "moderate",
  "key": "food",
  "value": 18,
  "threshold": 20,
  "expires_at": 1760000000000
}
```

Reguli:

- semnalul nu modifica progresul;
- semnalul nu acorda reward;
- semnalul are cooldown;
- semnalul are severitate;
- semnalul poate expira;
- semnalul este vizibil in debug;
- `QuestDirector` decide ce face cu semnalul.

## Conditii de quest bazate pe mediu

Chiar fara `EnvironmentEngine`, questurile pot avea conditii read-only bazate pe environment context.

Exemplu:

```yml
conditions:
  can_start:
    - type: "environment:time_of_day"
      value: "night"
    - type: "environment:weather"
      value: "clear"
    - type: "story_state"
      scope: "region"
      target: "current_region"
      key: "road_danger"
      value: "active"
```

Reguli:

- conditiile se evalueaza prin `QuestRuleEngine`;
- conditiile sunt read-only;
- conditiile trebuie sa poata explica de ce nu trec;
- conditiile instabile, cum ar fi vremea, trebuie folosite cu grija;
- questurile importante trebuie sa aiba fallback daca depind de timp sau vreme.

## Legatura cu questurile

Questurile folosesc environment context pentru:

- `visit_region`;
- `visit_place`;
- `inspect_node`;
- `kill_mob_in_region`;
- `bring_item_to_place`;
- conditii de timp;
- conditii de world event;
- story context pentru dialog.

Exemplu:

```text
PlayerMoveEvent
-> EnvironmentContextService.snapshotForPlayer(player)
-> QuestEngine verifica obiective active de tip visit_place
-> objective_progress visit_forge = 1
```

Reguli:

- `QuestEngine` decide progresul, nu `EnvironmentContextService`;
- contextul doar raspunde unde si in ce stare este lumea;
- mapping-ul semantic ramane sursa pentru region/place/node;
- contextul poate fi cache-uit ca sa evite cost pe `PlayerMoveEvent`.

## Legatura cu StoryStateService

`EnvironmentContextService` poate include story state, dar nu il scrie.

Flux corect:

```text
QuestEngine
-> record_story_event / set_story_state
-> StoryStateService
-> EnvironmentContextService poate citi snapshot-ul relevant
-> StoryContextService il include in prompt/debug
```

Reguli:

- story state este scris doar prin actiuni validate;
- environment context nu rescrie lore;
- AI-ul primeste context read-only;
- conflictele de story state sunt detectate prin audit/story QA.

## Legatura cu simularea satului

Simularea satului poate produce stari precum:

- hrana scazuta;
- economie dezechilibrata;
- drum periculos;
- populatie redusa;
- NPC-uri indisponibile;
- cladiri avariate.

In faza curenta, aceste stari pot fi documentate si inspectate, dar nu trebuie sa genereze automat questuri.

In faza viitoare:

```text
SettlementSimulation
-> EnvironmentEngine
-> EnvironmentSignal
-> QuestDirector
-> quest sistemic din template validat
```

Reguli:

- simularea produce semnale, nu questuri active direct;
- semnalele au cooldown;
- questurile sistemice folosesc template-uri validate;
- campaniile principale au prioritate peste sistemice;
- adminul poate dezactiva sistemicele fara sa opreasca simularea.

## Persistenta viitoare

Pentru faza curenta, nu este obligatorie tabela dedicata.

Pentru `EnvironmentEngine` viitor, poate fi util:

```text
environment_signals
- signal_id
- signal_type
- world_id
- region_id
- settlement_id
- place_id
- severity
- key
- value
- threshold
- payload
- created_at
- expires_at
- consumed_at
- cooldown_key
```

Indexuri recomandate:

```text
world_id
region_id
settlement_id
signal_type
expires_at
cooldown_key
```

Reguli:

- semnalele expirate se pot curata;
- semnalele consumate raman in istoric scurt pentru debug;
- semnalele importante pot fi regenerate determinist;
- semnalele nu sunt sursa de adevar pentru quest progress.

## Comenzi admin recomandate

Pentru faza curenta:

```text
/ainpc environment context <player>
/ainpc environment place <player>
/ainpc environment region <player>
/ainpc environment story <player>
```

Pentru faza viitoare:

```text
/ainpc environment signals
/ainpc environment signals region <region_id>
/ainpc environment signal inspect <signal_id>
/ainpc environment signal clear <signal_id>
/ainpc environment audit
```

Reguli:

- comenzile initiale sunt read-only;
- clear/repair cere permisiune admin explicita;
- auditul nu modifica starea;
- debug-ul trebuie sa arate sursa datelor: mapping, story, simulation sau world.

## Audit

Auditul pentru environment trebuie sa verifice:

- referinte la region/place/node inexistente;
- conditii `environment:*` necunoscute;
- world events referite dar inexistente;
- semnale expirate necuratate;
- questuri importante dependente de conditii instabile fara fallback;
- cooldown lipsa pentru semnale sistemice;
- engine activ fara `QuestDirector` sau template-uri validate.

Severitati:

| Problema | Severitate |
|---|---|
| condition handler lipsa | `ERROR` |
| region/place inexistent pentru quest activ | `ERROR` |
| vreme folosita ca blocaj pentru quest principal | `WARNING` |
| semnal expirat | `INFO` sau `WARNING` |
| systemic template fara cooldown | `ERROR` |

## Performanta

Environment context poate deveni scump daca este calculat prea des.

Reguli:

- nu scana toate regiunile la fiecare tick;
- filtreaza `PlayerMoveEvent` pe schimbare de block/chunk/region;
- cache-uieste snapshot-ul pe termen scurt;
- foloseste indexuri pentru region/place/node;
- calculeaza story snapshot doar cand este cerut;
- auditul complet nu ruleaza pe main tick.

Bugete:

| Operatie | Regula |
|---|---|
| player location snapshot | rapid si cache-uit |
| nearby node lookup | raza limitata |
| story snapshot | lazy |
| world event lookup | indexat dupa world/region |
| environment signals | scheduler controlat |

## Ordine recomandata de implementare

1. `QuestEngine`;
2. objective handlers;
3. `StoryStateService`;
4. hook-uri intermediare;
5. `EnvironmentContextService` read-only;
6. conditii simple: time, weather, region, place, story state;
7. comenzi read-only de debug;
8. audit pentru conditii environment;
9. `QuestDirector`;
10. `EnvironmentSignal`;
11. `EnvironmentEngine`;
12. questuri sistemice din template-uri validate.

## Ce trebuie evitat

- `EnvironmentEngine` mare inainte de `QuestEngine`;
- questuri generate direct din weather/time fara audit;
- AI care modifica environment state;
- questuri importante blocate de vreme fara fallback;
- scanari grele pe main thread;
- semnale fara cooldown;
- questuri sistemice fara template validat;
- dublarea logicii de mapping in environment;
- environment context care scrie story state.

## Criterii de acceptare pentru faza curenta

Faza curenta este suficienta cand:

- questurile pot afla regiunea/place-ul curent;
- `visit_region`, `visit_place` si `inspect_node` folosesc mapping semantic;
- story context poate include locul si starea lui;
- conditiile simple pot fi evaluate read-only;
- exista comenzi admin read-only pentru context;
- nu exista generatie automata de questuri din mediu.

## Criterii de acceptare pentru EnvironmentEngine viitor

`EnvironmentEngine` este gata cand:

- produce `EnvironmentSignal`, nu questuri active direct;
- semnalele au cooldown si expirare;
- semnalele sunt vizibile in debug;
- `QuestDirector` decide ce face cu semnalele;
- questurile generate folosesc template-uri validate;
- auditul poate explica ce semnal a activat recomandarea;
- sistemul nu scaneaza lumea excesiv;
- restartul pastreaza semnalele importante sau le reface determinist.

## Concluzie

Pentru etapa curenta, documentul canonic este:

```text
EnvironmentContextService read-only acum.
EnvironmentEngine complet mai tarziu.
```

Aceasta separare pastreaza questurile implementabile acum si lasa loc pentru questuri sistemice, world events si simulare de sat in fazele viitoare.
