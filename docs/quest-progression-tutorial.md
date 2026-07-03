# Quest / Progression Tutorial

Actualizat: 2026-07-01

Acest ghid arata fluxul recomandat pentru creare asistata de `Quest` si `Progression`.

## 1. Porneste din comanda asistata

Pentru quest:

```text
/ainpc quest create ai
```

Pentru progression:

```text
/ainpc progression create ai
```

Pentru un ghid mai detaliat de authoring foloseste `docs/quest-authoring-tutorial.md`.

## 2. Lasa wizard-ul sa precompleteze

Wizard-ul poate propune:

- nume
- id
- selector de NPC sau de anchor
- tip
- mecanica / model de progresie
- reward-uri sau obiective

Daca o valoare nu este clara, sistemul lasa campul editabil.

Poti folosi si hinturi explicite in comanda:

```text
/ainpc quest create ai name=quest_intro giver=blacksmith type=talk_to_npc reward=xp
/ainpc progression create ai selector=quest_intro mechanic=xp
```

## 3. Corecteaza cu sugestii

Foloseste:

- `Suggest`
- `Edit`
- `Back`
- `Next`

Cand exista conflict de nume sau de tip, ai disponibile si:

- `Auto rename`
- `Auto fit`
- `Auto move`

## 4. Verifica preview-ul

Nu confirma pana cand preview-ul arata corect:

- quest-ul are un nume stabil
- selectorul puncteaza obiectivul dorit
- progression-ul foloseste mecanica potrivita
- legaturile cu `Region / Place / Node` sunt corecte

## 5. Foloseste ancorele din world

Quest si progression pot fi legate de:

- `region`
- `place`
- `node`

Exemple:

```text
/ainpc world create ai preview region name=curte_castel type=castle size=48
/ainpc map edit
/ainpc map confirm
/ainpc world create ai preview place name=poarta_castel region=curte_castel type=castle_room size=16
/ainpc map edit
/ainpc map confirm
/ainpc world create ai preview node name=intrare_castel region=curte_castel place=poarta_castel type=entrance radius=2.5
/ainpc map edit
/ainpc map confirm
```

Pentru mapping asistat, `preview`, `dryrun` si `inspect` creeaza draftul fara scriere directa. Confirma doar dupa ce editorul si preview-ul vizual arata corect.

## 6. Inspecteaza dupa confirmare

Comenzi utile:

```text
/ainpc quest create ai help
/ainpc progression create ai help
/ainpc debugdump quest summary
/ainpc debugdump progression summary
```

## 7. Rezumat recomandat

Ordinea buna este:

1. pornesti `create ai`
2. completezi wizard-ul
3. corectezi cu `Suggest` sau `Edit`
4. confirmi doar dupa preview valid
5. verifici rezultatul cu `help` sau `debugdump`
