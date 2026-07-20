# Idei BetonQuest relevante pentru AINPC

Status: nota de inspiratie, non-normativa.
Actualizat: 2026-07-15.

Acest document pastreaza cateva principii de design utile. Nu este dovada ca o functie exista si nu poate suprascrie contractele AINPC verificate in cod.

## Idei utile

- building blocks declarative;
- registri expliciti pentru extensii;
- API public separat de core;
- progres persistent pe obiective;
- evenimente pentru schimbari de stare.

## Ce nu se copiaza implicit

- API static greu de controlat;
- DSL extins inaintea unui runtime stabil;
- hooks publice fara lifecycle si compatibilitate definite;
- concepte externe fara traducere in schema, audit si teste AINPC.

## Regula de adoptie

O idee devine directie AINPC numai dupa ce apare intr-un contract local, are owner, criterii de acceptare si test. Pana atunci ramane inspiratie.

## Legaturi

- `architecture/progression-service.md`
- `reference/api-events-listeners-triggers.md`
- `planning/questuri-avansate-v2.md`
