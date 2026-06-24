# Sistem de permisiuni si compatibilitate cu alte pluginuri

Actualizat: 2026-06-23

Acest document descrie cum se implementeaza sistemul de permisiuni pentru AINPC astfel incat sa ramana compatibil cu manageri externi precum LuckPerms, dar si cu sistemul standard Bukkit/Paper.

## Principiul de baza

- AINPC nu implementeaza propriul sistem de grupuri sau stocare de permisiuni.
- AINPC foloseste doar API-ul standard Bukkit:
  - `Player.hasPermission(...)`
  - `CommandSender.hasPermission(...)`
  - `Permissible`
  - `plugin.yml` pentru declararea nodurilor
- Managerul extern de permisiuni decide:
  - ce grupuri exista;
  - ce noduri moșteneste fiecare grup;
  - ce permisiuni temporare sau pe context sunt active.

Consecinta practica: daca serverul foloseste LuckPerms, AINPC trebuie sa functioneze fara cod special, atata timp cat apeleaza `hasPermission` si isi declara corect nodurile.

## Ce inseamna compatibil cu LuckPerms

LuckPerms este relevant prin:

- permisiuni pe noduri;
- inheritanta intre grupuri;
- context awareness;
- grants temporare;
- sincronizare cu operatori si grupuri.

AINPC trebuie sa fie compatibil prin design:

1. defineste noduri clare si stabile;
2. evita sa hardcodezi nume de grupuri;
3. nu salva local permisiile unui player;
4. nu duplica roluri sau ierarhii in baza proprie;
5. reevalueaza permisiunea la momentul actiunii, nu la login doar o data.

## Noduri recomandate

Nodurile deja folosite in proiect sunt un punct bun de plecare:

| Nod | Scop |
| --- | --- |
| `ainpc.admin` | bypass pentru administrare si UI admin |
| `ainpc.talk` | vorbit cu NPC-uri |
| `ainpc.info` | informatii NPC |
| `ainpc.quest` | quest log si actiuni quest |
| `ainpc.gui` | hub GUI general |
| `ainpc.gui.quest` | GUI quest |
| `ainpc.gui.story` | GUI story read-only |
| `ainpc.gui.stats` | GUI stats |
| `ainpc.gui.interact` | GUI interactiuni NPC |
| `ainpc.gui.routine` | GUI rutine |
| `ainpc.gui.shop` | GUI shop |
| `ainpc.gui.world` | GUI world |
| `ainpc.gui.manager` | manager NPC |
| `ainpc.gui.audit` | audit |
| `ainpc.gui.debug` | debug |
| `ainpc.creator` | modul creator |
| `ainpc.gui.quest_map` | quest map |

## Reguli de proiectare

### 1. Permisiunile trebuie sa fie la nivel de actiune

Nu face permisiuni dependente de numele UI-ului intern sau de clasă.

Corect:

- `ainpc.gui.quest`
- `ainpc.gui.world`
- `ainpc.admin`

Nerecomandat:

- `ainpc.gui.quest.create_button_17`
- `ainpc.quest.log.page.3.slot.12`

### 2. Permisiunea se verifica in momentul executiei

Pentru GUI si comenzi:

- verificarea se face la deschidere;
- se verifica din nou la click/confirm;
- daca rolul s-a schimbat intre timp, actiunea se respinge.

### 3. UI-ul poate ascunde sau dezactiva, dar nu trebuie sa presupuna

Meniurile pot:

- ascunde butoane;
- le pot arata dezactivate;
- pot afisa motivul in lore.

Dar decizia finala trebuie sa ramana in cod la executie.

### 4. Adminul poate avea bypass, dar nu trebuie sa-l ascunzi

`ainpc.admin` trebuie sa ramana nodul de bypass pentru:

- GUI admin;
- debug;
- audit;
- quest edit/authoring;
- world mapping;
- actiuni cu risc mare.

### 5. Nu cache-ui permisiunile pe termen lung

Nu salva rezultatul `hasPermission` in memorie pe sesiuni lungi.

Motiv:

- LuckPerms poate schimba grupuri si contexte dinamic;
- alte pluginuri pot actualiza permisiunea fara reconnect;
- actorii admin trebuie sa vada schimbarile imediat sau la urmatoarea verificare.

## Cum se declara in `plugin.yml`

AINPC deja declara nodurile in `ainpc-core-plugin/src/main/resources/plugin.yml`.

Model recomandat:

```yaml
permissions:
  ainpc.admin:
    description: Permite accesul la toate comenzile admin
    default: op
  ainpc.quest:
    description: Permite quest log-ul si comenzile normale de quest
    default: true
  ainpc.gui.quest:
    description: Permite deschiderea GUI-ului de questuri
    default: true
```

Reguli:

- `default: true` pentru functii de player;
- `default: op` pentru functii de admin;
- descriere scurta si clara;
- nodurile trebuie sa fie stabile intre versiuni.

## Cum lucreaza cu LuckPerms

Cu LuckPerms, administratorul serverului poate:

- sa acorde `ainpc.quest` unui grup de jucatori;
- sa acorde `ainpc.admin` unui grup staff;
- sa creeze grupuri separate pentru `creator`, `questeditor`, `worldadmin`;
- sa foloseasca contexte pentru lume, server, world sau server local.

AINPC nu trebuie sa stie despre aceste grupuri. Trebuie doar sa ceara:

- `player.hasPermission("ainpc.admin")`
- `player.hasPermission("ainpc.gui.quest")`
- `sender.hasPermission("ainpc.quest")`

### Exemple LuckPerms

```text
/lp group member permission set ainpc.quest true
/lp group member permission set ainpc.gui true
/lp group staff permission set ainpc.admin true
/lp group staff permission set ainpc.gui.debug true
```

`LuckPerms` va rezolva inheritanta si contextul; AINPC doar intreaba API-ul Bukkit.

## Structura recomandata in cod

### 1. Defineste nodurile intr-un singur loc

Ideal:

- o clasa sau obiect cu constante;
- aceleasi constante folosite in comenzi, GUI si listeners;
- fara string-uri hardcodate in 20 de locuri.

### 2. Centralizeaza verificarea

Ai deja un model bun in `GuiService.canOpen(...)`.

Acela trebuie sa fie sursa de adevar pentru:

- ce meniuri pot fi deschise;
- ce noduri dau acces;
- cand adminul face bypass.

### 3. Logheaza refuzul doar la nivel util

Cand permisiunea lipseste:

- trimite mesajul `no_permission`;
- logheaza doar daca este actiune admin sau audit important;
- nu transforma fiecare refuz intr-un spam de consola.

### 4. Valideaza din nou la click / confirm

Permisiunea pentru GUI nu inseamna permisiune permanenta.

Pattern recomandat:

1. deschide GUI;
2. la click, verifica iar permisiunea;
3. daca nu mai exista, inchide actiunea si afiseaza mesaj.

## Ce sa nu faci

- Nu lega permisiunile de numele unui plugin extern.
- Nu depinde de `LuckPerms` direct in logica principala daca nu ai nevoie de API-ul sau avansat.
- Nu salva grupurile si permisiunile in DB proprie ca sursa de adevar.
- Nu amesteca rolurile UI cu permisiunile tehnice.
- Nu lasa `op` sa devina singurul mecanism de admin daca proiectul deja are noduri dedicate.

## Cand are sens integrarea directa cu LuckPerms API

Doar daca ai nevoie de:

- contexte avansate;
- inspectarea grupurilor;
- scriere de permisiuni temporare;
- sincronizare cu metadata LuckPerms;
- analize de audit sau comparatie intre grupuri.

Chiar si atunci:

- tine integrarea intr-un adaptor separat;
- pastreaza Bukkit ca fallback;
- nu pune dependenta directa in business logic daca nu e necesar.

## Flux de implementare recomandat

1. Defineste nodurile de permisiune.
2. Adauga-le in `plugin.yml`.
3. Creeaza un helper central pentru verificare.
4. Leaga helperul in comenzi, GUI si listeners.
5. Asigura-te ca mesajele de refuz sunt consistente.
6. Adauga teste pentru:
   - player cu permisiune;
   - player fara permisiune;
   - admin cu bypass;
   - schimbare de permisiune intre doua actiuni.
7. Daca vrei integrare LuckPerms avansata, adauga un adaptor optional.

## Recomandare practica pentru AINPC

Pentru acest proiect, modelul potrivit este:

- Bukkit permissions ca strat canonic;
- LuckPerms ca manager extern suportat implicit;
- `plugin.yml` ca contract public;
- `GuiService` si comenzi ca puncte de control;
- noduri stabile, simple, fara ierarhii inutile.

Astfel, AINPC ramane compatibil cu LuckPerms si cu alte pluginuri similare fara sa devina dependent de unul singur.
