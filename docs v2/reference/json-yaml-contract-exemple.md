# Exemple JSON/YAML pentru contracte

Status: index catre fixture-urile reale.
Actualizat: 2026-07-15.

Acest document nu inventeaza exemple noi; indica fisierele folosite efectiv de teste si pack-urile livrate.

## Fixture-uri pereche

- `ainpc-core-plugin/src/test/resources/json-yaml-contract/quests.yml`
- `ainpc-core-plugin/src/test/resources/json-yaml-contract/quests.json`
- `ainpc-core-plugin/src/test/resources/json-yaml-contract/world-admin.yml`
- `ainpc-core-plugin/src/test/resources/json-yaml-contract/world-admin.json`

Aceste fisiere verifica paritatea arborelui logic si helper-ele de snapshot. Ele nu sunt schema feature pack de productie.

## Exemple feature pack

- `ainpc-scenario-medieval/src/main/resources/packs/medieval.yml`
- `ainpc-scenario-medieval/src/main/resources/packs/social.yml`
- `ainpc-scenario-medieval/src/main/resources/packs/medieval_quest.yml`
- celelalte fisiere din acelasi director sunt mostre ambalate, dar nu toate sunt instalate automat.

Pack-urile livrate sunt YAML. Pentru un feature pack JSON, aceeasi semantica trebuie verificata printr-un test dedicat loaderului inainte de a fi tratata drept garantie operationala.

## Legaturi

- `reference/json-yaml-contract.md`
- `reference/scenario-pack-schema.md`
- `reference/objective-examples.md`
