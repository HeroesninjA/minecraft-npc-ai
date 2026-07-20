# Creator Hub: quest si mapping

Status: ghid operational.
Verificat in cod: 2026-07-16.

Creator Hub reuneste formulare si comenzi existente. Nu publica automat questuri si nu persista automat mapping-ul.

## Acces

- `/ainpc gui creator` deschide `CreatorHubGui`;
- hub-ul cere intern `ainpc.admin` sau `ainpc.creator`;
- ecranele individuale au si verificari per `GuiKey` in `GuiService.canOpen`;
- `features.gui` trebuie sa fie activ, iar actiunile quest/world depind de feature-urile lor.

## Quest rapid

1. deschide `Quick Quest Wizard`;
2. completeaza giver-ul, obiectivele, stage-urile si recompensa;
3. foloseste preview-ul pentru a verifica sumarul;
4. exportul rapid afiseaza YAML simplificat in chat;
5. muta manual continutul intr-un pack valid si reincarca-l prin fluxul operational aprobat.

Quick Quest nu publica pack-ul si nu garanteaza ca toate campurile formularului avansat sunt reprezentate.

## Quest avansat

1. deschide `Quest Creator`;
2. campurile text inchid inventarul si consuma urmatorul mesaj din chat;
3. verifica ID-ul, mecanica, obiectivele, recompensele, dialogul si story events;
4. foloseste preview-ul inainte de export;
5. exporta draftul JSON in `debug-dumps/quest-drafts`;
6. revizuieste si transforma draftul intr-un pack incarcat explicit.

`Quest Authoring` este snapshot read-only. `Quest Definitions`, `Quest Editor` si `Test Quest` lucreaza cu definitii deja incarcate; nu transforma singure un draft exportat in continut live.

## Actiuni numite AI

- `/ainpc quest create ai` precompleteaza local un draft determinist in fluxul curent;
- `/ainpc quest create ai from selection` porneste de la selectia curenta;
- eticheta AI nu demonstreaza un apel extern de model;
- rezultatul ramane draft pana la validare si incarcare explicita.

## Quest Map

- selecteaza definitia si obiectivul;
- inspecteaza binding-urile existente;
- mapeaza numai targeturi valide din world mapping;
- stergerea unei ancore foloseste confirmare;
- binding-urile persistate apartin serviciului de progresie, nu draftului quest creator.

Vezi `reference/quest-anchor-bindings.md` pentru namespace, fallback si limite.

## Mapping creator

1. deschide `Creator Mapping`;
2. alege region, place sau node;
3. completeaza campurile si coordonatele;
4. inspecteaza preview-ul;
5. confirma pentru a modifica mapping-ul runtime;
6. ruleaza `/ainpc world save` pentru persistenta in configuratia world.

Confirmarea unui wizard si salvarea world sunt doua operatii distincte. Inchiderea inventarului nu persista automat.

## Draft si input

- valorile formularului sunt tinute in memorie per player;
- un singur request text poate fi activ per player;
- `clear`, `cancel` si `anuleaza` curata campul, nu pastreaza valoarea anterioara;
- quit-ul curata drafturile si selectiile tranzitorii;
- mesajul de salvare a campului nu inseamna export, persistenta sau publish.

## Checklist

- rol si permisiuni verificate;
- feature flags active;
- distinctie clara intre preview, draft, export, persist si publish;
- ID-uri stabile si fara conflict;
- artefact exportat inspectat manual;
- mapping salvat explicit dupa confirmare;
- quest pack incarcat si testat separat;
- audit/debugdump revizuit dupa mutatii.

## Legaturi

- `guides/gui-admin-mapping-quest.md`
- `guides/quest-authoring-tutorial.md`
- `guides/mapping-harti-manuale.md`
- `reference/gui-stack.md`
- `reference/quest-anchor-bindings.md`
- `planning/questuri-avansate-v2.md`
