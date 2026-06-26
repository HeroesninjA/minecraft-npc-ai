# DeepSeek Taskuri - Batch 29 (L1451-L1500)

Actualizat: 2026-06-26

Acest document continua backlog-ul pentru DeepSeek v4 Flash cu taskuri mici, sigure si implementabile incremental.

Reguli:
- fiecare task schimba o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce mecanici mari fara contract si regresie.

## Operare si control

### L1451 Index pentru batch-ul 29
**Descriere tehnica:** Leaga noul batch de indexurile principale pentru descoperire imediata.
**Scop:** Face batch-ul 29 vizibil fara cautare manuala.
**Target:** `./deepseek-batch-guide.md`, `./README.md`, `../README.md`.
**Prompt AI:** Adauga referintele pentru batch-ul DeepSeek 29 in indexurile canonice.
**Acceptare:** Fisierul nou apare in toate indexurile relevante.

### L1452 Sumarul seriei active
**Descriere tehnica:** Actualizeaza sumarul seriei active cu intervalul nou.
**Scop:** Pastreaza contextul curent la zi.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Actualizeaza sumarul seriei active cu batch-ul DeepSeek 29.
**Acceptare:** Intervalul nou este vizibil in sumar.

### L1453 Tranzitie intre batch-uri
**Descriere tehnica:** Noteaza trecerea de la batch-ul 28 la batch-ul 29 fara ambiguitati.
**Scop:** Ajuta la auditul schimbarii de serie.
**Target:** `./deepseek-active-series-summary.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie o nota de tranzitie intre batch-ul 28 si 29.
**Acceptare:** Tranzitia este explicita si scurta.

### L1454 Ordine de citire
**Descriere tehnica:** Stabileste ordinea recomandata de citire pentru batch-ul nou.
**Scop:** Reduce pierderea de context la deschidere.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Adauga o regula pentru citirea secventiala a batch-ului 29.
**Acceptare:** Ordinea de citire este clara.

### L1455 Backlink catre ghid
**Descriere tehnica:** Leaga batch-ul 29 de ghidul canonic pentru reguli si proces.
**Scop:** Pastreaza regulile usor de gasit.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Adauga un backlink util catre ghidul DeepSeek.
**Acceptare:** Navigarea la regulile centrale este directa.

### L1456 Schimbare in changelog
**Descriere tehnica:** Creeaza urmele necesare pentru auditul schimbarii in jurnalul de modificari.
**Scop:** Pastreaza istoria operationala completa.
**Target:** `../../CHANGELOG.md`.
**Prompt AI:** Scrie o intrare scurta de changelog pentru batch-ul DeepSeek 29.
**Acceptare:** Intrarea mentioneaza fisierul si motivul.

### L1457 Punct de review uman
**Descriere tehnica:** Marcheaza punctul principal de review pentru batch-ul 29.
**Scop:** Face verificarea mai rapida.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Creeaza un punct de intrare pentru review-ul batch-ului 29.
**Acceptare:** Reviewer-ul vede rapid unde incepe.

### L1458 Inventar de dependinte
**Descriere tehnica:** Listeaza documentele care depind de noul batch.
**Scop:** Evita referinte pierdute la actualizare.
**Target:** `./deepseek-active-series-summary.md`, `../README.md`.
**Prompt AI:** Creeaza un inventar scurt pentru dependintele batch-ului 29.
**Acceptare:** Dependintele critice sunt enumerate.

### L1459 Indicator de stare
**Descriere tehnica:** Marcheaza explicit daca batch-ul este activ executabil sau doar documentare.
**Scop:** Evita consumul prematur.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Adauga un indicator de stare pentru batch-ul 29.
**Acceptare:** Starea batch-ului este fara ambiguitate.

### L1460 Nota pentru consum AI
**Descriere tehnica:** Specifica modul corect de consum automat al batch-ului fara a confunda arhiva cu activul.
**Scop:** Reduce interpretarea gresita de catre modele.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie o nota despre consumul AI al batch-ului 29.
**Acceptare:** Regula de citire automata este clara.

## Persistenta si arhiva

### L1461 Politica de mutare in arhiva
**Descriere tehnica:** Defineste cand batch-ul 29 poate fi mutat in arhiva fara sa rupa istoricul.
**Scop:** Pastreaza seria curata pe termen lung.
**Target:** `./deepseek-batch-guide.md`, `./arhiva/README.md`.
**Prompt AI:** Scrie o politica pentru mutarea batch-urilor DeepSeek in arhiva.
**Acceptare:** Conditiile de mutare sunt verificabile.

### L1462 Checklist pentru backup
**Descriere tehnica:** Listeaza pasii obligatorii de backup inainte de orice mutare de document.
**Scop:** Reduce riscul de pierdere a contextului.
**Target:** `./deepseek-taskuri-archive-audit-2026-06-25.md`.
**Prompt AI:** Creeaza o checklist pentru backup inainte de mutare.
**Acceptare:** Verificarile esentiale apar ordonat.

### L1463 Raport pentru restaurare temporara
**Descriere tehnica:** Standardizeaza restaurarea temporara a documentelor istorice.
**Scop:** Pastreaza trasabilitatea revenirii.
**Target:** `./deepseek-taskuri-archive-verification-2026-06-25.md`.
**Prompt AI:** Scrie un raport scurt pentru restaurarea temporara a unui batch.
**Acceptare:** Sursa, scopul si durata sunt clare.

### L1464 Regula pentru linkuri relative
**Descriere tehnica:** Impune folosirea rutelor relative corecte intre activ si arhiva.
**Scop:** Evita linkurile rupte.
**Target:** `./deepseek-batch-guide.md`, `./arhiva/README.md`.
**Prompt AI:** Scrie o regula pentru linkurile relative dintre batch-uri.
**Acceptare:** Calea corecta este specificata clar.

### L1465 Nota pentru reactivare temporara
**Descriere tehnica:** Clarifica ce inseamna readucerea temporara a unui batch istoric.
**Scop:** Pastreaza distinctia dintre istoric si activ.
**Target:** `./arhiva/README.md`.
**Prompt AI:** Adauga o nota despre reactivarea temporara a unui batch.
**Acceptare:** Reactivarea nu schimba statutul istoric.

### L1466 Raport comparativ pentru mutare
**Descriere tehnica:** Ofera forma scurta de comparatie intre varianta veche si copia mutata.
**Scop:** Face verificarea post-mutare mai rapida.
**Target:** `./deepseek-taskuri-archive-audit-2026-06-25.md`.
**Prompt AI:** Creeaza un raport comparativ pentru mutarea unui batch.
**Acceptare:** Diferentele relevante sunt listate.

### L1467 Curatare referinte duplicate
**Descriere tehnica:** Detecteaza referintele care trimit simultan la activ si la arhiva.
**Scop:** Previne confuzia de sursa.
**Target:** `./deepseek-batch-guide.md`, `../README.md`.
**Prompt AI:** Creeaza o regula pentru eliminarea referintelor duplicate.
**Acceptare:** Ramane o singura sursa canonica.

### L1468 Validare ruta arhiva
**Descriere tehnica:** Verifica daca toate trimiterile folosesc calea canonica de arhiva.
**Scop:** Pastreaza mutarile consecvente.
**Target:** `./arhiva/README.md`.
**Prompt AI:** Scrie o validare pentru rutele batch-urilor din arhiva.
**Acceptare:** Orice ruta gresita poate fi detectata.

### L1469 Inventar dependinte documentare
**Descriere tehnica:** Listeaza documentele afectate de batch-ul 29 inainte de orice mutare sau rescriere.
**Scop:** Evita ruperea lantului de referinte.
**Target:** `./deepseek-active-series-summary.md`, `../README.md`.
**Prompt AI:** Creeaza un inventar scurt pentru dependintele unui batch DeepSeek.
**Acceptare:** Dependintele critice sunt enumerate.

### L1470 Nota finala pentru arhivare
**Descriere tehnica:** Rezuma ce inseamna un batch complet mutat in arhiva si cum ramane urmaribil.
**Scop:** Inchide clar ciclul de mutare.
**Target:** `./arhiva/README.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie o nota finala pentru ciclul de arhivare DeepSeek.
**Acceptare:** Starea finala si traseul de referinta sunt clare.

## Audit si control

### L1471 Audit pentru numar de taskuri
**Descriere tehnica:** Verifica daca batch-ul are exact 50 de taskuri numerotate fara lipsuri.
**Scop:** Pastreaza integritatea intervalului.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru numarul de taskuri din batch-ul 29.
**Acceptare:** Lipsa sau suprascrierea numerelor este detectata.

### L1472 Audit pentru campuri obligatorii
**Descriere tehnica:** Verifica prezenta campurilor cerute pentru fiecare task.
**Scop:** Evita taskurile incomplete.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru campurile obligatorii ale taskurilor DeepSeek.
**Acceptare:** Orice task lipsa este semnalat.

### L1473 Audit pentru consistenta targetului
**Descriere tehnica:** Verifica daca `Target` indica zona tehnica real afectata.
**Scop:** Face taskurile actionabile.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru consistenta campului Target.
**Acceptare:** Targeturile neclare sunt listate separat.

### L1474 Audit pentru scop masurabil
**Descriere tehnica:** Verifica daca `Scop` descrie un rezultat verificabil.
**Scop:** Pastreaza taskurile evaluabile.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru formularea masurabila a scopului.
**Acceptare:** Scopurile vagi sunt marcate pentru rescriere.

### L1475 Audit pentru prompt executabil
**Descriere tehnica:** Verifica daca promptul cere o actiune concreta si unica.
**Scop:** Evita taskurile imposibil de rulat.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru prompturile executabile din batch-ul 29.
**Acceptare:** Prompturile ambigue sunt identificate.

### L1476 Audit pentru acceptare clara
**Descriere tehnica:** Verifica daca acceptarea poate fi validata fara interpretare libera.
**Scop:** Face inchiderea taskurilor obiectiva.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru criteriile de acceptare ale taskurilor DeepSeek.
**Acceptare:** Criteriile prea generale sunt semnalate.

### L1477 Audit pentru tema batch-ului
**Descriere tehnica:** Verifica daca taskul se potriveste temei batch-ului si nu amesteca zone distincte.
**Scop:** Pastreaza structura batch-ului curata.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru categoria si tema taskurilor DeepSeek.
**Acceptare:** Taskurile in afara temei sunt marcate.

### L1478 Audit pentru referinte proaspete
**Descriere tehnica:** Detecteaza referintele la fisiere vechi sau mutate care pot induce eroare.
**Scop:** Reduce drift-ul de documentatie.
**Target:** `./deepseek-batch-guide.md`, `../README.md`.
**Prompt AI:** Creeaza un audit pentru referintele stale din documentatia DeepSeek.
**Acceptare:** Referintele invechite apar in raport.

### L1479 Audit pentru fisiere lipsa
**Descriere tehnica:** Verifica daca toate fisierele mentionate in indexuri exista.
**Scop:** Evita legaturi false in documentatie.
**Target:** `./README.md`, `../README.md`.
**Prompt AI:** Creeaza un audit pentru fisierele lipsa din indexurile DeepSeek.
**Acceptare:** Orice referinta care nu exista este listata.

### L1480 Audit pentru titluri duplicate
**Descriere tehnica:** Detecteaza taskurile cu titluri prea apropiate care pot crea confuzie.
**Scop:** Pastreaza unicitatea intrarilor.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un audit pentru titlurile duplicate din batch-ul DeepSeek 29.
**Acceptare:** Duplicatele sunt semnalate cu numerele lor.

## Handoff si raportare

### L1481 Handoff pentru batch-ul curent
**Descriere tehnica:** Creeaza un rezumat scurt al ceea ce acopera batch-ul si ce trebuie retinut.
**Scop:** Ajuta la predarea contextului.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Scrie un handoff scurt pentru batch-ul DeepSeek 29.
**Acceptare:** Contextul esential este compact.

### L1482 Handoff pentru batch-ul urmator
**Descriere tehnica:** Noteaza ce tip de continut trebuie continuat dupa L1500.
**Scop:** Face urmatorul ciclu previzibil.
**Target:** `./deepseek-active-series-summary.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie un handoff pentru urmatorul batch DeepSeek.
**Acceptare:** Directia urmatoare este clara.

### L1483 Raport pentru generarea batch-ului
**Descriere tehnica:** Resumeaza procesul de creare si regulile aplicate la generare.
**Scop:** Face sesiunea usor de audit.
**Target:** `../../CHANGELOG.md`.
**Prompt AI:** Creeaza un raport pentru generarea batch-ului DeepSeek 29.
**Acceptare:** Raportul include baza decizionala si rezultatul.

### L1484 Raport pentru indexuri schimbate
**Descriere tehnica:** Arata exact ce indexuri au fost modificate cand batch-ul a fost adaugat.
**Scop:** Simplifica verificarea post-editare.
**Target:** `./README.md`, `../README.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie un raport pentru indexurile actualizate in batch-ul 29.
**Acceptare:** Toate indexurile schimbate sunt enumerate.

### L1485 Raport pentru elemente incomplete
**Descriere tehnica:** Marcheaza orice sectiune, link sau referinta ramas in lucru.
**Scop:** Evita concluziile false despre finalizare.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Scrie un raport pentru elementele incomplete din batch-ul DeepSeek 29.
**Acceptare:** Orice lacuna este numita explicit.

### L1486 Rezumat de risc ramas
**Descriere tehnica:** Compileaza riscurile ramase dupa publicarea batch-ului.
**Scop:** Ajuta la prioritizare ulterioara.
**Target:** `./deepseek-active-series-summary.md`, `../../CHANGELOG.md`.
**Prompt AI:** Creeaza un rezumat al riscurilor ramase pentru batch-ul 29.
**Acceptare:** Riscurile sunt ordonate de la mare la mic.

### L1487 Status pe o pagina
**Descriere tehnica:** Produce un status compact pentru review uman rapid.
**Scop:** Reduce timpul de verificare.
**Target:** `./deepseek-active-series-summary.md`.
**Prompt AI:** Scrie un status pe o pagina pentru seria DeepSeek.
**Acceptare:** Starea curenta este imediat vizibila.

### L1488 Nota pentru consum AI
**Descriere tehnica:** Specifica modul corect de consum automat al batch-ului.
**Scop:** Reduce interpretarea gresita.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Adauga o nota despre consumul AI al batch-ului 29.
**Acceptare:** Regula de citire automata este explicita.

### L1489 Raport pentru publicare
**Descriere tehnica:** Defineste forma scurta de raport care confirma publicarea corecta.
**Scop:** Asigura trasabilitatea lansarii.
**Target:** `../../CHANGELOG.md`, `./README.md`.
**Prompt AI:** Creeaza un raport scurt pentru publicarea batch-ului DeepSeek 29.
**Acceptare:** Publicarea poate fi verificata din raport.

### L1490 Nota finala pentru batch-ul 29
**Descriere tehnica:** Rezuma extinderea L1451-L1500 si rolul ei in operare, persistenta si audit.
**Scop:** Inchide batch-ul cu un rezumat canonic.
**Target:** `./deepseek-active-series-summary.md`, `../../CHANGELOG.md`.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L1451-L1500.
**Acceptare:** Documentul marcheaza clar ce a fost adaugat si de ce.

## Guarduri si consistenta

### L1491 Guard pentru lipsa de goluri
**Descriere tehnica:** Verifica daca intervalul nou continua fara salt numeric de la batch-ul precedent.
**Scop:** Pastreaza secventa strict crescatoare.
**Target:** `./deepseek-batch-guide.md`, `../../scripts/deepseek-next-task.ps1`.
**Prompt AI:** Creeaza un guard pentru lipsa golurilor intre batch-uri.
**Acceptare:** Orice gol numeric este detectat.

### L1492 Guard pentru reutilizarea numerelor
**Descriere tehnica:** Detecteaza folosirea repetata a unui numar L deja publicat.
**Scop:** Protejeaza unicitatea taskurilor.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un guard pentru reutilizarea numerelor L.
**Acceptare:** Numarul duplicat este semnalat clar.

### L1493 Guard pentru sincronizarea listei active
**Descriere tehnica:** Verifica daca lista de batch-uri active din README si sumarul seriei spun acelasi lucru.
**Scop:** Evita drift-ul intre indexuri.
**Target:** `./README.md`, `./deepseek-active-series-summary.md`.
**Prompt AI:** Creeaza un guard pentru sincronizarea listei active.
**Acceptare:** Divergenta dintre fisiere este vizibila imediat.

### L1494 Guard pentru sincronizarea arhivei
**Descriere tehnica:** Verifica daca fisierul din arhiva si indexul arhivei descriu acelasi interval.
**Scop:** Pastreaza trasabilitatea arhivei.
**Target:** `./arhiva/README.md`, `../index-arhiva.md`.
**Prompt AI:** Creeaza un guard pentru sincronizarea arhivei DeepSeek.
**Acceptare:** Orice diferenta de interval este raportata.

### L1495 Guard pentru alinierea cursorului
**Descriere tehnica:** Verifica daca executorul porneste de la taskul corect si nu sare peste taskuri neinchise.
**Scop:** Pastreaza executia controlata.
**Target:** `./deepseek-execution-cursor.md`, `./deepseek-execution-ledger.json`.
**Prompt AI:** Creeaza un guard pentru alinierea cursorului DeepSeek.
**Acceptare:** Taskul urmator eligibil este identificat corect.

### L1496 Guard pentru intrare in changelog
**Descriere tehnica:** Verifica daca orice batch nou are o intrare de changelog asociata.
**Scop:** Pastreaza istoricul complet.
**Target:** `../../CHANGELOG.md`.
**Prompt AI:** Creeaza un guard pentru intrarea de changelog a batch-ului 29.
**Acceptare:** Lipsa intrarii este raportata ca problema.

### L1497 Guard pentru creare de fisier
**Descriere tehnica:** Verifica daca fisierul batch nou respecta schema de denumire si locatia asteptata.
**Scop:** Previne plasarea gresita a documentelor.
**Target:** `./README.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un guard pentru crearea fisierelor batch DeepSeek.
**Acceptare:** Numele si calea sunt validate.

### L1498 Guard pentru linkuri stale
**Descriere tehnica:** Detecteaza linkurile care trimit la batch-uri mutate sau inlocuite.
**Scop:** Reduce linkurile rupte.
**Target:** `./deepseek-batch-guide.md`, `../README.md`, `./README.md`.
**Prompt AI:** Creeaza un guard pentru linkurile stale din documentatia DeepSeek.
**Acceptare:** Linkurile vechi sunt listate clar.

### L1499 Guard pentru titluri duplicate
**Descriere tehnica:** Verifica daca doua taskuri au titlu prea apropiat si pot induce confuzie.
**Scop:** Pastreaza unicitatea denumirilor.
**Target:** `./deepseek-batch-guide.md`.
**Prompt AI:** Creeaza un guard pentru titlurile duplicate din batch-ul 29.
**Acceptare:** Titlurile aproape identice sunt marcate.

### L1500 Nota finala pentru L1451-L1500
**Descriere tehnica:** Inchide batch-ul cu un rezumat scurt al structurii, regulilor si scopului general.
**Scop:** Marcheaza finalul extensiei fara ambiguitate.
**Target:** `./deepseek-active-series-summary.md`, `./deepseek-batch-guide.md`.
**Prompt AI:** Scrie o nota finala scurta pentru batch-ul L1451-L1500.
**Acceptare:** Finalul batch-ului este clar si usor de citat.
