# GUI Interfete

Status: ghid operational.
Verificat in cod: 2026-07-16.

Acest ghid explica folosirea suprafetelor GUI. Contractul infrastructurii si limitele sale sunt in `reference/gui-stack.md`.

## Inainte de acces

- `features.gui` trebuie sa fie activ;
- comanda trebuie executata de un player;
- permisiunea depinde de ecran, conform `GuiService.canOpen` si `plugin.yml`;
- unele operatii depind suplimentar de feature flagul si permisiunea comenzii pe care butonul o executa.

## Deschidere

- `/ainpc gui` deschide `Player Hub`, `Creator Hub` sau `Admin Hub` dupa rol;
- `/ainpc gui player|creator|admin` cere explicit hub-ul dorit;
- `/ainpc gui quest [filter]` deschide quest log-ul;
- `/ainpc quest gui [filter]` este intrarea echivalenta pentru quest;
- `/ainpc gui resume` redeschide ultima cheie memorata;
- un selector necunoscut afiseaza optiunile scurte, nu deschide un ecran arbitrar.

## Alegerea suprafetei

- player: quest log, detalii, oferte, interactiune NPC, shop si statistici;
- context: world, region/place, story, rutina, relatii si memorii NPC;
- creator: quick quest, formular avansat, editor, definitii, test, quest map si mapping creator;
- admin: manager NPC, mapping/quest admin, audit, debug si MCP.

Accesul efectiv este per cheie. Numele familiei nu inlocuieste verificarea permisiunii.

## Navigare

- butonul `Refresh` randeaza din nou ecranul din datele curente;
- controlul standard `Inapoi` revine la hub-ul principal, nu neaparat la ecranul parinte;
- `Resume` retine o singura cheie, nu un istoric complet;
- butoanele disabled nu executa actiunea si afiseaza action bar;
- un buton care ruleaza o comanda inchide inventarul inainte de executie.

## Formulare prin chat

Unele ecrane creator cer valori in chat.

1. inventarul se inchide;
2. urmatorul mesaj al playerului este anulat si consumat ca input;
3. inputul invalid repeta promptul;
4. inputul valid actualizeaza draftul in memorie si redeschide ecranul;
5. `clear`, `cancel` sau `anuleaza` curata campul si redeschid formularul.

Nu folosi acest flux pentru secrete. Nu confunda mesajul `Salvat` al campului cu persistenta unui quest sau mapping.

## Confirmari

- confirmarea exista numai cand ecranul a cerut-o explicit;
- verifica titlul, warning-urile si targetul inainte de acceptare;
- `gui.skip_confirmations=true` ocoleste dialogul si executa imediat;
- pe servere normale, pastreaza `gui.skip_confirmations=false`.

## Interpretarea rezultatelor

- `preview`: afisare fara commit;
- `draft`: stare de formular ori fisier exportat;
- `export`: artefact scris sau text afisat, fara incarcare automata;
- `persist mapping`: modificari runtime urmate de `/ainpc world save`;
- `publish pack`: pas separat, neexecutat automat de creatorii GUI curenti.

## Depanare rapida

- inventarul se inchide la primul click: verifica `features.gui`;
- buton blocat: verifica permisiunea exacta a cheii si rolul intern al hub-ului;
- comanda ruleaza dar actiunea esueaza: verifica feature flagul si permisiunea comenzii;
- formularul nu primeste chat: verifica existenta `TextInputRequest` si listenerul de chat;
- pagina pare veche: foloseste `Refresh`; inventarele nu se actualizeaza reactiv;
- actiunea nu are confirmare: verifica apelantul, deoarece confirmarea nu este automata.

## Legaturi

- `reference/gui-stack.md`
- `architecture/harta-clase-gui.md`
- `guides/tutorial-gui-creator.md`
- `guides/gui-admin-mapping-quest.md`
- `planning/gui-ux-hardening.md`
