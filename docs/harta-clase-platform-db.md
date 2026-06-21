# Harta claselor pentru platform si database

Actualizat: 2026-06-21

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste infrastructura de platforma si persistenta: `AINPCPlatform`, `DatabaseManager`, registrul de addonuri, profilul de runtime si rezolvarea feature-urilor.

Nu este un inventar complet al infrastructurii. Este o harta de lucru pentru nodurile care fac bootstrap, configurare, persistenta si rezolutie de capabilitati.

## Noduri principale

- `AINPCPlatform` -> runtime central pentru addonuri, profile, overlay si reload
- `AINPCPlatformApi` -> contractul public expus addonurilor
- `AddonRegistry` -> registrul addonurilor si descriptorilor
- `AddonRegistryApi` -> contractul public pentru registru
- `DatabaseManager` -> conexiuni, schema si executie SQL
- `DatabaseDialect` si `DatabaseDialectSql` -> adaptare SQL pe dialect
- `PlatformProfile` -> profilul platformei si al executiei
- `RuntimeFeatureResolver` -> rezolva capabilitati si feature flags
- `RuntimeFeatureSnapshot` -> snapshot al feature-urilor active
- `RuntimeFeatureState` / `RuntimeFeatureSource` / `RuntimeFeatureKey` -> modelarea sursei si starii feature-urilor

## Flux principal

`AINPCPlatform` -> `AddonRegistry` -> addonuri si descriptorii lor

`AINPCPlatform` -> `RuntimeFeatureResolver` -> `RuntimeFeatureSnapshot`

`DatabaseManager` -> deschidere, creare schema, executie si inchidere

`AINPCPlatformApi` expune informatia esentiala catre addonuri.

## Relatii utile

- `AINPCPlatform` este stratul care orchestreaza bootstrap-ul si reload-ul.
- `AddonRegistry` pastreaza descriptorii si addonurile active.
- `DatabaseManager` este infrastructura de date pentru persistenta si schema.
- `RuntimeFeatureResolver` si `RuntimeFeatureSnapshot` decid ce capabilitati sunt active.
- `PlatformProfile` este un nod mic, dar util pentru reguli de executie.

## Cum se citeste

1. Incepe cu `AINPCPlatform`.
2. Continua cu `AddonRegistry` si `AINPCPlatformApi`.
3. Treci la `DatabaseManager` pentru persistenta.
4. Urmareste `RuntimeFeatureResolver` si snapshot-urile pentru capabilitati.
5. Foloseste `DatabaseDialect` cand vrei detalii despre SQL-ul adaptat.

## Nota

Daca vrei doar traseul intre module si pachete, foloseste harta de pachete. Daca vrei relatiile dintre clasele de infrastructura, acesta este documentul potrivit.
