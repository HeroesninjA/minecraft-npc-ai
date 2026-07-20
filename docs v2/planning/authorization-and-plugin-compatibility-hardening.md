# Authorization and plugin compatibility hardening

Status: roadmap compatibil, neimplementat integral.
Actualizat: 2026-07-16.

Comportamentul verificat este detinut de `reference/sistem-permisiuni-compatibilitate-pluginuri.md`. Acest document pastreaza numai lucrul ramas.

## Baseline implementat

- permisiuni Bukkit/Paper fara stocare interna de grupuri;
- separare intre `ainpc.quest` public si `ainpc.creator` pentru formularele de authoring;
- revalidarea cheii GUI la click;
- toate nodurile folosite de `GuiService.canOpen` declarate in `plugin.yml`;
- Vault declarat `softdepend`, providerul reflexiv inregistrat si raspunsurile de tranzactie corectate;
- teste unitare pentru politica creator, descriptor si contractul Vault.

## P0 - inventar complet de autorizare

- extrage un catalog unic pentru toate nodurile folosite de comenzi, listener-e, GUI si servicii;
- clasifica fiecare operatie ca `view`, `play`, `author`, `mutate`, `admin` sau `diagnostic`;
- verifica toate comenzile dispatch-uite din GUI si toate actiunile directe care scriu in DB/fisiere;
- elimina ramurile unde un nod public ajunge la o mutatie creator/admin;
- adauga un audit static care detecteaza noduri runtime nedeclarate si declaratii nefolosite.

## P0 - gate final si confirmare

- muta autorizarea mutatiilor in servicii sau command policies reutilizabile, nu numai in ecran;
- revalideaza targetul, feature flagul, read-only si permisiunea dupa confirmare;
- defineste fail-closed pentru sesiuni stale, schimbare de lume si revocare de permisiune;
- inventariaza scrierile directe din `QuestMapGui`, `QuestCreateGui` si ecranele admin;
- adauga audit context pentru actor, operatie, target si rezultat.

## P1 - noduri si UX

- decide daca sunt necesare noduri distincte precum `ainpc.quest.author`, `ainpc.world.author` si `ainpc.debug.export`;
- pastreaza compatibilitatea `ainpc.creator` prin `children` numai dupa o politica de migrare explicita;
- filtreaza help-ul si tab completion-ul dupa permisiune fara a le transforma in singurul gate;
- documenteaza granturi minime pentru player, moderator, creator si operator;
- testeaza negarile explicite si combinatiile parent/child intr-un manager real de permisiuni.

## P1 - LuckPerms

- adauga smoke-uri cu LuckPerms pentru contexte world/server si schimbare de context;
- confirma re-evaluarea dupa grant/revoke fara reconnect;
- verifica interactiunea dintre `default: true`, negari explicite si wildcard-uri;
- pastreaza integrarea prin Bukkit daca nu exista o nevoie verificata pentru API-ul LuckPerms.

## P1 - protectie world

- defineste un contract generic `ProtectionIntegration` pentru mutatiile world;
- implementeaza optional un adaptor WorldGuard numai cu API suportat si `softdepend` explicit;
- verifica build mode, mapping, fixture, patch si spawn la intrarea in zona protejata;
- stabileste comportamentul cand adaptorul este absent, indisponibil sau arunca eroare;
- nu cupla API-ul public generic la tipuri WorldGuard.

## P1 - Vault

- ruleaza teste de integrare cu VaultAPI/Vault real pe Paper controlat;
- testeaza selectia providerului cand exista EssentialsX Economy sau alt provider;
- decide suportul pentru mutatii offline si persistenta conturilor inexistente;
- documenteaza explicit politica pentru sume fractionare si limite de sold;
- trateaza dynamic enable/disable si retry fara restart;
- adauga health output pentru adaptor prezent, provider inregistrat si provider selectat.

## P2 - alte integrari

- adauga PlaceholderAPI expansion numai pentru placeholder-e stabile si read-only;
- evalueaza Citizens/ProtocolLib numai daca exista un use case care nu poate fi rezolvat de runtime-ul curent;
- pastreaza WorldEdit separat de protectia WorldGuard si de mapping-ul semantic;
- cere pentru fiecare adaptor: owner, lifecycle, absenta tolerata, test, metric si procedura de dezactivare.

## Criteriu de finalizare

- fiecare intrare publica are feature gate si politica de permisiune documentate;
- fiecare mutatie are gate final independent de UI;
- niciun nod folosit nu lipseste din descriptor;
- matricea player/creator/admin/console este testata comportamental;
- Vault absent/prezent/multiprovider are smoke reproductibil;
- mutatiile world respecta politica de protectie aprobata sau refuza explicit;
- documentatia nu promite integrari fara adaptor verificat.

## Legaturi

- `reference/sistem-permisiuni-compatibilitate-pluginuri.md`
- `reference/gui-stack.md`
- `reference/feature-flags-lifecycle.md`
- `planning/gui-ux-hardening.md`
- `architecture/worldedit-integration-contract.md`
- `planning/stabilizare-api-si-addonuri.md`
- `operations/demo-server-verification.md`
