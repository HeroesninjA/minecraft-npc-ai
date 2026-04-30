# Categorii Documentatie

Actualizat: 2026-04-30

Acest folder separa documentatia pe categorii fara sa mute documentele canonice din radacina `docs/`.

Motiv:

- linkurile existente raman stabile
- documentele pot fi gasite mai rapid pe zona de lucru
- mutarea fizica a documentelor poate fi facuta ulterior, dupa o migrare controlata a referintelor

## Categorii

| Categorie | Scop | Index |
|---|---|---|
| Stare si roadmap | Ce exista, ce urmeaza, ce faze sunt active | `00-stare-roadmap/README.md` |
| World mapping si spawn | Regiuni, places, nodes, spawn order, household | `01-world-mapping-spawn/README.md` |
| NPC, rutine si simulare | NPC-uri, rutina, timeline, reactii si NPC temporari | `02-npc-rutine-simulare/README.md` |
| Quest, story si AI | Questuri, story state, context AI si authoring asistat | `03-quest-story-ai/README.md` |
| Generare si worldgen | Generare sate, WorldEdit optional, constructie asistata | `04-generare-worldgen/README.md` |
| API, modularizare si addonuri | API public, module, scenarii programabile si addonuri | `05-api-modularizare-addonuri/README.md` |
| Operare si hardening | Audit, debugging, testare, jar size si productie | `06-operare-hardening/README.md` |
| Referinte si arhiva | Surse externe, materiale brute, versiuni vechi | `07-referinte-arhiva/README.md` |

## Regula

Documentele de categorie sunt indexuri, nu surse primare. Cand schimbi o functionalitate, actualizeaza documentul canonic si apoi indexul categoriei doar daca se schimba rolul sau prioritatea lui.
