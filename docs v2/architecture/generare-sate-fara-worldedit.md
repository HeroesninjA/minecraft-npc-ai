# Import si completare semantica fara WorldEdit

Status: contract verificat in cod.
Actualizat: 2026-07-18.

Runtime-ul curent nu genereaza fizic sate. El scaneaza blocuri existente si creeaza sau completeaza mapping-ul `Region -> Place -> Node`.

## Scanare si import

`/ainpc world scan village [radius]` este preview read-only. Comanda programeaza o sesiune in `VanillaVillageScanService`; varianta cu `import [regionId]` apeleaza `SemanticVillageMapper` si creeaza mapping in memorie numai dupa finalizarea raportului.

- scannerul cauta semnale vanilla precum bell, bed, workstation si door;
- `VanillaVillageScanCursor` reia determinist volumul `x/z/y` si nu depaseste bugetul primit la un pas;
- coada FIFO imparte intre toate cererile bugetul global `world_admin.scan.blocks_per_tick`, implicit `4096` si limitat la `256..65536`;
- fiecare pas ruleaza prin schedulerul sincron Bukkit; blocurile nu sunt citite off-thread;
- importul refuza o zona fara semnale de sat;
- regiunea, place-urile si node-urile sunt create in runtime;
- comanda cere apoi `/ainpc world save` pentru persistenta;
- daca importul esueaza dupa prima mutatie, region/place/node create de acel import sunt compensate inainte de rezultat.

`/ainpc world settlement auto <regionId> [maxHouses]` programeaza aceeasi scanare, apoi transmite raportul complet catre `AutoSettlementGenerator`, care executa importul si `HouseAllocationPlanner`. Numele comenzii ramane scurt, dar runtime-ul afiseaza inainte de scanare ca produce numai mapping semantic si `HouseAllocation`-uri, fara blocuri fizice; mesajul de succes spune `Mapping semantic de sat generat`. Comanda nu spawneaza NPC-uri si nu salveaza mapping-ul automat. Daca ID-ul exista, handlerul scaneaza centrul regiunii, iar auto-generatorul activeaza reutilizarea opt-in: mapper-ul valideaza lumea si centrul, pastreaza regiunea si tag-urile ei si adauga numai place-uri/node-uri noi. Importul standard prin `world scan village ... import` ramane strict si refuza un ID de regiune existent.

## Gap-uri si patch-uri

`/ainpc patch analyze|plan|validate` inspecteaza mapping-ul. `apply` poate aplica planuri semantice valide.

- capabilitatea implicita este `semantic-place-mapping`;
- patch-urile pentru node-uri pot fi aplicate;
- casele, locurile de munca si hub-urile sociale cer `native-block-build` si sunt blocate in fluxul normal de comanda;
- chiar implementarea interna `NATIVE_PATCH` creeaza numai limite de `Place` si node-uri, nu blocuri;
- `WORLDEDIT_TEMPLATE` intoarce doar warning de nesuportare.

## Template-uri de cladiri

`/ainpc building auto-place <templateId> <regionId>` creeaza un `Place` semantic si ancorele sale. Nu construieste cladirea fizica si nu foloseste WorldEdit.

## Protectii si limite

- citirea blocurilor ramane pe main thread pentru siguranta Bukkit, dar este impartita incremental intre tick-uri; bugetul este pe numar de blocuri, nu un deadline masurat in milisecunde;
- coada este anulata la oprirea pluginului, iar erorile unei sesiuni sunt izolate prin callback fara a bloca urmatoarea cerere;
- inainte de importul amanat, callback-ul reverifica `read_only`, `features.mapping` si starea world admin; schimbarea oricareia in timpul scanarii anuleaza mutatia;
- importul, patch apply, auto-place si fixture apply compenseaza creatiile proprii la esec; nu exista undo dupa succes sau tranzactie comuna cu spawn/persistenta;
- protectia runtime read-only este clasificata central pentru mutatiile de mapping si spawn fixture; preview-urile, planurile, validarile si dry-run-urile raman disponibile;
- salvarea explicita este obligatorie, iar dispatcher-ul afiseaza reminder-ul unic `/ainpc world save` cand mapping-ul ramane nesalvat;
- niciun mod activ nu plaseaza drumuri, case sau decor ca blocuri Minecraft.

## Legaturi

- `architecture/mapping.md`
- `architecture/settlement-plan.md`
- `planning/patch-planner.md`
- `reference/template-cladiri-si-marker-nodes.md`
