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

Managerul verifica disponibilitatea reala prin `AINPC.isSpawned()`, nu doar flagul memorat `spawned`. Intrarea in motor si revenirea din actualizarea perceptiei sunt protejate de verificarea entitatii si a reviziei ciclului de viata, atat pentru `FULL`, cat si pentru `LIGHT`. Daca entitatea dispare sau un callback invalideaza revizia in timpul actualizarii contextului, tick-ul se opreste inainte de schimbarea nevoilor si de selectie. Coordonatele persistente ale unui NPC indisponibil nu devin dovada ca acesta continua sa actioneze.

Aplicarea verifica separat revenirea din callback-ul schimbarii de stare: revizia ciclului de viata trebuie pastrata, starea trebuie sa fie cea ceruta, iar revizia starii sa reflecte numai tranzitia asteptata. O trecere `FULL -> LIGHT -> FULL` sau `EATING -> TALKING -> EATING` invalideaza aplicarea veche chiar daca valoarea finala pare compatibila. Aceasta nu mai scrie intentia, explicatia, obiectivul de ingrijire sau perioada de invatare; nici nu sterge dovezile unei alegeri noi aplicate reentrant din callback. Nu se anuleaza retroactiv schimbarile de stare ori de nevoi deja efectuate.

Dupa aplicare, sincronizarea contextului verifica din nou disponibilitatea entitatii si foloseste pozitia ei curenta, nu captura de la intrarea in tick. Astfel, o mutare din callback sau un tick reentrant nu este urmata de restaurarea vechilor indicii `isAtHome`/`isAtWork`. Intentia este proiectata din starea autoritativa curenta; disparitia entitatii opreste sincronizarea fara a reintroduce dovezi de locatie deja sterse. Aceasta proiectie nu rescaneaza imprejurimile, nu actualizeaza perceptia completa a lumii si nu reaplica actiunea.

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

`NpcSelfCareGoal` urmareste un singur obiectiv temporar de hrana (`EAT`) sau odihna (`SLEEP`), pornit numai dupa aplicarea unei alegeri compatibile in simularea `FULL`, cat timp nevoia este sub pragul de recuperare 60. Si `RoutineActivityExecutor` poate porni obiectivul dupa deplasarea acceptata sau sosirea verificata la ancora casei pentru masa ori somn, cu starea compatibila si revizia neschimbata. O destinatie invalida, deplasarea refuzata, o simpla previzualizare sau o intrebare nu confirma un asemenea angajament. Monitorizarea nu adauga un planificator general, pathfinding, miscari sau influente asupra scorurilor.

Etapele disting intentia de intoarcere acasa, prezenta observata acasa pentru refacere, lipsa unei case confirmate, incheierea si intreruperea. Intoarcerea aleasa nu confirma plecarea, existenta unei rute sau sosirea. Sosirea singura nu confirma refacerea: aceasta este observata strict in jurul actualizarii reale a nevoilor. Monitorizarea functioneaza si cu `simulation.decision.experience_enabled=false`, separat de invatare.

Obiectivul se incheie numai dupa o actualizare observata cu nevoia de cel putin 60. O schimbare externa a nevoii poate satisface obiectivul, dar nu este atribuita automat planului. Descrierea confirma o imbunatatire in timpul ingrijirii de acasa numai daca ultima actualizare care incheie obiectivul a observat un castig in starea potrivita; altfel declara cauza neconfirmata. Nu confirma consumul unui obiect, ajutorul jucatorului sau o recompensa de quest. Experienta cauzala ramane responsabilitatea separata a `NpcActionExperience`.

Observarea cere un interval pozitiv de cel mult 120 de secunde, timestamp-uri valide, aceeasi casa si un context sigur. Conversatia, combatul, o activitate incompatibila, schimbarea casei sau pericolul intrerup obiectivul activ fara sa il declare reusit. Spawn-ul, reatasarea, despawn-ul, indisponibilitatea, teleportul, restaurarea starii si schimbarea modului il sterg; verificarea reviziei previne recrearea lui dintr-un callback invalidat. Lipsa unei locatii si actualizarea de baza `LIGHT` il sterg de asemenea.

Este pastrat numai ultimul obiectiv, fara persistenta, cu snapshot imuabil de cel mult cinci minute de la ultima actualizare valida; citirea nu reinnoieste termenul. `NpcCognitiveState`, raspunsurile locale despre progres si textul `Progresul ingrijirii` capturat pentru AI folosesc aceleasi date, fara acces Bukkit asincron.

## Continuitate optionala si lipsa progresului

`simulation.decision.goal_continuity_enabled`, implicit `false`, permite pastrarea revenirii acasa deja angajate pentru ingrijire. Necesita un NPC `FULL`, simularea si rutina pornite prin ambele perechi de flaguri globale/locale, aceeasi casa valida, nevoie inca nesatisfacuta, stare `WALKING`, siguranta si lipsa unui interlocutor activ. Pauzele si override-urile rutinei au prioritate. Cand conditiile sunt valabile, `WALK_TO_TARGET` este singurul candidat de continuitate; nu sunt modificate scorurile, iar `EAT`/`SLEEP` raman indisponibile departe de casa.

Aplicarea continua obiectivul original de hrana ori odihna fara sa confirme sosirea sau sa retrimita pathfinding. Miscarea ramane exclusiv in rutina. La sosire, ingrijirea obisnuita de acasa isi pastreaza regulile. Explicatia ultimei alegeri retine separat ca revenirea pentru obiectivul in curs a fost pastrata. Schimbarea eligibilitatii continuitatii invalideaza imediat reutilizarea deciziei din cache, inclusiv la oprirea feature-ului.

`withoutProgressMillis` cumuleaza numai timpul nou acoperit de actualizari valide ale nevoilor, dupa angajarea obiectivului. Sosirea confirmata sau un castig observat in timpul ingrijirii reseteaza acest contor. Timpul anterior angajarii, citirile, selectiile, reaplicarea aceleiasi intentii si observatiile duplicate nu adauga progres si nu reseteaza bugetul. Dupa trei minute cumulate fara sosire ori refacere confirmata, prioritatea revenirii este eliberata pentru urmatoarea alegere normala. Monitorul poate descrie lipsa progresului, dar nu afirma ca ruta este blocata sau ca ii cunoaste cauza.

Aceasta nu este o lista permanenta de destinatii interzise: dupa intrerupere, o noua deplasare efectiv acceptata poate porni alt obiectiv. Nu exista replanificare generala, navigatie suplimentara, modificari de inventar sau confirmari de ajutor. Activarea continuitatii nu porneste singura taskurile periodice si ramane experimentala pana la verificarea in serverul Minecraft.

## Diagnostic cognitiv read-only

`/ainpc decision [numeNpc|nearest]`, cu `ainpc.admin`, include acum atentia, nevoile numerice autoritative, intentia curenta, ultima decizie aplicata si progresul obiectivului de ingrijire. Daca exista o observatie recenta, afiseaza actiunea si etapa, nevoia initiala si ultima valoare observata, timpul cumulat fara progres confirmat si vechimea inregistrarii. Intentia, alegerea aplicata si rezultatul observat au etichete separate; candidatii previzualizati nu devin istoric de actiuni.

`NpcSelfCareContinuity` explica primul impediment din evaluarea folosita chiar de selector: modul nepotrivit, feature oprit, simulare sau rutina oprita, lipsa obiectivului recent, etapa incompatibila, lipsa progresului, casa neconfirmata ori schimbata, prezenta acasa, nesiguranta, interlocutor, stare incompatibila, nevoie invalida ori satisfacuta, pauza sau override. `READY` inseamna eligibilitate pentru continuarea revenirii, nu sosire garantata. Raportul nu enumera simultan toate impedimentele.

`DecisionEngine.cognitiveDiagnostic` captureaza un model imuabil, cu acelasi timestamp pentru experienta, decizia istorica si progres. Nu actualizeaza lumea sau contextul, nu alege o actiune, nu scrie in cache/statistici si nu reinnoieste observatiile. Citirea la un timestamp care ar expira o inregistrare nu o sterge din starea live. Textul narativ este limitat la 320 de caractere pe camp si redus la o singura linie; lipsa dovezilor este declarata explicit. Procedura de verificare este in [runbook-ul de testare](../operations/debugging-si-testare.md#smoke-test-cognitiv).

## Experienta actiunilor de ingrijire

`NpcActionExperience` leaga o masa sau un somn aplicat acasa de schimbarea nevoii observata la urmatorul tick. Observatia este capturata strict in jurul actualizarii nevoilor, inainte de alegerea noii actiuni. O simpla selectie sau previzualizare nu creeaza experienta.

O perioada este acceptata o singura data daca actiunea si starea corespund, nevoia initiala nu a fost schimbata din exterior, casa/lumea si revizia starii au ramas aceleasi, contextul este sigur, iar intervalul este pozitiv si nu depaseste 120 de secunde. Intreruperile de stare, despawn-ul, reatasarea, teleportul si schimbarea modului de simulare anuleaza perioada in asteptare. Actualizarea `LIGHT` si dezactivarea invatarii o anuleaza de asemenea. Perioadele in asteptare nu sunt persistate.

O intrerupere din callback-ul aplicarii nu poate rearma perioada anulata. Chiar daca masa simulata continua ulterior, invatarea necesita mai intai o noua alegere aplicata valid si abia apoi o actualizare observata a nevoilor.

Efectul somnului scade recuperarea ambientala acordata oricarui NPC aflat acasa. `REST` nu primeste experienta, deoarece acea recuperare nu permite distingerea odihnei de simpla prezenta acasa. Efectele sunt normalizate la intervalul simularii. Sunt retinute cel mult 32 de observatii, cu snapshot sincronizat pentru autosave; numai cele din ultimele sapte zile si care nu sunt datate in viitor influenteaza scorurile.

Experienta ajusteaza cu cel mult 12 puncte in plus sau in minus scorul aceleiasi actiuni, la aceeasi casa, cand nevoia este din nou activa si contextul este sigur. Nu modifica eligibilitatea actiunilor sau prioritatea pericolului. Contributia este separata de memoria relatiilor in diagnosticul `/ainpc decision`. `simulation.decision.experience_enabled`, implicit `true`, controleaza invatarea si aplicarea bonusului, fara stergerea observatiilor deja pastrate.

Istoricul este salvat in `npc_profiles.profile_data`, sub `simulation.action_experience`, cu schema versionata; profilele vechi raman compatibile. `Ce ai invatat?` poate primi un raspuns local despre ultima schimbare observata, iar aceeasi descriere intra detasata in contextul AI. Acest circuit masoara efecte ale simularii nevoilor: nu confirma consumul de obiecte, ajutorul unui anumit jucator sau invatarea generala din povesti si conversatii.

Testele prin tick-ul public verifica si influenta asupra unei alegeri aplicate ulterioare, nu doar diferenta de scor: o observatie reala despre masa poate schimba alegerea dintre somn si masa in conditii echivalente, cu acelasi seed si acelasi numar de selectie. Comparatia este repetata dupa serializarea JSON a memoriei. Oprirea invatarii si schimbarea casei elimina influenta, fara sa stearga istoricul. Acesta este un scenariu controlat care demonstreaza posibilitatea adaptarii, nu o garantie ca orice observatie schimba urmatoarea actiune; metoda si limitarile sunt in [runbook](../operations/debugging-si-testare.md#comparatie-cauzala-a-alegerii).

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

Dupa consumul reusit al unui aliment si inregistrarea confirmarii, `NpcFoodAid` incheie anticipat numai pauza unei promisiuni active a aceluiasi UUID, daca livrarea nu precede promisiunea. Refuzul explicit isi pastreaza pauza, chiar daca jucatorul ofera ulterior voluntar hrana. Un omonim, o confirmare veche recitita, un transfer repetat ori esuat nu indeplinesc promisiunea. Este confirmata livrarea alimentului, nu satisfacerea completa a nevoii, o recompensa sau o invatare autonoma. Cooldown-urile cererilor autonome raman neschimbate: indeplinirea promisiunii nu produce singura alt mesaj si nu permite solicitari repetate imediat.

Pauza este respectata si de cererea locala inclusa in `De ce ai nevoie?`. Pentru o nevoie alimentara activa si un context sigur, raspunsul local explica separat refuzul acceptat sau timpul lasat pentru mancarea promisa, numai jucatorului cererii. Lipsa unui motiv capturat nu este interpretata ca refuz, iar promisiunea nu devine confirmare de ajutor. Explicatia nu modifica starea, memoria, durata pauzei sau interlocutorul comun. Dupa livrarea confirmata ori expirare, o intrebare noua primeste situatia curenta; raspunsul la intrebarea initiata de jucator nu este un mesaj autonom.

Snapshot-ul pentru AI captureaza indicatorul `foodRequestDeferred`, expus ca `npc_food_request_deferred`, si motivul predefinit in textul `Pauza cererilor de hrana` din context. Ambele provin din aceeasi citire a pauzei pentru UUID-ul cererii, cu acelasi timestamp folosit pentru confirmarea alimentara. Textul este inghetat: o livrare ori expirare ulterioara nu modifica snapshot-ul vechi. Nu sunt trimise motivele altor jucatori sau referinte live la preferinte. Constructorul `PromptSnapshot` si metoda publica de raspuns cognitiv isi pastreaza semnaturile existente. Cererile in asteptare si preferintele sunt exclusiv runtime, nu sunt salvate in profil si se pierd la restart.

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

## Validarea buclei publice

`CognitiveLifeSimulationTickTest` parcurge `NPCManager.runLifeSimulationTick()` si motoarele reale, fara apelarea prin reflectie a pasilor privati de actualizare, selectie sau aplicare. Acopera revenirea angajata prin executorul rutinei, sosirea, recuperarea, incheierea, experienta si descrierea locala a rezultatului; include somnul, pericolul, modurile `NONE`/`LIGHT`, entitatea disparuta si invalidarea in timpul perceptiei.

Fixture-ul controleaza perceptia si pozitia, apoi foloseste sincronizarea si regulile reale de simulare. Constructorul public `DecisionEngine(AINPCPlugin)` pastreaza actualizarea Paper obisnuita; o intrare interna permite izolarea perceptiei in test fara a instala registre Paper globale. Constructia publica ramane disponibila si pentru Java. Nu este un test al fizicii, pathfinding-ului sau scanarii efective Bukkit si nu inlocuieste verificarea in serverul Minecraft.

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
