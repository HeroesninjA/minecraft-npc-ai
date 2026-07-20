# Strategie Plugin Modular si Scenarii Programabile

Status: directie arhitecturala activa, implementata partial.
Actualizat: 2026-07-15.

Strategia pastreaza core-ul universal si separa extensiile de cod de continutul declarativ.

## Baseline implementat

- patru module Gradle separa API-ul, core-ul Paper, sidecar-ul MCP si addonul medieval;
- `AINPCAddon` si `AddonRegistryApi` ofera lifecycle pentru extensii de cod;
- `ainpc-scenario-medieval` este plugin Paper separat si instaleaza pack-uri gestionate;
- `FeaturePackLoader` incarca continut YAML si il proiecteaza in descriptori de addon;
- scenariile din pack-uri pot completa sau inlocui template-urile de baza;
- integrari externe pot fi inregistrate prin `IntegrationRegistryApi`.

## Limite de responsabilitate

- `ainpc-api`: contracte JVM publice, fara implementari core;
- `ainpc-core-plugin`: runtime, persistenta, validare si executori deterministi;
- addon Paper: lifecycle, configuratie proprie si instalarea resurselor detinute;
- feature pack: date declarative pentru traits, profesii, dialog, mecanici si scenarii;
- MCP sidecar: proces separat, nu addon Paper si nu extensie in-process.

## Regula de extensie

- continutul uzual ramane declarativ;
- comportamentul nou foloseste contracte publice si registri expliciti;
- accesul direct la internals core nu este mecanism de extensie;
- executia sensibila ramane determinista si validata de core;
- dezactivarea unui addon nu trebuie sa corupa datele sau sa stearga resurse straine.

## Ce nu este implementat inca

- descoperire proprie de JAR-uri de catre AINPC; pluginurile sunt incarcate de Paper;
- enforcement complet pentru dependintele descriptorilor de cod;
- ordine topologica aplicata lifecycle-ului pluginurilor;
- arbitraj strict pentru mai multe scenarii primare;
- scripting arbitrar sau DSL executabil; schema curenta foloseste tipuri de conditii, triggere si actiuni inregistrate;
- hot-unload generic pentru cod in afara lifecycle-ului Paper;
- compatibilitate Java completa si politica formala de versionare API.

Aceste directii raman propuneri compatibile si active. Ele nu sunt arhivate doar pentru ca nu sunt implementate.

## Legaturi

- `architecture/refactorizare-si-impartire-pe-module.md`
- `architecture/harta-clase-addons.md`
- `reference/scenario-pack-schema.md`
- `planning/stabilizare-api-si-addonuri.md`
