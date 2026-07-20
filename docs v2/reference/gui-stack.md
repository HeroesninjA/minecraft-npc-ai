# GUI Stack

Status: contract canonic de runtime verificat in cod.
Verificat in cod: 2026-07-16.

Acest document detine contractul infrastructurii GUI AINPC. Ghidurile explica utilizarea ecranelor, iar serviciile domeniilor detin validarea si persistenta datelor afisate sau mutate.

## Intrari

- `/ainpc gui` cere un player si verifica `features.gui`; fara selector deschide hub-ul rolului rezolvat;
- rolurile sunt `ADMIN`, `CREATOR`, `PLAYER`, in aceasta ordine, pe baza permisiunilor `ainpc.admin` si `ainpc.creator`;
- `/ainpc gui player|admin|creator` cere explicit un hub;
- `/ainpc gui <guiKey>` foloseste ID-urile si aliasurile din `GuiKey.fromId`;
- `/ainpc gui quest <filter>` si `/ainpc quest gui [filter]` deschid quest log-ul filtrat;
- `/ainpc gui resume` redeschide ultima cheie memorata, nu o stiva de navigare.

Comanda blocheaza intrarea cand `features.gui` este `false`. `GuiInventoryListener` verifica acelasi flag la click si drag. `GuiService.open` nu verifica singur flagul, deci apelantii directi trebuie tratati separat in hardening.

## Flux runtime

`command/listener -> GuiService.open -> canOpen -> GuiScreen.render -> GuiSession -> AINPCGuiHolder -> inventory`

La deschidere:

1. `GuiService.canOpen` verifica accesul pentru cheia ceruta;
2. registry-ul intern rezolva implementarea `GuiScreen`;
3. `GuiSessionManager` inchide sesiunea GUI anterioara a playerului si creeaza un UUID nou;
4. `GuiRenderContext` scrie itemele si retine numai butoanele actionabile;
5. sesiunea primeste snapshot-ul butoanelor, iar inventarul este deschis.

La click:

- inventarele AINPC sunt recunoscute prin `AINPCGuiHolder`, nu prin titlu;
- click-ul si drag-ul sunt anulate pentru inventarul superior;
- sesiunea trebuie sa existe si sa apartina playerului;
- `canOpen` este reaplicat pentru cheia sesiunii inainte de actiune;
- slotul trebuie sa contina un `GuiButton` activ cu actiune;
- exceptiile runtime sunt logate, iar playerul primeste un action bar de eroare.

`handleClick` reaplica permisiunea cheii, dar nu recalculeaza automat targetul, read-only, feature flagul ori starea de business. Un ecran trebuie sa dezactiveze actiunea la render si sa delege mutatia unei comenzi sau unui serviciu care valideaza din nou.

## Acces

`GuiService.canOpen` este sursa tehnica pentru permisiunile pe cheie. Verificarea foloseste OR intre permisiunile listate pentru ecran, nu o ierarhie universala de roluri.

- hub-ul principal si suprafetele player folosesc permisiuni precum `ainpc.gui`, `ainpc.gui.quest`, `ainpc.quest`, `ainpc.talk` si `ainpc.info`;
- world, rutina, manager, audit, debug si MCP folosesc permisiuni specializate ori `ainpc.admin`;
- formularele quest creator, quick quest si quest map cer `ainpc.admin`, `ainpc.creator` ori nodul specializat `ainpc.gui.quest_map`;
- mapping creator accepta `ainpc.admin`, `ainpc.creator` ori `ainpc.gui.world`;
- unele hub-uri aplica suplimentar verificari interne de rol si pot afisa un ecran restrictionat chiar daca cheia a trecut `canOpen`.

Default-urile Bukkit sunt declarate in `plugin.yml`. Documentatia nu trebuie sa deduca accesul efectiv numai din numele hub-ului.

## Actiuni

Exista trei forme principale:

1. navigare directa catre alt `GuiScreen`;
2. actiune locala sau apel de serviciu din callback-ul butonului;
3. `GuiService.runCommand`, care inchide inventarul si executa comanda ca player.

Pentru actiunile de tip comanda, permission check-ul, feature gate-ul si mesajele finale apartin comenzii. Inchiderea inventarului inainte de dispatch este comportament normal, nu eroare de navigare.

## Confirmari

`ConfirmActionGui` protejeaza numai actiunile care apeleaza explicit `openConfirmCommand`. Nu exista o interceptare automata pentru orice buton distructiv.

- request-ul de confirmare este tinut in memorie per player;
- confirmarea executa comanda memorata ca player;
- anularea revine la cheia si selectorul configurate de apelant;
- `gui.skip_confirmations` este `false` implicit;
- cand `gui.skip_confirmations=true`, comanda este executata imediat dupa mesajul `Auto-confirm`.

Acest flag este potrivit numai pentru medii controlate unde bypass-ul este intentionat.

## Input text prin chat

Formularele creator pot inchide inventarul si crea un singur `TextInputRequest` per player.

- `NPCChatListener` anuleaza mesajul si il proceseaza sincron;
- `clear`, `cancel` si `anuleaza` curata campul curent si redeschid ecranul; nu exista un rezultat separat de tip „pastreaza valoarea si abandoneaza”;
- inputul invalid este reprogramat cu acelasi prompt;
- valorile formularului sunt stare tranzitorie in `GuiService`, nu continut publicat;
- nu exista timeout dedicat pentru request-ul text.

Comportamentul global al chatului si dialogului este detinut de `architecture/interactiuni.md`.

## Sesiuni si stare

- exista cel mult o sesiune de inventar AINPC per player;
- inchiderea inventarului sterge sesiunea, dar nu toate selectiile formularului;
- filtrele, paginile, selectiile, drafturile, confirmarea, build mode-ul si ultimul ecran sunt harti in memorie per player;
- `PlayerQuitEvent` apeleaza `clearPlayerState` si elimina toate selectiile tranzitorii cunoscute;
- deschiderea unui ecran nou re-randeaza datele; nu exista sincronizare reactiva a unui inventar deja deschis;
- paginarea este implementata individual de ecrane, nu de infrastructura comuna.

## Navigare

`GuiNavigation.addStandardControls` adauga, pentru ecranele care il folosesc:

- slot 45: inapoi la `MAIN`, nu la parintele anterior;
- slot 49: refresh pentru cheia indicata;
- slot 53: inchidere.

Alte ecrane au controale proprii. `lastGuiKeys` retine o singura cheie pentru `resume`; nu reprezinta istoric complet.

## Familii de ecrane

- player si progresie: `PlayerHubGui`, `QuestLogGui`, `QuestDetailGui`, ofertele, statistici si shop;
- NPC si context: interactiune, rutina, relatii, memorie si story;
- world: hub, regiune, place, mapping admin si creatorii region/place/node;
- quest creator: quick quest, formular avansat, definitii, test, editor, authoring si quest map;
- operare: admin hub, manager NPC, audit, debug si MCP;
- infrastructura: confirmare si controale standard.

Lista exacta a cheilor si registrarilor este detinuta de `GuiKey` si blocul `init` din `GuiService`.

## Limite si lucru ramas

- autorizarea ramane impartita intre cheia GUI si gate-ul final al comenzii/serviciului;
- feature gate-ul nu este impus in interiorul `GuiService.open`;
- confirmarea si paginarea depind de implementarea fiecarui ecran;
- navigarea nu are stiva de parinti;
- formularele chat nu au timeout si folosesc cuvinte de „cancel” ca reset de camp;
- multe teste GUI sunt diagnostice statice, nu teste Bukkit end-to-end.

Lucrul compatibil ramas este urmarit in `planning/gui-ux-hardening.md`.

## Legaturi

- `architecture/harta-clase-gui.md`
- `guides/gui-interfete.md`
- `guides/tutorial-gui-creator.md`
- `guides/gui-admin-mapping-quest.md`
- `reference/sistem-permisiuni-compatibilitate-pluginuri.md`
- `planning/gui-ux-hardening.md`
