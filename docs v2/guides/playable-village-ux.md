# Playable Village UX

Status: criterii operationale de experienta pentru demo.
Verificat fata de runtime: 2026-07-16.

Aceasta pagina defineste ce trebuie sa poata intelege un player in demo. Implementarea si dovezile serverului raman in runbook-urile operationale.

## Parcurs minim

1. playerul deschide `/ainpc gui` si ajunge in `Player Hub`;
2. vede questurile/progresiile disponibile si poate deschide detaliile;
3. identifica un NPC apropiat si intelege actiunile talk, quest, shop si rutina disponibile;
4. vede contextul world numai daca are permisiunea potrivita;
5. primeste feedback clar la actiune blocata, feature dezactivat sau date lipsa;
6. dupa restart, progresia si mapping-ul care pretind persistenta sunt reverificate.

## Criterii UX

- ecranul arata contextul curent, actiunea principala si cauza unei blocari;
- butoanele de inspectie nu sunt etichetate ca mutatii;
- `preview`, `draft`, `export`, `persist` si `publish` sunt termeni distincti;
- inchiderea inventarului pentru o comanda este urmata de un rezultat lizibil in chat;
- refresh-ul arata starea curenta, iar listele goale au explicatie;
- interactiunea NPC, oferta quest si shop-ul nu cer playerului sa ghiceasca ID-uri interne;
- erorile nu expun secrete ori prompturi brute.

## Limite curente

- onboarding-ul dedicat de profil/starter kit nu este implementat; exista separat scenarii quest de tip tutorial;
- navigarea standard revine la hub-ul principal, fara breadcrumb complet;
- unele liste au paginare proprie, altele nu;
- inputul creator prin chat nu este flux player normal;
- confirmarea este implementata numai pentru actiunile care o cer explicit.

## Verificare

- executa parcursul cu un player normal, unul creator si unul admin;
- noteaza fiecare buton disabled si permisiunea care il explica;
- testeaza feature flags dezactivate fara a considera inchiderea inventarului crash;
- coreleaza mesajele GUI cu logul, auditul si dovezile din `operations/demo-server-verification.md`;
- nu declara demo-ul PASS numai pentru ca inventarele se deschid.

## Legaturi

- `reference/gui-stack.md`
- `guides/gui-interfete.md`
- `operations/playable-village-runbook.md`
- `operations/demo-server-verification.md`
- `planning/player-onboarding-initiere.md`
- `planning/gui-ux-hardening.md`
