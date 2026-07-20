# Contract JSON/YAML pentru documente declarative

Status: referinta derivata; nu inlocuieste schemele de domeniu.
Actualizat: 2026-07-17.

JSON si YAML pot reprezenta acelasi arbore logic in fixture-urile de contract, dar fiecare domeniu isi pastreaza propria schema si propriul loader.

## Ce este implementat

- `JsonYamlContractFixturesTest` incarca perechi JSON/YAML pentru quest si world admin;
- testele compara aceleasi valori logice si alimenteaza helper-ele de snapshot/debug;
- `ScriptDocumentNormalizer` normalizeaza documente de script pentru diagnostic;
- `FeaturePackLoader` selecteaza fisiere `.yml`, `.yaml` si `.json` din directorul de pack-uri.
- `FeaturePackLoader` foloseste `ScriptConfigurationLoader` pentru pre-scanarea metadata si pentru incarcarea efectiva a fiecarui pack;
- testul JSON feature-pack continua dupa parsare prin validatorul metadata si `FeaturePackYamlSupport`, pana la modelele runtime.

## Ce nu trebuie dedus

- nu exista o singura schema comuna pentru world admin, quest drafts si feature packs;
- paritatea sintactica nu garanteaza ca fiecare consumer accepta ambele formate;
- fixture-urile din `src/test/resources` nu sunt continut instalat pe server;
- schema feature pack este definita in `reference/scenario-pack-schema.md` si in codul loaderului.

## Regula

- defineste mai intai schema de domeniu;
- foloseste JSON/YAML doar ca reprezentari;
- valideaza separat sintaxa, metadata si semantica runtime;
- nu promite suport de format fara fixture si test pentru consumerul respectiv.

## Surse in cod

- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/engine/JsonYamlContractFixturesTest.kt`
- `ainpc-core-plugin/src/test/resources/json-yaml-contract/`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/debug/ScriptDocumentNormalizer.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/FeaturePackLoader.kt`

## Legaturi

- `reference/json-yaml-contract-exemple.md`
- `reference/scenario-pack-schema.md`
- `reference/mapping-stack.md`
