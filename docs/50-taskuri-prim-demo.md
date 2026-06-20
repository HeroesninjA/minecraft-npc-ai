# 50 de Taskuri pentru Primul Demo Jucabil AINPC

Actualizat: 2026-06-15

## Scop

Acest document defineste cele 50 de taskuri necesare pentru a trece primul demo jucabil AINPC. Fiecare task are un **prompt AI** care explica contextul, dependintele, comenzi de verificat si criterii de trecere.

Taskurile sunt grupate pe fazele D0-D9.

---

## D0: Scope & Baseline

### T001 Server dedicat si variabile de mediu
**Descriere:** Alege si documenteaza serverul Paper target, numele playerului, regionId, cali JAR-uri.
**Prompt AI:** Creeaza un document cu variabilele de mediu: ServerDir, RegionId (implicit demo_sat), PlayerName, JAVA_HOME, OpenAI enabled (da/nu). Folosit de toate taskurile urmatoare. Fara API keys.

### T002 Definirea criteriilor de gata
**Descriere:** Stabileste ce inseamna primul demo e gata.
**Prompt AI:** Pe baza docs/prim-demo-functionalitate-minima-diversa.md, extrage Non-Obiective: generator cladiri, story avansat, economie, reputatie. Marcheaza-le ca non-blocante.

### T003 Inventar comenzi existente
**Descriere:** Listeaza toate comenzile /ainpc necesare pentru demo, grupate pe faze.
**Prompt AI:** Extrage din documentatie TOATE comenzile si grupeaza-le pe D0-D9. Format tabelar. Folosit de toate taskurile de verificare.

### T004 Backup initial
**Descriere:** Asigura backup inainte de a lucra pe serverul Paper.
**Prompt AI:** Creeaza o procedura: copiaza plugins/AINPC/, ainpc_data.db, JAR-urile in backup/ cu timestamp.

---

## D1: Build & Config

### T005 Build curat
**Descriere:** Ruleaza clean build si verifica testele.
**Prompt AI:** Ruleaza gradlew clean build (JAVA_HOME JDK 21+). Verifica: BUILD SUCCESSFUL, 0 erori, 458/458 teste.

### T006 Artefacte JAR
**Descriere:** Verifica JAR-urile core, addon, API.
**Prompt AI:** Verifica existenta si dimensiunea celor 3 JAR-uri. Daca lipsesc, ruleaza assemble.

### T007 Config minima YAML
**Descriere:** Config.yml cu setarile minime.
**Prompt AI:** Asigura-te ca world_admin.enabled: true, simulation.enabled: true, routine.enabled: true. API key NU in config.

### T008 Verificare incarcare plugin
**Descriere:** Porneste Paper, verifica plugin incarcat fara erori.
**Prompt AI:** /plugins arata AINPC enabled. /ainpc raspunde. /ainpc audit db fara erori critice.

### T009 Config quest-uri
**Descriere:** Verifica quests.yml exista.
**Prompt AI:** quests.yml exista dupa primul start. Contine village_contracts, npc_duties, local_bounties.

### T010 Config rutina si simulare
**Descriere:** Activeaza setarile de rutina.
**Prompt AI:** routine.enabled true, routine.teleport_enabled true, routine.natural_movement true, simulation.enabled true.

---

## D2: Mapping demo_sat

### T011 Creare harta demo
**Descriere:** Genereaza harta semantica a satului demo.
**Prompt AI:** /ainpc world demo create demo_sat in zona plata. Minim 1 house, 1 workplace, 1 social. Fara erori.

### T012 Verificare regiuni si locuri
**Descriere:** Verifica structura hartii.
**Prompt AI:** /ainpc world places demo_sat arata minim 3 locuri cu tip si coordonate valide.

### T013 Verificare noduri
**Descriere:** Verifica punctele de interes.
**Prompt AI:** Minim 1 nod quest_board sau interaction. /ainpc audit world fara gap-uri critice.

### T014 Salvare harta
**Descriere:** Salveaza harta si verifica persistenta.
**Prompt AI:** /ainpc world save fara erori. Dupa restart datele sunt aceleasi.

### T015 Auto-index
**Descriere:** Verifica auto-indexul.
**Prompt AI:** Cu auto-index ON, whereami intr-un loc cunoscut. Dezactiveaza, repeta. Acelasi rezultat.

### T016 World audit
**Descriere:** Audit complet al lumii.
**Prompt AI:** /ainpc audit world. Niciun gap critic. Toate locurile complete.

### T017 Persistenta mapping
**Descriere:** Harta supravietuieste restartului.
**Prompt AI:** Salveaza, restart, verifica. Regiuni, locuri, noduri identice.

---

## D3: NPC Population & Bindings

### T018 Planificare asezare
**Descriere:** Planifica casele pentru populare.
**Prompt AI:** /ainpc world settlement plan demo_sat 5. Case disponibile, fara erori.

### T019 Spawn asezare
**Descriere:** Populeaza satul cu NPC-uri.
**Prompt AI:** /ainpc world settlement spawn demo_sat 5. 3-5 NPC create, apar in list. Rollback la esec partial.

### T020 Verificare NPC-uri
**Descriere:** Detaliile fiecarui NPC.
**Prompt AI:** /ainpc list. Fiecare NPC: nume, ocupatie, profil DB, spawnat, fara duplicate.

### T021 NPC bindings
**Descriere:** Verifica home/work/social.
**Prompt AI:** /ainpc world bindings pentru fiecare. home, work, social setate. Locurile exista in demo_sat.

### T022 Audit NPC
**Descriere:** Audit NPC complet.
**Prompt AI:** /ainpc audit npc. Fara NPC fara entitate, fara duplicate, profile valide.

### T023 Audit spawn
**Descriere:** Integritatea spawn-ului.
**Prompt AI:** /ainpc audit spawn. Case populate corect, fara NPC fara entitate.

### T024 NPC persist dupa restart
**Descriere:** NPC-urile supravietuiesc restartului.
**Prompt AI:** Restart, /ainpc list. Acelasi numar, aceleasi bindings, aceleasi profile.

---

## D4: Routine & UX

### T025 Rutina NPC
**Descriere:** Verifica rutine inspectabile.
**Prompt AI:** Langa un NPC, /ainpc routine status nearest. Afiseaza slotul curent.

### T026 Tick rutina
**Descriere:** Tick manual de rutina.
**Prompt AI:** /ainpc routine tick. Admin summary, fara spam in chat.

### T027 Interactiune click dreapta
**Descriere:** Click pe NPC deschide interactiunea.
**Prompt AI:** Click pe NPC arata actiunea urmatoare clara.

### T028 NPC moves
**Descriere:** NPC-urile nu sar haotic.
**Prompt AI:** Observa 1-2 minute. Cooldown functioneaza, teleport doar fallback.

### T029 GUI hub
**Descriere:** GUI principal functioneaza.
**Prompt AI:** /ainpc gui deschide hub-ul. Toate sectiunile se deschid.

### T030 Nearest selector
**Descriere:** nearest e selector explicit.
**Prompt AI:** info nearest si quest nearest functioneaza. Fara NPC, mesaj clar.

---

## D5: Quest + Progression

### T031 Quest clasic
**Descriere:** Quest poate fi acceptat si inspectat.
**Prompt AI:** /ainpc quest nearest, /ainpc quest accept nearest. Obiective folosesc locuri/noduri demo_sat.

### T032 Quest status
**Descriere:** Status inspectabil.
**Prompt AI:** /ainpc quest status nearest. Stage curent, obiective, tracking.

### T033 Progression non-quest
**Descriere:** O mecanica non-QUEST e listata.
**Prompt AI:** /ainpc progression definitions. Contract, duty, bounty, event, tutorial, ritual.

### T034 Progression stored
**Descriere:** Progresia persistata e inspectabila.
**Prompt AI:** /ainpc progression stored <player> dupa interactiune.

### T035 Quest tracking
**Descriere:** Tracking-ul quest-ului functioneaza.
**Prompt AI:** /ainpc quest track start, busola indica tinta. Tracking persistent dupa restart.

### T036 Questuri nu depind de AI
**Descriere:** Progresul nu necesita AI extern.
**Prompt AI:** Cu OpenAI dezactivat, quest-urile functioneaza complet. Fallback suficient.

---

## D6: Story Context

### T037 Story context
**Descriere:** Context narativ inspectabil.
**Prompt AI:** /ainpc story context. Foloseste mapping si ancore quest.

### T038 Story region
**Descriere:** Starea povestii pe regiune.
**Prompt AI:** /ainpc story region demo_sat. Arata starea. Zero stacktrace.

### T039 Story events
**Descriere:** Evenimentele de poveste.
**Prompt AI:** /ainpc story events dupa quest completat. Minim 1 event vizibil.

### T040 Debugdump story
**Descriere:** Export story fara secrete.
**Prompt AI:** /ainpc debugdump story. Fara API keys, token-uri sau parole.

---

## D7: Dialog & AI Fallback

### T041 Dialog cu NPC
**Descriere:** Conversatia functioneaza.
**Prompt AI:** Click pe NPC, mesaj scurt, raspuns sau fallback. Inchide cu pa. Fara stacktrace.

### T042 Fallback AI
**Descriere:** Dialogul functioneaza fara API key.
**Prompt AI:** Oprire, sterge OPENAI_API_KEY, porneste. Confirmare fallback clar. Server stabil.

### T043 Debug OpenAI
**Descriere:** Scriptul debug functioneaza.
**Prompt AI:** scripts/debug-openai.ps1. Testeaza acces model. Log in debug-logs/.

---

## D8: Restart & Persistenta

### T044 Smoke test mapping
**Descriere:** Scriptul smoke-mapping trece.
**Prompt AI:** smoke-paper-mapping.ps1 -ServerDir path -SkipWandFlow. Verifica raportul.

### T045 Smoke test quests
**Descriere:** Scriptul smoke-quests trece.
**Prompt AI:** smoke-paper-quests.ps1 -ServerDir path -PlayerName nume -RegionId demo_sat.

### T046 Comenzi demo
**Descriere:** Toate /ainpc demo raspund.
**Prompt AI:** definition, status, next, phases, script, evidence, runbook, smoke, summary, commands, restart.

### T047 Restart gate
**Descriere:** Verificari inainte si dupa restart.
**Prompt AI:** /ainpc demo restart demo_sat. Oprire, pornire, re-audit.

---

## D9: Demo Script Final

### T048 Audit final
**Descriere:** Audit fara secrete.
**Prompt AI:** /ainpc audit all, /ainpc debugdump all. Fara API keys in rapoarte.

### T049 Script demo complet
**Descriere:** Testerul urmeaza pasii.
**Prompt AI:** 20 pasi: join, status, definition, places, spawn, list, routine, dialog, quest, quest accept, status, progression, story, audit, debugdump, status final.

### T050 Documentare concluzie
**Descriere:** Rezumat si urmatorii pasi.
**Prompt AI:** Ce a mers, ce n-a mers, ce taskuri au ramas, ce urmeaza dupa D9.