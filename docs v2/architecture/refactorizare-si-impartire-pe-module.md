# Refactorizare si Impartire pe Module

Acesta este planul curat pentru finalizarea modularizarii codului.

## Scop

- muta codul legacy in modulele Maven reale;
- separa clar API, core si addonuri;
- sparge clasele mari din nucleu in componente testabile;
- lasa scenariile si continutul sa evolueze fara atingeri dese in infrastructura centrala.

## Stare actuala

- layout-ul Maven este deja mutat pe `ainpc-api`, `ainpc-core-plugin` si `ainpc-scenario-medieval`;
- sursele si resursele core sunt acum in propriul modul;
- `src/src` a fost eliminat dupa migrare;
- task-urile programate au fost extrase din `AINPCPlugin` in `SchedulerCoordinator`;
- validarea curenta confirma ca build-ul trece dupa mutari.

## Ramas de facut

- continua extragerea bootstrap-ului si a reload-ului din `AINPCPlugin`;
- sparge `ScenarioEngine`, `FeaturePackLoader`, `NPCManager` si serviciile mari in componente mai mici;
- sterge referintele istorice la structura veche doar daca nu mai ajuta navigarea.
