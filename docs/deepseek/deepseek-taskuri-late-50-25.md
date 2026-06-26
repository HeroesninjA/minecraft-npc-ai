# DeepSeek Taskuri - Batch 25 (L1251-L1300)

Actualizat: 2026-06-26

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Indexare si navigare

### L1251 Index pentru batch-ul 25
**Descriere tehnica:** Actualizeaza indexurile ca noul batch sa fie descoperit imediat din documentatia principala.
**Scop:** Face batch-ul 25 vizibil fara cautare manuala.
**Target:** `./deepseek-batch-guide.md`, `./README.md`, `../README.md`.
**Prompt AI:** Adauga referinta pentru batch-ul DeepSeek 25 in indexurile relevante.
**Acceptare:** Batch-ul nou apare in toate indexurile canonice.

### L1252 Link de intrare pentru batch-ul activ
**Descriere tehnica:** Leaga sumarul seriei active direct de fisierul nou creat.
**Scop:** Reduce salturile de context la deschiderea seriei.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Actualizeaza sumarul seriei active cu batch-ul DeepSeek 25.
**Acceptare:** Fisierul activ este vizibil dintr-o singura referinta.

### L1253 Nota pentru intervalul L1251-L1300
**Descriere tehnica:** Adauga un rezumat scurt care indica exact intervalul numeric acoperit de batch.
**Scop:** Evita confuziile intre batch-uri vecine.
**Target:** `./deepseek-active-series-summary.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie o nota scurta pentru intervalul L1251-L1300.
**Acceptare:** Intervalul este mentionat explicit si corect.

### L1254 Punct de intrare pentru citire
**Descriere tehnica:** Stabileste ordinea recomandata de citire pentru batch-ul nou.
**Scop:** Face navigarea mai rapida pentru oameni si AI.
**Target:** `./README.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un punct de intrare clar pentru batch-ul DeepSeek 25.
**Acceptare:** Ordinea de citire este usor de urmat.

### L1255 Backlink catre ghid
**Descriere tehnica:** Asigura legatura din batch catre ghidul central de reguli si format.
**Scop:** Pastreaza regulile aproape de taskuri.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Adauga un backlink util catre ghidul DeepSeek.
**Acceptare:** Cititorul ajunge rapid la regulile canonice.

### L1256 Referinta pentru changelog
**Descriere tehnica:** Leaga extinderea batch-ului de istoricul operational din changelog.
**Scop:** Pastreaza traseul de schimbare auditat.
**Target:** `../../CHANGELOG.md`.
**Prompt AI:** Scrie o intrare scurta in changelog pentru batch-ul DeepSeek 25.
**Acceptare:** Intrarea mentioneaza fisierele si motivul schimbarii.

### L1257 Sumar de teme pentru serie
**Descriere tehnica:** Grupeaza rapid temele acoperite de batch pentru a simplifica trierea ulterioara.
**Scop:** Ajuta la orientare fara lectura lunga.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Adauga un sumar scurt al temelor pentru batch-ul 25.
**Acceptare:** Teme principale si scopul lor apar clar.

### L1258 Index de cautare pentru taskuri
**Descriere tehnica:** Creeaza o referinta care ajuta la gasirea rapida a taskurilor dupa numar sau categorie.
**Scop:** Reduce timpul de cautare in documentatie.
**Target:** `./README.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Adauga un indiciu de cautare pentru batch-ul DeepSeek 25.
**Acceptare:** Cautarea batch-ului este directa si ambigua zero.

### L1259 Nota pentru ordine stricta
**Descriere tehnica:** Marcheaza faptul ca executia ramane controlata de cursor si nu de ordinea vizuala a fisierelor.
**Scop:** Evita executia in afara fluxului.
**Target:** `./deepseek-execution-cursor.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie o nota despre ordinea stricta de executie pentru batch-ul 25.
**Acceptare:** Regula cursorului este explicita.

### L1260 Nota pentru fisiere nepublicate
**Descriere tehnica:** Marcheaza orice fisier nou care nu trebuie consumat ca sursa operationala pana la indexare completa.
**Scop:** Evita utilizarea prematura a batch-ului.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Adauga o nota despre consumul doar dupa indexare completa.
**Acceptare:** Fisierul nou nu este tratat ca sursa partiala.

## Arhivare si restaurare

### L1261 Politica pentru mutare in arhiva
**Descriere tehnica:** Stabileste cand un batch activ poate fi mutat in arhiva fara sa rupa trasabilitatea.
**Scop:** Pastreaza seria curata pe termen lung.
**Target:** `./deepseek-batch-guide.md`, `./arhiva/README.md`.
**Prompt AI:** Scrie o politica pentru mutarea batch-urilor DeepSeek in arhiva.
**Acceptare:** Conditiile de mutare sunt clare si verificabile.

### L1262 Checklist pentru backup inainte de mutare
**Descriere tehnica:** Listeaza verificarile minime inainte de orice mutare a documentatiei active.
**Scop:** Reduce riscul de pierdere a contextului.
**Target:** `./deepseek-taskuri-archive-audit-2026-06-25.md`.
**Prompt AI:** Creeaza o checklist pentru backup inainte de mutare.
**Acceptare:** Verificarile esentiale apar intr-o ordine utila.

### L1263 Raport pentru restaurare temporara
**Descriere tehnica:** Standardizeaza raportul folosit cand un batch istoric este adus temporar inapoi in lucru.
**Scop:** Face restaurarea trasabila.
**Target:** `./deepseek-taskuri-archive-verification-2026-06-25.md`.
**Prompt AI:** Scrie un raport scurt pentru restaurarea temporara a unui batch.
**Acceptare:** Raportul include sursa, scop si durata.

### L1264 Regula pentru linkuri relative
**Descriere tehnica:** Forteaza folosirea linkurilor relative corecte intre active si arhiva.
**Scop:** Evita linkuri rupte dupa mutare.
**Target:** `./deepseek-batch-guide.md`, `./arhiva/README.md`.
**Prompt AI:** Scrie o regula pentru linkurile relative dintre batch-uri.
**Acceptare:** Calea corecta este specificata fara ambiguitate.

### L1265 Nota pentru reactivare batch
**Descriere tehnica:** Defineste cum este readus temporar un batch din arhiva fara a-l transforma iar in activ.
**Scop:** Pastreaza distinctia dintre istoric si activ.
**Target:** `./arhiva/README.md`.
**Prompt AI:** Adauga o nota despre reactivarea temporara a unui batch.
**Acceptare:** Reactivarea nu schimba statutul arhivei pe termen lung.

### L1266 Raport comparativ vechi / nou
**Descriere tehnica:** Ofera un format scurt pentru comparatia intre fisierul original si versiunea mutata.
**Scop:** Face verificarea post-mutare mai rapida.
**Target:** `./deepseek-taskuri-archive-audit-2026-06-25.md`.
**Prompt AI:** Creeaza un raport comparativ pentru mutarea unui batch.
**Acceptare:** Diferentele relevante sunt listate clar.

### L1267 Curatare de referinte duplicate
**Descriere tehnica:** Identifica referintele care trimit simultan la fisierul activ si la copia din arhiva.
**Scop:** Previne confuzia de sursa.
**Target:** `./deepseek-batch-guide.md`, `../README.md`.
**Prompt AI:** Creeaza o regula pentru eliminarea referintelor duplicate.
**Acceptare:** Ramane o singura sursa canonica pentru fiecare batch.

### L1268 Validare de ruta arhiva
**Descriere tehnica:** Verifica daca toate trimiterile folosesc calea canonica din subfolderul arhiva.
**Scop:** Pastreaza mutarile consecvente.
**Target:** `./arhiva/README.md`.
**Prompt AI:** Scrie o validare pentru rutele batch-urilor din arhiva.
**Acceptare:** Orice ruta gresita poate fi detectata usor.

### L1269 Inventar de dependente
**Descriere tehnica:** Listeaza documentele care depind de batch-ul curent inainte de o mutare sau rescriere.
**Scop:** Evita ruperea lantului de referinte.
**Target:** `./deepseek-active-series-summary.md`, `../README.md`.
**Prompt AI:** Creeaza un inventar scurt pentru dependentele unui batch DeepSeek.
**Acceptare:** Dependentele critice sunt enumerate.

### L1270 Nota finala pentru arhivare
**Descriere tehnica:** Rezuma ce inseamna un batch complet mutat in arhiva si cum ramane urmaribil.
**Scop:** Inchide clar ciclul de mutare.
**Target:** `./arhiva/README.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie o nota finala pentru ciclul de arhivare DeepSeek.
**Acceptare:** Starea finala si traseul de referinta sunt clare.

## Audit si integritate

### L1271 Audit pentru numar de taskuri
**Descriere tehnica:** Verifica daca batch-ul are exact 50 de taskuri si nu lipseste nicio intrare numerotata.
**Scop:** Pastreaza integritatea intervalului.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru numarul de taskuri din batch-ul 25.
**Acceptare:** Auditul identifica lipsa sau suprascrierea numerelor.

### L1272 Audit pentru campurile obligatorii
**Descriere tehnica:** Verifica prezenta campurilor `Descriere tehnica`, `Scop`, `Target`, `Prompt AI`, `Acceptare`.
**Scop:** Evita taskurile incomplete.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru campurile obligatorii ale taskurilor DeepSeek.
**Acceptare:** Orice task lipsa este marcat explicit.

### L1273 Audit pentru consistenta targetului
**Descriere tehnica:** Verifica daca `Target` indica zona tehnica real afectata si nu o descriere vaga.
**Scop:** Face taskurile actionabile.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru consistenta campului Target.
**Acceptare:** Targeturile neclare sunt listate separat.

### L1274 Audit pentru scop masurabil
**Descriere tehnica:** Verifica daca `Scop` descrie un rezultat verificabil si nu o intentie generala.
**Scop:** Pastreaza taskurile evaluabile.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru formularea masurabila a scopului.
**Acceptare:** Scopurile prea vagi sunt marcate pentru rescriere.

### L1275 Audit pentru prompt executabil
**Descriere tehnica:** Verifica daca instructiunea din `Prompt AI` cere o actiune concreta si unica.
**Scop:** Evita taskurile imposibil de rulat.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru prompturile executabile din batch-ul 25.
**Acceptare:** Prompturile ambigue sunt identificate.

### L1276 Audit pentru acceptare clara
**Descriere tehnica:** Verifica daca criteriile de acceptare pot fi validate fara interpretare libera.
**Scop:** Face inchiderea taskurilor obiectiva.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru criteriile de acceptare ale taskurilor DeepSeek.
**Acceptare:** Criteriile prea generale sunt semnalate.

### L1277 Audit pentru categorie si tema
**Descriere tehnica:** Verifica daca taskul se potriveste temei batch-ului si nu amesteca zone tehnice distincte.
**Scop:** Pastreaza structura batch-ului curata.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru categoria si tema taskurilor DeepSeek.
**Acceptare:** Taskurile in afara temei sunt marcate.

### L1278 Audit pentru referinte proaspete
**Descriere tehnica:** Detecteaza referintele la fisiere vechi sau mutate care pot induce eroare.
**Scop:** Reduce drift-ul de documentatie.
**Target:** `./deepseek-batch-guide.md`, `../README.md`.
**Prompt AI:** Creeaza un audit pentru referintele stale din documentatia DeepSeek.
**Acceptare:** Referintele invechite apar in raport.

### L1279 Audit pentru fisiere lipsa
**Descriere tehnica:** Verifica daca toate fisierele mentionate in indexuri exista efectiv in repo.
**Scop:** Evita legaturi false in documentatie.
**Target:** `./README.md`, `../README.md`.
**Prompt AI:** Creeaza un audit pentru fisierele lipsa din indexurile DeepSeek.
**Acceptare:** Orice referinta care nu exista este listata.

### L1280 Audit pentru titluri duplicate
**Descriere tehnica:** Detecteaza taskurile cu titluri aproape identice care pot crea confuzie la cautare.
**Scop:** Pastreaza unicitatea intrarilor.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru titlurile duplicate din batch-ul DeepSeek 25.
**Acceptare:** Duplicatele sunt semnalate cu numerele lor.

## Handoff si raportare

### L1281 Handoff pentru batch-ul curent
**Descriere tehnica:** Creeaza un rezumat scurt al ce acopera batch-ul si ce nu trebuie uitat la citire.
**Scop:** Ajuta la predarea contextului.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Scrie un handoff scurt pentru batch-ul DeepSeek 25.
**Acceptare:** Contextul esential incape intr-un singur bloc.

### L1282 Handoff pentru urmatorul batch
**Descriere tehnica:** Noteaza ce tip de continut ar trebui continuat dupa L1300.
**Scop:** Face urmatorul ciclu previzibil.
**Target:** `./deepseek-active-series-summary.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie un handoff pentru urmatorul batch DeepSeek.
**Acceptare:** Directia urmatoare este clara si scurta.

### L1283 Raport pentru generarea batch-ului
**Descriere tehnica:** Resumeaza procesul de creare a noilor taskuri si regulile aplicate in timpul generarii.
**Scop:** Face sesiunea usor de audit.
**Target:** `../../CHANGELOG.md`.
**Prompt AI:** Creeaza un raport pentru generarea batch-ului DeepSeek 25.
**Acceptare:** Raportul include baza decizionala si rezultatul.

### L1284 Raport pentru indexuri schimbate
**Descriere tehnica:** Arata exact ce indexuri au fost modificate cand batch-ul a fost adaugat.
**Scop:** Simplifica verificarea post-editare.
**Target:** `./README.md`, `../README.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie un raport pentru indexurile actualizate in batch-ul 25.
**Acceptare:** Toate indexurile schimbate sunt enumerate.

### L1285 Raport pentru elemente incomplete
**Descriere tehnica:** Marcheaza orice sectiune, link sau referinta care a ramas in lucru.
**Scop:** Evita concluziile false despre finalizare.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Scrie un raport pentru elementele incomplete din batch-ul DeepSeek 25.
**Acceptare:** Orice lacuna este numita explicit.

### L1286 Rezumat de risc ramas
**Descriere tehnica:** Compileaza riscurile care raman dupa publicarea batch-ului.
**Scop:** Ajuta la prioritizare ulterioara.
**Target:** `./deepseek-active-series-summary.md`, `../../CHANGELOG.md`.
**Prompt AI:** Creeaza un rezumat al riscurilor ramase pentru batch-ul 25.
**Acceptare:** Riscurile sunt ordonate de la mare la mic.

### L1287 Status pe o pagina
**Descriere tehnica:** Produce un status compact pentru review uman rapid.
**Scop:** Reduce timpul de verificare.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Scrie un status pe o pagina pentru seria DeepSeek.
**Acceptare:** Un om poate vedea starea curenta imediat.

### L1288 Nota pentru consum AI
**Descriere tehnica:** Spune cum trebuie citit batch-ul de un model automat fara sa confunde ghidul cu istoria.
**Scop:** Reduce interpretarea gresita.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Adauga o nota despre consumul AI al batch-ului 25.
**Acceptare:** Regula de citire automata este explicita.

### L1289 Raport pentru publicare
**Descriere tehnica:** Defineste forma scurta de raport care confirma ca batch-ul a fost publicat corect.
**Scop:** Asigura trasabilitatea lansarii.
**Target:** `../../CHANGELOG.md`, `./README.md`.
**Prompt AI:** Creeaza un raport scurt pentru publicarea batch-ului DeepSeek 25.
**Acceptare:** Publicarea poate fi verificata din raport.

### L1290 Nota finala pentru batch-ul 25
**Descriere tehnica:** Rezuma extinderea L1251-L1300 si rolul ei in navigare, arhivare si audit.
**Scop:** Inchide batch-ul cu un rezumat canonic.
**Target:** `./deepseek-active-series-summary.md`, `../../CHANGELOG.md`.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L1251-L1300.
**Acceptare:** Documentul marcheaza clar ce a fost adaugat si de ce.

## Automatizare si guarduri

### L1291 Guard pentru lipsa de goluri
**Descriere tehnica:** Verifica daca intervalul nou continua fara salt numeric de la batch-ul precedent.
**Scop:** Pastreaza secventa strict crescatoare.
**Target:** `./deepseek-batch-guide.md`, `../../scripts/deepseek-next-task.ps1`.
**Prompt AI:** Creeaza un guard pentru lipsa golurilor intre batch-uri.
**Acceptare:** Orice gol numeric este detectat.

### L1292 Guard pentru reutilizarea numerelor
**Descriere tehnica:** Detecteaza folosirea repetata a unui numar L deja publicat in alta parte.
**Scop:** Protejeaza unicitatea taskurilor.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un guard pentru reutilizarea numerelor L.
**Acceptare:** Numarul duplicat este semnalat clar.

### L1293 Guard pentru sincronizarea listei active
**Descriere tehnica:** Verifica daca lista de batch-uri active din README si sumarul seriei spun acelasi lucru.
**Scop:** Evita drift-ul intre indexuri.
**Target:** `./README.md`, `./deepseek-active-series-summary.md`.
**Prompt AI:** Creeaza un guard pentru sincronizarea listei active.
**Acceptare:** Divergenta dintre fisiere este vizibila imediat.

### L1294 Guard pentru sincronizarea arhivei
**Descriere tehnica:** Verifica daca fisierul din arhiva si indexul arhivei descriu acelasi interval.
**Scop:** Pastreaza trasabilitatea arhivei.
**Target:** `./arhiva/README.md`, `../index-arhiva.md`.
**Prompt AI:** Creeaza un guard pentru sincronizarea arhivei DeepSeek.
**Acceptare:** Orice diferenta de interval este raportata.

### L1295 Guard pentru alinierea cursorului
**Descriere tehnica:** Verifica daca executorul porneste de la taskul corect si nu sare peste taskuri neinchise.
**Scop:** Pastreaza executia controlata.
**Target:** `./deepseek-execution-cursor.md`, `./deepseek-execution-ledger.json`.
**Prompt AI:** Creeaza un guard pentru alinierea cursorului DeepSeek.
**Acceptare:** Taskul urmator eligibil este identificat corect.

### L1296 Guard pentru intrare in changelog
**Descriere tehnica:** Verifica daca orice batch nou are o intrare de changelog asociata.
**Scop:** Pastreaza istoricul complet.
**Target:** `../../CHANGELOG.md`.
**Prompt AI:** Creeaza un guard pentru intrarea de changelog a batch-ului 25.
**Acceptare:** Lipsa intrarii este raportata ca problema.

### L1297 Guard pentru creare de fisier
**Descriere tehnica:** Verifica daca fisierul batch nou respecta schema de denumire si locatia asteptata.
**Scop:** Previne plasarea gresita a documentelor.
**Target:** `./README.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un guard pentru crearea fisierelor batch DeepSeek.
**Acceptare:** Numele si calea sunt validate.

### L1298 Guard pentru linkuri stale
**Descriere tehnica:** Detecteaza linkurile care trimit la batch-uri mutate sau inlocuite.
**Scop:** Reduce linkurile rupte.
**Target:** `./deepseek-batch-guide.md`, `../README.md`, `./README.md`.
**Prompt AI:** Creeaza un guard pentru linkurile stale din documentatia DeepSeek.
**Acceptare:** Linkurile vechi sunt listate clar.

### L1299 Guard pentru titluri duplicate
**Descriere tehnica:** Verifica daca doua taskuri au titlu prea apropiat si pot induce confuzie la citire.
**Scop:** Pastreaza unicitatea denumirilor.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un guard pentru titlurile duplicate din batch-ul 25.
**Acceptare:** Titlurile aproape identice sunt marcate.

### L1300 Nota finala pentru L1251-L1300
**Descriere tehnica:** Inchide batch-ul cu un rezumat scurt al structurii, regulilor si scopului general.
**Scop:** Marcheaza finalul extensiei fara ambiguitate.
**Target:** `./deepseek-active-series-summary.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L1251-L1300.
**Acceptare:** Finalul batch-ului este clar si usor de citat.
