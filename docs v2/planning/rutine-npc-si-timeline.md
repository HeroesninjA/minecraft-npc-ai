# Rutine NPC si timeline

Status: roadmap activ; nu confirma implementarea.
Actualizat: 2026-10-19.

Rutina de baza exista. Acest document pastreaza numai munca ramasa pana la un timeline coerent.

## Baseline implementat

- `RoutineEngine`, `RoutineAssignment`, `RoutineService` si `RoutineCoordinator`;
- profile pe ocupatie, ferestre orare si offset stabil;
- ancore home/work/social si miscare cu pathfinding plus teleport fallback;
- `AINPCRoutineChangedEvent` public;
- household-uri persistente si family binding in fluxul de spawn;
- pause/resume si custom override sunt aplicate de `RoutineService`; starile ocupate au prioritate fata de override;
- numai `NpcSimulationMode.FULL` intra in rutina, procesarea sociala si activitatile sezoniere;
- cleanup-ul actorilor de scenariu respecta lifecycle-ul si persistenta la despawn; expirarea verifica UUID-ul instantei;
- metadata actorilor persistati noi este salvata, iar la restart actorii tranzitorii sau cu metadata necunoscuta sunt filtrati conservator, fara stergeri automate.
- auditul DB si dryrun-ul listeaza actorii in carantina; repararea explicita sterge numai un actor tranzitoriu validat per comanda.
- descoperirea si repararea duplicatelor pastreaza villagerii cu rand DB neincarcat ori neverificabil; un actor in carantina nu isi pierde entitatea prin scanarea de startup sau de chunk.
- instanta activa de scenariu poate fi pornita/incheiata manual; pornirea este idempotenta per template, valideaza `minimumNpcCount` pentru NPC-uri active in raza de 32 blocuri (cu `--force` explicit pentru teste) si copiaza fazele si regulile actorilor din template-ul selectat, inclusiv din addonuri.
- captura JSON v1 si verificarea structurala/de template a snapshot-ului sunt disponibile prin diagnosticul read-only `scenario snapshot`.
- snapshot-urile v1 ale sesiunilor sunt salvate tranzactional in `scenario_sessions` la mutatii si sterse la final; la restart sunt restaurate automat doar sesiunile narative fara roluri/progresie, fara actori ori cu actori permanenti si/sau tranzitorii persistati si verificati, in limita a 1000 de randuri. Actorii tranzitorii cer Villagerul original incarcat si marcaje de identitate intacte; nu se face respawn.
- actorii noi persistati au marcajul `scenario_session_id`, iar `scenario recovery` ofera un preview paginat, read-only, al conditiilor de restaurare; nu reface singur instantele.
- preview-ul separa deadline-urile valide dar expirate de deadline-urile inconsistente si verifica independent identitatea actorului; ambele stari de timer blocheaza in continuare restaurarea automata.

## Partial sau neconectat

- ora vine direct din `World.time`; nu exista `TimeService`;
- household-ul si familia nu influenteaza atribuirea rutinei;
- evenimentul de rutina nu are consumator core pentru quest sau story;
- `NpcInteractionProfile` limiteaza dialogul, dar scenariile cu progresie nu sunt rehidratate dupa restart; pornirea manuala valideaza doar numarul minim de NPC-uri active din raza de 32 blocuri, fara alocare de roluri. Actorii temporari vechi, fara entitatea originala ori cu timer expirat raman in carantina.

## Ordinea ramasa

1. extinde restaurarea verificata la actorii fara entitatea originala si la progresie; fluxul cu actori permanenti sau tranzitorii verificati, amprenta determinista a definitiilor relevante si reluarea timerelor neexpirate exista, dar lipsesc reconcilierea actorilor pierduti, timer-ele expirate si efectele runtime;
2. stabileste consumatori read-only, idempotenti, pentru evenimentul de rutina;
3. introduce o abstractie de timp numai daca apar calendar sau timp accelerat;
4. proiecteaza `TimelineEngine` dupa ce sursele si ownership-ul evenimentelor sunt stabile.

## Non-obiective imediate

- scheduler global care rescrie quest, story si economie;
- pathfinding semantic pe drumuri in acelasi refactor;
- timeline bazat pe AI ca autoritate.

## Legaturi

- `architecture/simulation-service.md`
- `architecture/harta-clase-routine.md`
- `architecture/households-persistente.md`
- `architecture/npc-uri-temporare-si-episodice.md`
