# Quest authoring: flux operational

Status: ghid operational.
Verificat in cod: 2026-07-15.
Depinde de: `reference/objective-types-reference.md` si `architecture/generare-automata-questuri-ai.md`.

Foloseste acest ghid pentru draft, preview si verificare. Niciun flux de authoring nu transforma singur draftul intr-un quest live.

## Alege suprafata potrivita

### Quick Quest

Deschide wizard-ul cu:

```text
/ainpc quest quick
```

sau precompleteaza-l local cu:

```text
/ainpc quest create ai contract_lemne type=collect_item
```

Comanda cu `ai` foloseste reguli si hint-uri locale; nu apeleaza implicit un model. `Quick Quest` expune doar opt tipuri: `talk_to_npc`, `collect_item`, `visit_place`, `inspect_node`, `kill_mob`, `craft_item`, `place_block` si `break_block`.

Completeaza giver-ul, numele, tipul, tintele, cantitatile si recompensa. Verifica `Previzualizare YAML` inainte de export.

Limita actuala: preview-ul poate contine mai multe obiective si structura completa, dar `Exporta YAML` ruleaza `quick-export`, care afiseaza in chat o varianta simplificata cu un singur obiectiv. Nu scrie fisier si nu incarca pack-ul. Preseturile afiseaza cantitati ca `x5`, in timp ce serializer-ele asteapta o valoare numerica; verifica manual recompensa exportata. Pentru continut important, trateaza acest flux doar ca scaffolding.

### Formularul avansat

Din `Quest Creator`, deschide `Creeaza Quest Nou` sau foloseste hint-uri avansate:

```text
/ainpc quest create ai advanced name=Provizii id=Q_PROVIZII objective=collect_item:OAK_LOG:3 reward=item:EMERALD:2
```

Formularul avansat expune toate cele 12 tipuri canonice, plus stage-uri, dialog si recompense. Foloseste:

1. preview-ul JSON;
2. `Validate Draft` pentru campurile formularului;
3. `Preview in chat` pentru inspectia artefactului;
4. `Exporta Draft JSON` numai dupa corectare.

Exportul scrie un fisier in `<data-folder>/debug-dumps/quest-drafts/`. Acesta este un draft de authoring, nu un scenario pack publicat.

### Quest Authoring

Suprafata `Quest Authoring` este read-only. Foloseste-o pentru seed, context, warnings, selectie si diagnostic. Nu o descrie ca editor sau publisher.

## Mapping si ancore

- foloseste `Quest Map` pentru inspectarea definitiilor si binding-urilor;
- foloseste objective key stabil pentru fiecare obiectiv;
- rezolva `Region`, `Place` si `Node` prin ID semantic ori selector neambiguu;
- nu presupune ca un preview de ancora este persistent;
- pentru `deliver_to_npc`, verifica limita de schema din `reference/quest-anchor-bindings.md`.

## Verificare corecta

- `/ainpc quest preview` si `/ainpc quest validate` inspecteaza draftul curent;
- `/ainpc debugdump quest` produce diagnostic pentru starea runtime;
- `Testeaza in joc` / `accept nearest` opereaza pe o definitie deja incarcata, nu pe fisierul JSON exportat;
- dupa publicarea separata a unui pack, verifica acceptarea, progresul fiecarui obiectiv, turn-in-ul, recompensa, persistenta dupa restart si mesajele din GUI/debug.

## Criteriu de finalizare

Un quest este gata numai cand exista un pack revizuit si incarcat, ancore valide, progres persistent si un test cap-coada. Un draft, un preview sau un fisier din `debug-dumps` nu indeplineste acest criteriu.

## Legaturi

- `reference/objective-types-reference.md`
- `reference/objective-examples.md`
- `reference/quest-anchor-bindings.md`
- `architecture/progression-service.md`
- `architecture/generare-automata-questuri-ai.md`
- `planning/questuri-avansate-v2.md`
