# Harta claselor pentru settlement

Actualizat: 2026-06-25

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `settlement`: incarcarea configuratiilor de asezari si registry-ul de template-uri pentru cladiri.

## Noduri principale

- `SettlementConfigLoader` -> incarca si parseaza `settlements.yml` in `SettlementDefinition`
- `BuildingTemplateRegistry` -> registry in-memorie pentru `BuildingTemplateDefinition`; incarca template-uri default (casa mica, fierarie, ferma)

## Flux principal

`SettlementConfigLoader` -> citeste `settlements.yml` -> produce `SettlementDefinition` (world, centru, raza, profil, tema, metadata)

`BuildingTemplateRegistry` -> `loadDefaults()` -> inregistreaza template-uri standard cu definitii de ancore

## Relatii utile

- `SettlementConfigLoader` produce datele de intrare pentru planificarea si spawn-ul de asezari
- `BuildingTemplateRegistry` este folosit de `NpcSpawnOrchestrator` si `HouseAllocationPlanner`
- Ambele sunt straturi de configuratie, nu contin logica de runtime

## Cum se citeste

1. Incepe cu `SettlementConfigLoader` (incarcarea configuratiei)
2. Continua cu `BuildingTemplateRegistry` (template-urile de cladiri)
