# Feature Pack si Scenario Pack Schema

Status: referinta verificata dupa loaderul curent.
Actualizat: 2026-07-17.

Termenul general este `feature pack`. Un pack devine scenario pack numai daca are definitii valide in sectiunea `scenarios`.

## Locatie si incarcare

- core-ul scaneaza recursiv `plugins/AINPC/packs/`;
- sunt selectate extensiile `.yml`, `.yaml` si `.json`;
- toate cele trei extensii trec prin `ScriptConfigurationLoader`, atat la pre-scanarea dependintelor, cat si la incarcarea continutului;
- fixture-ul JSON de contract este validat pana in modelele runtime de trait, dialogue si scenario;
- pack-urile gestionate de addonul medieval sunt instalate sub `packs/addons/ainpc-scenario-medieval/`;
- `feature_packs.allow_addon_packs: false` exclude subarborele `packs/addons/`;
- la reload, descriptorii vechi cu originea `feature-pack` sunt eliminati fara cascada, pack-urile sunt reconstruite, apoi registrul strict reconciliaza dependintele pe graful final.

## Schelet minim

```yaml
id: example_pack
name: "Example Pack"
description: "Continut declarativ exemplu"
version: 1
minecraft_version: "1.21"

addon:
  type: "scenario"
  version: "1.0.0"
  primary_scenario: false
  runtime_modes: ["standalone", "hybrid", "advanced"]
  capabilities: ["scenarios"]
  dependencies: []

scenarios:
  EXAMPLE:
    name: "Scenariu exemplu"
    base_type: "QUEST"
    phases:
      INTRODUCTION: "Inceput"
      COMPLETION: "Final"
```

## Metadata efectiva

- `id` foloseste numele fisierului ca fallback;
- `name` foloseste ID-ul ca fallback;
- `description` este optional;
- top-level `version` este versiunea schemei pack si are implicit valoarea `1`;
- `addon.version` este versiunea descriptorului, implicit `1.0.0`;
- `minecraft_version` poate respinge pack-ul daca versiunea majora nu coincide cu serverul;
- sectiunea `addon` este optionala; tipul si capabilitatile pot fi inferate din continut.

Loaderul citeste si raporteaza consecvent cheia top-level `version`. Cheia `schema_version` nu face parte din contractul feature-pack.

## Sectiuni acceptate

- `traits`;
- `professions`;
- `topologies`;
- `dialogues`;
- `mechanics`;
- `scenarios`;
- `story_defaults`.

Schema detaliata pentru obiective, quest metadata, roluri, faze, conditii, triggere si actiuni este validata de `FeaturePackYamlSupport` si de validatorii de scenariu. Catalogul obiectivelor ramane in `reference/objective-types-reference.md`.

## Reguli pentru descriptor

- valorile `addon.type` sunt mapate prin `AddonType`;
- `primary_scenario` conteaza numai pentru tipul `scenario` cu definitii reale de scenariu;
- un pack declarat `scenario` fara scenarii este retrogradat la `feature`;
- capabilitatile `scenarios` si `progression` sunt corectate dupa continutul incarcat;
- dependintele sunt ID-uri de feature pack, nu nume de plugin Paper;
- o dependinta lipsa respinge pack-ul cand validarea metadata este activa;
- `addons.disabled` poate dezactiva un pack dupa ID.

## Validare si fallback

- validarea metadata ruleaza numai daca ambele flaguri `validate_on_startup` si `validate_addon_metadata` sunt active;
- `fail_invalid_pack: false` sare pack-ul invalid si continua;
- `fail_invalid_pack: true` transforma invaliditatea in eroare de incarcare;
- core-ul nu mai instaleaza pack-uri tematice implicite;
- daca nu se incarca niciun pack, fallback-urile minimale din cod pot ramane active prin `allow_builtin_fallbacks`.

## Legaturi

- `reference/addon-developer-guide.md`
- `reference/addon-config-template.md`
- `reference/objective-types-reference.md`
- `reference/json-yaml-contract.md`
- `planning/stabilizare-api-si-addonuri.md`
