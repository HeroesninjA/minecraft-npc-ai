# Limita dintre authoring asistat si generarea AI de questuri

Status: contract canonic de integrare.
Verificat in cod: 2026-07-15.

Numele `quest create ai` descrie astazi un flux local de precompletare. Nu demonstreaza ca un model AI a generat continut si nu publica un quest in runtime.

## Starea implementata

- `/ainpc quest create ai [text si hint-uri]` parseaza determinist textul si precompleteaza `Quick Quest` sau formularul avansat;
- formularul avansat produce un draft JSON prin `QuestDraftExporter`;
- butonul de export scrie draftul in `<data-folder>/debug-dumps/quest-drafts/`;
- `Quest Authoring` si `QuestSeed` expun context, seed, warnings si diagnostic read-only;
- `Quick Quest` poate afisa YAML in chat, iar actiunea `quick-export` afiseaza o varianta simplificata tot in chat;
- niciunul dintre aceste trasee nu scrie automat un pack de scenariu incarcat.

## Ce nu trebuie afirmat

- ca textul liber este trimis automat unui model;
- ca `Exporta` inseamna publicare sau import;
- ca un draft JSON este deja compatibil cu schema completa de pack;
- ca `accept nearest` testeaza draftul neincarcat;
- ca AI-ul poate modifica progresul, mapping-ul sau story state-ul.

## Pipeline tinta, neimplementat cap-coada

1. construieste un snapshot read-only din mapping, story si progresie;
2. cere unui provider explicit configurat un draft conform unei scheme versionate;
3. valideaza schema, tipurile de obiective, ancorele, recompensele si dependentele;
4. prezinta diff-ul si problemele unui admin;
5. scrie numai intr-o zona de staging;
6. publica printr-o actiune explicita cu backup, import/reload si rollback;
7. confirma rezultatul prin audit si test runtime.

Acest pipeline ramane roadmap pana cand fiecare pas are implementare si test. Eticheta `AI` din GUI nu poate inlocui aceste criterii.

## Reguli

- sugestia ramane separata de efectul executabil;
- validarea determinista are prioritate fata de continutul generat;
- orice provider extern trebuie sa fie optional si izolat;
- publicarea necesita aprobare umana si artefact versionat;
- lipsa mapping-ului sau a unei ancore blocheaza publicarea, nu este completata prin coordonate inventate.

## Surse tehnice

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandMisc.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens/QuestCreateGui.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens/QuickQuestGui.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens/QuestAuthoringGui.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/QuestSeedFactory.kt`

## Legaturi

- `guides/quest-authoring-tutorial.md`
- `architecture/story-context-service.md`
- `architecture/story-si-context-ai.md`
- `reference/scenario-pack-schema.md`
- `planning/questuri-avansate-v2.md`
