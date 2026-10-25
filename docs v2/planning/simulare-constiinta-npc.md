# Simulare de conștiință pentru NPC-uri: momentul potrivit și etape

Data: 2026-10-07
Actualizat: 2026-10-25.

Statut: planificare pentru circuitul cognitiv complet; autoevaluarea, experiența limitată de îngrijire, monitorizarea obiectivelor de îngrijire și cererile autonome către ajutori confirmați sunt implementate în cod, fără validare în serverul Minecraft.

## Primul nucleu implementat — 2026-10-24

`NpcCognitiveState` oferă o reprezentare comună, imuabilă, a nevoilor, atenției și intenției curente. Contribuția nevoilor la alegerea acțiunilor din `DecisionEngine` și răspunsurile despre propria stare din `DialogueEngine` folosesc același model. NPC-ul poate răspunde local la „La ce te gândești?”, „De ce ai nevoie?” și „Ce vrei să faci?”, iar autoevaluarea intră și în contextul capturat pentru providerul AI.

Acest pas urmărește starea proprie și separă intenția de rezultat. Contractul curent este descris în [runtime-ul de simulare](../architecture/simulation-service.md).

## Circuit de experiență implementat — 2026-10-24

Masa și somnul acasă au acum un circuit limitat: acțiune aplicată → schimbare observată la următorul tick → memorie persistentă → ajustarea unei alegeri ulterioare în condiții echivalente. `NpcActionExperience` reține cel mult 32 de observații și limitează influența lor asupra scorului. Testele compară aceeași situație și același număr de selecție cu și fără experiență, pentru a separa efectul memoriei de variația aleatorie.

NPC-ul poate explica local ce a observat prin „Ce ai învățat?”. Învățarea din rezultatele propriilor acțiuni rămâne limitată la îngrijire; deplasările și învățarea generală din conversații nu sunt acoperite. Verificarea în serverul Minecraft rămâne necesară înainte de a considera validat comportamentul în joc.

## Ajutor alimentar și experiență socială — 2026-10-24

`Shift + click dreapta` cu un aliment acceptat în mâna principală poate transfera un obiect către un NPC înfometat. Numai consumul reușit și creșterea efectivă a sațietății produc o confirmare persistentă, legată de UUID-ul exact al celui care a ajutat. Un eșec de inventar este restaurat, fără amintire de ajutor.

La o nevoie ulterioară, experiența poate favoriza acțiuni conversaționale față de aceeași persoană și poate schimba răspunsul la „De ce ai nevoie?” într-o nouă cerere de hrană. Declarațiile din chat, reputația și simpla ținere a mâncării nu produc această dovadă. Confirmările pot susține și inițiativa autonomă limitată descrisă mai jos.

## Cereri autonome către ajutori confirmați — 2026-10-25

`NpcFoodRequestService` poate trimite o cerere privată celui mai apropiat ajutor confirmat eligibil din contextul actualizat. Inițiativa cere un `Villager` viu și spawnat, simulare `FULL`, profil `FULL_AI`, foame dominantă cu sațietate 0-34, siguranță de cel puțin 40 și stare `IDLE`, `WALKING`, `WAITING`, `RESTING` sau `CURIOUS`. Ajutorul trebuie dovedit pentru UUID-ul exact al destinatarului printr-o confirmare de cel mult șapte zile, care nu este datată în viitor.

Jucătorul este verificat online, conectat, viu, valid, în aceeași lume și în raza `min(npc.interaction_distance, 6)`. Sunt necesare vizibilitatea NPC-ului pentru jucător și linia de vedere a NPC-ului către acesta; invizibilitatea, `vanished` activ și modul spectator îl exclud. Un interlocutor prezent în context sau o sesiune activă a NPC-ului blochează inițiativa. Conversația activă a jucătorului, un input text GUI sau blueprint wizard îl exclud temporar.

Mesajul cere hrană și explică oferirea ei prin `Shift + click dreapta`, fără AI, fără a deschide o sesiune și fără modificări automate de stare, obiectiv, memorie sau inventar. NPC-ul continuă alegerea acțiunii obișnuite. `NpcFoodRequestThrottle` permite un mesaj la cinci minute per NPC și unul pe minut per jucător între toate NPC-urile; limitele se marchează numai după trimitere reușită. Starea păstrează numai UUID-uri și timestamp-uri, are expirare lazy și maximum 4096 de intrări per map, nu evacuează cooldown-uri valide și nu permite ocolirea lor prin întoarcerea ceasului. Este numai runtime și nu se resetează la invalidarea cache-ului de decizii.

`simulation.food_requests_enabled` și `simulation.food_help_enabled` sunt citite la fiecare încercare și sunt implicit `true`. Inițiativa periodică necesită și `features.simulation=true` împreună cu `simulation.enabled=true`; `simulation.enabled` rămâne implicit `false`. Contractul exact este în [runtime-ul de simulare](../architecture/simulation-service.md).

## Explicatia alegerii implementata - 2026-10-25

NPC-ul poate raspunde la `De ce ai ales asta?` sau `Ce ai decis?` folosind ultima alegere aplicata, cu factorii favorabili capturati atunci. Explicatia recunoaste variatia unei selectii ponderate si ramane distincta de intentia curenta si de reusita observata. Este temporara, expira dupa cinci minute si este invalidata de schimbarile de ciclu de viata. Intrebarile si previzualizarile nu creeaza decizii. Detaliile sunt in [runtime-ul de simulare](../architecture/simulation-service.md); validarea in Minecraft ramane de realizat.

## Raspunsul la initiativa alimentara - 2026-10-25

Cererea autonoma are acum o fereastra de raspuns de 45 de secunde pentru destinatarul exact. Un refuz direct amana solicitarile acelui NPC cu 15 minute, iar o promisiune cu 10 minute. NPC-ul respecta pauza si in dialogul despre nevoi; promisiunea nu este considerata ajutor primit. Conversatiile active, formularele si evenimentele anulabile isi pastreaza prioritatea. Preferintele sunt temporare, fara penalizari de relatie ori modificari de inventar. Detaliile si limitele sunt in [runtime-ul de simulare](../architecture/simulation-service.md).

## Progresul observat al ingrijirii - 2026-10-25

`NpcSelfCareGoal` urmareste acum obiectivul de hrana sau odihna pornit de o alegere aplicata: intoarcere intentionata acasa, prezenta confirmata acasa, refacere observata, incheiere ori intrerupere. Lipsa unei case confirmate este raportata separat. Intrebarile despre progres folosesc aceleasi date ca snapshot-ul pentru AI, fara sa execute actiuni. O nevoie refacuta printr-o schimbare externa poate incheia obiectivul, dar nu produce o explicatie cauzala inventata.

Monitorizarea este temporara si limitata la aceste doua nevoi. Functioneaza separat de invatare, fara sa adauge navigatie, adaptarea scorurilor sau planificare generala. Testele acopera progresul prin actualizarea reala a nevoilor, intreruperile si invalidarea din callback; verificarea sosirii si a comportamentului complet in serverul Minecraft ramane necesara. Contractul este in [runtime-ul de simulare](../architecture/simulation-service.md).

## Continuitate limitata a obiectivului - 2026-10-25

Obiectivul de ingrijire poate porni acum si din deplasarea acceptata ori sosirea verificata de executorul rutinei, nu doar din masa sau somnul aplicat de simulare. Cu `simulation.decision.goal_continuity_enabled=true` si ambele sisteme active, urmatoarea selectie pastreaza temporar revenirea deja angajata, in loc sa aleaga o activitate exploratorie incompatibila. Pericolul, conversatia, pauzele si override-urile nu sunt suprascrise. Feature-ul este implicit oprit.

Trei minute cumulate de observatii valide fara sosire sau refacere elibereaza aceasta prioritate. Reaplicarea aceleiasi intentii nu ascunde lipsa progresului. Autoevaluarea modifica astfel o alegere verificabila, fara sa declare o ruta imposibila sau sa inventeze cauza stagnarii. Miscarea ramane cea a rutinei existente; nu este un planificator general. Testele compara selectia cu si fara continuitate in aceleasi conditii si verifica intreruperile, cache-ul si snapshot-ul de comunicare. Verificarea runtime in Minecraft ramane necesara.

## Observabilitatea circuitului cognitiv - 2026-10-25

`/ainpc decision` expune read-only atentia, nevoile, intentia, ultima alegere aplicata, progresul observat si motivul eligibilitatii sau opririi continuitatii. Evaluarea este comuna diagnosticului si selectorului; simpla inspectie nu executa actiuni, nu creeaza memorie si nu prelungeste dovezile temporare. Acest pas permite verificarea circuitului existent inainte de extinderea comportamentului. [Runbook-ul](../operations/debugging-si-testare.md#smoke-test-cognitiv) separa observarea de scenariile care necesita activarea explicita a simularii si rutinei pe un server de test. Testele automate nu inlocuiesc aceasta verificare in Minecraft.

## Validare pe tick-ul public - 2026-10-25

Bucla publica are acum scenarii automate pentru manager, perceptie controlata, nevoi, decizie, aplicare, experienta si comunicarea rezultatului. Au fost reproduse si corectate doua cazuri in care disparitia entitatii permitea continuarea pe coordonatele memorate ori dupa invalidarea din actualizarea contextului. Tick-urile `FULL` si `LIGHT` se opresc inainte sa produca nevoi sau alegeri pentru o entitate indisponibila. Aceste teste nu valideaza fizica ori perceptia Paper; [smoke test-ul Minecraft](../operations/debugging-si-testare.md#smoke-test-cognitiv) ramane necesar.

## Aplicare intrerupta fara invatare falsa - 2026-10-25

Callback-urile schimbarii de stare nu mai pot transforma o alegere invalidata intr-o noua perioada de invatare si nici permite aplicarii vechi sa suprascrie intentia ori dovezile mai noi. Verificarea urmareste atat revizia ciclului de viata, cat si tranzitia exacta de stare, inclusiv revenirile la aceeasi valoare. Patru regresii prin tick-ul public separa refacerea efectiva a nevoii de creditul cauzal acordat unei alegeri valide; comportamentul fizic in Minecraft ramane de verificat separat.

## Context coerent dupa interactiuni - 2026-10-25

Proiectia de la finalul tick-ului nu mai restaureaza pozitia capturata inaintea callback-ului. Verifica disponibilitatea entitatii si citeste pozitia actuala, pastrand intentia autoritativa si observatia mai noua a unui tick reentrant. Trei regresii reproduse priveau teleportul, mutarea in timpul conversatiei si pierderea entitatii; un al patrulea scenariu protejeaza aplicarea normala. Textul pentru comunicare nu mai afirma prezenta acasa cand observatia controlata confirma atelierul. Nu se adauga scanari ale lumii, apeluri AI sau confirmari de navigatie reala.

## Adaptarea verificata prin bucla publica - 2026-10-25

Doua scenarii automate urmaresc acum masa aplicata, refacerea observata, memoria si o alegere ulterioara efectiv aplicata prin manager. In conditii echivalente si cu aceeasi aleatorietate, eliminarea memoriei conduce la somn, iar prezenta observatiei conduce la masa. Comparatia este repetata dupa serializarea JSON, cu controale pentru oprirea invatarii si schimbarea casei. Testele demonstreaza posibilitatea unei adaptari limitate, nu o schimbare garantata la fiecare alegere, persistenta completa intr-un restart sau experienta subiectiva. [Metoda](../operations/debugging-si-testare.md#comparatie-cauzala-a-alegerii) separa explicit aceasta verificare locala de efectele fizice in Minecraft.

## Indeplinirea verificabila a promisiunii - 2026-10-25

O livrare alimentara reusita incheie acum asteptarea unei promisiuni active numai pentru jucatorul care a promis, dupa consumul unitatii si inregistrarea dovezii. Declaratia, istoricul recitit, un omonim sau un transfer esuat nu primesc acest credit. Refuzul explicit si limitele cererilor autonome raman respectate. Scenariile automate leaga inventarul controlat, memoria sociala, initiativa, raspunsul si livrarea, fara sa confunde indeplinirea promisiunii cu satisfacerea completa a nevoii ori invatarea generala din conversatie. [Verificarea in Minecraft](../operations/debugging-si-testare.md#promisiunea-indeplinita-prin-ajutor) ramane necesara.

## Motivul asteptarii comunicat corect - 2026-10-25

Dialogul despre nevoi distinge acum refuzul de promisiunea acceptata, fara sa confunde timpul acordat unui jucator cu ajutor deja primit. Motivul apartine numai destinatarului exact si este capturat impreuna cu indicatorul pauzei in textul imuabil pentru AI; snapshot-urile vechi nu sunt rescrise dupa livrare sau expirare. Citirea nu prelungeste asteptarea, nu produce experienta si nu schimba conversatia comuna. Compatibilitatea semnaturilor existente este pastrata. [Runbook-ul](../operations/debugging-si-testare.md#explicarea-asteptarii-in-dialog) separa testarea locala de verificarea raspunsului efectiv in Minecraft.

## Verificare integrala inainte de runtime - 2026-10-25

Checkpoint-ul initial complet core/API trece 3.132 de teste, cu zero esecuri si doua omise, iar artefactul core este construit si inspectat. Nu extinde garantia testelor la comportamentul fizic sau raspunsul unui provider AI. Poarta ABI identifica separat doua clase deja modificate fata de baseline; la acest checkpoint metadatele de versionare nu sunt actualizate automat, iar release-ul ramane oprit. [Raportarea si comenzile](../operations/debugging-si-testare.md#verificare-integrala-si-artefact) separa explicit testele verzi de gate-ul nereusit si de smoke-ul Minecraft inca neexecutat.

## Deblocarea controlata a portii ABI - 2026-10-25

Revizuirea ulterioara a extensiilor `PermissionNode` si `WorldRegionInfo` duce la API 1.1.0 si baseline regenerat intentionat, cu constructorul anterior pastrat si teste de consum Java/Kotlin suplimentare. Verificarile complete core/API/addon medieval trec 3.156 de teste, cu zero esecuri si doua omise; ABI-ul si freeze-ul static trec fara avertismente. Versiunea distributiei ramane 1.0.0. [Checkpoint-ul operational](../operations/debugging-si-testare.md#revizuire-abi-si-verificare-addon---2026-10-25) pastreaza distinct dovezile noi de raportul initial. Nu a fost pornit un server sau instalat un plugin: validarea circuitului cognitiv in Minecraft ramane pasul urmator, nu o garantie dedusa din testele locale.

## Dovezi alimentare vizibile fara modificarea simularii - 2026-10-25

Diagnosticul administrativ `decision` separa acum confirmarile alimentare pastrate de nevoile curente si de invatarea autonoma. Ultima confirmare are ID, timestamp, UUID-ul ajutorului si efectul istoric; pauza `PROMISE` sau `DECLINE` apartine numai administratorului care citeste ca jucator. Consola nu presupune identitatea altuia. Citirea nu consuma alimente, nu creeaza dovezi, nu prelungeste ori elimina pauze si nu executa decizii. [Pasii de observare](../operations/debugging-si-testare.md#livrari-si-pauze-observabile) pregatesc verificarea circuitului in joc; nu inlocuiesc smoke-ul Minecraft.

## Concluzie

O variantă simplă poate fi abordată gradual. Un sistem amplu de „conștiință simulată” ar fi prematur înainte de validarea circuitului dintre percepție, acțiuni, consecințe și memorie.

Prin acest termen înțelegem simularea unor funcții cognitive: urmărirea stării proprii, memorarea experiențelor, autoevaluare și adaptarea deciziilor. Aceste comportamente nu demonstrează existența unei experiențe subiective.

## Mediul necesar

Prioritatea este un mediu coerent, nu unul foarte complex. NPC-ul trebuie să poată observa o situație, să acționeze și să perceapă rezultatul acțiunii. Un scenariu restrâns este suficient pentru un prim prototip.

Scenariu de verificare în joc: NPC-ului îi este foame → un jucător îi oferă efectiv un aliment → reține confirmarea → la o foame ulterioară dominantă, cere din nou ajutor aceleiași persoane dacă este aproape, vizibilă și disponibilă.

Pașii sunt implementați în limitele de mai sus; scenariul complet rămâne de verificat în serverul Minecraft.

## Puncte de plecare în proiect

`NpcCognitiveState`, `NpcActionExperience`, `NpcSocialExperience` și `NpcFoodRequestService` leagă deja starea proprie, rezultatele măsurate, ajutorul confirmat și comunicarea în scenariile descrise. `MemoryManager` și `EnvironmentEngine` rămân relevante pentru extinderi, dar nu conferă automat învățare generală din dialog sau un circuit cognitiv complet.

Documentație conexă:

- [Contextul mediului](../architecture/environment-context-si-engine.md)
- [Comportament natural, rutine și alocări](../architecture/comportament-natural-npc-rutine-alocari.md)
- [Simularea satului și lumii](../architecture/simulare-sat-si-lume.md)

## Etape recomandate

1. **Validarea circuitului implementat:** percepție → nevoie → decizie → acțiune → rezultat → memorie. Verifică în server îngrijirea și ajutorul alimentar, inclusiv efectul experienței asupra unei decizii sau cereri ulterioare.
2. **Autoevaluare limitată:** urmărește ce încerca NPC-ul, dacă a reușit și ce trebuie schimbat. Autoevaluarea trebuie să influențeze o alegere observabilă.
3. **Extindere graduală:** adaugă obiective concurente, relații și reflecție asupra experiențelor după ce circuitul simplu funcționează.

Monologul interior permanent și reflecțiile LLM frecvente se amână până când pot modifica efectiv acțiunile. Dialogul convingător, singur, nu validează adaptarea comportamentală.

## Criteriul de trecere la etapa următoare

Întrebarea de verificare este: „Poate NPC-ul să aleagă diferit data viitoare din cauza unei experiențe anterioare?”

Pentru prototip, urmărește observația inițială, acțiunea executată, rezultatul, memoria creată și decizia ulterioară. Compară situații echivalente cu și fără experiența memorată, pentru a distinge efectul memoriei de variația aleatorie a răspunsului.

Extinderea este justificată când schimbarea deciziei poate fi explicată prin experiența anterioară și produce un efect în lume. Această notă nu stabilește un calendar de implementare și nu înlocuiește verificarea runtime.
