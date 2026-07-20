# Build Mode pentru Region / Place / Node

Status: ghid operational detaliat.
Verificat in cod: 2026-07-15.
Depinde de: `architecture/mapping.md`.

## Separarea responsabilitatilor

- `Build Mode` pastreaza stilul si tinta sesiunii de authoring;
- `MappingWandService` pastreaza selectia, draftul si preview-ul jucatorului;
- `MappingDraftFactory` transforma selectia si descrierea intr-un draft;
- `WorldAdminService` valideaza si aplica modificarea runtime;
- `/ainpc world save` persista modificarile in `config.yml`.

## Stiluri recomandate

### Wand pentru Region si Place

- foloseste `pos1` si `pos2` pentru bounds;
- `Region` defineste containerul mare;
- `Place` trebuie selectat complet in interiorul regiunii;
- preview-ul ramane temporar si poate fi refacut inainte de confirmare.

```text
/ainpc build mode wand region
/ainpc build mode wand place
```

### Point pentru Node

- foloseste un singur punct si o raza pozitiva;
- node-ul poate indica un place sau poate ramane direct sub regiune;
- `npc_bind` si `quest_anchor` sunt moduri specializate ale wand-ului, nu niveluri noi in ierarhia mapping.

```text
/ainpc build mode point node
/ainpc wand mode npc_bind
/ainpc wand mode quest_anchor
```

### Sign

`sign` este o intrare alternativa pentru authoring asistat. Nu o trata ca scriere directa: rezultatul trebuie verificat ca draft si confirmat prin acelasi lant de validare.

## Ciclul unei sesiuni

1. alege stilul si tinta;
2. captureaza bounds-ul sau punctul;
3. creeaza draftul cu `/ainpc map ...`;
4. verifica `/ainpc map preview`;
5. corecteaza cu `/ainpc map edit` sau anuleaza;
6. confirma cu `/ainpc map confirm`;
7. ruleaza `/ainpc audit world`;
8. persista cu `/ainpc world save`.

## Ce nu garanteaza Build Mode

- nu salveaza automat dupa confirmare;
- nu transforma preview-ul in sursa de adevar;
- nu permite unui place sa iasa din regiune;
- nu permite unui node sa iasa din containerul ales;
- nu ocoleste modul MCP `read_only`;
- nu transforma descrierea libera sau o sugestie AI in autoritate asupra datelor.

## Legaturi

- `guides/build-mode-tutorial.md`
- `guides/mapping-harti-manuale.md`
- `reference/mapping-stack.md`
- `architecture/mapping.md`
