# Prompt Safety Guide

Status: politica operationala; protectiile nu sunt toate conectate automat.
Actualizat: 2026-10-26.

## Reguli obligatorii

1. Construieste contextul minim pentru use case.
2. Redacteaza textul la ultimul boundary inainte de provider sau log.
3. Separa instructiunile de datele provenite din player, DB, YAML, MCP si model.
4. Valideaza raspunsul generic si apoi contractul domeniului.
5. Pastreaza AI-ul read-only pana exista aprobare si executor determinist.
6. Foloseste fallback care nu modifica stare.

## Realitatea implementarii curente

- `StoryContextSnapshot.toPromptBlock()` nu redacteaza;
- `OpenAIPromptBuilder` include nume, mesaje, istoric, memorii si context story ca text; in dialogul OpenAI, regulile stabile de la inceput sunt trimise separat prin `instructions`, iar datele raman in `input`;
- alegerea campului `instructions` este explicita in ruta de dialog, fara promovarea automata a unui prefix primit de `generateAsync`;
- `OpenAITextSupport` respinge un `status` explicit nefinalizat sau cu tip invalid inainte de a returna textul; JSON-ul structurat cere in continuare `completed`, iar textul legacy fara camp `status` ramane acceptat;
- mesajele interne sunt verificate inaintea shortcut-ului `output_text`: un status explicit diferit de `completed`, un rol explicit diferit de `assistant` sau un continut `refusal` respinge intreaga generare;
- campurile de text generate accepta numai stringuri; numerele, booleenii, obiectele si array-urile nu sunt convertite in replici sau JSON structurat;
- fazele explicite ale mesajelor Responses sunt respectate: `commentary` nu devine replica, iar textul direct agregat nu poate ocoli selectia mesajelor finale;
- `ContextRedactor` exista, dar nu este apelat de fluxul de prompt din `src/main`;
- `AIResponseValidator` exista, dar `AIOrchestrationService.orchestrate(...)` nu il apeleaza;
- `AISafetyLabel` este derivat in principal din confidence/status si nu inlocuieste validarea continutului;
- filtrul MCP redacteaza numai campuri selectate din snapshot-ul cunoscut sidecar-ului.

Separarea campurilor API respecta [contractul OpenAI pentru instructiuni](https://developers.openai.com/api/docs/guides/text#message-roles-and-instruction-following), dar nu garanteaza rezistenta completa la prompt injection sau corectitudinea raspunsului unui model. Promptul complet pentru Ollama si inspectia locala pastreaza aceleasi reguli si date. Apelurile generice si sugestiile structurate nu primesc automat rolul de dialog NPC.

## Date conversationale citate

`OpenAIPromptBuilder` serializeaza textul mesajului curent, replicile din istoricul recent si reperele pentru continuare, reparare sau raspuns scurt ca stringuri JSON complete. Etichetele vorbitorilor si ale sectiunilor raman ale aplicatiei; ghilimelele, backslash-urile, CR/LF si controalele din text sunt escapate. Un mesaj pe mai multe linii nu mai poate introduce fizic un nou titlu de sectiune ori o replica sub eticheta altui vorbitor. Promptul cere explicit ca aceste citate sa fie tratate drept dialog, nu roluri sau reguli noi.

Se pastreaza selectia si ordinea istoricului stabil, normalizarea si limitele existente ale replicilor/reperelor, aplicate inaintea citarii. Serializarea nu modifica snapshot-ul sau istoricul persistent si nu scurteaza mesajul curent. Escape-urile pot mari lungimea reprezentarii; nu este un buget total de tokeni. Promptul fallback foloseste aceeasi citare pentru mesaj si pastreaza salutul implicit. Marcajul final al NPC-ului ramane neschimbat pentru retry-ul conversational limitat.

Datele citate raman in `input`, inclusiv la retry; `instructions` contine numai regulile stabile. Aceeasi reprezentare ajunge in promptul complet Ollama, fara introducerea unor mesaje API cu rol privilegiat. Apelurile generice `generateAsync`, replicile livrate jucatorului si fallback-urile textuale nu sunt convertite automat la acest format. Separarea este o intarire locala a [limitelor dintre instructiuni si context](https://developers.openai.com/api/docs/guides/prompt-engineering), nu o garantie impotriva prompt injection sau a unei interpretari gresite de catre model.

Protectia vizeaza textul conversational, nu rescrie numele/metadatele, profilul, memoriile, story-ul ori contextul MCP ca JSON si nu conecteaza `ContextRedactor`. Citatele nu confirma adevarul afirmatiilor si nu autorizeaza actiuni de joc. Testele verifica structura fizica, decodarea JSON, conservarea datelor si separarea cererii, fara evaluare pe un provider real.

## Erori temporare si cooldown OpenAI

`OpenAIService` nu retrimite imediat o cerere esuata. Erorile temporare HTTP (408, 409, 429 si 5xx) si timeout-urile/erorile de conectare activeaza fallback-ul local si un cooldown comun dialogului si apelurilor OpenAI generice. Esecurile repetate dubleaza baza din `openai.offline_retry_seconds`, pana la 300 de secunde; o baza configurata mai mare ramane respectata. La baza se adauga jitter de 100-500 ms.

Un `Retry-After` valid este un minim, nu un plafon: o valoare mai mare decat baza calculata este respectata integral, inclusiv peste cinci minute. Cooldown-ul activ nu poate fi scurtat de o eroare ulterioara cu un termen mai mic. Headerul accepta secunde intregi nenegative sau data HTTP; numerele care depasesc reprezentarea `Long` satureaza termenul, nu revin la pauza scurta. Un header invalid foloseste baza progresiva existenta. Politica respecta [documentatia oficiala OpenAI pentru rate limits](https://developers.openai.com/api/docs/guides/rate-limits).

Contorul esecurilor si termenul sunt actualizate impreuna, sincronizat. Un succes pornit dupa cooldown reseteaza progresia; raspunsul tardiv al unei cereri pornite inaintea unui cooldown nou nu il sterge. Un client nou pentru alta cheie efectiva sau alt endpoint primeste un gate separat; simpla editare a configuratiei nu reseteaza gate-ul clientului vechi. Un simplu raspuns malformat sau un refuz de model nu este eroare de retea si nu mareste progresia. Codurile cunoscute de cota, billing ori autentificare raman suspendari care cer interventie, nu retry-uri temporare; politica Ollama nu este schimbata.

Nu exista aici o coada sau un scheduler de retry: dupa expirare, urmatoarea cerere eligibila poate incerca din nou. Testele locale verifica politica si metodele serviciului cu erori simulate, fara asteptari reale sau apeluri externe.

### Admisie pentru fiecare incercare Responses

`callOpenAI` reciteste sincronizat gate-ul comun dupa construirea requestului, inaintea predarii catre transport. Verificarea initiala a dialogului sau a `generateAsync` nu este suficienta: o alta cerere poate activa intre timp cooldown-ul ori suspendarea. Aceeasi verificare finala se aplica retry-ului conversational pentru format/repetitie si clientilor recreati la reload pentru acelasi backend.

O incercare blocata local foloseste motivul `offline_backoff` sau `api_suspended`. Dialogul intoarce fallback-ul local cu istoric de fallback, iar apelul generic intoarce `null`. Amanarea nu este inregistrata drept eroare de provider, nu mareste progresia esecurilor, nu sterge diagnosticul ultimei erori si nu modifica termenul deja impus. `recordPrompt` este apelat numai dupa aceasta admisie; un log de pregatire a requestului nu dovedeste ca acesta a fost trimis.

La termenul exact al cooldown-ului, admisia se redeschide daca nu exista suspendare, fara resetarea automata a progresiei. Probele explicite de model raman independente de gate-ul generarii. Cererile deja admise nu sunt anulate cand gate-ul se inchide; transportul poate continua asincron, iar un succes vechi nu sterge un cooldown nou. Nu se tine lock-ul pe durata HTTP si nu se adauga aici un plafon de concurenta, o coada, un retry automat sau un buget total pentru dialog. Politica pastreaza [cerinta OpenAI de respectare a `Retry-After`](https://developers.openai.com/api/docs/guides/rate-limits), fara a pretinde control asupra cererilor altor clienti/aplicatii.

## Termen limita pentru cererile HTTP OpenAI

`OpenAIHttpTransport` limiteaza fiecare cerere la `openai.read_timeout`, inclusiv asteptarea headerelor si citirea completa a corpului raspunsului. Generarea Responses si proba HTTP de model folosesc acelasi transport; proba primeste valoarea configurata de serviciu. Supraincarcarea existenta a probei cu cinci argumente ramane disponibila, cu limita implicita de 120 de secunde. Timeout-ul de conectare ramane separat.

La expirare, transportul solicita anularea schimbului HTTP si arunca `HttpTimeoutException`. Generarea urmeaza fallback-ul si cooldown-ul existente, fara retrimitere imediata; proba intoarce status inaccesibil, fara sa modifice gate-ul de generare. Intreruperea firului anuleaza schimbul, iar apelantii restabilesc flag-ul de intrerupere. Anularea nu este reclasificata drept timeout sau defect de conectare, iar exceptiile I/O asincrone isi pastreaza tipul pentru backoff si diagnostic.

Limita este pentru o singura incercare HTTP, nu pentru intregul dialog: captura contextului, MCP, programarea taskului si un eventual retry conversational au timpi separati. Distinctia dintre timeout per incercare si termenul intregii operatii este explicata in [documentatia oficiala OpenAI](https://developers.openai.com/api/docs/guides/rate-limits). Cererea de anulare nu garanteaza oprirea procesarii la provider sau evitarea costurilor; [contractul JDK pentru anularea HTTP](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html#sendAsync(java.net.http.HttpRequest,java.net.http.HttpResponse.BodyHandler,java.net.http.HttpResponse.PushPromiseHandler)) permite finalizarea asincrona a eliberarii resurselor. Testele folosesc un server pe `127.0.0.1` cu headere intarziate ori corp partial blocat si future-uri controlate local, nu un provider extern.

## Limita de dimensiune a raspunsului HTTP

Transportul OpenAI accepta cel mult 2 MiB (2.097.152 octeti) de corp pentru fiecare raspuns, inclusiv metadatele si envelope-ul JSON, nu doar textul util. Limita este locala si separata de `max_output_tokens`, care limiteaza [tokenii generati de model](https://developers.openai.com/api/docs/guides/token-counting), nu acumularea unui corp HTTP arbitrar. Se aplica atat generarii Responses, cat si probei de model, inclusiv raspunsurilor de eroare si transferului chunked fara `Content-Length`.

Subscriberul numara cumulativ octetii ramasi in fiecare buffer, inainte de a-i preda decodorului JDK. Corpul exact la limita ramane complet; la depasire, subscriberul anuleaza subscriptia si respinge intregul corp, fara text partial, payload ori cauza bruta in eroarea de limita. Nu se asteapta citirea integrala si nu se bazeaza numai pe lungimea declarata in headere. Pentru corpurile acceptate se pastreaza decodarea charset-ului realizata de handlerul JDK. [Contractul BodySubscriber](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpResponse.BodySubscriber.html) descrie fluxul ordonat de buffer-e si anularea subscriptiei cand corpul nu mai este consumat.

Depasirea la HTTP 2xx produce `IOException` generic si fallback-ul existent, fara clasificare drept timeout sau activarea cooldown-ului de retea doar din acest motiv. La non-2xx se pastreaza statusul si `Retry-After` din headere prin `OpenAIHttpException`: 401/403 raman suspendari administrative, iar erorile HTTP temporare isi pastreaza politica de pauza. Codurile si mesajele din corpul JSON supradimensionat nu sunt recuperate sau interpretate partial; o clasificare care cere acel cod, precum cota ori `model_not_found`, nu poate fi dedusa doar din corpul respins.

O proba cu corp supradimensionat raporteaza conexiune/model neconfirmate, fara modificarea gate-ului generarii sau copierea corpului in cache. Limita este pe corpul unei singure cereri, nu un plafon global de heap, concurenta, headere ori context de prompt; buffer-ele JDK si reprezentarea decodata au costuri proprii. Nu modifica Ollama, configuratia modelului sau bugetul promptului si nu garanteaza oprirea lucrului ori facturarii la provider. Testele folosesc subscriber-i controlati si HTTP pe `127.0.0.1`, inclusiv revenirea la o generare valida dupa respingerea corpului.

## Confirmarea modelului prin proba HTTP

Un HTTP 2xx nu este suficient pentru `modelAvailable=true`. `OpenAIConnectionProbe` cere un obiect JSON strict cu `id` string nevid si fara camp `error` nenul. Campul `object` poate lipsi pentru endpoint-uri compatibile minimale; daca exista, trebuie sa fie stringul `model`. Aceste campuri provin din [contractul oficial de recuperare a modelului](https://developers.openai.com/api/reference/resources/models/methods/retrieve). Campurile suplimentare sunt tolerate, iar un ID canonic diferit de aliasul configurat ramane acceptat; nu se cere intreaga schema oficiala.

Un corp gol, `null`, HTML, JSON trunchiat, un root care nu este obiect, un ID absent/cu tip gresit ori un envelope de eroare nu confirma modelul. Numele configurat nu mai este folosit drept dovada in lipsa unui ID. Sintaxa JSON lenient, precum comentarii sau chei neincadrate in ghilimele, este respinsa local la acest boundary; nu se modifica configuratia Gson din restul aplicatiei.

Pentru aceste raspunsuri 2xx invalide, proba pastreaza endpoint-ul HTTP drept accesibil, dar returneaza `modelAvailable=false`, lista de modele goala si un diagnostic generic cu statusul HTTP. Corpul invalid si detaliile exceptiei Gson nu sunt copiate in diagnostic. Rezultatul negativ inlocuieste si cache-ul probei, astfel incat o reusita anterioara sa nu ramana afisata dupa o eroare de parsare. `diagnoseConnection` si `checkConnection` se finalizeaza normal, fara activarea cooldown-ului de generare.

Validarea probei nu garanteaza permisiunea de generare, disponibilitatea unui raspuns Responses, cota, facturarea sau identitatea unui provider compatibil. Erorile HTTP, timeout-urile si intreruperile pastreaza tratarea lor existenta. Testele verifica raspunsuri sintetice si metodele asincrone publice prin HTTP pe `127.0.0.1`, fara provider extern.

## Reload si cereri deja pornite

`ServiceRegistry.reload()` reconstruieste clientul prin `OpenAIService.reloadedFromConfig()`: setarile de transport si generatie sunt capturate intr-o instanta noua, inaintea inlocuirii serviciului activ. Clientul vechi nu primeste pe rand cheia, URL-ul sau modelul nou. Constructorul trebuie sa reuseasca inainte de modificarea gate-ului existent; o configuratie de transport respinsa local nu sterge starea clientului anterior. Aceasta garantie vizeaza clientul OpenAI, nu intreaga procedura de reload a pluginului.

Inaintea alocarii clientului HTTP, constructorul cere valori strict pozitive pentru `openai.connect_timeout` si `openai.read_timeout` si construieste local un request pentru a verifica URI-ul si headerul de autentificare dupa aceleasi reguli JDK folosite la generare. Nu trimite HTTP. Un URL nesuportat/malformat ori un credential care nu poate forma headerul este respins imediat, inclusiv la pornire, nu abia la prima generare. Erorile indica numai campurile configuratiei; nu pastreaza valorile sau cauza JDK care ar putea contine secrete. Un reload respins lasa intacte setarile capturate, suspendarea, cooldown-ul si progresia gate-ului partajat.

Acesta este un preflight de constructie a transportului, nu validarea completa a configuratiei: nu verifica autentificarea la provider, modelul, cota sau conectivitatea si nu adauga restrictii de schema pentru chei ori URL-uri dincolo de regulile existente ale requestului. Cheia absenta ramane permisa pentru setup-ul inactiv. Valorile pozitive, normalizarea existenta a cheii/URL-ului si endpoint-urile HTTP/HTTPS compatibile raman acceptate. [Contractul JDK al builderului](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html) descrie verificarea URI-ului, a headerelor si a timeout-ului.

Cand cheia efectiva si URL-ul normalizat sunt aceleasi, clientii vechi si noi folosesc acelasi `OpenAIBackoffState`, cu sincronizare pe starea partajata. Se pastreaza termenul `Retry-After` si progresia esecurilor, inclusiv la reload-uri repetate sau la schimbarea modelului, timeout-urilor ori temperaturii. O eroare intarziata a unei cereri deja pornite este vazuta si de noua instanta; o reusita veche nu poate sterge un cooldown ulterior. Comparatia foloseste cheia si URL-ul dupa sanitizarea existenta, nu textul brut din YAML.

Schimbarea cheii efective sau a endpoint-ului incepe un gate separat numai la construirea clientului nou; erorile vechii instante nu suspenda noul backend. Reload-ul manual pastreaza politica existenta de stergere a suspendarii administrative, dar nu scurteaza cooldown-ul temporar al clientului pe care este apelat.

`reloadFromConfig()` ramane resetarea locala de stare si nu recaptureaza setarile imuabile. Chiar daca YAML contine deja alta cheie, instanta existenta continua sa foloseasca cheia capturata, deci trebuie sa pastreze termenul si progresia vechiului backend. Aceasta regula protejeaza si alte instante care partajeaza acel gate. Doar `reloadedFromConfig()` construieste clientul cu setarile noi si decide partajarea ori izolarea starii. Logul resetarii locale arata modelul capturat de client, nu modelul posibil diferit din configuratia editata.

Cheia nu apare in snapshot-ul de diagnostic: este expus doar indicatorul de prezenta, iar credentialul ramane in headerul de autentificare, conform [documentatiei oficiale OpenAI](https://developers.openai.com/api/reference/overview#authentication). Testele folosesc credentiale sintetice, construiesc requesturi fara a le trimite si simuleaza finalizarea cererilor vechi; nu valideaza un reload Paper sau un provider real.

## Mesaje Responses si fallback

Parserul parcurge toate mesajele din `output` inainte de a returna textul direct sau combinat. Un mesaj explicit nefinalizat, un rol invalid ori un refuz ulterior nu pot fi mascate de un `output_text` nevid sau de o replica valida anterioara. Lipsa statusului si rolului unui mesaj legacy ramane acceptata; cand sunt prezente, inclusiv cu `null` sau tip gresit, ele trebuie sa respecte contractul. Stringurile goale ori campurile text absente/null nu sunt continut util. Itemii de reasoning si tool-urile nu sunt tratati drept replici, iar regulile de status ale mesajelor nu li se aplica.

Refuzul este semnalat prin `IOException`, fara includerea textului sau detaliilor lui in mesajul exceptiei. Nu este o eroare de format pentru retry-ul conversational si nu declanseaza singur backoff de retea. In dialog, exceptia ajunge la fallback-ul local existent; `generateAsync` pastreaza tratarea proprie a erorilor. Politica nu incearca regenerarea pentru a ocoli refuzul. Contractele sursei sunt descrise in [exemplul oficial Responses](https://developers.openai.com/api/docs/guides/migrate-to-responses) si [tratarea refuzurilor](https://developers.openai.com/api/docs/guides/structured-outputs#refusals-with-structured-outputs).

Aceste verificari nu sunt validare completa de schema sau de adevar factual. Replicile acceptate din dialog trec separat prin `DialogueResponseNovelty` si `AIResponseValidator`, inclusiv politica existenta de retry pentru repetitie sau format invalid.

### Sintaxa envelope-ului JSON

Corpul Responses trebuie sa fie un singur obiect JSON strict, inaintea verificarilor de status, mesaje sau text. Comentariile, numele fara ghilimele, stringurile cu ghilimele simple, prefixul non-executabil, controalele neescapate si escape-urile neconforme nu mai pot produce o replica. Documentele suplimentare ori sufixele non-JSON sunt respinse, nu ignorate. Regula se aplica atat dialogului, cat si `generateAsync`, inclusiv modului structurat si endpoint-urilor compatibile.

Parserul foloseste local `Strictness.STRICT` pe o copie a configuratiei Gson primite; nu modifica instanta apelantului sau parsarea profilurilor, erorilor HTTP si datelor din alte module. [Documentatia Gson](https://github.com/google/gson/blob/main/Troubleshooting.md#default-lenient) explica de ce modul implicit lenient nu este suficient. Se pastreaza compatibilitatea campurilor legacy atunci cand envelope-ul este JSON valid, spatiile JSON normale, Unicode si controalele escapate.

Esecul de sintaxa produce `IOException` cu mesaj generic, fara payload, cai JSON sau cauza Gson atasata care ar putea expune nume de campuri private ori secrete. Urmeaza fallback-ul existent, fara retrimitere de format ori backoff de retea numai din acest motiv. Strictetea vizeaza envelope-ul primit, nu interpreteaza stringul de text generat drept JSON si nu il trunchiaza in modul structurat. Nu este validare completa de schema, de chei duplicate sau de adevar factual si nu modifica promptul, modelul ori parserul Ollama. Testele folosesc payloaduri sintetice si HTTP pe `127.0.0.1`, fara provider real.

### Structura outputului si tipurile itemilor

Daca `output` este prezent, trebuie sa fie array; `null`, un obiect sau o valoare scalara explicita nu pot fi ascunse de `output_text`. Fiecare item din array trebuie sa fie obiect cu `type` string nevid. Un item `message` trebuie sa aiba `content` array, iar fiecare item din acesta trebuie sa fie obiect cu propriul `type` string nevid. Nu se convertesc numere, booleeni sau array-uri in discriminatoare de tip si nu se ignora elementele malformate dupa o replica valida.

Aceste verificari preced extragerea textului agregat sau combinat. Un refuz pus gresit intr-un obiect in loc de array nu este interpretat ca text utilizabil: structura invalida respinge intreaga generare. Exceptia este un `IOException` cu mesaj generic, fara includerea payloadului; ruta existenta foloseste fallback, fara retry de format sau backoff de retea doar din acest motiv. Structura de baza este documentata in [ghidul oficial OpenAI pentru text si output](https://developers.openai.com/api/docs/guides/text).

Compatibilitatea textului legacy ramane pentru `output` absent, array-uri goale valide, `text` legacy si mesajele fara status/rol explicit. Tipurile string necunoscute, dar bine formate, raman separate si ignorate; payloadurile reasoning/tool sau ale unor extensii viitoare nu primesc regulile unui mesaj assistant. Nu se valideaza aici intreaga schema a acelor itemi, nu se schimba politica fazelor si nu se modifica parserul Ollama.

### Faza mesajului si raspunsul final

Conform [documentatiei oficiale OpenAI pentru `phase`](https://developers.openai.com/api/docs/guides/reasoning#phase-parameter), `commentary` eticheteaza actualizari intermediare, iar `final_answer` raspunsul final. Parserul accepta aceste doua stringuri sau faza absenta/null; o faza explicita necunoscuta ori cu tip gresit respinge generarea. Metadatele sunt interpretate numai pe itemii `message`, nu pe reasoning/tool si nu prin cautarea cuvintelor in textul generat.

Continutul `commentary` nu intra in replica, dar mesajul este verificat in continuare pentru status, rol, refuz si tipul textului. Daca exista o faza explicita nenula, parserul compune raspunsul din mesajele eligibile imbricate, nu din `output_text` agregat, care ar putea contine si preambulul. Mesajele legacy fara faza sau cu `null` raman eligibile alaturi de cele finale, in ordinea primita. Un raspuns care contine numai commentary, fara text eligibil, ajunge la fallback-ul existent prin `IOException`, fara retry de format ori backoff de retea.

Fara faze explicite, prioritatea existenta a textului direct este pastrata. JSON-ul structurat eligibil nu primeste curatarea ori limita de lungime a dialogului. Aceasta protectie vizeaza parserul OpenAI folosit si de `generateAsync`; nu schimba modelul configurat, promptul de intrare, istoricul textual sau fluxul Ollama. Nu adauga replay de itemi assistant, streaming ori evaluare pe un model real.

## Limite pentru contextul optional din dialog

`OpenAIPromptBuilder` limiteaza independent continutul din cinci surse, inainte de compunerea promptului final:

| Sursa | Limita de caractere |
| --- | ---: |
| Mediu imediat | 2000 |
| Consens topologic | 2000 |
| Context semantic al lumii | 8000 |
| Story local | 4000 |
| Context MCP | 2000 |

Continutul care incape este pastrat identic. La depasire se pastreaza numai liniile complete de la inceput care incap impreuna cu marcajul `[Context partial: detalii omise din cauza limitei de lungime.]`. O prima linie prea lunga este omisa integral, nu transformata intr-o afirmatie incompleta. Limita include marcajul, dar nu titlurile si explicatiile adaugate separat de builder. Selectia se face pe fiecare sursa, nu prin interpretarea titlurilor din textul primit.

Nu se taie promptul final: regulile stabile, `NPC_DATA`, autoevaluarea din `DESPRE TINE`, istoricul recent, memoriile selectate, faptele verificate si mesajul curent nu sunt consumate de aceste cinci bugete. Limitele lor deja existente raman neschimbate. Modelului i se precizeaza ca omisiunea unui detaliu nu dovedeste absenta lui din lume. Protectia este comuna pentru dialogul OpenAI si Ollama; nu se aplica automat prompturilor generice `generateAsync`.

Plafonul story vizeaza parametrul separat `storyContext`. Proiectiile story deja incorporate in `npcDescription` de `NPCContext` nu sunt extrase sau taiate aici, pentru a pastra autoevaluarea si explicatiile deterministe ale NPC-ului.

Aceste limite sunt plafoane locale de lungime, nu un buget total de tokeni si nici o garantie ca orice prompt incape in fereastra modelului. Conform [documentatiei oficiale OpenAI despre context](https://developers.openai.com/api/docs/guides/conversation-state#managing-the-context-window), fereastra include tokenii de intrare, iesire si reasoning. Verificarea comportamentului unui model real ramane separata de testele locale de compunere si serializare.

### Profil JSON complet sau omis

Campul `profil_json_db` are un plafon separat de 1200 de caractere, aplicat JSON-ului compact dupa filtrarea existenta a `simulation.social_experience`. Un profil care incape, inclusiv exact la limita, este transmis complet. La depasire se transmite obiectul gol `{}` si un marcaj explicit de omisiune, nu un prefix JSON taiat in mijlocul unui string, escape sau fapt. Dimensiunea foloseste lungimea stringului serializat, nu numarul de caractere al valorilor inainte de escapare, de octeti UTF-8 sau de tokeni.

Obiectul gol marcat nu dovedeste absenta trasaturilor ori faptelor din profil. Nu se selecteaza euristic fragmente sau campuri dintr-un profil prea mare. Numele, ocupatia, rezumatul profilului, autoevaluarea, restul contextului si mesajul curent raman pe caile lor existente; aceasta limita nu plafoneaza automat acele campuri. Datele persistente si profilul complet din snapshot nu sunt rescrise pentru a incapea in prompt.

Politica se aplica randarii comune dialogului OpenAI si Ollama, fara schimbarea modelului, a regulilor stabile din `instructions` sau a apelurilor generice `generateAsync`. Separarea datelor si structura lizibila a promptului urmeaza [recomandarile oficiale OpenAI](https://developers.openai.com/api/docs/guides/prompt-engineering), dar aceasta corectie nu valideaza adevarul profilului si nu garanteaza rezistenta la prompt injection. Testele verifica lungimea, JSON-ul complet, escaparea, Unicode, filtrarea inaintea limitei si conservarea datelor/contextului, fara un model real.

### Fapte verificate complete sau omise

Blocul suplimentar `FAPTE VERIFICATE PENTRU ACEST RASPUNS` are un plafon propriu de 1200 de unitati UTF-16 (`String.length`), dupa normalizarea existenta a spatiilor si liniilor intr-un singur sir. Un continut care incape, inclusiv exact la limita, este redat integral. La depasire se omite intregul continut si se emite `[Fapte verificate omise: ...]`, nu un prefix care ar putea pierde o negatie, conditie sau restrictie. Nu se selecteaza euristic propozitii ori linii aparent independente dintr-un set care poate contine dependente intre fapte.

Marcajul explica faptul ca omisiunea nu dovedeste absenta faptelor si cere folosirea restului contextului confirmat sau recunoasterea lipsei detaliilor. Titlul, explicatiile si marcajul nu consuma plafonul continutului factual. Nu este un buget de tokeni sau de octeti si nu schimba limitele celorlalte surse, mesajul curent, istoricul ori `NPC_DATA`. Normalizarea si omiterea afecteaza numai randarea promptului; valoarea originala nu este rescrisa.

Politica este comuna promptului OpenAI si Ollama, iar continutul/marcajul raman in `input`, fara modificarea regulilor stabile din `instructions` sau a apelurilor generice `generateAsync`. Omisiunea nu anuleaza cererea AI si nu forteaza automat fallback-ul. Cand fluxul existent foloseste fallback-ul factual local, acesta primeste si intoarce in continuare textul original complet, fara plafonul de randare. Aceasta protectie nu verifica adevarul faptelor si nu garanteaza raspunsul unui model; testele locale verifica limitele, normalizarea, restrictiile tarzii, Unicode, conservarea contextului si fallback-ul, fara provider real.

### Memorii complete la selectie si randare

`MemoryPromptPolicy` defineste aceeasi normalizare si limita de 500 de unitati UTF-16 pentru selector si builder. CR/LF devin spatii, iar marginile sunt eliminate, ca in randarea anterioara; nu se taie continutul. Limita pe intrare include textul si metadatele `source`, `recorded_on` si `record_count`, dar nu prefixul de lista sau newline-ul final. O intrare exact la limita ramane completa, iar una mai mare este omisa integral, fara pierderea unei negatii ori conditii tarzii.

`MemoryRecallSelector` aplica limita dupa gruparea duplicatelor si adaugarea contorului. Un candidat prea lung nu ocupa un slot; selectia continua cu urmatorii candidati disponibili in ordinea de relevanta existenta. Bugetul cumulat `npc.memory_prompt_max_chars` (implicit 2400) este calculat din aceleasi intrari normalizate pe care le va reda promptul, plus `- ` si newline pentru fiecare. Nu include titlul si explicatiile sectiunii si nu reprezinta bugetul total al promptului. Sursa, data, continutul, ordinea la egalitate si obiectele candidate nu sunt rescrise.

Builderul verifica independent orice lista primita, inclusiv de la apelanti care nu folosesc selectorul. Intrarea supradimensionata nu mai este abreviata la 500; este sarita, celelalte intrari sunt pastrate, iar sectiunea primeste un singur marcaj `[Amintiri omise: ...]`. Omisiunea nu confirma absenta unor amintiri sau evenimente. Marcajul apare numai cand builderul primeste efectiv intrari prea lungi, nu pentru orice candidat exclus anterior prin relevanta, numar sau buget.

Formatul metadatelor si regulile de provenienta raman cele existente: citatele confirma ce a spus jucatorul, nu adevarul afirmatiei, iar numarul inregistrarilor nu dovedeste evenimente independente. Politica nu valideaza provenienta ori adevarul textului, nu citeaza memoriile ca JSON si nu este un filtru anti-injection sau de secrete. Afecteaza contextul recuperat si randarea comuna OpenAI/Ollama, nu stocarea memoriilor, regulile `instructions` sau `generateAsync`. Testele verifica limitele, bugetul comun, inlocuirea candidatilor prea lungi, metadatele dupa grupare, Unicode si conservarea celorlalte surse, fara provider real.

## Secrete in diagnosticele OpenAI

`OpenAITextSupport.redactDiagnosticText` mascheaza literal cheia efectiva a clientului, inclusiv variante de litere mari/mici aparute in coduri de eroare normalizate. Apoi aplica politica `secretsOnly` a `SensitiveDataRedactor`, pentru formatele de chei, Bearer, perechi secret/valoare si credentiale URI deja suportate. Nu se construieste un regex din cheia configurata, iar o cheie absenta/goala nu modifica textul obisnuit.

`OpenAIService` foloseste filtrul inainte de stocarea erorilor, motivelor de fallback/suspendare, metadatelor interactiunii si preview-urilor. Redactarea precede scurtarea preview-ului: o cheie care intersecteaza limita de 180 de caractere nu lasa un prefix vizibil doar fiindca textul a fost taiat. Logurile de diagnostic/debug trec si prin filtrarea de la boundary. URL-ul si modelul din snapshot sunt valori de afisare filtrate; setarile imuabile de transport raman intacte.

Flag-urile `openai.diagnostics.log_prompt_summary` si `log_response_preview` sunt respectate la inregistrare atat de snapshot-ul principal, cat si de preview-urile istoricului de succes, fallback si eroare. Preview-ul dezactivat este `<disabled>`; dimensiunile originale raman disponibile pentru diagnostic. Flag-urile nu elimina intreaga interactiune sau metadatele erorilor.

`OpenAIConnectionProbe` filtreaza erorile HTTP/I/O si metadatele de afisare inainte de returnarea sau cache-uirea `ConnectionStatus`, inclusiv un ID de model care ar contine un secret. Headerul `Authorization`, URL-ul cererii, corpul promptului si raspunsul folosit de aplicatie nu sunt rescrise de acest filtru de diagnostic. Testele folosesc chei sintetice si server HTTP pe `127.0.0.1`, verificand inclusiv ca headerul real primit local ramane neschimbat, fara trafic catre un provider extern.

`extractOpenAIErrorMessage` mascheaza continutul complet inaintea extrasului fallback de 200 de caractere, inclusiv pentru HTML/text, JSON fara obiect `error` sau un obiect de eroare fara detalii. Serviciul si proba transmit cheia efectiva in acest punct; mascarea ulterioara a unui prefix deja taiat nu ar putea recupera secretul complet. Filtrul poate astfel vedea si blocul complet de cheie privata ori credentialele URI cu host aflat dupa limita extrasului. Formatarea obisnuita cod/tip/mesaj ramane neschimbata in lipsa secretelor; codul brut extras separat continua sa conduca politica HTTP/backoff.

Ruta de generare transmite acelasi context de redactare si parserului Responses pentru envelope-urile de eroare primite cu HTTP 2xx. Supraincarcarile JVM anterioare pentru extractia erorii si a textului raman disponibile. Contextul de credentiale nu rescrie textul unei generari reusite si nu transforma o eroare de parser in timeout sau defect de conectare. Testele verifica explicit lipsa fragmentelor de cheie ramase la granita extrasului, nu doar absenta cheii complete.

Aceasta masura nu conecteaza `ContextRedactor` la promptul de productie si nu este redactare universala de PII sau de secrete codate/fragmentate. Exceptiile originale pot contine in continuare text brut intern; protectia vizeaza mesajele si campurile emise/stocate de aceste cai de diagnostic. Filtrul nu modifica clasificarea HTTP, a timeout-urilor sau progresia backoff-ului. Principiul pastrarii credentialelor secrete este descris in [documentatia oficiala OpenAI](https://developers.openai.com/api/reference/overview#authentication).

## Gate inainte de activarea unui flux AI

- test cu API key, UUID, email si IP in fiecare sursa de context;
- test de prompt injection prin mesaj, memorie, metadata si eveniment story;
- test de output gol, trunchiat, prea lung si cu tip gresit;
- test ca niciun draft nu executa comanda, reward, world edit sau persistenta;
- audit cu provenance real al providerului si rezultat de validator;
- fallback verificat cand providerul si MCP sunt indisponibile.

## Interzis

- tratarea unui `toPromptBlock()` drept text sigur implicit;
- folosirea marcajului `[EXECUTED]` ca dovada de executie;
- publicarea tool-urilor de scriere fara autentificare, configurare si gate runtime;
- copierea secreta a configuratiei in debugdump, prompt sau audit.

## Legaturi

- `architecture/story-context-service.md`
- `architecture/ai-orchestrare-si-mecanici.md`
- `architecture/mcp-runtime-bridge-design.md`
- `planning/questuri-avansate-v2.md`
