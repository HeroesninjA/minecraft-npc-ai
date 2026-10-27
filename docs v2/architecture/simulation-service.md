# Runtime de simulare NPC

Status: contract verificat in cod.
Actualizat: 2026-10-27.

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

## Restaurarea nevoilor din profil

La rehidratarea `simulation` din profil, `hunger_level`, `energy_level`, `social_need_level`, `comfort_level` si `safety_level` accepta numai valori JSON numerice, exact integrale, intre 0 si 100 inclusiv. Formele numerice echivalente, precum `20.0` sau `2e1`, raman acceptate; schema si scrierea profilului nu se modifica.

`readSimulationNeed` pastreaza separat valoarea anterioara a fiecarui camp daca acesta lipseste ori contine null, string, boolean, obiect, array, fractie sau numar in afara intervalului. Nu rotunjeste, nu trunchiaza, nu transforma un numar prea mare prin overflow si nu il limiteaza artificial la 0 sau 100. O valoare invalida nu opreste restaurarea celorlalte nevoi, a memoriei confirmate, a obiectivului sau a ancorelor valide. Cititorii generici ai celorlalte campuri nu sunt schimbati prin aceasta corectie.

Aceasta validare previne nevoi inventate din date invalide, dar nu demonstreaza ca o valoare numeric valida corespunde lumii reale. Perceptia, executia actiunilor si creditul pentru ajutor raman verificate separat; simpla incarcare a profilului nu consuma obiecte si nu produce o noua confirmare.

## Restaurarea timpului si intentiilor

`last_simulation_tick_at` accepta numai un numar JSON exact integral din intervalul 0..Long.MAX_VALUE. `readSimulationTimestamp` pastreaza valoarea anterioara pentru camp absent, tip gresit, fractie, valoare negativa sau overflow; nu accepta stringuri numerice. Zero ramane marcajul de initializare: prima actualizare observa timpul curent fara sa modifice nevoile pe baza unui interval inventat. Formele zecimale/exponentiale exact integrale si valorile mari reprezentabile se pastreaza fara pierdere de precizie.

`current_goal` si `planned_routine_activity` accepta numai stringuri JSON prin `readSimulationText`. Un numar sau boolean nu devine text de intentie; un camp invalid pastreaza numai textul anterior al acelui camp. Stringurile valide, inclusiv cel gol si spatiile, nu sunt normalizate prin aceasta corectie. Restaurarea memoriei si a ancorelor continua chiar daca timpul sau intentia este invalida.

Schema, scrierea profilului si cititorii generici raman neschimbati. Un timestamp exact dar viitor ramane reprezentabil si este pastrat la rehidratare; verificarea tipului nu dovedeste ca timpul este o observatie reala. La consumarea lui se aplica protectia de interval descrisa mai jos. Textele restaurate nu certifica aplicarea unei actiuni sau executia unei rutine.

## Intervale de simulare fara timp scurs

Actualizarea nevoilor captureaza o singura valoare de ceas si o salveaza in `lastSimulationTickAt` la aplicarea unui calcul inca valid. Daca timestamp-ul anterior este neinitializat sau noul timp este egal ori mai mic, cele cinci nevoi raman neschimbate. Limita minima de factor 0.5 se aplica numai dupa confirmarea unui interval strict pozitiv, nu unui apel duplicat in aceeasi milisecunda sau unui ceas regresiv. Protectia este comuna fluxurilor `FULL` si `LIGHT`.

Un timp viitor restaurat, inclusiv `Long.MAX_VALUE`, nu blocheaza permanent simularea: prima actualizare rebazeaza ceasul fara consum sau refacere de nevoi, iar urmatorul interval pozitiv foloseste normal noua baza. Factorii 0.5..4.0 si coeficientii intervalelor pozitive raman neschimbati; nu se introduce un ceas monotonic sau o politica noua pentru toate cooldown-urile.

La observarea ingrijirii, intervalul egal sau regresiv nu confirma progres, masa ori somn. Garzile existente anuleaza atribuirea in asteptare si obiectivul activ neconfirmat, fara a sterge experientele istorice. O noua aplicare valida, urmata de un interval pozitiv si o imbunatatire observata, poate invata din nou. Selectia obisnuita dintr-un tick `FULL` poate aplica in continuare o intentie noua; lipsa actualizarii nevoilor nu inseamna oprirea intregului tick.

Un furnizor intern `needsClock` permite regresii deterministe fara asteptari reale pentru aceasta etapa. Constructorii existenti raman disponibili si folosesc `System.currentTimeMillis`; diagnosticele, prospetimea memoriei si celelalte ceasuri nu sunt inlocuite prin acest punct de testare.

## Intervale explicite si aplicarea nevoilor - continuare AG3

[`NpcSimulationInterval`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/NpcSimulationInterval.kt) tipizeaza intervalul folosit de actualizarea existenta: `INITIALIZED`, `REBASED`, `UNCHANGED` sau `ADVANCED`. Pastreaza ambele timestamp-uri, timpul real pozitiv, durata normalizata a modelului, partea omisa de plafon si suplimentul minimului. Nu introduce un alt ceas ori un scheduler.

Pentru un interval pozitiv, durata modelului este limitata la `15_000..120_000` ms, echivalentul exact al factorului legacy `0.5..4.0`. Un interval real de `1` ms are supliment `14_999` ms; unul de `900_000` ms are partea omisa `780_000` ms. Se pastreaza identitatea `elapsedMillis + minimumUpliftMillis = modelDurationMillis + omittedMillis`, inclusiv pentru cel mai mare interval pozitiv reprezentabil. Durata modelului nu este prezentata drept timp fizic efectiv scurs. Partea omisa nu formeaza o coada de recuperare si nu este reaplicata la urmatorul apel.

[`NpcNeedsModel`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/NpcNeedsModel.kt) calculeaza toate cele cinci rezultate din valori imuabile: nevoi, stare, indicii de mediu deja cunoscute, interval si coeficienti. Nu citeste NPC-uri, configuratie, lume, baze de date sau LLM si nu publica evenimente ori memorie. Formulele, rotunjirea, pragurile `0..100`, masa abstracta de acasa si recuperarea ambientala sunt cele existente. Functia de compatibilitate `DecisionEngine.nextHungerLevel` delega aceluiasi model, nu mentine o a doua formula.

[`NpcNeedsInput` si `NpcNeedsTransition`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/NpcNeedsTransition.kt) separa captura, calculul si aplicarea. Captura contine UUID-ul persistent, identitatea runtime, reviziile de ciclu/stare, disponibilitatea memorata, modul autorului, starea, nevoile, indiciile scalare din context si intervalul. Este o proiectie a starii canonice, nu o noua sursa de adevar. Inainte de scriere se verifica din nou aceste valori si cursorul temporal. Un alt obiect cu acelasi UUID, o revizie schimbata, modificarea directa a unei nevoi sau consumarea aceluiasi interval invalideaza rezultatul vechi. Coordonatele, vremea ori prezenta acasa sunt indicii capturate, nu dovezi noi de actiune fizica.

Calculul complet preceda scrierea celor cinci nevoi si a timestamp-ului. O eroare de calcul nu mai consuma intervalul si nu lasa, de exemplu, satietatea/energia schimbate inaintea esecului urmatoarei nevoi. Nu se adauga retry automat; un apel ulterior valid poate consuma acel interval o singura data. Daca apare o actualizare reentranta mai noua, rezultatul vechi este abandonat, iar tick-ul sau se opreste inainte de observatori, planner, Utility si sincronizarea finala. Acest lucru este comun pentru `FULL`, `LIGHT` al autorului si `LIGHT` derivat prin LOD.

Observatorii ingrijirii si experientei folosesc timestamp-urile reale din acelasi interval aplicat, nu durata normalizata. Astfel, un gol real de `150` de secunde poate primi actualizarea limitata a nevoilor, dar nu devine artificial o observatie eligibila de `120` de secunde. Initializarea, egalitatea si rebazarea raman fara recuperare confirmata; istoria invatata nu este stearsa. Un rezultat calculat sau simpla aplicare a nevoilor nu confirma consum de obiecte, productie ori finalizarea unui plan.

Aplicarea ramane in apelurile sincrone existente pe threadul principal si nu confera autorizare unui rezultat LLM. Capturile nu reprezinta o tranzactie intre toate sursele lumii; un executor asincron viitor necesita si politici de prospetime/configuratie si proprietar al progresului. Calculul pur nu necesita flaguri, schema SQL/profil, entitati sau acces Bukkit suplimentar. Checkpoint-ul optional de mai jos adauga separat persistenta si o sectiune critica scurta pentru aplicarea nevoilor.

Acesta este un contract pregatitor AG3, nu simulatorul abstract. Cursorul existent `last_simulation_tick_at` si rebazarea timestamp-urilor viitoare/regresive raman compatibile. Checkpoint-ul urmator extinde evidenta logica a nevoilor, nu contabilitatea de inventar sau un handoff complet fizic/abstract. Modelul legacy cu minim de `0.5` nu trebuie confundat cu o integrare exacta a timpului unei regiuni neincarcate.

## Checkpoint persistent al nevoilor - continuare AG3

`simulation.progress.enabled`, implicit `false`, integreaza [`NpcSimulationProgress`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/NpcSimulationProgress.kt) in actualizarea nevoilor. Nu activeaza simularea, rutina sau LOD-ul. Cu optiunea oprita se pastreaza calculul temporal legacy; un checkpoint existent poate ramane ca istoric, fara avansarea lui.

Un checkpoint are UUID-ul NPC-ului, o generatie opaca UUID, o secventa logica nenegativa, proprietarul `PHYSICAL` sau `SUSPENDED` si ultima baza de ceas. Secventa este watermark-ul logic al pasilor acceptati, inclusiv stabilirea/rebazarea unei baze, nu numarul de actiuni reusite sau un maxim monoton al timpului UTC. Nu se calculeaza `sequence + 1` la `Long.MAX_VALUE`: admiterea ramane blocata, fara overflow ori reset implicit.

Prima activare stabileste o baza fara modificarea nevoilor, chiar daca profilul contine un timestamp anterior. In continuitate fizica, urmatorul interval pozitiv foloseste formulele existente si avanseaza o singura data secventa. O observatie repetata la acelasi timestamp nu adauga durata sau secventa. Un ceas regresiv rebazeaza fara recuperare si schimba generatia, pastrand secventa logica crescatoare. Aceasta nu dovedeste conservarea exacta a secundelor reale intre epoci de ceas diferite.

Invalidarile existente ale ciclului de viata suspenda checkpoint-ul si revoca propunerile pregatite: indisponibilitate/despawn, reatasare, teleport, restaurare de stare sau schimbarea modului autorului. Reatasarea controlata poate schimba UUID-ul entitatii; progresul este legat explicit de noul UUID, cu aceeasi secventa si o generatie noua, fara a accepta automat un profil al altui NPC. Urmatorul apel eligibil stabileste din nou baza. Astfel, timpul unei absente nu este transformat implicit in masa, somn sau experienta fizica. Niciun actor indisponibil nu primeste prin aceasta optiune dreptul de a rula simularea.

O propunere de avans (`Lease`) este tranzitorie, legata de obiectul runtime al progresului si de checkpoint-ul exact citit. Pregatirea si calculul nu modifica progresul. Numai aplicarea unui `NpcNeedsTransition` inca valid poate confirma propunerea impreuna cu cele cinci nevoi si timestamp-ul. Reentranta, schimbarea generatiilor sau consumarea de catre o alta propunere invalideaza rezultatul vechi. O exceptie ori oprirea optiunii in timpul calculului nu confirma checkpoint-ul si nu trece la un plan vechi. Nu exista retry automat, coada sau task nou.

Persistenta foloseste campul optional `simulation.progress` din JSON-ul profilului, prin [`NpcSimulationProgressCodec`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NpcSimulationProgressCodec.kt), versiunea `1`. Profilurile fara checkpoint raman valide si nu primesc campul pana la prima folosire. Citirea cere UUID-uri canonice, proprietar cunoscut si numere JSON exact integrale, nenegative, fara conversia stringurilor, fractiilor sau overflow-ului. Un camp invalid/strain este ignorat fara a bloca restaurarea celorlalte nevoi, memorii ori ancore. Importul unui checkpoint valid nu reduce secventa logica deja cunoscuta in acel obiect runtime.

Un checkpoint restaurat devine intotdeauna `SUSPENDED`, cu generatie noua, chiar daca profilul il declara `PHYSICAL`. Prima actualizare dupa incarcare stabileste o baza fara recuperarea timpului in care serverul a fost oprit. Nu este nevoie de un flag separat de catch-up: politica acestui increment este explicit **fara catch-up**. Cand optiunea este reactivata dupa actualizari legacy care au schimbat cursorul, nepotrivirea cere aceeasi stabilire a bazei. Reincarcarea profilului este testata cu adaptoare locale, nu printr-un restart Paper real.

Aplicarea modelului, legarea controlata la alt UUID si captura pentru scriere a identitatii/nevoilor/timestamp-ului/checkpoint-ului folosesc acelasi monitor per NPC. Sectiunea critica are numai verificari si copieri de date; serializarea JSON, SQL, I/O, configuratia, callback-urile si operatiile Bukkit nu ruleaza sub acest monitor. Aceasta evita o combinatie de nevoi vechi cu checkpoint nou sau a UUID-ului profilului cu checkpoint-ul altei identitati pentru aceste tranzitii. Nu este o tranzactie a intregului profil/rand DB, nu serializeaza toate mutatiile directe ale altor subsisteme si nu garanteaza ordinea salvarilor DB concurente sau durabilitatea la crash. Aceste contracte raman necesare inaintea unei contabilitati de resurse.

`/ainpc decision` arata numai starea capturata: activ/istoric, proprietar, revizie si baza temporala. Nu expune UUID-urile generatiei/runtime, nu consuma o propunere, nu initializeaza checkpoint-ul si nu confirma o actiune. Comanda ramane un diagnostic read-only.

Proprietarii de aici privesc numai progresul nevoilor. Nu exista executant `ABSTRACT`, lease distribuit intre servere sau registru tranzactional de resurse. `SUSPENDED` nu distinge singur un NPC mort de unul neincarcat; nu poate autoriza simulare abstracta. Clasificarea separata a absentei este descrisa mai jos. Urmeaza protocolul de transfer fizic/abstract, politica timpului offline, reconcilierea entitatii si contabilitatea bunurilor/banilor. Flagul ramane implicit oprit; validarea Paper si masuratorile de incarcare nu au fost efectuate.

## Clasificarea explicita a absentei - continuare AG3

[`NpcAbsenceRecord`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NpcAbsenceRecord.kt) retine numai UUID-ul actorului, motivul, timpul observatiei si provenienta runtime. Nu contine entitati, `World`, chunk-uri sau drepturi de executie. Lipsa unei inregistrari inseamna lipsa unei clasificari, nu dovada prezentei fizice.

| Motiv | Sursa | Restaurare automata |
|---|---|---|
| `UNAVAILABLE` | Calea generica existenta sau o atasare la o entitate invalida | Compatibilitatea existenta este pastrata |
| `UNLOADED` | `EntitiesUnloadEvent`, numai pentru referinta fizica actualmente atasata actorului inregistrat | Permisa; nu autorizeaza simulare abstracta |
| `DEAD` | `EntityDeathEvent` neanulat, pentru aceeasi referinta fizica inregistrata | Blocata |
| `EXPLICIT_DESPAWN` | Cererea explicita `AINPC.despawn()` | Blocata |

`Entity.isValid == false` nu este interpretat ca unload sau moarte. Listenerul existent preia lista `EntitiesUnloadEvent.entities`, inclusiv actorii gestionati care nu sunt villageri. Managerul face doar cautari in indexurile UUID existente si verifica atat identitatea UUID, cat si `npc.bukkitEntity === entity`. Nu enumera lumile, nu citeste `event.chunk.entities`, nu adopta NPC-uri prin PDC si nu incarca chunk-uri. Entitatile necunoscute si evenimentele intarziate pentru un obiect inlocuit sunt ignorate. Dupa detasare, un eveniment primit numai pe baza UUID-ului nu poate transforma o clasificare necunoscuta in dovada confirmata; sunt preferate rezultate conservative in locul atribuirii unui eveniment unei generatii noi.

La un unload confirmat se copiaza ultima pozitie scalara furnizata de entitate, se elimina asocierea din indexul fizic si se suspenda progresul. Nevoile, timpul deja confirmat si watermark-ul nu avanseaza. Propunerile pregatite anterior devin invalide. Nu se lanseaza un task DB pentru fiecare entitate descarcata: inregistrarea va fi persistata prin salvarile existente, deci poate fi pierduta la crash inaintea lor. Nu se introduce scheduler, worker, inferenta LLM sau simulare fara entitate.

Moartea confirmata este inregistrata **inainte** de publicarea evenimentului API existent, inclusiv pentru NPC-uri gestionate care nu sunt villageri. Un callback poate atasa explicit o noua entitate; continuarea vechiului eveniment nu o detaseaza. Persistenta asincrona existenta este pastrata. Evenimentele de moarte anulate nu produc clasificare sau publicare. Pentru despawn, detasarea logica are loc inainte de `Entity.remove()`, ca un callback reentrant sa nu suprascrie o atasare noua. `EXPLICIT_DESPAWN` descrie cererea logica, nu garanteaza ca eliminarea fizica a reusit daca API-ul arunca o exceptie.

`attachLoadedNPC`, folosit de restaurarea automata la chunk incarcat si de recuperarea periodica existenta, refuza `DEAD` si `EXPLICIT_DESPAWN` inaintea accesului la lume. Nu blocheaza spawn-ul explicit sau reatasarea intentionata la o entitate reala. O atasare valida elimina clasificarea, iar una invalida ramane `UNAVAILABLE`. O observatie generica repetata fara entitate nu sterge motivul anterior. Politica nu garanteaza detectarea mortilor produse cat timp pluginul nu observa evenimentele; `UNAVAILABLE` ramane compatibil cu restaurarea legacy.

Profilul foloseste campul optional `spawn_state.absence`, cu `version: 1`, `npc_uuid`, `reason` si `observed_at`. [`NpcAbsenceCodec`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NpcAbsenceCodec.kt) cere identitate canonica egala cu actorul, motiv cunoscut si timestamp JSON exact integral, pozitiv, fara overflow sau conversie din string. Inregistrarile invalide/straine sunt ignorate fara blocarea nevoilor, ancorelor ori checkpoint-ului. Provenienta nu este acceptata din JSON: restaurarea produce intotdeauna `RESTORED`, nu o observatie fizica proaspata. Profilurile vechi fara camp raman compatibile. `spawn_state.restorable` devine `false` pentru motivele terminale; verificarea efectiva a managerului nu se bazeaza doar pe acest boolean descriptiv.

Captura profilului include identitatea, flagul `spawned`, absenta si checkpoint-ul sub acelasi monitor per NPC, fara operatii Bukkit sau I/O sub monitor. Nu este o tranzactie a coordonatelor lumii, a tuturor subsistemelor sau a randului DB. `/ainpc decision` afiseaza motivul, provenienta, timpul si politica de restaurare fara a modifica evidenta sau a expune UUID-uri interne. Nu apare o linie noua pentru un actor fara clasificare.

Evidenta de ciclu de viata nu are flag nou: nu activeaza simularea sau vreun flag existent. Toate optiunile AI avansate raman implicit oprite. `UNLOADED` este doar o observatie, nu permisiunea unui executant abstract; mai sunt necesare transferul autoritatii, reconcilierea, durata offline explicita si contabilitatea resurselor. Testele locale folosesc evenimente Paper construite cu proxy-uri, nu un server Paper real sau o proba de conservare a resurselor la unload/reload.

## Rezervarea revocabila a transferului de progres - continuare AG3

`simulation.handoff.enabled`, implicit `false`, pregateste transferul **numai al progresului nevoilor** dupa un unload confirmat. Necesita separat `simulation.progress.enabled: true`; nu il activeaza si nu porneste simularea, rutina sau un executant abstract. Cu flagul oprit, clasificarea absentei functioneaza in continuare. Nu exista coada, scheduler, worker, apel LLM sau task DB nou.

[`NpcSimulationHandoff`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/NpcSimulationHandoff.kt) poate rezerva un singur checkpoint suspendat daca sunt indeplinite simultan aceste conditii:

- Actorul este detasat: `spawned == false`, fara referinta fizica si cu modul autorului diferit de `NONE`.
- Absenta este `UNLOADED`, observata in acest runtime pentru acelasi UUID, nu importata din profil.
- Suspendarea a urmat unui checkpoint `PHYSICAL` confirmat in acelasi obiect runtime. Un checkpoint restaurat/suspendat, un actor fara prima actualizare fizica sau o a doua invalidare nu satisfac aceasta dovada.
- Cursorul nevoilor coincide cu baza checkpoint-ului, timpul unload-ului nu precede cursorul, iar secventa logica nu este epuizata.

Rezervarea ingheata identitatea actorului si a obiectului runtime, reviziile, modul/starea, cele cinci nevoi, cursorul si observatia imuabila a absentei. Nu copiaza `NPCContext`, presupuneri despre mediu, coordonate autorizate, inventar, bani sau obiecte Bukkit. O stare precum `SLEEPING` este doar o eticheta capturata, nu dovada unei actiuni dupa unload. Captura si admiterea folosesc monitorul existent per NPC, fara I/O ori callback-uri sub el.

Proprietarul checkpoint-ului ramane **`SUSPENDED`**, iar secventa, nevoile si timestamp-ul nu avanseaza. Rezervarea este o bariera temporara, **nu** atribuirea autoritatii `ABSTRACT`, confirmarea persistentei sau permisiunea de a executa un plan. Numai un candidat poate ocupa checkpoint-ul; cererile repetate nu il reinnoiesc. Calea fizica nu poate admite/consuma un lease in paralel. Si aplicarea legacy fara lease refuza checkpoint-ul rezervat, inclusiv daca flagurile sunt dezactivate intre timp.

Identitatea exacta a checkpoint-ului si un token opac runtime leaga rezervarea de obiectul de progres. Verificarea sursei detecteaza schimbari de UUID, obiect, revizie, stare, mod, nevoi, cursor sau observatie. O modificare directa a datelor este raportata ca rezervare invalida; citirea nu o elibereaza si nu acorda implicit dreptul altui executant. Reconcilierea explicita ramane necesara.

Suspendarea, restaurarea reusita, legarea la alt UUID si invalidarile existente ale ciclului de viata revoca rezervarea si propunerile fizice vechi. Revocarea functioneaza acum si pentru un lease pregatit **inaintea primului checkpoint**, cand compararea a doua valori `null` nu era suficienta. O restaurare/relegare respinsa nu invalideaza rezervarea curenta. Tokenurile nu sunt persistate sau expuse in diagnostic.

Reatasarea existenta revoca rezervarea inaintea noii asocieri fizice, pentru acelasi UUID sau unul nou. Prima actualizare restabileste baza conform politicii explicite `REBASE_WITHOUT_CATCH_UP`, fara recuperarea nevoilor ori experientei pentru absenta; actualizarile ulterioare continua normal. Dezactivarea flagului opreste rezervarile noi, dar nu elibereaza singura o rezervare existenta: este necesara invalidarea/reconcilierea/reatasarea.

Rezervarea si dovada tranzitorie a ultimului proprietar fizic **nu sunt serializate**. Profilul pastreaza numai campurile canonice existente. La restaurare progresul este suspendat, absenta este `RESTORED` si nu exista rezervare. Campurile JSON care pretind tokenuri sau proprietari abstracti nu pot crea autoritate runtime. `/ainpc decision` afiseaza read-only o rezervare valida/invalida, revizia, baza si politica offline, fara identitati interne sau efecte.

Acesta este contractul runtime de **pregatire si revocare**, nu handoff-ul complet fizic/abstract. Lipsesc confirmarea durabila, ordinea salvarilor, reconcilierea lumii si a efectelor in curs, contabilitatea resurselor si executantul abstract bounded. Nu exista un API de activare/confirmare a unui executant abstract. Politica reatasarii fara catch-up nu decide singura cum va fi simulata ulterior o regiune neincarcata. Testele cu proxy-uri verifica exclusivitatea si revenirea fizica, nu conservarea resurselor sau performanta Paper.

## Confirmarea SQL a profilului rezervat - continuare AG3

[`NpcProfileWriter`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NpcProfileWriter.kt) reutilizeaza tabelul `npc_profiles` si `DatabaseManager`. `NPCManager.saveProfile` trece prin acest adaptor pentru toate salvarile profilului. Protectia persistenta foloseste acum coloana `write_revision` din schema DB `2`, descrisa mai jos, in locul UPSERT-ului neconditionat. Nu exista tabel, flag, worker sau task nou la unload. Confirmarea foloseste salvarile deja solicitate de runtime; rezervarea nu programeaza singura I/O.

`captureProfileWrite` leaga payload-ul JSON de acelasi snapshot al nevoilor/timpului/checkpoint-ului/absentei folosit la serializare. ID-ul DB, UUID-ul, starea, modul de simulare si metadatele sursei profilului sunt capturate, nu recitite la bind SQL. Referinta rezervarii este numai metadatum runtime, nu camp JSON. Sursa rezervarii include acum si ID-ul DB, pentru ca schimbarea randului asociat sa invalideze dovada.

Toate salvarile profilului folosesc acum `DatabaseManager.executeTransaction` pentru revizia persistenta, chiar daca AI este oprit. Statement-urile sunt inchise inainte de `commit`. Confirmarea handoff-ului necesita suplimentar flagurile existente ale progresului/handoff-ului, rezervare actuala si revenirea conexiunii in autocommit. Inspectia conexiunii este sub lock-ul DB. Apelul dintr-o tranzactie deja activa este acum **refuzat fara scriere**, nu imbricat sau confirmat inaintea commit-ului exterior; aceasta schimbare evita publicarea unei revizii locale inca neconfirmate.

O cerere admisa obtine un token opac sub lock-ul tranzactiei DB si inainte de SQL. Acesta invalideaza confirmarea precedenta. Dupa commit, confirmarea cere revizie persistenta confirmata, acelasi token de scriere, aceeasi rezervare/checkpoint, acelasi ID DB si o sursa actuala. Cererile deja obsolete in propriul obiect runtime sunt refuzate inainte de SQL, fara retragerea confirmarii mai noi. Cache-ul verifica in continuare tokenul si identitatea randului. Monitoarele NPC protejeaza numai copieri/verificari, nu I/O.

O scriere SQL esuata, un commit cu eroare sau rezultat ambiguu, inchiderea esuata a statement-ului, lipsa conexiunii ori un conflict de revizie nu produc confirmare noua. O incercare admisa poate invalida conservativ confirmarea veche chiar daca DB a pastrat continutul. Rezultatul boolean este acum `false` pentru conflict, cerere veche, identitate schimbata, revizie blocata sau tranzactie exterioara; erorile SQL sunt tratate de managerul existent. Nu se introduc retry-uri automate sau recuperarea nevoilor offline.

Aceasta este o **confirmare runtime a salvarii profilului**, nu proprietate abstracta durabila. Incrementul de revizie de mai jos impiedica acum suprascrierea unei versiuni SQL mai noi de catre o cerere participanta veche, inclusiv din alt obiect/conexiune. Nu este insa o tranzactie a tuturor tabelelor NPC sau a resurselor si nu protejeaza impotriva unui writer care ocoleste protocolul. Aceste limite raman relevante inaintea executiei abstracte.

Confirmarea nu este serializata, nu este restaurata din JSON si este revocata impreuna cu rezervarea. Checkpoint-ul ramane `SUSPENDED`, fara actiuni sau increment de timp. `/ainpc decision` distinge profil confirmat prin commit de profil neconfirmat, fara a repara date, a repeta I/O sau a conferi autoritate. Snapshot-urile diagnostice existente raman imuabile.

Durabilitatea este cea configurata de backend. SQLite foloseste deja WAL cu `synchronous=NORMAL`: un commit poate fi pierdut la caderea sistemului/alimentarii; nu se pretinde garantie power-loss, backup sau exactly-once. Aceasta limita este precizata de [documentatia SQLite](https://www.sqlite.org/pragma.html#pragma_synchronous). Nu s-au schimbat PRAGMA-urile pentru a obtine confirmarea. Testele cu fisiere temporare verifica vizibilitatea dintr-o a doua conexiune si fault injection JDBC; nu sunt teste MySQL real, restart Paper sau conservare a resurselor. Detalii comune: [storage runtime](../reference/storage-runtime.md#confirmarea-profilului-pentru-rezervarea-progresului).

## Revizii persistente si ordonarea profilului - continuare AG3

Schema DB este acum **versiunea 2**. `npc_profiles.write_revision` este `BIGINT NOT NULL DEFAULT 0`, distinct de versiunea formatului profilului si de watermark-ul nevoilor. Migrarea compatibila adauga coloana la baze v1 sau fara registru de versiune, fara rescrierea JSON-ului; tabelele noi o contin de la creare. Compatibilitatea registrului `schema_version.updated_at` este pastrata. Este necesar backup inaintea primei porniri cu acest cod; [procedura de migrare](../reference/storage-runtime.md#revizii-persistente-ale-profilului) descrie aplicarea si rollback-ul. In aceasta sesiune s-au folosit numai baze de test.

[`NpcProfileRevision`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/NpcProfileRevision.kt) retine baza SQL cunoscuta, un epoch opac runtime si starea `READY`, `WRITING`, `CONFLICT`, `UNCERTAIN`, `INVALID` sau `EXHAUSTED`. Loaderul comun pentru NPC-uri si recuperarea actorilor citeste revizia din aceeasi interogare ca profilul, nu din JSON. Profilurile legacy pornesc de la `0`; prima scriere acceptata produce `1`. Capturarea unei cereri nu incrementeaza revizia.

Fiecare payload capturat are un ticket pentru ID-ul DB, epoch si revizia asteptata. Numai un ticket poate fi in scriere in acel obiect; capturile in timpul publicarii unui commit sunt refuzate, fara coada. Cererile cu ID/UUID schimbat sau epoch vechi nu ajung la SQL. In tranzactie, writerul face `UPDATE ... WHERE npc_id = ? AND write_revision = ?`, atribuind explicit urmatoarea valoare `Long`, nu aritmetica SQL care ar putea depasi domeniul. Numai un rand modificat confirma succesul. La revizia initiala `0`, un rand realmente absent poate fi inserat; un profil sters cu revizie cunoscuta pozitiva nu este recreat automat.

CAS-ul este o politica **primul commit valid castiga**, nu „ultima captura castiga”. Doi candidati cu aceeasi baza nu sunt reexecutati automat pe revizia castigatoare. Dupa succes, baza locala avanseaza si epoch-ul se schimba. Un ticket vechi al aceluiasi actor este refuzat fara a bloca baza sa mai noua. Un alt actor/conexiune cu baza ramasa in urma primeste `CONFLICT`; nu adopta orbeste revizia DB si nu poate resalva starea sa veche. Reconcilierea trebuie sa verifice **datele si revizia** impreuna, nu doar sa schimbe contorul. Comanda limitata descrisa mai jos nu ofera o optiune de fortare; restaurarea nevoilor are un contract separat, strict.

Dupa o eroare SQL se permite o cerere noua numai daca inspectia gestionata confirma ca stocarea a ramas la baza asteptata, ori lipseste inca randul initial. Ticket-ul esuat este revocat. Un commit ambiguu, o conexiune indisponibila sau o baza schimbata lasa actorul `UNCERTAIN`, fara replay automat. Reviziile DB invalide sunt blocate; valorile peste precizia `Double` raman exacte, iar `Long.MAX_VALUE` produce `EXHAUSTED`, nu overflow sau reset la zero.

Revizia persistenta nu este token de executie, generatie de entitate sau lease distribuit. Writerii participanti cu conexiuni diferite nu pot confirma aceeasi baza, dar SQL direct fara predicate, resetarea manuala/stergerea/recrearea profilului ori alte tabele nu sunt acoperite. Nu sunt introduse actiuni abstracte, I/O per tick/unload, scanari de lume sau flaguri noi. Fiecare salvare deja solicitata are un numar limitat de operatii, fara retry in bucla; costul suplimentar si backend-ul MySQL necesita masuratori reale.

`/ainpc decision` arata read-only baza SQL **cunoscuta** si starea, inclusiv cererea de reconciliere pentru conflict/incertitudine/date invalide. Nu citeste DB pentru a repara sau avansa baza. Confirmarea handoff-ului ramane dovada propriului commit, nu o citire permanent actualizata a DB sau un lease: o schimbare pe alta conexiune poate fi descoperita numai la o operatie ulterioara. Ea este separata si nerestaurabila; un profil protejat prin CAS nu activeaza simulatorul abstract.

## Reconciliere explicita a profilurilor identice

`/ainpc profile-reconcile <numeNpc> [preview|apply|restore-needs --confirm]` necesita `ainpc.admin`. Implicit este `preview`: nu modifica NPC-ul sau SQL. Se admit numai actori canonici persistati, fara entitate asociata si cu `spawned = false`, blocati in `CONFLICT` sau `UNCERTAIN`. Un singur request este admis pentru intregul manager; repetarea comenzii nu creeaza o coada noua. Nu exista reconciliere automata per tick/unload, flag, schema sau executor nou.

Captura de pe threadul principal foloseste acelasi serializer ca writerul. Executorul DB existent primeste numai cererea imuabila si face un singur `SELECT` gestionat: UUID-ul din `npcs`, profilul complet, metadatele SQL si revizia. Revizia trebuie sa fie exacta, nenegativa, cel putin baza cunoscuta si sub `Long.MAX_VALUE`. UUID-ul si `npc_id` din JSON trebuie sa corespunda actorului. Se compara toate datele profilului si metadatele; numai `spawn_state.updated_at`, generat la serializare, este ignorat. Ordinea cheilor JSON si formatarea numerica echivalenta nu conteaza; ordinea listelor conteaza, iar intregii peste precizia `Double` sunt comparati exact.

In modul `apply`, date diferite, profil lipsa, identitate/revizie/metadate invalide inseamna refuz. Nu se hidrateaza partial actorul si nu se adopta doar revizia unui profil divergent. Acest mod rezolva numai profiluri deja identice, de exemplu un commit ambiguu cu aceleasi date; nu este mecanismul de import al castigatorului pentru orice conflict. Un checkpoint eligibil cu diferente limitate la nevoi/progres este doar semnalat, nu consumat de preview sau apply.

Rezultatul este programat prin scheduler pe threadul principal Paper. Inainte de `apply` se reverifica permisiunea, pluginul activ, modul MCP read-only, managerul si backend-ul DB curente, actorul canonic, identitatea runtime, epoch-ul blocat, reviziile locale, martorul runtime al progresului si intregul profil curent. Orice rezultat invechit este refuzat. Numai dupa aceste verificari baza SQL cunoscuta este adoptata, tokenurile vechi sunt revocate si lucrul local pregatit este invalidat. Nu se acorda o confirmare de commit sau autoritate abstracta noua; nevoile, cursorul temporal, secventa logica, memoria observata si pozitia nu sunt avansate. Generatia runtime a progresului este invalidata, fara catch-up. SQL si lumea nu sunt scrise.

Erorile SQL, respingerea executorului/schedulerului si refuzurile din callback elibereaza admiterea fara deblocarea profilului. Citirea SQL nu este un lease: un alt writer poate modifica profilul dupa inspectie; urmatoarea salvare ramane protejata de CAS si descopera conflictul. Nu sunt reconciliate tabelele NPC in ansamblu, entitatile/chunk-urile, inventarele, banii, efectele in curs sau autoritatea distribuita. Pentru celelalte date divergente este inca necesar un contract complet separat, nu resetarea manuala a reviziei.

## Recuperare explicita a nevoilor divergente

`/ainpc profile-reconcile <numeNpc> restore-needs --confirm` alege explicit datele SQL pentru un singur domeniu: **toate cele cinci nevoi, cursorul temporal si checkpoint-ul lor, impreuna cu baza reviziei profilului**. Lipsa confirmarii nu programeaza SQL. Modul respecta aceleasi verificari admin/read-only, admitere unica si revalidare pe threadul principal ca reconcilierea identica. `preview` si `apply` nu se transforma automat in restaurare, nici daca aceasta este eligibila.

`NpcProfileNeedsRecovery` evalueaza pur datele capturate, fara acces la actor pe worker. Sunt necesare toate conditiile:

- Absenta locala este `UNLOADED`, observata in runtime pentru acelasi UUID, nu doar restaurata din JSON. Un actor incarcat dupa restart nu obtine aceasta dovada prin simpla citire a profilului.
- Exista deja un checkpoint local coerent `SUSPENDED`; si cel SQL este `SUSPENDED`, schema `1`, cu UUID/generatie valide, numai campurile cunoscute si secventa pozitiva sub `Long.MAX_VALUE`. Lipsa checkpoint-ului nu este completata speculativ.
- Revizia SQL este strict mai noua decat baza locala, valida si neepuizata. Timestamp-ul fiecarui checkpoint este pozitiv si egal cu propriul `last_simulation_tick_at`. Nici timpul, nici secventa SQL nu pot regresa; timpul SQL nu poate depasi momentul unload-ului observat.
- Fiecare nevoie locala si SQL este un intreg exact intre `0` si `100`. Nu se aplica fallback, clamp sau conversie din siruri. Reviziile/secventele mari raman exacte.
- Dupa eliminarea stricta a celor cinci campuri de nevoi, `last_simulation_tick_at` si `simulation.progress`, **tot restul profilului si metadatele SQL sunt identice**. Pozitia, ancorele, personalitatea, starea, absenta, obiectivele, memoria si campurile necunoscute nu pot fi ascunse intr-o restaurare a nevoilor. Numai timestamp-ul de serializare ramane ignorat ca inainte.

Dupa inspectie, orice schimbare a sursei, actorului, managerului/backend-ului, epoch-ului sau profilului curent refuza rezultatul. Aplicarea foloseste monitoarele progresului si reviziei, fara I/O sub ele: serializerul comun nu poate captura nevoi noi cu baza veche sau invers. Lucrul pregatit, rezervarile si confirmarile vechi sunt revocate; checkpoint-ul se restaureaza cu generatie runtime noua si proprietar `SUSPENDED`. Prima reluare fizica stabileste baza fara catch-up. SQL, entitatile, memoria observata, obiectele si banii nu sunt modificate; importul nevoilor nu reprezinta consum sau recompensa noua.

Operatia importa un snapshot SQL verificat, nu garanteaza ca acesta ramane ultima stare distribuita dupa citire. O modificare concurenta ulterioara este in continuare detectata de CAS la salvarea urmatoare. Nu exista confirmare noua de handoff, transfer de autoritate abstracta, replay de actiuni sau activare implicita a flagurilor. Diferentele din celelalte domenii, efectele externe ambigue si reconcilierea completa a lumii/resurselor raman blocate pentru un contract separat.

## Autoevaluare comuna pentru decizie si dialog

`NpcCognitiveState` este un instantaneu imuabil al nevoilor, starii, pericolului, interlocutorului, obiectivului si rutinei. `DecisionEngine` foloseste `needModifier` din acest model pentru contributia nevoilor la scorurile actiunilor; coeficientii si pragurile existente sunt pastrate.

Cache-ul unei alegeri de cel mult o secunda verifica si nivelul de siguranta folosit la selectie. Daca acesta se schimba, urmatoarea selectie si previzualizare recalculeaza scorurile chiar daca starea NPC si indicatorul de pericol raman aceleasi. Simpla previzualizare nu creeaza o decizie sau o explicatie istorica.

Modelul identifica o prioritate de atentie si formuleaza o descriere a nevoilor si intentiei curente. Pericolul imediat are prioritate. Foamea si oboseala respecta pragurile de inceput si recuperare ale ingrijirii. Intentia ramane separata de actiunea executata: descrierea nu confirma consumul de obiecte, deplasarea sau reusita unui obiectiv.

`AINPC.generateContextDescription()` include aceasta autoevaluare in textul copiat de `OpenAIPromptSnapshotFactory`, iar `NpcCognitiveResponse` raspunde local la intrebari despre ganduri, nevoi si intentii prin `DialogueEngine`. Captura citeste numai campuri deja disponibile; nu executa actiuni si nu acceseaza lumea prin API Bukkit.

Toate tiparele cognitive folosesc acum textul direct extras de `DialogueMessageText.directIntentText`, inclusiv intrebarile despre nevoi, invatare si ajutor alimentar. Citatele si introducerile de relatare ori explicare recunoscute de acest extractor nu devin cereri adresate NPC-ului. Intr-un mesaj precum `Maria a zis „ce nevoi ai?”; dar ce vrei sa faci?`, resolverul cognitiv raspunde numai despre intentie; nu adauga o noua cerere de hrana din citat. O continuare directa ramane eligibila, cu ordinea raspunsurilor si dovezile capturate pastrate.

## Snapshot comun al agentului - primul increment AG1

`NpcAgentSnapshot` adauga un invelis imuabil, cu schema `2`, peste `NpcCognitiveState` si `NpcPerceptionSnapshot`: UUID persistent, identitatea instantei runtime, reviziile de decizie si tranzitie, momentul capturii, cognitia si perceptia. Schema initiala `1` acoperea numai cognitia. Captura reutilizeaza datele existente, fara acces nou la Bukkit, SQL, provider AI sau persistenta. Identitatea runtime distinge doua instante care au acelasi UUID persistent.

`DecisionEngine` reutilizeaza o singura captura cognitiva pentru scorurile unui set de actiuni. Eligibilitatea pentru masa/somn, contributiile nevoilor si rutinei si indiciul contextual de foame folosesc valorile autoritative din NPC, chiar daca o copie din `NPCContext` nu a fost sincronizata. Experienta de ingrijire, experienta sociala si verificarea recenta a interactiunii folosesc acelasi moment de captura. Previzualizarea expune snapshotul folosit, fara a aplica o alegere sau a rescrie contextul.

`OpenAIPromptSnapshotFactory` ataseaza aceeasi forma de snapshot in `PromptSnapshot.agentSnapshot`. Descrierea NPC, nevoile si intentia din descrierea contextului, starea/activitatea si campurile obiectiv/rutina ale promptului sunt proiectate din aceeasi cognitie capturata. O schimbare ulterioara a NPC nu rescrie acele valori. Metodele publice existente de descriere raman disponibile; constructorii Kotlin existenti ai `PromptSnapshot` pot omite noul camp optional.

`NpcPerceptionSnapshot` copiaza din `NPCContext` valorile deja disponibile: lumea raportata, timpul, vremea, biomul/topologia, starea fizica si pozitionarea semantica, contoarele de vecinatate, regiunea si contextul de mediu. Copia evenimentelor de mediu este defensiva si nemodificabila. Lista de jucatori este folosita numai pentru numar; snapshotul nu pastreaza entitati si nu le citeste API-urile. Nu se fac scanari sau incarcare de chunk-uri pentru aceasta proiectie.

Scorarea contextuala si eligibilitatea pentru ingrijire folosesc perceptia capturata. Fabrica promptului foloseste aceeasi perceptie pentru descrierea mediului, topologie, locatia descriptiva, regiune si contextul de mediu. Schimbarea contextului sursa dupa captura nu muta acele proiectii la alta locatie sau alta stare de sanatate.

Aceasta este o proiectie a cache-ului de perceptie, nu o observatie noua a lumii. `capturedAtMillis` indica momentul copierii, fara sa dovedeasca prospetimea tuturor observatiilor. Blocurile world/story si unele date sociale raman surse separate; captura nu transforma contextul administrativ global in cunoastere individuala a NPC-ului. Adaptorul memoriei selectate si limitele sale sunt descrise in [contractul dialogului](dialog-si-conversatii.md#memorie-contextuala-tipizata---continuare-ag1).

Snapshotul nu contine referinte la entitati, lume sau colectii runtime mutable. Nu reprezinta insa o tranzactie atomica intre profil, istoric, memorii, story si toate faptele lumii. Reviziile existente nu cresc la fiecare modificare directa a unei nevoi sau a textului obiectivului; ele nu inlocuiesc reverificarea preconditiilor unui viitor plan. Schema snapshotului nu este o versiune noua de DB sau de API pentru addonuri.

AG1 nu introduce actiuni autonome noi, AI LOD sau coordonare regionala/globala si nu activeaza flagurile simularii. Previzualizarea plannerului limitat AG2 este separata, conform sectiunii urmatoare. Pasii ulteriori sunt in [arhitectura agentica](../planning/arhitectura-agentica-local-regional-global.md).

## Planner local de ingrijire - previzualizare AG2

`simulation.decision.care_planner_shadow_enabled`, implicit `false`, adauga un diagnostic la `/ainpc decision [numeNpc|nearest]`, cu permisiunea existenta `ainpc.admin`. Nu porneste simularea sau rutina. Acest flag ruleaza plannerul numai la previzualizare, nu periodic si nu la fiecare tick Minecraft. Dezactivarea lui elimina calculul si liniile suplimentare din diagnostic. Executia controlata are un flag independent, descris separat.

[`NpcCarePlanner`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/NpcCarePlanner.kt) este o descompunere determinista limitata, de tip HTN, nu un motor GOAP general. Primeste acelasi `NpcAgentSnapshot` folosit de previzualizarea Utility AI, plus o copie valorica a casei cunoscute si a modului de simulare. Captura nu rezolva `Location`, nu retine entitati si nu verifica sau incarca chunk-uri. Plannerul nu consulta Bukkit, DB sau LLM si nu creeaza taskuri.

Un singur obiectiv local este propus: satietate sau energie pana la pragul observat `60/100`. Alegerea foloseste focusul cognitiv existent si pragurile `35/60`; un obiectiv activ pentru aceeasi casa poate continua peste pragul de initiere. Un obiectiv cu trei minute cumulate fara progres nu primeste o noua propunere identica. Siguranta, interlocutorii si starile incompatibile au prioritate. Modul diferit de `FULL`, valorile invalide ori o casa lipsa/nefinita/din alta lume blocheaza propunerea.

| Obiectiv | Alternative modelate | Pasi propusi |
|---|---|---|
| Satietate | `EAT` | Revenire acasa daca este necesara → masa → observarea refacerii |
| Energie | `SLEEP`, `REST` | Revenire acasa daca este necesara → somn/odihna → observarea refacerii |

Exista cel mult doua alternative cu maximum trei pasi fiecare, fara cautare recursiva, retry sau cozi. Costul ordinal prefera continuarea somnului/odihnei deja incepute, altfel somnul in fata odihnei. Nu este o estimare de durata, distanta sau performanta. Modelul de nevoi existent permite recuperarea energiei prin somn sau odihna; pentru masa nu sunt inventate stocuri, rezervari sau tranzactii. Acestea sunt propuneri in modelul abstract existent, nu dovezi de hrana consumata ori refacere produsa.

`NpcCarePlanPreview` separa statusul, obiectivul si alternativele nemodificabile. Comanda afiseaza explicit `SHADOW, fara executie`, pasii, alternativa preferata si probabilitatea Utility curenta a fiecarei actiuni de ingrijire. Aceasta probabilitate poate fi zero departe de casa; nu se compara `RETURN_HOME` cu o actiune Utility ca si cum ar avea aceeasi semantica. Cache-ul selectiei ramane intact, iar propunerea este recalculata din captura curenta chiar intr-o previzualizare cu selectie in cooldown.

`invalidationReason` compara o noua captura cu intrarea propunerii: UUID si identitate runtime, ambele revizii, fereastra strict mai mica de cinci secunde, mod, lume, coordonatele casei, cognitie si prezenta semantica acasa. Timestamp-urile invalide sau intoarcerea ceasului invalideaza propunerea; citirile nu reinnoiesc termenul. Sunt detectate inclusiv modificarile directe de nevoi, obiectiv, rutina si interlocutor care nu cresc reviziile. Verificarea este conservatoare si se testeaza separat; nu este o autorizatie de executie si nu demonstreaza prospetimea perceptiei, accesibilitatea rutei sau disponibilitatea resurselor.

Limita previzualizarii shadow: nu modifica scoruri/probabilitati, stare, nevoi, obiectivul existent, dovezi, statistici sau navigatie. Nu injecteaza propunerea in memoria/dialogul NPC-ului ca intentie asumata, inclusiv atunci cand flagul separat de executie este pornit. AG2 ramane partial; activarea in productie necesita masuratori si verificari Paper.

## Executie controlata a ingrijirii acasa - continuare AG2

`simulation.decision.care_planner_execution_enabled`, implicit `false`, permite aplicarea planului numai pentru NPC-uri spawnate, in modul `FULL`, cu simularea si rutina active prin ambele perechi de flaguri globale/locale. Necesita threadul principal Paper, absenta unei pauze/override de rutina, siguranta si lipsa interlocutorilor. Nu activeaza niciun alt flag si nu adauga un scheduler. Reutilizeaza apelul sincron existent al simularii de viata, dupa actualizarea perceptiei si a nevoilor.

[`NpcCarePlanExecutor`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/NpcCarePlanExecutor.kt) este un adaptor determinist bounded: aplica `EAT` pentru satietate sau `SLEEP`/`REST` pentru energie, cu secventa exacta ingrijire → observarea refacerii. Odihna fara somn este monitorizata distinct, conform sectiunii urmatoare. Un plan cu `RETURN_HOME` nu este executat aici; deplasarea si eventualele fallback-uri raman exclusiv rutinei existente. Nu se introduc miscari, teleporturi, rezervari, inventare sau consumuri fizice.

Intrarea este capturata dupa actualizarea lumii si a nevoilor. Inainte de aplicare se verifica invalidarea, politica runtime si pozitia fizica: aceeasi lume, coordonate finite, distanta tridimensionala de cel mult `5.5` blocuri fata de casa capturata si chunk-ul casei deja incarcat. Nu este suficient `context.isAtHome`. Verificarea foloseste pozitia entitatii si `isChunkLoaded`, fara scanari sau incarcari de chunk-uri. Dupa citirea fizica se recaptureaza preconditiile, inclusiv valorile fara revizie. Aceste citiri raman pe threadul principal; nu fac adaptorul Paper sigur pentru executie asincrona.

Aplicarea reutilizeaza `DecisionEngine.applySimulationOutcome`, tranzitia existenta, `NpcSelfCareGoal` si `NpcActionExperience`. Dupa evenimentul de schimbare a starii se verifica din nou politica, pozitia fizica, identitatea, reviziile si preconditiile. Numai tranzitia asteptata este permisa; o schimbare dus-intors de stare nu poate fi ascunsa prin normalizarea reviziei. Modificarile directe de nevoi/intentii, teleportul, conversatia, despawn-ul, oprirea flagului sau un tick reentrant nu primesc dovezi fabricate si nu sunt suprascrise de intentia veche. O tranzitie deja publicata nu se anuleaza fortat; aplicarea intrerupta nu continua cu o a doua alegere in acelasi tick.

La acceptarea aplicarii se elimina numai cooldown-ul Utility al NPC-ului. Statisticile Utility nu numara alegerea plannerului. `NpcDecisionExplanation.fromCarePlan` marcheaza sursa planului numai dupa aplicarea validata, fara factori de scor inventati si fara afirmatii de succes. Dialogul si promptul folosesc explicatia prin capturile cognitive existente. Propunerea shadow nu devine istoric; explicatia aplicata ramane temporara, fara schema noua de persistenta.

Executia nu creste imediat satietatea sau energia. Observatorii existenti confirma recuperarea numai la un tick ulterior, din diferentele reale ale modelului de nevoi. Pragul `60/100` incheie obiectivul observat; pana atunci planul poate fi recompus o singura data per apel eligibil de simulare. Nu exista coada de planuri, cautare recursiva, LLM, SQL sau retry in acelasi apel. Recuperarea este cea abstracta deja existenta, nu o dovada de aliment consumat ori o contabilitate fizica a resurselor.

Cand nu exista un plan eligibil, lipseste politica sau alternativa nu este executabila de acest adaptor, decizia Utility existenta ramane fallback-ul. Ea poate alege independent ingrijirea; lipsa eligibilitatii plannerului nu este o interdictie globala asupra vechiului comportament. In schimb, o casa neconfirmata fizic pentru planul deja propus, invalidarea observata pe parcursul verificarii ori intreruperea aplicarii opreste alegerea pentru apelul curent, evitand un fallback pe acelasi context nesigur sau depasit.

Limite: executia in afara casei, rezervarile de resurse, un executor general de planuri si profilarea Paper raman de implementat. Inainte de activarea pe un server real se verifica separat: pornire opt-in si oprire prin flag; masa/somn/odihna cu refacere observata ulterior; conversatie sau teleport in timpul ingrijirii; chunk tinta neincarcat; lipsa miscarilor suplimentare si costul p95/p99 cu flagul oprit/pornit. Testele locale folosesc adaptoare/proxy-uri, nu reprezinta acest smoke test Paper.

## Odihna fara somn - extindere AG2

Alternativa `REST` poate fi executata prin acelasi flag `simulation.decision.care_planner_execution_enabled`, ramas implicit `false`, cu aceleasi preconditii fizice si de lifecycle. Secventa acceptata este exact `REST_AT_HOME → OBSERVE_RECOVERY`; nu este o deplasare ori o conversie in `SLEEP`. Costul euristic existent prefera continuarea odihnei deja incepute; din repaus, plannerul prefera in continuare somnul. Nu este o selectie noua invatata intre cele doua strategii.

`NpcSelfCareGoal` poate urmari acum odihna fara somn, cu energia drept nevoie si `RESTING` drept stare obligatorie. Pornirea acestui tip de obiectiv necesita calea explicita a plannerului si o casa cunoscuta; nu se poate crea un obiectiv de revenire `REST` in afara casei. Interlocutorii, pericolul sau schimbarea in somn/conversatie intrerup observarea, fara a atribui acele castiguri odihnei anterioare. Intentia aplicata nu confirma refacerea.

Numai un obiectiv `REST` activ, in etapa `RECOVERING` si fara bugetul de lipsa a progresului epuizat pastreaza focusul cognitiv pe energie pana la `60/100`. Astfel, dialogul despre nevoi si plannerul folosesc aceeasi continuitate, inclusiv peste pragul initial `35`. Odihna legacy fara un asemenea obiectiv nu primeste aceasta histereza suplimentara. Rutina amana activitatea obisnuita numai pentru un obiectiv de odihna recent, sigur, la aceeasi casa, cu executia opt-in si simularea `FULL` active; pauzele si override-urile raman prioritare.

`NpcActionExperience` inregistreaza separat energia observata in timpul odihnei, numai dupa o aplicare explicita a plannerului si un tick ulterior valid. Refacerea ambientala de acasa se scade si pentru `REST`: o crestere egala cu recuperarea normala are efect invatat `0`, desi cresterea bruta este descrisa fidel. Nu se inventeaza un avantaj cauzal al odihnei fata de simpla prezenta acasa. Somnul si odihna pastreaza actiuni, stari, observatii si texte distincte; istoricul uneia nu devine experienta celeilalte.

Istoricul `REST` se salveaza prin codec-ul existent, in `simulation.action_experience`, cu aceeasi schema JSON `1`, limite si validari numerice. Nu este necesara o migrare SQL; un cititor vechi care nu accepta experienta `REST` o ignora, fara sa transforme inregistrarea in somn. Obiectivul, planul si perioada in asteptare raman temporare. Restaurarea observatiilor nu aplica actiuni si nu reface nevoi. Explicatia ultimei experiente, dialogul local si contextul AI mentioneaza odihna, nu un somn neobservat.

Semnaturile publice existente pentru pornirea obiectivului si a perioadei de experienta sunt pastrate si nu inroleaza implicit `REST` legacy in monitorizare. Calea interna a plannerului acorda explicit aceasta permisiune. Oprirea executiei nu sterge istoricul invatat; folosirea lui ramane sub `simulation.decision.experience_enabled`. Nu se adauga scheduler, provider, coada, schema noua de snapshot sau inventar.

## Buget comun al plannerului - primul increment AG3

[`NpcCarePlanningBatch`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/NpcCarePlanningBatch.kt) limiteaza pornirea evaluarilor plannerului intre NPC-urile unui apel `NPCManager.runLifeSimulationTick`. Este un buget al ciclului de simulare, nu al fiecarui tick Minecraft. Ramane sub flagul de executie existent, implicit `false`; nu activeaza simularea sau alt serviciu.

| Configurare | Implicit | Interval efectiv | Semnificatie |
|---|---|---|---|
| `simulation.decision.care_planner_max_per_cycle` | `32` | `0..256` | Maximum de incercari ale plannerului in acel ciclu, inclusiv planurile neeligibile |
| `simulation.decision.care_planner_budget_ms` | `2.0` | `0..20` | Consum cumulat dupa care nu se mai admit evaluari noi |

`0` opreste admiterea de planuri noi, nu actualizarea nevoilor sau toate deciziile NPC-ului. Valorile sunt normalizate; un buget de timp nefinit revine la `2.0` ms. Limitele sunt capturate la inceputul ciclului. Capacitatea se consuma numai dupa verificarea politicilor de executie si a prezentei semantice acasa, inainte de capturarea intrarii si calculul planului.

Cu executia pornita, lista actorilor este copiata o singura data per ciclu, iar evaluarea incepe dupa ultimul UUID admis in ciclul anterior. Pentru o populatie cu ordine stabila, aceasta rotatie distribuie incercarile intre NPC-uri, inclusiv atunci cand exista mai multi candidati decat credite. Cursorul avanseaza dupa ultima incercare, nu numai dupa o aplicare reusita. Un cursor disparut reia evaluarea de la inceput; nu se pastreaza o coada, un registru nou de NPC-uri sau planuri amanate. Nu este o garantie de latenta sau de prioritizare sub schimbari continue de populatie/ordine. Cu flagul oprit se pastreaza iterarea existenta, fara aceasta copie ori masuratori noi.

Nevoile, observarea rezultatelor si contextul tuturor actorilor eligibili continua sa fie actualizate, indiferent de creditele plannerului. Cand nu mai exista capacitate, Utility AI ramane fallback-ul. Exceptia este o ingrijire deja aplicata, inca valida: acelasi obiectiv recent in `RECOVERING`, aceeasi casa, stare compatibila, nevoie sub `60`, siguranta si lipsa interlocutorilor. Aceasta poate continua fara o noua alegere, fara reinnoirea explicatiei si fara pornirea unei noi perioade de experienta. O perioada acceptata anterior poate fi observata o singura data, iar monitorul obiectivului poate confirma in continuare diferente reale. Pericolul, schimbarea casei/starii, expirarea, nevoia satisfacuta sau lipsa progresului nu primesc aceasta prioritate.

Timpul foloseste `System.nanoTime` si acopera segmentele admise ale plannerului: captura, planificare, verificari si aplicare, inclusiv callback-urile sale. Nu acopera perceptia initiala, actualizarea nevoilor, Utility AI, intreaga rutina, schedulerul sau LLM-ul. Nici copia/rotatia actorilor, verificarile preliminare de politica si continuitatea dupa amanare nu sunt limitate temporal de aceasta optiune. Pentru o amanare fara obiectiv in recuperare nu se mai construieste o captura cognitiva suplimentara. Pauzele dintre segmentele admise nu sunt taxate. Bugetul este o limita de admitere, nu preemptie: o operatie deja pornita poate depasi pragul si este lasata sa se incheie in siguranta. Nu se promit `2` ms ca limita totala a tick-ului. Citirile si mutatiile Paper raman pe threadul principal, conform [documentatiei schedulerului Paper](https://github.com/PaperMC/docs/blob/main/src/content/docs/paper/dev/api/scheduler.md).

Apelurile reentrante ale managerului impart acelasi buget si nu reseteaza creditele. Timpul lor suprapus este numarat o singura data in consumul ciclului, inclusiv intervalul activ al apelului exterior. Un ceas invalid blocheaza admiteri noi pana la urmatorul ciclu. `finally` elibereaza cadrul si produce un raport partial daca procesarea esueaza, fara a transforma o exceptie intr-o aplicare reusita. Apelul public direct `DecisionEngine.runLifeSimulationTick(npc)` deschide un ciclu individual daca nu exista deja unul; apeluri directe independente nu constituie un rate-limit global pe tick.

`/ainpc decision` afiseaza numai ultimul raport finalizat sau intrerupt: limitele capturate, incercarile, amanarile dupa primul prag atins, rezultatele, timpul si momentul capturii. Raportul este detasat si nemodificabil; citirea sau shadow preview nu consuma credite, nu roteste cursorul si nu actualizeaza masuratorile. Raportul poate ramane vizibil ca istoric dupa oprirea flagului. Contorul de exceptii este al segmentelor plannerului, in timp ce marcajul de ciclu intrerupt poate reflecta si o eroare din alta parte a procesarii.

Masuratorile per incercare folosesc `PerformanceMonitor` existent, cu numele fix `tick.care_planner`, fara serii per UUID/nume. Se aplica limitele si configurarea existente ale observabilitatii. Duratele individuale includ apelurile reentrante, astfel ca suma lor nu trebuie confundata cu uniunea intervalelor din raportul ciclului. Monitorul furnizeaza statisticile si urmele deja suportate; nu se declara percentile noi `p95/p99` ori rezultate de profilare Paper prin simpla instrumentare.

Acest buget local, separat de LOD-ul descris mai jos, nu introduce simulare a entitatilor neincarcate, contabilitate de resurse, coada comuna pentru LLM/regiuni sau limita de timp a intregii simulari. Snapshotul tranzitoriu al actorilor are dimensiunea populatiei managerului; cursorul si ultimul raport sunt singurele date noi pastrate de buget intre cicluri. Validarea de incarcare si costurile Paper raman obligatorii inaintea activarii in productie.

## AI LOD dinamic pentru entitati disponibile - continuare AG3

[`NpcSimulationLodPass`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NpcSimulationLod.kt) adauga detaliu runtime derivat, fara rescrierea `AINPC.simulationMode`, a profilului persistent sau a API-ului public. Optiunea este independenta de executorul plannerului si nu activeaza simularea/rutina.

| Configurare | Implicit | Interval efectiv | Semnificatie |
|---|---|---|---|
| `simulation.lod.enabled` | `false` | boolean | Activeaza reducerea deciziilor autonome la distanta |
| `simulation.lod.full_radius` | `48.0` | `8..256` blocuri | Revenirea din detaliu redus la `FULL` |
| `simulation.lod.light_radius` | `64.0` | `full_radius..512` blocuri | Iesirea din `FULL` spre `LIGHT` |

Se foloseste distanta euclidiana 3D pana la un jucator online din aceeasi lume. Limitele sunt inclusive; in banda dintre raze se pastreaza ultima clasificare de proximitate. Prima evaluare cere raza interioara. Valorile nefinite revin la valorile implicite, pastrand ordinea razelor. Modul autorului ramane plafon: `NONE` nu este reactivat, `LIGHT` nu este promovat, iar numai `FULL` poate fi redus temporar. O entitate indisponibila nu este simulata din coordonatele persistente.

Conversatia/interlocutorii, pericolul observat, siguranta redusa, deplasarile in curs, masa/somnul, urmarirea, oferirea unei misiuni si actorii cu proprietar de scenariu/misiune pastreaza detaliul complet. Un obiectiv recent de ingrijire, activ si fara plafonul lipsei de progres epuizat, protejeaza inclusiv odihna `REST`. Citirea lui nu reinnoieste dovezi sau explicatii. Sunt exceptii conservative, nu o clasificare generala a importantei: de exemplu, o deplasare care ramane blocata poate mentine `FULL`. Pauzele, override-urile explicite si politicile de executie existente raman autoritare.

Managerul captureaza pozitiile jucatorilor o singura data per ciclu; coordonatorul rutinei foloseste o captura comuna pentru rutina, socializare, sincronizare, intalniri si activitati sezoniere. Apelurile publice separate isi creeaza propriul cadru. Captura ruleaza numai pe threadul principal Paper; cu flagul oprit nu citeste jucatori, entitati sau threadul Bukkit. [Contractul `Bukkit.getOnlinePlayers`](https://jd.papermc.io/paper/1.21.11/org/bukkit/Bukkit.html#getOnlinePlayers()) interzice iterarea asincrona a colectiei vii. Nu se introduc task-uri noi sau incarcari de chunk-uri.

Indexul spatial tranzitoriu grupeaza coordonate scalare dupa lume si celule X/Z de dimensiunea razei exterioare. O cautare consulta maximum noua celule si confirma separat distanta 3D; nu parcurge intreaga lista a jucatorilor pentru fiecare NPC. Costul celulelor aglomerate ramane proportional cu ocupantii lor, nu are o limita temporala stricta. Pozitiile necunoscute/invalide nu sunt interpretate ca dovada ca zona este goala: un NPC `FULL` ramane conservativ `FULL`. Captura nu este adaugata perceptiei, dialogului sau memoriei NPC-urilor si nu le confera cunoasterea jucatorilor indepartati.

Un cadru memoreaza cel mult o clasificare de proximitate per obiect NPC si revizie de ciclu de viata; importanta este reverificata la utilizare. Intre cadre ramane numai istoricul mic de histereza al NPC-ului, marcat tranzitoriu. Schimbarea configurarii, a lumii sau a ciclului de viata il invalideaza; oprirea flagului revine imediat la modul autorului in fluxurile evaluate, fara o clasificare `LIGHT` persistenta. Diagnosticul si shadow preview raman inspectii ale capabilitatilor modului autorului, nu dovezi de admitere intr-un tick LOD.

Implicit, in simularea vietii perceptia este actualizata o singura data inaintea deciziei finale LOD. Extensia optionala de mai jos poate reutiliza temporar observatia pentru actorii indepartati eligibili. Un pericol nou detectat la o actualizare promoveaza acelasi tick, chiar daca verificarea initiala il clasifica drept indepartat. In `LIGHT` se reutilizeaza actualizarea de baza a nevoilor si anularea atribuirii/obiectivului in asteptare, fara selectie Utility, planner sau cereri autonome de hrana. Nu se cauta ancore noi pentru actorii deja clasificati `LIGHT`. Rutina obisnuita, perechile sociale si procesarile autonome mentionate nu initiaza activitati noi pentru acest detaliu; un override explicit este totusi evaluat prin contractul existent. Platile salariale existente nu sunt conditionate de LOD, pentru a nu modifica implicit economia in lipsa contractului de contabilitate abstracta.

LOD-ul de baza reduce selectia si coordonarea, nu costul tuturor citirilor Paper. Modelul existent al nevoilor ramane activ pentru NPC-urile disponibile. Nu opreste miscari in curs, nu creeaza simulare fara entitate si nu contabilizeaza resurse. Nu pretinde un buget global al tick-ului sau castiguri de performanta masurate. Acestea si probele unload/reload raman pasi AG3 separati; flagurile raman implicit oprite pana la validarea Paper.

## Cadenta perceptiei pentru NPC-uri indepartate

Politica din [`NpcPerceptionRefresh.kt`](../../ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NpcPerceptionRefresh.kt) adauga reutilizare temporara, integrata numai in refresh-ul periodic al simularii vietii. Necesita `simulation.lod.enabled` si optiunea noua `simulation.lod.perception.enabled`, ambele implicit `false`. Nu activeaza simularea, rutina, plannerul sau vreun worker. Cu LOD oprit, motorul nu construieste setarile acestei politici si nu citeste ceasul Bukkit pentru ea.

| Configurare | Implicit | Interval efectiv | Semnificatie |
|---|---|---|---|
| `simulation.lod.perception.enabled` | `false` | boolean | Permite reutilizarea numai pentru `FULL` al autorului redus dinamic la `LIGHT` |
| `simulation.lod.perception.light_refresh_ticks` | `1200` | `20..1200` tick-uri server | La atingerea varstei se cere refresh, nu se prelungeste cache-ul |
| `simulation.lod.perception.max_reuse_distance` | `2.0` | `0..8` blocuri | Distanta 3D maxima fata de pozitia observatiei initiale; valori nefinite revin la `2.0` |

Prima observatie este obligatoriu actualizata. Reutilizarea verifica setarile, UUID-ul si instanta runtime, reviziile de decizie/stare, obiectele context/entitate, tokenul observatiei, lumea si pozitia valida. Deplasarea este raportata la ultima observatie reala, nu la pozitia ultimului cache hit. Schimbarea sursei, ciclului de viata sau configurarii cere refresh; o actualizare fortata externa roteste tokenul si nu poate fi confundata cu vechiul cache. Un context al altui NPC este refuzat. Varsta foloseste tick-ul serverului, inclusiv rollover-ul semnat, nu timpul Minecraft al zilei si nici timpul offline.

NPC-urile importante, interlocutorii, apropierea jucatorilor, pericolul cunoscut, deplasarile/ingrijirea active si actorii scenariilor pastreaza refresh-ul complet prin clasificarea LOD existenta. Modurile explicite `LIGHT/NONE` nu sunt promovate sau incadrate speculativ in noul cache. Apelurile directe `AINPC.updateContext()` / `NPCContext.updateFromWorld()` folosite de interactiuni raman actualizari complete, cu deduplicarea legacy a scanarii vecinatatii in acelasi tick. Politica nu muta accesul la lume pe async si nu incarca chunk-uri pentru a afla proximitatea.

Pe un cache hit se proiecteaza in continuare nevoile si ancorele curente, apoi ruleaza numai calea de nevoi `LIGHT`, fara Utility, planner, cereri autonome de hrana sau credit de invatare. Importanta este reverificata inaintea reutilizarii. La un refresh obligatoriu, un pericol nou observat poate promova acelasi pas in `FULL`. Un esec ori o invalidare in callback nu publica un cache reutilizabil. Nu se emit artificial evenimente noi de observatie prin simpla reutilizare.

`NpcPerceptionSnapshot.observation` este metadata optionala imuabila: momentul observatiei originale, reutilizarea si varsta **la ultima evaluare periodica**. Captura ei nu citeste Bukkit; nu este o varsta recalculata permanent si nici dovada de autoritate. Descrierea/promptul marcheaza datele vechi; numarul persoanelor este descris ca ultima observatie, nu un nou „vad”. Resolverele de mediu si fallback-ul nu confirma vremea/lumina/evenimentele actuale dintr-o observatie reutilizata. Descrierea factuala a locatiei nu foloseste topologia cache-uita ca fapt curent. `/ainpc decision` afiseaza read-only provenienta/varsta, fara a forta refresh-ul sau a reinnoi timestamp-ul.

**Limite:** un pericol nou, neobservat inca, poate fi descoperit abia la urmatorul refresh ori la o interactiune/invalidare care impune refresh. Nu este detectie de urgenta continua. Plafonul este exprimat in tick-uri server; lag-ul poate extinde durata reala. Cu simularea implicita la `30` de secunde, la `20 TPS` si limita `1200`, un actor stabil poate reutiliza observatia la pasul intermediar; daca intervalul simularii atinge/depaseste limita, nu apare reutilizare intre pasi. Cache-ul nu este un buget global: refresh-urile initiale, expirate sau importante pot ramane grupate intr-un ciclu. Alte apeluri explicite ale perceptiei pot continua sa scaneze. Testele verifica apelurile adaptorului si politica cu proxy-uri; nu reprezinta benchmark Paper sau promisiune de TPS.

Istoricul si tokenurile sunt tranzitorii, cel mult un istoric per actor; nu se serializeaza in profil si nu se restaureaza din SQL. Nu exista coada, scheduler, migrare sau autoritate abstracta noua. Reconcilierea completa a lumii/resurselor si bugetarea globala a perceptiei raman separate.

## Explicatia ultimei decizii

`NpcDecisionExplanation` pastreaza temporar actiunea selectata si cel mult doua contributii pozitive la scorul ei, ordonate descrescator: nevoi, context, rutina, personalitate, emotii, relatia cu interlocutorul, experienta de ingrijire sau ajutor alimentar confirmat. Factorii provin din calculul efectiv al alegerii si nu sunt recalculati din starea prezenta. Scorul de baza si contributiile negative nu sunt prezentate ca motive favorabile. Explicatia este partiala; nu reproduce toti multiplicatorii, cooldown-urile si regulile de eligibilitate.

Selectia retine si faptul ca exista mai multe optiuni cu probabilitate pozitiva, daca alegerea a fost ponderata. Descrierea mentioneaza atunci ca alegerea putea varia, fara sa pretinda ca actiunea aleasa avea neaparat cel mai mare scor. `AINPC.lastDecision` este actualizat numai la aplicarea rezultatului corespunzator in simularea `FULL`; previzualizarea, diagnosticul si o intrebare nu creeaza aceasta amintire. Descrierea raporteaza intentia ultimei alegeri, fara sa confirme reusita actiunii.

Pentru o alegere aplicata prin plannerul local de ingrijire, `fromCarePlan` separa explicit sursa de selectia Utility si nu atribuie planului factori de scor sau variatie probabilistica. Confirmarea refacerii ramane responsabilitatea observatorului de nevoi, nu a explicatiei.

Explicatia expira dupa cinci minute, ignora timestamp-uri viitoare si nu este persistata. Spawn-ul, reatasarea, despawn-ul, indisponibilitatea entitatii, teleportul, restaurarea starii si schimbarea modului de simulare o invalideaza impreuna cu revizia selectiei. Verificarea reviziei previne restaurarea unei explicatii invalidate inclusiv dintr-un callback al schimbarii de stare. Conversatia poate pastra explicatia istorica, iar invalidarea cache-ului de decizii nu ii reinnoieste termenul.

`De ce ai ales asta?`, `De ce faci asta?`, `Cum ai luat decizia?` si `Ce ai decis?` primesc explicatia prin `NpcCognitiveResponse`; fara o inregistrare recenta, raspunsul recunoaste lipsa ei. `NpcCognitiveState` si snapshot-ul pentru AI captureaza aceeasi descriere, separat de intentia curenta si de rezultatele observate. Referirea la un interlocutor anterior nu confirma ajutorul jucatorului care intreaba acum.

## Progresul obiectivelor de ingrijire

`NpcSelfCareGoal` urmareste un singur obiectiv temporar de hrana (`EAT`), somn (`SLEEP`) sau, exclusiv pe calea explicita a plannerului, odihna fara somn (`REST`), pornit numai dupa aplicarea unei alegeri compatibile in simularea `FULL`, cat timp nevoia este sub pragul de recuperare 60. Si `RoutineActivityExecutor` poate porni obiectivul dupa deplasarea acceptata sau sosirea verificata la ancora casei pentru masa ori somn, cu starea compatibila si revizia neschimbata. O destinatie invalida, deplasarea refuzata, o simpla previzualizare sau o intrebare nu confirma un asemenea angajament. Monitorizarea nu adauga un planificator general, pathfinding, miscari sau influente asupra scorurilor.

Etapele disting intentia de intoarcere acasa, prezenta observata acasa pentru refacere, lipsa unei case confirmate, incheierea si intreruperea. Intoarcerea aleasa nu confirma plecarea, existenta unei rute sau sosirea. Sosirea singura nu confirma refacerea: aceasta este observata strict in jurul actualizarii reale a nevoilor. Monitorizarea functioneaza si cu `simulation.decision.experience_enabled=false`, separat de invatare.

Obiectivul se incheie numai dupa o actualizare observata cu nevoia de cel putin 60. O schimbare externa a nevoii poate satisface obiectivul, dar nu este atribuita automat planului. Descrierea confirma o imbunatatire in timpul ingrijirii de acasa numai daca ultima actualizare care incheie obiectivul a observat un castig in starea potrivita; altfel declara cauza neconfirmata. Nu confirma consumul unui obiect, ajutorul jucatorului sau o recompensa de quest. Experienta cauzala ramane responsabilitatea separata a `NpcActionExperience`.

Observarea cere un interval pozitiv de cel mult 120 de secunde, timestamp-uri valide, aceeasi casa si un context sigur. Conversatia, combatul, o activitate incompatibila, schimbarea casei sau pericolul intrerup obiectivul activ fara sa il declare reusit. Spawn-ul, reatasarea, despawn-ul, indisponibilitatea, teleportul, restaurarea starii si schimbarea modului il sterg; verificarea reviziei previne recrearea lui dintr-un callback invalidat. Lipsa unei locatii si actualizarea de baza `LIGHT` il sterg de asemenea.

Este pastrat numai ultimul obiectiv, fara persistenta, cu snapshot imuabil de cel mult cinci minute de la ultima actualizare valida; citirea nu reinnoieste termenul. `NpcCognitiveState`, raspunsurile locale despre progres si textul `Progresul ingrijirii` capturat pentru AI folosesc aceleasi date, fara acces Bukkit asincron.

Explicatiile etapelor active sunt ancorate explicit la ultima observatie, nu prezentate ca perceptie curenta. `RECOVERING` spune ca NPC-ul era acasa atunci, `BLOCKED` ca ii lipsea o casa confirmata atunci, iar `RETURNING_HOME` ca sosirea nu era inca confirmata. La fel, lipsa progresului descrie intervalul inregistrat pana la acea observatie. O schimbare ulterioara de pozitie, intentie sau ancora nu transforma automat inregistrarea in adevar prezent. Raspunsul local, contextul capturat pentru AI si diagnosticul folosesc aceeasi formulare istorica; citirea nu rescaneaza lumea si nu corecteaza ori reinnoieste obiectivul. Etapele de succes si intrerupere isi pastreaza explicatiile istorice si aceleasi reguli.

## Continuitate optionala si lipsa progresului

`simulation.decision.goal_continuity_enabled`, implicit `false`, permite pastrarea revenirii acasa deja angajate pentru ingrijire. Necesita un NPC `FULL`, simularea si rutina pornite prin ambele perechi de flaguri globale/locale, aceeasi casa valida, nevoie inca nesatisfacuta, stare `WALKING`, siguranta si lipsa unui interlocutor activ. Pauzele si override-urile rutinei au prioritate. Cand conditiile sunt valabile, `WALK_TO_TARGET` este singurul candidat de continuitate; nu sunt modificate scorurile, iar `EAT`/`SLEEP` raman indisponibile departe de casa.

Aplicarea continua obiectivul original de hrana ori odihna fara sa confirme sosirea sau sa retrimita pathfinding. Miscarea ramane exclusiv in rutina. La sosire, ingrijirea obisnuita de acasa isi pastreaza regulile. Explicatia ultimei alegeri retine separat ca revenirea pentru obiectivul in curs a fost pastrata. Schimbarea eligibilitatii continuitatii invalideaza imediat reutilizarea deciziei din cache, inclusiv la oprirea feature-ului.

`withoutProgressMillis` cumuleaza numai timpul nou acoperit de actualizari valide ale nevoilor, dupa angajarea obiectivului. Sosirea confirmata sau un castig observat in timpul ingrijirii reseteaza acest contor. Timpul anterior angajarii, citirile, selectiile, reaplicarea aceleiasi intentii si observatiile duplicate nu adauga progres si nu reseteaza bugetul. Dupa trei minute cumulate fara sosire ori refacere confirmata, prioritatea revenirii este eliberata pentru urmatoarea alegere normala. Monitorul poate descrie lipsa progresului, dar nu afirma ca ruta este blocata sau ca ii cunoaste cauza.

Contorizarea foloseste un reper privat al ultimei observatii, separat de `Snapshot.updatedAt`. Reaplicarile aceleiasi ingrijiri intre doua tick-uri pot actualiza snapshot-ul, dar nu scurteaza intervalul observat fara progres. Un obiectiv nou sau sosirea confirmata porneste un reper nou; o observatie valida avanseaza reperul. Se numara numai intersectia cu intervalul tick-ului curent, fara timpul dinaintea obiectivului, dinaintea sosirii ori din intervale neobservate.

Aceasta nu este o lista permanenta de destinatii interzise: dupa intrerupere, o noua deplasare efectiv acceptata poate porni alt obiectiv. Nu exista replanificare generala, navigatie suplimentara, modificari de inventar sau confirmari de ajutor. Activarea continuitatii nu porneste singura taskurile periodice si ramane experimentala pana la verificarea in serverul Minecraft.

## Diagnostic cognitiv read-only

`/ainpc decision [numeNpc|nearest]`, cu `ainpc.admin`, include acum atentia, nevoile numerice autoritative, intentia curenta, ultima decizie aplicata si progresul obiectivului de ingrijire. Daca exista o observatie recenta, afiseaza actiunea si etapa, nevoia initiala si ultima valoare observata, timpul cumulat fara progres confirmat si vechimea inregistrarii. Intentia, alegerea aplicata si rezultatul observat au etichete separate; candidatii previzualizati nu devin istoric de actiuni.

`NpcSelfCareContinuity` explica primul impediment din evaluarea folosita chiar de selector: modul nepotrivit, feature oprit, simulare sau rutina oprita, lipsa obiectivului recent, etapa incompatibila, lipsa progresului, casa neconfirmata ori schimbata, prezenta acasa, nesiguranta, interlocutor, stare incompatibila, nevoie invalida ori satisfacuta, pauza sau override. `READY` inseamna eligibilitate pentru continuarea revenirii, nu sosire garantata. Raportul nu enumera simultan toate impedimentele.

`DecisionEngine.cognitiveDiagnostic` captureaza un model imuabil, cu acelasi timestamp pentru experienta, decizia istorica si progres. Nu actualizeaza lumea sau contextul, nu alege o actiune, nu scrie in cache/statistici si nu reinnoieste observatiile. Citirea la un timestamp care ar expira o inregistrare nu o sterge din starea live. Textul narativ este limitat la 320 de caractere pe camp si redus la o singura linie; lipsa dovezilor este declarata explicit. Procedura de verificare este in [runbook-ul de testare](../operations/debugging-si-testare.md#smoke-test-cognitiv).

## Experienta actiunilor de ingrijire

`NpcActionExperience` leaga o masa, un somn sau o odihna aplicata explicit prin planner acasa de schimbarea nevoii observata la urmatorul tick. Observatia este capturata strict in jurul actualizarii nevoilor, inainte de alegerea noii actiuni. O simpla selectie sau previzualizare nu creeaza experienta.

O perioada este acceptata o singura data daca actiunea si starea corespund, nevoia initiala nu a fost schimbata din exterior, casa/lumea si revizia starii au ramas aceleasi, contextul este sigur, iar intervalul este pozitiv si nu depaseste 120 de secunde. Intreruperile de stare, despawn-ul, reatasarea, teleportul si schimbarea modului de simulare anuleaza perioada in asteptare. Actualizarea `LIGHT` si dezactivarea invatarii o anuleaza de asemenea. Perioadele in asteptare nu sunt persistate.

O intrerupere din callback-ul aplicarii nu poate rearma perioada anulata. Chiar daca masa simulata continua ulterior, invatarea necesita mai intai o noua alegere aplicata valid si abia apoi o actualizare observata a nevoilor.

Efectul somnului si al odihnei planificate scade recuperarea ambientala acordata oricarui NPC aflat acasa. `REST` legacy nu porneste perioade noi de experienta. In modelul curent, castigul obisnuit al odihnei planificate este egal cu recuperarea ambientala, deci efectul invatat este `0`; cresterea bruta a energiei poate fi totusi relatata ca observatie, fara avantaj cauzal inventat. Efectele sunt normalizate la intervalul simularii. Sunt retinute cel mult 32 de observatii, cu snapshot sincronizat pentru autosave; numai cele din ultimele sapte zile si care nu sunt datate in viitor influenteaza scorurile.

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
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NpcAgentSnapshot.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NpcPerceptionSnapshot.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/OpenAIPromptSnapshotFactory.kt`
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
