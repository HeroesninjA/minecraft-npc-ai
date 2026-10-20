# NPC-uri temporare si episodice

Status: implementare partiala, cu gap-uri de lifecycle confirmate.
Actualizat: 2026-10-19.

Actorii sunt definiti in scenarii si spawnati de `ScenarioEngine`. Etichetele de lifecycle nu formeaza inca o politica completa de persistenta, simulare si cleanup.

## Contracte disponibile

- lifecycle: `PERMANENT`, `TEMPORARY`, `EPISODIC`, `SCENE_ONLY`;
- persistenta: `FULL`, `LIGHT`, `RUNTIME_ONLY`;
- simulare: `FULL`, `LIGHT`, `NONE`;
- interactiune: `FULL_AI`, `QUEST_ONLY`, `SHOP_ONLY`, `SCENE_ONLY`, `MINIMAL`;
- spawn policy: auto, stage sau manual;
- ownership, despawn rule, durata si tag-uri temporare.

## Comportament activ

- definitia YAML este aplicata pe obiectul `AINPC`;
- `DialogManager` consulta profilul de interactiune inainte de accesarea istoricului sau a providerului AI; `MINIMAL` si `SCENE_ONLY` blocheaza dialogul direct, iar `QUEST_ONLY` si `SHOP_ONLY` il permit numai cand conditia lor minima este disponibila;
- refuzurile de profil au variante rotite, aplica cooldown si nu sunt persistate ca dialog sau relatie;
- `NpcSimulationMode.NONE` nu intra in simularea nevoilor, `LIGHT` actualizeaza doar nevoile, iar `FULL` permite si deciziile autonome, rutina si procesarea sociala;
- `VILLAGER/HUMANOID` folosesc Villager, `MONSTER` foloseste Zombie, iar `SPIRIT/MARKER` ArmorStand;
- `ANIMAL/NONE` folosesc adaptorul no-op si nu ofera spawn real;
- orice mod diferit de `RUNTIME_ONLY` apeleaza acelasi `saveNPC(...)`; `FULL` si `LIGHT` nu au politici distincte;
- `NPCManager.saveNPC` refuza actorii `RUNTIME_ONLY`, iar `saveAllNPCs` ii omite la shutdown; o instanta runtime-only nu este promovata tacit in DB la oprirea pluginului;
- un actor deja alocat nu poate fi spawnat din nou prin acelasi `actorId`; `duration_seconds` programeaza despawn-ul pentru instanta curenta, iar un timer vechi nu poate inchide actorul inlocuit intre timp;
- la despawn explicit sau final de scenariu, actorii `RUNTIME_ONLY` sunt scosi din cache, cei `TEMPORARY`/`EPISODIC`/`SCENE_ONLY` persistati sunt stersi prin `NPCManager.deleteNPC`, iar cei `PERMANENT` persistati sunt doar despawnati;
- stergerea este permisa numai pentru NPC-uri cu provenienta `scenario_actor`; daca stergerea din DB esueaza, scenariul ramane activ pentru o noua incercare;
- profilurile noi cu provenienta `scenario_actor` salveaza metadata de lifecycle, persistenta, simulare, interactiune, entitate si ownership in `profile_data.scenario_actor` cu versiune explicita;
- daca salvarea initiala a unui actor esueaza dupa insert, se incearca stergerea randului partial; un esec de rollback este raportat explicit;
- la incarcarea generala, dintre randurile cu `profile_source=scenario_actor`, numai actorii `PERMANENT` cu metadata valida sunt incarcati direct; ceilalti intra initial in carantina si sunt raportati in log. Restaurarea scenariului poate recupera ulterior numai actorii tranzitorii eligibili, cu entitatea originala verificata; restul raman in DB pentru audit, fara inregistrare runtime;
- villagerii care indica un ID DB neincarcat sunt pastrati daca randul exista sau verificarea DB esueaza. Descoperirea, reasocierea si repararea duplicatelor nu ii adopta si nu ii sterg, inclusiv cand impart un `source_key` cu un NPC activ. Auditul distinge randul neincarcat/neverificabil de unul confirmat inexistent. Verificarea nu este memorata in cache: dupa stergerea explicita a randului, o noua scanare poate elimina entitatea orfana;
- `/ainpc audit db` si `/ainpc repair scenario-actors dryrun [pagina]` arata randurile in carantina; `apply <id> <uuid>` poate sterge doar un actor tranzitoriu cu metadata valida, neincarcat in runtime, dupa verificarea ID-ului, UUID-ului si profilului. Randurile legacy/invalide nu pot fi sterse prin aceasta comanda.
- `/ainpc scenario templates` listeaza ID-uri de template cu faza initiala; un admin in joc poate porni unul prin `/ainpc scenario start <templateId> [--force]` la pozitia sa si il poate incheia prin `/ainpc scenario end <templateId>`. Pornirea cere implicit `minimumNpcCount` NPC-uri active in raza de 32 blocuri din aceeasi lume; `--force` ignora doar acest prag pentru test, nu esecul de persistenta ori duplicatele. Actorii definiti in template, spawnati ulterior, nu sunt numarati in acest prag. Pornirea creeaza o singura instanta per template si incearca spawn-ul actorilor automati; fazele si regulile actorilor din instanta raman cele de la pornire chiar daca template-urile sunt reincarcate.
- `ScenarioSessionSnapshotCodec` poate captura si valida un snapshot JSON versionat (v1) cu ID-ul instantei, template-ul, faza, timpul, ancora, rolurile si legaturile actorId/UUID/ID DB. Pentru actorii cu durata pozitiva, snapshot-ul nou include optional `expires_at` (epoch millis), calculat la spawn; valorile invalide sau depasite numeric sunt respinse inainte de spawn. `retired_actor_ids` marcheaza explicit actorii eliminati din instanta dupa cleanup reusit; un ID retras nu poate figura simultan ca actor activ. `/ainpc scenario snapshot <templateId>` afiseaza un rezumat read-only si incompatibilitatile de template, fara a afisa JSON-ul complet.
- snapshot-urile v1 noi includ optional o amprenta SHA-256 determinista a identitatii template-ului, fazelor, regulilor de rol si definitiilor actorilor. Decodarea pastreaza compatibilitatea cu randurile v1 vechi fara amprenta; restaurarea cu actori cere obligatoriu amprenta identica cu template-ul curent. Restaurarea fara actori poate folosi si randuri vechi.
- `scenario_sessions` pastreaza snapshot-ul v1 la pornire, spawn/despawn si schimbarea fazei; un final reusit il sterge. Shutdown-ul resalveaza sesiunile active, iar pornirea logheaza numarul de randuri ramase. Salvarea initiala esuata opreste pornirea instantei; stergerea esuata pastreaza instanta activa pentru retry.
- actorii noi persistati primesc `scenario_session_id` in metadata de profil, separat de `owner_scenario_id` definit in pack. `/ainpc scenario recovery [pagina]` pagineaza snapshot-urile stocate si afiseaza doar un preview read-only: versiunea, template-ul, lumea, duplicatele, actorii si ownership-ul sunt verificate conservator. Pentru `CANDIDAT`, preview-ul explica limitarile politicii de restaurare automata fara a modifica snapshot-ul; `CANDIDAT` nu activeaza scenariul.
- la pornire, se restaureaza automat numai snapshot-uri v1 valide, unice per template, cu lume incarcata, fara roluri/progresie/obiective/recompense/trigger-e/actiuni runtime. Fiecare actor din template trebuie sa fie activ sau retras explicit in snapshot; un actor absent fara marcaj nu este presupus retras. Actorii activi trebuie sa fie persistati, `AUTO`, fara regula de despawn, cu amprenta de template, UUID, ID DB, `scenario_session_id`, source key, lume si metadata identice cu snapshot-ul si definitia curenta. Amprenta este obligatorie si daca exista numai actori retrasi. Actorii permanenti activi trebuie sa fie deja incarcati; actorii activi `TEMPORARY`/`EPISODIC`/`SCENE_ONLY` sunt cititi read-only din DB si acceptati doar daca Villagerul original este inca incarcat, valid, cu UUID, ID DB, lume si marcajele PDC intacte, iar `npc_source_keys` indica acelasi rand. Nu se creeaza entitati noi. Un actor cu `duration_seconds` pozitiv cere `expires_at` neexpirat si coerent cu durata si timpul de pornire; schedulerul reia numai timpul ramas, cu verificare UUID si deadline la executie. Daca inregistrarea actorilor ori programarea unui timer esueaza, noua sesiune si actorii reactivati sunt scosi din cache, iar randurile DB si entitatile raman nemodificate. Se pastreaza ID-ul, faza, ancora, timpul, legaturile actorilor activi si marcajele celor retrasi, fara executarea altor efecte. Peste 1000 de randuri nu se restaureaza nimic. `scenario recovery` marcheaza `ACTIV`, `AUTO`, `CANDIDAT` si `BLOCAT`.

## Gap-uri confirmate

- `QUEST_ONLY` verifica o oferta asociata actorului curent prin UUID, ID DB sau nume; `SHOP_ONLY` cere un magazin configurat pentru ocupatie, tipul/ID-ul cladirii mapate la `workAnchor` daca magazinul are `place_types`/`place_ids`, permisiunea jucatorului si cel putin o oferta cu schimb valid structural; nu valideaza plata sau stocul;
- `despawn_rule`, ownership-ul si tag-urile nu conduc cleanup-ul;
- randurile actorilor temporari create inainte de marcajul de sesiune si amprenta de template nu sunt curatate automat la restart; scenariile cu progresie nu sunt rehidratate;
- un rand vechi fara inregistrare in `npc_profiles` nu poate fi identificat sigur drept actor de scenariu si necesita audit manual;
- randurile legacy/invalide si cele fara `npc_profiles` necesita in continuare investigatie si reparare manuala; pot ocupa chei de sursa persistente;
- scenariile pornite manual nu aloca automat roluri NPC/jucator ori quest progression; validarea `minimumNpcCount` verifica doar prezenta NPC-urilor active, nu compatibilitatea lor cu rolurile, si nu garanteaza ca fiecare actor definit poate fi spawnat. Cele cu actori trebuie incheiate explicit inainte de shutdown daca actorii nu trebuie sa lase randuri in carantina;
- snapshot-urile cu actori neeligibili sau progresie nu produc rehidratare. Dupa restart, randurile neeligibile raman pentru diagnostic si nu blocheaza pornirea unei noi instante din acelasi template; `scenario snapshot` inspecteaza numai instanta runtime. Verificarea de template nu confirma singura existenta, identitatea si ownership-ul randurilor NPC dupa restart;
- actorii persistati inainte de marcajul `scenario_session_id` sau snapshot-urile cu actori fara amprenta de template nu pot trece restaurarea automata. Actorii tranzitorii fara Villager original valid, cei cu alt tip de entitate si cei din chunk-uri neincarcate raman in carantina; preview-ul citeste candidatii fara a-i inregistra ori sterge. Daca NPC-ul unui actor urmarit lipseste din cache la cleanup, legatura din snapshot este pastrata pentru diagnostic, nu transformata in marcaj de retragere. O captura noua este refuzata cand un actor urmarit nu este incarcat sau cand un actor persistent nu mai are ID DB disponibil, ca sa nu suprascrie identitatea din snapshot-ul anterior. Preview-ul de recovery distinge acum un deadline valid dar expirat de unul inconsistent (durata/deadline imposibil, spawn anterior sesiunii ori in viitor). Verificarea identitatii, a randului DB si a entitatii tranzitorii ruleaza independent de verificarea timerului: un actor expirat poate avea simultan si identitate incorecta, ceea ce nu autorizeaza cleanup. Ambele stari de timer raman diagnostice, fara cleanup automat; reluarea timerelor neexpirate functioneaza si pentru actorii tranzitorii verificati, dar efectele runtime lipsesc. Un `CANDIDAT` este doar o preconditie, nu o garantie de restore;
- o eroare de salvare la spawn/despawn/faza este logata, dar nu anuleaza mutatia runtime; snapshot-ul persistent poate ramane in urma pana la urmatoarea salvare reusita;
- `FULL` si `LIGHT` de persistenta folosesc in continuare acelasi format de salvare.

## Regula de folosire curenta

- foloseste `RUNTIME_ONLY` pentru actori care nu trebuie sa lase rand DB;
- trateaza persistenta `LIGHT` si `despawn_rule` drept contracte incomplete; simularea `LIGHT` reduce procesarea, dar nu limiteaza memoria persistenta;
- verifica explicit logul si randurile vechi la restart; ruleaza dryrun si backup inainte de orice `apply`. Carantina nu sterge date automat.

## Proiect separat: rehidratarea scenariilor active

Profilul unui actor nu poate reconstrui singur `ActiveScenario`: acesta retine template-ul, faza, rolurile, obiectivele, actorii alocati si timpul de inceput. Rehidratarea necesita un snapshot versionat al instantei de scenariu, nu reinterpretarea tuturor randurilor `scenario_actor` ca scenarii active.

- persista ID-ul instantei, ID-ul si versiunea template-ului, faza, timpul de inceput, rolurile si perechile `actorId`/UUID (plus ID DB pentru actorii persistati);
- la pornire, incarca si valideaza template-urile si NPC-urile persistente inaintea snapshot-urilor; reia numai instantele compatibile, fara a crea duplicat pentru actorii deja alocati;
- trateaza actorii `RUNTIME_ONLY` ca pierduti dupa restart si defineste explicit politica de respawn sau anulare a scenariului; nu ii reconstitui din DB;
- revalideaza durata, ownership-ul si world binding-ul inainte de a reporni timere ori efecte; o restaurare esuata lasa snapshot-ul diagnosticabil, fara cleanup automat al randurilor ambigue.

Captura, decodarea, persistenta v1, preview-ul si restaurarea limitata la sesiuni narative fara actori sau cu actori permanenti ori tranzitorii verificati sunt implementate, inclusiv reluarea conservatoare a timerelor neexpirate si marcarea actorilor retrasi dupa cleanup reusit. Respawn-ul actorilor pierduti, recuperarea timerelor expirate, a progresiei si a efectelor raman neimplementate. Nu interpreta orice rand `scenario_sessions` drept sesiune activa dupa restart si nu curata automat actorii prin simpla citire a snapshot-ului. Procedura pentru randurile legacy/invalide este in `operations/audit.md`.

## Legaturi

- `architecture/simulation-service.md`
- `architecture/dialog-si-conversatii.md`
- `planning/rutine-npc-si-timeline.md`
