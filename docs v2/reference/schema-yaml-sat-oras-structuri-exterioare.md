# Schema YAML pentru sat, oras si structuri exterioare

Acesta este exemplul YAML concret pentru tipurile de baza ale lumii.

## Scop

- completeaza schema structural-semantic cu exemple authoring-friendly;
- acopera `village`, `city`, `outpost`, `dungeon` si `quest_structure`;
- pastreaza formatul validabil si extensibil.

## Reguli comune

- fiecare structura are `id`, `type`, `category`, `displayName`, `parentRegion`, `tags`, `entryNodes`, `interactionNodes`, `questAnchors` si `requiresValidation`;
- optional pot aparea `parentPlace`, `npcRoles`, `spawnPolicy`, `dangerLevel`, `faction`, `buildMode` si `templateId`;
- tipurile noi folosesc aceeasi schema atata timp cat sunt inregistrate semantic.

## Forma

- defineste un bloc comun YAML pentru authoring;
- foloseste alias-uri si taxonomie extinsa, nu campuri ad-hoc;
- trateaza exemplele ca model pentru scenarii, nu ca lista inchisa.
