# Reactia NPC la jucator

Status: model canonic pentru evaluarea si aplicarea reactiei NPC.
Actualizat: 2026-07-14.

Reactia leaga rezultatul actiunii jucatorului de relatia, memoria si emotia NPC-ului.

## Pipeline

1. primeste intentia si rezultatul validat de runtime;
2. citeste relatia, memoria, emotia si contextul local;
3. calculeaza reactia prin reguli deterministe;
4. aplica modificarile prin serviciile responsabile;
5. transmite dialogului rezultatul care poate fi exprimat.

## Regula

- reactia nu este doar text;
- dialogul este expresia externa a unei reactii deja validate;
- questurile, combatul si simularea reutilizeaza acelasi model de reactie.

## Legaturi

- `architecture/interactiuni.md`
- `architecture/dialog-si-conversatii.md`
- `architecture/interactiune-dialog-reactie-stack.md`
