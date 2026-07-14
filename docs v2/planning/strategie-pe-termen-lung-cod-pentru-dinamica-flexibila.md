# Strategie pe termen lung: cod mai putin hardcoded, mai mult datadriven si generativ

Acesta este planul de directie pentru reducerea hardcode-ului pe termen lung.

## Principiu

- muta continutul si comportamentul cat mai mult in date, reguli si generare controlata;
- pastreaza hardcode doar pentru siguranta, validare, persistenta critica si protectie a starii;
- foloseste audit si inspectie pentru a evita regresii greu de urmarit.

## Zone tintite

- modele semantice ale lumii;
- bindings persistente pentru NPC-uri;
- engine pe reguli si scoruri;
- generatoare de continut;
- validare explicita si rollback controlat.
