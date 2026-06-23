# Interactiune, Dialog si Reactie

Actualizat: 2026-06-23

Acesta este punctul de intrare pentru stack-ul care leaga interactiunea cu NPC, formularea dialogului si reactia NPC la jucator.

## Cum se citeste

1. `interactiuni.md` - fluxul concret de click, chat, sesiuni si intentii de quest.
2. `dialog-si-conversatii.md` - continutul dialogului, contextul si formularea.
3. `reactie-npc-jucator.md` - starea NPC, emotii, memorie si pipeline-ul de reactie.

## Ce acopera impreuna

- cum porneste o conversatie;
- cum este aleasa tinta;
- cum se separa dialogul de progresul determinist;
- cum se transforma evenimentul jucatorului in reactie NPC;
- ce ramane in runtime si ce ramane in text/formulare.

## Regula comuna

```text
Interactiunea stabileste contextul.
Dialogul formuleaza raspunsul.
Reactia aplica stare, emotie si decizie.
Serviciile deterministe decid progresul si persistenta.
```

## Contracte separate

Chiar daca sunt legate, cele trei doc-uri raman separate pentru:

- fluxul de interactiune;
- contractul de continut al dialogului;
- pipeline-ul de reactie si stare emotionala.

## Relatii

- `interactiuni.md` - intrare pentru sesiuni si intentii
- `dialog-si-conversatii.md` - contract de dialog
- `reactie-npc-jucator.md` - contract de reactie
- `story-context-service.md` - context narativ read-only
- `questuri-avansate-v2.md` - progres determinist peste intentii si dialog
