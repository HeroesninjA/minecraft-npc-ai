# Addon Config Template

Status: referinta verificata pentru configuratia implementata.
Actualizat: 2026-07-17.

Core-ul stabileste calea comuna, iar fiecare addon isi defineste si valideaza propriul continut de configurare.

## Cai

- config core: `plugins/AINPC/config.yml`;
- director per-addon implicit: `plugins/AINPC/addons/<addon-id>/`;
- calea reala se obtine prin `AINPCPlatformApi.getAddonConfigDirectory(addonId)`;
- radacina per-addon poate fi schimbata prin `addons.config_directory` in config-ul core;
- ID-ul este normalizat la litere mici si caracterele nesigure sunt inlocuite.

## Setari core pentru addonuri

```yaml
addons:
  enabled: true
  config_directory: "addons"
  strict_validation: true
  disabled: []
  load_order: []
```

- `enabled` controleaza registrul;
- `disabled` filtreaza addonuri de cod si feature pack-uri dupa ID; cu validare stricta, dependentii de cod activi sunt opriti tranzitiv;
- `strict_validation` controleaza validarea descriptorilor de cod si gate-ul candidatului pentru dependinte, cicluri si conflictul de scenariu primar;
- `load_order` pastreaza compatibil cheia existenta si ordoneaza numai descriptorii returnati de registru; nu schimba ordinea Paper de activare.

## Implementarea medievala

`ainpc-scenario-medieval` livreaza `src/main/resources/config-template.yml` si, la pornire:

- copiaza sau actualizeaza `config-template.yml` in directorul addonului;
- creeaza `config.yml` numai daca acesta lipseste;
- parseaza strict ambele fisiere si verifica ID-ul addonului, versiunea si tipurile valorilor cunoscute;
- respinge `config.yml` cu `addon.config_version` mai nou decat versiunea suportata;
- in modul `auto`, creeaza `config.yml.v<versiune>.bak`, completeaza numai cheile lipsa, pastreaza valorile si cheile operatorului, apoi scrie atomic;
- in modul `validate_only`, raporteaza versiunea veche si cheile lipsa fara a modifica fisierul;
- citeste `addon.enabled`, `content.install_pack` si `content.playable_content` inainte de instalarea pack-urilor;
- pastreaza configuratia utilizatorului la pornirile urmatoare.

```yaml
addon:
  id: "ainpc-scenario-medieval"
  config_version: 2
  config_migration: "auto" # auto | validate_only
```

Acesta este un model implementat si testat in addon, nu un validator generic oferit automat tuturor addonurilor.

## Validari separate

- core-ul nu valideaza arbitrar schema `config.yml` a fiecarui addon;
- addonul este responsabil pentru valori implicite, tipuri, migrari, backup si mesaje de eroare;
- `addons.strict_validation` valideaza `AddonDescriptor`, nu config-ul specific;
- metadata feature pack este validata separat prin `feature_packs.validate_on_startup` si `feature_packs.validate_addon_metadata`;
- `feature_packs.fail_invalid_pack` decide daca un pack invalid este sarit sau produce eroare de incarcare.

## Reguli

- core-ul ramane generic;
- continutul si balansarea specifice raman in config-ul addonului;
- `config-template.yml` documenteaza schema curenta, iar `config.yml` pastreaza valorile operatorului;
- o migrare automata nu suprascrie valori existente si nu face downgrade unei versiuni viitoare;
- modul fara scriere trebuie sa permita validare si diagnostic inaintea activarii addonului;
- un addon trebuie sa-si poata elimina numai resursele pe care le gestioneaza.

## Legaturi

- `reference/addon-developer-guide.md`
- `reference/scenario-pack-schema.md`
- `reference/feature-flags-lifecycle.md`
