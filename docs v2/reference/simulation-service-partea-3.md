# Simulation Service - Partea 3

Acesta este designul pentru semnale, evenimente si consumatori controlati ai simulatiei.

## Scop

- transformă simularea in sursa de semnale de gameplay, nu in motor care face totul;
- emite semnale validate si auditate pentru quest, story, dialog si debug;
- introduce reguli declarative, cooldown si agregare pe regiune sau household.

## Principiu

- simularea observa si emite semnale;
- sistemele specializate decid ce devin acele semnale;
- consumatorii trebuie sa declare explicit ce citesc.
