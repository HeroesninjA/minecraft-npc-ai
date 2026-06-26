# DeepSeek Taskuri - Batch 2 (L031-L055)

Actualizat: 2026-06-23

Acest document continua backlog-ul de taskuri mici, sigure si implementabile incremental pentru DeepSeek v4 Flash.

Reguli:
- fiecare task trebuie sa schimbe o singura zona mica;
- daca taskul atinge runtime, adauga test sau audit read-only;
- daca taskul atinge parserul, adauga warning clar pentru input invalid;
- nu introduce noi mecanici mari fara contract si regresie.

## Baza de contract

### L031 Registru comun de aliasuri pentru objective type
**Descriere:** Scoate aliasurile de objective type din liste duplicate si pune-le intr-un helper comun.
**Prompt AI:** Creeaza un registru central pentru aliasuri de objective type si foloseste-l in parser, resolver, command text si validare.
**Acceptare:** `talk_nlc`, `talk_npc` si `interact_nkde` se normalizeaza identic peste tot; nu mai exista liste divergente.

### L032 Audit read-only pentru objective types necunoscute
**Descriere:** Adauga o comanda sau subcomanda care listeaza objective type-urile necunoscute din questurile incarcate.
**Prompt AI:** Creeaza un audit read-only care scaneaza questurile si raporteaza tipurile neacceptate, fara sa modifice datele.
**Acceptare:** adminul vede rapid fisierele cu typo-uri sau tipuri lipsa.

### L033 Warning pentru objective type invalid in YAML
**Descriere:** Fa parserul sa marcheze clar obiectivele cu type necunoscut.
**Prompt AI:** La incarcarea YAML-ului, scrie warning explicit pentru orice objective type care nu exista in registrul suportat.
**Acceptare:** tipurile gresite nu sunt ignorate silentios.

### L034 Warning pentru obiective fara target sau anchor
**Descriere:** Valideaza obiectivele incomplete.
**Prompt AI:** Daca un objective nu are nici `target`, nici `anchor`, marcheaza-l ca warning in validator si debug.
**Acceptare:** questurile incomplete apar cu warning clar in raport.

### L035 Test de regresie pentru `talk_nlc`
**Descriere:** Acopera aliasul typo frecvent pentru vorbit cu NPC.
**Prompt AI:** Adauga test care verifica faptul ca `talk_nlc` ajunge la tipul canonic `talk_to_npc`.
**Acceptare:** testul trece fara dependente runtime.

### L036 Test de regresie pentru `interact_nkde`
**Descriere:** Acopera aliasul typo frecvent pentru noduri.
**Prompt AI:** Adauga test care verifica faptul ca `interact_nkde` ajunge la tipul canonic `inspect_node`.
**Acceptare:** parserul si resolverul se comporta la fel pe alias si pe tipul canonic.

## Runtime obiective

### L037 Progress runtime pentru `place_block`
**Descriere:** Adauga progres la plasarea blocului tinta.
**Prompt AI:** Leaga event-ul de block place de objective progress si filtreaza pe materialul corect.
**Acceptare:** obiectivul creste numai pentru blocul configurat.

### L038 Progress runtime pentru `break_block`
**Descriere:** Adauga progres la spargerea blocului tinta.
**Prompt AI:** Leaga event-ul de block break de objective progress si valideaza materialul/locatia tinta.
**Acceptare:** spargerea altor blocuri nu avanseaza questul.

### L039 Progress runtime pentru `craft_item`
**Descriere:** Adauga progres la crafting-ul itemului tinta.
**Prompt AI:** Leaga crafting event-ul de objective progress si verifica rezultatul craftului.
**Acceptare:** questul avanseaza doar pentru itemul cerut.

### L040 Helper comun pentru rezolvare item/material
**Descriere:** Unifica logica de rezolvare a materialelor intre `collect_item` si `craft_item`.
**Prompt AI:** Extracteaza o functie comuna care rezolva string-ul din YAML intr-un material sau item id valid.
**Acceptare:** validarea si runtime-ul folosesc acelasi cod de rezolvare.

### L041 Warning pentru `collect_item` fara item valid
**Descriere:** Semnalizeaza target-urile de colectare invalide.
**Prompt AI:** Daca `collect_item` nu poate fi mapat la un item valid, adauga warning explict la load.
**Acceptare:** YAML-ul invalid nu produce progres fals.

### L042 Warning pentru `craft_item` fara item valid
**Descriere:** Semnalizeaza target-urile de crafting invalide.
**Prompt AI:** Daca `craft_item` nu poate fi mapat la un item valid, adauga warning explicit.
**Acceptare:** numele gresite de item nu sunt tratate ca valide.

### L043 Rezolvare stricta pentru `visit_place`
**Descriere:** Fă obiectivul de vizitare sa lege doar place-ul corect.
**Prompt AI:** Valideaza ca `visit_place` nu cade accidental pe o regiune sau pe un alt place cu nume similar.
**Acceptare:** progresul se face numai pe place-ul exact configurat.

### L044 Rezolvare stricta pentru `inspect_node`
**Descriere:** Fă obiectivul de inspectare sa lege doar node-ul corect.
**Prompt AI:** Verifica ca node-ul target este rezolvat determinist si afiseaza warning daca lipseste.
**Acceptare:** obiectivul nu avanseaza pe ancore ambigue.

### L045 Rezolvare mai buna pentru `talk_to_npc`
**Descriere:** Imbunatateste selectarea NPC-ului tinta.
**Prompt AI:** Permite rezolvare dupa `npc`, `name` si `profession`, cu prioritate clara si warning la ambiguitate.
**Acceptare:** acelasi quest nu trebuie sa depinda de un singur selector.

### L046 Fallback robust pentru `kill_mob`
**Descriere:** Pastreaza kill credit si fara `entity.killer`, daca atacul recent vine de la player.
**Prompt AI:** Mentine fallback-ul de killer recent si adauga metrica sau test de regresie daca e nevoie.
**Acceptare:** kill-ul din lava / projectile / hit intarziat poate fi atribuit corect.

## Debug si UX

### L047 Debug dump cu type brut si normalizat
**Descriere:** Arata explicit ce a venit din YAML si ce a fost normalizat.
**Prompt AI:** Extinde debug dump-ul astfel incat sa afiseze `raw_type` si `normalized_type` pentru fiecare objective.
**Acceptare:** diagnosticarea typo-urilor devine directa.

### L048 Command text cu lista de aliasuri suportate
**Descriere:** Afiseaza aliasurile in help si in textul de comanda.
**Prompt AI:** Actualizeaza output-ul de help pentru quest objectives astfel incat sa arate aliasurile acceptate.
**Acceptare:** utilizatorul vede imediat ca `talk_nlc` este tolerant sau respins.

### L049 UI de quest builder cu grupare pe categorii
**Descriere:** Aranjeaza objective types pe categorii, nu ca lista plata.
**Prompt AI:** Grupeaza optiunile din UI in sociale, explorare, colectare, constructie si combat.
**Acceptare:** creatorul de quest vede mai usor tipurile suportate.

### L050 Exemplu YAML complet pentru obiective suportate
**Descriere:** Adauga un quest exemplu care acopera tipurile principale si aliasurile.
**Prompt AI:** Creeaza un exemplu simplu si coerent care arata `talk_to_npc`, `visit_place`, `inspect_node`, `kill_mob`, `collect_item`, `place_block`, `break_block`, `craft_item`.
**Acceptare:** documentatia are un exemplu copiat usor in questuri reale.

### L051 Tabel canonic de contract objective
**Descriere:** Documenteaza tip canonic, aliasuri si hook runtime intr-un singur tabel.
**Prompt AI:** Adauga un tabel in docs care spune clar ce tipuri exista, ce aliasuri sunt acceptate si ce eveniment le avanseaza.
**Acceptare:** autorii de questuri au o singura sursa de adevar.

### L052 Warning summary in `scenario warnings`
**Descriere:** Arata sumarul warning-urilor pe tipuri, nu doar pe count.
**Prompt AI:** Extinde comanda de warnings astfel incat sa indice categoria problemei: alias, target lipsa, item invalid, anchor lipsa.
**Acceptare:** adminul vede ce fel de problema exista fara sa deschida debug dump-ul.

### L053 Smoke test pentru un quest mixt
**Descriere:** Adauga un test de flux pe un quest mic cu mai multe objective type-uri.
**Prompt AI:** Creeaza un smoke test cu cel putin un obiectiv social, unul de explorare si unul de combat sau craft.
**Acceptare:** testul confirma ca minim trei tipuri diferite progreseaza corect.

### L054 Audit pentru obiective suportate in quest builder
**Descriere:** Compara lista din UI cu lista din validator si parser.
**Prompt AI:** Creeaza un audit read-only care verifica daca UI, validatorul si parserul expun aceleasi objective types.
**Acceptare:** diferentele intre straturi sunt raportate explicit.

### L055 Compatibilitate pentru aliasuri vechi in drafturi salvate
**Descriere:** Nu rupe questurile vechi care folosesc aliasuri tolerate.
**Prompt AI:** Adauga o cale de compatibilitate pentru drafturi vechi si marcheaza aliasurile legacy ca tolerate sau deprecated.
**Acceptare:** questurile vechi continua sa se incarce, dar emit warning daca folosesc aliasuri depasite.

## Extensii mici de quest

### L056 `deliver_to_npc` cu rezolvare semantica
**Descriere:** Intareste livrarea la NPC tinta.
**Prompt AI:** Permite rezolvare dupa nume, profesie sau anchor si adauga warning daca exista mai multi candidati.
**Acceptare:** obiectivul nu se blocheaza pe un selector unic prea fragil.

### L057 `return_to_giver` cu target explicit
**Descriere:** Fă întoarcerea la giver mai determinista.
**Prompt AI:** Leaga `return_to_giver` de quest giver-ul real si nu de un NPC generic cu aceeasi profesie.
**Acceptare:** intoarcerea marcheaza progresul numai la giver-ul corect.

### L058 `use_item` ca obiectiv minim
**Descriere:** Adauga un obiectiv simplu de utilizare item.
**Prompt AI:** Creeaza progres pentru folosirea unui item tinta, cu validare la load si event runtime clar.
**Acceptare:** folosirea altor iteme nu avanseaza questul.

### L059 `equip_item` ca obiectiv minim
**Descriere:** Adauga un obiectiv simplu de echipare item.
**Prompt AI:** Leaga progress-ul de equip event si verifica itemul exact configurat in YAML.
**Acceptare:** echiparea itemului gresit nu conteaza.

### L060 `talk_to_npc` cu dialog state
**Descriere:** Leaga progresul de starea dialogului, nu doar de interactiune.
**Prompt AI:** Adauga o verificare optionala care permite progres doar daca NPC-ul este intr-o stare sau linie de dialog asteptata.
**Acceptare:** questul poate diferentia intre simpla conversatie si raspunsul corect.

### L061 `visit_region` cu context de intrare
**Descriere:** Evita progresul fals la teleport sau spawn.
**Prompt AI:** Marcheaza `visit_region` doar la intrare reala si adauga filtrare pentru teleporturi administrative.
**Acceptare:** nu se bifeaza obiectivul din spawn sau debug teleport.

### L062 `visit_place` cu enter/exit audit
**Descriere:** Urmareste exact momentul de intrare in place.
**Prompt AI:** Adauga audit pentru intrarea si iesirea din place astfel incat debugging-ul sa fie simplu.
**Acceptare:** logul arata clar cand si de ce s-a marcat progresul.

### L063 `inspect_node` cu interactiune repetata
**Descriere:** Permite inspectarea node-ului fara duplicare falsa.
**Prompt AI:** Blocheaza progresul repetat pe aceeasi actiune pana la resetul corect al obiectivului.
**Acceptare:** acelasi click nu poate completa obiectivul de mai multe ori.

### L064 `collect_item` cu inventar partial
**Descriere:** Adauga progres chiar daca itemul e deja in inventar.
**Prompt AI:** Verifica inventarul la load sau la first sync, nu doar la pickup.
**Acceptare:** questul poate porni cu itemul deja obtinut.

### L065 `collect_item` cu stack count
**Descriere:** Permite cantitati mai mari pe acelasi item.
**Prompt AI:** Progresul trebuie sa tina cont de amount si sa numere corect stack-urile partiale.
**Acceptare:** 3 bucati din 5 nu finalizeaza obiectivul.

### L066 `kill_mob` cu tipuri multiple
**Descriere:** Extinde combat objective la mai multe tipuri de mob.
**Prompt AI:** Permite lista de tipuri tinta, nu doar un singur mob, si adauga validare pe YAML.
**Acceptare:** questul poate cere mai multe variante de inamic.

### L067 `kill_mob` cu biome context
**Descriere:** Adauga context optional pentru mob-urile din quest.
**Prompt AI:** Permite ca obiectivul sa ceara kill doar in anumite zone sau biomes, fara sa complice runtime-ul.
**Acceptare:** kill-ul in afara contextului nu avanseaza.

### L068 `place_block` cu locatie limitata
**Descriere:** Limiteaza plasarea la o zona sau schematic mica.
**Prompt AI:** Adauga suport pentru o lista mica de locatii acceptate sau un cub de validare.
**Acceptare:** blocul plasat in afara ariei nu conteaza.

### L069 `break_block` cu protectie pe block blacklist
**Descriere:** Evita progresul pe blocuri gresite.
**Prompt AI:** Permite o lista de blocuri interzise care nu trebuie sa avanseze niciodata obiectivul.
**Acceptare:** questul nu se poate finaliza prin spargerea gresita.

### L070 `craft_item` cu rezultat exact
**Descriere:** Verifica itemul rezultat, nu doar intentia de crafting.
**Prompt AI:** Foloseste rezultatul craftului ca sursa de adevar si accepta doar itemul configurat.
**Acceptare:** ingredientele similare nu produc progres fals.

## Parser si validare

### L071 Normalizare whitespace si case pentru objective type
**Descriere:** Fă parserul tolerant la formatare simpla.
**Prompt AI:** Normalizeaza inputul `objective.type` prin trim, lowercase si separatori simpli, fara sa accepti orice typo.
**Acceptare:** YAML-urile scrise neuniform raman compatibile.

### L072 Warning pentru duplicate objective ids
**Descriere:** Prinde obiectivele definite de doua ori.
**Prompt AI:** Adauga validare pentru duplicate de id in acelasi quest si emite warning explicit.
**Acceptare:** acelasi id nu poate suprascrie silentios alt obiectiv.

### L073 Warning pentru objective order inconsistent
**Descriere:** Verifica ordinea logica a obiectivelor.
**Prompt AI:** Daca un quest are obiective dependente definite in ordine ciudata, raporteaza un warning de validare.
**Acceptare:** autorul vede cand secventa este suspecta.

### L074 Compatibilitate cu obiective vechi serializate
**Descriere:** Pastreaza citirea obiectivelor vechi.
**Prompt AI:** Daca in config exista un tip vechi, mapeaza-l la tipul nou sau marcheaza-l deprecated, dar nu rupe load-ul.
**Acceptare:** questurile vechi continua sa porneasca.

### L075 Contract clar pentru objective metadata
**Descriere:** Definește ce metadata este optionala si ce este obligatorie.
**Prompt AI:** Documenteaza si valideaza campurile minime pentru fiecare objective type, mai ales target, amount si context.
**Acceptare:** nu exista campuri ambigue fara descriere.

### L076 Test de regresie pentru tipuri neuniforme
**Descriere:** Acopera formatele diverse ale aceluiasi objective.
**Prompt AI:** Creeaza test care verifica daca `Talk_To_Npc`, `talk_to_npc` si `talk npc` sunt tratate conform regulilor stabilite.
**Acceptare:** doar regulile aprobate sunt tolerate.

### L077 Test de regresie pentru duplicate ids
**Descriere:** Asigura detectarea duplicatelelor.
**Prompt AI:** Adauga test care injecteaza doua obiective cu acelasi id si verifica warning-ul de validare.
**Acceptare:** duplicatele nu trec silentios.

### L078 Test de regresie pentru obiectiv cu amount
**Descriere:** Verifica progresul cu cantitati.
**Prompt AI:** Acopera un objective cu amount > 1 si confirma ca progressul se opreste la pragul corect.
**Acceptare:** progresul partial este raportat corect.

## Observabilitate

### L079 Debug summary pentru objective progression
**Descriere:** Arata starea obiectivelor active intr-un sumar scurt.
**Prompt AI:** Extinde debug-ul questurilor astfel incat sa afiseze obiectivele active, completate si blocate.
**Acceptare:** adminul poate vedea rapid unde s-a oprit progresul.

### L080 Raport de compatibilitate pentru aliasuri
**Descriere:** Listeaza aliasurile acceptate si cele deprecated.
**Prompt AI:** Creeaza un raport read-only care spune ce aliasuri sunt tolerate, ce aliasuri sunt recomandate si ce aliasuri sunt deprecated.
**Acceptare:** migrarea questurilor vechi devine controlata si vizibila.

## Runtime si workflow

### L081 Sincronizare initiala a obiectivelor active
**Descriere:** Marcheaza progresul pentru obiectivele deja indeplinite cand questul pornește.
**Prompt AI:** La activarea questului, citeste starea curenta a jucatorului si completeaza obiectivele care sunt deja rezolvate.
**Acceptare:** questurile nu cer reexecutarea actiunilor deja facute.

### L082 Reset controlat pentru obiective temporare
**Descriere:** Adauga reset doar acolo unde conteaza.
**Prompt AI:** Permite reset separat pentru obiective temporare, fara sa stearga progresul permanent din acelasi quest.
**Acceptare:** questurile lungi nu pierd progresul valid cand un stage temporar e resetat.

### L083 Stage enter cu spawn de NPC secundar
**Descriere:** Leaga NPC-urile temporare de intrarea in stage.
**Prompt AI:** Ataseaza spawn-ul de actori secundari la intrarea in stage si asigura despawn la iesire.
**Acceptare:** NPC-urile de scena apar doar cand stage-ul este activ.

### L084 Stage complete cu cleanup de actori
**Descriere:** Curata actorii temporari la final de stage.
**Prompt AI:** Despawn-eaza actorii scenariului cand stage-ul se incheie si valideaza ca nu raman entitati orfane.
**Acceptare:** nu raman NPC-uri temporare dupa completarea stage-ului.

### L085 Quest fail cu cleanup complet
**Descriere:** Sterge actorii si starea temporara la fail.
**Prompt AI:** La fail, curata actorii secundari, marker-ele temporare si progresul de scena care nu trebuie pastrat.
**Acceptare:** questul poate fi reluat curat dupa fail.

### L086 Quest reset cu reenroll sigur
**Descriere:** Reseteaza questul fara sa lase artefacte.
**Prompt AI:** La reset, reinitializeaza actorii, trigger-ele si obiectivele astfel incat refacerea questului sa fie determinista.
**Acceptare:** un reset produce aceeasi stare initiala ca prima pornire.

### L087 Traseu de progres pentru `objective_complete`
**Descriere:** Leaga completarea obiectivului de trigger-ul urmator.
**Prompt AI:** Dupa ce un objective este completat, ruleaza hook-ul de progres urmator fara sa dubleze evenimentul.
**Acceptare:** secventa de quest avanseaza o singura data per obiectiv.

### L088 Traseu de progres pentru `stage_complete`
**Descriere:** Leaga trecerea de stage de hook-ul corect.
**Prompt AI:** Foloseste un singur flux canonical pentru finalizarea stage-ului si evita dublarea intre `exit` si `complete`.
**Acceptare:** stage transition produce o singura mutare logica.

### L089 Traseu de progres pentru `quest_complete`
**Descriere:** Finalizeaza questul o singura data.
**Prompt AI:** Adauga guard ca hook-ul de completare sa nu fie rulat de doua ori daca mai multe evenimente sosesc aproape simultan.
**Acceptare:** recompensa si cleanup nu se dubleaza.

### L090 Traseu de progres pentru `quest_accept`
**Descriere:** Porneste questul doar dupa acceptul real.
**Prompt AI:** Leaga inceputul progresului de acceptul explicit si nu de simpla vizualizare sau apropiere.
**Acceptare:** questul nu porneste accidental.

## Tooling si comenzi

### L091 Subcomanda `quest warnings-only`
**Descriere:** Afiseaza doar questurile cu warning-uri.
**Prompt AI:** Creeaza o subcomanda dedicata care listeaza numai questurile suspecte si numarul de warning-uri.
**Acceptare:** adminul poate gasi rapid questurile cu probleme.

### L092 Subcomanda `quest warnings-first`
**Descriere:** Sorteaza questurile cu probleme in fata.
**Prompt AI:** Adauga o varianta de listare care afiseaza primele questurile cu cele mai multe warning-uri.
**Acceptare:** problemele apar primele in output.

### L093 Help clar pentru aliasuri tolerate
**Descriere:** Marcheaza aliasurile vechi ca tolerate sau deprecated in help.
**Prompt AI:** Actualizeaza help-ul astfel incat sa explice daca un alias e doar tolerant sau recomandat.
**Acceptare:** operatorul stie ce poate curata.

### L094 Tab-complete pentru aliasuri de objective
**Descriere:** Sugereaza aliasurile compatibile in comenzi.
**Prompt AI:** Extinde completarea automata astfel incat sa arate aliasuri suportate si tipurile canonice.
**Acceptare:** inputul devine mai putin fragil.

### L095 Audit pentru sincronizarea UI-Parser-Validator
**Descriere:** Verifica daca UI, parserul si validatorul expun acelasi set de objective.
**Prompt AI:** Creeaza un audit read-only care compara cele trei surse si raporteaza diferentele.
**Acceptare:** discrepantele sunt vizibile inainte de release.

### L096 Test de comanda pentru warnings
**Descriere:** Acopera output-ul administrativ de warnings.
**Prompt AI:** Adauga test care verifica faptul ca `/ainpc scenario warnings` si `/ainpc scenario list warnings-first` produc output stabil.
**Acceptare:** comportamentul comenzii nu regreseaza.

### L097 Test de completare pentru objective aliases
**Descriere:** Acopera completarea automata.
**Prompt AI:** Adauga test care verifica faptul ca aliasurile si obiectivele canonice apar in suggestions.
**Acceptare:** tab-completion ramane utila dupa fiecare schimbare.

### L098 Test de serializare pentru warning summary
**Descriere:** Pastreaza warning summary in debug dump.
**Prompt AI:** Adauga test care verifica serializarea count + lista de warning-uri in dump-ul JSON.
**Acceptare:** warning-urile sunt vizibile si in debug artifact.

### L099 Documentatie scurta pentru alias policy
**Descriere:** Clarifica regula de toleranta pentru aliasuri.
**Prompt AI:** Scrie o sectiune care explica diferenta dintre alias tolerant, alias deprecated si typo respins.
**Acceptare:** autorii stiu ce trebuie corectat si ce este permis.

### L100 Changelog mic pentru objective contract
**Descriere:** Rezuma schimbarea de contract intr-o nota scurta.
**Prompt AI:** Adauga un changelog entry scurt pentru obiectivele noi si aliasurile tolerate.
**Acceptare:** istoria modificarilor ramane usor de urmarit.

## Stabilizare quest

### L101 `visit_region` cu minim de intrari
**Descriere:** Adauga prag minim de intrari pentru progres.
**Prompt AI:** Permite ca `visit_region` sa ceara mai multe intrari sau un timp minim petrecut in zona.
**Acceptare:** trecerea fugara prin zona nu finalizeaza obiectivul.

### L102 `visit_place` cu vizita unica
**Descriere:** Evita completarea repetata a aceleiasi vizite.
**Prompt AI:** Marcheaza un place ca vizitat o singura data pe ciclu de quest, cu reset clar la restart.
**Acceptare:** acelasi pas nu poate fi bifat de doua ori accidental.

### L103 `inspect_node` cu interactiune valida
**Descriere:** Filtreaza interactiunile invalide cu node-ul.
**Prompt AI:** Accepta doar interactiunile care se potrivesc cu actiunea asteptata, nu orice click pe acelasi node.
**Acceptare:** obiectivul nu se completeaza la interactiuni irelevante.

### L104 `talk_to_npc` cu cooldown minim
**Descriere:** Previi spam-ul de dialog.
**Prompt AI:** Adauga un cooldown scurt intre conversatii daca acelasi NPC este interogat prea des.
**Acceptare:** conversatiile repetate nu dubleaza progresul.

### L105 `collect_item` cu source filter
**Descriere:** Distinge sursa colectarii.
**Prompt AI:** Permite ca obiectivul sa accepte doar colectarea din drop, chest sau crafting, in functie de configurare.
**Acceptare:** itemul obtinut din alta sursa nu avanseaza questul.

### L106 `kill_mob` cu source filter
**Descriere:** Distinge sursa kill-ului.
**Prompt AI:** Permite sa filtrezi daca kill-ul e valid doar pentru player, companion sau actor de scenariu.
**Acceptare:** mob-ul ucis de alt sistem nu completeaza obiectivul.

### L107 `place_block` cu protectie la duplicate
**Descriere:** Evita progresul dublu pe acelasi loc.
**Prompt AI:** Daca acelasi block este plasat de mai multe ori pe acelasi target, numara o singura data.
**Acceptare:** rebuild-ul pe acelasi loc nu dubleaza progresul.

### L108 `break_block` cu confirmare de target
**Descriere:** Verifica targetul inainte de progres.
**Prompt AI:** Adauga confirmare ca block-ul spart este cel cerut si nu unul substitut.
**Acceptare:** block-urile invecinate nu completeaza questul.

### L109 `craft_item` cu recipe whitelist
**Descriere:** Permite doar retete acceptate.
**Prompt AI:** Leaga obiectivul de o lista scurta de retete permise si respinge crafting-ul accidental al altor iteme.
**Acceptare:** numai reteta corecta conteaza.

### L110 Reset al progresului pe fail partial
**Descriere:** Pastreaza ce e permanent si reseteaza ce e temporar.
**Prompt AI:** Defineste clar ce parte din progres supravietuieste la fail partial si ce parte se reseteaza.
**Acceptare:** fail-ul partial nu sterge accidental tot questul.

## Date si contracte

### L111 Contract JSON pentru quest objective types
**Descriere:** Expune lista de objective types intr-un format citibil de tools.
**Prompt AI:** Creeaza un JSON mic sau un dump care listeaza tipurile canonice si aliasurile.
**Acceptare:** alte unelte pot consuma aceeasi lista fara sa parseze text liber.

### L112 Contract JSON pentru warnings
**Descriere:** Expune warning-urile intr-un format stabil.
**Prompt AI:** Serializarea warning-urilor trebuie sa includa tipul, mesajul si contextul minim.
**Acceptare:** debug tools pot filtra warnings fara parsare fragila.

### L113 Compatibilitate pentru YAML vechi
**Descriere:** Pastreaza incarcarea pentru questuri vechi.
**Prompt AI:** Daca YAML-ul vechi foloseste campuri deprecate, mapeaza-le in parser si adauga warning.
**Acceptare:** nu se pierde compatibilitatea cu questurile salvate anterior.

### L114 Compatibilitate pentru aliasuri multiple
**Descriere:** Prioritizeaza aliasul canonic, dar accepta pe cele vechi.
**Prompt AI:** Defineste ordinea de prioritate cand acelasi concept are mai multe aliasuri tolerate.
**Acceptare:** nu apar rezultate diferite in funcție de locul unde este parsat.

### L115 Contract minimal pentru stage metadata
**Descriere:** Standardizeaza metadata folosita in stage-uri.
**Prompt AI:** Specifica ce chei sunt acceptate in stage metadata si ce chei sunt ignorate cu warning.
**Acceptare:** metadata nefolosita nu produce efecte surpriza.

### L116 Test pentru contractul JSON de objective
**Descriere:** Protejeaza formatul de date pentru tools.
**Prompt AI:** Adauga test care verifica faptul ca lista de objective types si aliasuri ramane stabila.
**Acceptare:** modificarile de contract sunt detectate repede.

### L117 Test pentru contractul JSON de warnings
**Descriere:** Protejeaza formatul warning-urilor.
**Prompt AI:** Adauga test care verifica serializarea warning-urilor cu campurile minime.
**Acceptare:** debug tools nu se rup dupa modificari.

### L118 Test pentru compatibilitate YAML vechi
**Descriere:** Protejeaza questurile vechi.
**Prompt AI:** Adauga test care incarca un YAML vechi si confirma ca se parseaza cu warning, nu cu fail.
**Acceptare:** datele istorice raman utile.

### L119 Audit pentru campuri deprecated
**Descriere:** Lista campurile vechi folosite in questuri.
**Prompt AI:** Creeaza un audit read-only care raporteaza campurile deprecated din questuri sau scenarii.
**Acceptare:** curatarea configurarii devine controlata.

### L120 Note de migrare pentru authoring
**Descriere:** Spune cum sa treci de la aliasuri vechi la contract nou.
**Prompt AI:** Scrie un ghid scurt de migrare care arata ce trebuie inlocuit si ce warning-uri apar.
**Acceptare:** autorul stie cum sa curete drafturile existente.

## Operare si debugging

### L121 Sumar de progres pe comanda admin
**Descriere:** Arata progresul obiectivelor intr-o forma compacta.
**Prompt AI:** Extinde comanda admin astfel incat sa afiseze obiectivele in curs, cele complete si cele blocate.
**Acceptare:** operatorul vede statusul questului dintr-un singur ecran.

### L122 Mesaj clar pentru typo-uri de objective
**Descriere:** Nu lasa typo-ul sa para un feature valid.
**Prompt AI:** Cand parserul vede un typo cunoscut, afiseaza mesajul exact cu sugestia corecta.
**Acceptare:** mesajul de error ajuta la repararea rapida a YAML-ului.

### L123 Audit de mapping intre debug si runtime
**Descriere:** Verifica daca debug dump si runtime raporteaza acelasi objective state.
**Prompt AI:** Creeaza un audit read-only care compara progresul raportat de debug cu progresul intern.
**Acceptare:** discrepantele sunt detectabile usor.

### L124 Verificare pentru obiective inactive
**Descriere:** Raporteaza obiectivele care nu mai pot progresa.
**Prompt AI:** Adauga un check care detecteaza obiective blocate, invalide sau imposibil de completat.
**Acceptare:** questurile moarte apar in raport.

### L125 Raport de exemple pentru DeepSeek
**Descriere:** Strange exemple minime care pot fi implementate sigur de AI.
**Prompt AI:** Creeaza o lista cu 5 exemple YAML mici si valide, fiecare pentru un tip de objective diferit.
**Acceptare:** avem inputuri clare pentru automatizare si testare.

## Comportament NPC

### L126 `spawn_policy` documentat clar
**Descriere:** Clarifica ce face fiecare policy de spawn pentru actori temporari.
**Prompt AI:** Documenteaza diferenta dintre spawn one-shot, spawn pe stage, spawn pe trigger si spawn runtime-only.
**Acceptare:** autorii de questuri stiu ce policy sa aleaga.

### L127 `durationSeconds` pentru actori temporari
**Descriere:** Limiteaza durata de viata a NPC-urilor scenariu.
**Prompt AI:** Adauga si testeaza un timeout simplu care despawneaza actorii temporari dupa durata configurata.
**Acceptare:** actorii temporari nu raman pe lume la infinit.

### L128 `spawn_phase` cu fallback sigur
**Descriere:** Asigura un comportament stabil cand faza lipseste.
**Prompt AI:** Daca `spawn_phase` nu este setat, foloseste un fallback determinist si adauga warning doar daca e nevoie.
**Acceptare:** actorii nu dispar din cauza unui camp omis accidental.

### L129 `on_accept` pentru actor secundar
**Descriere:** Spawn actorii temporari imediat dupa acceptarea questului.
**Prompt AI:** Leaga un actor secundar simplu de trigger-ul `on_accept` si curata-l la fail/reset.
**Acceptare:** NPC-ul apare doar dupa acceptul real.

### L130 `on_complete` pentru cleanup actor
**Descriere:** Curata actorii cand questul este completat.
**Prompt AI:** Adauga un hook care despawneaza actorii de scena dupa completarea questului.
**Acceptare:** nu raman entitati active dupa reward.

### L131 `on_fail` pentru cleanup actor
**Descriere:** Curata actorii la esec.
**Prompt AI:** Despawneaza actorii asociati questului daca acesta esueaza.
**Acceptare:** fail-ul lasa lumea curata.

### L132 `on_reset` pentru cleanup actor
**Descriere:** Curata actorii la reset.
**Prompt AI:** La reset, sterge actorii temporari si reinitializeaza scenariul.
**Acceptare:** restartul questului incepe din nou curat.

### L133 `quest_actor_triggers` cu aliasuri canonice
**Descriere:** Foloseste aceleasi chei canonice peste tot.
**Prompt AI:** Normalizarea trigger-elor trebuie sa accepte aliasuri dar sa salveze cheile canonice.
**Acceptare:** debug-ul si runtime-ul arata aceeasi forma.

### L134 Warning pentru trigger-uri necunoscute
**Descriere:** Nu ignora trigger-ele invalide.
**Prompt AI:** Daca questul foloseste un trigger necunoscut pentru actori, marcheaza warning si continua safe.
**Acceptare:** typos-urile nu se pierd fara semnal.

### L135 Audit pentru actori fara trigger
**Descriere:** Detecteaza actorii care nu se vor mai spawna.
**Prompt AI:** Adauga audit care listeaza actorii definiti dar fara niciun trigger activ.
**Acceptare:** configuratiile moarte apar in raport.

## World mapping

### L136 Persistenta pentru node-uri generate
**Descriere:** Pastreaza node-urile create programatic.
**Prompt AI:** Asigura-te ca mapping-ul creat automat marcheaza dirty si este salvat corect.
**Acceptare:** node-urile apar dupa restart.

### L137 Cleanup corect la remove place
**Descriere:** Evita nodurile orfane in memorie.
**Prompt AI:** Curata lista de node-uri dupa place id, nu dupa node id, si adauga test de regresie.
**Acceptare:** recrearea place-ului nu reintroduce noduri vechi.

### L138 Dirty flag pentru fallback mapping
**Descriere:** Nu pierde starea de modificare pentru mapping-ul creat automat.
**Prompt AI:** Păstrează `dirty` cand fallback mapping-ul este generat programatic.
**Acceptare:** save-ul isi vede modificarile reale.

### L139 Audit pentru save command
**Descriere:** Verifica fluxul de save din comanda admin.
**Prompt AI:** Adauga o verificare read-only care confirma ca save command scrie doar dupa `hasUnsavedChanges()`.
**Acceptare:** nu mai exista overwrite accidental.

### L140 Debug summary pentru world nodes
**Descriere:** Afiseaza numarul de region/place/node in raportul admin.
**Prompt AI:** Extinde sumarul world mapping cu contori si avertismente de consistenta.
**Acceptare:** adminul vede imediat daca mapping-ul e partial.

## Tooling si UX

### L141 Sugestii de command pentru quest objectives
**Descriere:** Completeaza mai bine numele obiectivelor in comenzi.
**Prompt AI:** Extinde tab-complete-ul astfel incat sa sugereze tipurile canonice si aliasurile acceptate.
**Acceptare:** inputul manual devine mai sigur.

### L142 Help pentru aliasuri deprecated
**Descriere:** Arata ce aliasuri trebuie inlocuite.
**Prompt AI:** Marcheaza in help aliasurile deprecated cu sugestia de inlocuire.
**Acceptare:** migratia devine ghidata.

### L143 Audit pentru divergente UI-parser
**Descriere:** Verifica daca UI expune mai putin sau mai mult decat parserul.
**Prompt AI:** Creeaza un audit read-only pentru listele de objective types dintre UI si backend.
**Acceptare:** discrepantele sunt vizibile inainte de release.

### L144 Snapshot de debug pentru quest mix
**Descriere:** Salveaza un snapshot minimal de quest activ.
**Prompt AI:** Adauga un dump scurt care surprinde obiectivele, actorii si warnings pentru un quest activ.
**Acceptare:** diagnosticul unui quest este repetabil.

### L145 Exemplu de quest secundar temporar
**Descriere:** Ofera un quest scurt cu NPC temporar si cleanup.
**Prompt AI:** Creeaza un exemplu YAML care foloseste un NPC secundar, un stage si cleanup la fail/reset.
**Acceptare:** exemplul poate fi copiat direct pentru un quest nou.

### L146 Test pentru comanda `scenario warnings`
**Descriere:** Protejeaza output-ul de warnings.
**Prompt AI:** Creeaza test care verifica output-ul de listare a scenariilor cu warnings.
**Acceptare:** comportamentul admin raman stabil.

### L147 Test pentru quest actor triggers
**Descriere:** Protejeaza parserul de trigger-e.
**Prompt AI:** Adauga test care verifica normalizarea si validarea trigger-elor actorilor.
**Acceptare:** aliasurile si warning-urile raman corecte.

### L148 Test pentru actor cleanup la stage complete
**Descriere:** Verifica despawn-ul actorilor la final de stage.
**Prompt AI:** Adauga un test care confirma ca actorii temporari dispar dupa stage complete.
**Acceptare:** nu raman actori orfani.

### L149 Test pentru dirty mapping save
**Descriere:** Protejeaza persistența mapping-ului.
**Prompt AI:** Adauga test care confirma ca mapping-ul generat automat este marcat dirty si se salveaza.
**Acceptare:** nodurile generate nu se pierd dupa restart.

### L150 Changelog pentru batch-ul nou
**Descriere:** Noteaza extinderea backlog-ului.
**Prompt AI:** Scrie un changelog scurt pentru batch-ul L126-L150, cu accent pe NPC, mapping si UX.
**Acceptare:** istoricul schimbarilor ramane usor de urmarit.
