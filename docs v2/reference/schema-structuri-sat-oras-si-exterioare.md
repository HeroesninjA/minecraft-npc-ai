# Schema pentru structuri de sat, oras si structuri exterioare

Acesta este contractul semantic si structural pentru generarea de structuri reutilizabile.

## Principiu

- fiecare structura trebuie sa existe in plan semantic si in plan spatial;
- daca lipseste unul dintre ele, continutul devine decor fragil sau hardcodat;
- schema trebuie sa fie validabila si conectata la NPC, rutina, quest si story.

## Clase principale

- structuri rezidentiale;
- structuri de profesie;
- structuri de utilitate;
- structuri de timp liber;
- structuri publice;
- structuri exterioare;
- structuri pentru questuri.

## Contract

- fiecare intrare are `id`, `type`, `category`, `displayName`, `parentRegion`, `tags`, `entryNodes`, `interactionNodes`, `questAnchors` si `requiresValidation`;
- extensiile pot adauga `parentPlace`, `npcRoles`, `spawnPolicy`, `dangerLevel`, `faction`, `buildMode` si `templateId`;
- tipurile noi trebuie sa foloseasca registru semantic, nu hardcode.
