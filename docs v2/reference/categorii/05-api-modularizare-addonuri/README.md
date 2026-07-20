# API, modularizare si addonuri

Status: index tematic verificat.
Actualizat: 2026-07-15.

## Contracte curente

- `reference/documentatie-api.md` - suprafata publica si providerii reali;
- `architecture/harta-clase-addons.md` - lifecycle de cod si flux feature pack;
- `reference/api-events-listeners-triggers.md` - evenimente Bukkit si producatori;
- `reference/scenario-pack-schema.md` - schema citita de loader;
- `reference/addon-config-template.md` - cai si responsabilitatea config-ului.

## Ghiduri

- `reference/addon-developer-guide.md` - integrarea unui plugin Paper addon;
- `reference/kotlin-interop-api-addonuri.md` - ABI Kotlin/Java si limitele curente;
- `reference/kotlin-paper-packaging-si-smoke.md` - packaging si smoke Paper.

## Arhitectura si roadmap

- `architecture/refactorizare-si-impartire-pe-module.md` - modulele Gradle si datoria structurala;
- `architecture/strategie-plugin-modular-si-scenarii-programabile.md` - directia platformei extensibile;
- `planning/stabilizare-api-si-addonuri.md` - golurile publice active.

## Regula

- codul stabileste runtime-ul curent;
- ghidurile deriva din contracte si nu transforma interfete neconectate in functionalitati active;
- dependintele plugin Paper, dependintele descriptorilor si dependintele feature pack sunt mecanisme distincte;
- ideile compatibile neimplementate raman roadmap activ, nu continut abandonat.
