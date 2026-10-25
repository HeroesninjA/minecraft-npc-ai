# Probabilitati pentru actiunile NPC

Status: implementare initiala pentru selectia de stare si obiectiv din simularea de viata.
Actualizat: 2026-10-25.

## Analiza

Un procent fix pentru fiecare actiune ar ignora nevoile, profesia, personalitatea si pericolul. Inainte de aceasta schimbare, `DecisionEngine` calcula deja un scor pentru fiecare actiune permisa de starea NPC-ului, adauga un zgomot aleator intre 0 si 10 puncte, apoi alegea mereu scorul maxim. Acest mecanism oferea variatie limitata, dar scorul afisat nu era stabil si nu reprezenta o probabilitate explicita. In plus, un apel repetat in cooldown refacea alegerea din scoruri, nu reutiliza explicit actiunea selectata.

Modelul implementat pastreaza scorul ca utilitate contextuala si aplica aleatorul o singura data, la selectia dintre optiunile relevante. Astfel, o actiune mai potrivita ramane mai probabila, fara a elimina complet alternativele apropiate.

## Selectie

1. `DecisionEngine` obtine candidatii pentru starea curenta, adaugand fuga si cererea de ajutor daca apare pericolul, respectiv masa si somnul pentru nevoi scazute atunci cand NPC-ul este acasa. Elimina replicile directe fara interlocutor activ in raza sociala, munca fara o ancora utilizabila si socializarea fara interlocutor ori ancora sociala utilizabila, apoi calculeaza scoruri deterministe din baza actiunii, trasaturi, emotii, relatii, nevoi, context si rutina.
2. `ActionProbabilitySelector` exclude `DO_NOTHING` cand exista o actiune cu scor pozitiv si `idle_weight` este zero, pastreaza cel mult `max_choices` actiuni aflate la maximum `score_window` puncte sub scorul maxim si ordoneaza egalitatile stabil dupa enum. Cu `idle_weight` pozitiv, rezerva un slot din `max_choices` pentru pauza voluntara, apoi selecteaza cel mult restul de actiuni utile.
3. Fiecare candidat primeste ponderea `exp((scor - scor_maxim) / temperature)`. Ponderile normalizate dau probabilitati cu suma 1; un singur numar aleator alege actiunea.
4. Daca nu exista un scor pozitiv, selectia revine la `DO_NOTHING`. Cand `simulation.decision.probabilistic_enabled: false`, ramane alegerea determinista a scorului maxim.

Pentru plimbare, observare si investigare, repetarea aceleiasi actiuni reduce initial ponderea ei la `repeat_weight` (implicit jumatate), dupa selectarea candidatilor apropiati de scorul maxim. Ponderea revine liniar la 1 pana la expirarea ferestrei configurate (implicit 90 de secunde), fara salt brusc al sanselor la final. Cu `repeat_history_size` mai mare decat 1, mai multe explorari distincte recente primesc separat penalizarea; o alegere obisnuita nu sterge lista, iar o noua explorare actualizeaza doar intrarea ei. Nu interzice repetarea; daca este singura optiune, probabilitatea ramane 100%. Penalizarea nu se aplica muncii, conversatiei, urgentelor sau modului determinist. Istoricul scurt este doar in memoria procesului si dispare la repornire.

Cooldown-urile configurate prin `action_cooldowns` sunt o protectie separata, mai stricta: o actiune activa este scoasa din distributie daca exista o alternativa in `score_window` calculata fata de cel mai bun scor initial. O actiune sub acest prag nu devine eligibila doar fiindca optiunile mai bune sunt in cooldown. Daca toate optiunile din fereastra sunt in cooldown, cea mai buna actiune este pastrata ca fallback, astfel incat NPC-ul nu ramane fara decizie. Cooldown-ul pe `DO_NOTHING` elimina pauza voluntara cand exista actiuni utile, dar pastreaza fallback-ul cand nu exista un scor pozitiv. Cooldown-urile se aplica numai selectiei probabilistice, nu pericolului sau selectiei deterministe, si sunt pastrate doar in cache-ul procesului.

Pentru scorurile 50, 48 si 47, probabilitatile sunt aproximativ 40,5%, 31,6% si 27,9%. Valorile nu sunt procente fixe configurate pe actiuni: se recalculeaza din starea NPC-ului la fiecare decizie eligibila.

## Configurare

- `simulation.decision.goal_continuity_enabled`: implicit `false`. Cu simularea si rutina active, un obiectiv sigur de ingrijire pornit dintr-o deplasare acceptata poate pastra temporar `WALK_TO_TARGET` ca unic candidat de revenire. Lipsa progresului observat elibereaza prioritatea dupa trei minute cumulate; pericolul, interlocutorul, pauzele si override-urile au prioritate. Nu schimba scorurile si nu executa miscare. Contractul este in [runtime-ul de simulare](simulation-service.md).
- `simulation.decision.max_choices`: implicit 3, permis 1-8. Limiteaza numarul alternativelor, inclusiv `DO_NOTHING` cand `idle_weight` este pozitiv.
- `simulation.decision.score_window`: implicit 15, permis 0-100. Zero permite doar actiuni la egalitate cu cel mai bun scor.
- `simulation.decision.temperature`: implicit 8, permis 0,5-50. O valoare mai mare face optiunile incluse mai apropiate ca sansa; una mai mica favorizeaza scorul maxim.
- `simulation.decision.repeat_weight`: implicit 0,5, permis 0,1-1. Este ponderea minima imediat dupa explorare; valoarea 1 dezactiveaza reducerea.
- `simulation.decision.repeat_window_seconds`: implicit 90, permis 0-3600. Controleaza revenirea liniara la ponderea 1; valoarea 0 dezactiveaza penalizarea.
- `simulation.decision.repeat_history_size`: implicit 1, permis 1-8. Controleaza cate actiuni exploratorii distincte din fereastra recenta pot fi penalizate; valoarea implicita pastreaza doar ultima explorare.
- `simulation.decision.random_seed`: optional, absent implicit. Cand este setat ca numar intreg, roll-ul probabilistic este reproductibil pentru acelasi seed, UUID NPC si numar de decizie; absent pastreaza aleatorul normal.
- `simulation.decision.action_cooldowns`: implicit gol. Mapeaza nume `NPCAction` la secunde (1-3600); o actiune in cooldown este omisa cand exista o alternativa, iar daca toate sunt blocate se foloseste cea mai buna ca fallback.
- `simulation.decision.idle_weight`: implicit 0, permis 0-1. Adauga `DO_NOTHING` ca alternativa voluntara; valoarea este pondere relativa fata de o actiune cu greutate 1 si nu afecteaza fallback-ul cand toate scorurile sunt nepozitive.
- `simulation.decision.statistics_window`: implicit 32, permis 1-256. Controleaza cate decizii reale sunt pastrate pentru evaluarea distributiei; modificarea configului redimensioneaza istoricul existent pastrand cele mai noi probe.
- `simulation.decision.action_weights`: implicit gol. Permite multiplicatori pozitivi intre 0,1 si 10 pentru numele `NPCAction` (de exemplu `OBSERVE: 1.5`); `DO_NOTHING` multiplica separat `idle_weight`, iar ceilalti multiplicatori se aplica doar ponderilor probabilistice, nu descompunerii scorului, selectiei deterministe sau starilor periculoase.
- `simulation.decision.action_weights_by_occupation`: implicit gol. Permite aceleasi multiplicatoare sub chei de ocupatie (comparare fara diferenta de litere); valoarea ocupatiei suprascrie doar actiunile declarate si pastreaza ponderile globale pentru restul.
- `simulation.decision.action_weights_by_time`: implicit gol. Permite aceleasi multiplicatoare sub `MORNING`, `AFTERNOON`, `EVENING` sau `NIGHT`; momentul zilei are prioritate peste ocupatie, iar actiunile nedeclarate pastreaza valorile straturilor anterioare.
- `simulation.decision.action_weights_by_weather`: implicit gol. Permite aceleasi multiplicatoare sub `CLEAR`, `RAIN` sau `THUNDER`; vremea are prioritate peste toate straturile anterioare, iar valorile lipsa raman mostenite.

Valorile negative ori zero acolo unde nu sunt permise revin la implicit; valorile peste maxim sunt limitate, iar numerele nefinite revin la implicit. Optiunile afecteaza doar selectia probabilistica, nu scorurile, prioritatea pericolului sau executarea actiunilor. Noile valori sunt citite din config la urmatoarea decizie dupa reincarcarea configuratiei.

## Diagnostic in joc

Pe langa distributia candidatelor, comanda afiseaza acum autoevaluarea, nevoile numerice, intentia, ultima decizie efectiv aplicata si progresul ingrijirii, cu vechimea si timpul cumulat fara progres confirmat. Motivul de continuitate provine din aceeasi evaluare folosita la alegere si reprezinta primul impediment gasit, nu toate conditiile simultan. `eligibila` nu confirma sosirea. Aceste date sunt capturate read-only si nu creeaza decizii sau observatii; detaliile sunt in [runtime-ul de simulare](simulation-service.md#diagnostic-cognitiv-read-only).

Experienta sociala are o contributie separata, `ajutor`: 5-8 puncte pentru `GREET`, `TALK` sau `THANK` cand NPC-ului ii este foame, contextul este sigur si interlocutorul curent verificat are acelasi UUID ca un ajutor alimentar confirmat recent. Declaratiile si reputatia nu produc acest bonus. Se aplica numai actiunilor deja eligibile si respecta `simulation.decision.experience_enabled`.

Administratorii pot folosi `/ainpc decision [numeNpc|nearest]` pentru a vedea scorul si sansa fiecarei actiuni candidate. `nearest` foloseste primul NPC din raza de 10 blocuri; consola trebuie sa indice un nume. Comanda necesita `ainpc.admin` si foloseste contextul deja memorat plus pozitiile curente ale tintelor: nu actualizeaza contextul lumii, nu consuma un numar aleator, nu executa o actiune si nu scrie in cache-ul deciziilor. Daca decizia curenta este in cooldown, afiseaza actiunea reutilizata cu 100%; in starile deterministe sau in pericol afiseaza castigatorul cu 100%. Daca simularea periodica este dezactivata, rezultatul ramane o previzualizare, nu o promisiune ca NPC-ul va actiona. Contextul se poate schimba intre comanda si urmatorul tick, deci procentele nu sunt garantii pentru o decizie viitoare.

Cand se calculeaza o noua decizie, diagnosticul afiseaza si numarul actiunilor eliminate de preconditii, cu primele trei motive: lipsa interlocutorului, a locului de munca/social, a prezentei acasa sau nevoia deja satisfacuta. Acestea nu sunt actiuni cu probabilitate zero din cauza scorului; sunt eliminate inainte de calculul probabilitatilor. In cooldown ori cat timp masa/somnul trebuie continuate, alternativele nu sunt reevaluate si lista motivelor ramane goala.

Separat, comanda arata pana la trei actiuni eligibile care au ramas fara sansa in selectia curenta: scor nepozitiv, scor sub `score_window`, depasirea `max_choices` sau alegerea determinista a celui mai mare scor. Actiunea `DO_NOTHING` nu aglomereaza lista cand este doar fallback. Aceasta clasificare este exclusiv pentru diagnostic si nu participa la alegerea actiunii; in cooldown nu se recalculeaza.

Daca penalizarea pentru explorare modifica efectiv distributia (actiunea recenta concureaza cu cel putin o alternativa), diagnosticul arata actiunea, multiplicatorul curent si secundele ramase. Nu afiseaza un multiplicator pentru o singura actiune cu sansa 100%, dupa expirarea ferestrei, in modul determinist sau cat timp este reutilizata decizia din cooldown. Cand `idle_weight` este pozitiv, `DO_NOTHING` apare ca alternativa calculata, nu ca eroare sau fallback.

Pentru fiecare candidat din previzualizare, diagnosticul afiseaza si componentele scorului: baza actiunii, trasaturi, emotii, memoria relatiilor, context, nevoi, rutina si experienta actiunilor. Contributia experientei vine din rezultatele observate ale mesei sau somnului la aceeasi casa, este limitata la ±12 puncte si cere nevoia activa intr-un context sigur. `simulation.decision.experience_enabled`, implicit `true`, permite dezactivarea invatarii si a acestei contributii. Regulile sunt descrise in [runtime-ul de simulare](simulation-service.md). Componentele afisate insumeaza scorul brut inainte de limitarea la zero; afisarea nu modifica selectia si nu creeaza observatii.

Daca este configurat un multiplicator in `action_weights`, `action_weights_by_occupation`, `action_weights_by_time` sau `action_weights_by_weather`, diagnosticul afiseaza separat ponderea efectiva pentru candidatul respectiv. Aceasta nu este un bonus de scor si nu apare pentru actiunile cu multiplicator implicit 1,0; afisarea explica doar de ce doua actiuni cu scoruri apropiate pot avea sanse diferite.

Pentru actiunile configurate in `action_cooldowns`, diagnosticul afiseaza secundele ramase. Acestea pot aparea simultan cu o actiune exclusa pentru `cooldown activ`; actiunile sub `score_window` sunt raportate ca atare chiar daca au si cooldown. Cand nu exista alternativa in fereastra, actiunea cu scorul cel mai bun poate ramane totusi in distributie ca fallback.

Dupa o decizie reala, motorul pastreaza in memorie ultimele `statistics_window` actiuni alese per NPC (implicit 32, maximum 256). `/ainpc decision` afiseaza numarul si distributia acestui esantion, fara a inregistra previzualizarea. Istoricul este eliminat cand NPC-ul este scos din cache sau la repornire; nu reprezinta memorie narativa si nu influenteaza probabilitatile.

Fiecare esantion retine si distributia care a fost folosita la alegere. Cand toate deciziile din fereastra au probabilitati disponibile, diagnosticul afiseaza abaterea dintre distributia observata si cea asteptata (distanta totala variationala, intre 0% si 100%). O valoare mica indica faptul ca esantionul urmeaza aproximativ sansele calculate; o valoare mare poate fi normala pentru doar cateva decizii si nu modifica automat configuratia.

Cand exista probabilitati retinute, acelasi diagnostic afiseaza si primele trei valori asteptate ca numar de alegeri in fereastra curenta. Acestea nu sunt actiuni suplimentare si nu modifica selectia; servesc doar la compararea rapida dintre comportamentul observat si distributia folosita de sampler.

## Prioritate si stabilitate

- Pericolul, starile `COMBAT` si `FLEEING`, precum si `TALKING` folosesc in continuare scorul maxim, nu selectia probabilistica. In pericol, fuga si cererea de ajutor devin candidate chiar daca NPC-ul era in repaus, la munca ori in conversatie.
- Alegerea selectata este pastrata in cooldown-ul existent de o secunda; schimbarea starii sau a semnalului de pericol o invalideaza. O actiune sociala sau de munca este recalculata si in interiorul cooldown-ului daca preconditia ei dispare. Cache-ul este limitat si curatat la eliminarea NPC-ului.
- `simulation.enabled` ramane implicit `false`; activarea probabilitatilor nu porneste singura taskul periodic. Cu simularea activa, decizia se ia la intervalul `simulation.tick_seconds` (implicit 30 de secunde), nu la fiecare tick Minecraft.
- Selectia modifica numai `currentState` si `currentGoal` prin fluxul existent. Nu executa comenzi, tranzactii, questuri sau actiuni privilegiate si nu permite unui LLM sa schimbe direct starea persistenta.
- Masa simulata recupereaza satietate la tick-ul urmator numai in starea `EATING` si acasa; somnul recupereaza deja energie. Nevoile sunt mentinute in intervalul 0-100. Nu se consuma obiecte si nu se afirma ca NPC-ul detine mancare.
- Masa si somnul pot incepe sub nivelul 35 si continua pana cand nevoia ajunge la 60. In aceasta perioada, la tick-urile fara pericol, ingrijirea aflata deja in curs ramane singura candidata; rutina activa nu o intrerupe acasa. Diferenta dintre cele doua praguri evita plecarile repetate imediat dupa o recuperare minima.

## Limite si pasii urmatori

Filtrul actual cere ca interlocutorul sa fie online (jucator) sau spawnat (NPC), in aceeasi lume si la cel mult raza sociala, calculata ca maximul dintre `npc.interaction_distance` si `dialog.passive_listen_radius` (implicit 8 blocuri). Acest control se aplica si interlocutorilor ramasi in context dupa ce s-au indepartat; disparitia tintei invalideaza si decizia sociala aflata in cooldown. Filtrul nu valideaza vointa interlocutorului, permisiunile, resursele sau tinta de lupta; acestea raman obligatorii inaintea unui executor real. `DecisionEngine` nu deplaseaza entitatea; miscarea spre casa pentru nevoi este gestionata separat de rutina, numai daca ambele sisteme sunt active si exista o ancora. Rutina foloseste protectiile existente de navigatie, inclusiv verificarea lumii si a chunk-ului; nu garanteaza sosirea cand deplasarea este imposibila sau dezactivata. Penalizarea repetitiei acopera doar actiunile exploratorii mentionate mai sus si, implicit, ultima actiune exploratorie; `repeat_history_size` permite extinderea listei fara a introduce procente fixe per NPC. Un `random_seed` configurat foloseste numarul deciziei din cache pentru reproducere; daca NPC-ul este scos din cache, secventa incepe din nou. Nu se scrie seed-ul in istoricul narativ.

Bonusurile de scor bazate pe relatia cu jucatorul si pe interactiunea activa se aplica doar daca acel jucator este inca online si in raza sociala; prezenta altui NPC sau jucator apropiat nu reactiveaza relatia cu un interlocutor indepartat. Bonusul de salut pentru o interactiune recenta foloseste intervalul scurs de la timestamp (sub 5 secunde), nu compara direct timestampul absolut cu 5000. Aceste verificari nu sterg memoria sau relatia persistenta, ci doar limiteaza influenta lor asupra deciziei curente.

Pentru actiunile de munca si socializare catre un loc, simpla existenta a ancorei nu este suficienta: numele lumii trebuie sa fie nevid, coordonatele finite, iar lumea si chunk-ul destinatiei deja incarcate pe server. Verificarea foloseste coordonate de chunk corecte inclusiv pentru pozitii negative. Validarea nu incarca chunk-uri si nu garanteaza ca NPC-ul poate ajunge ori lucra efectiv acolo; deplasarea ramane responsabilitatea rutinei, iar executorul verifica separat destinatia.

Cooldown-urile de actiuni nu sunt o memorie narativa si nu se persista in baza de date; eliminarea NPC-ului din cache sau repornirea serverului le reseteaza. Nu configureaza cooldown pentru o nevoie urgenta daca doresti ca `EAT`, `SLEEP` sau `FLEE` sa poata castiga imediat.

Cand `SOCIALIZE` este ales datorita unui interlocutor aflat deja in raza, rezultatul simulat este `SOCIALIZING`, cu obiectiv de interactiune, nu o deplasare catre o ancora sociala inexistenta. Fara interlocutor apropiat, ancora sociala ramane destinatia necesara; replicile directe au obiectiv de interactiune cu interlocutorul, nu de deplasare. Simularea nu genereaza singura replici si nu executa interactiuni privilegiate.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/DecisionEngine.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ActionProbabilitySelector.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/DecisionStatistics.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ActionEligibility.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ActionTargetAvailability.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/SocialTargetProximity.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/SocialActionOutcome.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/npc/NpcSelfCareThresholds.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandMisc.kt`
- `ainpc-core-plugin/src/main/resources/config.yml`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/engine/ActionProbabilitySelectorTest.kt`
