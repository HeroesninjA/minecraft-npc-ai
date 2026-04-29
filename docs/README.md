# Documentatie

Actualizat: 2026-04-29

Acest folder contine documentatia tehnica locala a proiectului.

Fisiere:

- `analiza-erori-si-plan-rezolvare.md` - raport tehnic despre erorile confirmate, problemele structurale si ordinea recomandata de remediere
- `betonquest-directii-potrivite-pentru-ainpc.md` - analiza directiilor BetonQuest care se potrivesc cu AINPC
- `debugging-si-testare.md` - ghid practic pentru scripturi, audit, debug dump, teste Maven si smoke tests pe server
- `documentatie-api.md` - contractul public curent din `ainpc-api`, inclusiv `WorldAdminApi`
- `generare-ai-si-constructie-automata.md` - directia recomandata pentru generare AI, constructie automata, template-uri si integrare optionala WorldEdit
- `generare-sate-fara-worldedit.md` - plan si status pentru scanarea, maparea si completarea satelor vanilla cu loturi, proportii, mapping semantic si fara WorldEdit API obligatoriu
- `generare-sate-worldedit-si-npc.md` - design pentru generare de sate cu WorldEdit, asociere NPC, case, locuri de munca si decoratii
- `implementat-deja.md` - rezumat al functionalitatii existente deja in cod
- `mapping.md` - starea actuala a sistemului de mapare: regiuni, places, nodes, audit si limitarile ramase
- `mapping-pentru-implementari-ulterioare.md` - cum trebuie consumat mapping-ul in questuri, NPC-uri, generare, AI si ce imbunatatiri merita adaugate
- `npc-uri-temporare-si-episodice.md` - design pentru NPC-uri temporare, low-impact si non-villager
- `ordine-spawn-npc-cladiri-region-node.md` - ordinea recomandata si statusul fazelor pentru generare regiune, cladiri, node-uri, spawn NPC, atasare familie, scanare vanilla si rutina
- `questuri-avansate.md` - documentatie pentru evolutia sistemului de questuri la nivel avansat
- `reactie-npc-jucator.md` - design pentru sistemul de reactie dintre NPC si jucator
- `reducere-marime-jar.md` - plan de refactorizare pentru micsorarea JAR-ului core prin reducerea dependintelor shaded
- `refactorizare-si-impartire-pe-module.md` - document de refactorizare si migrare a surselor catre modulele Maven reale
- `roadmap-orientativ.md` - roadmap realist pentru first playable, modularizare, runtime de scenarii si addonuri
- `rutine-npc-si-timeline.md` - ordinea recomandata si statusul initial pentru TimeService, rutine NPC, household/familie, quest hooks si timeline
- `strategie-plugin-modular-si-scenarii-programabile.md` - strategie pentru plugin modular si scenarii programabile
- `surse-inspiratie-plugin-ainpc.md` - surse publice de inspiratie pentru NPC-uri, questuri, dialog AI, world mapping, extensibilitate si generare de continut

Observatii:

- `TODO.md` ramane in radacina proiectului ca lista de lucru
- `docs.text` ramane separat ca document intern de referinta
