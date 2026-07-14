# Schema scenariu predefinit de testare

Acesta este contractul pentru scenariul controlat de test peste un sat si NPC-uri predictibile.

## Scop

- defineste un fixture de lume si un fixture de scenariu;
- permite testarea questurilor si story-ului intr-un mediu stabil;
- evita dependenta de generare aleatoare sau de o harta finala incompleta.

## Reguli

- activare explicita prin profil de test sau comanda admin;
- dezactivat implicit pe serverele reale;
- ID-uri stabile si namespaced;
- mapping semantic inainte de spawn, quest sau story;
- audit si debugdump la fiecare pas relevant.

## Structura

- `scenario` pentru identitate si versiune;
- `fixture_policy` pentru reguli de utilizare;
- `world_fixture` pentru sat, cladiri si structuri;
- `population_fixture` pentru NPC-uri si roluri;
- `semantic_context` pentru istoric si relatii;
- `quest_story_tests` pentru questuri smoke si evenimente asteptate.
