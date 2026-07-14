# Prompt Safety Guide

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este ghidul scurt pentru prompturi AI sigure.

## Reguli

- contextul trebuie sa fie limitat si validat;
- prompturile nu primesc secrete;
- AI propune, runtime valideaza;
- fallback-ul trebuie sa existe cand providerul lipseste;
- output-ul care modifica stare trece prin serviciu determinist.

## Ce trebuie evitat

- date sensibile in prompt sau debugdump;
- ordine de actiune nesemnificata;
- tool calls fara validare;
- prompturi care amesteca design cu executie.

## Legaturi

- `architecture/ai-orchestrare-si-mecanici.md`
- `architecture/story-context-service.md`
- `planning/questuri-avansate-v2.md`
