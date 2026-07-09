# Mapping

Status: canonical in `docs v2`.
Actualizat: 2026-07-01.

Acesta este rezumatul canonic pentru mapping semantic: `Region -> Place -> Node`.

## Ce face mapping-ul

- descrie lumea in termeni semantici, nu doar in coordonate;
- serveste NPC-urile, questurile, story-ul, GUI-ul si generarea;
- permite inspectie, audit si debug pentru locuri importante;
- ofera un strat stabil pentru bind-uri si ancore.

## Starea curenta

- modelul semantic exista si este citit prin servicii dedicate;
- mapping-ul poate lista regiuni, places si nodes;
- indexarea automata pe chunk-uri este deja folosita pentru lookup;
- API-ul expune modele read-only pentru consum extern.

## Reguli

- `Region` ramane zona mare;
- `Place` este locul semantic intermediar;
- `Node` este punctul exact de interactiune;
- coordonatele nu sunt contractul principal;
- ID-urile trebuie sa fie stabile si auditable;
- codul trebuie sa consume API/service, nu YAML direct.

## Folosire recomandata

- questurile consuma ancore si tag-uri semantice;
- NPC-urile consuma bind-uri de home/work/social;
- GUI-ul afiseaza mapping-ul ca stare inspectabila;
- story-ul si AI-ul citesc contextul, nu inventeaza locatii;
- generarea foloseste mapping-ul ca strat de validare.

## Documente legate

- `canonical/implementat-deja.md`
- `planning/questuri-avansate-v2.md`
- `guides/gui-interfete.md`
- `architecture/ai-orchestrare-si-mecanici.md`
