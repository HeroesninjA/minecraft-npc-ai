# Release Checklist

Status: canonical in `docs v2`.
Actualizat: 2026-07-10.

Acesta este checklist-ul operational pentru build si livrare pe server Paper.

## Prag minim

- artefact JAR identificabil;
- pornire fara erori critice;
- smoke test minim;
- audit si debugdump disponibile;
- backup si rollback clar.

## Opreste release-ul daca

- testele esueaza fara explicatie;
- JAR-ul lipseste sau nu are `plugin.yml`;
- pluginul nu incarca;
- startup-ul are exceptii repetate;
- `/ainpc` nu raspunde;
- auditul raporteaza eroare critica;
- lipseste backup-ul pentru date reale;
- secretele apar in repo sau debugdump.

## Flux

1. pre-release local;
2. build local;
3. inspectie JAR;
4. pregatire server Paper;
5. startup smoke;
6. smoke mapping si spawn;
7. smoke NPC si rutina;
8. smoke quest;
9. audit si debugdump final;
10. verificare dupa restart;
11. configuratie finala;
12. api/addon freeze;
13. raport release;
14. rollback rapid.

## Legaturi

- `operations/server-admin-runbook.md`
- `operations/debugging-si-testare.md`
- `operations/audit.md`
