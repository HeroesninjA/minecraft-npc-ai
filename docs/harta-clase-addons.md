# Harta claselor pentru addons

Actualizat: 2026-06-25

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `addons`: registry-ul de addonuri care gestioneaza ciclul de viata al descriptorilor si instantele de addon.

## Noduri principale

- `AddonRegistry` -> registry thread-safe care gestioneaza `AddonDescriptor` si `AINPCAddon`: register, unregister, configure, validate; implementeaza `AddonRegistryApi`
- `AddonDescriptor` (in `ainpc-api`) -> metadata addon (id, nume, versiune, tip, capabilitati)
- `AddonType` (in `ainpc-api`) -> enum: CORE, FEATURE, SCENARIO, STORY, RESOURCE, TEXTURE, DATAPACK
- `AINPCAddon` (in `ainpc-api`) -> interfata pentru instantele de addon

## Flux principal

`AddonRegistry.register(addon, descriptor)` -> valideaza descriptor (runtime compatibility, capabilities, dependencies) -> inregistreaza in index -> configureaza -> `onEnable()`

`AddonRegistry.unregister(addon)` -> `onDisable()` -> elimina din index

`AddonRegistry.getDescriptors(type)` -> lookup dupa tip

## Relatii utile

- `AddonRegistry` este singleton per plugin; accesat prin `AINPCPlatform.addonRegistry`
- Valideaza dependintele intre addonuri si compatibilitatea cu `RuntimeMode`
- Respecta listele de `addons.disabled` si `addons.load_order` din config
- Suporta `strict_validation` pentru rejectie la metadata invalida

## Cum se citeste

1. Incepe cu `AddonRegistry` (singura clasa din pachet)
2. Consulta API-ul (`AddonRegistryApi`, `AddonDescriptor`, `AddonType`, `AINPCAddon`) pentru contractul public
