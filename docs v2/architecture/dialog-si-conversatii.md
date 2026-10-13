# Dialog si conversatii

Status: contract canonic verificat in cod pentru formularea si post-procesarea replicii NPC.
Actualizat: 2026-10-13.

Calea activa este `DialogManager -> DialogueEngine -> OpenAIService`. Cu provider disponibil, conversatiile explicite si raspunsurile factuale eligibile folosesc AI; raspunsurile locale sunt disponibile la configuratie dezactivata sau esec.

Inainte de aceasta cale, `DialogManager` aplica `NpcInteractionProfile`. `FULL_AI` intra normal in dialog; `MINIMAL` refuza conversatia libera; `SCENE_ONLY` refuza dialogul direct din chat; `QUEST_ONLY` intra numai cand jucatorul are o oferta de quest asociata NPC-ului curent; `SHOP_ONLY` intra numai daca exista un magazin configurat pentru ocupatia NPC-ului, accesibil jucatorului, cu cel putin o oferta acceptata de verificarile de configuratie ale comertului. Refuzurile folosesc trei variante naturale rotite separat pentru perechea NPC-jucator si profil, apoi aplica acelasi cooldown ca dialogul obisnuit. Refuzul pentru lipsa permisiunii mentioneaza accesul, nu lipsa marfii. Cache-ul de rotatie pastreaza maximum 512 perechi si nu este memorie narativa: refuzurile nu sunt salvate in `dialog_history`, nu modifica relatia si se reseteaza la recrearea `DialogManager`.

Pentru `QUEST_ONLY`, asocierea foloseste in ordine UUID-ul giverului, ID-ul DB si apoi numele, acceptand atat cheile canonice `quest_giver_*`, cat si cheile `quest_giver_npc_*` scrise la oferirea questului. Un identificator mai puternic prezent si diferit nu este suprascris de coincidenta numelui. Un quest activ, finalizat, esuat sau oferit fara giver identificabil nu deschide dialogul acestui profil. Raspunsurile scurte precum `da` si `nu` sunt interpretate ca decizii de quest numai in conversatia cu NPC-ul care a facut oferta; rezolvarea giverului, numele afisat si obiectivele de intoarcere la NPC recunosc si cheile `quest_giver_npc_*`. `SHOP_ONLY` foloseste aceeasi asociere ocupatie-magazin si, pentru magazinele cu `place_types` sau `place_ids`, verifica tipul si/sau ID-ul cladirii de la `workAnchor`; apoi cere permisiune si o oferta fara limite sau cerinte nesuportate, cu cost si rezultat pozitive. Nu verifica plata sau stocul. Profilul controleaza accesul la pipeline, nu continutul tuturor replicilor dupa ce accesul este permis.

In contextul semantic al dialogului, cladirea de munca este gasita din lumea si coordonatele `workAnchor`, prin `WorldAdminApi.findPlace`. Eticheta ancorei este numele afisat al locului, nu neaparat ID-ul sau; de aceea nu este folosita ca identificator. Cand cladirea este mapata, raspunsurile factuale despre meseria altor NPC-uri pot include numele real al locului de munca. Lipsa unui loc mapat nu este prezentata ca dovada ca NPC-ul nu lucreaza.

Pentru intrebari comerciale directe precum `ce vinzi?`, `ce oferte ai?` si `unde este magazinul?`, `NpcShopFactResolver` raspunde din ofertele configurate pentru ocupatia NPC-ului si din cladirea mapata la `workAnchor`. Calea activa a dialogului verifica permisiunile pe threadul principal si transmite numai magazinele accesibile catre engine-ul asincron. Aceeasi verificare este facuta si pentru intentia `TRADE` recunoscuta local, inclusiv `comert?` sau `preturi?`, fara a depinde de clasificarea AI. Catalogul enumera cel mult patru produse din ofertele pe care `ShopOffer.configurationIssue()` le considera corect configurate; ofertele cu limite sau cerinte nesuportate, fara cost/rezultat ori cu cantitati invalide sunt omise. Daca marfa exista doar in magazine inaccesibile, NPC-ul explica restrictia fara a enumera produsele; daca nu exista oferte validate, spune asta fara a promite marfa. Raspunsul nu garanteaza stocul sau resursele pentru tranzactie. Daca locul de munca nu este mapat, resolverul nu inventeaza un nume de cladire; celelalte ramuri ale dialogului pot continua conversatia.

Magazinele folosite de dialog sunt incarcate din `shops.yml`, inclusiv ofertele implicite pentru ocupatiile generate. Acestea cer si un tip de cladire corespunzator la `workAnchor`, astfel un NPC cu ocupatia potrivita dar fara loc de munca mapat nu pretinde ca are magazin. Administratorul poate adauga `place_ids` pentru un magazin legat de o cladire anume; tipul si ID-ul trebuie sa corespunda daca ambele sunt configurate. Pentru compatibilitate, magazinele personalizate fara `place_types` si `place_ids` raman asociate doar prin ocupatie. Daca administratorul schimba fisierul, reincarcarea continutului actualizeaza catalogul fara a pastra ofertele eliminate.

## Context incarcat

`DialogManager.processMessage(...)` aplica cooldown si citeste asincron:

- ultimele 5 perechi din `dialog_history`, selectate dupa `created_at DESC, id DESC` si readuse in ordine cronologica; ID-ul departajeaza mesajele salvate in aceeasi secunda;
- pana la 5 randuri obisnuite din `npc_memories` pentru NPC si player, plus pana la 3 declaratii personale cerute explicit pentru aceeasi pereche;
- relatia player-NPC din `npc_relationships`;
- numarul total de memorii si impactul emotional agregat.

Metoda `MemoryManager.getRelevantMemories(...)` nu foloseste in prezent textul `context`; selectia este doar dupa importanta si data. Numele metodei nu confirma cautare semantica.

`DialogHistory.recentStable(...)` pregateste o copie a istoricului pentru recunoastere, alegerea raspunsului, prompt si fallback: pastreaza numai schimburile stabile (`SELECTED` sau `EXECUTED`) cu ambele replici neblank, le ordoneaza cronologic si retine cel mult cinci. Pentru timestampuri egale pastreaza ordinea furnizata de citirea SQL. Schimburile esuate, respinse, doar propuse sau incomplete nu stabilesc singure o relatie cunoscuta si nu sunt prezentate AI-ului ca discutii anterioare. Memoriile si relatia persistata raman surse separate de recunoastere.

`DialogHistoryRepository` citeste strict perechea ID NPC-UUID jucator. Randurile persistate sunt citite ca `EXECUTED`; statusurile de ramura suplimentare se aplica listelor `DialogHistory` furnizate in memorie. Limitele de citire egale cu zero sau negative (`limit <= 0`) intorc o lista goala.

## Alegerea raspunsului

`DialogueEngine` foloseste urmatoarea ordine:

1. Resolverele locale aduna fapte despre NPC, mediu, story, locuri si munca; profilul jucatorului, amintirile si ofertele comerciale sunt verificate separat.
2. Cu `features.ai` activ, provider disponibil si `ai.dialogue.ground_facts_with_ai: true`, faptele eligibile sunt transmise modelului in prompt pentru o replica naturala. Raspunsurile despre oferte, identitatea jucatorului si rememorarea explicita raman exacte.
3. Fara provider sau la esecul apelului, faptele eligibile primesc replica locala deja calculata. Pentru celelalte mesaje, engine-ul selecteaza intentia si un template local; conversatiile explicite, inclusiv mesajele scurte, folosesc AI cand este disponibil.

Textul factual transmis modelului este o sursa de date, nu instructiuni. Promptul cere pastrarea faptelor si interzice detalii neconfirmate, dar modelul poate gresi; pentru comert si date personale sensibile calea exacta este pastrata. Folosirea providerului la mai multe replici poate creste costul si timpul de raspuns. Comutatorul `ai.dialogue.ground_facts_with_ai` poate dezactiva reformularea faptelor.

Optional, intre pasii 3 si 4, `AIOrchestrationService` poate clasifica un mesaj ambiguu scurt in `TRADE` sau `WORK_TALK`, numai cu `features.ai`, `ai.orchestration.enabled` si `ai.orchestration.intent_classification.enabled` active. Nu se apeleaza pentru raspunsuri factuale, intentii cunoscute sau mesaje care ar folosi deja AI contextual. Rezultatul este validat si verificat din nou fata de magazinul accesibil si ocupatia NPC-ului; un rezultat nepermis revine la intentia locala. Clasificarea alege doar o replica factuala construita local, nu executa comert si nu creeaza questuri.

Cand clasificarea este activa, `DialogManager` cauta magazinul asociat si pentru mesajele ambigue ale NPC-urilor `FULL_AI`, pe thread-ul principal, inainte de citirea DB. Astfel intentia `TRADE` poate folosi aceeasi verificare de cladire si permisiune ca `SHOP_ONLY`; fara magazin accesibil, eticheta AI este ignorata. Cu `TRADE` valid, replica enumera numai produsele din ofertele configurate si accesibile prin `NpcShopFactResolver.catalogSummary`, nu foloseste o promisiune comerciala generica. Catalogul nu garanteaza stocul sau plata.

Tot pe thread-ul principal, `DialogManager` capteaza pentru `WORK_TALK` profesia, rutina planificata si numele cladirii rezolvate din `workAnchor`. Instantaneul este capturat si pentru intentia recunoscuta local, indiferent de configuratia AI. `NpcWorkFactResponse` compune apoi replica din acest instantaneu, fara a inventa un loc de munca si fara a confunda programul cu activitatea deja realizata. Cu provider disponibil, modelul o poate reformula pe baza acelor fapte; la esec se livreaza replica exacta. In absenta profesiei, NPC-ul spune ca nu are o meserie stabilita; datele de rutina lipsa sau mesajele interne despre ancore invalide nu sunt rostite.

Saluturile locale folosesc acum recunoasterea derivata din relatia persistata si istoricul perechii NPC-jucator, nu starea globala `NPCContext.relationshipStatus`. Prietenii primesc un salut cald, rivalii unul prudent, iar un jucator deja cunoscut nu mai este tratat ca strain. Preferintele `formal` si `direct` aleg variante de salut rotite, nu o singura replica fixa. Cand providerul AI nu este configurat, replica locala ramane activa.

Clasificarea intentiei potriveste cuvinte intregi: „pa” nu mai activeaza despartirea in cuvinte precum „parere”. Un salut urmat de o intrebare sau o cerere nu mai ascunde intentia principala. Intrebarile necunoscute primesc un raspuns prudent, iar afirmatiile necunoscute o continuare de conversatie, in locul unui salut repetat. Rotatia template-urilor evita repetarea imediata dupa epuizarea variantelor, dar fallback-ul local ramane limitat la intentiile si faptele implementate; nu inlocuieste un provider AI pentru intrebari deschise. Pentru diagnostic, `/ainpc debugdump ai` arata daca cheia API lipseste, daca backoff-ul este activ si motivul ultimului fallback.

Cand `OpenAIService` ajunge la fallback-ul propriu, raspunsul factual furnizat engine-ului are prioritate fata de fallback-ul generic; variantele generice se rotesc separat pentru perechea NPC-mesaj numai cand nu exista acest raspuns. Rotatia pastreaza cel mult 512 perechi active. Fara provider AI, dialogul ramane limitat la regulile si variantele locale.

Faptele profesionale folosesc acelasi resolver in engine si in fallback: o intrebare ca „cine este brutarul?” selecteaza doar NPC-urile cu meseria ceruta, inclusiv forme uzuale precum „brutarul” sau „brutarii”. Prezenta cuvintelor „cine” sau „care” nu mai reda automat lista de meserii; o meserie necunoscuta lasa dialogul sa continue prin celelalte ramuri. Povestea unui NPC este folosita numai cand numele complet sau meseria lui se potriveste subiectului, fara alegerea automata a primului NPC cunoscut.

Fallback-ul normalizeaza diacriticele si foloseste potriviri pe cuvinte intregi, la fel ca selectorul de intentii si analiza rapida a sentimentului. Un salut cu intrebare pastreaza intrebarea, iar fragmente precum „da” din „adapost” sau „urat” din „curat” nu mai declanseaza confirmari sau replici de insulta. Analiza sentimentului nu mai penalizeaza relatia pentru fragmentele „urat” din „curatat” sau „mori” din „comori”; expresiile explicite cu diacritice, precum „multumesc” sau „iti rup”, raman recunoscute dupa normalizare.

## Continuarea subiectului

`PlayerConversationContinuation` recunoaste cereri scurte complete precum „continua”, „de ce?”, „spune-mi mai mult” sau „nu am inteles”, inclusiv diacritice si formule cu „te rog”. O intrebare explicita precum „de ce ploua?” ramane o intrebare noua. Cu AI disponibil, chiar primul schimb stabil este suficient pentru trimiterea cererii scurte cu istoricul conversatiei; promptul cere explicatii sustinute de context si evitarea repetarii salutului sau a aceleiasi replici.

Fara AI, engine-ul si fallback-ul serviciului folosesc aceeasi selectie de subiect din istoricul perechii NPC-jucator. Replicile locale cer o precizare despre categoria discutata (de exemplu meserie sau vreme), fara sa inventeze detalii suplimentare. Cererile intermediare de continuare si recapitulare nu inlocuiesc subiectul; schimburile esuate sau cu replica goala sunt ignorate. Un subiect nou neclar nu reactiveaza unul mai vechi, iar lipsa contextului produce o cerere generala de clarificare. Limita ramane istoricul disponibil de maximum cinci schimburi, fara memorie suplimentara globala intre jucatori.

## Recunoasterea jucatorului

- Identitatea persistenta este UUID-ul jucatorului folosit pentru relatie, istoric, amintiri si reputatie; numele afisat poate varia si nu este folosit drept cheie de recunoastere.
- `PlayerRecognition` separa prima intalnire personala de un jucator cunoscut, prieten sau relatie tensionata. Istoricul/amintirile confirma recunoasterea chiar daca randul de relatie lipseste.
- Reputatia din regiunea NPC-ului este un semnal comunitar separat: o reputatie mare nu inseamna ca NPC-ul l-a intalnit personal.
- Obiectul vizibil din mana jucatorului este capturat pe threadul Paper pentru prompt, dar nu devine automat profesie, personalitate sau fapta memorata.
- Jucatorul isi poate declara separat un rol, un stil de comunicare si o trasatura prin `/ainpc profile`. Optiunile sunt controlate, persistate dupa UUID in `player_profiles` si citite in acelasi context asincron ca relatia; un NPC le trateaza drept auto-descriere, nu drept profesie verificata, dovada de incredere sau memorie comuna.
- `/ainpc profile show` afiseaza profilul; `/ainpc profile rol <calator|comerciant|mestesugar|explorator>`, `/ainpc profile stil <direct|prietenos|formal>` si `/ainpc profile trasatura <curios|prudent|generos>` modifica doar profilul propriu; `/ainpc profile reset` il sterge. Este necesara permisiunea `ainpc.profile` (implicit disponibila jucatorilor). Consola nu poate edita profiluri.
- Pentru saluturile locale, stilurile `formal`, `direct` si `prietenos` pot schimba formularea; o relatie tensionata are prioritate si pastreaza raspunsul prudent. Rolul si trasatura ghideaza subiectele si formularea promptului AI ca preferinte declarate, fara a modifica reputatia sau mecanica de quest.
- La intrebari precum „ma cunosti?” sau „ce stii despre mine?”, raspunsul local foloseste numele, istoricul/relatia confirmata, rolul/trasatura declarate si, numai la intrebari despre cunostintele NPC-ului, pana la trei declaratii recente cerute explicit pentru memorare. Chiar fara provider AI, NPC-ul distinge prima intalnire de o relatie cunoscuta sau tensionata, fara sa inventeze amintiri.
- La „ce fel de persoana esti?” sau „cum e firea ta?”, NPC-ul raspunde local din trasaturile Big Five ale profilului sau: cel mult doua tendinte pronuntate, in ordinea intensitatii. Cand profilul este neutru, raspunsul spune ca se adapteaza situatiei. Aceeasi descriere ajunge in contextul AI si in fallback; starea emotionala de moment ramane separata de personalitate.
- La „despre ce am vorbit?” sau „ce am discutat?”, NPC-ul poate rezuma doar subiectul ultimului schimb stabil disponibil dintre el si acel jucator (maximum cinci schimburi citite). Nu reda textul brut al mesajelor si nu pretinde ca stie intreaga istorie sau data unei conversatii anterioare. Intrebarile de recapitulare repetate sunt ignorate la alegerea subiectului.
- Promptul cere continuitate cu replicile recente si interzice amintiri inventate. Relatia per-jucator vine din DB; descrierea generica a `NPCContext` nu mai pretinde o relatie personala neconfirmata.

In conversatia directa, `tine minte ca ...` sau `ține minte că ...` salveaza o declaratie ceruta explicit de jucator pentru NPC-ul curent. `uita ce ti-am cerut sa tii minte` sterge toate declaratiile cerute explicit pentru perechea NPC-jucator; `uita ce ti-am cerut sa tii minte: <declaratia exacta>` sterge numai declaratia indicata, fara sa afecteze celelalte amintiri. Ambele cereri de stergere functioneaza chiar daca memorarea a fost dezactivata ori profilul NPC blocheaza dialogul obisnuit. Cererile primesc raspuns local, respecta cooldown-ul si nu intra in `dialog_history`; memorarea respecta si profilul de interactiune. Continutul este memorat ca afirmatie atribuita jucatorului, nu ca fapt verificat independent. Declaratiile cerute explicit nu expira automat si nu consuma limita amintirilor obisnuite; cand depasesc limita proprie, se elimina cele mai vechi. La intrebari precum `ce tii minte despre mine?`, NPC-ul poate enumera pana la trei declaratii recente; `max_prompt_statements` limiteaza numai ce primeste modelul AI, nu si acest raspuns local. `npc.personal_memory.enabled`, `max_statement_chars`, `max_prompt_statements` si `max_stored_statements` controleaza mecanismul; valorile implicite sunt `true`, `160`, `3` si `10`.

Stergerea explicita elimina, in aceeasi tranzactie, schimburile din `dialog_history` ale perechii NPC-jucator care contin textul declaratiei in mesaj sau raspuns. Compararea ignora majusculele, inclusiv pentru diacritice; semnele `%` si `_` sunt caractere literale. Astfel, o enumerare veche a declaratiilor nu reintra in contextul conversatiei dupa stergere. Curatarea este textuala si nu identifica parafrazele. Daca o stergere esueaza, tranzactia pastreaza atat declaratiile, cat si istoricul.

Pentru corectare, foloseste `corecteaza ce ti-am cerut sa tii minte: locuiesc in sat -> locuiesc la oras`. Sunt acceptate si diacriticele si separatorul `→`. NPC-ul cauta declaratia veche pentru acel jucator, actualizeaza textul si curata ecourile variantei vechi in aceeasi tranzactie. Daca varianta noua exista deja, declaratiile sunt reunite intr-o singura inregistrare. O declaratie veche inexistenta sau o corectare care nu schimba textul primeste un raspuns local fara modificari. Corectarea respecta activarea memoriei, profilul de interactiune, cooldown-ul si limita configurata pentru textul nou; declaratia veche poate avea pana la 300 de caractere chiar daca limita curenta a fost redusa. Mesajele de corectare nu intra in istoric si nu schimba relatia sau emotiile NPC-ului.

Fiecare stergere sau corectare reusita actualizeaza revizia in memorie pentru acea pereche. Inainte sa salveze un raspuns generat, `DialogManager` verifica revizia folosita la citirea contextului. Daca memoria a fost schimbata intre timp, cere repetarea intrebarii si abandoneaza raspunsul vechi, fara sa-l salveze ori sa-i aplice efectele de relatie.

`OpenAIService.isAvailable` verifica doar fereastra de offline backoff. Flagul `features.ai` si cheia API sunt verificate ulterior in `generateResponse(...)`:

- AI dezactivat sau cheie lipsa: `OpenAITextSupport.generateFallbackResponse(...)`;
- backoff detectat inainte de ramura AI: template-ul local al `DialogueEngine`;
- eroare de prompt sau request: fallback local construit din `PromptSnapshot`;
- raspuns gol sau exceptie propagata spre engine: template-ul local.

Prin urmare, un apel al `OpenAIService` nu inseamna automat request extern.
Pentru reformularea unui raspuns factual, toate ramurile de eroare sau indisponibilitate pastreaza replica factuala exacta transmisa de `DialogueEngine`, in locul fallback-ului generic.

La raspunsuri HTTP temporare (408, 409, 429 fara eroare de cota si 5xx), serviciul livreaza fallback local si pune urmatoarele apeluri in cooldown, fara a retrimite imediat aceeasi cerere. Pentru `Retry-After` valid (secunde sau data HTTP), pauza respecta cel putin durata indicata de server; in lipsa lui foloseste `openai.offline_retry_seconds` si un mic jitter. Timeout-urile intra si ele in pauza.

Erorile de autentificare/acces (401/403), modelul inexistent si codurile confirmate de cota, credit sau facturare (429) suspenda apelurile API pana la reincarcarea configuratiei. Dialogul continua local, iar `/ainpc debugdump ai` si `/ainpc test` arata codul suspendarii. Dupa corectarea cheii, modelului ori limitelor, administratorul ruleaza `/ainpc reload`. Alte erori de cerere invalida nu sunt tratate ca limitari temporare, dar nu suspenda automat serviciul: pot depinde de un singur mesaj. Diagnosticul HTTP logheaza statusul si codul erorii, nu corpul brut al raspunsului.

## Mediu, rutina si MCP

`OpenAIPromptSnapshotFactory` captureaza pe threadul Paper descrierea mediului, regiunea curenta, activitatea planificata si obiectivul imediat. Promptul distinge explicit activitatea curenta de plan si nu trebuie sa prezinte un plan drept fapt realizat. `NPCContext` descrie vremea locala, biomul, anotimpul, temperatura si lumina de la pozitia NPC-ului, plus locurile si legaturile cunoscute din world mapping.

La intrebari directe despre vreme, anotimp, temperatura, momentul zilei si evenimente speciale active, `EnvironmentFactResolver` raspunde din `EnvironmentContext` capturat pentru pozitia NPC-ului. Aceeasi sursa este folosita de `DialogueEngine` si de fallback-ul `OpenAIService`, chiar daca nu exista mapping semantic. Evenimentele speciale active sunt diferite de evenimentele istorice din `WORLD_HISTORY`: o intrebare despre ce s-a intamplat in trecut ramane pe calea istorica. Daca lumea nu este cunoscuta, resolverul nu inventeaza date de mediu; lista goala de evenimente inseamna numai ca registrul curent nu cunoaste un eveniment activ.

In fallback-ul semantic, intrebarile despre starea curenta a povestii citesc `region_story_mode.state` si `place_story_state` efective si folosesc descrierile configurate cand sunt disponibile; intrebarile despre trecut folosesc istoria configurata a locului si titlurile din `recent_events`. Starea curenta si `pool` nu sunt prezentate drept evenimente trecute, iar un eveniment recent nu este prezentat drept activ cand lipseste snapshot-ul de mediu. Cheile story fara descriere raman etichete configurate, nu descrieri narative generate automat.

Aceeasi rezolvare functioneaza si in dialogul direct fara provider AI: `DialogManager` captureaza mapping-ul locului la inceputul cererii, citeste asincron starea persistenta si cel mult doua evenimente recente numai pentru intrebari story relevante, apoi transmite raspunsul factual catre `DialogueEngine`. Intrebarile strict despre evenimente primesc titlurile evenimentelor, fara istoria statica a cladirii. Starea persistenta valida are prioritate fata de mapping; un rand cu ID diferit nu suprascrie locul curent. Daca citirea persistenta esueaza, ramane disponibila starea mapata, fara a inventa evenimente. Nu este necesara o configuratie noua pentru aceasta cale.

Sunt recunoscute si intrebari naturale despre situatia curenta: `Ce se intampla in sat?` alege regiunea, `Cum merg lucrurile in cladire?` alege place-ul curent, iar `Cum e situatia aici?` poate include ambele. Pentru aceste formulari se folosesc numai descrierile narative configurate; o cheie tehnica precum `default` nu este rostita ca explicatie. Intrebarile explicite despre cheia starii pot primi in continuare cheia. Intrebarile despre trecut, viitor, cauze sau evenimente speciale active nu folosesc raspunsul despre starea curenta. Daca nu exista o descriere pentru locul cerut, resolverul lasa dialogul sa continue fara sa inventeze una.

Intrebarile naturale pot folosi si numele complet al regiunii sau al cladirii curente, de exemplu `Cum e situatia in Valea Verde?` ori `Ce se intampla la Fierarie?`. Numele se iau din `WorldContextSnapshot` in dialogul direct si din `WORLD_LORE` in fallback-ul semantic. Doar locatia curenta poate furniza raspunsul factual; numele altui sat sau al altei cladiri nu foloseste accidental starea locului curent.

La intrebari directe despre program (`care e rutina ta?`, `ce urmeaza sa faci?`), `NpcFactResolver` foloseste `plannedRoutineActivity`; raspunsul local din `DialogueEngine`, fallback-ul `OpenAIService` si faptele din promptul AI folosesc aceeasi sursa. Daca NPC-ul vorbeste cu jucatorul, raspunsul la `ce faci?` separa discutia curenta de activitatea planificata, fara sa spuna ca planul a fost deja realizat. Cand nu exista un plan valid, spune ca programul nu este stabilit; mesajele interne despre ancore lipsa sau invalide nu sunt rostite ca activitati.

Cu `features.mcp` si `ai.orchestration.enabled` active, `OpenAIService` cere `ainpc.dialog.context` pentru regiunea NPC-ului. Sidecar-ul citeste doar snapshotul runtime si poate furniza numarul NPC-urilor inregistrate in acea regiune si maximum cinci exemple din esantionul exportat. Exemplele nu sunt lista completa. Contextul MCP nu este folosit daca regiunea lipseste, snapshotul este invechit sau sidecar-ul nu raspunde; dialogul ramane functional cu datele capturate local. Serverul MCP de proiect de pe portul `3000` nu participa la aceasta cale runtime.

Pentru comportament efectiv de rutina activeaza `routine.enabled` (implicit `false`); pentru nevoi periodice activeaza separat `simulation.enabled` (implicit `false`). Completarea dialogului prin MCP necesita si `mcp.enabled`, sidecar-ul in profil `local-bridge` cu snapshot comun si un provider AI configurat; `ai.orchestration.enabled` este implicit `false`.

## Post-procesare

Pentru un raspuns nevid, `DialogManager`:

1. clasifica local sentimentul mesajului prin `OpenAITextSupport.analyzeSentimentFast(...)`;
2. salveaza perechea player-replica in `dialog_history`;
3. inregistreaza o decizie de branch generica;
4. actualizeaza direct relatia player-NPC din `npc_relationships`;
5. creeaza o memorie numai pentru sentimente cu importanta de cel putin 2;
6. cere `EmotionManager` sa aplice efectul emotional si sa persiste emotiile asincron.

Multiplicatorul emotional foloseste relatia citita inaintea actualizarii curente, nu randul reincarcat dupa scriere. Erorile individuale SQL sunt in mare parte logate si absorbite, astfel replica poate fi livrata chiar daca o mutatie auxiliara esueaza.

## Limite de siguranta

- raspunsul providerului nu trece prin `AIResponseValidator`;
- `ContextRedactor` nu este conectat la promptul activ;
- promptul si preview-ul raspunsului pot fi logate conform configuratiei de diagnostic;
- `DialogAIRequestBuiltEvent` poate fi emis si pentru ramuri care vor folosi doar fallback local;
- o replica de quest deja produsa de `ScenarioEngine` nu intra obligatoriu in acest pipeline.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/DialogManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/DialogueEngine.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/OpenAIService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/ai/OpenAIPromptSnapshotFactory.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/MemoryManager.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/managers/EmotionManager.kt`

## Legaturi

- `architecture/interactiuni.md`
- `architecture/reactie-npc-jucator.md`
- `architecture/story-si-context-ai.md`
- `reference/prompt-safety-guide.md`
