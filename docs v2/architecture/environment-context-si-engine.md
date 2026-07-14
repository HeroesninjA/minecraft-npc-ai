# Environment Context si Environment Engine

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Rezumatul mediului read-only si al motorului de semnale de mediu.

## Starea curenta

- `EnvironmentContext` si `EnvironmentEngine` sunt considerate implementate;
- focusul este pe snapshot-uri locale despre lume, timp si vreme;
- contextul alimenteaza story si NPC systems.

## Rol

- afla unde este jucatorul;
- expune regiune, place si node;
- ofera timp, vreme si semnale utile;
- ramane read-only pentru consumatori.

## Legaturi

- `architecture/story-context-service.md`
- `architecture/mapping.md`
