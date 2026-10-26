# Story si context in fluxul AI

Status: document derivat din fluxul runtime activ.
Actualizat: 2026-10-26.

Story context ajunge in dialogul contextual existent, dar nu trece prin `AIOrchestrationService`.

## Flux activ

1. `DialogManager` pregateste istoricul, memoriile si relatia player-NPC.
2. `DialogueEngine` incearca raspunsuri factuale si template-uri deterministe.
3. Cand criteriile cer ramura contextuala, `OpenAIService` captureaza un `PromptSnapshot` pe thread-ul Paper.
4. `NPCContext.generateContextDescription()` poate include `StoryContextService.buildForNpc(npc, playerul activ)`.
5. `OpenAISemanticWorldContextBuilder` include separat un snapshot construit prin `buildForNpc(npc, null)`.
6. `OpenAIPromptBuilder` adauga istoricul, memoriile si relatia, iar `McpDialogContextProvider` poate adauga context MCP read-only.
7. `OpenAIService` foloseste providerul sau un fallback local, in functie de flag, cheie, backoff si erori.

## Contractul cererii si raspunsului OpenAI

Regulile stabile din preambulul `OpenAIPromptBuilder` sunt pastrate intr-un sir comun, imuabil. Ruta de dialog le transmite explicit in campul Responses API `instructions`, cu autoritate separata de datele dinamice din `input`. Prefixul este eliminat numai de la inceputul promptului, o singura data; o copie citata in context ramane continut. Istoricul, memoriile, profilul, contextul MCP si mesajul jucatorului nu sunt promovate in instructiuni. Ollama si diagnosticul local pastreaza promptul complet. `generateAsync` si formatele structurate isi pastreaza comportamentul generic.

Mesajul curent, replicile din istoric si textele din reperele de continuare/reparare/raspuns scurt sunt citate ca stringuri JSON complete, cu controalele, ghilimelele si backslash-urile escapate. Textul pe mai multe linii nu mai formeaza fizic sectiuni ori etichete de vorbitor suplimentare. Selectia istoricului, normalizarea si limitele sale raman cele existente, aplicate inainte de citare; mesajul curent nu este trunchiat, iar snapshot-ul si istoricul persistent nu sunt rescrise. Promptul fallback citeaza si el mesajul. Citatele raman date in `input`, inclusiv la retry, nu mesaje privilegiate sau instructiuni dinamice. Aceasta formatare nu este redactare si nu garanteaza rezistenta modelului la prompt injection; celelalte surse de context nu sunt convertite automat la JSON. Detaliile sunt in `reference/prompt-safety-guide.md`.

Inaintea construirii arborelui Responses, o parcurgere stricta, iterativa verifica unicitatea numelor in fiecare obiect JSON, inclusiv in metadate necunoscute si array-uri imbricate. Duplicatele sunt respinse chiar cu valori identice ori un prim `null`; escape-urile numelor sunt decodate, fara ignorarea majusculelor sau normalizare Unicode suplimentara. Numele din obiecte diferite nu intra in conflict, iar textul generat nu este reparsat ca envelope. Esecul produce un diagnostic generic fara cheie, cale sau cauza bruta, nu un retry ori backoff de retea. Se pastreaza configuratia Gson a apelantului, proba de model, parserul Ollama si modelul configurat.

La extractia textului, un camp `status` prezent trebuie sa fie stringul `completed`. Raspunsurile `incomplete`, `failed`, `cancelled`, `queued`, `in_progress` ori statusurile cu tip invalid sunt respinse inaintea textului direct sau imbricat. JSON-ul structurat cere campul `status`; raspunsurile text legacy fara el raman acceptate. Eroarea urmeaza fallback-ul existent si nu este, singura, tratata drept defect de retea ori motiv de offline/backoff. Aceasta validare nu inlocuieste validatorul de domeniu.

Toate mesajele `output` sunt inspectate inaintea returnarii `output_text`. Statusul explicit al unui mesaj trebuie sa fie `completed`, iar rolul explicit trebuie sa fie `assistant`; lipsa lor ramane acceptata pentru mesaje legacy. Un continut `refusal`, chiar dupa text util sau intr-un mesaj ulterior, respinge generarea fara retry conversational si fara expunerea detaliilor refuzului in exceptie. Campurile text nenule trebuie sa fie stringuri: valorile numerice, booleene sau containerele JSON nu sunt convertite in text. Reasoning-ul si tool-urile raman separate de replici.

Un camp `output` prezent trebuie sa fie array de obiecte cu `type` string nevid; pentru `message`, `content` trebuie sa fie de asemenea array de obiecte cu tip string nevid. Elementele malformate nu sunt sarite si nu pot fi ascunse de textul agregat ori de un mesaj anterior valid. Structura gresita produce o eroare de parser cu diagnostic generic, nu un retry de format sau backoff de retea. Tipurile string necunoscute bine formate raman ignorate, iar output-ul absent dintr-un raspuns text legacy si array-urile goale valide isi pastreaza comportamentul anterior.

Cand mesajele au faze explicite, `commentary` este exclus din textul returnat, iar `output_text` agregat nu poate reintroduce preambulul. `final_answer` si mesajele legacy fara faza/null raman eligibile, in ordinea primita; fara faze explicite se pastreaza prioritatea textului direct. Fazele necunoscute sau cu tip invalid sunt respinse. Commentary ramane supus verificarilor de status, rol, refuz si tip al textului, iar absenta unei replici eligibile declanseaza eroarea de parsare existenta, nu o noua generare. Validarea fazelor nu modifica modelul configurat sau reprezentarea datelor de intrare.

Contractele sunt descrise in [ghidul OpenAI pentru roluri si instructiuni](https://developers.openai.com/api/docs/guides/text#message-roles-and-instruction-following) si [tratarea raspunsurilor incomplete](https://developers.openai.com/api/docs/guides/structured-outputs). Testele locale verifica serializarea, conservarea datelor, validarea statusului si fallback-ul de eroare; nu masoara calitatea unui raspuns generat de un provider real.

Checkpoint 2026-10-26: doua regresii de status esueaza inainte de corectie. Sunt adaugate sapte teste pentru status, compatibilitatea textului legacy, separarea instructiunilor si conservarea tuturor surselor dinamice. Cele 91 de teste focalizate si verificarea completa core/API/addon trec; totalul este de 3.213 teste, zero esecuri sau erori si doua omise. API/addon reutilizeaza rezultatele up-to-date, core-ul este executat din nou. JAR-urile sunt construite, iar freeze-ul static `.ai/release-reports/ai-api-prompt-boundary-20261026-api-addon-freeze.json` are zero avertismente si ABI API 1.1.0 neschimbat.

## Context optional limitat

Builderul plafoneaza separat mediul imediat (2000 caractere), consensul topologic (2000), contextul semantic (8000), parametrul story separat (4000) si contextul MCP (2000). Blocurile mici raman identice. Blocurile mari pastreaza numai prefixul format din linii complete care incap impreuna cu un marcaj de omisiune. O afirmatie prea lunga nu este taiata la mijloc, iar contextul partial nu poate fi interpretat ca lista completa a lumii.

Nu se trunchiaza promptul final si nu se consuma mesajul curent, istoricul, `NPC_DATA`, autoevaluarea ori faptele verificate din cauza acestor blocuri optionale. Story-ul deja inclus de `NPCContext` in `npcDescription` ramane neschimbat. Nu este un buget total de tokeni; detaliile si limitele protectiei sunt in `reference/prompt-safety-guide.md`.

Profilul JSON persistent are separat limita de 1200 de caractere serializate, dupa filtrarea receipt-urilor sociale brute. Daca depaseste limita, builderul emite `{}` cu marcaj de omisiune, nu un prefix JSON invalid sau o afirmatie incompleta. Profilurile care incap raman complete, iar datele persistente, snapshot-ul si celelalte campuri de identitate/context nu sunt scurtate de aceasta regula. Omisiunea nu confirma absenta unor trasaturi ori fapte.

Faptele verificate suplimentare au propriul plafon de 1200 de unitati UTF-16 dupa normalizarea existenta a spatiilor. Sunt transmise integral daca incap; altfel intregul set este omis cu un marcaj explicit, fara taierea unei conditii ori negatii si fara selectarea unor propozitii aparent independente. Restul contextului ramane, iar omisiunea nu confirma absenta faptelor si nu forteaza automat fallback-ul. Fallback-ul factual local continua sa foloseasca valoarea originala completa; aceasta limita afecteaza numai randarea comuna OpenAI/Ollama, nu datele sursa, `instructions` sau apelurile generice.

Memoriile recuperate folosesc o politica comuna intre selector si builder: cel mult 500 de unitati UTF-16 pentru intreaga intrare normalizata, inclusiv sursa, data si contorul duplicatelor. Selectorul sare intrarile prea lungi dupa grupare si poate umple sloturile cu alti candidati disponibili; bugetul cumulat foloseste aceeasi forma normalizata, plus formatarea listei. Builderul nu mai taie o memorie acceptata de selector si verifica separat listele primite direct, omitand intrarea prea lunga cu un marcaj explicit. Metadatele raman lizibile, iar memoria persistenta si regulile de provenienta nu sunt modificate. Aceasta este o protectie de integritate a contextului, nu validarea adevarului sau un filtru anti-injection.

Declaratiile personale retinute explicit sunt normalizate ca inainte si redate ca stringuri JSON complete sub atribuirea `Mi-ai spus ca:`. Limita de 400 de unitati UTF-16 se aplica inaintea escaparii; o declaratie prea lunga este omisa integral cu un singur marcaj explicativ pentru selectie, nu trunchiata. Selectia initiala de cel mult 0-10 declaratii isi pastreaza ordinea si nu este completata din intrari mai vechi dupa omisiune; intrarile goale sunt ignorate. Datele persistente si parserul comenzilor personale raman neschimbate. Citatele si marcajul sunt date in `input`, comune OpenAI/Ollama; confirma doar ce a spus jucatorul, nu adevarul afirmatiei, si nu garanteaza protectie impotriva prompt injection.

Checkpoint context optional 2026-10-26: cinci teste noi, dintre care trei esueaza inainte de implementare, verifica plafoanele independente, conservarea contextului mic, omisiunea unei linii factuale supradimensionate, CRLF, text Unicode si limita exacta. Trec cele 69 de teste tintite, apoi verificarea completa core/API/addon si construirea JAR-urilor: 3.218 teste, zero esecuri sau erori, doua omise. Core-ul este executat din nou; rezultatele API/addon sunt up-to-date. Freeze-ul static `.ai/release-reports/ai-optional-context-limits-20261026-api-addon-freeze.json` trece fara avertismente, cu acelasi ABI API 1.1.0. Nu s-au facut requesturi catre modele reale.

Checkpoint mesaje Responses 2026-10-26: opt teste noi, dintre care sase esueaza inainte de corectie, acopera statusul si rolul mesajelor, refuzuri dupa text valid, bypass prin text direct, tipuri JSON invalide, separarea reasoning/tool, compatibilitatea legacy si absenta retry-ului conversational la refuz. Trec cele 97 de teste tintite, apoi verificarea completa core/API/addon si construirea JAR-urilor: 3.226 teste, zero esecuri sau erori, doua omise. Core-ul este executat din nou; API/addon sunt up-to-date. Freeze-ul static `.ai/release-reports/ai-response-message-validation-20261026-api-addon-freeze.json` are zero avertismente si ABI API 1.1.0 neschimbat. Nu s-au facut apeluri catre modele reale.

## Cooldown dupa erori OpenAI

Dialogul si `generateAsync` folosesc acelasi backoff temporar pentru erori HTTP eligibile, timeout-uri si probleme de conectare. Baza configurata creste exponential dupa esecuri consecutive, cu plafon de 300 de secunde sau minimul configurat mai mare, plus jitter. `Retry-After` poate impune un termen mai lung si nu este redus de plafon; secventele numerice prea mari satureaza termenul in loc sa permita retry prematur. Nu exista retrimitere imediata sau coada automata.

Contorul si termenul sunt actualizate sincronizat. Succesul eligibil reseteaza progresia; succesul unei cereri vechi nu poate sterge un cooldown aparut dupa pornirea ei. Erorile de parser nu cresc backoff-ul de retea, iar suspendarea pentru cota sau autentificare ramane separata. Detaliile si limitele testarii locale sunt in `reference/prompt-safety-guide.md`.

Fiecare incercare Responses reciteste gate-ul comun dupa pregatirea requestului, inclusiv un retry conversational. Daca o alta cerere a activat intre timp cooldown-ul ori suspendarea, incercarea este amanata local fara HTTP si fara o noua eroare de provider; dialogul foloseste fallback, iar apelul generic intoarce `null`. Admisia nu reseteaza progresia la simpla expirare si nu anuleaza cererile deja admise. Proba explicita de model ramane disponibila fara sa reseteze gate-ul generarii; nu se adauga o coada sau limita noua de concurenta.

La reload, registry-ul recreeaza clientul cu setarile noi, dar partajeaza `OpenAIBackoffState` cu instanta anterioara daca cheia efectiva si URL-ul normalizat nu se schimba. Schimbarea modelului sau a timeout-urilor nu permite ocolirea cooldown-ului. Cererile vechi pentru acelasi backend pot prelungi gate-ul comun; dupa schimbarea cheii ori a endpoint-ului, ele raman izolate de gate-ul nou. Sincronizarea este pe starea partajata, nu pe fiecare instanta de serviciu. Constructorul nou este evaluat inaintea inlocuirii clientului si a resetarii suspendarii administrative.

Apelul direct `reloadFromConfig()` sterge numai suspendarea administrativa si pastreaza cooldown-ul/progresia, chiar daca intre timp cheia din YAML s-a schimbat. Instanta nu si-a schimbat credentialele capturate, deci nu poate elibera gate-ul vechi sau clientii care il partajeaza doar pe baza fisierului editat. Recapturarea setarilor si alegerea unui gate nou raman responsabilitatea fabricii `reloadedFromConfig()`; diagnosticul resetarii locale afiseaza modelul real al instantei.

## Cereri HTTP cu termen limita

`OpenAIHttpTransport` aplica `openai.read_timeout` fiecarei cereri Responses si probei HTTP de model, inclusiv atunci cand headerele au sosit dar corpul ramane blocat. La timeout sau intrerupere solicita anularea schimbului; timeout-ul generarii urmeaza fallback-ul si cooldown-ul existente, fara retry imediat. Proba esuata nu modifica gate-ul generarii. Limita este per incercare HTTP, nu pentru intregul dialog sau pregatirea contextului; anularea nu garanteaza oprirea procesarii la provider. Contractul si limitele sunt in `reference/prompt-safety-guide.md`.

## Diagnostic fara credentiale

Serviciul mascheaza cheia efectiva si formatele de secrete suportate inaintea stocarii erorilor, motivelor de fallback, metadatelor si preview-urilor. Preview-urile sunt redactate inainte de scurtare, iar optiunile de dezactivare sunt respectate si in istoricul interactiunilor. Proba HTTP mascheaza datele de afisare din `ConnectionStatus` inainte de cache. Headerul de autentificare, setarile de transport si textul cererii/raspunsului aplicatiei nu sunt rescrise de aceasta protectie locala; ea nu inlocuieste redactarea contextului trimis providerului. Detaliile si limitele sunt in `reference/prompt-safety-guide.md`.

Extractia mesajului de eroare primeste cheia efectiva inainte de construirea extrasului fallback de 200 de caractere. Redactarea corpului complet precede astfel taierea, inclusiv pentru raspunsuri de proxy, credentiale URI si blocuri de chei private. Parserul Responses primeste acelasi context pentru erori incluse in raspunsuri HTTP 2xx, fara rescrierea textului unei generari reusite. Clasificarea foloseste in continuare codurile brute separate, iar entry point-urile JVM existente raman disponibile.

Proba confirma modelul numai dintr-un obiect JSON strict cu ID string nevid, fara eroare nenula si cu `object: model` daca acest camp este prezent. Un 2xx cu corp invalid ramane un endpoint HTTP accesibil, dar nu un model confirmat. Rezultatul negativ este returnat si cache-uit, fara corp brut, exceptie Gson expusa sau modificarea cooldown-ului de generare. Aliasurile rezolvate la un ID canonic si raspunsurile minimale cu ID raman acceptate; proba nu garanteaza succesul unei generari Responses.

## Consecinte

- snapshot-ul semantic construit cu player `null` nu include ancorele quest active ale playerului;
- blocul din `NPCContext` poate include playerul, inclusiv numele si story context-ul lui;
- acelasi prompt poate contine proiectii story cu scope diferit;
- inchiderea sesiunii nu goleste imediat playerul din `NPCContext`;
- `ContextRedactor` nu este apelat automat;
- replicile de dialog sunt validate prin `DialogueResponseNovelty` si `AIResponseValidator`, dar aceasta verificare nu este aplicata automat apelurilor generice `generateAsync`;
- `OpenAIService.isAvailable` nu confirma flag activ sau cheie configurata.

## Flux neconectat

`AIOrchestrationService` contine use case-uri pentru dialog, story, quest si build-plan drafts, dar nu are apelant in codul de productie. El ramane scaffold dezactivat implicit, nu etapa activa a dialogului sau authoring-ului.

## Regula

- story state-ul persistent ramane autoritatea;
- contextul serializat este continut, nu tranzactie si nu dovada de validare;
- orice integrare viitoare trebuie sa conecteze explicit redactarea, validatorul si serviciul determinist care detine efectul.

## Legaturi

- `architecture/dialog-si-conversatii.md`
- `architecture/story-state-service.md`
- `architecture/story-context-service.md`
- `architecture/harta-clase-context.md`
- `architecture/ai-orchestrare-si-mecanici.md`
- `reference/prompt-safety-guide.md`
