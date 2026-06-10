# Feature Flags Lifecycle

Importanta: ridicata.

Acest document defineste regula de lifecycle pentru caracteristicile majore AINPC. Constitutia cere ca toate caracteristicile sa poata fi dezactivate si ca starea finala a feature-urilor sa fie rezolvata central. Dezactivarea trebuie sa fie clara pe patru niveluri: rezolvare, comenzi, runtime si initializare servicii.

## Regula Generala

Fiecare feature major trebuie sa aiba:

- flag in `features.*`;
- intrare in `RuntimeFeatureState`;
- mesaj clar cand o comanda este blocata;
- listener/task runtime care nu executa logica feature-ului cand flag-ul este false;
- serviciu in mod `disabled` sau `read-only`;
- initializare conditionala doar dupa ce exista fallback/no-op sigur.

Config-ul brut nu este sursa finala de adevar pentru runtime. Cheile din `features.*`, profilul serverului, `demo.enabled`, addonurile active si pack-urile incarcate sunt input-uri pentru resolver. Serviciile trebuie mutate gradual spre starea finala rezolvata, nu spre citiri directe din `plugin.config`.

## Niveluri de Dezactivare

### Nivel 0: Feature Resolution

Resolverul calculeaza starea finala a fiecarui feature major inainte ca serviciile sa execute logica runtime.

Surse de intrare:

- `features.*` din configul core;
- profilul serverului;
- `demo.enabled` si setarile demo/fallback;
- `addons.enabled`, `addons.disabled`, `addons.load_order`;
- manifestele addonurilor;
- metadata pack-urilor: capabilities, dependencies si, ulterior, provides/requires/conflicts/fallback.

Rezultatul minim este un `RuntimeFeatureState` inspectabil cu stari precum:

- `enabled`;
- `disabled`;
- `optional`;
- `blocked`;
- `fallback`;
- `experimental`.

Acest nivel trebuie sa fie initial read-only si auditabil. Nu se muta toate serviciile pe resolver dintr-o singura schimbare mare.

### Nivel 1: Command Gate

Comanda publica verifica starea rezolvata si raspunde clar utilizatorului. Pana la migrare completa, poate verifica `features.*`, dar mesajul trebuie sa ramana compatibil cu viitorul `RuntimeFeatureState`.

Exemple:

- `features.quest`
- `features.progression`
- `features.gui`
- `features.story`
- `features.mapping`
- `features.routine`
- `features.generation`

Acest nivel exista initial pentru comenzile majore.

### Nivel 2: Runtime Gate

Listenerele, scheduler-ele si taskurile async verifica starea rezolvata inainte sa execute logica feature-ului.

Acest nivel este necesar pentru cazuri in care feature-ul poate fi declansat fara comanda directa, de exemplu interactiuni NPC, chat, inventory click sau tick scheduler.

### Nivel 3: Service Lifecycle

Serviciul nu trebuie sa porneasca procese costisitoare sau sa produca efecte cand feature-ul este false.

Exista doua variante acceptate:

1. `NoOpService`: acelasi API, dar metodele returneaza rezultate neutre.
2. `DisabledMode`: serviciul exista, dar raporteaza clar ca feature-ul este dezactivat si nu executa efecte.

Nu este acceptabil sa lasi un `lateinit` neinitializat daca exista cod Java/Kotlin care il poate accesa prin getter. Intai se introduce no-op/read-only, apoi se poate conditiona initializarea reala.

## Stare Curenta

- Feature resolution: modelul read-only `RuntimeFeatureState` exista initial; resolverul central lipseste inca, iar runtime-ul foloseste citiri directe din config si validatoare separate.
- Command gate: existent initial pentru comenzile majore prin `features.*`.
- Runtime gate: existent initial pentru quest listeners, GUI listener, mapping wand si scheduler routine/simulation/quest tracking prin `features.*`.
- Service lifecycle: partial. Serviciile principale sunt inca initializate pentru compatibilitate API, chiar daca feature-ul este dezactivat.

## Prioritati

1. `FeatureResolver`: combina configul core, profilul serverului, demo flags, addon registry si metadata pack-urilor.
2. Audit/diagnostic: comanda sau raport care explica pentru fiecare feature de ce este `enabled`, `disabled`, `blocked`, `fallback` sau `experimental`.
3. Migrare treptata a call site-urilor publice de la citiri directe `plugin.config` la starea rezolvata.
4. `features.ai`: oprire diagnostice startup/reload si mod local/no-op cand AI este off.
5. `features.gui`: `GuiService` disabled mode, cu sesiuni inchise si fara inventory custom.
6. `features.mapping`: `MappingWandService` disabled mode si world admin read-only daca mapping este off.
7. `features.quest`: `ScenarioEngine` disabled mode pentru quest runtime, pastrand template loading pentru audit.
8. `features.progression`: `ProgressionService` disabled mode, cu rezultate neutre pentru GUI/commands.
9. `features.story`: `StoryStateService` si `StoryContextService` read-only sau disabled.
10. `features.generation`: orice creare automata, demo create, spawn NPC sau executie de structuri trebuie sa fie blocata cand flag-ul este false; analiza, planificarea si validarea pot ramane read-only.

## Implementare Incrementala Recomandata

### FF0: Model read-only

Status: facut initial.

Livrabile:

- enum pentru stari feature;
- model pentru motiv si surse;
- lista initiala de feature-uri majore;
- teste unitare fara Bukkit.

### FF1: Resolver minim

Status: facut initial pentru model si snapshot read-only; audit/debugdump ramas.

Livrabile:

- citeste `features.*`, `demo.enabled`, `addons.enabled`, `addons.disabled`;
- produce `RuntimeFeatureState`;
- nu schimba inca behavior-ul serviciilor;
- expune rezultatul prin audit/debugdump.

### FF2: Metadata addon/pack

Livrabile:

- campuri optionale `provides`, `requires`, `conflicts`, `fallback`;
- validare de schema;
- conflictele nerezolvate marcheaza feature-ul sau addonul ca `blocked`.

### FF3: Migrare call sites

Livrabile:

- `ensureFeatureEnabled` foloseste starea rezolvata;
- schedulerul foloseste starea rezolvata;
- listenerele majore folosesc starea rezolvata;
- citirile directe `plugin.config.getBoolean("features.*")` raman doar in resolver sau in compat wrappers.

## Criteriu de Conformitate

Un feature este conform doar cand:

- are stare finala in `RuntimeFeatureState`;
- poate explica sursele care au dus la acea stare;
- poate fi dezactivat din config fara crash;
- nu ruleaza taskuri sau efecte secundare cand este dezactivat;
- comenzile si GUI-urile raporteaza clar dezactivarea;
- reload-ul aplica aceeasi regula ca startup-ul;
- exista test pentru cel putin o cale publica dezactivata.
