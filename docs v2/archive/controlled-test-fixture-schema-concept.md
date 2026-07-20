# Concept istoric: schema declarativa pentru fixture controlat

Status: superseded de alternativa hardcodata implementata.
Arhivat: 2026-07-15.

Documentul original propunea o schema declarativa cu `scenario`, `fixture_policy`, `world_fixture`, `population_fixture`, `semantic_context` si `quest_story_tests`.

## Alternativa implementata

- fixture-ul runtime este definit direct in `ControlledTestWorldFixturePlanner`;
- populatia este hardcodata separat in `ControlledTestWorldFixturePopulator`;
- comenzile `plan`, `validate`, `apply`, `populate` si `context` opereaza pe acest model;
- runbook-ul activ documenteaza coordonatele, riscurile si cleanup-ul manual.

Documentul nu este arhivat doar pentru ca schema externa lipseste, ci pentru ca fixture-ul hardcodat este alternativa implementata si folosita in prezent. Daca externalizarea revine ca obiectiv, ea trebuie deschisa ca propunere noua in `planning/`.

## Sursa operationala

- `../operations/mediu-test-controlat-sat-si-structuri-exterioare.md`

## Regula

Arhiva nu interzice o externalizare viitoare; evita doar doua contracte active pentru acelasi fixture curent.
