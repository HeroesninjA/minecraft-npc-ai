# GUI si UX hardening

Status: roadmap activ, neimplementat integral.
Verificat fata de runtime: 2026-07-16.

Acest document pastreaza numai imbunatatirile compatibile ramase. Comportamentul curent este detinut de `reference/gui-stack.md`.

Baseline-ul curent separa `ainpc.gui.quest` de ecranele creator/quest map si reaplica `canOpen` la click. Inventarul mutatiilor si gate-ul final unificat raman deschise.

## P0 - autorizare consecventa

- extinde separarea implementata intre inspectie player si mutatie creator/admin la toate ecranele;
- foloseste aceeasi politica pentru `canOpen`, butoanele hub-urilor si actiunile finale;
- pastreaza reautorizarea cheii la click si adauga revalidarea targetului, read-only si preconditiilor de business;
- decide si testeaza explicit daca `features.gui=false` trebuie sa blocheze toate apelurile directe `GuiService.open`;
- inventariaza fiecare buton care executa o comanda si confirma permission/feature gate-ul din comanda tinta.

## P0 - formulare si stare

- adauga timeout pentru `TextInputRequest` si mesaje de expirare;
- separa `cancel` de `clear`: cancel trebuie sa pastreze valoarea anterioara si sa abandoneze request-ul;
- centralizeaza schema, sanitizarea si limitele campurilor creator;
- nu colecta secrete prin chat;
- adauga o operatie explicita de reset complet al draftului si afiseaza ce stare ramane numai in memorie;
- pastreaza testul de cleanup pentru toate hartile noi adaugate in `GuiService`.

## P1 - navigare

- introdu o stiva sau un parent contract explicit pentru `Back`;
- defineste semantic diferit `Back`, `Home`, `Refresh`, `Close` si `Resume`;
- pastreaza selectorii necesari cand se revine din confirmare ori input text;
- unifica paginarea si mesajele pentru lista goala, pagina invalida si filtru fara rezultate.

## P1 - confirmari

- inventariaza actiunile distructive si marcheaza acoperirea lor prin `openConfirmCommand`;
- centralizeaza metadata operatiei: titlu, efect, target, return key si audit context;
- limiteaza `gui.skip_confirmations` la medii de test aprobate sau adauga un warning persistent;
- revalideaza targetul si permisiunea dupa confirmare, inainte de mutatie.

## P1 - feedback si accesibilitate

- diferentiaza vizual `read-only`, `preview`, `draft`, `export`, `persist` si `publish`;
- arata cauza exacta pentru butoanele disabled fara a expune date sensibile;
- uniformizeaza culorile, materialele, terminologia si mesajele de eroare;
- adauga localizare pentru textele hardcodate;
- evita sa ascunda rezultatul unei comenzi dupa inchiderea inventarului.

## P1 - testare

- extinde testele comportamentale rol-cheie-permisiune dincolo de suprafetele creator;
- testeaza sesiuni stale, click de la alt player, close, quit si feature disable;
- testeaza confirmarea cu si fara bypass;
- testeaza chat input valid, invalid, clear, cancel, timeout si reluarea selectorului;
- transforma gradual auditurile care cauta stringuri in teste ale API-ului GUI sau smoke-uri Paper controlate.

## Criteriu de finalizare

- fiecare ecran are owner, rol, feature gate si tip de efect documentate;
- nicio mutatie nu depinde exclusiv de starea butonului randat;
- toate hartile per player au cleanup si test;
- toate actiunile distructive au confirmare si revalidare sau o exceptie justificata;
- fluxurile player, creator si admin au smoke-uri reproductibile.

## Legaturi

- `reference/gui-stack.md`
- `architecture/harta-clase-gui.md`
- `guides/gui-interfete.md`
- `reference/sistem-permisiuni-compatibilitate-pluginuri.md`
- `operations/debugging-si-testare.md`
