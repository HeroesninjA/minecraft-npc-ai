# Contract config core si addonuri

Scopul este ca `ainpc-core-plugin` sa ramana universal. Core-ul poate incarca, valida si coordona addonuri, dar nu trebuie sa contina balancing sau continut specific unui scenariu.

## Config core

Fisier runtime:

```text
plugins/AINPC/config.yml
```

Contine setari universale:

- AI, diagnostic si timeout-uri;
- platform mode si world mode;
- registru addonuri;
- reguli generale pentru feature packs;
- dialog, rutina, simulare si audit generic;
- politici globale de validare.

Exemplu:

```yml
addons:
  enabled: true
  config_directory: "addons"
  strict_validation: true
  disabled: []
  load_order: []

feature_packs:
  validate_on_startup: true
  fail_invalid_pack: false
  allow_addon_packs: true
  validate_addon_metadata: true
```

Core-ul nu trebuie sa primeasca chei precum `medieval_rewards`, `blacksmith_contracts` sau `ritual_balance`. Acestea apartin addonului.

Cand `feature_packs.validate_on_startup` si `feature_packs.validate_addon_metadata` sunt active, core-ul face preflight peste sectiunea `addon:` declarata in pack-uri. Sunt respinse pack-urile cu `addon.type` necunoscut, liste invalide pentru `runtime_modes`/`capabilities`/`dependencies`, runtime incompatibil sau dependinte lipsa. Dependintele sunt verificate dupa un prepass peste toate pack-urile candidate, deci ordinea fisierelor nu produce respingeri false. Daca `feature_packs.fail_invalid_pack` este `false`, pack-ul invalid este sarit si serverul continua; daca este `true`, incarcarea arunca eroare.

## Config addon

Fisier runtime recomandat:

```text
plugins/AINPC/addons/<addon-id>/config.yml
```

Folderul de baza este controlat de `addons.config_directory` din config-ul core. Addonurile trebuie sa ceara calea prin `AINPCPlatformApi.getAddonConfigDirectory(addonId)`, nu sa hardcodeze `plugins/AINPC/addons`.

Fiecare addon livreaza in JAR un template:

```text
src/main/resources/config-template.yml
```

La prima pornire, addonul copiaza template-ul in folderul sau runtime. Dupa aceea nu suprascrie `config.yml`, ca modificarile adminului sa ramana intacte.

Addonul poate suprascrie mereu `config-template.yml` din folderul runtime pentru referinta, dar `config.yml` se creeaza doar daca lipseste.

Chei recomandate pentru orice addon:

```yml
addon:
  id: "addon-id"
  config_version: 1
  enabled: true

content:
  install_pack: true
  playable_content: true
```

`addon.enabled: false` opreste addonul din propria configuratie. Pentru dezactivare globala, adminul foloseste `addons.disabled` in config-ul core.

`content.install_pack: false` sau `content.playable_content: false` pastreaza addonul inregistrat, dar il lasa sa curete/evite instalarea pack-ului sau jucabil.

## Pack-uri livrate de addon

Pack-urile de continut raman sub:

```text
plugins/AINPC/packs/addons/<addon-id>/
```

Configuratia addonului ramane sub:

```text
plugins/AINPC/addons/<addon-id>/
```

Separarea previne stergerea configuratiei cand addonul isi curata pack-ul instalat.

## Exemplu addon medieval

Addonul `ainpc-scenario-medieval` livreaza:

```text
ainpc-scenario-medieval/src/main/resources/config-template.yml
ainpc-scenario-medieval/src/main/resources/packs/medieval_quest.yml
```

La runtime:

```text
plugins/AINPC/addons/ainpc-scenario-medieval/config.yml
plugins/AINPC/addons/ainpc-scenario-medieval/config-template.yml
plugins/AINPC/packs/addons/ainpc-scenario-medieval/medieval_quest.yml
```

Regula de baza: descriptorul addonului declara identitatea si capabilitatile, config-ul addonului controleaza optiunile sale, iar pack-ul declara continutul jucabil.

Implementarea curenta:

- core-ul expune `AINPCPlatformApi.getAddonConfigDirectory(addonId)`;
- addonul medieval copiaza mereu `config-template.yml` ca referinta;
- addonul medieval creeaza `config.yml` doar daca lipseste;
- `addon.enabled: false` dezactiveaza addonul medieval local;
- `content.install_pack: false` sau `content.playable_content: false` nu mai instaleaza `medieval_quest.yml`.
- metadata `addon:` din feature pack-uri este validata la startup/reload cand optiunile core aferente sunt active;
- dependintele declarate in `addon.dependencies` sunt verificate inclusiv tranzitiv.
