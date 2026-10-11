# Playable Village Runbook

Status: runbook operational derivat.
Actualizat: 2026-10-05 (salvare mapping si verificare dupa restart).

Procedura aduce un sat demo verificabil pe un server Paper controlat. Nu promite worldgen fizic complet si nu considera output-ul unui script drept PASS fara inspectie.

## Inainte de pornire

- foloseste un server de test sau un backup restaurabil;
- instaleaza numai core JAR si addonurile Paper dorite, nu `ainpc-api` ca plugin;
- verifica backend-ul si credentialele conform runbook-ului de server;
- pentru fixture-ul hardcodat, confirma lumea si coordonatele inainte de apply;
- stabileste un player de test si un `regionId` unic.

## Flux

1. construieste, inspecteaza si instaleaza artefactele conform `operations/server-admin-runbook.md`;
2. porneste Paper si pastreaza logul complet de startup;
3. confirma pluginurile, DB si feature flags;
4. creeaza ori importa mapping-ul demo folosind ghidul potrivit;
5. aplica fixture-ul controlat numai daca zona poate fi modificata in siguranta;
6. planifica si creeaza populatia, apoi verifica bindings-urile;
7. executa interactiunea, rutina, questul, progresia si story context-ul activate;
8. ruleaza gate-urile din `operations/demo-server-verification.md`;
9. ruleaza `/ainpc world save` si sincronizeaza sectiunea completa `world_admin` in overlay-ul prioritar; salvarea in `config.yml` nu actualizeaza overlay-ul;
10. opreste controlat, reporneste si verifica separat mapping-ul, NPC-urile, household-urile, binding-urile si evenimentele/starea story;
11. genereaza auditul final si un debugdump revizuit manual.

Procedura de sincronizare este in [configurarea lumii](../reference/configurare-lume-harta-generare.md). Pentru un traseu concret cu o casa, fierarie, piata si un NPC, foloseste [exemplul complet](../guides/exemplu-complet-sat-npc-story.md). Exemplul contine rezultate asteptate, nu dovada unui test live reusit.

## Automatizari existente

- `scripts/setup-docker-demo.ps1` pregateste mediul Docker, dar nu certifica demo-ul;
- `scripts/smoke-demo-complet.ps1` poate executa comenzi prin RCON, dar nu valideaza semantic raspunsurile;
- `scripts/test-demo.ps1` este checklist, nu test automat;
- smoke-urile specializate si raportul de release trebuie corelate cu output-ul serverului.

Nu porni deploy-ul sau mutatiile pe un server live printr-un script care are cai, Java, restart ori credentiale nevalidate pentru mediul tinta.

## Criteriu de finalizare

Satul este demonstrabil numai cand mapping-ul, NPC-urile, interactiunea, quest/progression, story, restartul, auditul si backup/rollback-ul au dovezi. Functiile dezactivate sau in afara scope-ului se marcheaza `N/A` cu motiv, nu `PASS`.

## Legaturi

- `operations/demo-server-verification.md`
- `operations/test-fixtures-and-demo-world.md`
- `operations/mediu-test-controlat-sat-si-structuri-exterioare.md`
- `operations/migration-si-backup.md`
- `reference/criterii-gata-prim-demo.md`
