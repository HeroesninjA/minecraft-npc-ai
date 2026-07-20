# Harta claselor pentru addonuri

Status: harta verificata a runtime-ului curent.
Actualizat: 2026-07-17.

Subsistemul are doua cai distincte: addonuri Paper cu cod si feature pack-uri declarative.

## Contracte publice

- `AINPCAddon` - lifecycle si callback-uri optionale;
- `AddonDescriptor` - identitate, tip, runtime modes, capabilitati si dependinte;
- `AddonType` - clasificarea descriptorului;
- `AddonRegistryApi` - register, unregister, interogare si selectie scenariu;
- `AINPCPlatformApi` - accesul addonului la serviciile publice si directoare;
- `ExternalPluginIntegration` si `IntegrationRegistryApi` - adaptoare pentru pluginuri externe, separate de addonuri;
- `DependencyResolver` si `AddonDependencyGraph` - contracte publice rezervate de diagnostic, fara provider prin platforma.

## Implementari core

- `ServiceRegistry` inregistreaza `AINPCPlatformApi` in Bukkit `ServicesManager`;
- `AINPCPlatform` implementeaza fatada publica si profilul runtime;
- `AddonRegistry` aplica gate-ul de dependinte al candidatului, activeaza tranzactional, restaureaza la esec si dezactiveaza addonurile de cod;
- `IntegrationRegistry` gestioneaza atomic lifecycle-ul adaptoarelor externe dupa numele normalizat al pluginului;
- `AddonDependencyResolver` calculeaza pentru `AddonRegistry` dependinte lipsa, cicluri, conflicte si ordine topologica; `AINPCPlatform` nu pastreaza o a doua instanta si nu expune accessori concreti;
- `FeaturePackLoader` incarca fisiere, creeaza descriptori `feature-pack` si reincarca registrul;
- `RuntimeFeatureResolver` foloseste capabilitatile descriptorilor pentru feature snapshot.

## Flux addon de cod

```text
Paper depend -> ServicesManager -> isAddonEnabled
-> registerAddon
-> dependency gate pe graful candidatului
-> onLoad
-> registerDescriptor
-> onEnable
-> la esec: onDisable partial + rollback la inregistrarea anterioara
-> optional pack sync + reloadContent
-> unregisterAddon
-> dependenti activi in ordine inversa
-> onDisable
```

Cu validare stricta, missing dependency, ciclul care include candidatul si conflictul de scenariu primar resping addonul de cod inainte de orice callback si pastreaza inregistrarea anterioara. Erorile declarative fara legatura nu blocheaza candidatul. `registerDescriptor` nu porneste lifecycle si nu respinge problemele globale de graf: pastreaza declaratia, emite warning pentru diagnosticele care o implica si lasa comparatorul registrului sa aleaga determinist scenariul primar. Reinregistrarea aceleiasi instante este idempotenta. `unregisterAddon` dezactiveaza tranzitiv dependentii activi de la frunze spre tinta, continua dupa exceptii si elimina fiecare descriptor. Inlocuirea aceluiasi ID nu cascadeaza, `removeByOrigin` ruleaza lifecycle pentru instantele de cod, iar shutdown-ul ramane best-effort.

Implementarea de referinta este:

- `AINPCScenarioMedievalPlugin` pentru integrarea Paper, config si resurse;
- `MedievalScenarioAddon` pentru descriptorul public.

## Flux feature pack

```text
plugins/AINPC/packs/**/*.{yml,yaml,json}
-> FeaturePackMetadataValidator
-> dependency validation intre pack-uri
-> FeaturePackYamlSupport
-> AddonDescriptor(origin=feature-pack)
-> ScenarioEngine.reloadTemplates
```

Feature pack-ul nu este instanta `AINPCAddon` si nu primeste `onLoad`, `onEnable` sau `onDisable`.
La reload, descriptorii feature pack sunt scosi intr-o fereastra fara cascada, apoi registrul reconciliaza starea finala. Consumatorii de cod raman activi daca dependinta reapare si sunt opriti tranzitiv daca lipseste la final.

## Flux integrare externa

```text
ExternalPluginIntegration
-> onRegister reusit
-> commit in IntegrationRegistry
-> inlocuire: onUnregister vechi + onRegister nou
-> rollback la integrarea veche daca activarea noua esueaza
-> onUnregister la unregister sau shutdown
```

Reinregistrarea aceleiasi instante este idempotenta. O activare esuata este curatata si nu ramane vizibila in registry, iar `clear()` continua eliminarea celorlalte integrari daca un callback normal de dezregistrare arunca exceptie.

## Callback-uri addon

| Callback | Producator core |
| --- | --- |
| `onStoryEvent` | `StoryAuthoringService` |
| `onRelationshipChange` | `RelationshipService` |
| `onDailySalaryPaid` | `NpcEconomyService` |
| `onSeasonChange` | `SeasonalBehaviorService` |
| `onNpcStateChange` | `AINPC.changeState` si calea fortata a simularii |

Dispatcherul foloseste un snapshot al ID-urilor, revalideaza instanta inaintea fiecarui apel si logheaza la `WARNING` callback-ul, addonul si exceptia. Un addon poate fi eliminat in propriul callback fara `ConcurrentModificationException`, iar esecul lui nu opreste livrarea catre ceilalti consumatori inca inregistrati.

Tranzitiile NPC publica UUID-ul si valorile `NPCState.name` dupa schimbarea efectiva. O tranzitie identica sau respinsa de prioritate nu publica, simularea pastreaza schimbarea fortata existenta si publica, iar `NPCManager` restaureaza starea persistata fara callback de runtime.

## Limite curente

- gate-ul se aplica la `registerAddon`, nu la `registerDescriptor`, si poate fi dezactivat explicit prin `addons.strict_validation`;
- cu `addons.strict_validation: false`, dependintele raman complet permisive atat la register, cat si la unregister;
- `addons.load_order` ramane, pentru compatibilitate, numai prioritate de descriptori si fallback, nu ordine de activare Paper;
- descriptorii declarativi pot introduce mai multe scenarii primare; conflictul ramane vizibil in resolver;
- callback-urile addon si evenimentele Bukkit sunt doua mecanisme diferite.

## Legaturi

- `reference/documentatie-api.md`
- `reference/addon-developer-guide.md`
- `reference/scenario-pack-schema.md`
- `reference/api-events-listeners-triggers.md`
- `planning/stabilizare-api-si-addonuri.md`
