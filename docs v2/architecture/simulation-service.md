# Runtime de simulare NPC

Status: contract verificat in cod.
Actualizat: 2026-10-25.

Nu exista o clasa de productie numita `SimulationService`. Numele fisierului ramane pentru compatibilitate, iar implementarea reala este distribuita intre doua fluxuri independente: simularea nevoilor si rutina cu miscare.

## Fluxul de simulare

`SchedulerCoordinator -> NPCManager.runLifeSimulationTick() -> DecisionEngine.runLifeSimulationTick()`:

- proceseaza numai NPC-urile spawnate;
- actualizeaza `NPCContext` din lume si activitatea planificata;
- ajusteaza hunger, energy, social, comfort si safety dupa timpul real scurs, limitand rezultatele la 0-100; masa simulata acasa poate recupera satietatea la tick-ul urmator;
- poate trimite o cerere privata de hrana unui ajutor confirmat din apropiere, dupa sincronizarea nevoilor si inainte de alegerea actiunii obisnuite;
- calculeaza scoruri contextuale si alege ponderat dintre actiunile apropiate ca scor, cu alegere determinista pentru pericol, combat si conversatie; actualizeaza fortat starea prin `changeStateFromSimulation` si apoi `currentGoal`;
- publica `AINPCAddon.onNpcStateChange` numai daca starea rezultata este diferita;
- nu muta entitatea si nu scrie quest, story sau economie.

## Autoevaluare comuna pentru decizie si dialog

`NpcCognitiveState` este un instantaneu imuabil al nevoilor, starii, pericolului, interlocutorului, obiectivului si rutinei. `DecisionEngine` foloseste `needModifier` din acest model pentru contributia nevoilor la scorurile actiunilor; coeficientii si pragurile existente sunt pastrate.

Modelul identifica o prioritate de atentie si formuleaza o descriere a nevoilor si intentiei curente. Pericolul imediat are prioritate. Foamea si oboseala respecta pragurile de inceput si recuperare ale ingrijirii. Intentia ramane separata de actiunea executata: descrierea nu confirma consumul de obiecte, deplasarea sau reusita unui obiectiv.

`AINPC.generateContextDescription()` include aceasta autoevaluare in textul copiat de `OpenAIPromptSnapshotFactory`, iar `NpcCognitiveResponse` raspunde local la intrebari despre ganduri, nevoi si intentii prin `DialogueEngine`. Captura citeste numai campuri deja disponibile; nu executa actiuni si nu acceseaza lumea prin API Bukkit.

## Explicatia ultimei decizii

`NpcDecisionExplanation` pastreaza temporar actiunea selectata si cel mult doua contributii pozitive la scorul ei, ordonate descrescator: nevoi, context, rutina, personalitate, emotii, relatia cu interlocutorul, experienta de ingrijire sau ajutor alimentar confirmat. Factorii provin din calculul efectiv al alegerii si nu sunt recalculati din starea prezenta. Scorul de baza si contributiile negative nu sunt prezentate ca motive favorabile. Explicatia este partiala; nu reproduce toti multiplicatorii, cooldown-urile si regulile de eligibilitate.

Selectia retine si faptul ca exista mai multe optiuni cu probabilitate pozitiva, daca alegerea a fost ponderata. Descrierea mentioneaza atunci ca alegerea putea varia, fara sa pretinda ca actiunea aleasa avea neaparat cel mai mare scor. `AINPC.lastDecision` este actualizat numai la aplicarea rezultatului corespunzator in simularea `FULL`; previzualizarea, diagnosticul si o intrebare nu creeaza aceasta amintire. Descrierea raporteaza intentia ultimei alegeri, fara sa confirme reusita actiunii.

Explicatia expira dupa cinci minute, ignora timestamp-uri viitoare si nu este persistata. Spawn-ul, reatasarea, despawn-ul, indisponibilitatea entitatii, teleportul, restaurarea starii si schimbarea modului de simulare o invalideaza impreuna cu revizia selectiei. Verificarea reviziei previne restaurarea unei explicatii invalidate inclusiv dintr-un callback al schimbarii de stare. Conversatia poate pastra explicatia istorica, iar invalidarea cache-ului de decizii nu ii reinnoieste termenul.

`De ce ai ales asta?`, `De ce faci asta?`, `Cum ai luat decizia?` si `Ce ai decis?` primesc explicatia prin `NpcCognitiveResponse`; fara o inregistrare recenta, raspunsul recunoaste lipsa ei. `NpcCognitiveState` si snapshot-ul pentru AI captureaza aceeasi descriere, separat de intentia curenta si de rezultatele observate. Referirea la un interlocutor anterior nu confirma ajutorul jucatorului care intreaba acum.

## Progresul obiectivelor de ingrijire

`NpcSelfCareGoal` urmareste un singur obiectiv temporar de hrana (`EAT`) sau odihna (`SLEEP`), pornit numai dupa aplicarea unei alegeri compatibile in simularea `FULL`, cat timp nevoia este sub pragul de recuperare 60. Previziunile si intrebarile nu pornesc sau avanseaza obiectivul. Monitorizarea nu adauga un planificator general, pathfinding, miscari sau influente asupra scorurilor.

Etapele disting intentia de intoarcere acasa, prezenta observata acasa pentru refacere, lipsa unei case confirmate, incheierea si intreruperea. Intoarcerea aleasa nu confirma plecarea, existenta unei rute sau sosirea. Sosirea singura nu confirma refacerea: aceasta este observata strict in jurul actualizarii reale a nevoilor. Monitorizarea functioneaza si cu `simulation.decision.experience_enabled=false`, separat de invatare.

Obiectivul se incheie numai dupa o actualizare observata cu nevoia de cel putin 60. O schimbare externa a nevoii poate satisface obiectivul, dar nu este atribuita automat planului. Descrierea confirma o imbunatatire in timpul ingrijirii de acasa numai daca ultima actualizare care incheie obiectivul a observat un castig in starea potrivita; altfel declara cauza neconfirmata. Nu confirma consumul unui obiect, ajutorul jucatorului sau o recompensa de quest. Experienta cauzala ramane responsabilitatea separata a `NpcActionExperience`.

Observarea cere un interval pozitiv de cel mult 120 de secunde, timestamp-uri valide, aceeasi casa si un context sigur. Conversatia, combatul, o activitate incompatibila, schimbarea casei sau pericolul intrerup obiectivul activ fara sa il declare reusit. Spawn-ul, reatasarea, despawn-ul, indisponibilitatea, teleportul, restaurarea starii si schimbarea modului il sterg; verificarea reviziei previne recrearea lui dintr-un callback invalidat. Lipsa unei locatii si actualizarea de baza `LIGHT` il sterg de asemenea.

Este pastrat numai ultimul obiectiv, fara persistenta, cu snapshot imuabil de cel mult cinci minute de la ultima actualizare valida; citirea nu reinnoieste termenul. `NpcCognitiveState`, raspunsurile locale despre progres si textul `Progresul ingrijirii` capturat pentru AI folosesc aceleasi date, fara acces Bukkit asincron.

## Experienta actiunilor de ingrijire

`NpcActionExperience` leaga o masa sau un somn aplicat acasa de schimbarea nevoii observata la urmatorul tick. Observatia este capturata strict in jurul actualizarii nevoilor, inainte de alegerea noii actiuni. O simpla selectie sau previzualizare nu creeaza experienta.

O perioada este acceptata o singura data daca actiunea si starea corespund, nevoia initiala nu a fost schimbata din exterior, casa/lumea si revizia starii au ramas aceleasi, contextul este sigur, iar intervalul este pozitiv si nu depaseste 120 de secunde. Intreruperile de stare, despawn-ul, reatasarea, teleportul si schimbarea modului de simulare anuleaza perioada in asteptare. Actualizarea `LIGHT` si dezactivarea invatarii o anuleaza de asemenea. Perioadele in asteptare nu sunt persistate.

Efectul somnului scade recuperarea ambientala acordata oricarui NPC aflat acasa. `REST` nu primeste experienta, deoarece acea recuperare nu permite distingerea odihnei de simpla prezenta acasa. Efectele sunt normalizate la intervalul simularii. Sunt retinute cel mult 32 de observatii, cu snapshot sincronizat pentru autosave; numai cele din ultimele sapte zile si care nu sunt datate in viitor influenteaza scorurile.

Experienta ajusteaza cu cel mult 12 puncte in plus sau in minus scorul aceleiasi actiuni, la aceeasi casa, cand nevoia este din nou activa si contextul este sigur. Nu modifica eligibilitatea actiunilor sau prioritatea pericolului. Contributia este separata de memoria relatiilor in diagnosticul `/ainpc decision`. `simulation.decision.experience_enabled`, implicit `true`, controleaza invatarea si aplicarea bonusului, fara stergerea observatiilor deja pastrate.

Istoricul este salvat in `npc_profiles.profile_data`, sub `simulation.action_experience`, cu schema versionata; profilele vechi raman compatibile. `Ce ai invatat?` poate primi un raspuns local despre ultima schimbare observata, iar aceeasi descriere intra detasata in contextul AI. Acest circuit masoara efecte ale simularii nevoilor: nu confirma consumul de obiecte, ajutorul unui anumit jucator sau invatarea generala din povesti si conversatii.

## Ajutor alimentar si experienta sociala

Un jucator poate oferi un singur aliment din mana principala prin `Shift + click dreapta` pe un NPC apropiat. `simulation.food_help_enabled`, implicit `true`, controleaza gestul. Clickul obisnuit pastreaza dialogul si interactiunea de quest; simpla prezenta a mancarii in mana nu este un transfer.

`NPCInteractionListener` verifica disponibilitatea, lumea si distanta, respecta evenimentele deja anulate si omite mana secundara. `NpcFoodInteraction` revalideaza slotul, cantitatea si identitatea obiectului inainte de consum. `NpcFoodAid` accepta numai NPC-uri cu mod `FULL`, satietate sub 35 si stare sigura care permite interactiunea. Nu cere ca simularea periodica sa fie pornita; un NPC cu nevoia deja satisfacuta refuza fara sa consume obiectul. Toate verificarile si mutatiile inventarului ruleaza sincron pe threadul principal.

| Aliment | Recuperare satietate NPC |
|---|---:|
| `minecraft:bread` | 20 |
| `minecraft:apple` | 12 |
| `minecraft:baked_potato` | 20 |
| `minecraft:cooked_beef` | 30 |

Valorile sunt reguli ale simularii NPC, nu valori de nutritie vanilla. Transferul reusit scade exact un obiect, actualizeaza nevoia autoritativa si copia din context si creeaza o confirmare cu UUID-ul jucatorului, obiectul si schimbarea aplicata. Eroarea de transfer restaureaza slotul original si nevoile, fara confirmare de ajutor. Ajutorul extern anuleaza observatia autonoma in asteptare, pentru a nu atribui mancarea primita unei actiuni `EAT` anterioare.

`NpcSocialExperience` pastreaza cel mult 32 de confirmari persistente, cu schema versionata sub `simulation.social_experience`. Confirmarile recente de cel mult sapte zile pot adauga 5-8 puncte la `GREET`, `TALK` sau `THANK`, numai cand interlocutorul curent verificat este acelasi UUID, NPC-ului ii este din nou foame si contextul este sigur. Numele, reputatia, prietenia si declaratiile din chat nu dovedesc ajutorul. Bonusul este separat ca `ajutor` in `/ainpc decision` si respecta `simulation.decision.experience_enabled`; eligibilitatea actiunilor ramane neschimbata.

In dialog, `De ce ai nevoie?` poate include o noua cerere de hrana adresata jucatorului care a ajutat efectiv. Confirmarile brute ale tuturor jucatorilor sunt excluse din profilul trimis providerului AI; numai descrierea pentru UUID-ul cererii curente este capturata separat in prompt.

## Cereri autonome de hrana

`NpcFoodRequestService` poate initia cererea in tick-ul real de simulare, pe threadul principal. Citeste la fiecare incercare `simulation.food_requests_enabled` si `simulation.food_help_enabled`, ambele implicit `true`. Calea periodica necesita si taskul de simulare activ: `features.simulation=true` si `simulation.enabled=true`; ultimul este implicit `false`. Ajutorul manual ramane disponibil cand numai cererile autonome sunt oprite.

NPC-ul trebuie sa fie un `Villager` spawnat si viu, cu mod de simulare `FULL` si profil de interactiune `FULL_AI`. Foamea trebuie sa fie prioritatea dominanta, cu satietate intre 0 si 34, siguranta de cel putin 40 si una dintre starile `IDLE`, `WALKING`, `WAITING`, `RESTING` sau `CURIOUS`. Un interlocutor deja prezent in context ori o sesiune activa de conversatie a NPC-ului blocheaza initiativa.

Candidatii provin din jucatorii apropiati ai contextului actualizat. Fiecare trebuie sa aiba o confirmare de ajutor alimentar pentru UUID-ul sau exact, veche de cel mult sapte zile si nedatata in viitor. Sunt verificati din nou online, conectati, valizi, vii, in aceeasi lume, fara invizibilitate, mod spectator sau metadatum `vanished` activ. Jucatorul trebuie sa poata vedea entitatea NPC, iar NPC-ul sa aiba linie de vedere catre el. Raza efectiva este `min(npc.interaction_distance, 6)`; o valoare configurata nefinita sau nepozitiva opreste cererea. Conversatia activa a jucatorului, un input text GUI sau o sesiune de blueprint wizard il exclud temporar.

Candidatii sunt ordonati dupa distanta, apoi UUID pentru egalitati. Serviciul incearca cel mai apropiat candidat care poate primi mesajul si respecta limitele de trimitere. Mesajul este privat si direct, fara provider AI: aminteste ajutorul confirmat, cere din nou mancare si explica gestul `Shift + click dreapta` cu paine sau mar in mana principala. Nu presupune ca jucatorul are acum un aliment.

Cererea nu deschide o sesiune, nu schimba starea sau obiectivul NPC-ului si nu produce singura memorie, confirmare de ajutor ori transfer de inventar. Alegerea actiunii obisnuite continua in acelasi tick. O eroare runtime a acestei initiative optionale este izolata la integrarea din `DecisionEngine`, pentru a permite continuarea simularii.

`NpcFoodRequestThrottle` impune cinci minute intre mesajele aceluiasi NPC si un minut intre cererile primite de acelasi jucator de la orice NPC. Limitele se marcheaza numai dupa trimiterea reusita. Timestamp-urile viitoare raman blocate daca ceasul merge inapoi. Sunt pastrate numai UUID-uri si timestamp-uri, cu expirare lazy si maximum 4096 de intrari per map; la capacitate se refuza o cerere noua fara eliminarea unui cooldown valid. Aceasta stare este numai runtime, nu se persista si nu este resetata prin invalidarea cache-ului de decizii.

## Refuzul si promisiunea dupa o cerere de hrana

Dupa trimiterea reusita, `NpcFoodRequestFollowUps` pastreaza cel mult 45 de secunde o singura cerere per jucator. Inregistrarea contine UUID-urile participantilor, identitatea runtime si revizia NPC-ului, confirmarea alimentara existenta si momentul trimiterii. Sunt pastrate cel mult 4096 de cereri, fara referinte la obiectele Bukkit; o cerere activa nu este inlocuita pentru a elibera capacitate. Limita globala de un minut intre cererile primite de un jucator evita suprapunerea ferestrelor de raspuns.

In aceasta fereastra, replici directe precum `Nu pot acum` sau `Nu am mancare` amana cererile acelui NPC catre acel jucator pentru 15 minute. `Iti aduc mancare` sau `Vin cu mancare` le amana pentru 10 minute. Este acceptat si numele exact al NPC-ului urmat de virgula, de exemplu `Bob, nu pot acum`. Parserul este deliberat restrans: intrebarile, citatele, relatarile, promisiunile negate, alte nume si formularile neacceptate raman in dialogul obisnuit.

`NPCChatListener` incearca acest raspuns pe threadul principal, inaintea rutarii pasive, inclusiv cu `dialog.passive_listen_enabled=false`. Conversatiile active, inclusiv cu acelasi NPC, inputurile GUI, wizardul si clarificarile de adresare au prioritate. Mesajele deja anulate nu intra pe aceasta cale. Evenimentul anulabil `DialogMessageReceivedEvent` foloseste motivul `food_request_reply`; dupa callback se verifica din nou disponibilitatea, identitatea, revizia, foamea, siguranta, distanta, vizibilitatea si confirmarea alimentara. Un transfer de hrana intervenit intre cerere si raspuns invalideaza vechea cerere chiar daca NPC-ul mai este flamand. Iesirea jucatorului elimina cererea in asteptare.

`NpcFoodRequestPreferences` retine pauza numai pentru perechea NPC-jucator, cu maximum 32 de intrari per NPC si expirare fara reinnoire prin citire. Pauzele active nu sunt eliminate pentru a face loc altora; capacitatea plina blocheaza cereri noi pana la expirare. O eroare de trimitere a confirmarii verbale pastreaza refuzul deja acceptat. Nici refuzul, nici promisiunea nu modifica nevoile, reputatia, relatia, inventarul sau confirmarile de ajutor. O promisiune ramane o declaratie, iar ajutorul este confirmat doar prin transferul fizic reusit.

Pauza este respectata si de cererea locala inclusa in `De ce ai nevoie?`. Snapshot-ul pentru AI captureaza numai indicatorul `foodRequestDeferred` al jucatorului cererii, expus ca `npc_food_request_deferred`; instructiunea cere respectarea pauzei fara a transforma promisiunea in dovada de ajutor. Cererile in asteptare si preferintele sunt exclusiv runtime, nu sunt salvate in profil si se pierd la restart.

## Fluxul de rutina

`SchedulerCoordinator -> RoutineCoordinator.tick() -> RoutineService.runRoutineTick()`:

- proceseaza numai NPC-urile spawnate si sare peste starile ocupate; pentru NPC-uri in simulare completa, prioritizeaza intoarcerea la ancora casei cand foamea sau oboseala scade sub 35 si lasa masa sau somnul sa continue acasa pana cand nevoia ajunge la 60;
- in afara prioritatilor de ingrijire, rezolva profilul, fereastra orara, slotul si ancora prin `RoutineEngine`;
- actualizeaza activitatea, goal-ul si starea prin `AINPC.changeState` cu regulile de prioritate;
- incearca intai pathfinding Paper direct spre ancora, apoi teleport configurabil;
- emite `AINPCRoutineChangedEvent` cand activitatea se schimba.

Rutina este singurul dintre cele doua fluxuri care deplaseaza NPC-ul. Nu exista rutare explicita pe reteaua semantica de drumuri.

## Activare si intervale

- simularea cere simultan `features.simulation=true` si `simulation.enabled=true`;
- rutina cere simultan `features.routine=true` si `routine.enabled=true`;
- configuratia livrata are flagurile globale active, dar ambele flaguri locale sunt `false`;
- cererile autonome necesita in plus `simulation.food_requests_enabled=true` si `simulation.food_help_enabled=true`, citite live; ambele sunt livrate active;
- intervalul configurat este 30 secunde pentru simulare si 120 secunde pentru rutina;
- ambele taskuri ruleaza pe thread-ul principal;
- persistenta starii NPC ruleaza separat la 5 minute: sincronizare pe thread-ul principal, apoi salvare async.
- rehidratarea `currentState` din profil este silentioasa si nu simuleaza o tranzitie runtime.

## Limite confirmate

- nu exista sumar comun pentru cele doua fluxuri;
- simularea nu emite semnale regionale pentru quest sau story;
- navigatia spre casa pentru nevoi cere simultan simulare `FULL`, rutina activa si o ancora de casa; nu porneste in pericol, iar o pauza sau un override explicit al rutinei are prioritate;
- `RoutineService` respecta pauzele si override-urile din `RoutineCoordinator`; override-ul se aplica doar cat timp este valid si nu poate teleporta NPC-ul;
- `NpcSimulationMode.NONE` exclude NPC-ul, iar `LIGHT` actualizeaza doar nevoile; numai `FULL` intra in selectia actiunii, rutina si procesarea sociala;
- householdu-rile persistente nu sunt citite de motorul de rutina.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/bootstrap/SchedulerCoordinator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NPCManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/DecisionEngine.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/NpcFoodRequestService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/NpcFoodRequestThrottle.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineCoordinator.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/routine/RoutineSelfCarePolicy.kt`

## Legaturi

- `reference/simulation-stack.md`
- `architecture/probabilitati-actiuni-npc.md`
- `architecture/comportament-natural-npc-rutine-alocari.md`
- `planning/rutine-npc-si-timeline.md`
- `canonical/implementat-deja.md`
