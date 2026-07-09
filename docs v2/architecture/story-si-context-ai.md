# Story si Context AI

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este rezumatul pentru legatura dintre story, context local si questuri.

## Rol

- conecteaza mapping-ul semantic cu progresul si naratiunea;
- expune context read-only pentru AI si GUI;
- pastreaza separarea dintre indicii narative si starea canonica;
- ajuta questurile sa identifice locuri, personaje si evenimente relevante.

## Reguli

- contextul se construieste din surse validate;
- AI primeste context, nu autoritate asupra starii;
- quest anchors, story events si mapping-ul semantic raman auditable;
- schimbarea de story state trece prin servicii specializate.

## Stare curenta

- exista baza pentru story context read-only;
- questurile pot folosi context narativ pentru briefing si progres;
- GUI-ul poate prezenta starea story fara sa o modifice;
- pipeline-ul de generare ramane asistat si controlat.

## Legaturi

- `canonical/implementat-deja.md`
- `architecture/mapping.md`
- `architecture/ai-orchestrare-si-mecanici.md`
- `planning/questuri-avansate-v2.md`
