# Documentatie API

Status: canonical in `docs v2`.
Actualizat: 2026-06-21.

Acesta este rezumatul suprafetei publice a API-ului AINPC.

## Regula principala

- addonurile trebuie sa consume doar clasele din `ainpc-api`;
- dependintele directe catre `ainpc-core-plugin` sunt semn de design gresit;
- API-ul public trebuie sa ramana mic, stabil si documentat.

## Ce expune API-ul

- platforma prin `AINPCPlatformApi`;
- registrul de addonuri prin `AddonRegistryApi`;
- world mapping semantic prin `WorldAdminApi`;
- contracte si tipuri read-only pentru addonuri;
- acces la directoarele de date si pack-uri.

## Reguli de consum

- addonurile citesc contracte, nu internals;
- write path-urile trec prin servicii validate;
- compatibilitatea Java-friendly ramane implicită;
- breaking changes cer documentare si verificare.

## Legaturi

- `canonical/constitutie-proiect.md`
- `architecture/harta-clase-platform-db.md`
- `architecture/harta-clase-world.md`
- `reference/harta-clase-index.md`
