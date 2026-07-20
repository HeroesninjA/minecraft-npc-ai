# Fixture-uri de test si demo world

Status: index operational derivat.
Actualizat: 2026-07-18.

Aceasta pagina separa fixture-urile automate de test de fixture-ul administrativ disponibil in runtime. Nu defineste un contract nou.

## Tipuri

- testele unitare si resursele din `src/test` sunt izolate de serverul real;
- `ControlledTestWorldFixture*` este hardcodat in `src/main` si expus prin comenzi admin;
- lumea demo si smoke-urile de deploy au propriile scripturi si rapoarte.

## Owner operational

Pentru comenzile, coordonatele fixe, limitarile si procedura sigura a fixture-ului runtime, foloseste `operations/mediu-test-controlat-sat-si-structuri-exterioare.md`.

## Regula

- nu presupune ca fixture-ul runtime este dezactivat prin profil;
- nu-l rula intr-o lume reala fara backup si verificarea zonei;
- nu confunda mapping-ul sintetic cu o constructie fizica;
- cleanup-ul este manual pana apare o comanda dedicata.

## Fixture-uri bounded de diagnostic

- auditul JSON are cazuri `small`, `medium` si `large` sub, exact la si peste limita de 100 de constatari retinute per sectiune;
- exportul text si JSON foloseste payload-uri de 128, 256 si 512 bytes fata de un plafon de test de 256 bytes, exercitand aceeasi logica folosita de plafonul runtime de 4 MiB;
- cazurile sub prag si la prag trebuie pastrate integral, iar cazul peste prag trebuie sa ramana valid, bounded si marcat explicit ca trunchiat;
- fixture-urile UTF-8, tail-ul de log si redaction adversarial raman teste separate pentru a izola cauza unei regresii.

## Legaturi

- `operations/mediu-test-controlat-sat-si-structuri-exterioare.md`
- `operations/release-checklist.md`
- `reference/criterii-gata-prim-demo.md`
