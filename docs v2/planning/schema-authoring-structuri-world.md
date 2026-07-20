# Propunere: schema de authoring pentru structuri world

Status: propunere compatibila, neimplementata.
Actualizat: 2026-07-15.

Proiectul poate beneficia in viitor de o schema versionata pentru sate, orase si structuri exterioare. Schema nu este abandonata, dar forma veche nu trebuie prezentata drept contract runtime.

## Baseline curent

- mapping-ul activ foloseste `Region`, `Place` si `Node`;
- `settlements.yml` incarca doar `SettlementDefinition` si este consumat de comanda de listare;
- template-urile `house_small`, `forge` si `farm` sunt hardcodate;
- blueprint-urile exterioare sunt hardcodate intr-un catalog separat;
- nu exista un parser comun pentru structuri fizice, populatie si quest anchors.

## Scop propus

O schema viitoare ar putea descrie, fara a le amesteca:

- identitatea, versiunea si tipul structurii;
- proiectia in `Region`, `Place` si `Node`;
- referinta optionala la un template fizic si capabilitatile necesare;
- tags, aliases si metadata validate;
- referinte optionale pentru populatie, factiune, risc sau quest anchors;
- reguli de validare si politica de aplicare.

Numele exacte ale campurilor raman decizie de design. Listele istorice precum `category`, `parentRegion`, `spawnPolicy` sau `buildMode` sunt candidati, nu API stabil.

## Reguli de compatibilitate

- schema trebuie sa compileze spre modelele runtime existente, nu sa le dubleze;
- extensiile de scenariu raman separate de core-ul generic;
- authoring-ul nu acorda quest progress, loot sau spawn fara servicii deterministe;
- referintele invalide trebuie respinse inainte de orice mutatie;
- formatul cere `schemaVersion`, migration si teste de round-trip;
- YAML sau JSON sunt forme de transport, nu surse de adevar diferite.

## Decizii deschise

- reutilizarea DTO-urilor `SettlementPlan` versus un contract nou;
- relatia cu `scenario-pack-schema`;
- ownership-ul template-urilor fizice si al blueprint-urilor semantice;
- granularitatea dintre fisier de structura, settlement si scenariu;
- politica de override pentru addon-uri.

## Gate-uri pentru canonizare

- DTO si schema publicate;
- parser plus validator;
- consumer de productie;
- exemple testate si migration policy;
- document separat pentru comenzile de apply/preview.

## Legaturi

- `architecture/mapping.md`
- `reference/template-cladiri-si-marker-nodes.md`
- `architecture/structuri-exterioare-satului.md`
- `architecture/settlement-plan.md`
- `reference/scenario-pack-schema.md`

