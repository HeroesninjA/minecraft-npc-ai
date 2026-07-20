# Simulation Stack

Status: index canonic de navigare.
Actualizat: 2026-07-15.

Acest index separa runtime-ul verificat, persistenta de lume si roadmap-ul ramas. Nu defineste un serviciu nou.

## Ordinea recomandata

1. `architecture/simulation-service.md` - cele doua tick-uri reale si limitele lor;
2. `architecture/harta-clase-routine.md` - clasele active din rutina;
3. `architecture/comportament-natural-npc-rutine-alocari.md` - alocare si miscare;
4. `architecture/evita-comportamentul-robotic-prin-rutine-staggered.md` - offseturile implementate;
5. `architecture/npc-world-bindings.md` - sursa persistenta pentru ancore semantice;
6. `architecture/households-persistente.md` - persistenta de spawn, nu autoritate de rutina;
7. `architecture/npc-uri-temporare-si-episodice.md` - lifecycle-ul actorilor de scenariu;
8. `planning/rutine-npc-si-timeline.md` - numai gap-uri si ordine viitoare.

## Continut retras

Seria `Simulation Service - Partea 2/3/4` descria o extractie neimplementata. A fost consolidata in `archive/simulation-service-extraction-concept.md` si nu mai face parte din traseul activ.

## Regula

- `architecture/simulation-service.md` detine starea runtime-ului;
- documentele de arhitectura specializate detin contractele locale;
- roadmap-ul si arhiva nu pot confirma implementarea.
