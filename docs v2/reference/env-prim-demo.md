# Environment Variables pentru Primul Demo

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Referinta pentru variabilele de mediu si parametrii necesari primului demo jucabil.

## Acopera

- `ServerDir`;
- `RegionId`;
- `PlayerName`;
- `JAVA_HOME`;
- `OpenAI`;
- `CoreJAR`, `AddonJAR`, `ApiJAR`.

## Reguli

- serverul local trebuie sa existe;
- JAR-urile merg in `plugins/`;
- cheia OpenAI se citeste din env, nu din fisiere;
- demo-ul trebuie sa mearga si fara OpenAI.

## Legaturi

- `operations/test-fixtures-and-demo-world.md`
- `planning/prim-demo-functionalitate-minima-diversa.md`
